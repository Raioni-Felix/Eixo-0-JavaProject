package com.jogo.componentes;

//Os três ataques do chefe (ComponenteChefe), na ordem em que ele usa.
//Cada um tem um tempo de aviso (o chefe brilha antes de atacar) pra dar
//chance de reagir, como nos chefes de Metroid.
public enum AtaqueChefe {
    RAJADA("Rajada"),       //leque de tiros na direção da jogadora
    INVESTIDA("Investida"), //mergulha em cima de onde ela estava
    CHUVA("Chuva de dados"); //colunas avisadas no chão e blocos caindo do teto

    private final String nome;

    AtaqueChefe(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    //Próximo da rotação (depois do último volta pro primeiro)
    public AtaqueChefe proximo() {
        AtaqueChefe[] todos = values();
        return todos[(ordinal() + 1) % todos.length];
    }
}
