package com.jogo.armadilhas;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;

/**
 * Armadilha de Espinhos Retráteis de chão.
 *
 * Comportamento baseado em Proximidade (sem ciclo automático infinito):
 * 1. INACTIVE     -> Permanece oculta/recolhida no chão aguardando o jogador pisar na superfície (triggerBox).
 * 2. TELEGRAPHING -> Ao detectar o jogador em cima, vibra e emite aviso durante 2.0 segundos.
 * 3. ACTIVE       -> Lança as pontas afiadas causando dano letal e knockback a quem estiver em contato.
 * 4. COOLDOWN     -> Recolhe as lâminas para o solo e desarma temporariamente.
 * 5. INACTIVE     -> Rearma e fica pronta para um novo acionamento por aproximação.
 */
public class SpikesRetrateis extends Trap {

    private int numberOfSpikes = 4;
    private double spikeExtensionRatio = 0.0; // 0.0 (recolhido) a 1.0 (totalmente erguido)
    private boolean cyclic = false; // Desabilitado por padrão: ativação puramente por proximidade

    /**
     * Construtor padrão com ativação por proximidade e 2.0s de aviso prévio.
     */
    public SpikesRetrateis(double x, double y, double width, double height) {
        this(x, y, width, height, -1.0, 1.0, 2.0, 1.5, 25);
    }

    /**
     * Construtor completo parametrizável.
     */
    public SpikesRetrateis(double x, double y, double width, double height,
                           double inactiveDuration, double telegraphDuration,
                           double activeDuration, double cooldownDuration, int damage) {
        super("Espinhos Retráteis", x, y, width, height);
        // inactiveDuration <= 0 desativa o temporizador automático infinito
        this.inactiveDuration = inactiveDuration;
        // Pelo menos 2.0 segundos de aviso (telegraphing) antes do disparo
        this.telegraphDuration = Math.max(2.0, telegraphDuration);
        this.activeDuration = activeDuration;
        this.cooldownDuration = cooldownDuration;
        this.damage = damage;
        this.knockbackForce = 320.0;
        this.cyclic = inactiveDuration > 0;

        // Hitbox de dano ajustada nas pontas:
        double horizontalMargin = 4.0;
        double basePlateHeight = height * 0.25;
        this.damageHitbox = new Hitbox(
                horizontalMargin,
                0,
                Math.max(4.0, width - (horizontalMargin * 2)),
                height - basePlateHeight
        );

        // TriggerBox de proximidade: exatamente na superfície superior do piso dos espinhos
        // Cobre toda a largura dos espinhos (0 a width) e a camada imediatamente acima (-12px)
        this.triggerBox = new Hitbox(0, -12, width, height + 12);
    }

    @Override
    protected void transitionTo(TrapState newState) {
        super.transitionTo(newState);
        if (newState == TrapState.TELEGRAPHING) {
            spikeExtensionRatio = 0.25;
        } else if (newState == TrapState.ACTIVE) {
            spikeExtensionRatio = 1.0;
        } else if (newState == TrapState.INACTIVE) {
            spikeExtensionRatio = 0.0;
        }
    }

    @Override
    protected void updateCustom(double deltaTime) {
        // Atualiza a animação de extensão dos espinhos conforme o estado
        switch (state) {
            case INACTIVE -> spikeExtensionRatio = 0.0;
            case TELEGRAPHING -> {
                // Durante o aviso (2.0s), as pontas apontam 25% para fora do chão tremendo
                spikeExtensionRatio = 0.25;
            }
            case ACTIVE -> {
                // Totalmente erguidos e letais
                spikeExtensionRatio = 1.0;
            }
            case COOLDOWN -> {
                // Recolhendo gradativamente até 0
                double progress = Math.min(1.0, stateTimer / cooldownDuration);
                spikeExtensionRatio = Math.max(0.0, 1.0 - progress);
            }
            default -> {}
        }
    }

    @Override
    protected void updateInactive(double deltaTime) {
        // Não avança por ciclo automático infinito a menos que explicitamente configurado com inactiveDuration > 0
        if (cyclic && inactiveDuration > 0) {
            super.updateInactive(deltaTime);
        }
    }

    @Override
    protected void renderContent(Graphics g, double drawX, double drawY) {
        // [PONTO DE INSERÇÃO DE SPRITE: Se houver imagem ou textura, renderize aqui]
        Color prev = g.getColor();

        // 1. Placa base de metal fixa no chão
        g.setColor(new Color(60, 60, 65));
        int baseHeight = Math.max(4, (int) (height * 0.25));
        g.fillRect((int) drawX, (int) (drawY + height - baseHeight), (int) width, baseHeight);

        // 2. Lâminas dos espinhos (triângulos desenhados de acordo com a extensão)
        if (spikeExtensionRatio > 0.05) {
            double currentSpikeHeight = (height - baseHeight) * spikeExtensionRatio;
            double spikeWidth = width / numberOfSpikes;

            Color spikeColor = (state == TrapState.ACTIVE)
                    ? new Color(220, 40, 40)
                    : new Color(180, 185, 195);

            g.setColor(spikeColor);

            for (int i = 0; i < numberOfSpikes; i++) {
                double sx = drawX + (i * spikeWidth);
                double sy = drawY + height - baseHeight;

                Polygon triangle = new Polygon();
                triangle.addPoint((int) sx, (int) sy);
                triangle.addPoint((int) (sx + spikeWidth / 2.0), (int) (sy - currentSpikeHeight));
                triangle.addPoint((int) (sx + spikeWidth), (int) sy);

                g.fillPolygon(triangle);

                g.setColor(Color.DARK_GRAY);
                g.drawPolygon(triangle);
                g.setColor(spikeColor);
            }
        }

        g.setColor(prev);
    }

    public int getNumberOfSpikes() {
        return numberOfSpikes;
    }

    public void setNumberOfSpikes(int numberOfSpikes) {
        this.numberOfSpikes = Math.max(1, numberOfSpikes);
    }

    public double getSpikeExtensionRatio() {
        return spikeExtensionRatio;
    }

    public boolean isCyclic() {
        return cyclic;
    }

    public void setCyclic(boolean cyclic) {
        this.cyclic = cyclic;
    }
}
