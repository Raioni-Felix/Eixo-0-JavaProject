package com.jogo.armadilhas;

import com.almasb.fxgl.entity.Entity;
import com.jogo.componentes.ComponenteJogador;

/**
 * Adaptador (Adapter pattern) que conecta a entidade Jogador do FXGL
 * à interface desacoplada {@link DamageTarget} do sistema de armadilhas,
 * incluindo controle de Invulnerabilidade Temporária (I-Frames de 1.0s)
 * com efeito visual de piscar/opacidade.
 */
public class PlayerDamageTargetAdapter implements DamageTarget {

    public static final double DEFAULT_IFRAME_DURATION = 1.0;
    private static final double BLINK_INTERVAL = 0.08;
    private static final double BLINK_LOW_OPACITY = 0.3;

    private final Entity playerEntity;
    private final ComponenteJogador componenteJogador;
    private final Hitbox hitbox;
    private Entity trapEntity;
    private double invulnerabilityTimer = 0.0;

    public PlayerDamageTargetAdapter(Entity playerEntity) {
        this(playerEntity, null);
    }

    public PlayerDamageTargetAdapter(Entity playerEntity, Entity trapEntity) {
        if (playerEntity == null) {
            throw new IllegalArgumentException("Entidade do jogador não pode ser nula.");
        }
        this.playerEntity = playerEntity;
        this.trapEntity = trapEntity;
        this.componenteJogador = playerEntity.hasComponent(ComponenteJogador.class)
                ? playerEntity.getComponent(ComponenteJogador.class)
                : null;

        // Bounding box completa do jogador para detecção fiel em todas as direções
        double w = playerEntity.getWidth() > 0 ? playerEntity.getWidth() : 36.0;
        double h = playerEntity.getHeight() > 0 ? playerEntity.getHeight() : 72.0;
        this.hitbox = new Hitbox(0, 0, w, h);
    }

    public void setTrapEntity(Entity trapEntity) {
        this.trapEntity = trapEntity;
    }

    /**
     * Atualiza o temporizador de I-Frames e o efeito visual de piscar/opacidade.
     */
    public void update(double deltaTime) {
        if (deltaTime <= 0) {
            return;
        }
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer = Math.max(0.0, invulnerabilityTimer - deltaTime);
            try {
                if (playerEntity.getViewComponent() != null) {
                    if (invulnerabilityTimer <= 0) {
                        playerEntity.getViewComponent().setOpacity(1.0);
                    } else {
                        boolean lowOpacity = (((int) Math.floor(invulnerabilityTimer / BLINK_INTERVAL)) % 2) == 0;
                        playerEntity.getViewComponent().setOpacity(lowOpacity ? BLINK_LOW_OPACITY : 1.0);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean isInvulnerable() {
        return invulnerabilityTimer > 0
                || (componenteJogador != null && (componenteJogador.estaInvencivel() || componenteJogador.estaAparando()));
    }

    @Override
    public void activateInvulnerability(double durationSeconds) {
        if (durationSeconds <= 0) {
            return;
        }
        this.invulnerabilityTimer = Math.max(this.invulnerabilityTimer, durationSeconds);
        if (componenteJogador != null) {
            componenteJogador.ativarInvulnerabilidade(durationSeconds);
        }
    }

    @Override
    public void takeDamage(int damage, DamageSource source) {
        if (isInvulnerable()) {
            return;
        }
        if (componenteJogador != null) {
            Entity sourceEntity = (trapEntity != null) ? trapEntity : playerEntity;
            componenteJogador.serAtingido(sourceEntity, damage);
        }
        activateInvulnerability(DEFAULT_IFRAME_DURATION);
    }

    @Override
    public void applyKnockback(double forceX, double forceY) {
        // O ComponenteJogador já calcula e aplica knockback com física no serAtingido()
    }

    @Override
    public double getX() {
        return playerEntity.getX();
    }

    @Override
    public double getY() {
        return playerEntity.getY();
    }

    @Override
    public Hitbox getHitbox() {
        // Atualiza dimensões caso o bounding box da entidade seja definido após a construção
        if (playerEntity.getWidth() > 0 && playerEntity.getHeight() > 0) {
            hitbox.setWidth(playerEntity.getWidth());
            hitbox.setHeight(playerEntity.getHeight());
        }
        return hitbox;
    }

    @Override
    public boolean isAlive() {
        return playerEntity.isActive() && (componenteJogador == null || !componenteJogador.estaMorto());
    }

    public double getInvulnerabilityTimer() {
        return invulnerabilityTimer;
    }
}
