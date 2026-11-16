package com.jogo.habilidades;

import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.TimeScaleManager;
import com.jogo.componentes.efeitos.Cyberpsicose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//Sandevistan (tecla D) e o debuff Cyberpsicose: regras que não dependem
//do motor (energia, recarga, câmera lenta e efeitos da Cyberpsicose)
class SandevistanTest {

    private ComponenteJogador jogador;

    @BeforeEach
    void criar() {
        TimeScaleManager.reset();
        jogador = new ComponenteJogador("J", 100, 200, 50);
    }

    @AfterEach
    void limpar() {
        TimeScaleManager.reset(); //o estado é estático: não pode vazar pros outros testes
    }

    @Test
    @DisplayName("Liga a câmera lenta, gasta 20 de energia por segundo e desliga sozinho ao zerar")
    void ligarEGastar() {
        SandevistanMichyle sandevistan = jogador.getSandevistan();
        assertTrue(sandevistan.toggle(jogador));
        assertTrue(sandevistan.isAtivo());
        assertEquals(0.25, TimeScaleManager.getScale());

        sandevistan.atualizarSandevistan(jogador, 1.0);
        assertEquals(30, jogador.getEnergiaAtual());

        sandevistan.atualizarSandevistan(jogador, 2.0); //gasta os 30 que faltam
        assertFalse(sandevistan.isAtivo());
        assertEquals(1.0, TimeScaleManager.getScale());
        assertEquals(30.0, sandevistan.getCooldownRestante(), 0.001);
    }

    @Test
    @DisplayName("Depois de desligar, fica 30 s em recarga")
    void recarga() {
        SandevistanMichyle sandevistan = jogador.getSandevistan();
        sandevistan.toggle(jogador);
        sandevistan.toggle(jogador); //desliga na mão
        assertFalse(sandevistan.toggle(jogador)); //ainda em recarga
        sandevistan.atualizarSandevistan(jogador, 30.0);
        assertTrue(sandevistan.toggle(jogador));
    }

    @Test
    @DisplayName("Cyberpsicose: metade da velocidade e recargas dobradas, sem mexer na velocidade base")
    void cyberpsicose() {
        Cyberpsicose efeito = new Cyberpsicose();
        efeito.aplicar(jogador);
        assertTrue(efeito.estaAtivo());
        assertEquals(0.5, jogador.getMultiplicadorVelocidade());
        assertEquals(200, jogador.getVelocidade()); //a base continua a mesma

        //Dano contínuo para em 50% da vida do começo
        for (int i = 0; i < 20; i++) {
            efeito.atualizar(jogador, 1.0);
        }
        assertEquals(50, jogador.getVidaAtual());

        efeito.remover(jogador);
        assertFalse(efeito.estaAtivo());
        assertEquals(1.0, jogador.getMultiplicadorVelocidade());
    }

    @Test
    @DisplayName("Cyberpsicose só acaba depois do Sandevistan acabar e a energia voltar a 50%")
    void fimDaCyberpsicose() {
        Cyberpsicose efeito = new Cyberpsicose();
        efeito.aplicar(jogador);
        assertTrue(efeito.atualizar(jogador, 0.1)); //energia cheia, mas o Sandevistan não acabou
        efeito.marcarFimDoSandevistan();
        assertFalse(efeito.atualizar(jogador, 0.1)); //energia 50/50: acabou
    }
}
