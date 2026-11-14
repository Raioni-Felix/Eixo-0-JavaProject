package com.jogo.armadilhas;

import com.almasb.fxgl.dsl.FXGL;

/**
 * Implementação concreta de {@link TrapListener} integrada ao motor FXGL
 * para fornecer feedback de impacto ("Game Feel") e reforço de proteção:
 * <ul>
 *   <li><b>Camera Shake no Impacto ({@code onImpact}):</b> Dispara {@code viewport.shakeTranslational(...)}
 *       quando um {@link BlocoQueCai} atinge o solo.</li>
 *   <li><b>Camera Shake e I-Frames no Dano ({@code onDamageDealt}):</b> Dispara tremor de câmera
 *       e garante {@code 1.0s} de invulnerabilidade temporária no alvo atingido.</li>
 * </ul>
 */
public class FxglTrapFeedbackListener implements TrapListener {

    public static final double DEFAULT_IMPACT_SHAKE_POWER = 4.0;
    public static final double DEFAULT_DAMAGE_SHAKE_POWER = 6.0;
    public static final double DEFAULT_IFRAME_DURATION = 1.0;

    private final double impactShakePower;
    private final double damageShakePower;
    private final double invulnerabilityDuration;

    private int impactShakeCount = 0;
    private int damageShakeCount = 0;
    private double lastShakePower = 0.0;

    public FxglTrapFeedbackListener() {
        this(DEFAULT_IMPACT_SHAKE_POWER, DEFAULT_DAMAGE_SHAKE_POWER, DEFAULT_IFRAME_DURATION);
    }

    public FxglTrapFeedbackListener(double impactShakePower, double damageShakePower, double invulnerabilityDuration) {
        this.impactShakePower = Math.max(0.0, impactShakePower);
        this.damageShakePower = Math.max(0.0, damageShakePower);
        this.invulnerabilityDuration = Math.max(0.0, invulnerabilityDuration);
    }

    @Override
    public void onImpact(Trap trap) {
        impactShakeCount++;
        lastShakePower = impactShakePower;
        triggerCameraShake(impactShakePower);
    }

    @Override
    public void onDamageDealt(Trap trap, DamageTarget target, int damage) {
        if (target != null && invulnerabilityDuration > 0) {
            target.activateInvulnerability(invulnerabilityDuration);
        }
        damageShakeCount++;
        lastShakePower = damageShakePower;
        triggerCameraShake(damageShakePower);
    }

    private void triggerCameraShake(double power) {
        if (power <= 0) {
            return;
        }
        try {
            var viewport = FXGL.getGameScene().getViewport();
            if (viewport != null) {
                viewport.shakeTranslational(power);
            }
        } catch (Exception ignored) {
            // Ambiente de teste unitário sem GameScene inicializada no FXGL
        }
    }

    public int getImpactShakeCount() {
        return impactShakeCount;
    }

    public int getDamageShakeCount() {
        return damageShakeCount;
    }

    public double getLastShakePower() {
        return lastShakePower;
    }

    public double getImpactShakePower() {
        return impactShakePower;
    }

    public double getDamageShakePower() {
        return damageShakePower;
    }

    public double getInvulnerabilityDuration() {
        return invulnerabilityDuration;
    }
}
