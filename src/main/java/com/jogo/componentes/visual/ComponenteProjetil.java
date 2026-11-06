package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.jogo.componentes.ComponentePersonagem;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.RelogioJogo;
import com.jogo.entidades.TipoEntidade;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import javafx.geometry.Point2D;

/**
 * Projétil "bala de verdade": NÃO é homing. A direção é calculada UMA
 * ÚNICA VEZ, no momento em que o projétil é criado (com base em onde o
 * alvo estava naquele instante). Depois disso ele só anda em linha
 * reta nessa direção fixa, mesmo que o alvo se mova pra outro lugar.
 *
 * Isso é diferente de "não checar se acertou": a cada frame ele ainda
 * olha a distância ATUAL até o alvo pra saber se colidiu (isso é só
 * detecção de colisão, não perseguição/homing, ele não muda de rumo
 * por causa disso).
 */
public class ComponenteProjetil extends Component {

    private static final double DISTANCIA_ACERTO = 10;
    private static final double TEMPO_VIDA_MAXIMO_SEGUNDOS = 3;

    private final Entity origem;
    private final Entity alvo;
    private final int dano;
    private final double velocidade;

    //Não é mais final, calculada em onAdded() (ver comentário lá embaixo).
    private Point2D direcao;

    private double tempoDeVida = 0;

    //Raio do acerto: o tiro dos robôs mira no centro (10); os do chefe
    //vêm em leque/de cima e usam um raio maior
    private final double distanciaAcerto;
    //Tiros do chefe batem nas plataformas (dá pra se esconder embaixo)
    private final boolean paraEmPlataforma;

    public ComponenteProjetil(Entity origem, Entity alvo, int dano, double velocidade) {
        this(origem, alvo, dano, velocidade, null, DISTANCIA_ACERTO);
    }

    //Direção já decidida por quem atira (chefe: leque, chuva). null =
    //mirar no alvo, como os robôs
    public ComponenteProjetil(Entity origem, Entity alvo, int dano, double velocidade,
            Point2D direcaoFixa, double distanciaAcerto) {
        this.direcao = direcaoFixa;
        this.distanciaAcerto = distanciaAcerto;
        this.paraEmPlataforma = direcaoFixa != null;
        this.origem = origem;
        this.alvo = alvo;
        this.dano = dano;
        this.velocidade = velocidade;
    }

    //Calcula a direção aqui, não no construtor. Usar origem.getCenter()
    //(centro do atirador) como origem da mira dava um desvio
    //constante, pq o projétil nasce no canto (x,y) do atirador, não no
    //centro dele. entity.getCenter() usa a posição real de nascimento
    //do projétil, sem esse desvio.
    @Override
    public void onAdded() {
        if (direcao != null) {
            direcao = direcao.normalize();
            return;
        }
        Point2D de = entity.getCenter();
        Point2D para = alvo.getCenter();
        Point2D bruta = para.subtract(de);

        // Se por algum motivo origem e alvo estiverem no mesmo ponto
        // exato (magnitude 0), usa uma direção padrão pra não dividir
        // por zero no normalize().
        direcao = bruta.magnitude() > 0 ? bruta.normalize() : new Point2D(1, 0);
    }

    @Override
    public void onUpdate(double tpf) {
        tempoDeVida += tpf;

        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }
        if (tempoDeVida >= TEMPO_VIDA_MAXIMO_SEGUNDOS) {
            entity.removeFromWorld();
            return;
        }

        // Sempre na mesma direção, isso que faz ser "bala" e não mais
        // "míssil teleguiado".
        entity.translate(direcao.multiply(velocidade * tpf));

        if (paraEmPlataforma) {
            for (Entity plataforma : getGameWorld().getEntitiesByType(TipoEntidade.PLATAFORMA)) {
                if (entity.isColliding(plataforma)) {
                    entity.removeFromWorld();
                    return;
                }
            }
        }

        // Só checa colisão se o alvo ainda existir. Se o alvo morreu no
        // meio do caminho, a bala não desaparece por causa disso, ela
        // continua reto até bater no tempoDeVida (bala não sabe que o
        // alvo morreu, ela só ia numa direção).
        if (alvo != null && alvo.isActive()
                && entity.getCenter().distance(alvo.getCenter()) <= distanciaAcerto) {
            ComponentePersonagem personagem = ComponentePersonagem.obterDe(alvo);

            //Se o alvo é o jogador, usa serAtingido(), ganha knockback e
            //piscada igual ao toque físico. Qualquer outro alvo,
            //tomarDano() normal.
            if (personagem instanceof ComponenteJogador) {
                ((ComponenteJogador) personagem).serAtingido(origem, dano);
            } else if (personagem != null) {
                personagem.tomarDano(dano);
            }

            entity.removeFromWorld();
        }
    }
}
