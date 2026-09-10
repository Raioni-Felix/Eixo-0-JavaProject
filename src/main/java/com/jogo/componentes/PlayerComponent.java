package com.jogo.componentes;

import com.almasb.fxgl.physics.PhysicsComponent;

/**
 * Componente do jogador.
 *
 * Herda de CharacterComponent (vida, dano recebido, movimento básico) e
 * adiciona o que é específico do player: frames de invencibilidade
 * depois de tomar dano, o sistema de XP/level (metroidvania, sem
 * score) e o movimento (agora via física de verdade, não mais
 * translate na mão).
 */
public class PlayerComponent extends CharacterComponent {

    //Atributos de invencibilidade
    protected double invincibilityDuration = 1.0; //segundos de invencibilidade após tomar dano
    private double invincibilityTimer = 0;

    //Atributos de progressão (XP / level)
    protected int level = 1;
    protected int xp = 0;
    protected int xpToNextLevel = 100;

    // Velocidade vertical do pulo (negativo = pra cima; no FXGL/Box2D o
    // eixo Y cresce pra baixo, igual tela). A gravidade em si não é
    // mais calculada na mão aqui — quem cuida disso agora é o motor de
    // física (ver Main.initPhysics(), que define a gravidade do
    // PhysicsWorld uma vez só, pra todo mundo que tiver física).
    private static final double JUMP_SPEED = -500;

    // PhysicsComponent do FXGL: é ele quem realmente move a entidade
    // agora (gravidade, colisão com plataformas). Pego a referência em
    // onAdded() — chamado quando ESTE componente é anexado à entidade,
    // momento em que o PhysicsComponent já foi anexado antes dele (ver
    // ordem dos .with() em FabricaEntidades.spawnJogador()).
    private PhysicsComponent physics;

    public PlayerComponent(String name, int maxHealth, double moveSpeed) {
        super(name, maxHealth, moveSpeed);
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        if (invincibilityTimer > 0) {
            invincibilityTimer -= tpf;
        }
    }

    //Sobrescreve takeDamage pra ignorar dano enquanto estiver invencível
    //e reiniciar o timer sempre que um hit for realmente aplicado
    @Override
    public void takeDamage(int quant) {
        if (isInvincible()) {
            return;
        }

        super.takeDamage(quant);
        invincibilityTimer = invincibilityDuration;
    }

    public boolean isInvincible() {
        return invincibilityTimer > 0;
    }

    //MOVIMENTO
    //Agora tudo passa pelo PhysicsComponent (setVelocityX/Y) — com
    //física de verdade a posição é controlada pelo motor, então mexer
    //nela na mão (entity.translateX, setX...) seria ignorado ou
    //brigaria com o Box2D.

    //Só pula se physics.isOnGround() disser que está no chão (sensor
    //colado nos pés, configurado na FabricaEntidades) — evita pulo
    //infinito no ar.
    public void jump() {
        if (physics.isOnGround()) {
            physics.setVelocityY(JUMP_SPEED);
        }
    }

    public void moveLeft() {
        physics.setVelocityX(-moveSpeed);
    }

    public void moveRight() {
        physics.setVelocityX(moveSpeed);
    }

    //Chamado quando A ou D é solto (onActionEnd em Main.initInput()) —
    //zera a velocidade horizontal. Se a outra tecla ainda estiver
    //segurada, o onAction dela nesse mesmo frame corrige de novo, então
    //isso não trava/interrompe o movimento contínuo.
    public void stopHorizontal() {
        physics.setVelocityX(0);
    }

    //XP / LEVEL
    //Metroidvania não tem score, então progressão vem de subir de
    //nível: ganhar XP suficiente aumenta o level e melhora os
    //atributos do personagem (vida e velocidade), curando ele.
    public void addXp(int quant) {
        xp += quant;

        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel;
            levelUp();
        }
    }

    private void levelUp() {
        level++;

        //Aumenta os atributos base a cada level e cura o player
        maxHealth += 20;
        currentHealth = maxHealth;
        moveSpeed += 5;

        //Próximo nível exige mais XP (curva simples, ajustável depois)
        xpToNextLevel = 100 + (level - 1) * 50;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                "hp=" + currentHealth + "/" + maxHealth +
                ", lvl=" + level +
                ", xp=" + xp + "/" + xpToNextLevel +
                '}';
    }
}
