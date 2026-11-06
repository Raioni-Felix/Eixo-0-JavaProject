package com.jogo.visao;

import com.jogo.componentes.ComponenteArma;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.visual.Sprites;
import com.jogo.inventario.Inventario;
import com.jogo.itens.CatalogoArmas;
import com.jogo.itens.Arma;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

//HUD do jogo: barras de vida e energia, ícones dos módulos (com
//quantidade) e da arma equipada, nível/XP, e avisos temporários (ex: mensagem de uma
//exceção tratada). Antes isso tudo morava no Main.initUI().
//
//É VISÃO: só lê o modelo (ComponenteJogador/ComponenteArma) e desenha.
//As barras se atualizam sozinhas por bind (propriedades observáveis); os
//textos são atualizados por quem controla (atualizarInventario/Status).
public class HudJogo {

    private static final double LARGURA_BARRA = 200;
    private static final double ALTURA_BARRA_VIDA = 20;
    private static final double ALTURA_BARRA_ENERGIA = 10;
    private static final double MARGEM = 20;

    private final ComponenteJogador jogador;
    private final ComponenteArma arma;
    private final String idModuloCura;
    private final String idModuloEnergia;

    private final Rectangle preenchimentoVida;
    private final Text textoInventario;
    private final Text textoStatus;
    private final Text textoAviso;
    private final Text textoMensagem;
    //Linha de ícones: módulos (com quantidade) e a arma equipada
    private final Text qtdCura = criarTexto(0, Color.WHITE);
    private final Text qtdEnergia = criarTexto(0, Color.WHITE);
    private final ImageView iconeArma = new ImageView();
    private SequentialTransition animacaoAviso;
    private SequentialTransition animacaoMensagem;

    public HudJogo(ComponenteJogador jogador, ComponenteArma arma, String idModuloCura, String idModuloEnergia) {
        this.jogador = jogador;
        this.arma = arma;
        this.idModuloCura = idModuloCura;
        this.idModuloEnergia = idModuloEnergia;

        //VIDA. Correção: antes a largura era vida * 200 / vidaMáxima com a
        //vida máxima lida UMA vez, no início. Depois de subir de nível
        //(vida máxima maior) a barra passava do fundo. Agora a conta lê a
        //vida máxima atual toda vez que a vida muda (e subir de nível
        //enche a vida, então a barra se recalcula na hora).
        Rectangle fundoVida = new Rectangle(LARGURA_BARRA, ALTURA_BARRA_VIDA, Color.DARKSLATEGRAY);
        preenchimentoVida = new Rectangle(LARGURA_BARRA, ALTURA_BARRA_VIDA, Color.LIMEGREEN);
        posicionar(fundoVida, MARGEM);
        posicionar(preenchimentoVida, MARGEM);
        preenchimentoVida.widthProperty().bind(Bindings.createDoubleBinding(
                () -> jogador.getVidaAtual() * LARGURA_BARRA / jogador.getVidaMaxima(),
                jogador.vidaAtualProperty()));

        //Verde -> vermelho com 30% de vida ou menos
        jogador.vidaAtualProperty().addListener((observable, antigo, novo) -> {
            double percentual = novo.doubleValue() / jogador.getVidaMaxima();
            preenchimentoVida.setFill(percentual <= 0.3 ? Color.CRIMSON : Color.LIMEGREEN);
        });

        //ENERGIA, logo abaixo, mesmo esquema
        double yEnergia = MARGEM + ALTURA_BARRA_VIDA + 6;
        Rectangle fundoEnergia = new Rectangle(LARGURA_BARRA, ALTURA_BARRA_ENERGIA, Color.DARKSLATEGRAY);
        Rectangle preenchimentoEnergia = new Rectangle(LARGURA_BARRA, ALTURA_BARRA_ENERGIA, Color.DEEPSKYBLUE);
        posicionar(fundoEnergia, yEnergia);
        posicionar(preenchimentoEnergia, yEnergia);
        preenchimentoEnergia.widthProperty().bind(Bindings.createDoubleBinding(
                () -> jogador.getEnergiaAtual() * LARGURA_BARRA / jogador.getEnergiaMaxima(),
                jogador.energiaAtualProperty()));

        //TEXTOS
        //Ícones: [1] cura xN   [2] energia xN   [Q] arma
        HBox icones = new HBox(8,
                rotuloTecla("1"), icone("modulo_cura"), qtdCura, new Text("  "),
                rotuloTecla("2"), icone("modulo_energia"), qtdEnergia, new Text("  "),
                rotuloTecla("Q"), iconeArma);
        qtdCura.setTranslateY(0);
        qtdEnergia.setTranslateY(0);
        qtdCura.setTranslateX(0);
        qtdEnergia.setTranslateX(0);
        icones.setAlignment(Pos.CENTER_LEFT);
        icones.setPadding(new Insets(4, 10, 4, 6));
        icones.setStyle("-fx-background-color: rgba(4,10,18,0.55); -fx-background-radius: 6;");
        icones.setTranslateX(MARGEM);
        icones.setTranslateY(yEnergia + ALTURA_BARRA_ENERGIA + 8);

        //Texto do inventário fica vazio (os ícones mostram); o de status
        //mostra o nome da arma e o nível, logo abaixo dos ícones
        textoInventario = criarTexto(0, Color.WHITE);
        textoStatus = criarTexto(yEnergia + ALTURA_BARRA_ENERGIA + 62, Color.web("#dffdf7"));
        textoAviso = criarTexto(textoStatus.getTranslateY() + 24, Color.web("#ff8a80"));
        textoAviso.setOpacity(0);
        textoMensagem = criarTexto(textoAviso.getTranslateY() + 22, Color.web("#ffe066"));
        textoMensagem.setOpacity(0);

        getGameScene().addUINodes(fundoVida, preenchimentoVida, fundoEnergia, preenchimentoEnergia,
                icones, textoStatus, textoAviso, textoMensagem);
        atualizarInventario();
        atualizarStatus();
    }

