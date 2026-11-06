package com.jogo.componentes.visual;

import com.jogo.componentes.visual.ComponenteAnimado.EstadoAnimacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

//A regra de qual animação mostrar não depende do motor: dá pra testar
class ComponenteAnimadoTest {

    @Test
    @DisplayName("Parado no chão e andando no chão")
    void paradoEAndando() {
        assertEquals(EstadoAnimacao.PARADO, ComponenteAnimado.escolherEstado(false, false, true, 0, 0));
        assertEquals(EstadoAnimacao.PARADO, ComponenteAnimado.escolherEstado(false, false, true, 5, 0));
        assertEquals(EstadoAnimacao.ANDANDO, ComponenteAnimado.escolherEstado(false, false, true, -200, 0));
    }

    @Test
    @DisplayName("Subindo é pulo, descendo é queda; atacando correndo usa a animação própria")
    void puloEAtaque() {
        assertEquals(EstadoAnimacao.PULANDO, ComponenteAnimado.escolherEstado(false, false, false, 200, -300));
        assertEquals(EstadoAnimacao.CAINDO, ComponenteAnimado.escolherEstado(false, false, false, 200, 300));
        assertEquals(EstadoAnimacao.ATACANDO, ComponenteAnimado.escolherEstado(false, true, true, 0, 0));
        assertEquals(EstadoAnimacao.ATACANDO_ANDANDO, ComponenteAnimado.escolherEstado(false, true, true, 200, 0));
        assertEquals(EstadoAnimacao.ATACANDO, ComponenteAnimado.escolherEstado(false, true, false, 200, 0));
    }

    @Test
    @DisplayName("Dano vence todo o resto")
    void danoPrimeiro() {
        assertEquals(EstadoAnimacao.MACHUCADO, ComponenteAnimado.escolherEstado(true, true, false, 200, 0));
    }

    @Test
    @DisplayName("Sem a animação pedida, usa a mais parecida")
    void substituta() {
        Set<EstadoAnimacao> drone = EnumSet.of(EstadoAnimacao.PARADO, EstadoAnimacao.ANDANDO, EstadoAnimacao.MACHUCADO);
        assertEquals(EstadoAnimacao.ANDANDO, ComponenteAnimado.animacaoDisponivel(EstadoAnimacao.PULANDO, drone));
        assertEquals(EstadoAnimacao.ANDANDO, ComponenteAnimado.animacaoDisponivel(EstadoAnimacao.CAINDO, drone));
        assertEquals(EstadoAnimacao.PARADO, ComponenteAnimado.animacaoDisponivel(EstadoAnimacao.ATACANDO, drone));
        Set<EstadoAnimacao> robo = EnumSet.of(EstadoAnimacao.PARADO, EstadoAnimacao.ATACANDO);
        assertEquals(EstadoAnimacao.ATACANDO, ComponenteAnimado.animacaoDisponivel(EstadoAnimacao.ATACANDO_ANDANDO, robo));
    }

    @Test
    @DisplayName("Falha: personagem sem animação PARADO")
    void semParado() {
        assertThrows(IllegalArgumentException.class, () -> new ComponenteAnimado(Map.of(), 10, 5, 10, 1,
                () -> EstadoAnimacao.PARADO, () -> true));
    }

    @Test
    @DisplayName("Todos os quadros dos sprites existem e carregam")
    void quadrosExistem() {
        assertEquals(8, Sprites.quadros("jogador", "correr", 8, 1).size());
        assertEquals(4, Sprites.quadros("drone", "voar", 4, 1).size());
        assertEquals(6, Sprites.quadros("robo-melee", "andar", 6, 1).size());
        assertEquals(3, Sprites.quadros("robo-atirador", "tiro", 3, 1).size());
        assertEquals(6, Sprites.quadros("explosao", "explosao", 6, 1).size());
        assertThrows(IllegalStateException.class, () -> Sprites.quadros("jogador", "nao-existe", 1, 1));
    }

    @Test
    @DisplayName("Ampliar 1,5x: tamanho certo, cada pixel vira bloco sem borrar")
    void ampliar() {
        var original = Sprites.quadros("drone", "voar", 1, 1).get(0);
        var grande = Sprites.quadros("drone", "voar", 1, 1.5).get(0);
        assertEquals(Math.round(original.getWidth() * 1.5), (long) grande.getWidth());
        assertEquals(Math.round(original.getHeight() * 1.5), (long) grande.getHeight());
        //o pixel (20,20) ampliado vem do (13,13) original, sem mistura
        assertEquals(original.getPixelReader().getArgb(13, 13), grande.getPixelReader().getArgb(20, 20));
        assertThrows(IllegalArgumentException.class, () -> Sprites.quadros("drone", "voar", 1, 0));
    }
}
