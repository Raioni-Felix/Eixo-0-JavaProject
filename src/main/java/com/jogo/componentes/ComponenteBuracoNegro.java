package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.jogo.componentes.TimeScaleManager;
import com.jogo.entidades.TipoEntidade;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;

//Comportamento do buraco negro (habilidades.BuracoNegro): enquanto dura,
//mantém todos os inimigos da sala congelados e puxa cada um pro centro;
//quando o tempo acaba, cada um toma danoDeBuracoNegro() (a vida inteira:
//morrem, explodem e dão o XP normal; o chefe só perde 25%) e o buraco some.
public class ComponenteBuracoNegro extends Component {

    private static final TipoEntidade[] TIPOS_INIMIGO = {
            TipoEntidade.INIMIGO_VOADOR, TipoEntidade.INIMIGO_DISTANCIA, TipoEntidade.INIMIGO_MELEE,
            TipoEntidade.CHEFE
    };
    private static final double FORCA_PUXAO = 260;

    private final double duracao;
    private double tempo = 0;

    public ComponenteBuracoNegro(double duracao) {
        if (duracao <= 0) {
            throw new IllegalArgumentException("Duração precisa ser positiva");
        }
        this.duracao = duracao;
    }

    @Override
    public void onUpdate(double tpf) {
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }
        
        // Aplica time scale do Sandevistan
        double tpfEfetivo = TimeScaleManager.getTpfPara(tpf, entity);
        
        tempo += tpfEfetivo;
        entity.getViewComponent().getParent().setRotate(tempo * -240);

        List<ComponenteInimigo> inimigos = inimigosDaSala();
        if (tempo >= duracao) {
            for (ComponenteInimigo inimigo : inimigos) {
                inimigo.tomarDano(inimigo.danoDeBuracoNegro());
            }
            entity.removeFromWorld();
            return;
        }

        //Puxa: congelado o inimigo não anda sozinho, então a velocidade
        //que damos aqui é a única que vale
        Point2D centro = entity.getPosition();
        for (ComponenteInimigo inimigo : inimigos) {
            inimigo.congelar(duracao - tempo + 0.1);
            inimigo.puxarPara(centro, FORCA_PUXAO);
        }
    }

    private static List<ComponenteInimigo> inimigosDaSala() {
        List<ComponenteInimigo> lista = new ArrayList<>();
        for (TipoEntidade tipo : TIPOS_INIMIGO) {
            for (Entity e : getGameWorld().getEntitiesByType(tipo)) {
                ComponenteInimigo inimigo = ComponenteInimigo.obterDe(e);
                if (e.isActive() && inimigo != null && !inimigo.estaMorto()) {
                    lista.add(inimigo);
                }
            }
        }
        return lista;
    }

    public double getDuracao() {
        return duracao;
    }
}
