package com.jogo.habilidades;

import com.jogo.componentes.ComponenteJogador;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HabilidadeTest {

    //Habilidade de teste: só conta quantas vezes executou (as reais
    //criam disparos no mundo do jogo)
    static class HabilidadeDeTeste extends Habilidade {
        int execucoes = 0;

        HabilidadeDeTeste(boolean aprendida) {
            super(aprendida, 2.0, 20, 0.0);
        }

        @Override
        protected void executar(ComponenteJogador usuario) {
            execucoes++;
        }
    }

    private ComponenteJogador jogador;

    @BeforeEach
    void criar() {
        jogador = new ComponenteJogador("J", 100, 200, 50);
    }

    @Test
    @DisplayName("Usar gasta energia, executa e liga o cooldown")
    void usar() {
        HabilidadeDeTeste h = new HabilidadeDeTeste(true);
        assertTrue(h.usar(jogador));
        assertEquals(1, h.execucoes);
        assertEquals(30, jogador.getEnergiaAtual());
        assertFalse(h.estaDisponivel());
        assertEquals(2.0, h.getTempoRestante());
    }

    @Test
    @DisplayName("Falha: em cooldown não usa nem gasta energia")
    void emCooldown() {
        HabilidadeDeTeste h = new HabilidadeDeTeste(true);
        h.usar(jogador);
        assertFalse(h.usar(jogador));
        assertEquals(1, h.execucoes);
        assertEquals(30, jogador.getEnergiaAtual());
    }

    @Test
    @DisplayName("atualizar(dt) desconta o cooldown até zero")
    void atualizar() {
        HabilidadeDeTeste h = new HabilidadeDeTeste(true);
        h.usar(jogador);
        h.atualizar(1.5);
        assertFalse(h.estaDisponivel());
        h.atualizar(1.0);
        assertTrue(h.estaDisponivel());
        assertEquals(0.0, h.getTempoRestante());
        assertTrue(h.usar(jogador));
    }

    @Test
    @DisplayName("Falha: não aprendida ou sem energia")
    void naoPodeUsar() {
        HabilidadeDeTeste naoAprendida = new HabilidadeDeTeste(false);
        assertFalse(naoAprendida.usar(jogador));

        jogador.consumirEnergia(45); //sobra 5, custa 20
        HabilidadeDeTeste semEnergia = new HabilidadeDeTeste(true);
        assertFalse(semEnergia.usar(jogador));
        assertEquals(0, naoAprendida.execucoes + semEnergia.execucoes);
        assertEquals(5, jogador.getEnergiaAtual());
    }

    @Test
    @DisplayName("Falha: cooldown, custo ou duração negativos")
    void valoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Apara(true, -1, 5, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new Apara(true, 1, -5, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new BolaDeFogo(true, 1, 5, -1, 10));
    }

    @Test
    @DisplayName("BolaDeFogo sobrescreve a cor e o sprite do GolpeDeEnergia")
    void bolaDeFogo() {
        GolpeDeEnergia golpe = new BolaDeFogo(true, 2.0, 15, 0.0, 25);
        assertEquals(Color.ORANGERED, golpe.getCor());
        assertEquals("bola-de-fogo", golpe.getSprite());
        assertEquals(25, golpe.getDano());
        assertEquals(15, golpe.getCustoEnergia());
    }
}
