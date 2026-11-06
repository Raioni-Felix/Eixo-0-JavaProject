package com.jogo.inventario;

import com.jogo.itens.Arma;
import com.jogo.itens.ArmaDeFogo;
import com.jogo.itens.Espada;
import com.jogo.itens.RifleSniper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventarioTest {

    private Inventario inventario;

    @BeforeEach
    void criar() {
        inventario = new Inventario();
    }

    private ModuloCura cura() {
        return new ModuloCura("modulo_cura", "Cura", "", Categoria.CONSUMIVEL, 30);
    }

    @Test
    @DisplayName("Consumíveis iguais empilham: um id, contador sobe")
    void empilhar() {
        assertTrue(inventario.adicionar(cura()));
        assertTrue(inventario.adicionar(cura()));
        assertEquals(1, inventario.quantidadeItem());
        assertEquals(2, inventario.quantidadeDe("modulo_cura"));
    }

    @Test
    @DisplayName("Armas não empilham: cada uma ocupa um espaço")
    void armasSeparadas() {
        inventario.adicionar(new Espada("espada", "Espada", "", 20, 0.5, 40));
        inventario.adicionar(new Espada("espada", "Espada", "", 20, 0.5, 40));
        assertEquals(2, inventario.quantidadeItem());
    }

    @Test
    @DisplayName("Falha: o mesmo objeto de arma duas vezes, ou null")
    void duplicataENull() {
        Espada espada = new Espada("espada", "Espada", "", 20, 0.5, 40);
        assertTrue(inventario.adicionar(espada));
        assertFalse(inventario.adicionar(espada));
        assertFalse(inventario.adicionar((Inventariavel) null)); //o cast escolhe qual sobrecarga
        assertEquals(1, inventario.quantidadeItem());
    }

    @Test
    @DisplayName("remover desconta a pilha e apaga quando chega a zero")
    void remover() {
        inventario.adicionar(cura());
        inventario.adicionar(cura());
        assertTrue(inventario.remover("modulo_cura"));
        assertEquals(1, inventario.quantidadeDe("modulo_cura"));
        assertTrue(inventario.remover("modulo_cura"));
        assertFalse(inventario.temItem("modulo_cura"));
        assertTrue(inventario.inventarioVazio());
    }

    @Test
    @DisplayName("Falha: remover/buscar o que não existe")
    void removerInexistente() {
        assertFalse(inventario.remover("nada"));
        assertFalse(inventario.remover(null));
        assertNull(inventario.buscar("nada"));
        assertNull(inventario.buscar(null));
        assertEquals(0, inventario.quantidadeDe("nada"));
    }

    @Test
    @DisplayName("listarDoTipo (genérico) filtra por classe e mantém a ordem")
    void listarDoTipo() {
        Espada espada = new Espada("espada", "Espada", "", 20, 0.5, 40);
        RifleSniper rifle = new RifleSniper("rifle", "Rifle", "", 30, 1.4, 900, 3);
        inventario.adicionar(espada);
        inventario.adicionar(cura());
        inventario.adicionar(rifle);

        List<Arma> armas = inventario.listarDoTipo(Arma.class);
        assertEquals(List.of(espada, rifle), armas);
        assertEquals(List.of(rifle), inventario.listarDoTipo(ArmaDeFogo.class));
        assertEquals(1, inventario.listarDoTipo(Consumivel.class).size()); //interface também serve
        assertTrue(inventario.listarDoTipo(ModuloEnergia.class).isEmpty());
    }

    @Test
    @DisplayName("Sobrecarga adicionar(List): conta só os que entraram")
    void adicionarLista() {
        Espada espada = new Espada("espada", "Espada", "", 20, 0.5, 40);
        List<Arma> armas = List.of(espada, espada, new RifleSniper("rifle", "Rifle", "", 30, 1.4, 900, 3));
        assertEquals(2, inventario.adicionar(armas)); //a espada repetida é recusada
        assertEquals(2, inventario.adicionar(List.of(new ModuloCura(30), new ModuloCura(30))));
        assertEquals(2, inventario.quantidadeDe(ModuloCura.ID));
    }

    @Test
    @DisplayName("Construtor sobrecarregado do módulo usa id e categoria padrão")
    void construtorCurto() {
        ModuloEnergia energia = new ModuloEnergia(20);
        assertEquals(ModuloEnergia.ID, energia.getId());
        assertEquals(Categoria.CONSUMIVEL, energia.getCategoria());
        assertEquals("Recupera 20 de energia", energia.getDescricao());
    }

    @Test
    @DisplayName("getItens devolve cópia: mexer nela não muda o inventário")
    void copiaDefensiva() {
        inventario.adicionar(cura());
        assertThrows(UnsupportedOperationException.class, () -> inventario.getItens().clear());
        assertEquals(1, inventario.quantidadeItem());
    }
}
