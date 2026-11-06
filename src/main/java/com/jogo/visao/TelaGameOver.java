package com.jogo.visao;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

//Tela de Game Over: fundo escuro + título + "Reiniciar" e "Menu
//Principal". A tela não sabe O QUE os botões fazem: recebe isso pronto
//(Runnable) de quem controla o jogo (controle.ControleJogo). É a
//separação do MVC: a visão desenha, o controle decide.
public class TelaGameOver extends TelaSobreposta {

    public TelaGameOver(Runnable aoReiniciar, Runnable aoVoltarAoMenu) {
        //Fundo quase preto em vez de cinza puro: sensação de vinheta
        Rectangle background = new Rectangle(getAppWidth(), getAppHeight(), Color.rgb(8, 6, 10, 0.88));

        Text title = new Text("GAME OVER");
        title.setFill(Color.web("#e63946"));
        title.setFont(Font.font("Arial", FontWeight.BLACK, 64));
        DropShadow titleShadow = new DropShadow(24, Color.rgb(0, 0, 0, 0.9));
        titleShadow.setOffsetY(3);
        title.setEffect(titleShadow);

        Text subtitle = new Text("Você foi derrotado");
        subtitle.setFill(Color.web("#cfcfcf"));
        subtitle.setFont(Font.font("Arial", 18));

        Button restartButton = EstiloUI.botaoMenu("Reiniciar", "#2e7d32", "#3fa043");
        restartButton.setOnAction(e -> aoReiniciar.run());

        Button quitButton = EstiloUI.botaoMenu("Menu Principal", "#7a1f1f", "#9c2b2b");
        quitButton.setOnAction(e -> aoVoltarAoMenu.run());

        VBox buttons = new VBox(14, restartButton, quitButton);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(30, title, subtitle, buttons);
        box.setAlignment(Pos.CENTER);

        getChildren().addAll(background, box);
    }
}
