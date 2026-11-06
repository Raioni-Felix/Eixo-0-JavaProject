package com.jogo.habilidades;

import javafx.scene.paint.Color;

//Um golpe de energia concreto. Herda o dano e o executar() do
//GolpeDeEnergia; só muda a cor e o sprite do disparo. Um efeito extra (ex:
//queimadura) entraria aqui, sobrescrevendo o executar() e chamando
//super.executar().
public class BolaDeFogo extends GolpeDeEnergia {

    public BolaDeFogo(boolean aprendida, double tempoEspera, int custoEnergia, double duracao, int dano) {
        super(aprendida, tempoEspera, custoEnergia, duracao, dano);
    }

    @Override
    public Color getCor() {
        return Color.ORANGERED;
    }

    @Override
    public String getSprite() {
        return "bola-de-fogo";
    }
}
