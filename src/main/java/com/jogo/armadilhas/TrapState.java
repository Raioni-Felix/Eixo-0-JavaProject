package com.jogo.armadilhas;

/**
 * Estados do ciclo de vida de uma armadilha.
 *
 * Inclui o estado explícito TELEGRAPHING para feedback visual e sonoro antecipado
 * (Game Juice), essencial para o gameplay justo e fluido em jogos de plataforma.
 */
public enum TrapState {

    /**
     * Armadilha em repouso / desarmada / recolhida, aguardando ativação por tempo ou gatilho.
     */
    INACTIVE,

    /**
     * Gatilho físico ou de proximidade foi detectado. Transiciona imediatamente para TELEGRAPHING.
     */
    TRIGGERED,

    /**
     * Fase de alerta visual e sonoro (tremor, brilho, indicador, estalo mecânico).
     * O jogador tem uma janela de tempo para reagir antes de o dano se tornar ativo.
     */
    TELEGRAPHING,

    /**
     * Fase letal ativa. A hitbox de dano está ligada e causa ferimento/empurrão ao contato.
     */
    ACTIVE,

    /**
     * Período de resfriamento / rearme / retorno à posição inicial após o disparo.
     */
    COOLDOWN
}
