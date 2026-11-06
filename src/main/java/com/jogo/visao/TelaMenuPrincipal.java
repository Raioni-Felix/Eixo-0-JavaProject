package com.jogo.visao;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

//Menu principal. Layout baseado no mockup "EIXO Game Main Menu" que o
//Lincoln mandou (imagem de fundo do corredor de cryo-lab + título com
//aberração cromática + scanlines/vinheta + itens de menu estilo HUD).
//A fonte "Press Start 2P" do mockup não vem instalada, então usa
//Consolas em negrito (mesma família do resto da UI).
public class TelaMenuPrincipal extends TelaSobreposta {

    public TelaMenuPrincipal(Runnable aoJogar, Runnable aoSair) {
        double width = getAppWidth();
        double height = getAppHeight();

        //Camada 1: imagem de fundo esticada sem suavizar (pixel art nítido)
        ImageView background = new ImageView(RecursosVisuais.imagemFundoMenu());
        background.setFitWidth(width);
        background.setFitHeight(height);
        background.setSmooth(false);

        //Camada 2: vinheta (escurece as bordas) via gradiente radial
        Rectangle vignette = new Rectangle(width, height);
        vignette.setFill(new RadialGradient(
                0, 0, 0.5, 0.45, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.28, Color.rgb(6, 16, 26, 0)),
                new Stop(1.0, Color.rgb(4, 10, 18, 0.86))
        ));
        vignette.setMouseTransparent(true);

        //Camada 3: scanlines (efeito de monitor CRT/holograma)
        Node scanlines = EstiloUI.scanlines(width, height);

