package com.jogo.inventario;
import com.jogo.componentes.ComponenteJogador;

public class ModuloEnergia extends Item implements Consumivel{
    public static final String ID = "modulo_energia";

    private int quantidadeEnergia;
    public ModuloEnergia(String id,String nome,String descricao,Categoria categoria,int quantidadeEnergia){
        super(id,nome,descricao,categoria);
        this.quantidadeEnergia = quantidadeEnergia;

    }

    //Sobrecarga do construtor (mesma ideia do ModuloCura)
    public ModuloEnergia(int quantidadeEnergia) {
        this(ID, "Módulo de Energia", "Recupera " + quantidadeEnergia + " de energia", Categoria.CONSUMIVEL, quantidadeEnergia);
    }
    //Mesma ideia do ModuloCura: o limite da energia máxima fica
    //no recuperarEnergia() do ComponenteJogador
    @Override
    public void efeito(ComponenteJogador jogador){
        jogador.recuperarEnergia(this.quantidadeEnergia);
    }
}
