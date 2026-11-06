package com.jogo.niveis;

import com.google.gson.annotations.SerializedName;

//Espelha cada item da lista "enemies" do JSON do nível. "tipo" tem que
//valer "voador", "ranged" ou "melee" (exatamente como está no JSON,
//que não foi traduzido) — é o que decide qual spawn da
//FabricaEntidades usar (ver GerenciadorSalas.spawnInimigos(): spawn("inimigo_" + tipo, ...)),
//então um valor diferente desses três quebra o carregamento (FXGL não
//acha o @Spawns correspondente).
public class DadosInimigo {
    @SerializedName("type")
    public String tipo;
    public double x;
    public double y;
    @SerializedName("name")
    public String nome;
    @SerializedName("maxHealth")
    public int vidaMaxima;
    @SerializedName("moveSpeed")
    public double velocidade;
    @SerializedName("damage")
    public int dano;
    @SerializedName("attackRange")
    public double alcanceAtaque;
    @SerializedName("detectionRange")
    public double alcanceDeteccao;
}
