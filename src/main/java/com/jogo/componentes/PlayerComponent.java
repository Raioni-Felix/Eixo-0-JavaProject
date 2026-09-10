package com.jogo.componentes;

/**
 * Componente do jogador.
 *
 * Herda de CharacterComponent (vida, dano recebido, movimento básico) e
 * adiciona o que é específico do player: frames de invencibilidade
 * depois de tomar dano, o sistema de XP/level (metroidvania, sem
 * score) e os métodos de movimento chamados pelos controles
 * (ver initInput() em Main.java).
 */

public class PlayerComponent extends CharacterComponent {

    //Atributos de invencibilidade
    protected double invincibilityDuration = 1.0; //segundos de invencibilidade após tomar dano
    private double invincibilityTimer = 0;

    //Atributos de progressão (XP / level)
    protected int level = 1;
    protected int xp = 0;
    protected int xpToNextLevel = 100;

    //CONSTRUTOR
    //Super chama o construtor da superclasse CharacterComponent
    public PlayerComponent(String name, int maxHealth, double moveSpeed) {
        super(name, maxHealth, moveSpeed);
    }

    //onUpdate roda a cada frame do jogo, usado aqui só pra contar o
    //tempo de invencibilidade
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
    //Chamados pelas UserAction dos controles (WASD) em initInput().
    //tpf (time per frame) garante que a velocidade seja igual em
    //qualquer framerate.
    public void moveUp(double tpf) {
        entity.translateY(-moveSpeed * tpf);
    }

    public void moveDown(double tpf) {
        entity.translateY(moveSpeed * tpf);
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
