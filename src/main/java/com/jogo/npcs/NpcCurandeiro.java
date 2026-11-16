package com.jogo.npcs;

import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.ComponentePersonagem;

import java.util.List;

/**
 * NPC que conversa e também cura o jogador (vida cheia) enquanto ele
 * estiver perto e ferido. Cada cura custa energia da jogadora
 * (custoEnergia) e gasta uma das curas da curandeira, então não vira
 * vida infinita. De vida cheia, ou sem energia suficiente, nada é gasto.
 *
 * A checagem é feita a cada frame da conversa, então funciona também se o
 * jogador se ferir (ou recuperar energia) já perto dela.
 */
public class NpcCurandeiro extends ComponenteNpc {

    public static final int CUSTO_ENERGIA_PADRAO = 20;

    private int curasRestantes;
    private final int custoEnergia;

    //Pra não repetir o aviso de "falta energia" a cada frame
    private boolean avisouFaltaEnergia = false;

    public NpcCurandeiro(String nome, List<String> falas, int curas) {
        this(nome, falas, curas, CUSTO_ENERGIA_PADRAO);
    }

    public NpcCurandeiro(String nome, List<String> falas, int curas, int custoEnergia) {
        super(nome, falas);
        if (curas < 0) {
            throw new IllegalArgumentException("Número de curas não pode ser negativo: " + curas);
        }
        if (custoEnergia < 0) {
            throw new IllegalArgumentException("Custo de energia não pode ser negativo: " + custoEnergia);
        }
        this.curasRestantes = curas;
        this.custoEnergia = custoEnergia;
    }

    @Override
    protected void aoConversar(ComponentePersonagem personagem) {
        //Só a jogadora tem energia pra pagar a cura
        if (!(personagem instanceof ComponenteJogador jogador) || curasRestantes <= 0) {
            return;
        }
        if (jogador.getVidaAtual() >= jogador.getVidaMaxima()) {
            return;
        }
        if (!jogador.energiaSuficiente(custoEnergia)) {
            if (!avisouFaltaEnergia) {
                mostrarAviso("Preciso de " + custoEnergia + " de energia sua pra te curar.");
                avisouFaltaEnergia = true;
            }
            return;
        }
        jogador.consumirEnergia(custoEnergia);
        jogador.curar(jogador.getVidaMaxima()); //curar() já limita na vida máxima
        curasRestantes--;
        avisouFaltaEnergia = false;
        mostrarAviso("Pronto! Sua vida foi restaurada (-" + custoEnergia + " de energia).");
    }

    @Override
    protected void aoEncerrarConversa() {
        avisouFaltaEnergia = false; //avisa de novo na próxima conversa
    }

    public int getCurasRestantes() {
        return curasRestantes;
    }

    public int getCustoEnergia() {
        return custoEnergia;
    }
}
