package com.jogo.itens;

import com.almasb.fxgl.entity.Entity;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

//Trazida do barebones: vários balins por disparo, abertos em leque.
//O dano é por balim, então de perto (todos acertam) bate forte, e de
//longe os balins se espalham e erram.
public class Shotgun extends ArmaDeFogo {
    private static final double ABERTURA_GRAUS = 9; //ângulo entre um balim e o próximo

    private int quantidadeBalins;

    public Shotgun(String idBase, String nome, String descricao, int danoPorBalim, int quantidadeBalins,
                   double cooldown, double alcance) {
        super(idBase, nome, descricao, danoPorBalim, cooldown, alcance);
        this.quantidadeBalins = quantidadeBalins;
    }

    @Override
    protected void executarAtaque(Entity portador, double direcaoX) {
        //O balim do meio é o tiro normal da ArmaDeFogo: reaproveita com super
        super.executarAtaque(portador, direcaoX);

        //Os outros saem em pares, um pra cima e um pra baixo, abrindo o leque
        int pares = (quantidadeBalins - 1) / 2;
        for (int i = 1; i <= pares; i++) {
            dispararProjetil(portador, direcaoX, -ABERTURA_GRAUS * i);
            dispararProjetil(portador, direcaoX, ABERTURA_GRAUS * i);
        }
    }

    @Override
    protected double getTamanhoProjetil() {
        return 6;
    }

    @Override
    public Node criarVisual() {
        return new Rectangle(24, 8, Color.SADDLEBROWN);
    }

    @Override
    public String getDescricaoCombate() {
        return quantidadeBalins + " balins de " + getDanoTotal();
    }
}
