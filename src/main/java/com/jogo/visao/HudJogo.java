package com.jogo.visao;

import com.jogo.componentes.ComponenteArma;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.inventario.Inventario;
import com.jogo.itens.Arma;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.binding.Bindings;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

//HUD do jogo: barras de vida e energia, módulos no inventário, arma
//equipada e nível/XP, e um aviso temporário (ex: mensagem de uma
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
        textoInventario = criarTexto(yEnergia + ALTURA_BARRA_ENERGIA + 20, Color.WHITE);
        textoStatus = criarTexto(textoInventario.getTranslateY() + 20, Color.WHITE);
        textoAviso = criarTexto(textoStatus.getTranslateY() + 24, Color.web("#ff8a80"));
        textoAviso.setOpacity(0);

        getGameScene().addUINodes(fundoVida, preenchimentoVida, fundoEnergia, preenchimentoEnergia,
                textoInventario, textoStatus, textoAviso);
        atualizarInventario();
        atualizarStatus();
    }

    //Quantos módulos de cada tipo o jogador tem
    public void atualizarInventario() {
        Inventario inventario = jogador.getInventario();
        textoInventario.setText("[1] Cura x" + inventario.quantidadeDe(idModuloCura)
                + "    [2] Energia x" + inventario.quantidadeDe(idModuloEnergia));
    }

    //Arma equipada (e como ela ataca) e nível/XP
    public void atualizarStatus() {
        Arma equipada = arma.getArmaEquipada();
        String textoArma = equipada == null ? "sem arma" : equipada.getNome() + " (" + equipada.getDescricaoCombate() + ")";
        textoStatus.setText("[Q] " + textoArma
                + "    Nv " + jogador.getNivel()
                + "  XP " + jogador.getXp() + "/" + jogador.getXpProximoNivel());
    }

    //Mensagem que aparece por 2 segundos e some devagar
    public void mostrarAviso(String mensagem) {
        textoAviso.setText(mensagem);
        textoAviso.setOpacity(1);
        PauseTransition espera = new PauseTransition(Duration.seconds(2));
        FadeTransition sumir = new FadeTransition(Duration.millis(400), textoAviso);
        sumir.setToValue(0);
        new SequentialTransition(espera, sumir).play();
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
