package com.jogo.componentes;

//Dificuldade escolhida no menu de Opções: muda quanto dano a jogadora
//recebe (ver ComponenteJogador.tomarDano). Enum com valor próprio em
//cada constante: o "switch" fica dentro do próprio tipo.
public enum Dificuldade {
    FACIL("Fácil", 0.5),
    NORMAL("Normal", 1.0),
    DIFICIL("Difícil", 1.5);

    private final String nome;
    private final double multiplicadorDanoRecebido;

    Dificuldade(String nome, double multiplicadorDanoRecebido) {
        this.nome = nome;
        this.multiplicadorDanoRecebido = multiplicadorDanoRecebido;
    }

    public String getNome() {
        return nome;
    }

    //Dano depois da dificuldade (arredonda, e um golpe nunca vira 0)
    public int aplicar(int dano) {
        if (dano <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(dano * multiplicadorDanoRecebido));
    }

    //Fácil -> Normal -> Difícil -> Fácil (botão das Opções)
    public Dificuldade proxima() {
        return values()[(ordinal() + 1) % values().length];
    }
}
