package com.jogo.inventario;

public enum Categoria {
    ARMA(false),
    CONSUMIVEL(true),
    CHAVE(true),
    UPGRADE(false);

    private final boolean empilhavel;

    Categoria(boolean empilhavel) {
        this.empilhavel = empilhavel;
    }

    public boolean isEmpilhavel() {
        return empilhavel;
    }


}
