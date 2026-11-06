package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.ViewComponent;
import com.almasb.fxgl.input.KeyTrigger;
import com.almasb.fxgl.input.TriggerListener;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.jogo.excecoes.ItemIncompativelException;
import com.jogo.excecoes.ItemNaoEncontradoException;
import com.jogo.habilidades.Apara;
import com.jogo.habilidades.BolaDeFogo;
import com.jogo.habilidades.GolpeDeEnergia;
import com.jogo.inventario.Consumivel;
import com.jogo.inventario.Inventariavel;
import com.jogo.inventario.Inventario;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getInput;

/**
 * Componente do jogador.
 *
 * Herda de ComponentePersonagem (vida, dano recebido, movimento básico) e
 * adiciona o que é específico do jogador: frames de invencibilidade
 * depois de tomar dano, o sistema de XP/nível (metroidvania, sem
 * score), o movimento (via física de verdade) e o empurrão (recoil)
 * de contato com inimigos.
 *
 * Trazido do barebones: energia (a "mana" das habilidades), inventário
 * com consumíveis, e as habilidades GolpeDeEnergia (tecla X) e Apara
 * (tecla C), cada uma com o próprio cooldown (ver pacote habilidades).
 */


public class ComponenteJogador extends ComponentePersonagem implements Atacante {

    //Atributos de invencibilidade
    protected double duracaoInvencibilidade = 1.0; //segundos de invencibilidade após tomar dano
    private double timerInvencibilidade = 0;



    //Atributos de progressão (XP / nível)
    protected int nivel = 1;
    protected int xp = 0;
    protected int xpProximoNivel = 100;



    //Velocidade vertical do pulo (negativo é pra cima, no FXGL/Box2D o
    //eixo Y cresce pra baixo, igual tela). A gravidade quem cuida é o
    //motor de física agora (controle.ControleJogo.registrarColisoes()).
    private static final double VELOCIDADE_PULO = -500;



    //Força do empurrão de contato (recoil). Sempre pra direção oposta
    //de quem causou o hit (ver empurrar()), não da direção que o
    //jogador tá olhando.
    private static final double VELOCIDADE_EMPURRAO_HORIZONTAL = 220;
    private static final double VELOCIDADE_EMPURRAO_VERTICAL = 260;



    //Jogador não tem atrito nenhum (friction 0, ver FabricaEntidades),
    //então sem isso o empurrão do knockback deslizava pra sempre se
    //não apertasse nenhuma tecla. Corta a velocidade horizontal quando
    //zera (mesma janela do hit-stun).
    private double timerRecuperacaoEmpurrao = 0;



    //"Hit-stun" de verdade, só trava o CONTROLE do jogador (ver
    //estaAtordoado()) por essa janela bem curta, bem menor que
    //duracaoInvencibilidade (que continua sendo o cooldown de
    //dano/invencibilidade, 1s). Sem essa separação o movimento ficava
    //travado o segundo inteiro, e piorava o "preso entre inimigos".
    private static final double DURACAO_HIT_STUN = 0.2;



    //Por quanto tempo o jogador fica piscando depois de um hit. Só
    //efeito visual, não mexe em colisão nem física. O empurrão roda
    //sempre, piscando ou não.
    private static final double DURACAO_PISCADA = 0.6;
    private double timerPiscada = 0;
    private static final double INTERVALO_PISCADA = 0.08;
    private static final double OPACIDADE_PISCADA_FRACA = 0.3;

    //PhysicsComponent do FXGL, quem move a entidade de verdade agora
    //(gravidade, colisão com plataforma). Pego em onAdded(), que roda
    //depois que o PhysicsComponent já foi anexado (ver ordem dos
    //.with() em FabricaEntidades.spawnJogador()).
    private PhysicsComponent fisica;

    //Componente que guarda a visão (o retângulo azul do jogador por
    //enquanto), usado só pra mexer na opacidade e fazer o efeito de
    //piscar durante os frames de invencibilidade.
    private ViewComponent visao;

    //ENERGIA (trazida do barebones). Propriedade observável igual a
    //vida, pra barra de energia do HUD se atualizar sozinha (ver
    //visao.HudJogo).
    protected int energiaMaxima;
    protected IntegerProperty energiaAtual = new SimpleIntegerProperty();

