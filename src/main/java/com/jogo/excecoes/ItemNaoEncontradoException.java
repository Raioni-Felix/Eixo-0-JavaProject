package com.jogo.excecoes;

//Lançada quando se pede um item pelo id e ele não está no inventário
//(ex: usar um Módulo de Cura sem ter nenhum).
public class ItemNaoEncontradoException extends JogoException {

    private final String idItem;

    public ItemNaoEncontradoException(String idItem) {
        super("Item não encontrado no inventário: " + idItem);
        this.idItem = idItem;
    }

    public String getIdItem() {
        return idItem;
    }
}
