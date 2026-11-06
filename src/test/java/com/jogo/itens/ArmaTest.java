package com.jogo.itens;

import com.almasb.fxgl.entity.Entity;
import com.jogo.inventario.Categoria;
import javafx.scene.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmaTest {

    //Arma "de mentira" só pros testes: conta quantas vezes atacou, sem
    //precisar do motor do jogo (as armas reais criam entidades)
    static class ArmaDeTeste extends Arma {
        int ataques = 0;

        ArmaDeTeste(double cooldown) {
            super("teste", "Teste", "Arma de teste", 10, cooldown, 50);
        }

        @Override
        protected void executarAtaque(Entity portador, double direcaoX) {
            ataques++;
        }

        @Override
        public Node criarVisual() {
            return null;
        }

        @Override
        public String getDescricaoCombate() {
            return "teste";
        }
    }

    @Test
    @DisplayName("atacar() executa uma vez e respeita o cooldown")
    void cooldown() {
        ArmaDeTeste arma = new ArmaDeTeste(10);
        assertTrue(arma.podeAtacar());
        assertTrue(arma.atacar(null, 1));
        assertEquals(1, arma.ataques);

        assertFalse(arma.atacar(null, 1)); //ainda em cooldown
        assertEquals(1, arma.ataques);
        assertTrue(arma.getCooldownRestante() > 9);
    }

    @Test
    @DisplayName("Sem cooldown, ataca toda vez")
    void semCooldown() {
        ArmaDeTeste arma = new ArmaDeTeste(0);
        arma.atacar(null, 1);
        arma.atacar(null, -1);
        assertEquals(2, arma.ataques);
    }

    @Test
    @DisplayName("multiplicarDano acumula e arredonda")
    void multiplicarDano() {
        ArmaDeTeste arma = new ArmaDeTeste(1);
        assertEquals(10, arma.getDanoTotal());
        arma.multiplicarDano(1.2);
        assertEquals(12, arma.getDanoTotal());
        arma.multiplicarDano(1.5);
        assertEquals(18, arma.getDanoTotal());
    }

    @Test
    @DisplayName("Id único por arma e categoria ARMA (não empilha)")
    void inventariavel() {
        ArmaDeTeste a = new ArmaDeTeste(1);
        ArmaDeTeste b = new ArmaDeTeste(1);
        assertNotEquals(a.getId(), b.getId());
        assertEquals(Categoria.ARMA, a.getCategoria());
        assertEquals("Teste", a.getNome());
    }

    @Test
    @DisplayName("Falha: dano ou cooldown negativo, alcance zero")
    void valoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Espada("e", "E", "", -1, 0.5, 40));
        assertThrows(IllegalArgumentException.class, () -> new Espada("e", "E", "", 10, -0.5, 40));
        assertThrows(IllegalArgumentException.class, () -> new ArmaDeFogo("f", "F", "", 10, 0.5, 0));
    }
}