    //Recarga lenta da energia, pontos por segundo. Não existe no
    //barebones; sem ela o golpe acabava depois de 3 usos e só voltava
    //com Módulo de Energia. Pra desligar, é só pôr 0.
    private static final double RECARGA_ENERGIA_POR_SEGUNDO = 2.0;
    private double acumuladorRecarga = 0; //guarda a fração até virar 1 ponto

    //INVENTÁRIO (trazido do barebones): cada jogador tem o seu.
    private final Inventario inventario = new Inventario();

    //HABILIDADES (trazidas do barebones, mesmos números do Jogo.java
    //de lá). Variável do tipo da mãe (GolpeDeEnergia) guardando uma
    //filha (BolaDeFogo): polimorfismo.
    private final GolpeDeEnergia golpe = new BolaDeFogo(true, 2.0, 15, 0.0, 20);
    private final Apara apara = new Apara(true, 1.5, 5, 0.3);

    //Janela da Apara: enquanto > 0, serAtingido() ignora dano e
    //empurrão. O escudo é só o contorno visual dessa janela.
    private double timerApara = 0;
    private Rectangle escudo;

    //Pra qual lado o jogador está olhando, atualizado em
    //moverEsquerda()/moverDireita(). Só usado pra saber se o jogador pode se
    //mexer (estaAtordoado()), o empurrão agora usa a posição de quem
    //bateu, não isso aqui.
    private boolean olhandoDireita = true;


    // Controle do Sistema de Habilidades
    private boolean podePularDuas = true; // Flag para autorizar o pulo no ar
    private boolean jaDeuDash = false; // Evita que o jogador dê dashes infinitos sem pisar no chão
    private double timerCooldownDash = 0;
    private static final double VELOCIDADE_DASH = 600;
    private static final double COOLDOWN_DASH = 1.0;

    //Quanto tempo dura a "rajada" do dash (estilo Hollow Knight/Silksong:
    //um impulso curto e fixo, não uma velocidade que fica pra sempre).
    //Enquanto timerDash > 0 o jogador está "trancado" nessa velocidade —
    //nem moverEsquerda/moverDireita nem pararHorizontal mexem nela (ver
    //estaNoDash() abaixo) — e ao zerar o controle normal volta. Sem
    //isso, como o jogador não tem fricção nenhuma (todo movimento é
    //velocidade setada na mão, ver moverEsquerda/moverDireita/pararHorizontal),
    //a velocidade do dash nunca era desfeita e o jogador ficava
    //deslizando na velocidade do dash pra sempre.
    private double timerDash = 0;
    private static final double DURACAO_DASH = 0.15;

    //Pequeno bônus de velocidade pra recompensar quem segura o Shift
    //depois do dash em vez de soltar na hora (tipo um "sprint" curto
    //puxado pelo próprio golpe do dash). boostDashAtivo só fica true
    //se o último dash realmente aconteceu (dash() só chega a ligar
    //isso se passar pelas travas dele) e o Shift continuar segurado
    //sem soltar depois — soltando o Shift (onKeyEnd) desliga o bônus
    //na hora.
    private boolean boostDashAtivo = false;
    private static final double MULTIPLICADOR_BOOST_DASH = 1.12; // bem pequeno, só pra fazer diferença

    //O dash não é um UserAction registrado em controle.ControleEntrada (o FXGL
    //proíbe addAction em teclas modificadoras como SHIFT/CONTROL/ALT —
    //ver comentário em controle.ControleEntrada — e, nessa versão do FXGL,
    //nem existe um Input.isHeld(KeyCode) pra consulta crua; só
    //descobri isso depois de tentar e o Maven acusar "cannot find
    //symbol"). O jeito certo, confirmado na fonte do próprio FXGL
    //17.3 (Input.kt/TriggerListener.kt), é um TriggerListener: ele é
    //avisado de TODA tecla pressionada no jogo (onKeyBegin/onKeyEnd),
    //então o listener registrado em onAdded() só reage quando a tecla
    //for Shift — dash() no toque (onKeyBegin dispara uma vez só, sem
    //precisar controlar "borda de subida" na mão) e desliga o boost
    //ao soltar (onKeyEnd).
    public ComponenteJogador(String nome, int vidaMaxima, double velocidade, int energiaMaxima) {
        super(nome, vidaMaxima, velocidade);
        this.energiaMaxima = energiaMaxima;
        this.energiaAtual.set(energiaMaxima); //começa com energia cheia
    }

