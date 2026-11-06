package com.jogo.itens;

import com.almasb.fxgl.entity.Entity;
import com.jogo.inventario.Categoria;
import com.jogo.inventario.Inventariavel;

//Igual ao barebones: a arma é Inventariavel, então vai pro inventário
//do jogador junto com os módulos. Categoria ARMA não empilha, por isso
//cada arma ganha um id único (idBase + número), e duas espadas iguais
//ocupam dois espaços diferentes no Inventario.
//
//Abstrata, igual no barebones: não existe "arma genérica", só Espada,
//ArmaDeFogo e as filhas dela. Antes era uma classe só com um enum
//TipoArma, e o ComponenteArma decidia o ataque num switch. Agora cada
//subclasse diz como ataca no executarAtaque() (polimorfismo no lugar
//do switch): arma nova = classe nova, sem mexer no ComponenteArma.
public abstract class Arma implements Inventariavel {

    private static int contadorInstancias = 0;

    //Informações pro inventário
    private final String id;
    private final String nome;
    private final String descricao;

    //atributos de combate
    private int dano;
    private double cooldown;
    private double alcance;

    private double multiplicadorDano;
    private double ultimoUso;

    public Arma(String idBase, String nome, String descricao, int dano, double cooldown, double alcance){
        if (dano < 0 || cooldown < 0 || alcance <= 0) {
            throw new IllegalArgumentException("Arma com valores inválidos: dano=" + dano
                    + ", cooldown=" + cooldown + ", alcance=" + alcance);
        }
        this.id = idBase + "-" + (++contadorInstancias);
        this.nome = nome;
        this.descricao = descricao;
        this.dano = dano;
        this.cooldown = cooldown;
        this.alcance = alcance;
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

    //final: a regra do cooldown é a mesma pra TODA arma (mesma ideia do
    //causarDano() final do barebones). O que muda de uma arma pra
    //outra fica no executarAtaque(), que é abstrato.
    //portador = quem segura a arma; direcaoX = 1 (direita) ou -1 (esquerda)
    public final boolean atacar(Entity portador, double direcaoX) {
        if (!podeAtacar()) {
            return false;
        }
        ultimoUso = System.currentTimeMillis();
        executarAtaque(portador, direcaoX);
        return true;
    }

    //Cada arma decide do jeito que quiser
    protected abstract void executarAtaque(Entity portador, double direcaoX);

    //Como a jogadora segura a arma: "espada", "adagas" ou "arma" (de
    //fogo). A animação usa isso pra escolher os sprites (ver
    //FabricaEntidades); o modelo não sabe nada de imagem.
    public abstract String getEmpunhadura();

    //Uma linha explicando como a arma ataca (usada no HUD). Cada filha
    //sobrescreve com os números dela
    public abstract String getDescricaoCombate();

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

    //Métodos da interface Inventariavel
    @Override
    public String getId() { return id; }
    @Override
    public String getNome() { return nome; }
    @Override
    public String getDescricao() { return descricao; }
    @Override
    public Categoria getCategoria() { return Categoria.ARMA; }

    // Getters básicos
    public int getDano() { return dano; }
    public double getCooldownSegundos() { return cooldown; }
    public double getAlcance() { return alcance; }
}
