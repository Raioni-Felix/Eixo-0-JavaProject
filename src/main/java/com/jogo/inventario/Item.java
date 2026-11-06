package com.jogo.inventario;

//Classe Pai para consumíveis, upgrades e chaves.
public abstract class Item implements Inventariavel {

    private static int contadorInstancias = 0;

    private final String id;
    private final String nome;
    private final String descricao;
    private final Categoria categoria;

    public Item(String idBase, String nome, String descricao, Categoria categoria) {
        this.categoria = categoria;
        this.nome = nome;
        this.descricao = descricao;

        if (categoria.isEmpilhavel()) {
            this.id = idBase;
        } else {
            this.id = idBase + "-" + (++contadorInstancias);
        }
    }


    @Override 
    public String getId() { 
        return id; 
    }
    @Override 
    public String getNome() { 
        return nome;
    }
    @Override 
    public String getDescricao() { 
        return descricao; 
    }

    @Override
    public Categoria getCategoria() {
        return categoria;
    }

}