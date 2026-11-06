package com.jogo.habilidades;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.jogo.componentes.ComponenteJogador;
import javafx.scene.paint.Color;

import static com.almasb.fxgl.dsl.FXGL.spawn;

//Trazida do barebones: mãe de toda habilidade que causa dano.
//Abstrata: não existe "golpe de energia genérico", só golpes
//concretos (BolaDeFogo e os que vierem). O executar() já fica pronto
//aqui: cria um disparo que anda reto na direção em que o jogador olha
//(ver FabricaEntidades "disparo_jogador" e ComponenteDisparoJogador,
//os mesmos das armas de fogo).
public abstract class GolpeDeEnergia extends Habilidade {
    private static final double VELOCIDADE_DISPARO = 600;
    private static final double TEMPO_VIDA_DISPARO = 1.5;
    private static final double TAMANHO_DISPARO = 16;

    private int dano;

    public GolpeDeEnergia(boolean aprendida, double tempoEspera, int custoEnergia, double duracao, int dano) {
        super(aprendida, tempoEspera, custoEnergia, duracao);
        this.dano = dano;
    }

    public int getDano() {
        return dano;
    }

    //Cor do disparo. As filhas podem sobrescrever (polimorfismo: a
    //BolaDeFogo fica laranja sem mexer no executar()).
    public Color getCor() {
        return Color.DEEPSKYBLUE;
    }

    @Override
    protected void executar(ComponenteJogador usuario) {
        Entity corpo = usuario.getEntity();
        double direcaoX = usuario.estaOlhandoDireita() ? 1 : -1;

        //Nasce colado na frente do jogador, na altura do meio dele.
        double x = usuario.estaOlhandoDireita() ? corpo.getRightX() : corpo.getX() - TAMANHO_DISPARO;
        double y = corpo.getY() + corpo.getHeight() / 2 - TAMANHO_DISPARO / 2;

        spawn("disparo_jogador", new SpawnData(x, y)
                .put("dano", dano)
                .put("direcaoX", direcaoX)
                .put("direcaoY", 0.0)
                .put("velocidade", VELOCIDADE_DISPARO)
                .put("tempoVida", TEMPO_VIDA_DISPARO)
                .put("alvosMaximos", 1)
                .put("tamanho", TAMANHO_DISPARO)
                .put("cor", getCor()));
    }
}
