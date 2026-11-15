package com.jogo.npcs;

import com.almasb.fxgl.entity.Entity;
import com.jogo.componentes.ComponentePersonagem;
import com.jogo.componentes.RelogioJogo;
import com.jogo.entidades.TipoEntidade;
import javafx.scene.control.Label;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;

/**
 * Classe base de todo NPC amigável (não ataca, não morre, só conversa).
 *
 * Herda de ComponentePersonagem (nome, vida, velocidade), igual os
 * inimigos. Como a base exige vida > 0, o NPC tem vida 1 e sobrescreve
 * tomarDano() pra ignorar qualquer dano: é invulnerável.
 *
 * Como funciona a conversa (sem tecla, sem mexer no HUD):
 *  - o jogador chega perto (ALCANCE_CONVERSA): a conversa começa e
 *    aparece um balão em cima do NPC com a primeira fala;
 *  - a cada SEGUNDOS_POR_FALA a fala passa pra próxima (volta pra
 *    primeira depois da última);
 *  - o jogador se afasta: a conversa termina e o balão some.
 *
 * Abstrata: cada tipo de NPC decide o que faz enquanto conversa com o
 * jogador (aoConversar). O Informante só fala; o Curandeiro cura. Mesma
 * ideia do iniciarAtaque() dos inimigos: a base chama, a filha escolhe
 * o efeito (polimorfismo).
 */
public abstract class ComponenteNpc extends ComponentePersonagem {

    public static final double ALCANCE_CONVERSA = 140;
    public static final double SEGUNDOS_POR_FALA = 3.0;
    public static final double SEGUNDOS_AVISO = 2.5;

    private final List<String> falas;
    private int indiceFala = 0;
    private double tempoNaFala = 0;
    private boolean conversando = false;

    //Aviso temporário (ex.: "Pronto! Sua vida foi restaurada."): aparece no
    //balão no lugar da fala por SEGUNDOS_AVISO.
    private String aviso = "";
    private double tempoAviso = 0;

    //Balão de fala (só visual). Criado no onAdded(), já que precisa da entidade.
    private Label balao;
    private String textoExibido = "";

    protected ComponenteNpc(String nome, List<String> falas) {
        //vida 1 e velocidade 0: NPC fica parado e não pode morrer (ver tomarDano)
        super(nome, 1, 0);
        if (falas == null || falas.isEmpty()) {
            throw new IllegalArgumentException("NPC precisa de pelo menos uma fala");
        }
        for (String fala : falas) {
            if (fala == null || fala.isBlank()) {
                throw new IllegalArgumentException("NPC não pode ter fala vazia");
            }
        }
        this.falas = List.copyOf(falas);
    }

    //O que o NPC faz enquanto o jogador está perto. Chamado a CADA frame da
    //conversa (inclusive no primeiro), então o NPC reage ao estado atual do
    //jogador, e não só ao momento em que ele chegou.
    protected abstract void aoConversar(ComponentePersonagem jogador);

    //Invulnerável: tiro, espada e Buraco Negro não fazem nada.
    @Override
    public void tomarDano(int quant) {
        //de propósito sem efeito
    }

    //=== Lógica da conversa (não depende do motor do FXGL, dá pra testar) ===

    public String getFalaAtual() {
        return falas.get(indiceFala);
    }

    //Passa pra próxima fala; depois da última volta pra primeira.
    public String avancarFala() {
        indiceFala = (indiceFala + 1) % falas.size();
        tempoNaFala = 0;
        return getFalaAtual();
    }

    public boolean estaConversando() {
        return conversando;
    }

    //Texto que aparece no balão: o aviso (se tiver um ativo) ou a fala atual.
    public String getTextoBalao() {
        return getNome() + ": " + (tempoAviso > 0 ? aviso : getFalaAtual());
    }

    //Usado pelas filhas pra dar um recado no balão (ex.: depois de curar).
    protected void mostrarAviso(String texto) {
        aviso = texto;
        tempoAviso = SEGUNDOS_AVISO;
    }

    public void iniciarConversa(ComponentePersonagem jogador) {
        conversando = true;
        indiceFala = 0;
        tempoNaFala = 0;
        aoConversar(jogador);
    }

    public void encerrarConversa() {
        conversando = false;
        indiceFala = 0;
        tempoNaFala = 0;
        tempoAviso = 0;
    }

    //Chamado todo frame com a situação atual: jogador perto ou não.
    public void atualizarConversa(boolean jogadorPerto, ComponentePersonagem jogador, double tpf) {
        if (!jogadorPerto) {
            encerrarConversa();
            return;
        }
        if (!conversando) {
            iniciarConversa(jogador);
            return;
        }
        aoConversar(jogador);
        tempoAviso = Math.max(0, tempoAviso - tpf);
        tempoNaFala += tpf;
        if (tempoNaFala >= SEGUNDOS_POR_FALA) {
            avancarFala();
        }
    }

    //=== Parte do motor (FXGL) ===

    @Override
    public void onAdded() {
        balao = new Label();
        balao.setWrapText(true);
        balao.setMaxWidth(240);
        balao.setStyle("-fx-background-color: rgba(0,0,0,0.78); -fx-text-fill: white;"
                + " -fx-padding: 4 8 4 8; -fx-background-radius: 6; -fx-font-size: 13px;");
        balao.setMouseTransparent(true);
        balao.setVisible(false);
        entity.getViewComponent().addChild(balao);
    }

    @Override
    public void onUpdate(double tpf) {
        //Ignora a rajada de ticks logo após despausar (ver RelogioJogo).
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }

        Entity jogador = acharJogador();
        ComponentePersonagem personagem = jogador == null ? null : ComponentePersonagem.obterDe(jogador);
        boolean perto = personagem != null && jogador.isActive()
                && entity.getCenter().distance(jogador.getCenter()) <= ALCANCE_CONVERSA;

        atualizarConversa(perto, personagem, tpf);
        atualizarBalao();
    }

    //O jogador é sempre a entidade do tipo JOGADOR (mesmo tipo usado pela FabricaEntidades).
    private Entity acharJogador() {
        List<Entity> jogadores = getGameWorld().getEntitiesByType(TipoEntidade.JOGADOR);
        return jogadores.isEmpty() ? null : jogadores.get(0);
    }

    private void atualizarBalao() {
        if (balao == null) {
            return;
        }
        balao.setVisible(conversando);
        if (!conversando) {
            return;
        }
        String texto = getTextoBalao();
        if (!texto.equals(textoExibido)) {
            balao.setText(texto);
            textoExibido = texto;
        }
        //Centralizado em cima do NPC (a largura/altura do balão só existem depois do layout)
        balao.setTranslateX(entity.getWidth() / 2 - balao.getWidth() / 2);
        balao.setTranslateY(-balao.getHeight() - 8);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{nome='" + nome + "'}";
    }
}