    //Quantos módulos de cada tipo o jogador tem
    public void atualizarInventario() {
        Inventario inventario = jogador.getInventario();
        qtdCura.setText("x" + inventario.quantidadeDe(idModuloCura));
        qtdEnergia.setText("x" + inventario.quantidadeDe(idModuloEnergia));
        textoInventario.setText("Cura x" + inventario.quantidadeDe(idModuloCura)
                + "  Energia x" + inventario.quantidadeDe(idModuloEnergia));
    }

    //Arma equipada (e como ela ataca) e nível/XP
    public void atualizarStatus() {
        Arma equipada = arma.getArmaEquipada();
        String textoArma = equipada == null ? "sem arma" : equipada.getNome() + " (" + equipada.getDescricaoCombate() + ")";
        iconeArma.setImage(equipada == null ? null
                : Sprites.quadros("itens", CatalogoArmas.nomeDe(equipada), 1, 1).get(0));
        textoStatus.setText(textoArma
                + "    Nv " + jogador.getNivel()
                + "  XP " + jogador.getXp() + "/" + jogador.getXpProximoNivel());
    }

    //Mensagem que aparece por 2 segundos e some devagar
    //(vermelho: algo deu errado, ex: exceção tratada)
    public void mostrarAviso(String mensagem) {
        animacaoAviso = piscar(textoAviso, mensagem, animacaoAviso);
    }

    //Mensagem boa (amarela), ex: item coletado
    public void mostrarMensagem(String mensagem) {
        animacaoMensagem = piscar(textoMensagem, mensagem, animacaoMensagem);
    }

    //Mostra o texto por 2 s e some devagar. Para a animação anterior,
    //senão um aviso antigo apagaria o novo no meio
    private static SequentialTransition piscar(Text texto, String mensagem, SequentialTransition anterior) {
        if (anterior != null) {
            anterior.stop();
        }
        texto.setText(mensagem);
        texto.setOpacity(1);
        PauseTransition espera = new PauseTransition(Duration.seconds(2));
        FadeTransition sumir = new FadeTransition(Duration.millis(400), texto);
        sumir.setToValue(0);
        SequentialTransition animacao = new SequentialTransition(espera, sumir);
        animacao.play();
        return animacao;
    }

    private static ImageView icone(String nome) {
        return new ImageView(Sprites.quadros("itens", nome, 1, 1).get(0));
    }

    private static Text rotuloTecla(String tecla) {
        Text texto = new Text("[" + tecla + "]");
        texto.setFill(Color.web("#4de8d4"));
        texto.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        return texto;
    }

    private static void posicionar(Rectangle retangulo, double y) {
        retangulo.setTranslateX(MARGEM);
        retangulo.setTranslateY(y);
    }

    private static Text criarTexto(double y, Color cor) {
        Text texto = new Text();
        texto.setFill(cor);
        texto.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        texto.setTranslateX(MARGEM);
        texto.setTranslateY(y);
        return texto;
    }
}
