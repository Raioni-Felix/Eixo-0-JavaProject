package com.jogo.interfaces;

import com.jogo.componentes.PlayerComponent;

public interface IHabilidade {
    String getNome();
    String getDescricao();
    void equipar(PlayerComponent player);
    void desequipar(PlayerComponent player);
    void usar(PlayerComponent player);
}