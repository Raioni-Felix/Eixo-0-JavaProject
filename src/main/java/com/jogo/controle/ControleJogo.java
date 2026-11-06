package com.jogo.controle;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.CollisionHandler;
import com.jogo.componentes.ComponenteArma;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.RelogioJogo;
import com.jogo.entidades.TipoEntidade;
import com.jogo.excecoes.JogoException;
import com.jogo.factories.FabricaEntidades;
import com.jogo.inventario.ModuloCura;
import com.jogo.inventario.ModuloEnergia;
import com.jogo.componentes.Dificuldade;
import com.jogo.itens.Arma;
import com.jogo.itens.CatalogoArmas;
import com.jogo.visao.TelaInventario;
import com.jogo.visao.TelaOpcoes;
import com.jogo.niveis.DadosNivel;
import com.jogo.visao.CortinaTransicao;
import com.jogo.visao.HudJogo;
import com.jogo.visao.TelaGameOver;
import com.jogo.visao.TelaMenuPrincipal;
import com.jogo.visao.TelaPausa;
import com.jogo.visao.TelaSobreposta;
import com.jogo.visao.TelaVitoria;
import com.jogo.componentes.ComponenteChefe;
import java.util.List;
import javafx.scene.input.KeyCombination;


import static com.almasb.fxgl.dsl.FXGL.getGameController;
import static com.almasb.fxgl.dsl.FXGL.getGameScene;
import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.getPhysicsWorld;
import static com.almasb.fxgl.dsl.FXGL.getPrimaryStage;
import static com.almasb.fxgl.dsl.FXGL.spawn;

//CONTROLE principal do MVC: decide o fluxo do jogo (menu, pausa, Game
//Over, reiniciar), cria o jogador e o arsenal, trata as colisões de
//gatilho/item e as exceções do modelo. O Main só repassa pra cá cada
//etapa do FXGL (initGame, initPhysics, initUI, onUpdate).
//
//  Modelo:   componentes, itens, habilidades, inventario, niveis...
//  Visão:    visao (HUD, telas, cortina) e componentes.visual
//  Controle: controle (este, ControleEntrada e GerenciadorSalas)
public class ControleJogo {

    //Ids dos módulos no inventário (CONSUMIVEL empilha: um id só, com contador)
    public static final String ID_MODULO_CURA = ModuloCura.ID;
    public static final String ID_MODULO_ENERGIA = ModuloEnergia.ID;

    //Energia máxima quando o JSON da sala não traz "maxEnergy"
    private static final int ENERGIA_MAXIMA_PADRAO = 50;

    private final GerenciadorSalas salas = new GerenciadorSalas();

    private Entity jogador;
    private HudJogo hud;
    private TelaMenuPrincipal menuPrincipal;
    private TelaPausa pausa;
    private TelaGameOver gameOver;
    private TelaVitoria vitoria;

    //Chefe da sala atual (null fora da arena), a fase já anunciada e a
    //contagem até a tela de vitória depois que ele explode
    private ComponenteChefe chefeAtual;
    private int faseAvisada = 1;
    private double tempoAteVitoria = 0;
    private double tempoDeJogo = 0;
    private TelaInventario inventario;
    private TelaOpcoes opcoes;
    private CortinaTransicao cortina;

    //Escolhida nas Opções; vale pra jogos novos também (sobrevive ao
    //startNewGame, igual aos controles)
    private Dificuldade dificuldade = Dificuldade.NORMAL;
    //Tela que abriu as Opções (menu principal ou pausa), pra voltar pra ela
    private TelaSobreposta telaAntesDasOpcoes;
    //Instantes (System.nanoTime) do último toque no inventário e de
    //quando ele fechou, pra filtrar a mesma tecla chegando duas vezes
    //Último nível já anunciado no HUD (ver avisarMagiasNovas)
    private int nivelAvisado = 1;
    private long ultimoToqueInventario = System.nanoTime() - 1_000_000_000L;
    private long fechouInventarioEm = ultimoToqueInventario;

    //Evita mostrar o Game Over de novo a cada frame enquanto o jogador
    //continua morto
    private boolean gameOverExibido = false;

    //IMPORTANTE: startNewGame() chama initGame()+initPhysics()+initUI()
    //de novo TODA vez, e não é garantido que termine de forma síncrona.
    //Por isso quem decide "mostrar o menu e pausar" é o montarInterface()
    //(chamado pelo initUI()), e não o código depois do startNewGame().
    //Por padrão ele sempre mostra o menu e pausa; só reiniciarNivel()
    //liga essa flag antes, pra cair direto no jogo.
    private boolean pularMenuAoIniciar = false;

    //=== Etapas do FXGL (chamadas pelo Main) ===