    @Override
    public void onAdded() {
        fisica = entity.getComponent(PhysicsComponent.class);
        visao = entity.getComponent(ViewComponent.class);

        //Contorno ciano em volta do jogador, só aparece durante a Apara.
        escudo = new Rectangle(entity.getWidth() + 8, entity.getHeight() + 8, Color.TRANSPARENT);
        escudo.setStroke(Color.CYAN);
        escudo.setStrokeWidth(3);
        escudo.setTranslateX(-4);
        escudo.setTranslateY(-4);
        escudo.setVisible(false);
        visao.addChild(escudo);

        getInput().addTriggerListener(new TriggerListener() {
            @Override
            protected void onKeyBegin(KeyTrigger keyTrigger) {
                if (keyTrigger.getKey() == KeyCode.SHIFT) {
                    dash();
                }
            }

            @Override
            protected void onKeyEnd(KeyTrigger keyTrigger) {
                if (keyTrigger.getKey() == KeyCode.SHIFT) {
                    boostDashAtivo = false;
                }
            }
        });
    }

    @Override
    public void onUpdate(double tpf) {
        //Ignora a rajada de ticks logo após despausar (ver RelogioJogo).
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }

        if (timerCooldownDash > 0) timerCooldownDash -= tpf;

        //Cooldown das habilidades: cada uma conta o próprio tempo.
        golpe.atualizar(tpf);
        apara.atualizar(tpf);

        if (timerApara > 0) {
            timerApara -= tpf;
            if (timerApara <= 0) {
                escudo.setVisible(false);
            }
        }

        recarregarEnergia(tpf);

        //Conta a janela do dash. Ao terminar, corta a velocidade na
        //hora — sem desaceleração suave. Pesquisei o comportamento
        //documentado do dash de Hollow Knight/Silksong (não dá pra ver
        //o código-fonte deles, é fechado) e o padrão do gênero é
        //exatamente esse: uma rajada curta e "seca", onde o controle
        //volta instantaneamente ao normal assim que ela termina — não
        //um "glide" suave de saída. Tentei uma desaceleração suave
        //antes (smoothstep) e ficou "flutuando" demais pro estilo
        //pretendido; isso aqui é mais fiel.
        if (timerDash > 0) {
            timerDash -= tpf;

            if (timerDash <= 0) {
                fisica.setVelocityX(0);
            }
        }

        //reseta as habilidades aéreas sempre que tocar o chão
        if (fisica.isOnGround()) {
            podePularDuas = true;
            jaDeuDash = false;
        }
        if (timerInvencibilidade > 0) {
            timerInvencibilidade -= tpf;
        }

        if (timerPiscada > 0) {
            timerPiscada -= tpf;

            if (timerPiscada <= 0) {
                //Acabou a piscada, garante que a visão volta 100%
                //opaca (senão podia travar semi transparente se o
                //timer zerasse no meio de uma piscada apagada).
                visao.setOpacity(1.0);
            } else {
                //Pisca, alterna a opacidade a cada INTERVALO_PISCADA
                //segundos enquanto durar a invencibilidade visível.
                boolean fraco = ((int) Math.floor(timerPiscada / INTERVALO_PISCADA)) % 2 == 0;
                visao.setOpacity(fraco ? OPACIDADE_PISCADA_FRACA : 1.0);
            }
        }

