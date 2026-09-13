package com.jogo.niveis;

//Espelha o bloco "player" do JSON do nível (ver DadosNivel). Nomes de
//campo em inglês de propósito, pro Gson mapear direto do JSON sem
//precisar de anotação @SerializedName em cada um.
public class DadosJogador {
    public double x;
    public double y;
    public String name;
    public int maxHealth;
    public double moveSpeed;
}
