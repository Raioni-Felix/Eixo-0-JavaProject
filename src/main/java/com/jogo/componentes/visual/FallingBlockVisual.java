package com.jogo.componentes.visual;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.jogo.armadilhas.TrapState;
import javafx.scene.image.Image;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utilitário e fábrica visual para a armadilha Bloco Que Cai (Ceiling Crusher).
 *
 * Recorta a spritesheet única (bloco-cai.png: prensa hidráulica, 4 quadros
 * de 64x64 desenhados para o projeto) por código, com {@link AnimationChannel}
 * e {@link AnimatedTexture}. O que muda entre os quadros são as luzes do painel:
 * - Frame 0: INACTIVE     (parada, luz ciano)
 * - Frame 1: TELEGRAPHING (luzes amarelas e vapor: vai cair)
 * - Frame 2: ACTIVE       (luzes vermelhas: caindo)
 * - Frame 3: COOLDOWN     (luzes azuis: subindo de volta)
 */
public final class FallingBlockVisual {

    private static Image spritesheet;
    private static Map<TrapState, AnimationChannel> canais;

    private FallingBlockVisual() {
    }

    /**
     * Carrega a imagem da spritesheet e gera os canais de animação correspondentes a cada estado.
     */
    public static synchronized void carregarRecursos() {
        if (spritesheet != null) {
            return;
        }

        try (InputStream stream = FallingBlockVisual.class.getResourceAsStream("/assets/textures/bloco-cai.png")) {
            if (stream != null) {
                spritesheet = new Image(stream);
            }
        } catch (Exception ignored) {
        }

        if (spritesheet == null) {
            try {
                spritesheet = FXGL.image("bloco-cai.png");
            } catch (Exception ignored) {
            }
        }

        if (spritesheet != null && !spritesheet.isError()) {
            int totalFrames = 4;
            int frameWidth = (int) (spritesheet.getWidth() / totalFrames);
            int frameHeight = (int) spritesheet.getHeight();

            canais = new EnumMap<>(TrapState.class);
            canais.put(TrapState.INACTIVE,
                    new AnimationChannel(spritesheet, totalFrames, frameWidth, frameHeight, Duration.seconds(1), 0, 0));
            canais.put(TrapState.TELEGRAPHING,
                    new AnimationChannel(spritesheet, totalFrames, frameWidth, frameHeight, Duration.seconds(1), 1, 1));
            canais.put(TrapState.ACTIVE,
                    new AnimationChannel(spritesheet, totalFrames, frameWidth, frameHeight, Duration.seconds(1), 2, 2));
            canais.put(TrapState.COOLDOWN,
                    new AnimationChannel(spritesheet, totalFrames, frameWidth, frameHeight, Duration.seconds(1), 3, 3));
        }
    }

    /**
     * Cria e retorna uma nova instância de {@link AnimatedTexture} inicializada no estado INACTIVE
     * e ajustada para as dimensões desejadas no mundo.
     */
    public static AnimatedTexture criarTextura(double largura, double altura) {
        carregarRecursos();
        if (canais == null || canais.isEmpty()) {
            return null;
        }
        AnimationChannel canalInativo = canais.get(TrapState.INACTIVE);
        AnimatedTexture texture = new AnimatedTexture(canalInativo);
        texture.setFitWidth(largura);
        texture.setFitHeight(altura);
        return texture;
    }

    /**
     * Retorna uma cópia do mapa mapeando cada {@link TrapState} para seu respectivo {@link AnimationChannel}.
     */
    public static Map<TrapState, AnimationChannel> getCanais() {
        carregarRecursos();
        return canais != null ? new EnumMap<>(canais) : new EnumMap<>(TrapState.class);
    }

    /**
     * Retorna a imagem da spritesheet carregada.
     */
    public static Image getSpritesheet() {
        carregarRecursos();
        return spritesheet;
    }
}