    //initGame(): monta a sala inicial, o jogador e o arsenal
    public void iniciarMundo() {
        getGameWorld().addEntityFactory(new FabricaEntidades());

        DadosNivel sala = salas.comecarDoInicio();

        jogador = spawn("jogador", new SpawnData(sala.jogador.x, sala.jogador.y)
                .put("nome", sala.jogador.nome)
                .put("vidaMaxima", sala.jogador.vidaMaxima)
                .put("velocidade", sala.jogador.velocidade)
                .put("energiaMaxima", sala.jogador.energiaMaxima > 0
                        ? sala.jogador.energiaMaxima
                        : ENERGIA_MAXIMA_PADRAO));

        nivelAvisado = 1;
        chefeAtual = null;
        faseAvisada = 1;
        tempoAteVitoria = 0;
        tempoDeJogo = 0;
        getComponenteJogador().setDificuldade(dificuldade);
        prepararArsenal();

        salas.centralizarCamera(jogador);
        salas.spawnInimigos(sala, jogador);
        salas.spawnItens(sala);

        //Sem isso, "Reiniciar" depois de morrer nunca mais mostraria o
        //Game Over numa segunda morte
        gameOverExibido = false;
    }

    //initPhysics(): gravidade e o que acontece ao encostar em gatilho/item
    public void registrarColisoes() {
        getPhysicsWorld().setGravity(0, 1200);

        //Gatilho de passagem: troca de sala com a cortina
        getPhysicsWorld().addCollisionHandler(new CollisionHandler(TipoEntidade.JOGADOR, TipoEntidade.GATILHO) {
            @Override
            protected void onCollisionBegin(Entity jogadorEntity, Entity gatilhoEntity) {
                int indiceDestino = gatilhoEntity.getInt("indiceDestino");
                String ladoEntrada = gatilhoEntity.getString("ladoEntrada");
                salas.iniciarTransicao(indiceDestino, ladoEntrada, jogador, cortina);
            }
        });

        //Módulo no chão: vira objeto do inventário e some do mapa
        getPhysicsWorld().addCollisionHandler(new CollisionHandler(TipoEntidade.JOGADOR, TipoEntidade.ITEM) {
            @Override
            protected void onCollisionBegin(Entity jogadorEntity, Entity itemEntity) {
                String tipoItem = itemEntity.getString("tipoItem");
                int quantidade = itemEntity.getInt("quantidade");

                ComponenteJogador componente = jogadorEntity.getComponent(ComponenteJogador.class);
                if ("arma".equals(tipoItem)) {
                    Arma arma = CatalogoArmas.criar(itemEntity.getString("arma"));
                    componente.coletarItem(arma);
                    hud.mostrarMensagem("Nova arma: " + arma.getNome() + "   [Q] trocar   [I] inventário");
                } else if ("energia".equals(tipoItem)) {
                    componente.coletarItem(new ModuloEnergia(quantidade));
                    hud.mostrarMensagem("+1 Módulo de Energia   [2] usar");
                } else {
                    componente.coletarItem(new ModuloCura(quantidade));
                    hud.mostrarMensagem("+1 Módulo de Cura   [1] usar");
                }

                salas.registrarColeta(itemEntity.getString("idColeta"));
                itemEntity.removeFromWorld();
                hud.atualizarInventario();
            }
        });
    }

    //initUI(): cria o HUD e as telas. A ordem de adicionar é a ordem de
    //desenho (o último fica por cima): HUD, Game Over, pausa, menu, cortina
    public void montarInterface() {
        hud = new HudJogo(getComponenteJogador(), getComponenteArma(), ID_MODULO_CURA, ID_MODULO_ENERGIA);

        gameOver = new TelaGameOver(this::reiniciarNivel, () -> voltarAoMenuPrincipal(gameOver));
        vitoria = new TelaVitoria(this::reiniciarNivel, () -> voltarAoMenuPrincipal(vitoria));
        pausa = new TelaPausa(this::retomarDaPausa, () -> abrirOpcoes(pausa), () -> voltarAoMenuPrincipal(pausa));
        menuPrincipal = new TelaMenuPrincipal(this::iniciarJogo, () -> abrirOpcoes(menuPrincipal),
                () -> getGameController().exit());
        inventario = new TelaInventario(this::fecharInventario, this::equiparPeloInventario, this::usarModulo);
        opcoes = new TelaOpcoes(this::fecharOpcoes, this::trocarTelaCheia, this::trocarDificuldade, Controles.LISTA);
        //Opções por cima do menu (abre dele), inventário por baixo da pausa
        getGameScene().addUINodes(gameOver, vitoria, inventario, pausa, menuPrincipal, opcoes);

        if (pularMenuAoIniciar) {
            menuPrincipal.esconder();
            pularMenuAoIniciar = false;
        } else {
            menuPrincipal.mostrar();
            getGameController().pauseEngine();
        }

        cortina = new CortinaTransicao();
        getGameScene().addUINode(cortina);
    }

