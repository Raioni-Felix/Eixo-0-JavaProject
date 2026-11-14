package com.jogo.armadilhas;

/**
 * Contrato para qualquer entidade ou objeto que atue como fonte de dano no jogo.
 * Segue o Princípio de Segregação de Interfaces (ISP - SOLID).
 */
public interface DamageSource {

    /**
     * Quantidade de dano bruto causado pelo golpe ou armadilha.
     */
    int getDamage();

    /**
     * Intensidade do empurrão (knockback) aplicado ao alvo atingido.
     */
    double getKnockbackForce();

    /**
     * Nome identificador da fonte de dano (para logs, depuração ou mensagens).
     */
    String getName();
}
