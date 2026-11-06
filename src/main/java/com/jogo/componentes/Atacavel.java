package com.jogo.componentes;

//Mesmo contrato do Atacavel do barebones: qualquer coisa que sabe tomar
//dano. Antes se chamava Danificavel aqui no FXGL; trocou de nome pra as
//duas versões do projeto usarem as mesmas interfaces.
//
//Fica separado de ComponentePersonagem de propósito: um baú ou uma
//caixa destrutível poderia implementar Atacavel sem carregar nome,
//vida máxima e velocidade, que só fazem sentido pra personagem.
public interface Atacavel {
    void tomarDano(int quant);
    boolean estaMorto();
}
