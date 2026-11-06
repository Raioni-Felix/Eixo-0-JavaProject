package com.jogo.componentes;

//Inimigo corpo a corpo, herda de ComponenteInimigo. Não sobrescreve o
//iniciarAtaque(): o ataque padrão da mãe (dano direto) já é um golpe
//simples, então ele só herda.

public class ComponenteInimigoMelee extends ComponenteInimigo {

    //CONSTRUTOR
    public ComponenteInimigoMelee(String nome, int vidaMaxima,
        double velocidade, int dano, double alcanceAtaque, double alcanceDeteccao) {
        super(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao,
            false, false, true);
    }

    @Override
    public int getExperienciaConcedida() {
        return 20;
    }
}
