package com.jogo;

/**
 * Ponto de entrada do executável (.jar).
 *
 * Num .jar com todas as dependências juntas, o Java recusa abrir direto
 * uma classe que estende Application/GameApplication ("JavaFX runtime
 * components are missing"). Uma classe comum que só chama Main.main()
 * contorna isso. Pela IDE e pelo "mvn exec:java" continua sendo o Main.
 */
public class Lancador {

    public static void main(String[] args) {
        Main.main(args);
    }
}
