package com.jogo.visao;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

//Menu de Opções (OPTIONS no menu principal, ou "Opções" na pausa).
//Esquerda: as opções (tela cheia e dificuldade), cada uma uma linha que
//muda ao clicar. Direita: a lista completa de controles. As ações e a
//lista de teclas vêm de fora (controle), como nas outras telas.
public class TelaOpcoes extends TelaSobreposta {

    private final Text valorTelaCheia = TelaInventario.texto("", 16, "#4de8d4", true);
    private final Text valorDificuldade = TelaInventario.texto("", 16, "#4de8d4", true);

    public TelaOpcoes(Runnable aoVoltar, Runnable aoTrocarTelaCheia, Runnable aoTrocarDificuldade,
            List<String[]> controles) {
        Rectangle fundo = new Rectangle(getAppWidth(), getAppHeight(), Color.rgb(4, 10, 18, 0.94));
        Node scanlines = EstiloUI.scanlines(getAppWidth(), getAppHeight());

        Node titulo = EstiloUI.textoComAberracao("OPÇÕES", 40, "#dffdf7", 18);

        VBox opcoes = new VBox(10,
                TelaInventario.rotulo("JOGO"),
                linhaOpcao("TELA CHEIA", "F11", valorTelaCheia, aoTrocarTelaCheia),
                linhaOpcao("DIFICULDADE", "dano recebido", valorDificuldade, aoTrocarDificuldade),
                TelaInventario.texto("Fácil: metade do dano · Difícil: 1,5x", 12, "#6fb3c4", false));

        //Controles numa grade de duas colunas: tecla | ação
        GridPane grade = new GridPane();
        grade.setHgap(22);
        grade.setVgap(6);
        for (int i = 0; i < controles.size(); i++) {
            grade.add(TelaInventario.texto(controles.get(i)[0], 14, "#4de8d4", true), 0, i);
            grade.add(TelaInventario.texto(controles.get(i)[1], 14, "#dffdf7", false), 1, i);
        }
        VBox listaControles = new VBox(12, TelaInventario.rotulo("CONTROLES"), grade);

        HBox meio = new HBox(28, TelaInventario.painel(opcoes), TelaInventario.painel(listaControles));
        meio.setAlignment(Pos.CENTER);

        Region voltar = EstiloUI.linhaMenu("VOLTAR", "ESC", true, aoVoltar);
        voltar.setMaxWidth(Region.USE_PREF_SIZE);

        VBox conteudo = new VBox(28, titulo, meio, voltar);
        conteudo.setAlignment(Pos.CENTER);
        getChildren().addAll(fundo, scanlines, conteudo);

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                aoVoltar.run();
            }
            e.consume();
        });
    }

    @Override
    public void mostrar() {
        super.mostrar();
        requestFocus();
    }

    public void atualizar(boolean telaCheia, String dificuldade) {
        valorTelaCheia.setText(telaCheia ? "LIGADA" : "DESLIGADA");
        valorDificuldade.setText(dificuldade.toUpperCase());
    }

    //Linha clicável: nome da opção, dica pequena e o valor atual à direita
    private Node linhaOpcao(String nome, String dica, Text valor, Runnable aoClicar) {
        VBox textos = new VBox(2, TelaInventario.texto(EstiloUI.espacado(nome), 15, "#dffdf7", true),
                TelaInventario.texto(dica, 11, "#3f7a96", false));
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linha = new HBox(14, textos, espaco, valor);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPrefWidth(360);
        linha.setPadding(new Insets(12, 18, 12, 18));
        String base = "-fx-background-color: rgba(10,26,38,0.66); -fx-border-color: transparent transparent transparent #1c5f74; -fx-border-width: 0 0 0 4;";
        String foco = "-fx-background-color: rgba(77,232,212,0.14); -fx-border-color: transparent transparent transparent #4de8d4; -fx-border-width: 0 0 0 4;";
        linha.setStyle(base);
        linha.setOnMouseEntered(e -> linha.setStyle(foco));
        linha.setOnMouseExited(e -> linha.setStyle(base));
        linha.setCursor(Cursor.HAND);
        linha.setOnMouseClicked(e -> aoClicar.run());
        return new StackPane(linha);
    }
}
