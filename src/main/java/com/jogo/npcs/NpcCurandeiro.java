package com.jogo.npcs;

import com.jogo.componentes.ComponentePersonagem;

import java.util.List;

/**
 * NPC que conversa e também cura o jogador (vida cheia) enquanto ele
 * estiver perto e ferido. Tem um número limitado de curas pra não virar
 * vida infinita: cada cura gasta uma, e só gasta se o jogador realmente
 * estiver ferido (de vida cheia ele não gasta nada).
 *
 * A checagem é feita a cada frame da conversa, então funciona também se o
 * jogador se ferir (ou subir de nível e ganhar vida máxima) já perto dele.
 */
public class NpcCurandeiro extends ComponenteNpc {

    private int curasRestantes;

    public NpcCurandeiro(String nome, List<String> falas, int curas) {
        super(nome, falas);
        if (curas < 0) {
            throw new IllegalArgumentException("Número de curas não pode ser negativo: " + curas);
        }
        this.curasRestantes = curas;
    }

    @Override
    protected void aoConversar(ComponentePersonagem jogador) {
        if (jogador == null || curasRestantes <= 0) {
            return;
        }
        if (jogador.getVidaAtual() < jogador.getVidaMaxima()) {
            jogador.curar(jogador.getVidaMaxima()); //curar() já limita na vida máxima
            curasRestantes--;
            mostrarAviso("Pronto! Sua vida foi restaurada.");
        }
    }

    public int getCurasRestantes() {
        return curasRestantes;
    }
}
