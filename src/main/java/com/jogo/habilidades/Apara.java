package com.jogo.habilidades;

import com.jogo.componentes.ComponenteJogador;

//Trazida do barebones (parry). No barebones o executar() ficava vazio
//porque precisava da engine; aqui ele funciona: abre uma janela curta
//(getDuracao()) em que o jogador não toma dano nem é empurrado —
//vale pra toque de inimigo e pra projétil (ver
//ComponenteJogador.serAtingido()).
public class Apara extends Habilidade {

    public Apara(boolean aprendida, double tempoEspera, int custoEnergia, double duracao) {
        super(aprendida, tempoEspera, custoEnergia, duracao);
    }

    @Override
    protected void executar(ComponenteJogador usuario) {
        usuario.iniciarApara(getDuracao());
    }
}
