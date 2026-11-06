package com.jogo.niveis;

import com.google.gson.annotations.SerializedName;

//Espelha cada item da lista "items" do JSON do nível (módulos de cura
//e energia, trazidos do barebones). Mesmo padrão dos outros Dados*:
//chave em inglês no JSON, campo em português aqui.
//"tipo" vale "cura" ou "energia" (ver Main.spawnItensDaSala());
//"quantidade" é quanto de vida/energia o módulo devolve.
public class DadosItem {
    @SerializedName("type")
    public String tipo;
    public double x;
    public double y;
    @SerializedName("amount")
    public int quantidade;
}
