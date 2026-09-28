package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;

//Inimigo corpo a corpo, herda de ComponenteInimigo. atacar() só chama
//aplicarDanoDireto() da mãe, que já basta pra um golpe simples.

public class ComponenteInimigoMelee extends ComponenteInimigo {

    //CONSTRUTOR
    public ComponenteInimigoMelee(String nome, int vidaMaxima,
        double velocidade, int dano, double alcanceAtaque, double alcanceDeteccao) {
        super(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao,
            false, false, true);
    }

    @Override
    public void atacar(Entity alvo) {
        aplicarDanoDireto(alvo);
    }
}
