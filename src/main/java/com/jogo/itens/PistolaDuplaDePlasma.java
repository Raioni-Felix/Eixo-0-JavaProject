package com.jogo.itens;

import com.almasb.fxgl.entity.Entity;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.runOnce;

//Trazida do barebones: dispara duas vezes por ataque. No barebones era
//"dispararProjetil() + dispararProjetil()"; aqui o segundo tiro sai um
//instante depois do primeiro, pra dar pra ver os dois.
public class PistolaDuplaDePlasma extends ArmaDeFogo {
    private static final double INTERVALO_SEGUNDO_TIRO = 0.12;

    public PistolaDuplaDePlasma(String idBase, String nome, String descricao, int danoPorTiro,
                                double cooldown, double alcance) {
        super(idBase, nome, descricao, danoPorTiro, cooldown, alcance);
    }

    @Override
    protected void executarAtaque(Entity portador, double direcaoX) {
        //Primeiro tiro: o tiro normal da ArmaDeFogo
        super.executarAtaque(portador, direcaoX);

        //Segundo tiro, um pouco depois. runOnce é o timer do FXGL (para
        //junto com o jogo na pausa). O super também funciona dentro da
        //lambda: continua sendo o executarAtaque() da ArmaDeFogo.
        runOnce(() -> {
            if (portador.isActive()) {
                super.executarAtaque(portador, direcaoX);
            }
        }, Duration.seconds(INTERVALO_SEGUNDO_TIRO));
    }

    @Override
    protected Color getCorProjetil() {
        return Color.MAGENTA;
    }

    @Override
    public String getDescricaoCombate() {
        return "2 tiros de " + getDanoTotal();
    }
}
