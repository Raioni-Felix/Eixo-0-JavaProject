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
import com.jogo.itens.AdagaDupla;
import com.jogo.itens.Arma;
import com.jogo.itens.Espada;
import com.jogo.itens.PistolaDuplaDePlasma;
import com.jogo.itens.RifleSniper;
import com.jogo.itens.Shotgun;
import com.jogo.niveis.DadosNivel;
import com.jogo.visao.CortinaTransicao;
import com.jogo.visao.HudJogo;
import com.jogo.visao.TelaGameOver;
import com.jogo.visao.TelaMenuPrincipal;
import com.jogo.visao.TelaPausa;
import com.jogo.visao.TelaSobreposta;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getGameController;
import static com.almasb.fxgl.dsl.FXGL.getGameScene;
import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.getPhysicsWorld;
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
    private CortinaTransicao cortina;

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
                if ("energia".equals(tipoItem)) {
                    componente.coletarItem(new ModuloEnergia(quantidade));
                } else {
                    componente.coletarItem(new ModuloCura(quantidade));
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
        pausa = new TelaPausa(this::retomarDaPausa, () -> voltarAoMenuPrincipal(pausa));
        menuPrincipal = new TelaMenuPrincipal(this::iniciarJogo, () -> getGameController().exit());
        getGameScene().addUINodes(gameOver, pausa, menuPrincipal);

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
            hud.atualizarStatus(); //o XP muda quando um inimigo morre, sem avisar o controle
            return;
        }

        //A barra de vida já zera sozinha (bind); aqui só mostra o Game Over
        if (!gameOverExibido) {
            gameOverExibido = true;
            gameOver.mostrar();
            getGameController().pauseEngine();
        }
    }

    //=== Ações (teclas e botões) ===

    //Só abre a pausa com o jogador vivo e nenhuma outra tela por cima
    public boolean podePausar() {
        return jogador.isActive() && !gameOverExibido && !menuPrincipal.estaVisivel();
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

    //Arsenal inicial. Igual ao barebones: cada arma entra no inventário
    //e só depois é equipada pelo id. Todas numa lista do tipo Arma, mesmo
    //sendo de classes diferentes.
    private void prepararArsenal() {
        List<Arma> armasIniciais = List.of(
                new Espada("espada", "Espada", "Arma inicial do jogador", 20, 0.5, 40),
                new AdagaDupla("adagas", "Adagas Duplas", "Dois cortes rápidos, de perto", 8, 0.3, 30),
                new Shotgun("shotgun", "Shotgun", "Balins em leque, forte de perto", 8, 5, 0.9, 220),
                new PistolaDuplaDePlasma("pistola", "Pistola Dupla de Plasma", "Dois tiros por ataque", 9, 0.45, 450),
                new RifleSniper("rifle", "Rifle Sniper", "Tiro que atravessa inimigos", 30, 1.4, 900, 3));

        getComponenteJogador().coletarItem(armasIniciais); //sobrecarga da lista
        try {
            getComponenteArma().equiparArma(armasIniciais.get(0).getId());
        } catch (JogoException e) {
            //A espada acabou de entrar no inventário: se falhar é bug
            throw new IllegalStateException("Arsenal inicial inválido", e);
        }

        //Mesma chamada pra todas, resposta diferente pra cada classe:
        //polimorfismo (igual ao foreach de atacar() da atividade de RPG)
        System.out.println("Arsenal:");
        for (Arma arma : getComponenteArma().getArsenal()) {
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
