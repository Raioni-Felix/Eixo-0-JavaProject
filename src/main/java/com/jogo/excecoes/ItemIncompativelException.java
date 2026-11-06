package com.jogo.excecoes;

//Lançada quando o item existe, mas não serve pra ação pedida (ex:
//equipar um módulo como se fosse arma, ou "usar" uma arma como se
//fosse consumível).
public class ItemIncompativelException extends JogoException {

    public ItemIncompativelException(String idItem, String acao) {
        super("O item '" + idItem + "' não pode ser usado para " + acao);
    }
}
