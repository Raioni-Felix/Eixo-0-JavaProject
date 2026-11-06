package com.jogo.niveis;

import com.jogo.excecoes.NivelInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

//Teste de integração com o Gson: lê os JSON de verdade das salas
class CarregadorDeNiveisTest {

    @ParameterizedTest(name = "nivel{0}.json: {1} inimigos, {2} itens")
    @CsvSource({"1,3,4", "2,4,4", "3,5,4", "4,6,5"})
    @DisplayName("As 4 salas do mapa carregam completas")
    void salasDoMapa(int numero, int inimigos, int itens) throws Exception {
        DadosNivel sala = CarregadorDeNiveis.carregar("/assets/levels/nivel" + numero + ".json");
        assertEquals("Herói", sala.jogador.nome);
        assertEquals(100, sala.jogador.vidaMaxima);
        assertEquals(50, sala.jogador.energiaMaxima);
        assertFalse(sala.plataformas.isEmpty());
        assertEquals(inimigos, sala.inimigos.size());
        assertEquals(itens, sala.itens.size());
    }

    @Test
    @DisplayName("Exceção: arquivo que não existe")
    void inexistente() {
        NivelInvalidoException e = assertThrows(NivelInvalidoException.class,
                () -> CarregadorDeNiveis.carregar("/assets/levels/nao-existe.json"));
        assertEquals("/assets/levels/nao-existe.json", e.getCaminho());
        assertTrue(e.getMessage().contains("não encontrado"));
    }

    @Test
    @DisplayName("Exceção: JSON quebrado guarda o erro do Gson como causa")
    void quebrado() {
        NivelInvalidoException e = assertThrows(NivelInvalidoException.class,
                () -> CarregadorDeNiveis.carregar("/assets/levels/quebrado.json"));
        assertNotNull(e.getCause());
    }

    @Test
    @DisplayName("Exceção: sala sem jogador")
    void semJogador() {
        NivelInvalidoException e = assertThrows(NivelInvalidoException.class,
                () -> CarregadorDeNiveis.carregar("/assets/levels/sem-jogador.json"));
        assertTrue(e.getMessage().contains("player"));
    }

    @Test
    @DisplayName("Sala sem a chave de inimigos vira lista vazia (não null)")
    void semInimigos() throws Exception {
        DadosNivel sala = CarregadorDeNiveis.carregar("/assets/levels/sem-inimigos.json");
        assertNotNull(sala.inimigos);
        assertTrue(sala.inimigos.isEmpty());
        assertEquals(50, sala.jogador.vidaMaxima);
    }
}
