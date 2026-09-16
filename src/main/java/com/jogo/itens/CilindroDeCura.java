package com.jogo.itens;

import com.jogo.interfaces.IConsumivel;
import com.jogo.componentes.PlayerComponent;

public class CilindroDeCura implements IConsumivel {
    private final int quantidadeCura = 50;

    @Override
    public String getNome() {
        return "Cilindro de Cura";
    }

    @Override
    public boolean consumir(PlayerComponent player) {
        if (player.getCurrentHealth() < player.getMaxHealth()) {
            player.heal(quantidadeCura);
            return true; // Retorna true para o InventarioComponent deletar o item da lista
        }
        return false; // Inventário cheio, o item não é gasto
    }
} 