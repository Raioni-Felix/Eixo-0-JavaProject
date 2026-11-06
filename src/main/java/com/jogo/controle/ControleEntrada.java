package com.jogo.controle;

import com.almasb.fxgl.input.UserAction;
import com.jogo.componentes.ComponenteArma;
import com.jogo.componentes.ComponenteJogador;
import javafx.scene.input.KeyCode;

import static com.almasb.fxgl.dsl.FXGL.getInput;

//CONTROLE das teclas: liga cada tecla a uma ação do jogador ou do jogo.
//Antes era o Main.initInput(). Não sabe COMO pular, atacar ou usar
//item: só repassa pro modelo (ComponenteJogador/ComponenteArma) ou pro
//ControleJogo. Toda ação confere antes se o jogador ainda existe.
//
//O dash (Shift) não está aqui: o FXGL proíbe ligar ação direto em
//tecla modificadora (Shift/Ctrl/Alt) e derruba o jogo com
//"Cannot bind to illegal key: SHIFT". Ele é tratado por um
//TriggerListener em ComponenteJogador.onAdded().
public class ControleEntrada {

    private final ControleJogo jogo;

    public ControleEntrada(ControleJogo jogo) {
        this.jogo = jogo;
    }

    //Chamado uma vez só (o FXGL roda initInput() só no início do app).
    //Por isso cada ação busca o jogador na hora: ele é recriado a cada
    //jogo novo (Reiniciar/Menu)
    public void registrar() {
        //ESC: menu de pausa (nosso, no lugar do menu padrão do FXGL)
        acaoAoApertar("Pausar", KeyCode.ESCAPE, () -> {
            if (jogo.podePausar()) {
                jogo.abrirMenuPausa();
            }
        });

        //Pulo: Espaço ou seta pra cima
        acaoDoJogador("Pular", KeyCode.SPACE, ComponenteJogador::pular);
        acaoDoJogador("Pular (seta)", KeyCode.UP, ComponenteJogador::pular);

        //Movimento: onAction roda todo frame segurado; ao soltar, para
        //(senão deslizaria pra sempre)
        movimento("Mover Esquerda", KeyCode.LEFT, ComponenteJogador::moverEsquerda);
        movimento("Mover Direita", KeyCode.RIGHT, ComponenteJogador::moverDireita);

        //Combate
        acaoDoJogador("Atacar", KeyCode.Z, ComponenteJogador::iniciarAtaque);
        acaoAoApertar("Trocar Arma", KeyCode.Q, () -> {
            if (jogo.getJogador().isActive()) {
                jogo.getJogador().getComponent(ComponenteArma.class).trocarArma();
            }
        });
        acaoDoJogador("Golpe de Energia", KeyCode.X, ComponenteJogador::usarGolpeDeEnergia);
        acaoDoJogador("Apara", KeyCode.C, ComponenteJogador::usarApara);
        acaoDoJogador("Raio de Gelo", KeyCode.V, ComponenteJogador::usarGelo);
        acaoDoJogador("Buraco Negro", KeyCode.B, ComponenteJogador::usarBuracoNegro);

        //Inventário (I ou TAB) e tela cheia (F11)
        acaoAoApertar("Inventário", KeyCode.I, jogo::alternarInventario);
        acaoAoApertar("Inventário (TAB)", KeyCode.TAB, jogo::alternarInventario);
        acaoAoApertar("Tela cheia", KeyCode.F11, jogo::trocarTelaCheia);

        //Consumíveis: 1 = Módulo de Cura, 2 = Módulo de Energia
        acaoAoApertar("Usar Módulo de Cura", KeyCode.DIGIT1, () -> usarModulo(ControleJogo.ID_MODULO_CURA));
        acaoAoApertar("Usar Módulo de Energia", KeyCode.DIGIT2, () -> usarModulo(ControleJogo.ID_MODULO_ENERGIA));
    }

    private void usarModulo(String id) {
        if (jogo.getJogador().isActive()) {
            jogo.usarModulo(id);
        }
    }

    //Ação de um toque (onActionBegin dispara uma vez por aperto)
    private void acaoAoApertar(String nome, KeyCode tecla, Runnable acao) {
        getInput().addAction(new UserAction(nome) {
            @Override
            protected void onActionBegin() {
                acao.run();
            }
        }, tecla);
    }

    //Atalho pra ação de um toque que só chama um método do jogador
    private void acaoDoJogador(String nome, KeyCode tecla, java.util.function.Consumer<ComponenteJogador> acao) {
        acaoAoApertar(nome, tecla, () -> {
            if (jogo.getJogador().isActive()) {
                acao.accept(jogo.getComponenteJogador());
            }
        });
    }

    //Tecla segurada: repete a ação todo frame e para o jogador ao soltar
    private void movimento(String nome, KeyCode tecla, java.util.function.Consumer<ComponenteJogador> mover) {
        getInput().addAction(new UserAction(nome) {
            @Override
            protected void onAction() {
                if (jogo.getJogador().isActive()) {
                    mover.accept(jogo.getComponenteJogador());
                }
            }

            @Override
            protected void onActionEnd() {
                if (jogo.getJogador().isActive()) {
                    jogo.getComponenteJogador().pararHorizontal();
                }
            }
        }, tecla);
    }
}
