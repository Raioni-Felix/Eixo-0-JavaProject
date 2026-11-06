package com.jogo.componentes;

import com.jogo.excecoes.ItemIncompativelException;
import com.jogo.excecoes.ItemNaoEncontradoException;
import com.jogo.inventario.Categoria;
import com.jogo.inventario.ModuloCura;
import com.jogo.inventario.ModuloEnergia;
import com.jogo.itens.Espada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//O ComponenteJogador é criado sem entidade nem motor: só as regras
//(vida, energia, XP, inventário) são testadas aqui
class ComponenteJogadorTest {

    private ComponenteJogador jogador;

    @BeforeEach
    void criar() {
        jogador = new ComponenteJogador("Herói", 100, 200, 50);
    }

    @Test
    @DisplayName("Nasce com vida e energia cheias, nível 1")
    void estadoInicial() {
        assertEquals(100, jogador.getVidaAtual());
        assertEquals(50, jogador.getEnergiaAtual());
        assertEquals(1, jogador.getNivel());
        assertEquals(0, jogador.getXp());
        assertFalse(jogador.estaMorto());
    }

    @Test
    @DisplayName("Falha: nome vazio, vida zero ou velocidade negativa")
    void construtorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new ComponenteJogador("", 100, 200, 50));
        assertThrows(IllegalArgumentException.class, () -> new ComponenteJogador("X", 0, 200, 50));
        assertThrows(IllegalArgumentException.class, () -> new ComponenteJogador("X", 100, -1, 50));
    }

    @Test
    @DisplayName("Energia: consumir para em 0, recuperar para na máxima")
    void energia() {
        jogador.consumirEnergia(30);
        assertEquals(20, jogador.getEnergiaAtual());
        assertTrue(jogador.energiaSuficiente(20));
        assertFalse(jogador.energiaSuficiente(21));
        jogador.consumirEnergia(999);
        assertEquals(0, jogador.getEnergiaAtual());
        jogador.recuperarEnergia(999);
        assertEquals(50, jogador.getEnergiaAtual());
        jogador.recuperarEnergia(-5);
        assertEquals(50, jogador.getEnergiaAtual());
    }

    @Test
    @DisplayName("Toma dano e fica invencível: o segundo golpe seguido não conta")
    void invencibilidade() {
        jogador.tomarDano(30);
        assertEquals(70, jogador.getVidaAtual());
        assertTrue(jogador.estaInvencivel());
        jogador.tomarDano(30);
        assertEquals(70, jogador.getVidaAtual());
    }

    @Test
    @DisplayName("curar respeita a vida máxima e não aceita negativo")
    void curar() {
        jogador.tomarDano(50);
        jogador.curar(20);
        assertEquals(70, jogador.getVidaAtual());
        jogador.curar(-10);
        assertEquals(70, jogador.getVidaAtual());
        jogador.curar(999);
        assertEquals(100, jogador.getVidaAtual());
    }

    @Test
    @DisplayName("Subir de nível aumenta a vida máxima, enche a vida e a meta de XP")
    void subirNivel() {
        jogador.tomarDano(40);
        jogador.adicionarXp(120);
        assertEquals(2, jogador.getNivel());
        assertEquals(20, jogador.getXp());
        assertEquals(120, jogador.getVidaMaxima());
        assertEquals(120, jogador.getVidaAtual());
        assertEquals(150, jogador.getXpProximoNivel());
    }

    @Test
    @DisplayName("Muito XP de uma vez sobe vários níveis")
    void variosNiveis() {
        jogador.adicionarXp(250); //100 pro 2, 150 pro 3
        assertEquals(3, jogador.getNivel());
    }

    @Test
    @DisplayName("usarItem: módulos aplicam o efeito e saem do inventário")
    void usarModulos() throws Exception {
        jogador.coletarItem(new ModuloCura("modulo_cura", "Cura", "", Categoria.CONSUMIVEL, 30));
        jogador.coletarItem(new ModuloEnergia("modulo_energia", "Energia", "", Categoria.CONSUMIVEL, 20));
        jogador.tomarDano(50);
        jogador.consumirEnergia(40);

        jogador.usarItem("modulo_cura");
        jogador.usarItem("modulo_energia");

        assertEquals(80, jogador.getVidaAtual());
        assertEquals(30, jogador.getEnergiaAtual());
        assertTrue(jogador.getInventario().inventarioVazio());
    }

    @Test
    @DisplayName("Exceção: usar item que não tem")
    void itemInexistente() {
        ItemNaoEncontradoException e = assertThrows(ItemNaoEncontradoException.class,
                () -> jogador.usarItem("modulo_cura"));
        assertEquals("modulo_cura", e.getIdItem());
        assertTrue(e.getMessage().contains("modulo_cura"));
    }

    @Test
    @DisplayName("Exceção: usar uma arma como consumível (e ela continua lá)")
    void itemIncompativel() {
        Espada espada = new Espada("espada", "Espada", "", 20, 0.5, 40);
        jogador.coletarItem(espada);
        assertThrows(ItemIncompativelException.class, () -> jogador.usarItem(espada.getId()));
        assertTrue(jogador.getInventario().temItem(espada.getId()));
    }
}
