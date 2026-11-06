package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.PhysicsComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponenteAuxiliaresTest {

    @Test
    @DisplayName("obterDe acha o componente pela classe mãe, seja qual for a filha")
    void obterDe() {
        Entity entidade = new Entity();
        entidade.addComponent(new PhysicsComponent()); //o inimigo procura a física ao entrar
        ComponenteInimigoVoador voador = new ComponenteInimigoVoador("Morcego", 30, 120, 10, 40, 300);
        entidade.addComponent(voador);

        assertSame(voador, ComponenteInimigo.obterDe(entidade));
        assertSame(voador, ComponentePersonagem.obterDe(entidade));
    }

    @Test
    @DisplayName("obterDe numa entidade sem personagem devolve null")
    void obterDeVazio() {
        Entity parede = new Entity();
        assertNull(ComponenteInimigo.obterDe(parede));
        assertNull(ComponentePersonagem.obterDe(parede));
    }

    @Test
    @DisplayName("Logo depois de despausar está no período de graça")
    void relogio() {
        RelogioJogo.marcarRetomada();
        assertTrue(RelogioJogo.emPeriodoDeGraca());
    }

    @Test
    @DisplayName("Tomar um golpe atordoa o jogador por um instante")
    void atordoado() {
        ComponenteJogador jogador = new ComponenteJogador("J", 100, 200, 50);
        assertFalse(jogador.estaAtordoado());
        jogador.tomarDano(10);
        assertTrue(jogador.estaAtordoado());
    }

    @Test
    @DisplayName("toString mostra os dados do personagem")
    void textos() {
        ComponenteJogador jogador = new ComponenteJogador("Herói", 100, 200, 50);
        assertTrue(jogador.toString().contains("Herói"));
        assertTrue(jogador.toString().contains("100/100"));
        assertTrue(new ComponenteInimigoMelee("Bandido", 40, 100, 8, 40, 200).toString().contains("Bandido"));
    }
}
