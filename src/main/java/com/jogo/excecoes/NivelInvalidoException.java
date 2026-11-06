package com.jogo.excecoes;

//Lançada quando o JSON de uma sala não existe, está vazio ou mal
//formatado (ver niveis.CarregadorDeNiveis). Guarda a causa original
//(ex: erro do Gson) pra quem for investigar.
public class NivelInvalidoException extends JogoException {

    private final String caminho;

    public NivelInvalidoException(String caminho, String motivo) {
        super("Nível inválido (" + caminho + "): " + motivo);
        this.caminho = caminho;
    }

    public NivelInvalidoException(String caminho, Throwable causa) {
        this(caminho, causa.getMessage());
        initCause(causa);
    }

    public String getCaminho() {
        return caminho;
    }
}
