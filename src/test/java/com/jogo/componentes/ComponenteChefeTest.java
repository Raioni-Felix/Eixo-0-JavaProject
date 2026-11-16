package com.jogo.componentes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//Chefe NEXUS: regras que não dependem do motor (fases, rotação dos
//ataques e como as magias o afetam)
class ComponenteChefeTest {

    private ComponenteChefe chefe;

    @BeforeEach
    void criar() {
        chefe = new ComponenteChefe("NEXUS", 400, 170, 12, 1280);
    }

    @Test
    @DisplayName("Fase 2 com metade da vida ou menos: mais rápido e mais tiros")
    void fases() {
        assertEquals(1, chefe.getFase());
        assertEquals(5, chefe.getQuantidadeTiros());
        double pairandoFase1 = chefe.getTempoPairando();
        chefe.tomarDano(199);
        assertEquals(1, chefe.getFase());
        chefe.tomarDano(1); //200 de 400
        assertEquals(2, chefe.getFase());
        assertEquals(7, chefe.getQuantidadeTiros());
        assertEquals(6, chefe.getQuantidadeBlocos());
        assertTrue(chefe.getTempoPairando() < pairandoFase1);
    }

    @Test
    @DisplayName("Rotação dos 4 ataques: Rajada -> Investida -> Chuva -> Armadilhas -> Rajada")
    void rotacao() {
        assertEquals(AtaqueChefe.RAJADA, chefe.getProximoAtaque());
        assertEquals(AtaqueChefe.INVESTIDA, AtaqueChefe.RAJADA.proximo());
        assertEquals(AtaqueChefe.CHUVA, AtaqueChefe.INVESTIDA.proximo());
        assertEquals(AtaqueChefe.ARMADILHAS, AtaqueChefe.CHUVA.proximo());
        assertEquals(AtaqueChefe.RAJADA, AtaqueChefe.ARMADILHAS.proximo());
        assertEquals(ComponenteChefe.Estado.PAIRANDO, chefe.getEstado());
    }

    @Test
    @DisplayName("Resiste às magias: gelo pela metade e Buraco Negro tira só 25%")
    void resistencias() {
        ComponenteInimigo comum = new ComponenteInimigoMelee("Robô", 60, 80, 10, 30, 200);
        assertEquals(60, comum.danoDeBuracoNegro());
        assertEquals(100, chefe.danoDeBuracoNegro());

        chefe.congelar(3.0); //vira 1,5 s
        assertTrue(chefe.atualizarCongelamento(1.4));
        assertTrue(chefe.estaCongelado());
        chefe.atualizarCongelamento(0.2);
        assertFalse(chefe.estaCongelado());
    }

    @Test
    @DisplayName("Vale 300 de XP, dano de toque normal fora da investida e arena mínima")
    void basicos() {
        assertEquals(300, chefe.getExperienciaConcedida());
        assertEquals(12, chefe.getDano());
        assertEquals(180, chefe.limitarX(0));
        assertEquals(1100, chefe.limitarX(5000));
        assertThrows(IllegalArgumentException.class, () -> new ComponenteChefe("X", 100, 100, 5, 300));
    }

    @Test
    @DisplayName("Ataque Armadilhas: 3 espinhos na fase 1, 5 e a prensa na fase 2, sem sair da arena")
    void ataqueArmadilhas() {
        assertEquals(3, chefe.calcularPosicoesEspinhos(640).size());
        assertFalse(chefe.usaPrensa());
        //Um espinho embaixo de onde a jogadora estava
        assertTrue(chefe.calcularPosicoesEspinhos(640).contains(640 - 32.0));
        chefe.tomarDano(200);
        assertEquals(5, chefe.calcularPosicoesEspinhos(640).size());
        assertTrue(chefe.usaPrensa());
        //Perto da parede, todos continuam dentro da arena (1280 de largura)
        for (double x : chefe.calcularPosicoesEspinhos(20)) {
            assertTrue(x >= 0 && x + 64 <= 1280);
        }
    }
}