    //onUpdate(): câmera, HUD e Game Over
    public void atualizar(double tpf) {
        //Ignora a rajada de ticks logo após despausar (ver RelogioJogo)
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }

        salas.atualizarCamera(jogador, tpf);

        if (jogador.isActive()) {
            tempoDeJogo += tpf;
            hud.atualizarStatus(); //o XP muda quando um inimigo morre, sem avisar o controle
            avisarMagiasNovas();
            acompanharChefe(tpf);
            return;
        }

        //A barra de vida já zera sozinha (bind); aqui só mostra o Game Over
        if (!gameOverExibido) {
            gameOverExibido = true;
            gameOver.mostrar();
            getGameController().pauseEngine();
        }
    }

    //Arena do chefe: barra no topo, aviso da fase 2 e, quando ele cai,
    //a tela de vitória (depois de 2 s, pra ver as explosões)
    private void acompanharChefe(double tpf) {
        if (chefeAtual == null) {
            List<Entity> chefes = getGameWorld().getEntitiesByType(TipoEntidade.CHEFE);
            if (!chefes.isEmpty()) {
                chefeAtual = chefes.get(0).getComponent(ComponenteChefe.class);
                faseAvisada = 1;
                hud.mostrarChefe(chefeAtual);
                hud.mostrarMensagem(chefeAtual.getNome() + " despertou! A porta se trancou.");
            }
        } else if (chefeAtual.estaMorto()) {
            salas.marcarChefeDerrotado();
            hud.esconderChefe();
            chefeAtual = null;
            tempoAteVitoria = 2.0;
        } else if (chefeAtual.getFase() == 2 && faseAvisada == 1) {
            faseAvisada = 2;
            hud.mostrarAviso("NEXUS: núcleo instável! Ataques mais rápidos");
        }

        if (tempoAteVitoria > 0) {
            tempoAteVitoria -= tpf;
            if (tempoAteVitoria <= 0) {
                vitoria.atualizar(getComponenteJogador().getNivel(), tempoDeJogo);
                vitoria.mostrar();
                getGameController().pauseEngine();
            }
        }
    }

    //Subiu de nível (XP de inimigo morto): avisa se liberou magia nova
    private void avisarMagiasNovas() {
        int nivel = getComponenteJogador().getNivel();
        if (nivel == nivelAvisado) {
            return;
        }
        nivelAvisado = nivel;
        if (nivel == ComponenteJogador.NIVEL_GELO) {
            hud.mostrarMensagem("Nível " + nivel + "! Nova magia: Raio de Gelo [V]");
        } else if (nivel == ComponenteJogador.NIVEL_BURACO_NEGRO) {
            hud.mostrarMensagem("Nível " + nivel + "! Nova magia: Buraco Negro [B] (energia cheia)");
        } else {
            hud.mostrarMensagem("Nível " + nivel + "!");
        }
    }

    //=== Ações (teclas e botões) ===

    //Só abre a pausa com o jogador vivo e nenhuma outra tela por cima
    public boolean podePausar() {
        return jogador.isActive() && !gameOverExibido && !menuPrincipal.estaVisivel() && !vitoria.estaVisivel()
                && !inventario.estaVisivel() && !opcoes.estaVisivel()
                //ESC que fechou o inventário não abre a pausa no mesmo toque
                && System.nanoTime() - fechouInventarioEm > 200_000_000L;
    }

    //=== Inventário (I / TAB) ===

    public void alternarInventario() {
        //A mesma tecla pode chegar duas vezes (pela tela do inventário e
        //pelo input do FXGL): ignora a segunda se vier logo em seguida
        long agora = System.nanoTime();
        if (agora - ultimoToqueInventario < 200_000_000L) {
            return;
        }
        ultimoToqueInventario = agora;
        if (inventario.estaVisivel()) {
            fecharInventario();
        } else if (podePausar() && !pausa.estaVisivel()) {
            inventario.atualizar(getComponenteJogador(), getComponenteArma());
            inventario.mostrar();
            getGameController().pauseEngine();
        }
    }

    public void fecharInventario() {
        if (!inventario.estaVisivel()) {
            return;
        }
        fechouInventarioEm = System.nanoTime();
        inventario.esconder();
        hud.atualizarInventario();
        hud.atualizarStatus();
        despausarMotor();
    }

    //Clique numa arma do inventário. A arma veio do próprio inventário,
    //mas a exceção ainda é tratada aqui, como no resto do controle
    private void equiparPeloInventario(String idArma) {
        try {
            getComponenteArma().equiparArma(idArma);
        } catch (JogoException e) {
            hud.mostrarAviso(e.getMessage());
        }
        inventario.atualizar(getComponenteJogador(), getComponenteArma());
    }

    //=== Opções ===

    private void abrirOpcoes(TelaSobreposta origem) {
        telaAntesDasOpcoes = origem;
        origem.esconder();
        opcoes.atualizar(getPrimaryStage().isFullScreen(), dificuldade.getNome());
        opcoes.mostrar();
    }

    private void fecharOpcoes() {
        opcoes.esconder();
        if (telaAntesDasOpcoes != null) {
            telaAntesDasOpcoes.mostrar();
        }
    }

    public void trocarTelaCheia() {
        //Sem isso o ESC do JavaFX sairia da tela cheia em vez de pausar
        getPrimaryStage().setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        getPrimaryStage().setFullScreenExitHint("");
        getPrimaryStage().setFullScreen(!getPrimaryStage().isFullScreen());
        opcoes.atualizar(getPrimaryStage().isFullScreen(), dificuldade.getNome());
    }

    private void trocarDificuldade() {
        dificuldade = dificuldade.proxima();
        getComponenteJogador().setDificuldade(dificuldade);
        opcoes.atualizar(getPrimaryStage().isFullScreen(), dificuldade.getNome());
    }

    public void abrirMenuPausa() {
        if (pausa.estaVisivel()) {
            return; //seguraram ESC
        }
        pausa.mostrar();
        getGameController().pauseEngine();
    }

    public void retomarDaPausa() {
        pausa.esconder();
        despausarMotor();
    }

    //"PLAY" do menu principal
    public void iniciarJogo() {
        menuPrincipal.esconder();
        despausarMotor();
    }

    //"Reiniciar" do Game Over: recomeça direto no jogo, sem passar pelo menu
    public void reiniciarNivel() {
        gameOver.esconder();
        vitoria.esconder();
        //Garantia extra: os outros overlays também escondidos
        menuPrincipal.esconder();
        pausa.esconder();

        pularMenuAoIniciar = true;
        despausarMotor();
        getGameController().startNewGame();
    }

    //"Menu"/"Menu Principal" (pausa e Game Over): recomeça e volta pro
    //menu pausado. Não despausa: mostrar o menu e pausar já é o padrão
    //do montarInterface()
    public void voltarAoMenuPrincipal(TelaSobreposta telaParaEsconder) {
        telaParaEsconder.esconder();
        getGameController().startNewGame();
    }

    //Teclas 1 e 2. Aqui as exceções do modelo são TRATADAS: em vez de
    //derrubar o jogo, o motivo aparece no HUD
    public void usarModulo(String idModulo) {
        try {
            getComponenteJogador().usarItem(idModulo);
            hud.atualizarInventario();
        } catch (JogoException e) {
            hud.mostrarAviso(e.getMessage());
        }
        if (inventario.estaVisivel()) {
            inventario.atualizar(getComponenteJogador(), getComponenteArma());
        }
    }

    public Entity getJogador() {
        return jogador;
    }

    public ComponenteJogador getComponenteJogador() {
        return jogador.getComponent(ComponenteJogador.class);
    }

    public ComponenteArma getComponenteArma() {
        return jogador.getComponent(ComponenteArma.class);
    }

    //=== Auxiliares ===

    //Arsenal inicial: só a espada. As outras armas ficam no chão das
    //salas (itens "arma" no JSON) e entram no inventário ao encostar.
    //Igual ao barebones: a arma entra no inventário e só depois é
    //equipada pelo id.
    private void prepararArsenal() {
        Arma espada = CatalogoArmas.criar("espada");
        getComponenteJogador().coletarItem(espada);
        try {
            getComponenteArma().equiparArma(espada.getId());
        } catch (JogoException e) {
            //A espada acabou de entrar no inventário: se falhar é bug
            throw new IllegalStateException("Arsenal inicial inválido", e);
        }

        //Mesma chamada pra todas as armas do jogo, resposta diferente pra
        //cada classe: polimorfismo (igual ao foreach de atacar() da
        //atividade de RPG)
        System.out.println("Armas do jogo:");
        for (String nome : CatalogoArmas.NOMES) {
            Arma arma = CatalogoArmas.criar(nome);
            System.out.println("  " + arma.getNome() + ": " + arma.getDescricaoCombate());
        }
    }

    //Sempre no lugar de resumeEngine() direto, pra marcar o período de
    //graça junto (ver RelogioJogo)
    private void despausarMotor() {
        getGameController().resumeEngine();
        RelogioJogo.marcarRetomada();
    }
}
