package com.jogo.visao;

import com.jogo.componentes.ComponenteArma;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.visual.Sprites;
import com.jogo.inventario.Inventario;
import com.jogo.inventario.Inventariavel;
import com.jogo.itens.Arma;
import com.jogo.itens.CatalogoArmas;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.function.Consumer;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

//Tela do inventário (tecla I ou TAB). Mesmo visual do menu principal:
//painéis escuros com borda ciano e cantos de mira.
//  - esquerda: status da jogadora (nível, XP, vida, energia) e habilidades;
//  - direita: as 5 armas do jogo (as que ainda não foram achadas aparecem
//    apagadas) e os módulos com a quantidade;
//  - embaixo: a descrição do item com o mouse em cima.
//Clicar numa arma equipa; clicar num módulo usa. Como as outras telas, é
//VISÃO: só lê o modelo e repassa o clique pro controle (Consumer).
public class TelaInventario extends TelaSobreposta {

    private static final String CIANO = "#4de8d4";
    private static final String CIANO_ESCURO = "#1c5f74";
    private static final String TEXTO = "#dffdf7";
    private static final String TEXTO_FRACO = "#6fb3c4";
    private static final List<String> MODULOS = List.of("modulo_cura", "modulo_energia");

    private final Consumer<String> aoEquipar;
    private final Consumer<String> aoUsar;

    private final VBox painelStatus = new VBox(10);
    private final HBox linhaArmas = new HBox(14);
    private final HBox linhaModulos = new HBox(14);
    private final Text detalheNome = texto("", 20, TEXTO, true);
    private final Text detalheTexto = texto("Passe o mouse sobre um item", 14, TEXTO_FRACO, false);