        //Camada 4: faixa de brilho ciano descendo a tela em loop ("scanner")
        Rectangle sweep = new Rectangle(width, 160);
        sweep.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(77, 232, 212, 0)),
                new Stop(0.5, Color.rgb(77, 232, 212, 0.055)),
                new Stop(1, Color.rgb(77, 232, 212, 0))
        ));
        sweep.setMouseTransparent(true);
        sweep.setTranslateY(-160);

        TranslateTransition sweepAnim = new TranslateTransition(Duration.seconds(7), sweep);
        sweepAnim.setFromY(-160);
        sweepAnim.setToY(height + 160);
        sweepAnim.setInterpolator(Interpolator.LINEAR);
        sweepAnim.setCycleCount(TranslateTransition.INDEFINITE);
        sweepAnim.play();

        //Linha "CRYOGENIC DIVISION" com tracinhos dos dois lados
        Rectangle divisionLineLeft = new Rectangle(56, 1, Color.web("#4de8d4"));
        Text divisionLabel = new Text(EstiloUI.espacado("CRYOGENIC DIVISION"));
        divisionLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        divisionLabel.setFill(Color.web("#4de8d4"));
        divisionLabel.setOpacity(0.7);
        Rectangle divisionLineRight = new Rectangle(56, 1, Color.web("#4de8d4"));
        HBox divisionRow = new HBox(18, divisionLineLeft, divisionLabel, divisionLineRight);
        divisionRow.setAlignment(Pos.CENTER);

        //Título "EIXO 0" com aberração cromática + brilho neon
        Node eixoText = EstiloUI.textoComAberracao("EIXO", 64, "#dffdf7", 22);
        Rectangle titleDivider = new Rectangle(34, 6, Color.web("#4de8d4"));
        titleDivider.setEffect(new DropShadow(16, Color.web("#4de8d4")));
        Node zeroText = EstiloUI.textoComAberracao("0", 64, "#4de8d4", 26);

        HBox titleRow = new HBox(22, eixoText, titleDivider, zeroText);
        titleRow.setAlignment(Pos.CENTER);

        Text protocolLabel = new Text(EstiloUI.espacado("SUBJECT ZERO PROTOCOL"));
        protocolLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        protocolLabel.setFill(Color.web("#6fb3c4"));

        VBox titleContent = new VBox(18, titleRow, protocolLabel);
        titleContent.setAlignment(Pos.CENTER);

        //Painel do título: fundo escuro translúcido, bordas e 4 cantos
        //decorativos (estilo mira/HUD) por cima
        StackPane titlePanel = new StackPane(titleContent);
        titlePanel.setPadding(new Insets(26, 44, 26, 44));
        titlePanel.setStyle(
                "-fx-background-color: rgba(6,20,30,0.72);"
                + " -fx-border-color: #4de8d4 transparent #1c5f74 transparent;"
                + " -fx-border-width: 2 0 2 0;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 24, 0, 0, 0);"
        );

        AnchorPane titleWrapper = new AnchorPane(titlePanel);
        AnchorPane.setTopAnchor(titlePanel, 0.0);
        AnchorPane.setLeftAnchor(titlePanel, 0.0);
        AnchorPane.setRightAnchor(titlePanel, 0.0);
        AnchorPane.setBottomAnchor(titlePanel, 0.0);

        Node cornerTopLeft = EstiloUI.cantoDecorativo("#4de8d4");
        Node cornerTopRight = EstiloUI.cantoDecorativo("#4de8d4");
        Node cornerBottomLeft = EstiloUI.cantoDecorativo("#1c5f74");
        Node cornerBottomRight = EstiloUI.cantoDecorativo("#1c5f74");
        AnchorPane.setTopAnchor(cornerTopLeft, -6.0);
        AnchorPane.setLeftAnchor(cornerTopLeft, -6.0);
        AnchorPane.setTopAnchor(cornerTopRight, -6.0);
        AnchorPane.setRightAnchor(cornerTopRight, -6.0);
        AnchorPane.setBottomAnchor(cornerBottomLeft, -6.0);
        AnchorPane.setLeftAnchor(cornerBottomLeft, -6.0);
        AnchorPane.setBottomAnchor(cornerBottomRight, -6.0);
        AnchorPane.setRightAnchor(cornerBottomRight, -6.0);
        titleWrapper.getChildren().addAll(cornerTopLeft, cornerTopRight, cornerBottomLeft, cornerBottomRight);

        VBox titleBlock = new VBox(14, divisionRow, titleWrapper);
        titleBlock.setAlignment(Pos.CENTER);

        //Itens do menu (linhas estilo HUD). As ações vêm do controle.
        Region playRow = EstiloUI.linhaMenu("PLAY", "ENTER", true, aoJogar);
        Region optionsRow = EstiloUI.linhaMenu("OPTIONS", "O", false, null);
        Region quitRow = EstiloUI.linhaMenu("QUIT", "ESC", false, aoSair);
        VBox menuRows = new VBox(10, playRow, optionsRow, quitRow);
        menuRows.setAlignment(Pos.CENTER);

        VBox centerContent = new VBox(76, titleBlock, menuRows);
        centerContent.setAlignment(Pos.CENTER);

        Text footerLeft = new Text(EstiloUI.espacado("CRYOBAY 7 · SYS v0.4.1"));
        footerLeft.setFont(Font.font("Consolas", 10));
        footerLeft.setFill(Color.web("#3f7a96"));

        Text footerRight = new Text(EstiloUI.espacado("SELECT ENTER CONFIRM"));
        footerRight.setFont(Font.font("Consolas", 10));
        footerRight.setFill(Color.web("#3f7a96"));

        getChildren().addAll(background, vignette, scanlines, sweep, centerContent, footerLeft, footerRight);
        StackPane.setAlignment(sweep, Pos.TOP_LEFT);
        StackPane.setAlignment(centerContent, Pos.CENTER);
        StackPane.setAlignment(footerLeft, Pos.BOTTOM_LEFT);
        StackPane.setMargin(footerLeft, new Insets(0, 0, 22, 28));
        StackPane.setAlignment(footerRight, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(footerRight, new Insets(0, 28, 22, 0));
    }
}
