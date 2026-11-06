package com.jogo.excecoes;

//Mãe de todas as exceções próprias do jogo.
//É "checked" (estende Exception, não RuntimeException): quem chama um
//método que pode lançar uma delas é OBRIGADO pelo compilador a tratar
//(try/catch) ou repassar (throws). Serve pra situações que podem
//acontecer jogando normalmente, como pedir um item que não existe.
//
//Erro de programação (ex: criar personagem com vida negativa) não usa
//essas: usa IllegalArgumentException, que não precisa de try/catch.
public class JogoException extends Exception {

    public JogoException(String mensagem) {
        super(mensagem);
    }
}