        //Corta o deslize do empurrão se o jogador não mexeu em nada
        if (timerRecuperacaoEmpurrao > 0) {
            timerRecuperacaoEmpurrao -= tpf;

            if (timerRecuperacaoEmpurrao <= 0) {
                fisica.setVelocityX(0);
            }
        }
    }

    //Controla só o controle do jogador (mover/parar), bem mais curto
    //que estaInvencivel() (que rege o cooldown de dano). Ver comentário
    //de DURACAO_HIT_STUN. Público porque o ComponenteArma também
    //usa (não pode atacar durante o hit-stun).
    public boolean estaAtordoado() {
        return timerInvencibilidade > (duracaoInvencibilidade - DURACAO_HIT_STUN);
    }

    //Sobrescreve tomarDano pra ignorar dano enquanto estiver invencível
    //e reiniciar o timer sempre que um hit for realmente aplicado
    @Override
    public void tomarDano(int quant) {
        if (estaInvencivel()) {
            return;
        }

        super.tomarDano(quant);
        timerInvencibilidade = duracaoInvencibilidade;
    }

    public boolean estaInvencivel() {
        return timerInvencibilidade > 0;
    }

    //Chamado pelo sensor de contato de cada inimigo (ver
    //FabricaEntidades.setupContatoComPlayer) e pelo projétil do
    //atirador (ver ComponenteProjetil) quando acertam o jogador. O
    //empurrão acontece sempre, dano e piscada só se não estiver
    //invencível ainda.
    public void serAtingido(Entity origem, int dano) {
        //Apara ativa: o golpe é bloqueado por inteiro (sem dano e sem
        //empurrão).
        if (estaAparando()) {
            return;
        }

        empurrar(origem);

        if (!estaInvencivel()) {
            tomarDano(dano);
            timerPiscada = DURACAO_PISCADA;
        }
    }

    //Empurra pro lado oposto de quem bateu (baseado na posição de
    //quem causou o hit, não mais em olhandoDireita). Antes só olhava pra
    //onde o jogador tava olhando, então levar um hit pelas costas
    //empurrava errado (pra trás de novo, na cara de quem bateu). Agora
    //funciona igual pro contato de inimigo e pro projétil do atirador
    //(os dois passam a "origem" certa aqui). setLinearVelocity troca
    //a velocidade toda de uma vez, pro recoil ficar nítido tipo soco,
    //em vez de só somar em cima do que já tinha.
    private void empurrar(Entity origem) {
        double empurraoX = origem.getCenter().getX() < entity.getCenter().getX()
                ? VELOCIDADE_EMPURRAO_HORIZONTAL
                : -VELOCIDADE_EMPURRAO_HORIZONTAL;
        fisica.setLinearVelocity(empurraoX, -VELOCIDADE_EMPURRAO_VERTICAL);
        timerRecuperacaoEmpurrao = DURACAO_HIT_STUN;
    }

    //MOVIMENTO
    //Agora tudo passa pelo PhysicsComponent (setVelocityX/Y). Com
    //física de verdade a posição é controlada pelo motor, então mexer
    //nela na mão (entity.translateX, setX...) seria ignorado ou
    //brigaria com o Box2D.


    //Enquanto estaAtordoado() (janela bem mais curta que a invencibilidade
    //inteira, ver DURACAO_HIT_STUN), o jogador ignora comando de
    //movimento horizontal. Sem isso, segurar A ou D no momento do hit
    //sobrescreveria o recoil no frame seguinte e o soco nunca
    //apareceria de verdade. Depois desse tempinho curto o controle
    //volta, mesmo ainda invencível/piscando, pra dar tempo real do
    //jogador sair andando de perto de outros inimigos.


    // Só pula se estiver no chão ou se tiver o pulo duplo disponível.
    // Evita pulo infinito no ar.
    public void pular() {
        if (estaAtordoado()) return; // Ignora comando se estiver no hit-stun do empurrão

        if (fisica.isOnGround()) {
            fisica.setVelocityY(VELOCIDADE_PULO);
        } else if (podePularDuas) {
            // Pulo duplo é levemente mais fraco e consome a flag até pisar no chão de novo
            fisica.setVelocityY(VELOCIDADE_PULO * 0.9);
            podePularDuas = false;
        }
    }

    // Investida horizontal rápida. Concede invencibilidade temporária.
    // Pode ser usada andando ou no meio de um pulo (só trava o eixo Y
    // e "sequestra" o controle horizontal por DURACAO_DASH, ver
    // estaNoDash()/onUpdate()/moverEsquerda/moverDireita/pararHorizontal).
    public void dash() {
        // Bloqueia se atordoado, se já usou no ar sem pisar no chão, ou se está em cooldown
        if (estaAtordoado() || jaDeuDash || timerCooldownDash > 0) return;

        double direcaoDash = olhandoDireita ? VELOCIDADE_DASH : -VELOCIDADE_DASH;
        fisica.setVelocityX(direcaoDash);
        fisica.setVelocityY(0); // Zera o eixo Y para o dash ser perfeitamente reto no ar

        jaDeuDash = true;
        timerCooldownDash = COOLDOWN_DASH;
        timerDash = DURACAO_DASH;
        timerInvencibilidade = 0.2; // Pequena janela de invencibilidade (i-frames) durante o dash

        //Shift já está pressionado nesse instante (dash() só roda a
        //partir do toque dele) — o bônus vale em moverEsquerda/moverDireita
        //até soltar (onKeyEnd desliga).
        boostDashAtivo = true;
    }

    //Enquanto o dash está "rolando" (ver dash()/onUpdate()), o
    //controle normal de horizontal não deve sobrescrever a velocidade
    //dele — segurar A/D (que chamam moverEsquerda/moverDireita todo frame,
    //ver controle.ControleEntrada) sobrescrevia a velocidade do dash no frame
    //seguinte e cancelava a investida quase inteira. Exposto também
    //pro ComponenteArma, se um dia quiser bloquear ataque durante o
    //dash.
    public boolean estaNoDash() {
        return timerDash > 0;
    }

    // Dispara o ataque agora vive em ComponenteArma (atacar()), que
    // usa esse estaAtordoado() e o olhandoDireita abaixo pra saber se pode
    // agir e pra que lado golpear/mirar.

    //Exposto pro ComponenteArma saber pra que lado o jogador está
    //olhando (mira do ataque melee/distância).
    public boolean estaOlhandoDireita() {
        return olhandoDireita;
    }

    public void moverEsquerda() {
        if (estaAtordoado() || estaNoDash()) {
            return;
        }
        olhandoDireita = false;
        fisica.setVelocityX(-velocidadeComBoost());
    }

    public void moverDireita() {
        if (estaAtordoado() || estaNoDash()) {
            return;
        }
        olhandoDireita = true;
        fisica.setVelocityX(velocidadeComBoost());
    }

    //Aplica o pequeno bônus do dash (ver boostDashAtivo acima) em
    //cima da velocidade normal, sem duplicar essa conta nos dois
    //métodos de cima.
    private double velocidadeComBoost() {
        return boostDashAtivo ? velocidade * MULTIPLICADOR_BOOST_DASH : velocidade;
    }

    //Chamado quando a seta de movimento é solta (onActionEnd em
    //controle.ControleEntrada), zera a velocidade horizontal. Também respeita
    //o hit-stun curto,
    //mesmo motivo de moverEsquerda/moverDireita: soltar a tecla no meio do
    //recoil não pode zerar o empurrão.
    public void pararHorizontal() {
        if (estaAtordoado() || estaNoDash()) {
            return;
        }
        fisica.setVelocityX(0);
    }

    //XP / NÍVEL
    //Metroidvania, então a progressão vem de subir de
    //nível: ganhar XP suficiente aumenta o nível e melhora os
    //atributos do personagem (vida e velocidade), curando ele.
    public void adicionarXp(int quant) {
        xp += quant;

        while (xp >= xpProximoNivel) {
            xp -= xpProximoNivel;
            subirNivel();
        }
    }

    private void subirNivel() {
        nivel++;
        vidaMaxima += 20;

        // MODIFICADO: Substitui "vidaAtual = vidaMaxima;"
        // Motivo: Usar "=" sobrescreve a referência na memória e desliga a UI
        // dos eventos. O método .set() altera apenas o valor interno notificado.
        vidaAtual.set(vidaMaxima);

        velocidade += 5;
        xpProximoNivel = 100 + (nivel - 1) * 50;
    }

    public int getNivel() {
        return nivel;
    }

    public int getXp() {
        return xp;
    }

    public int getXpProximoNivel() {
        return xpProximoNivel;
    }

    //ENERGIA (mesmas regras do Jogador do barebones)

    //Substitui um setEnergiaAtual: o jogador garante que a energia não
    //passa da máxima. Quem recupera (ex: ModuloEnergia) só diz quanto.
    public void recuperarEnergia(int quantidade) {
        if (quantidade <= 0) {
            return;
        }
        energiaAtual.set(Math.min(energiaMaxima, energiaAtual.get() + quantidade));
    }

    public boolean energiaSuficiente(int custoEnergia) {
        return energiaAtual.get() >= custoEnergia;
    }

    public void consumirEnergia(int custoEnergia) {
        energiaAtual.set(Math.max(0, energiaAtual.get() - custoEnergia));
    }

    private void recarregarEnergia(double tpf) {
        if (energiaAtual.get() >= energiaMaxima) {
            acumuladorRecarga = 0;
            return;
        }
        acumuladorRecarga += RECARGA_ENERGIA_POR_SEGUNDO * tpf;
        if (acumuladorRecarga >= 1) {
            int pontos = (int) acumuladorRecarga;
            acumuladorRecarga -= pontos;
            recuperarEnergia(pontos);
        }
    }

    public ReadOnlyIntegerProperty energiaAtualProperty() {
        return energiaAtual;
    }

    public int getEnergiaAtual() {
        return energiaAtual.get();
    }

    public int getEnergiaMaxima() {
        return energiaMaxima;
    }

    //ATAQUE (interface Atacante, igual ao Jogador do barebones)

    //Tecla Z (ver controle.ControleEntrada). O jogador não escolhe alvo: ataca
    //pra onde está olhando com a arma equipada, e quem sabe COMO atacar
    //é a própria arma (ver ComponenteArma e pacote itens).
    @Override
    public void iniciarAtaque() {
        if (entity.getComponent(ComponenteArma.class).atacar()) {
            marcarAtaque();
        }
    }

    //ANIMAÇÃO: o componente de animação (componentes.visual) só lê estes

    //Quanto tempo a pose de ataque fica na tela depois de cada golpe
    private static final double DURACAO_POSE_ATAQUE = 0.35;
    private long instanteUltimoAtaque = 0;

    private void marcarAtaque() {
        instanteUltimoAtaque = System.nanoTime();
    }

    public boolean estaAtacando() {
        return instanteUltimoAtaque != 0
                && (System.nanoTime() - instanteUltimoAtaque) / 1e9 < DURACAO_POSE_ATAQUE;
    }

    public boolean estaNoChao() {
        return fisica == null || fisica.isOnGround();
    }

    public double getVelocidadeX() {
        return fisica == null ? 0 : fisica.getVelocityX();
    }

    public double getVelocidadeY() {
        return fisica == null ? 0 : fisica.getVelocityY();
    }

    //HABILIDADES

    //Tecla X (ver controle.ControleEntrada). Quem confere cooldown e energia é
    //o usar() da Habilidade; aqui só barra o hit-stun.
    public boolean usarGolpeDeEnergia() {
        if (estaAtordoado()) {
            return false;
        }
        boolean usou = golpe.usar(this);
        if (usou) {
            marcarAtaque();
        }
        return usou;
    }

    //Tecla C (ver controle.ControleEntrada).
    public boolean usarApara() {
        if (estaAtordoado()) {
            return false;
        }
        return apara.usar(this);
    }

    //Chamado pelo executar() da Apara.
    public void iniciarApara(double duracao) {
        timerApara = duracao;
        escudo.setVisible(true);
    }

    public boolean estaAparando() {
        return timerApara > 0;
    }

    public GolpeDeEnergia getGolpe() {
        return golpe;
    }

    public Apara getApara() {
        return apara;
    }

    //INVENTÁRIO

    //Chamado quando o jogador encosta num item do mapa (ver
    //controle.ControleJogo.registrarColisoes()).
    public boolean coletarItem(Inventariavel item) {
        return inventario.adicionar(item);
    }

    //Sobrecarga: vários itens de uma vez (o arsenal inicial, por exemplo)
    public int coletarItem(List<? extends Inventariavel> itens) {
        return inventario.adicionar(itens);
    }

    //Usa um item pelo id (teclas 1 e 2, ver controle.ControleEntrada).
    //Só consumível tem efeito; depois de usar, sai do inventário.
    //Antes devolvia false nos dois casos de erro; agora cada um tem a
    //sua exceção, e o controle mostra o motivo no HUD.
    public void usarItem(String id) throws ItemNaoEncontradoException, ItemIncompativelException {
        Inventariavel item = inventario.buscar(id);
        if (item == null) {
            throw new ItemNaoEncontradoException(id);
        }
        if (!(item instanceof Consumivel consumivel)) {
            throw new ItemIncompativelException(id, "consumir");
        }
        consumivel.efeito(this);
        inventario.remover(id);
    }

    public Inventario getInventario() {
        return inventario;
    }

    @Override
    public String toString() {
        return "Jogador{" +
                "nome='" + nome + '\'' +
                "vida=" + vidaAtual.get() + "/" + vidaMaxima +
                ", nivel=" + nivel +
                ", xp=" + xp + "/" + xpProximoNivel +
                ", energia=" + energiaAtual.get() + "/" + energiaMaxima +
                '}';
    }
}
