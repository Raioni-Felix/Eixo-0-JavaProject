package com.jogo.itens;

import javafx.scene.paint.Color;

//Trazida do barebones: arma de precisão, um disparo de dano alto que
//atravessa inimigos (Perfurante). Não precisa reescrever o ataque:
//só troca os ganchos da ArmaDeFogo (quantos alvos, velocidade, cor).
public class RifleSniper extends ArmaDeFogo implements Perfurante {
    private int alvosAtravessados;

    public RifleSniper(String idBase, String nome, String descricao, int dano, double cooldown,
                       double alcance, int alvosAtravessados) {
        super(idBase, nome, descricao, dano, cooldown, alcance);
        this.alvosAtravessados = alvosAtravessados;
    }

    @Override
    public int getAlvosAtravessados() {
        return alvosAtravessados;
    }

    //É aqui que o Perfurante vira comportamento: o projétil do rifle
    //só some depois de acertar alvosAtravessados inimigos
    @Override
    protected int getAlvosPorDisparo() {
        return getAlvosAtravessados();
    }

    @Override
    protected double getVelocidadeProjetil() {
        return 1100;
    }

    @Override
    protected Color getCorProjetil() {
        return Color.WHITE;
    }

    @Override
    public String getDescricaoCombate() {
        return "tiro de " + getDanoTotal() + " que atravessa " + alvosAtravessados;
    }
}
