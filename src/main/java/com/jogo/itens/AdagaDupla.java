package com.jogo.itens;

import com.almasb.fxgl.entity.Entity;

//Adagas duplas: corpo a corpo como a Espada, mas de alcance curto e
//rápidas, e cada ataque são dois cortes (uma lâmina em cada mão).
//Reaproveita o golpe da mãe chamando super.executarAtaque() duas vezes.
//Também existe no barebones (lá com o combo no 5º golpe).
public class AdagaDupla extends Espada {

    public AdagaDupla(String idBase, String nome, String descricao, int dano, double cooldown, double alcance) {
        super(idBase, nome, descricao, dano, cooldown, alcance);
    }

    @Override
    protected void executarAtaque(Entity portador, double direcaoX) {
        super.executarAtaque(portador, direcaoX);
        super.executarAtaque(portador, direcaoX);
    }

    @Override
    public String getEmpunhadura() {
        return "adagas";
    }

    @Override
    public String getDescricaoCombate() {
        return "2 cortes de " + getDanoTotal();
    }
}
