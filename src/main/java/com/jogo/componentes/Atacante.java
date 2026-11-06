package com.jogo.componentes;

//Trazida do barebones: quem sabe iniciar um ataque (inimigos e jogador).
//
//Diferença pro barebones: lá é iniciarAtaque(Personagem vitima), porque
//sem posição na tela alguém precisa dizer quem apanha. Aqui não precisa
//de parâmetro: o inimigo já guarda o alvo dele (definirAlvo()) e o
//jogador ataca pra onde está olhando.
public interface Atacante {
    void iniciarAtaque();
}
