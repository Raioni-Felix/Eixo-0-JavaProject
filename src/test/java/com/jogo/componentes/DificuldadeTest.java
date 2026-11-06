package com.jogo.componentes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

//Dificuldade das Opções: multiplica o dano que a jogadora recebe
class DificuldadeTest {

    @ParameterizedTest(name = "{0}: {1} de dano vira {2}")
    @CsvSource({"FACIL,20,10", "NORMAL,20,20", "DIFICIL,20,30", "FACIL,1,1", "DIFICIL,0,0"})
    void aplicar(Dificuldade dificuldade, int dano, int esperado) {
        assertEquals(esperado, dificuldade.aplicar(dano));
    }

    @Test
    @DisplayName("proxima() dá a volta: Fácil -> Normal -> Difícil -> Fácil")
    void ciclo() {
        assertEquals(Dificuldade.NORMAL, Dificuldade.FACIL.proxima());
        assertEquals(Dificuldade.DIFICIL, Dificuldade.NORMAL.proxima());
        assertEquals(Dificuldade.FACIL, Dificuldade.DIFICIL.proxima());
    }

    @Test
    @DisplayName("A jogadora toma menos dano no Fácil e mais no Difícil")
    void jogadora() {
        ComponenteJogador facil = new ComponenteJogador("A", 100, 200, 50);
        facil.setDificuldade(Dificuldade.FACIL);
        facil.tomarDano(20);
        assertEquals(90, facil.getVidaAtual());

        ComponenteJogador dificil = new ComponenteJogador("B", 100, 200, 50);
        dificil.setDificuldade(Dificuldade.DIFICIL);
        dificil.tomarDano(20);
        assertEquals(70, dificil.getVidaAtual());

        assertThrows(IllegalArgumentException.class, () -> facil.setDificuldade(null));
    }
}
