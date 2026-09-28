package com.jogo.niveis;

import com.google.gson.annotations.SerializedName;

//Espelha cada item da lista "platforms" do JSON do nível. O chão é só
//mais uma plataforma aqui (larga, cobrindo o nível inteiro), não tem
//tratamento especial nenhum.
public class DadosPlataforma {
    public double x;
    public double y;
    @SerializedName("width")
    public double largura;
    @SerializedName("height")
    public double altura;
}
