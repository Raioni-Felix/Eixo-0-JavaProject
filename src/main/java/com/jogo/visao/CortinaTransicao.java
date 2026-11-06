package com.jogo.visao;

import javafx.animation.FadeTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

//Tela preta da troca de sala: escurece, deixa o controle trocar o
//conteúdo por trás enquanto está opaca, e clareia já na sala nova (o
//"corte" fica escondido). Sempre transparente a clique.
public class CortinaTransicao extends Rectangle {

    private static final Duration DURACAO = Duration.millis(220);

    public CortinaTransicao() {
        super(getAppWidth(), getAppHeight(), Color.BLACK);
        setOpacity(0);
        setMouseTransparent(true);
    }

    //noEscuro roda com a tela toda preta; aoTerminar quando já clareou
    public void fecharEAbrir(Runnable noEscuro, Runnable aoTerminar) {
        FadeTransition fechar = new FadeTransition(DURACAO, this);
        fechar.setToValue(1.0);
        fechar.setOnFinished(e -> {
            noEscuro.run();

            FadeTransition abrir = new FadeTransition(DURACAO, this);
            abrir.setToValue(0.0);
            abrir.setOnFinished(e2 -> aoTerminar.run());
            abrir.play();
        });
        fechar.play();
    }
}
