package com.jogo.visao;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

//Peças visuais reaproveitadas pelas telas (botões, linhas de menu,
//efeitos). Antes eram métodos privados do Main; vieram pra cá na
//separação MVC. Classe utilitária: só métodos static e construtor
//privado (ninguém precisa criar um EstiloUI).
public final class EstiloUI {

    private EstiloUI() {
    }

    //Botão de menu com visual consistente (cantos arredondados, texto
    //branco em negrito) e um hover simples, sem arquivo .css separado.
    //Sobrecarga de 2 cores (usada pelo Game Over): usa a própria cor de
    //hover como brilho.
    public static Button botaoMenu(String label, String baseColorHex, String hoverColorHex) {
        return botaoMenu(label, baseColorHex, hoverColorHex, hoverColorHex);
    }

    //Versão "estilizada" do botão: borda fina na cor de destaque
    //(glowColorHex), leve brilho e um crescimento suave (scale) ao
    //passar o mouse, em vez de só trocar a cor.
    public static Button botaoMenu(String label, String baseColorHex, String hoverColorHex, String glowColorHex) {
        Button button = new Button(label);
        //minWidth em vez de prefWidth fixo: garante o tamanho mínimo pros
        //rótulos curtos, mas deixa crescer pros maiores ("Menu Principal")
        button.setMinWidth(220);
        button.setPrefHeight(50);
        button.setPadding(new Insets(0, 20, 0, 20));
        button.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        button.setTextFill(Color.WHITE);

        String baseStyle = "-fx-background-color: " + baseColorHex + "; -fx-background-radius: 6;"
                + " -fx-border-color: " + glowColorHex + "; -fx-border-radius: 6; -fx-border-width: 1; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + hoverColorHex + "; -fx-background-radius: 6;"
                + " -fx-border-color: " + glowColorHex + "; -fx-border-radius: 6; -fx-border-width: 1.5; -fx-cursor: hand;";

        button.setStyle(baseStyle);

        DropShadow glow = new DropShadow(18, Color.web(glowColorHex));

        ScaleTransition growIn = new ScaleTransition(Duration.millis(120), button);
        growIn.setToX(1.06);
        growIn.setToY(1.06);

        ScaleTransition shrinkBack = new ScaleTransition(Duration.millis(120), button);
        shrinkBack.setToX(1.0);
        shrinkBack.setToY(1.0);

        button.setOnMouseEntered(e -> {
            button.setStyle(hoverStyle);
            button.setEffect(glow);
            growIn.playFromStart();
        });
        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setEffect(null);
            shrinkBack.playFromStart();
        });

        return button;
    }

    //Item de menu estilo HUD: quadradinho indicador + label + tecla de
    //atalho à direita, com destaque pro item primário (PLAY) e hover que
    //clareia o fundo. action=null deixa o item só visual (é o caso de
    //"OPTIONS", que ainda não tem tela própria).
    public static Region linhaMenu(String label, String hotkey, boolean primary, Runnable action) {
        Rectangle indicator = new Rectangle(8, 8, Color.web(primary ? "#4de8d4" : "#1c5f74"));
        if (primary) {
            indicator.setEffect(new DropShadow(10, Color.web("#4de8d4")));
        }

        Text labelText = new Text(espacado(label));
        labelText.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        labelText.setFill(Color.web(primary ? "#dffdf7" : "#8fc3d1"));

        Text hotkeyText = new Text(espacado(hotkey));
        hotkeyText.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        hotkeyText.setFill(Color.web(primary ? "#4de8d4" : "#3f7a96"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, indicator, labelText, spacer, hotkeyText);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(340);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.setCursor(Cursor.HAND);

        String baseStyle = primary
                ? "-fx-background-color: rgba(77,232,212,0.14); -fx-border-color: transparent transparent transparent #4de8d4;"
                        + " -fx-border-width: 0 0 0 4; -fx-effect: dropshadow(gaussian, rgba(77,232,212,0.18), 24, 0, 0, 0);"
                : "-fx-background-color: rgba(10,26,38,0.66); -fx-border-color: transparent transparent transparent #1c5f74;"
                        + " -fx-border-width: 0 0 0 4;";
        String hoverStyle = primary
                ? "-fx-background-color: rgba(77,232,212,0.22); -fx-border-color: transparent transparent transparent #4de8d4;"
                        + " -fx-border-width: 0 0 0 4; -fx-effect: dropshadow(gaussian, rgba(77,232,212,0.25), 28, 0, 0, 0);"
                : "-fx-background-color: rgba(77,232,212,0.14); -fx-border-color: transparent transparent transparent #4de8d4;"
                        + " -fx-border-width: 0 0 0 4;";

        row.setStyle(baseStyle);
        row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
        row.setOnMouseExited(e -> row.setStyle(baseStyle));

        if (action != null) {
            row.setOnMouseClicked(e -> action.run());
        }

        return row;
    }

    //Insere espaço entre cada caractere, pra simular o "letter spacing"
    //bem aberto do mockup (JavaFX não tem isso nativo em Text).
    public static String espacado(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append(text.charAt(i));
            if (i < text.length() - 1) {
                result.append(' ');
            }
        }
        return result.toString();
    }

    //Texto com "aberração cromática": duas cópias levemente deslocadas
    //(azul e magenta) atrás do texto principal, efeito de holograma.
    public static Node textoComAberracao(String text, double size, String mainColorHex, double glowRadius) {
        Text ghostBlue = new Text(text);
        ghostBlue.setFont(Font.font("Consolas", FontWeight.BLACK, size));
        ghostBlue.setFill(Color.web("#1f4e8c"));
        ghostBlue.setOpacity(0.85);
        ghostBlue.setTranslateX(-3);
        ghostBlue.setTranslateY(2);

        Text ghostMagenta = new Text(text);
        ghostMagenta.setFont(Font.font("Consolas", FontWeight.BLACK, size));
        ghostMagenta.setFill(Color.web("#7a2f4a"));
        ghostMagenta.setOpacity(0.7);
        ghostMagenta.setTranslateX(3);
        ghostMagenta.setTranslateY(-2);

        Text front = new Text(text);
        front.setFont(Font.font("Consolas", FontWeight.BLACK, size));
        front.setFill(Color.web(mainColorHex));
        DropShadow glow = new DropShadow(glowRadius, Color.web("#4de8d4"));
        glow.setSpread(0.2);
        front.setEffect(glow);

        StackPane stack = new StackPane(ghostBlue, ghostMagenta, front);
        stack.setAlignment(Pos.CENTER);
        return stack;
    }

    //Cantinho decorativo estilo "mira de HUD": um "L" usado nos 4 cantos
    //do painel de título.
    public static Node cantoDecorativo(String colorHex) {
        Color color = Color.web(colorHex);
        Rectangle horizontal = new Rectangle(16, 2, color);
        Rectangle vertical = new Rectangle(2, 16, color);
        return new Group(horizontal, vertical);
    }

    //Linhas horizontais finas e translúcidas (efeito de monitor CRT),
    //geradas uma vez só na construção do menu.
    public static Node scanlines(double width, double height) {
        Pane pane = new Pane();
        pane.setPrefSize(width, height);
        pane.setMouseTransparent(true);

        for (double y = 0; y < height; y += 4) {
            Rectangle line = new Rectangle(width, 2, Color.rgb(0, 0, 0, 0.3));
            line.setLayoutY(y);
            pane.getChildren().add(line);
        }

        return pane;
    }
}
