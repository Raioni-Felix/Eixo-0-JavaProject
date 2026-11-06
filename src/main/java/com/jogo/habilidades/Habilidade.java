package com.jogo.habilidades;

import com.jogo.componentes.ComponenteJogador;

//Trazida do barebones: classe abstrata das habilidades que o jogador
//usa. Cada habilidade controla o próprio tempo de espera (cooldown).
//Diferença pro barebones: aqui não tem "alvo" (Personagem), porque no
//FXGL quem decide em quem acerta é a física/posição na tela — o golpe
//sai na direção em que o jogador olha, por exemplo.
public abstract class Habilidade {
    private boolean aprendida;
    private double tempoEspera;
    private double tempoRestante;
    private double duracao; //quanto tempo o efeito dura depois de usar
    private int custoEnergia;

    public Habilidade(boolean aprendida, double tempoEspera, int custoEnergia, double duracao) {
        if (tempoEspera < 0 || custoEnergia < 0 || duracao < 0) {
            throw new IllegalArgumentException("Habilidade com valores negativos");
        }
        this.aprendida = aprendida;
        this.tempoEspera = tempoEspera;
        this.custoEnergia = custoEnergia;
        this.duracao = duracao;
        this.tempoRestante = 0.0; //toda habilidade nasce disponível
    }

    //Chamado todo frame pelo ComponenteJogador.onUpdate(), com o tpf
    //(tempo do frame) do FXGL. Vai descontando o cooldown.
    public void atualizar(double dt) {
        tempoRestante = Math.max(0.0, tempoRestante - dt);
    }

    public boolean estaDisponivel() {
        return tempoRestante <= 0.0;
    }

    public boolean podeUsar(ComponenteJogador jogador) {
        return aprendida && estaDisponivel() && jogador.energiaSuficiente(custoPara(jogador));
    }

    //Quanto essa habilidade gasta agora. Padrão: o custo fixo do
    //construtor. O BuracoNegro sobrescreve (gasta toda a energia máxima).
    public int custoPara(ComponenteJogador jogador) {
        return custoEnergia;
    }

    //final: nenhuma filha pode mudar a ordem "confere, gasta energia,
    //executa, reinicia o cooldown". Cada filha só diz O QUE faz, no
    //executar().
    public final boolean usar(ComponenteJogador jogador) {
        if (!podeUsar(jogador)) {
            return false;
        }
        jogador.consumirEnergia(custoPara(jogador));
        executar(jogador);
        tempoRestante = tempoEspera;
        return true;
    }

    protected abstract void executar(ComponenteJogador usuario);

    public double getTempoEspera() {
        return tempoEspera;
    }

    public double getTempoRestante() {
        return tempoRestante;
    }

    public double getDuracao() {
        return duracao;
    }

    public int getCustoEnergia() {
        return custoEnergia;
    }

    public boolean isAprendida() {
        return aprendida;
    }

    public void setAprendida(boolean aprendida) {
        this.aprendida = aprendida;
    }
}
