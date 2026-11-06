package com.jogo.itens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//Catálogo: cria as armas dos itens "arma" do chão pelo nome do JSON
class CatalogoArmasTest {

    @Test
    @DisplayName("Cada nome cria a classe certa, e nomeDe volta pro nome")
    void criarTodas() {
        assertInstanceOf(Espada.class, CatalogoArmas.criar("espada"));
        assertInstanceOf(AdagaDupla.class, CatalogoArmas.criar("adagas"));
        assertInstanceOf(Shotgun.class, CatalogoArmas.criar("shotgun"));
        assertInstanceOf(PistolaDuplaDePlasma.class, CatalogoArmas.criar("pistola"));
        assertInstanceOf(RifleSniper.class, CatalogoArmas.criar("rifle"));
        for (String nome : CatalogoArmas.NOMES) {
            assertEquals(nome, CatalogoArmas.nomeDe(CatalogoArmas.criar(nome)));
        }
    }

    @Test
    @DisplayName("Nome desconhecido lança IllegalArgumentException")
    void desconhecida() {
        assertThrows(IllegalArgumentException.class, () -> CatalogoArmas.criar("bazuca"));
    }
}
