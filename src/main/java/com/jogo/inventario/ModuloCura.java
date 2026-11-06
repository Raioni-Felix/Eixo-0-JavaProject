package com.jogo.inventario;
import com.jogo.componentes.ComponenteJogador;

public class ModuloCura extends Item implements Consumivel {
    //Id fixo: todo módulo de cura empilha no mesmo espaço do inventário
    public static final String ID = "modulo_cura";

    private int quantidadeDeCura;

    public ModuloCura(String id,String nome,String descricao,Categoria categoria,int quantidadeDeCura){
        super(id,nome,descricao,categoria);
        this.quantidadeDeCura=quantidadeDeCura;

    }

    //Sobrecarga do construtor: o módulo "padrão" que aparece no mapa,
    //só muda quanto cura. Evita repetir id/nome/categoria em todo lugar
    public ModuloCura(int quantidadeDeCura) {
        this(ID, "Módulo de Cura", "Recupera " + quantidadeDeCura + " de vida", Categoria.CONSUMIVEL, quantidadeDeCura);
    }
    //O módulo só diz quanto cura; o limite da vida máxima e o
    //"não cura morto" ficam no curar() do ComponentePersonagem
    @Override
    public void efeito(ComponenteJogador jogador){
        jogador.curar(this.quantidadeDeCura);
    }
}
