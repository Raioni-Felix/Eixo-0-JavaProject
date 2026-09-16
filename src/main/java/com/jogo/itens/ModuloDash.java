package com.jogo.itens;

import com.jogo.interfaces.IHabilidade;
import com.jogo.componentes.PlayerComponent;

public class ModuloDash implements IHabilidade {

    @Override
    public String getNome() {
        return "Módulo Dash";
    }

    @Override
    public String getDescricao() {
        return "Propulsor de evasão horizontal de alta velocidade.";
    }

    @Override
    public void equipar(PlayerComponent player) {
        // Reservado para passivas futuras (ex: mudar cor do sprite)
    }

    @Override
    public void desequipar(PlayerComponent player) {
        // Reservado para remoção de passivas
    }

    @Override
    public void usar(PlayerComponent player) {
        // Delega a execução física para a mecânica residente no player
        player.dash();
    }
}