package com.jogo.niveis;

//Espelha cada item da lista "enemies" do JSON do nível. "type" tem que
//ser "voador", "ranged" ou "melee" — é o que decide qual spawn da
//FabricaEntidades usar (ver Main.initGame(): spawn("inimigo_" + type,
//...)), então um valor diferente desses três quebra o carregamento
//(FXGL não acha o @Spawns correspondente).
public class DadosInimigo {
    public String type;
    public double x;
    public double y;
    public String name;
    public int maxHealth;
    public double moveSpeed;
    public int damage;
    public double attackRange;
    public double detectionRange;
}
