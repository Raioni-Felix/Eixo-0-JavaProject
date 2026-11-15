package com.jogo.npcs;

import com.jogo.componentes.ComponentePersonagem;

import java.util.List;

/**
 * NPC que só conversa: dá dicas sobre o jogo (onde achar armas, como
 * desviar do chefe etc.). Não faz nada além de falar, então o
 * aoConversar() fica vazio.
 */
public class NpcInformante extends ComponenteNpc {

    public NpcInformante(String nome, List<String> falas) {
        super(nome, falas);
    }

    @Override
    protected void aoConversar(ComponentePersonagem jogador) {
        //só fala, sem efeito no jogador
    }
}
