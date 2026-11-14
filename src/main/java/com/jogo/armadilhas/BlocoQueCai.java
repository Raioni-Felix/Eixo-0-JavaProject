package com.jogo.armadilhas;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Bloco Esmagador que Cai do Teto (Falling Crushing Block / Thwomp style).
 *
 * Características e Comportamento:
 * 1. Gatilho Exato: A triggerBox possui exatamente a mesma largura (width) e alinhamento horizontal
 *    do bloco, cobrindo exclusivamente o espaço vertical logo abaixo dele até o chão. Não aciona se o jogador estiver ao lado.
 * 2. Dano Omnidirecional (100% do Perímetro): A damageHitbox cobre 100% da área/perímetro do bloco (cima, baixo, esquerda e direita).
 *    Qualquer contato do jogador com qualquer face aplica dano e knockback durante a queda, enquanto permanece ativo no solo
 *    e durante todo o ciclo de perigo.
 * 3. Solidez Física: Age como um corpo sólido intransponível no mundo físico.
 */
public class BlocoQueCai extends Trap {

    private final double initialY;
    private double currentY;
    private double velocityY = 0.0;

    private double gravity = 1800.0;
    private double maxFallDistance = 320.0;
    private double retractionSpeed = 120.0; // Velocidade de retorno ao teto
    private double groundStayDuration = 1.5; // Tempo que permanece ativo no solo após o impacto
    private double groundTimer = 0.0;

    private boolean hitGround = false;

    public BlocoQueCai(double x, double y, double width, double height) {
        this(x, y, width, height, 320.0, 0.5, 45);
    }

    public BlocoQueCai(double x, double y, double width, double height,
                       double maxFallDistance, double telegraphDuration, int damage) {
        super("Bloco Esmagador", x, y, width, height);
        this.initialY = y;
        this.currentY = y;
        this.maxFallDistance = maxFallDistance;
        this.telegraphDuration = telegraphDuration;
        this.activeDuration = 10.0; // Tempo máximo de segurança; a transição normal ocorre após atingir o solo + groundStayDuration
        this.cooldownDuration = 3.0; // Duração do retorno ao teto
        this.retractionSpeed = Math.max(120.0, this.maxFallDistance / this.cooldownDuration);
        this.damage = damage;
        this.knockbackForce = 450.0;
        this.shakeIntensity = 5.0; // Tremor vigoroso de aviso no teto
        this.inactiveDuration = -1; // Não é temporizado por ciclo, dispara exclusivamente por proximidade vertical

        // 1. Gatilho Exato: Mesma largura e alinhamento do bloco, projetado estritamente abaixo dele
        this.triggerBox = new Hitbox(0, height, width, maxFallDistance);

        // 2. Dano em todas as direções: Cobre 100% da área/perímetro da entidade do bloco (cima, baixo, esquerda e direita)
        this.damageHitbox = new Hitbox(0, 0, width, height);
    }

    @Override
    protected void updateActive(double deltaTime) {
        // Permanece em ACTIVE durante toda a queda e pelo tempo de permanência no solo (groundStayDuration)
        if (hitGround && groundTimer >= groundStayDuration) {
            transitionTo(TrapState.COOLDOWN);
        } else if (stateTimer >= activeDuration) {
            transitionTo(TrapState.COOLDOWN);
        }
    }

    @Override
    protected void updateCustom(double deltaTime) {
        if (state == TrapState.ACTIVE) {
            if (!hitGround) {
                // Queda acelerada por gravidade
                velocityY += gravity * deltaTime;
                currentY += velocityY * deltaTime;

                // Impacto no chão / distância máxima de curso: permanece ativo no solo e notifica onImpact
                if (currentY >= initialY + maxFallDistance) {
                    currentY = initialY + maxFallDistance;
                    velocityY = 0.0;
                    hitGround = true;
                    groundTimer = 0.0;
                    notifyImpact();
                }
                this.y = currentY;
            } else {
                // No solo: mantém-se ativo e letal em todas as faces antes de iniciar a subida
                groundTimer += deltaTime;
            }
        } else if (state == TrapState.COOLDOWN) {
            // Retorno mecânico constante em direção ao teto
            if (currentY > initialY) {
                currentY -= retractionSpeed * deltaTime;
                if (currentY <= initialY) {
                    currentY = initialY;
                }
                this.y = currentY;
            }
        }
    }

