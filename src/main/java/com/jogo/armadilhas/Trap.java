package com.jogo.armadilhas;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstrata base para todas as armadilhas do jogo.
 *
 * Implementa {@link DamageSource} para seguir o princípio da responsabilidade única (SRP)
 * e o princípio aberto/fechado (OCP), permitindo que novas armadilhas sejam adicionadas
 * sem modificar o loop de jogo ou os sistemas existentes.
 *
 * Arquitetura e Ciclo de Vida:
 * 1. INACTIVE     -> Armadilha armada ou oculta.
 * 2. TRIGGERED    -> Gatilho acionado (por tempo ou presença do jogador na triggerBox).
 * 3. TELEGRAPHING -> Aviso antecipado (Game Juice: tremor e indicador sonoro/visual).
 * 4. ACTIVE       -> Hitbox de dano ativa, infligindo dano a alvos que a tocarem.
 * 5. COOLDOWN     -> Período de desativação / rearme antes de retornar a INACTIVE.
 */
public abstract class Trap implements DamageSource {

    // Identificação e posição básica no mundo
    protected final String name;
    protected double x;
    protected double y;
    protected double width;
    protected double height;

    // Caixas de colisão separadas: Hitbox de DANO vs TriggerBox de ATIVAÇÃO
    protected Hitbox damageHitbox;
    protected Hitbox triggerBox;

    // Estado e temporizadores do ciclo de vida
    protected TrapState state = TrapState.INACTIVE;
    protected double stateTimer = 0.0;

    protected double inactiveDuration = 2.0;  // Para armadilhas com ciclo automático
    protected double telegraphDuration = 0.5; // Duração do aviso antes do dano
    protected double activeDuration = 1.0;    // Duração da fase letal
    protected double cooldownDuration = 2.0;  // Duração da recarga após o ataque

    // Propriedades de combate
    protected int damage = 20;
    protected double knockbackForce = 350.0;
    protected double invulnerabilityGrantDuration = 1.0; // 1.0s de I-Frames concedidos ao jogador após sofrer dano

    // Game Juice: tremor visual gerado durante o estado de TELEGRAPHING
    protected double shakeIntensity = 3.0;
    protected double shakeOffsetX = 0.0;
    protected double shakeOffsetY = 0.0;

    // Lista de ouvintes (Observer pattern) para áudio, partículas e telas
    protected final List<TrapListener> listeners = new ArrayList<>();

    // Permite ligar/desligar renderização de hitboxes para depuração
    protected boolean debugHitboxes = false;

