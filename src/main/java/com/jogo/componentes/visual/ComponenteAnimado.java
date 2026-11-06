package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Componente de animação (visão): mostra o sprite certo pro estado do
 * personagem.
 *
 * Separado dos componentes de gameplay de propósito: ele nunca mexe em
 * vida, dano ou XP, só LÊ o modelo. Quem cria (FabricaEntidades) passa:
 * - as animações de cada estado (quadros carregados por Sprites);
 * - de onde tirar o estado atual e o lado pra onde o personagem olha
 *   (lambdas que perguntam ao ComponenteJogador/ComponenteInimigo).
 *
 * O quadro do sprite costuma ser maior que a caixa de colisão (tem sobra
 * transparente em volta). centroX e peY dizem onde o personagem está
 * dentro do quadro (em pixels do arquivo), pra ele ficar centrado na
 * caixa e com o pé no chão. escala é quanto os quadros foram ampliados
 * ao carregar (ver Sprites). Olhar pra esquerda é o mesmo desenho
 * espelhado (scaleX = -1).
 */
public class ComponenteAnimado extends Component {

    public enum EstadoAnimacao {
        PARADO,
        ANDANDO,
        PULANDO,
        CAINDO,
        ATACANDO,
        ATACANDO_ANDANDO,
        MACHUCADO
    }

    //Estes tocam uma vez e param no último quadro; os outros repetem
    private static final Set<EstadoAnimacao> TOCAM_UMA_VEZ =
            EnumSet.of(EstadoAnimacao.PULANDO, EstadoAnimacao.ATACANDO, EstadoAnimacao.MACHUCADO);

    //Velocidade horizontal a partir da qual o personagem está "andando"
    private static final double VELOCIDADE_MINIMA_ANDANDO = 10;

    private final Map<EstadoAnimacao, AnimationChannel> animacoes;
    private final double larguraQuadro;
    private final double escala;
    private final double centroX;
    private final double peY;
    private final Supplier<EstadoAnimacao> estado;
    private final BooleanSupplier olhandoDireita;

    private AnimatedTexture textura;
    private EstadoAnimacao estadoAtual = EstadoAnimacao.PARADO;

    public ComponenteAnimado(Map<EstadoAnimacao, AnimationChannel> animacoes, double larguraQuadro,
            double centroX, double peY, double escala,
            Supplier<EstadoAnimacao> estado, BooleanSupplier olhandoDireita) {
        if (!animacoes.containsKey(EstadoAnimacao.PARADO)) {
            throw new IllegalArgumentException("Todo personagem precisa da animação PARADO");
        }
        if (escala <= 0) {
            throw new IllegalArgumentException("Escala precisa ser positiva");
        }
        this.animacoes = new EnumMap<>(animacoes);
        this.larguraQuadro = larguraQuadro;
        this.escala = escala;
        this.centroX = centroX;
        this.peY = peY;
        this.estado = estado;
        this.olhandoDireita = olhandoDireita;
    }

    @Override
    public void onAdded() {
        textura = new AnimatedTexture(animacoes.get(EstadoAnimacao.PARADO));
        textura.loop();
        entity.getViewComponent().addChild(textura);
        posicionar();
    }

    @Override
    public void onUpdate(double tpf) {
        EstadoAnimacao novo = animacaoDisponivel(estado.get(), animacoes.keySet());
        if (novo != estadoAtual) {
            estadoAtual = novo;
            if (TOCAM_UMA_VEZ.contains(novo)) {
                textura.playAnimationChannel(animacoes.get(novo));
            } else {
                textura.loopAnimationChannel(animacoes.get(novo));
            }
        }
        posicionar();
    }

    public EstadoAnimacao getEstado() {
        return estadoAtual;
    }

    //Centraliza o personagem na caixa de colisão e espelha se olhar pra
    //esquerda (espelhado, o centro dele vira largura - centroX)
    private void posicionar() {
        boolean direita = olhandoDireita.getAsBoolean();
        double largura = larguraQuadro * escala;
        double cx = centroX * escala;
        textura.setScaleX(direita ? 1 : -1);
        textura.setTranslateX(entity.getWidth() / 2 - (direita ? cx : largura - cx));
        textura.setTranslateY(entity.getHeight() - peY * escala);
    }

    //Regra de prioridade, igual pra todos os personagens: dano vence
    //ataque, ataque vence pulo, pulo vence andar. Estática e sem FXGL,
    //então dá pra testar sozinha.
    public static EstadoAnimacao escolherEstado(boolean machucado, boolean atacando, boolean noChao,
            double velocidadeX, double velocidadeY) {
        boolean andando = Math.abs(velocidadeX) > VELOCIDADE_MINIMA_ANDANDO;
        if (machucado) {
            return EstadoAnimacao.MACHUCADO;
        }
        if (atacando) {
            return andando && noChao ? EstadoAnimacao.ATACANDO_ANDANDO : EstadoAnimacao.ATACANDO;
        }
        if (!noChao) {
            //y cresce pra baixo: negativo = subindo
            return velocidadeY < 0 ? EstadoAnimacao.PULANDO : EstadoAnimacao.CAINDO;
        }
        return andando ? EstadoAnimacao.ANDANDO : EstadoAnimacao.PARADO;
    }

    //Se o personagem não tem a animação pedida, usa a mais parecida
    //(ex.: o drone só tem voar e dano)
    public static EstadoAnimacao animacaoDisponivel(EstadoAnimacao pedido, Set<EstadoAnimacao> existentes) {
        if (existentes.contains(pedido)) {
            return pedido;
        }
        if (pedido == EstadoAnimacao.ATACANDO_ANDANDO) {
            return animacaoDisponivel(EstadoAnimacao.ATACANDO, existentes);
        }
        if (pedido == EstadoAnimacao.CAINDO) {
            return animacaoDisponivel(EstadoAnimacao.PULANDO, existentes);
        }
        if (pedido == EstadoAnimacao.PULANDO && existentes.contains(EstadoAnimacao.ANDANDO)) {
            return EstadoAnimacao.ANDANDO;
        }
        return EstadoAnimacao.PARADO;
    }
}
