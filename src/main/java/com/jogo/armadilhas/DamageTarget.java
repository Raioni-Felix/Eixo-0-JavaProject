package com.jogo.armadilhas;

/**
 * Interface que representa um alvo passível de receber dano e knockback de armadilhas.
 *
 * Desacopla o sistema de armadilhas das implementações específicas da engine gráfica
 * ou da classe concreta do Jogador, facilitando testes unitários e extensibilidade.
 */
public interface DamageTarget {

    /**
     * Aplica o dano recebido ao alvo.
     *
     * @param damage Quantidade de vida a subtrair.
     * @param source Fonte causadora do dano.
     */
    void takeDamage(int damage, DamageSource source);

    /**
     * Aplica um vetor de empurrão (knockback) ao alvo.
     *
     * @param forceX Componente horizontal da força.
     * @param forceY Componente vertical da força.
     */
    void applyKnockback(double forceX, double forceY);

    /**
     * Posição horizontal do alvo no mundo.
     */
    double getX();

    /**
     * Posição vertical do alvo no mundo.
     */
    double getY();

    /**
     * Hitbox física de detecção de colisão do alvo.
     */
    Hitbox getHitbox();

    /**
     * Indica se o alvo ainda está vivo e ativo para receber dano.
     */
    boolean isAlive();

    /**
     * Indica se o alvo está em janela de invulnerabilidade temporária (I-Frames).
     */
    default boolean isInvulnerable() {
        return false;
    }

    /**
     * Ativa a invulnerabilidade temporária (I-Frames) no alvo pela duração especificada (em segundos).
     */
    default void activateInvulnerability(double durationSeconds) {
    }
}