    /**
     * Construtor base da armadilha.
     *
     * @param name Nome representativo da armadilha.
     * @param x Posição horizontal no mundo.
     * @param y Posição vertical no mundo.
     * @param width Largura da área visual da armadilha.
     * @param height Altura da área visual da armadilha.
     */
    public Trap(String name, double x, double y, double width, double height) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome da armadilha não pode ser nulo ou vazio.");
        }
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // Por padrão, a hitbox de dano possui uma margem de segurança (offset) de 2px
        // para nunca parecer punitiva injustamente nas bordas do sprite
        this.damageHitbox = new Hitbox(2, 2, Math.max(0, width - 4), Math.max(0, height - 4));

        // Por padrão, a triggerBox cobre a própria área da armadilha
        this.triggerBox = new Hitbox(0, 0, width, height);
    }

    // =========================================================================
    // CICLO DE VIDA (Lifecycle Methods)
    // =========================================================================

    /**
     * Atualização de lógica e temporizadores baseada em deltaTime (em segundos).
     * Não bloqueia a thread principal em nenhum momento.
     */
    public void update(double deltaTime) {
        if (deltaTime <= 0) {
            return;
        }

        stateTimer += deltaTime;

        switch (state) {
            case INACTIVE -> updateInactive(deltaTime);
            case TRIGGERED -> {
                // Ao ser acionado, passa imediatamente para o aviso prévio (telegraphing)
                transitionTo(TrapState.TELEGRAPHING);
            }
            case TELEGRAPHING -> updateTelegraphing(deltaTime);
            case ACTIVE -> updateActive(deltaTime);
            case COOLDOWN -> updateCooldown(deltaTime);
        }

        // Gancho polimórfico para comportamento específico de cada subclasse a cada tick
        updateCustom(deltaTime);
    }

    /**
     * Comportamento durante o estado inativo (ex: armadilhas cíclicas disparam por timer).
     */
    protected void updateInactive(double deltaTime) {
        if (inactiveDuration > 0 && stateTimer >= inactiveDuration) {
            onTrigger();
        }
    }

    /**
     * Comportamento durante o estado de alerta (TELEGRAPHING).
     * Aplica cálculo matemático de tremor (Game Juice) e checa fim do tempo de aviso.
     */
    protected void updateTelegraphing(double deltaTime) {
        // Cálculo senoidal em alta frequência para vibrar o visual
        double frequency = 45.0;
        shakeOffsetX = Math.sin(stateTimer * frequency) * shakeIntensity;
        shakeOffsetY = Math.cos(stateTimer * frequency) * (shakeIntensity * 0.5);

        if (stateTimer >= telegraphDuration) {
            // Fim do aviso: zera o tremor e inicia a fase de ataque real
            shakeOffsetX = 0.0;
            shakeOffsetY = 0.0;
            transitionTo(TrapState.ACTIVE);
            notifyTriggered();
        }
    }

    /**
     * Comportamento durante a fase letal (ACTIVE).
     */
    protected void updateActive(double deltaTime) {
        if (stateTimer >= activeDuration) {
            transitionTo(TrapState.COOLDOWN);
        }
    }

    /**
     * Comportamento durante a fase de recarga (COOLDOWN).
     */
    protected void updateCooldown(double deltaTime) {
        if (stateTimer >= cooldownDuration) {
            onReset();
        }
    }

    /**
     * Disparado quando a armadilha é ativada.
     */
    public void onTrigger() {
        if (state == TrapState.INACTIVE) {
            transitionTo(TrapState.TRIGGERED);
            transitionTo(TrapState.TELEGRAPHING);
        }
    }

    /**
     * Restaura a armadilha para o estado inicial pronto para novo disparo.
     */
    public void onReset() {
        shakeOffsetX = 0.0;
        shakeOffsetY = 0.0;
        transitionTo(TrapState.INACTIVE);
        for (TrapListener listener : listeners) {
            listener.onReset(this);
        }
    }

    /**
     * Transiciona o estado e notifica os observadores.
     */
    protected void transitionTo(TrapState newState) {
        TrapState oldState = this.state;
        this.state = newState;
        this.stateTimer = 0.0;

        if (newState == TrapState.TELEGRAPHING) {
            notifyTelegraphStarted(telegraphDuration);
        }

        for (TrapListener listener : listeners) {
            listener.onStateChanged(this, oldState, newState);
        }
    }

    // =========================================================================
    // DETECÇÃO DE GATILHOS E COLISÕES
    // =========================================================================

    /**
     * Verifica se o alvo entrou na área de ativação (triggerBox).
     * Se entrar e a armadilha estiver INACTIVE, dispara onTrigger().
     */
    public boolean checkTrigger(DamageTarget target) {
        if (target == null || !target.isAlive() || state != TrapState.INACTIVE) {
            return false;
        }

        boolean inside = triggerBox.intersects(x, y, target.getHitbox(), target.getX(), target.getY());
        if (inside) {
            onTrigger();
            return true;
        }
        return false;
    }

    /**
     * Verifica se o alvo tocou na hitbox de dano enquanto a armadilha está ACTIVE.
     * Caso positivo e o alvo não esteja em I-Frames, aplica o dano e empurrão.
     */
    public boolean checkCollision(DamageTarget target) {
        if (target == null || !target.isAlive() || target.isInvulnerable() || state != TrapState.ACTIVE) {
            return false;
        }

        boolean hit = damageHitbox.intersects(x, y, target.getHitbox(), target.getX(), target.getY());
        if (hit) {
            applyDamage(target);
            return true;
        }
        return false;
    }

    /**
     * Aplica o dano, calcula o vetor de knockback em direção oposta à armadilha
     * e ativa a janela de invulnerabilidade temporária (I-Frames) no alvo.
     */
    public void applyDamage(DamageTarget target) {
        if (target == null || !target.isAlive() || target.isInvulnerable()) {
            return;
        }

        target.takeDamage(damage, this);

        // Calcula direção do empurrão horizontal baseado nos centros
        double trapCenterX = x + damageHitbox.getOffsetX() + (damageHitbox.getWidth() / 2.0);
        double targetCenterX = target.getX() + target.getHitbox().getOffsetX() + (target.getHitbox().getWidth() / 2.0);

        double forceX = (targetCenterX >= trapCenterX) ? knockbackForce : -knockbackForce;
        double forceY = -Math.abs(knockbackForce * 0.75); // Lança ligeiramente para cima

        target.applyKnockback(forceX, forceY);
        target.activateInvulnerability(invulnerabilityGrantDuration);

        for (TrapListener listener : listeners) {
            listener.onDamageDealt(this, target, damage);
        }
    }

    // =========================================================================
    // RENDERIZAÇÃO E GAME JUICE
    // =========================================================================

    /**
     * Renderiza a armadilha aplicando o tremor visual (shake) durante o telegraphing.
     * Permite desenhar com o pacote padrão Java 2D Graphics.
     */
    public void render(Graphics g) {
        double drawX = x + shakeOffsetX;
        double drawY = y + shakeOffsetY;

        // Renderiza o conteúdo customizado (sprites, formas, detalhes de cada armadilha)
        renderContent(g, drawX, drawY);

        // Feedback de telegraphing visual (alerta vermelho translúcido / aviso visual)
        if (state == TrapState.TELEGRAPHING) {
            renderTelegraphEffect(g, drawX, drawY);
        }

        // Renderização de caixas de colisão para modo debug
        if (debugHitboxes && g instanceof Graphics2D g2d) {
            renderDebugBounds(g2d);
        }
    }

    /**
     * Gancho obrigatório para cada subclasse desenhar seu sprite/corpo.
     * [PONTO DE INSERÇÃO DE FRAMES DE SPRITE/TEXTURA DA ARTE]
     */
    protected abstract void renderContent(Graphics g, double drawX, double drawY);

    /**
     * Gancho de renderização do efeito de aviso (Game Juice).
     */
    protected void renderTelegraphEffect(Graphics g, double drawX, double drawY) {
        Color originalColor = g.getColor();
        // Brilho pulsante amarelo/vermelho translúcido sobre a armadilha
        float pulse = (float) (Math.abs(Math.sin(stateTimer * 10.0)) * 0.4 + 0.2);
        g.setColor(new Color(1.0f, 0.2f, 0.2f, pulse));
        g.fillRect((int) drawX, (int) drawY, (int) width, (int) height);
        g.setColor(originalColor);
    }

    /**
     * Desenha as hitboxes de dano (vermelho) e trigger box (amarelo) para inspeção.
     */
    protected void renderDebugBounds(Graphics2D g2d) {
        Color prev = g2d.getColor();

        // TriggerBox em amarelo pontilhado
        g2d.setColor(new Color(255, 215, 0, 140));
        g2d.drawRect((int) triggerBox.getMinX(x), (int) triggerBox.getMinY(y),
                (int) triggerBox.getWidth(), (int) triggerBox.getHeight());

        // Hitbox de dano em vermelho
        g2d.setColor(state == TrapState.ACTIVE ? new Color(255, 0, 0, 200) : new Color(255, 100, 100, 80));
        g2d.drawRect((int) damageHitbox.getMinX(x), (int) damageHitbox.getMinY(y),
                (int) damageHitbox.getWidth(), (int) damageHitbox.getHeight());

        g2d.setColor(prev);
    }

    /**
     * Gancho opcional para lógica extra no update de cada subclasse.
     */
    protected void updateCustom(double deltaTime) {}

    // =========================================================================
    // OBSERVER / LISTENERS
    // =========================================================================

    public void addListener(TrapListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(TrapListener listener) {
        listeners.remove(listener);
    }

    protected void notifyTelegraphStarted(double duration) {
        // [PONTO DE INSERÇÃO DE EFEITO SONORO (SFX) DE AVISO / CLIQUE MECÂNICO]
        for (TrapListener listener : listeners) {
            listener.onTelegraphStarted(this, duration);
        }
    }

    protected void notifyTriggered() {
        // [PONTO DE INSERÇÃO DE EFEITO SONORO (SFX) DE IMPACTO / LÂMINA / TIRO]
        for (TrapListener listener : listeners) {
            listener.onTriggered(this);
        }
    }

    protected void notifyImpact() {
        for (TrapListener listener : listeners) {
            listener.onImpact(this);
        }
    }

    public double getInvulnerabilityGrantDuration() {
        return invulnerabilityGrantDuration;
    }

    public void setInvulnerabilityGrantDuration(double invulnerabilityGrantDuration) {
        this.invulnerabilityGrantDuration = Math.max(0.0, invulnerabilityGrantDuration);
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================

    @Override
    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    @Override
    public double getKnockbackForce() {
        return knockbackForce;
    }

    public void setKnockbackForce(double knockbackForce) {
        this.knockbackForce = knockbackForce;
    }

    @Override
    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Hitbox getDamageHitbox() {
        return damageHitbox;
    }

    public void setDamageHitbox(Hitbox damageHitbox) {
        this.damageHitbox = damageHitbox;
    }

    public Hitbox getTriggerBox() {
        return triggerBox;
    }

    public void setTriggerBox(Hitbox triggerBox) {
        this.triggerBox = triggerBox;
    }

    public TrapState getState() {
        return state;
    }

    public double getStateTimer() {
        return stateTimer;
    }

    public double getInactiveDuration() {
        return inactiveDuration;
    }

    public void setInactiveDuration(double inactiveDuration) {
        this.inactiveDuration = inactiveDuration;
    }

    public double getTelegraphDuration() {
        return telegraphDuration;
    }

    public void setTelegraphDuration(double telegraphDuration) {
        this.telegraphDuration = telegraphDuration;
    }

    public double getActiveDuration() {
        return activeDuration;
    }

    public void setActiveDuration(double activeDuration) {
        this.activeDuration = activeDuration;
    }

    public double getCooldownDuration() {
        return cooldownDuration;
    }

    public void setCooldownDuration(double cooldownDuration) {
        this.cooldownDuration = cooldownDuration;
    }

    public boolean isDebugHitboxes() {
        return debugHitboxes;
    }

    public void setDebugHitboxes(boolean debugHitboxes) {
        this.debugHitboxes = debugHitboxes;
    }

    public double getShakeOffsetX() {
        return shakeOffsetX;
    }

    public double getShakeOffsetY() {
        return shakeOffsetY;
    }
}
