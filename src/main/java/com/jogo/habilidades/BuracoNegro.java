package com.jogo.habilidades;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.jogo.componentes.ComponenteJogador;

import static com.almasb.fxgl.dsl.FXGL.spawn;

//Magia final (tecla B), liberada no nível 3. Gasta TODA a energia
//(só dá pra usar com a barra cheia): abre um buraco negro na frente da
//jogadora que puxa todos os inimigos da sala e, ao fechar, destrói todos
//eles (ver ComponenteBuracoNegro). Cooldown longo.
public class BuracoNegro extends Habilidade {

    //Distância do centro da jogadora até onde o buraco abre
    private static final double DISTANCIA = 140;

    public BuracoNegro(boolean aprendida, double tempoEspera, double duracao) {
        super(aprendida, tempoEspera, 0, duracao);
    }

    //Sobrescreve o custo fixo: custa a energia máxima inteira
    @Override
    public int custoPara(ComponenteJogador jogador) {
        return jogador.getEnergiaMaxima();
    }

    @Override
    protected void executar(ComponenteJogador usuario) {
        Entity corpo = usuario.getEntity();
        double direcao = usuario.estaOlhandoDireita() ? 1 : -1;
        spawn("buraco_negro", new SpawnData(
                corpo.getCenter().getX() + direcao * DISTANCIA,
                corpo.getCenter().getY() - 40)
                .put("duracao", getDuracao()));
    }
}
