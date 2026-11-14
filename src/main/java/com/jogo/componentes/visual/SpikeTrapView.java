package com.jogo.componentes.visual;

import com.jogo.armadilhas.TrapState;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual sincronizado dos Espinhos Retráteis (Spike Trap).
 *
 * Nos quadros de spike-trap.gif (171x192):
 * - A metade superior (Y = 9..95) contém as lâminas que emergem acima da linha do piso.
 * - A metade inferior (Y = 95..191) contém as hastes/mecanismo subterrâneo que ficam ocultos atrás do bloco de chão.
 *
 * Sincronização por estado:
 * - INACTIVE:      Deslocado para baixo da linha do chão, ficando 100% oculto atrás do bloco de terreno.
 * - TELEGRAPHING:  Pontas preliminares emergem acima da linha do chão e vibram por 2.0s de aviso.
 * - ACTIVE:        Lâminas letais emergem totalmente acima da superfície do chão, mantendo a base atrás do terreno.
 * - COOLDOWN:      Lâminas recolhem suavemente para trás do bloco de chão.
 */
public class SpikeTrapView extends ImageView {

    private static final List<Image> FRAMES = new ArrayList<>();
    private static boolean carregado = false;

    private final double largura;
    private final double altura;
    private final Rectangle clipMask;
    private double timerAnimacao = 0.0;
    private double extensionOffsetY = 0.0;

    public SpikeTrapView(double largura, double altura) {
        this.largura = largura;
        this.altura = altura;
        carregarQuadros();

        // Como a linha do piso no GIF original (171x192) fica exatamente na metade da altura (Y=95 de 192),
        // dimensionamos a imagem com (altura * 2) para que a metade superior (as lâminas, 0..altura)
        // ocupe exatamente a área acima do chão, e a metade inferior (hastes subterrâneas, altura..2*altura)
        // fique para baixo da superfície (atrás do bloco de chão).
        setFitWidth(largura);
        setFitHeight(altura * 2.0);
        setSmooth(false);

        // Máscara de recorte inferior para garantir que a haste subterrânea nunca vaze por baixo de pisos finos (20px) ou grossos (40px)
        this.clipMask = new Rectangle(largura, altura + 18.0);
        setClip(this.clipMask);

        if (!FRAMES.isEmpty()) {
            setImage(getQuadroInativo());
        }
        atualizar(TrapState.INACTIVE, 0.0, 0.0);
    }

    private static synchronized void carregarQuadros() {
        if (carregado) {
            return;
        }
        try {
            for (int i = 0; i < 48; i++) {
                String caminho = String.format("/assets/textures/spike-trap/f_%02d.png", i);
                try (InputStream stream = SpikeTrapView.class.getResourceAsStream(caminho)) {
                    if (stream != null) {
                        FRAMES.add(new Image(stream));
                    }
                }
            }
            carregado = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Image getQuadroInativo() {
        if (FRAMES.isEmpty()) {
            return null;
        }
        // Quadro 4 é a base totalmente plana e recolhida
        return FRAMES.size() > 4 ? FRAMES.get(4) : FRAMES.get(0);
    }

    /**
     * Sincroniza o quadro do sprite e o deslocamento vertical de emergência em relação à superfície do chão.
     */
    public void atualizar(TrapState estado, double stateTimer, double tpf) {
        if (FRAMES.isEmpty()) {
            return;
        }

        timerAnimacao += tpf;

        switch (estado) {
            case INACTIVE -> {
                // Totalmente recolhido e deslocado para baixo da superfície do chão (escondido atrás do terreno)
                setImage(getQuadroInativo());
                extensionOffsetY = altura * 0.25;
            }
            case TELEGRAPHING -> {
                // Aviso prévio (2.0s): pontas curtas emergem visivelmente acima da linha do piso (quadros 12 a 15)
                int offset = ((int) (stateTimer * 12.0)) % 4;
                int idx = Math.min(12 + offset, FRAMES.size() - 1);
                setImage(FRAMES.get(idx));
                extensionOffsetY = -altura * 0.06;
            }
            case ACTIVE -> {
                // Disparo letal: lâminas sobem rapidamente e ficam totalmente expostas acima da linha do chão
                if (stateTimer < 0.15) {
                    double progress = stateTimer / 0.15;
                    int idx = 16 + (int) (progress * 5);
                    idx = Math.min(idx, FRAMES.size() - 1);
                    setImage(FRAMES.get(idx));
                    extensionOffsetY = (1.0 - progress) * (altura * 0.15);
                } else {
                    int idx = 21 + (((int) (timerAnimacao * 10.0)) % 10);
                    idx = Math.min(idx, FRAMES.size() - 1);
                    setImage(FRAMES.get(idx));
                    extensionOffsetY = 0.0;
                }
            }
            case COOLDOWN -> {
                // Recolhimento mecânico: lâminas descem de volta para trás do bloco de chão (quadros 35 a 47)
                int start = 35;
                int end = 47;
                int count = end - start + 1;
                double progress = Math.min(1.0, stateTimer / 1.0);
                int idx = start + (int) (progress * (count - 1));
                idx = Math.min(idx, FRAMES.size() - 1);
                setImage(FRAMES.get(idx));
                extensionOffsetY = progress * (altura * 0.25);
            }
            default -> {
                setImage(getQuadroInativo());
                extensionOffsetY = altura * 0.25;
            }
        }

        // Mantém o limite inferior do recorte dentro da espessura mínima das plataformas/chão (20px..40px)
        clipMask.setHeight(Math.max(altura, (altura + 18.0) - extensionOffsetY));
    }

    public double getExtensionOffsetY() {
        return extensionOffsetY;
    }
}