    /**
     * Força a interrupção imediata da queda, notifica o impacto e transiciona para recarga (COOLDOWN).
     */
    public void onImpact() {
        boolean firstImpact = !hitGround;
        hitGround = true;
        velocityY = 0.0;
        groundTimer = 0.0;
        if (firstImpact) {
            notifyImpact();
        }
        transitionTo(TrapState.COOLDOWN);
    }

    @Override
    public void onReset() {
        super.onReset();
        currentY = initialY;
        this.y = initialY;
        velocityY = 0.0;
        groundTimer = 0.0;
        hitGround = false;
    }

    /**
     * Indica se o bloco está em seu ciclo de perigo (caindo, ativo no solo ou retornando).
     */
    public boolean isDangerous() {
        return state == TrapState.ACTIVE || state == TrapState.COOLDOWN;
    }

    @Override
    public boolean checkCollision(DamageTarget target) {
        if (!isDangerous() || target == null || !target.isAlive() || target.isInvulnerable() || damageHitbox == null) {
            return false;
        }
        Hitbox targetBox = target.getHitbox();
        if (targetBox == null) {
            return false;
        }

        // Verifica sobreposição ou contato direto com 100% do perímetro do bloco (cima, baixo, esquerda e direita).
        // A margem de superfície (2px) garante que o contato físico lateral/superior seja detectado mesmo
        // quando a resolução de colisão sólida do Box2D encosta o jogador na face externa do bloco.
        double contactMargin = 2.0;
        double blockMinX = damageHitbox.getMinX(this.x) - contactMargin;
        double blockMaxX = damageHitbox.getMaxX(this.x) + contactMargin;
        double blockMinY = damageHitbox.getMinY(this.y) - contactMargin;
        double blockMaxY = damageHitbox.getMaxY(this.y) + contactMargin;

        double targetMinX = targetBox.getMinX(target.getX());
        double targetMaxX = targetBox.getMaxX(target.getX());
        double targetMinY = targetBox.getMinY(target.getY());
        double targetMaxY = targetBox.getMaxY(target.getY());

        boolean touchingAnyFace = blockMinX <= targetMaxX
                && blockMaxX >= targetMinX
                && blockMinY <= targetMaxY
                && blockMaxY >= targetMinY;

        if (touchingAnyFace) {
            applyDamage(target);
            return true;
        }
        return false;
    }

    @Override
    public void applyDamage(DamageTarget target) {
        if (target == null || !target.isAlive() || target.isInvulnerable()) {
            return;
        }

        target.takeDamage(damage, this);

        // Cálculo de knockback direcional de acordo com a face de contato (cima, baixo, esquerda ou direita)
        double blockCenterX = x + (width / 2.0);
        double blockCenterY = y + (height / 2.0);
        double targetCenterX = target.getX() + target.getHitbox().getOffsetX() + (target.getHitbox().getWidth() / 2.0);
        double targetCenterY = target.getY() + target.getHitbox().getOffsetY() + (target.getHitbox().getHeight() / 2.0);

        double dx = targetCenterX - blockCenterX;
        double dy = targetCenterY - blockCenterY;

        double forceX;
        double forceY;

        // Verifica qual eixo teve maior afastamento relativo para definir a direção do impacto
        if (Math.abs(dy) >= Math.abs(dx)) {
            // Colisão vertical (por baixo ou por cima)
            forceX = (dx >= 0) ? knockbackForce * 0.3 : -knockbackForce * 0.3;
            // Se o alvo estiver abaixo do centro do bloco, arremessa para baixo/lados; se acima, arremessa para cima
            forceY = (dy >= 0) ? knockbackForce : -knockbackForce * 0.75;
        } else {
            // Colisão lateral (laterais esquerda ou direita do bloco)
            forceX = (dx >= 0) ? knockbackForce : -knockbackForce;
            forceY = -knockbackForce * 0.4; // Empurrão diagonal para afastar o jogador
        }

        target.applyKnockback(forceX, forceY);
        target.activateInvulnerability(invulnerabilityGrantDuration);

        for (TrapListener listener : listeners) {
            listener.onDamageDealt(this, target, damage);
        }
    }

