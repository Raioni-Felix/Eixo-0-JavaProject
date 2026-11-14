package com.jogo.armadilhas;

/**
 * Interface ouvinte (Observer pattern) para eventos do ciclo de vida das armadilhas.
 *
 * Permite desacoplar a lógica de jogo (física/temporizadores) dos subsistemas
 * de efeitos sonoros (SFX), partículas, animações visuais e câmera shake.
 */
public interface TrapListener {

    /**
     * Disparado sempre que o estado da armadilha for modificado.
     */
    default void onStateChanged(Trap trap, TrapState oldState, TrapState newState) {}

    /**
     * Disparado quando a armadilha inicia a fase de aviso antecipado (telegraphing).
     * Ponto ideal para acionar som de clique/rearme ou brilho vermelho.
     *
     * @param trap Armadilha emissora.
     * @param duration Segundos até a ativação do dano.
     */
    default void onTelegraphStarted(Trap trap, double duration) {}

    /**
     * Disparado no exato instante em que a armadilha se torna letal.
     * Ponto ideal para som de lâmina cortando, queda de pedra ou explosão.
     */
    default void onTriggered(Trap trap) {}

    /**
     * Disparado quando a armadilha acerta um alvo com sucesso.
     */
    default void onDamageDealt(Trap trap, DamageTarget target, int damage) {}

    /**
     * Disparado quando uma armadilha de impacto (ex: {@link BlocoQueCai}) atinge o chão.
     * Ponto ideal para acionar Camera Shake e efeitos de impacto no cenário.
     */
    default void onImpact(Trap trap) {}

    /**
     * Disparado quando a armadilha conclui o ciclo e é restaurada ao estado inicial.
     */
    default void onReset(Trap trap) {}
}
