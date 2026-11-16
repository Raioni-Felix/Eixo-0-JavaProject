package com.jogo.visao.efeitos;

import com.almasb.fxgl.entity.components.ViewComponent;
import javafx.scene.Parent;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.Node;

/**
 * Efeito visual do SandevistanMichyle: desaturação + brilho nas bordas
 * estilo Cyberpunk 2077.
 * 
 * Aplica no ViewComponent do jogador (via getParent()) e/ou no root da GameScene.
 */
public class SandevistanShader {

    private static final ColorAdjust DESATURACAO = new ColorAdjust();
    private static final Glow BRILHO_BORDAS = new Glow();

    static {
        // Desaturação: tira cor (saturation = -1.0 = preto e branco)
        // Brightness levemente reduzido, contraste aumentado
        DESATURACAO.setSaturation(-0.85); // não 100% P&B, deixa um toque de cor
        DESATURACAO.setBrightness(-0.1);
        DESATURACAO.setContrast(0.3);

        // Brilho nas bordas (efeito "chromatic aberration" simplificado)
        BRILHO_BORDAS.setLevel(0.6);
    }

    /**
     * Retorna o efeito de desaturação para aplicar em nós individuais (ex: jogador).
     */
    public static Effect getEfeitoJogador() {
        return DESATURACAO;
    }

    /**
     * Retorna o efeito de brilho para aplicar na cena toda.
     */
    public static Effect getEfeitoCena() {
        return BRILHO_BORDAS;
    }

    /**
     * Aplica o efeito de desaturação no ViewComponent do jogador.
     * O ViewComponent do FXGL não tem setEffect direto, mas seu parent (Node) tem.
     */
    public static void aplicarNoJogador(ViewComponent view) {
        if (view != null) {
            Parent parent = view.getParent();
            if (parent != null) {
                parent.setEffect(DESATURACAO);
            }
        }
    }

    /**
     * Remove o efeito do ViewComponent do jogador.
     */
    public static void removerDoJogador(ViewComponent view) {
        if (view != null) {
            Parent parent = view.getParent();
            if (parent != null) {
                parent.setEffect(null);
            }
        }
    }

    /**
     * Aplica o efeito de brilho na cena inteira (root do GameScene).
     */
    public static void aplicarNaCena(Node root) {
        if (root != null) {
            root.setEffect(BRILHO_BORDAS);
        }
    }

    /**
     * Remove o efeito da cena.
     */
    public static void removerDaCena(Node root) {
        if (root != null) {
            root.setEffect(null);
        }
    }
}