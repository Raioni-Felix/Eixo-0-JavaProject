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

//Tela de pausa (ESC durante o jogo): "Continuar", "Opções" ou voltar pro menu.
//Visual mais simples que o menu principal. Como no Game Over, as ações
//dos botões vêm de fora (controle).
public class TelaPausa extends TelaSobreposta {

    public TelaPausa(Runnable aoContinuar, Runnable aoOpcoes, Runnable aoVoltarAoMenu) {
        Rectangle background = new Rectangle(getAppWidth(), getAppHeight(), Color.rgb(8, 6, 10, 0.88));

        //Nome do jogo, menorzinho e mais discreto que o "PAUSADO"
        Text gameTitle = new Text("EIXO 0");
        gameTitle.setFill(Color.web("#dffdf7"));
        gameTitle.setFont(Font.font("Consolas", FontWeight.BLACK, 26));
        gameTitle.setOpacity(0.8);

        Text title = new Text("PAUSADO");
        title.setFill(Color.web("#4fd1c5"));
        title.setFont(Font.font("Consolas", FontWeight.BLACK, 56));
        title.setEffect(new DropShadow(20, Color.web("#4fd1c5")));

        VBox titleBlock = new VBox(6, gameTitle, title);
        titleBlock.setAlignment(Pos.CENTER);

        Button continueButton = EstiloUI.botaoMenu("Continuar", "#1f5c33", "#2e8b4f", "#4fd1c5");
        continueButton.setOnAction(e -> aoContinuar.run());

        Button optionsButton = EstiloUI.botaoMenu("Opções", "#1c3f5c", "#285a82", "#4de8d4");
        optionsButton.setOnAction(e -> aoOpcoes.run());

        Button mainMenuButton = EstiloUI.botaoMenu("Menu", "#7a1f1f", "#9c2b2b");
        mainMenuButton.setOnAction(e -> aoVoltarAoMenu.run());

        VBox buttons = new VBox(14, continueButton, optionsButton, mainMenuButton);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(30, titleBlock, buttons);
        box.setAlignment(Pos.CENTER);

        getChildren().addAll(background, box);
    }
}
