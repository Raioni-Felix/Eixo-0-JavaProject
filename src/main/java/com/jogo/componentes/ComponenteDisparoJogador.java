package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.jogo.entidades.TipoEntidade;

import java.util.HashSet;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.spawn;

//Projétil do jogador: tiro das armas de fogo (ArmaDeFogo e filhas) e
//disparo do GolpeDeEnergia. Diferente do ComponenteProjetil dos
//inimigos, não precisa de alvo: anda reto na direção que recebeu.
//
//alvosMaximos é o que faz o RifleSniper atravessar (Perfurante): o
//projétil só some depois de acertar essa quantidade de inimigos. Tiro
//comum usa 1. Também some ao bater numa plataforma/parede ou quando o
//tempo de vida acaba (é assim que o alcance da arma vira distância).
public class ComponenteDisparoJogador extends Component {

    private static final TipoEntidade[] TIPOS_INIMIGO = {
            TipoEntidade.INIMIGO_VOADOR, TipoEntidade.INIMIGO_DISTANCIA, TipoEntidade.INIMIGO_MELEE,
            TipoEntidade.CHEFE
    };

    private final int dano;
    private final double direcaoX;
    private final double direcaoY;
    private final double velocidade;
    private final double tempoVidaMaximo;
    private final int alvosMaximos;
    //Segundos de congelamento no inimigo atingido (RaioDeGelo); 0 = nenhum
    private final double congelamento;
    private double tempoDeVida = 0;

    //Inimigos que este projétil já acertou. Sem isso, um tiro que
    //atravessa acertaria o mesmo inimigo em todo frame em que estivesse
    //encostado nele.
    private final Set<Entity> atingidos = new HashSet<>();

    public ComponenteDisparoJogador(int dano, double direcaoX, double direcaoY, double velocidade,
                                    double tempoVidaMaximo, int alvosMaximos) {
        this(dano, direcaoX, direcaoY, velocidade, tempoVidaMaximo, alvosMaximos, 0.0);
    }

    public ComponenteDisparoJogador(int dano, double direcaoX, double direcaoY, double velocidade,
                                    double tempoVidaMaximo, int alvosMaximos, double congelamento) {
        this.congelamento = congelamento;
        this.dano = dano;
        this.direcaoX = direcaoX;
        this.direcaoY = direcaoY;
        this.velocidade = velocidade;
        this.tempoVidaMaximo = tempoVidaMaximo;
        this.alvosMaximos = alvosMaximos;
    }

    @Override
    public void onUpdate(double tpf) {
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }

        tempoDeVida += tpf;
        if (tempoDeVida >= tempoVidaMaximo) {
            entity.removeFromWorld();
            return;
        }

        entity.translate(direcaoX * velocidade * tpf, direcaoY * velocidade * tpf);

        for (TipoEntidade tipo : TIPOS_INIMIGO) {
            for (Entity inimigo : getGameWorld().getEntitiesByType(tipo)) {
                if (!inimigo.isActive() || atingidos.contains(inimigo) || !entity.isColliding(inimigo)) {
                    continue;
                }
                ComponenteInimigo componente = ComponenteInimigo.obterDe(inimigo);
                if (componente != null) {
                    componente.tomarDano(dano);
                    if (congelamento > 0 && !componente.estaMorto()) {
                        componente.congelar(congelamento);
                    }
                }
                //Faísca no ponto do acerto (só visual, ver FabricaEntidades)
                spawn("impacto", entity.getCenter().getX(), entity.getCenter().getY());
                atingidos.add(inimigo);
                if (atingidos.size() >= alvosMaximos) {
                    entity.removeFromWorld();
                    return;
                }
            }
        }

        //Bateu em plataforma ou parede (as duas usam TipoEntidade.PLATAFORMA).
        for (Entity plataforma : getGameWorld().getEntitiesByType(TipoEntidade.PLATAFORMA)) {
            if (entity.isColliding(plataforma)) {
                entity.removeFromWorld();
                return;
            }
        }
    }
}
