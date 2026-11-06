package com.jogo.habilidades;

import javafx.scene.paint.Color;

//Segundo golpe de energia (tecla V). Mesmo executar() do GolpeDeEnergia
//(disparo reto); muda a cor, o sprite e o efeito: quem for atingido
//fica congelado (parado, sem atacar e sem dano de contato) por alguns
//segundos. Liberada no nível 2.
public class RaioDeGelo extends GolpeDeEnergia {

    private final double segundosCongelado;

    public RaioDeGelo(boolean aprendida, double tempoEspera, int custoEnergia, int dano, double segundosCongelado) {
        super(aprendida, tempoEspera, custoEnergia, 0.0, dano);
        if (segundosCongelado <= 0) {
            throw new IllegalArgumentException("Congelamento precisa durar algum tempo");
        }
        this.segundosCongelado = segundosCongelado;
    }

    @Override
    public Color getCor() {
        return Color.LIGHTCYAN;
    }

    @Override
    public String getSprite() {
        return "gelo";
    }

    @Override
    public double getCongelamento() {
        return segundosCongelado;
    }
}
