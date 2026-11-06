package com.jogo.itens;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.spawn;

//Arma de fogo: um disparo reto na direção em que o portador olha.
//Igual no barebones, é a mãe da Shotgun, da PistolaDuplaDePlasma e do
//RifleSniper. Ela já serve sozinha (não é abstrata), e as filhas
//reaproveitam o executarAtaque() daqui com super.
public class ArmaDeFogo extends Arma {

    public ArmaDeFogo(String idBase, String nome, String descricao, int dano, double cooldown, double alcance) {
        super(idBase, nome, descricao, dano, cooldown, alcance);
    }

    //Tiro normal: um projétil reto, sem ângulo
    @Override
    protected void executarAtaque(Entity portador, double direcaoX) {
        dispararProjetil(portador, direcaoX, 0);
    }

    //Cria UM projétil ("disparo_jogador" na FabricaEntidades). O ângulo
    //(em graus) abre o tiro pra cima/baixo, a Shotgun usa isso pro
    //leque. O projétil some quando percorre o alcance da arma.
    protected void dispararProjetil(Entity portador, double direcaoX, double anguloGraus) {
        double radianos = Math.toRadians(anguloGraus);
        double dx = Math.cos(radianos) * direcaoX;
        double dy = Math.sin(radianos);

        double tamanho = getTamanhoProjetil();
        double x = direcaoX > 0 ? portador.getRightX() : portador.getX() - tamanho;
        double y = portador.getY() + portador.getHeight() / 2 - tamanho / 2;

        spawn("disparo_jogador", new SpawnData(x, y)
                .put("dano", getDanoTotal())
                .put("direcaoX", dx)
                .put("direcaoY", dy)
                .put("velocidade", getVelocidadeProjetil())
                .put("tempoVida", getAlcance() / getVelocidadeProjetil())
                .put("alvosMaximos", getAlvosPorDisparo())
                .put("tamanho", tamanho)
                .put("cor", getCorProjetil()));
    }

    //Ganchos que as filhas podem sobrescrever (polimorfismo). Cada um
    //tem o valor "normal" aqui.

    //Quantos inimigos um projétil acerta antes de sumir
    protected int getAlvosPorDisparo() {
        return 1;
    }

    protected double getVelocidadeProjetil() {
        return 650;
    }

    protected double getTamanhoProjetil() {
        return 8;
    }

    protected Color getCorProjetil() {
        return Color.GOLD;
    }

    @Override
    public Node criarVisual() {
        return new Rectangle(20, 6, Color.DIMGRAY);
    }

    @Override
    public String getDescricaoCombate() {
        return "tiro de " + getDanoTotal();
    }
}