    @Override
    protected void renderContent(Graphics g, double drawX, double drawY) {
        Color prev = g.getColor();

        // 1. Corrente ou cabo mecânico de sustentação no teto
        if (currentY > initialY) {
            g.setColor(Color.GRAY);
            int chainX = (int) (drawX + (width / 2.0) - 2);
            g.fillRect(chainX, (int) initialY, 4, (int) (drawY - initialY));
        }

        // 2. Corpo maciço de pedra
        g.setColor(new Color(90, 85, 95));
        g.fillRect((int) drawX, (int) drawY, (int) width, (int) height);

        // Moldura sólida
        g.setColor(new Color(45, 40, 50));
        g.drawRect((int) drawX, (int) drawY, (int) width, (int) height);

        // 3. Dentes/espinhos inferiores e laterais do bloco esmagador
        if (state == TrapState.TELEGRAPHING) {
            g.setColor(new Color(255, 190, 60));
        } else if (state == TrapState.ACTIVE) {
            g.setColor(new Color(220, 40, 40));
        } else {
            g.setColor(new Color(140, 140, 150));
        }

        int teethCount = 4;
        int toothWidth = (int) (width / teethCount);
        int toothHeight = 8;
        int bottomY = (int) (drawY + height - toothHeight);

        for (int i = 0; i < teethCount; i++) {
            int tx = (int) drawX + (i * toothWidth);
            int[] xPoints = { tx, tx + (toothWidth / 2), tx + toothWidth };
            int[] yPoints = { bottomY, (int) (drawY + height), bottomY };
            g.fillPolygon(xPoints, yPoints, 3);
        }

        // 4. Olhos/runas que brilham no aviso (Telegraphing) e na queda (Active)
        if (state == TrapState.TELEGRAPHING || state == TrapState.ACTIVE) {
            g.setColor(state == TrapState.ACTIVE ? Color.RED : Color.ORANGE);
            int eyeSize = 8;
            int eyeY = (int) (drawY + (height * 0.35));
            g.fillRect((int) (drawX + width * 0.22), eyeY, eyeSize, eyeSize);
            g.fillRect((int) (drawX + width * 0.78 - eyeSize), eyeY, eyeSize, eyeSize);
        }

        g.setColor(prev);
    }

    public double getInitialY() {
        return initialY;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getMaxFallDistance() {
        return maxFallDistance;
    }

    public void setMaxFallDistance(double maxFallDistance) {
        this.maxFallDistance = maxFallDistance;
        this.triggerBox.setHeight(maxFallDistance);
        this.retractionSpeed = Math.max(120.0, this.maxFallDistance / Math.max(0.5, this.cooldownDuration));
    }

    /**
     * Ajusta a distância máxima de queda e trava o bloco exatamente na superfície superior do chão
     * (y = topoDoChao - alturaDoBloco), zerando a velocityY sem ultrapassar o piso.
     */
    public void alignWithGroundSurface(double groundTopY) {
        double exactStopY = groundTopY - this.height;
        if (exactStopY > this.initialY) {
            this.maxFallDistance = exactStopY - this.initialY;
            this.triggerBox.setHeight(this.maxFallDistance);
            this.retractionSpeed = Math.max(120.0, this.maxFallDistance / Math.max(0.5, this.cooldownDuration));
        }
        if (this.currentY >= exactStopY) {
            this.currentY = exactStopY;
            this.y = exactStopY;
            this.velocityY = 0.0;
            if (this.state == TrapState.ACTIVE && !this.hitGround) {
                this.hitGround = true;
                this.groundTimer = 0.0;
                notifyImpact();
            }
        }
    }

    public double getGroundStayDuration() {
        return groundStayDuration;
    }

    public void setGroundStayDuration(double groundStayDuration) {
        this.groundStayDuration = groundStayDuration;
    }

    public boolean hasHitGround() {
        return hitGround;
    }
}
