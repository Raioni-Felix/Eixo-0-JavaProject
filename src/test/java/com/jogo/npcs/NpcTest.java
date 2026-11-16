package com.jogo.npcs;

import com.jogo.componentes.ComponenteJogador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point2D;

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

    @Test
    @DisplayName("Cura da Curandeira custa energia")
    void curaCustaEnergia() {
        NpcCurandeiro npc = new NpcCurandeiro("Cura", List.of("oi"), 3);
        jogador.tomarDano(40);
        npc.atualizarConversa(true, jogador, 0.1);
        assertEquals(100, jogador.getVidaAtual());
        assertEquals(50 - NpcCurandeiro.CUSTO_ENERGIA_PADRAO, jogador.getEnergiaAtual());
        assertEquals(2, npc.getCurasRestantes());
    }

    @Test
    @DisplayName("Falha: sem energia a Curandeira não cura e avisa")
    void curaSemEnergia() {
        NpcCurandeiro npc = new NpcCurandeiro("Cura", List.of("oi"), 3);
        jogador.consumirEnergia(45); //sobra 5
        jogador.tomarDano(40);
        npc.atualizarConversa(true, jogador, 0.1);
        assertEquals(60, jogador.getVidaAtual());
        assertEquals(5, jogador.getEnergiaAtual());
        assertEquals(3, npc.getCurasRestantes());
        assertTrue(npc.getTextoBalao().contains("energia"));
    }

    @Test
    @DisplayName("Informante só na primeira sala, Curandeira só na última")
    void ondeAparecem() {
        assertTrue(NpcsDasSalas.temInformante(0));
        assertFalse(NpcsDasSalas.temInformante(1));
        assertFalse(NpcsDasSalas.temCurandeira(0, 5));
        assertFalse(NpcsDasSalas.temCurandeira(3, 5));
        assertTrue(NpcsDasSalas.temCurandeira(4, 5));
    }

    @Test
    @DisplayName("Chefe começa como NPC e só desperta depois de ouvir todas as falas")
    void chefeDespertaDepoisDaConversa() {
        List<Point2D> despertares = new ArrayList<>();
        NpcChefe npc = new NpcChefe("NEXUS", List.of("a", "b"), despertares::add);
        npc.atualizarConversa(true, jogador, 0.1);
        npc.atualizarConversa(true, jogador, ComponenteNpc.SEGUNDOS_POR_FALA); //vai pra "b"
        assertFalse(npc.conversaCompleta());
        assertFalse(npc.despertarSePronto(new Point2D(10, 20)));

        npc.atualizarConversa(true, jogador, ComponenteNpc.SEGUNDOS_POR_FALA); //"b" acabou
        assertTrue(npc.conversaCompleta());
        assertTrue(npc.despertarSePronto(new Point2D(10, 20)));
        assertFalse(npc.despertarSePronto(new Point2D(10, 20))); //uma vez só
        assertEquals(List.of(new Point2D(10, 20)), despertares);
    }

    @Test
    @DisplayName("Afastar no meio da conversa do chefe não começa a luta")
    void chefeConversaInterrompida() {
        NpcChefe npc = new NpcChefe("NEXUS", List.of("a", "b"), p -> fail("não devia despertar"));
        npc.atualizarConversa(true, jogador, 0.1);
        npc.atualizarConversa(true, jogador, ComponenteNpc.SEGUNDOS_POR_FALA);
        npc.atualizarConversa(false, jogador, 0.1); //saiu de perto: recomeça do "a"
        npc.atualizarConversa(true, jogador, 0.1);
        npc.atualizarConversa(true, jogador, ComponenteNpc.SEGUNDOS_POR_FALA);
        assertFalse(npc.conversaCompleta());
        assertFalse(npc.despertarSePronto(Point2D.ZERO));
    }
}
