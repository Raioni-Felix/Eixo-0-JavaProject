package com.jogo.npcs;

import com.jogo.componentes.ComponenteJogador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Só a lógica da conversa (sem motor): balão e física ficam de fora
class NpcTest {

    private ComponenteJogador jogador;

    @BeforeEach
    void criar() {
        jogador = new ComponenteJogador("Herói", 100, 200, 50);
    }

    @Test
    @DisplayName("Conversa começa perto, troca de fala com o tempo e acaba ao se afastar")
    void conversa() {
        NpcInformante npc = new NpcInformante("Info", List.of("a", "b"));
        npc.atualizarConversa(true, jogador, 0.1);
        assertTrue(npc.estaConversando());
        assertEquals("a", npc.getFalaAtual());
        npc.atualizarConversa(true, jogador, ComponenteNpc.SEGUNDOS_POR_FALA);
        assertEquals("b", npc.getFalaAtual());
        npc.atualizarConversa(false, jogador, 0.1);
        assertFalse(npc.estaConversando());
        assertEquals("a", npc.getFalaAtual()); // recomeça da primeira
    }

    @Test
    @DisplayName("Falha: NPC sem falas")
    void semFalas() {
        assertThrows(IllegalArgumentException.class, () -> new NpcInformante("Info", List.of()));
    }

    @Test
    @DisplayName("NPC é invulnerável")
    void invulneravel() {
        NpcInformante npc = new NpcInformante("Info", List.of("oi"));
        npc.tomarDano(999);
        assertFalse(npc.estaMorto());
    }

    @Test
    @DisplayName("Curandeira só gasta a cura com o jogador ferido")
    void curandeira() {
        NpcCurandeiro npc = new NpcCurandeiro("Cura", List.of("oi"), 1);
        npc.atualizarConversa(true, jogador, 0.1);
        assertEquals(1, npc.getCurasRestantes()); // vida cheia
        jogador.tomarDano(40);
        npc.atualizarConversa(true, jogador, 0.1);
        assertEquals(100, jogador.getVidaAtual());
        assertEquals(0, npc.getCurasRestantes());
        assertTrue(npc.getTextoBalao().contains("restaurada"));
    }
}
