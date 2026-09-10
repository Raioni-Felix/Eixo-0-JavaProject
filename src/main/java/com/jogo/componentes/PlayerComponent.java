package com.jogo.componentes;

/**
 * Componente do jogador.
 *
 * Herda de CharacterComponent (vida, dano recebido, movimento básico) e
 * adiciona o que é específico do player: frames de invencibilidade
 * depois de tomar dano, o sistema de XP/level (metroidvania, sem
 * score), gravidade + pulo (side-view, não birdseye) e os métodos de
 * movimento horizontal chamados pelos controles (ver initInput() em
 * Main.java).
 */

public class PlayerComponent extends CharacterComponent {

    //Atributos de invencibilidade
    protected double invincibilityDuration = 1.0; //segundos de invencibilidade após tomar dano
    private double invincibilityTimer = 0;

    //Atributos de progressão (XP / level)
    protected int level = 1;
    protected int xp = 0;
    protected int xpToNextLevel = 100;

    // --- Física simples (gravidade manual, sem colisão real) ---
    // Isso é um ponto de partida pro lado 2D side-view do jogo: o
    // player cai sozinho até um "chão" fixo (FLOOR_Y) e só pode pular
    // de lá. Não existe colisão de verdade com plataformas ainda — só
    // esse chão único, fixo. Migrar isso pra física de verdade
    // (FXGL/Box2D) depois é tranquilo, porque fica isolado aqui dentro
    // (o resto do jogo — vida, dano, XP, UI — nem sabe como o
    // movimento é feito por baixo dos panos).
    private double velocityY = 0;
    private boolean isOnGround = false;

    private static final double GRAVITY = 1200;    // aceleração pra baixo (px/s^2)
    private static final double JUMP_FORCE = -500;  // velocidade inicial do pulo (negativo = pra cima)
    private static final double FLOOR_Y = 400;      // altura fixa do chão, por enquanto

    //CONSTRUTOR
    //Super chama o construtor da superclasse CharacterComponent
    public PlayerComponent(String name, int maxHealth, double moveSpeed) {
        super(name, maxHealth, moveSpeed);
    }

    //onUpdate roda a cada frame do jogo: conta o tempo de
    //invencibilidade e aplica a gravidade.
    @Override
    public void onUpdate(double tpf) {
        if (invincibilityTimer > 0) {
            invincibilityTimer -= tpf;
        }

        applyGravity(tpf);
    }

    // Acumula velocidade vertical a cada frame (gravidade) e move a
    // entidade por ela. Ao "tocar" o chão fixo, trava a posição em
    // FLOOR_Y e zera a velocidade — senão o player ia atravessar o
    // chão (overshoot) e ficar oscilando pra sempre.
    private void applyGravity(double tpf) {
        velocityY += GRAVITY * tpf;
        entity.translateY(velocityY * tpf);

        if (entity.getY() >= FLOOR_Y) {
            entity.setY(FLOOR_Y);
            velocityY = 0;
            isOnGround = true;
        } else {
            isOnGround = false;
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
    //Chamados pelas UserAction dos controles em initInput().
    //tpf (time per frame) garante que a velocidade seja igual em
    //qualquer framerate.

    //Só pula se estiver no chão — evita pulo duplo/infinito no ar.
    public void jump() {
        if (isOnGround) {
            velocityY = JUMP_FORCE;
            isOnGround = false;
        }
    }

    public void moveLeft(double tpf) {
        entity.translateX(-moveSpeed * tpf);
    }

    public void moveRight(double tpf) {
        entity.translateX(moveSpeed * tpf);
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