    public TelaInventario(Runnable aoFechar, Consumer<String> aoEquipar, Consumer<String> aoUsar) {
        this.aoEquipar = aoEquipar;
        this.aoUsar = aoUsar;

        Rectangle fundo = new Rectangle(getAppWidth(), getAppHeight(), Color.rgb(4, 10, 18, 0.9));
        Node scanlines = EstiloUI.scanlines(getAppWidth(), getAppHeight());

        Node titulo = EstiloUI.textoComAberracao("INVENTÁRIO", 40, TEXTO, 18);
        Text dica = texto(EstiloUI.espacado("I  ·  TAB  ·  ESC  PARA FECHAR"), 11, TEXTO_FRACO, false);
        VBox cabecalho = new VBox(6, titulo, dica);
        cabecalho.setAlignment(Pos.CENTER);

        painelStatus.setPrefWidth(320);
        VBox colunaItens = new VBox(14,
                rotulo("ARMAS  ·  clique para equipar"), linhaArmas,
                rotulo("MÓDULOS  ·  clique (ou 1 / 2) para usar"), linhaModulos);

        VBox detalhe = new VBox(6, detalheNome, detalheTexto);
        detalhe.setPrefWidth(860);
        detalhe.setMinHeight(70);

        HBox meio = new HBox(28, painel(painelStatus), painel(colunaItens));
        meio.setAlignment(Pos.CENTER);

        StackPane botaoFechar = new StackPane(texto(EstiloUI.espacado("FECHAR"), 14, TEXTO, true));
        botaoFechar.setPadding(new Insets(10, 30, 10, 30));
        botaoFechar.setStyle("-fx-background-color: rgba(77,232,212,0.14); -fx-border-color: " + CIANO + "; -fx-border-width: 1;");
        botaoFechar.setCursor(Cursor.HAND);
        botaoFechar.setOnMouseClicked(e -> aoFechar.run());
        botaoFechar.setMaxWidth(Region.USE_PREF_SIZE);

        VBox conteudo = new VBox(22, cabecalho, meio, painel(detalhe), botaoFechar);
        conteudo.setAlignment(Pos.CENTER);

        getChildren().addAll(fundo, scanlines, conteudo);

        //Teclas com o jogo pausado: a tela trata as próprias
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.I || e.getCode() == KeyCode.TAB) {
                aoFechar.run();
            } else if (e.getCode() == KeyCode.DIGIT1) {
                aoUsar.accept(MODULOS.get(0));
            } else if (e.getCode() == KeyCode.DIGIT2) {
                aoUsar.accept(MODULOS.get(1));
            }
            e.consume();
        });
    }

    @Override
    public void mostrar() {
        super.mostrar();
        requestFocus(); //pra receber as teclas
    }

    //Redesenha tudo com o estado atual (chamado ao abrir e a cada ação)
    public void atualizar(ComponenteJogador jogador, ComponenteArma componenteArma) {
        Inventario inventario = jogador.getInventario();

        //STATUS
        painelStatus.getChildren().setAll(
                rotulo("STATUS"),
                texto(jogador.getNome() + "   ·   Nível " + jogador.getNivel(), 18, TEXTO, true),
                barra("XP", jogador.getXp(), jogador.getXpProximoNivel(), "#ffd800"),
                barra("VIDA", jogador.getVidaAtual(), jogador.getVidaMaxima(), "#50e678"),
                barra("ENERGIA", jogador.getEnergiaAtual(), jogador.getEnergiaMaxima(), "#00c8ff"),
                espaco(8),
                rotulo("HABILIDADES"),
                texto("[X] Bola de Fogo   custo " + jogador.getGolpe().getCustoEnergia(), 14, TEXTO, false),
                texto("[C] Apara          custo " + jogador.getApara().getCustoEnergia(), 14, TEXTO, false),
                texto("[Shift] Dash", 14, TEXTO, false),
                espaco(8),
                texto("Dificuldade: " + jogador.getDificuldade().getNome(), 13, TEXTO_FRACO, false));

        //ARMAS: as 5 do jogo, na ordem do catálogo
        Arma equipada = componenteArma.getArmaEquipada();
        linhaArmas.getChildren().clear();
        List<Arma> arsenal = componenteArma.getArsenal();
        for (String nome : CatalogoArmas.NOMES) {
            Arma achada = arsenal.stream().filter(a -> CatalogoArmas.nomeDe(a).equals(nome)).findFirst().orElse(null);
            if (achada == null) {
                linhaArmas.getChildren().add(slot(nome, "?", false, false, null, "???", "Arma ainda não encontrada. Procure pelas salas."));
            } else {
                boolean estaEquipada = achada == equipada;
                linhaArmas.getChildren().add(slot(nome, estaEquipada ? "EQUIPADA" : achada.getNome(), true, estaEquipada,
                        () -> aoEquipar.accept(achada.getId()), achada.getNome(),
                        achada.getDescricao() + ". Ataque: " + achada.getDescricaoCombate() + "."));
            }
        }

        //MÓDULOS com a quantidade
        linhaModulos.getChildren().clear();
        for (int i = 0; i < MODULOS.size(); i++) {
            String id = MODULOS.get(i);
            int quantidade = inventario.quantidadeDe(id);
            Inventariavel modulo = inventario.buscar(id);
            String nome = i == 0 ? "Módulo de Cura" : "Módulo de Energia";
            String descricao = modulo != null ? modulo.getDescricao()
                    : (i == 0 ? "Recupera vida" : "Recupera energia") + ". Ache no chão das salas.";
            linhaModulos.getChildren().add(slot(id, "x" + quantidade + "   [" + (i + 1) + "]", quantidade > 0, false,
                    quantidade > 0 ? () -> aoUsar.accept(id) : null, nome, descricao));
        }
    }

    //Um quadrado com o ícone 3x maior, a legenda e o efeito de foco
    private Node slot(String icone, String legenda, boolean ativo, boolean destaque, Runnable aoClicar,
            String nome, String descricao) {
        ImageView imagem = new ImageView(Sprites.quadros("itens", icone, 1, 3).get(0));
        imagem.setOpacity(ativo ? 1 : 0.18);

        Rectangle moldura = new Rectangle(96, 96, Color.rgb(10, 26, 38, 0.8));
        moldura.setStroke(Color.web(destaque ? CIANO : CIANO_ESCURO));
        moldura.setStrokeWidth(destaque ? 3 : 1.5);
        if (destaque) {
            moldura.setEffect(new DropShadow(16, Color.web(CIANO)));
        }
        StackPane caixa = new StackPane(moldura, imagem);
        if (!ativo) {
            caixa.getChildren().add(texto("?", 30, TEXTO_FRACO, true));
        }

        Text textoLegenda = texto(legenda, 12, destaque ? CIANO : TEXTO_FRACO, true);
        VBox slot = new VBox(6, caixa, textoLegenda);
        slot.setAlignment(Pos.CENTER);
        slot.setPrefWidth(100);

        slot.setOnMouseEntered(e -> {
            moldura.setStroke(Color.web(CIANO));
            detalheNome.setText(nome);
            detalheTexto.setText(descricao);
        });
        slot.setOnMouseExited(e -> moldura.setStroke(Color.web(destaque ? CIANO : CIANO_ESCURO)));
        if (aoClicar != null) {
            slot.setCursor(Cursor.HAND);
            slot.setOnMouseClicked(e -> aoClicar.run());
        }
        return slot;
    }

    private Node barra(String nome, int atual, int maximo, String corHex) {
        double largura = 260;
        Rectangle fundo = new Rectangle(largura, 10, Color.web("#0d2230"));
        Rectangle cheio = new Rectangle(maximo <= 0 ? 0 : largura * Math.min(1, atual / (double) maximo), 10, Color.web(corHex));
        StackPane trilho = new StackPane(fundo, cheio);
        StackPane.setAlignment(cheio, Pos.CENTER_LEFT);
        trilho.setAlignment(Pos.CENTER_LEFT);
        TextFlow legenda = new TextFlow(texto(nome + "  ", 12, TEXTO_FRACO, true), texto(atual + " / " + maximo, 12, TEXTO, true));
        return new VBox(4, legenda, trilho);
    }

    //Painel escuro com borda ciano em cima/embaixo e os 4 cantos de mira
    static Node painel(Node conteudo) {
        StackPane caixa = new StackPane(conteudo);
        caixa.setPadding(new Insets(20, 26, 20, 26));
        caixa.setStyle("-fx-background-color: rgba(6,20,30,0.85); -fx-border-color: " + CIANO + " transparent "
                + CIANO_ESCURO + " transparent; -fx-border-width: 2 0 2 0;");
        StackPane comCantos = new StackPane(caixa);
        Node[] cantos = {EstiloUI.cantoDecorativo(CIANO), EstiloUI.cantoDecorativo(CIANO),
                EstiloUI.cantoDecorativo(CIANO_ESCURO), EstiloUI.cantoDecorativo(CIANO_ESCURO)};
        Pos[] posicoes = {Pos.TOP_LEFT, Pos.TOP_RIGHT, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT};
        for (int i = 0; i < 4; i++) {
            StackPane.setAlignment(cantos[i], posicoes[i]);
            comCantos.getChildren().add(cantos[i]);
        }
        comCantos.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return comCantos;
    }

    static Text rotulo(String texto) {
        return texto(EstiloUI.espacado(texto), 12, CIANO, true);
    }

    static Text texto(String conteudo, double tamanho, String corHex, boolean negrito) {
        Text texto = new Text(conteudo);
        texto.setFont(Font.font("Consolas", negrito ? FontWeight.BOLD : FontWeight.NORMAL, tamanho));
        texto.setFill(Color.web(corHex));
        return texto;
    }

    private static Region espaco(double altura) {
        Region r = new Region();
        r.setMinHeight(altura);
        return r;
    }
}
