package com.jogo.componentes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComponenteInimigoTest {

    private final List<ComponenteInimigo> inimigos = List.of(
            new ComponenteInimigoMelee("Espadachim", 50, 100, 15, 40, 200),
            new ComponenteInimigoVoador("Morcego", 30, 120, 10, 40, 300),
            new ComponenteInimigoDistancia("Atirador", 20, 80, 12, 40, 350));

    @Test
    @DisplayName("Cada tipo concede o próprio XP (mesma chamada, valor diferente)")
    void experienciaPolimorfica() {
        int[] esperado = {20, 25, 35};
        for (int i = 0; i < inimigos.size(); i++) {
            ConcedeExperiencia inimigo = inimigos.get(i); //usado pelo tipo da interface
            assertEquals(esperado[i], inimigo.getExperienciaConcedida());
        }
    }

    @Test
    @DisplayName("Todo inimigo é Atacante e Atacavel")
    void interfaces() {
        for (ComponenteInimigo inimigo : inimigos) {
            assertInstanceOf(Atacante.class, inimigo);
            assertInstanceOf(Atacavel.class, inimigo);
        }
    }

    @Test
    @DisplayName("Dano que não mata só desconta a vida")
    void tomarDano() {
        ComponenteInimigo melee = inimigos.get(0);
        melee.tomarDano(20);
        assertEquals(30, melee.getVidaAtual());
        assertEquals(15, melee.getDano());
        assertFalse(melee.estaMorto());
    }

    @Test
    @DisplayName("Falha: inimigo sem vida")
    void vidaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> new ComponenteInimigoMelee("X", 0, 100, 1, 1, 1));
    }

    @Test
    @DisplayName("Congelar: fica congelado o tempo pedido e não encurta um congelamento maior")
    void congelar() {
        ComponenteInimigo inimigo = new ComponenteInimigoMelee("Robô", 50, 100, 15, 40, 200);
        assertFalse(inimigo.estaCongelado());
        inimigo.congelar(3.0);
        assertTrue(inimigo.estaCongelado());
        inimigo.congelar(1.0); //menor: continua valendo o de 3 s
        assertTrue(inimigo.atualizarCongelamento(2.5));
        assertTrue(inimigo.estaCongelado());
        assertTrue(inimigo.atualizarCongelamento(0.6));
        assertFalse(inimigo.estaCongelado());
        assertFalse(inimigo.atualizarCongelamento(0.1));
        assertThrows(IllegalArgumentException.class, () -> inimigo.congelar(0));
    }
}
