package com.jogo.inventario;
import com.jogo.componentes.ComponenteJogador;

public interface Consumivel{
    public void efeito(ComponenteJogador jogador);
    // a ideia é que antes de tudo o efeito do consumivel passe
    //pela interface e depois diretamente para o personagem
    // - Raioni

}