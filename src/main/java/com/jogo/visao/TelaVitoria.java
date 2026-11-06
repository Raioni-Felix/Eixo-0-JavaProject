package com.jogo.visao;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

//Tela final, depois de derrotar o chefe. Igual às outras telas: os
//botões recebem a ação pronta (Runnable) do controle.
public class TelaVitoria extends TelaSobreposta {

    private final Text resumo = new Text();

    public TelaVitoria(Runnable aoJogarDeNovo, Runnable aoVoltarAoMenu) {
        Rectangle fundo = new Rectangle(getAppWidth(), getAppHeight(), Color.rgb(4, 10, 18, 0.9));
        Node scanlines = EstiloUI.scanlines(getAppWidth(), getAppHeight());

        Node titulo = EstiloUI.textoComAberracao("VITÓRIA", 64, "#dffdf7", 24);

        Text subtitulo = new Text("O núcleo do NEXUS foi desligado.");
        subtitulo.setFill(Color.web("#4de8d4"));
        subtitulo.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        resumo.setFill(Color.web("#9fc9d6"));
        resumo.setFont(Font.font("Monospaced", 16));

        Button deNovo = EstiloUI.botaoMenu("Jogar de novo", "#2e7d32", "#3fa043");
        deNovo.setOnAction(e -> aoJogarDeNovo.run());
        Button menu = EstiloUI.botaoMenu("Menu Principal", "#1c3f5c", "#285a82", "#4de8d4");
        menu.setOnAction(e -> aoVoltarAoMenu.run());

        VBox botoes = new VBox(14, deNovo, menu);
        botoes.setAlignment(Pos.CENTER);
        VBox caixa = new VBox(24, titulo, subtitulo, resumo, botoes);
        caixa.setAlignment(Pos.CENTER);
        getChildren().addAll(fundo, scanlines, caixa);
    }

    //Nível e tempo de jogo, mostrados embaixo do título
    public void atualizar(int nivel, double segundos) {
        int min = (int) segundos / 60;
        int seg = (int) segundos % 60;
        resumo.setText(String.format("Nível %d   ·   Tempo %d:%02d", nivel, min, seg));
    }
}
