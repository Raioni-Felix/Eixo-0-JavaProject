package com.jogo.visao;

import javafx.scene.layout.StackPane;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

//Mãe das telas que cobrem o jogo (menu principal, pausa, Game Over).
//Abstrata: não existe "tela sobreposta genérica", só as filhas, e cada
//uma monta o próprio conteúdo no construtor. O que é igual pra todas
//fica aqui: ocupar a tela inteira e o mostrar/esconder.
//
//setMouseTransparent(true) ao esconder é importante: mesmo invisível, a
//tela ficaria por cima bloqueando o clique no resto do jogo.
public abstract class TelaSobreposta extends StackPane {

    protected TelaSobreposta() {
        setPrefSize(getAppWidth(), getAppHeight());
        esconder(); //toda tela nasce escondida; quem controla decide quando mostrar
    }

    public void mostrar() {
        setVisible(true);
        setMouseTransparent(false);
    }

    public void esconder() {
        setVisible(false);
        setMouseTransparent(true);
    }

    public boolean estaVisivel() {
        return isVisible();
    }
}
