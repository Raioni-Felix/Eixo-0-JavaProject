package com.jogo.niveis;

import com.google.gson.annotations.SerializedName;

//Espelha o bloco "player" do JSON do nível (ver DadosNivel). As
//chaves do JSON continuam em inglês (não vamos tocar nos arquivos
//.json), mas os campos Java agora ficam em português usando
//@SerializedName pra apontar pra chave original — o Gson lê a
//anotação em vez de bater pelo nome do campo.
public class DadosJogador {
    public double x;
    public double y;
    @SerializedName("name")
    public String nome;
    @SerializedName("maxHealth")
    public int vidaMaxima;
    @SerializedName("moveSpeed")
    public double velocidade;
    //Opcional no JSON. Se não vier, o Gson deixa 0 e o Main usa o
    //padrão (ENERGIA_MAXIMA_PADRAO).
    @SerializedName("maxEnergy")
    public int energiaMaxima;
}
