package com.jogo.integracao;

import com.jogo.componentes.ComponenteInimigo;
import com.jogo.componentes.ComponenteInimigoDistancia;
import com.jogo.componentes.ComponenteInimigoMelee;
import com.jogo.componentes.ComponenteInimigoVoador;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.controle.ControleJogo;
import com.jogo.excecoes.ItemNaoEncontradoException;
import com.jogo.excecoes.JogoException;
import com.jogo.inventario.ModuloCura;
import com.jogo.inventario.ModuloEnergia;
import com.jogo.itens.Arma;
import com.jogo.itens.CatalogoArmas;
import com.jogo.niveis.CarregadorDeNiveis;
import com.jogo.niveis.DadosInimigo;
import com.jogo.niveis.DadosItem;
import com.jogo.niveis.DadosNivel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//Integração: várias partes do modelo juntas, na mesma ordem do jogo
//(carregar sala -> criar jogador -> coletar itens -> derrotar inimigos
//-> usar itens), sem abrir a janela do FXGL
class JogadorSalaIntegracaoTest {

    private static ComponenteInimigo criarInimigo(DadosInimigo d) {
        return switch (d.tipo) {
            case "voador" -> new ComponenteInimigoVoador(d.nome, d.vidaMaxima, d.velocidade, d.dano, d.alcanceAtaque, d.alcanceDeteccao);
            case "ranged" -> new ComponenteInimigoDistancia(d.nome, d.vidaMaxima, d.velocidade, d.dano, d.alcanceAtaque, d.alcanceDeteccao);
            default -> new ComponenteInimigoMelee(d.nome, d.vidaMaxima, d.velocidade, d.dano, d.alcanceAtaque, d.alcanceDeteccao);
        };
    }

    @Test
    @DisplayName("Sala 4: coleta (módulos e o rifle), sobe de nível com o XP dos inimigos e usa os módulos")
    void percorrerSala() throws JogoException {
        DadosNivel sala = CarregadorDeNiveis.carregar("/assets/levels/nivel4.json");
        ComponenteJogador jogador = new ComponenteJogador(sala.jogador.nome, sala.jogador.vidaMaxima,
                sala.jogador.velocidade, sala.jogador.energiaMaxima);

        //Itens do chão viram objetos do inventário (como no ControleJogo)
        for (DadosItem item : sala.itens) {
            if ("arma".equals(item.tipo)) {
                jogador.coletarItem(CatalogoArmas.criar(item.arma));
            } else if ("energia".equals(item.tipo)) {
                jogador.coletarItem(new ModuloEnergia(item.quantidade));
            } else {
                jogador.coletarItem(new ModuloCura(item.quantidade));
            }
        }
        assertEquals(2, jogador.getInventario().quantidadeDe(ControleJogo.ID_MODULO_CURA));
        assertEquals(2, jogador.getInventario().quantidadeDe(ControleJogo.ID_MODULO_ENERGIA));
        //A arma do chão da sala 4 é o rifle
        List<Arma> armas = jogador.getInventario().listarDoTipo(Arma.class);
        assertEquals(1, armas.size());
        assertEquals("rifle", CatalogoArmas.nomeDe(armas.get(0)));

        //Derrota todos: soma o XP de cada tipo (polimorfismo)
        List<ComponenteInimigo> inimigos = new ArrayList<>();
        for (DadosInimigo d : sala.inimigos) {
            inimigos.add(criarInimigo(d));
        }
        int xpTotal = 0;
        for (ComponenteInimigo inimigo : inimigos) {
            xpTotal += inimigo.getExperienciaConcedida();
        }
        assertEquals(2 * 25 + 2 * 35 + 2 * 20, xpTotal); //160
        jogador.adicionarXp(xpTotal);
        assertEquals(2, jogador.getNivel());
        assertEquals(120, jogador.getVidaMaxima());

        //Apanha, gasta energia e usa os módulos
        jogador.tomarDano(70);
        jogador.consumirEnergia(40);
        jogador.usarItem(ControleJogo.ID_MODULO_CURA);
        jogador.usarItem(ControleJogo.ID_MODULO_CURA);
        jogador.usarItem(ControleJogo.ID_MODULO_ENERGIA);
        assertEquals(110, jogador.getVidaAtual());
        assertEquals(30, jogador.getEnergiaAtual());

        //Acabaram: a terceira cura lança a exceção do modelo
        assertThrows(ItemNaoEncontradoException.class, () -> jogador.usarItem(ControleJogo.ID_MODULO_CURA));
        //Sobram o segundo Módulo de Energia e o rifle
        assertEquals(1, jogador.getInventario().quantidadeDe(ControleJogo.ID_MODULO_ENERGIA));
        assertFalse(jogador.getInventario().inventarioVazio());
    }
}
