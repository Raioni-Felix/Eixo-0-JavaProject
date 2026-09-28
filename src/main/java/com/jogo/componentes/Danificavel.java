package com.jogo.componentes;

//Contrato pra qualquer coisa que sabe tomar dano, sem precisar
//herdar de ComponentePersonagem (que carrega nome, vida máxima etc,
//coisa que não faz sentido pra um baú ou caixa destrutível, por
//exemplo). Os nomes dos métodos são de propósito os mesmos que já
//existiam em ComponentePersonagem, então declarar "implements
//Danificavel" numa classe assim não muda nenhuma linha dela.
public interface Danificavel {
    void tomarDano(int quant);
    boolean estaMorto();
}
