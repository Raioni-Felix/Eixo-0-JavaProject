package com.jogo.interfaces;
import com.jogo.componentes.PlayerComponent;

public interface IConsumivel {
    String getNome();

    //returna true se o item foi gasto com sucesso
    boolean consumir(PlayerComponent player);
}
