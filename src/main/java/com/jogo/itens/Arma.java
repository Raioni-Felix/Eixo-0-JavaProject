package com.jogo.itens;
public class Arma {
    //atributos da classe
    private String nome;
    private int dano;
    private double cooldown;
    private double alcance;
    private TipoArma tipo;

    private double multiplicadorDano;
    private double ultimoUso;
    public Arma(String nome, int dano, double cooldown, double alcance, TipoArma tipo){
        this.nome = nome;
        this.dano = dano;
        this.cooldown = cooldown;
        this.alcance = alcance;
        this.tipo = tipo;
        this.multiplicadorDano = 1;
        this.ultimoUso = 0;
    }
    public int getDanoTotal(){
        return (int) Math.round(dano * multiplicadorDano);
    }
    // Verifica se o cooldown já passou
    public boolean podeAtacar() {
        long agora = System.currentTimeMillis();
        double segundosDesdeUltimoUso = (agora - ultimoUso) / 1000.0;
        return segundosDesdeUltimoUso >= cooldown;
    }

    // Executa o ataque, se possível
    public boolean atacar() {
        if (!podeAtacar()) {
            System.out.println(nome + " ainda em cooldown!");
            return false;
        }
        ultimoUso = System.currentTimeMillis();
        System.out.println(nome + " atacou causando " + getDanoTotal() + " de dano.");
        return true;
    }

    // Quanto tempo falta pro cooldown acabar (útil pra UI, tipo barra de cooldown)
    public double getCooldownRestante() {
        long agora = System.currentTimeMillis();
        double passado = (agora - ultimoUso) / 1000.0;
        double restante = cooldown - passado;
        return Math.max(0, restante);
    }

    // Aplica um upgrade/buff de dano (ex: encontrou um item que aumenta dano em 20%)
    public void multiplicarDano(double multiplicador) {
        this.multiplicadorDano *= multiplicador;
    }

    // Getters básicos
    public String getNome() { return nome; }
    public int getDano() { return dano; }
    public double getCooldownSegundos() { return cooldown; }
    public TipoArma getTipo() { return tipo ; }
    public double getAlcance() { return alcance; }
}
