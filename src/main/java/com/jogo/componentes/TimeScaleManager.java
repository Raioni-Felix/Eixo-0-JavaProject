package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;

/**
 * Gerencia o time scale global do jogo para o SandevistanMichyle.
 * Quando ativo, o tempo passa a 25% (0.25x) para todas as entidades
 * EXCETO o jogador que tem uma leve redução (0.9x) para sentir o efeito.
 * 
 * Projéteis do jogador, inimigos, partículas, chefe, etc. usam getScale() no onUpdate.
 * O jogador verifica isAtivo() para saber se deve ignorar o scale.
 */
public final class TimeScaleManager {

    private static final double SANDEVISTAN_SCALE = 0.25; // 25% velocidade (inimigos/projéteis)
    private static final double JOGADOR_SCALE = 0.90;     // 90% velocidade (leve redução no jogador)
    private static final double NORMAL_SCALE = 1.0;

    private static boolean ativo = false;
    private static ComponenteJogador jogadorReferencia = null;

    private TimeScaleManager() {
    }

    /**
     * Ativa/desativa o modo Sandevistan.
     */
    public static void setAtivo(boolean ativo, ComponenteJogador jogador) {
        TimeScaleManager.ativo = ativo;
        if (ativo) {
            jogadorReferencia = jogador;
        } else {
            jogadorReferencia = null;
        }
    }

    /**
     * Retorna true se o Sandevistan está ativo.
     */
    public static boolean isAtivo() {
        return ativo;
    }

    /**
     * Retorna o multiplicador de tempo para inimigos/projéteis/partículas.
     * - 0.25 se Sandevistan ativo
     * - 1.0 se normal
     */
    public static double getScale() {
        return ativo ? SANDEVISTAN_SCALE : NORMAL_SCALE;
    }

    /**
     * Retorna o multiplicador de tempo para o jogador.
     * - 0.90 se Sandevistan ativo (leve redução)
     * - 1.0 se normal
     */
    public static double getJogadorScale() {
        return ativo ? JOGADOR_SCALE : NORMAL_SCALE;
    }

    /**
     * Verifica se a entidade passada é o jogador (para saber qual scale aplicar).
     */
    public static boolean isJogador(ComponentePersonagem entidade) {
        return entidade instanceof ComponenteJogador && entidade == jogadorReferencia;
    }

    /**
     * Verifica se uma entidade (Entity do FXGL) é o jogador.
     */
    public static boolean isJogador(Entity entidade) {
        if (entidade == null || jogadorReferencia == null) {
            return false;
        }
        return entidade == jogadorReferencia.getEntity();
    }

    /**
     * Retorna o tpf ajustado para a entidade.
     * Jogador: leve redução (0.90x). Outros: slow-motion forte (0.25x).
     */
    public static double getTpfPara(double tpf, ComponentePersonagem entidade) {
        if (isJogador(entidade)) {
            return tpf * getJogadorScale(); // jogador com leve redução
        }
        return tpf * getScale(); // inimigos/projéteis com slow forte
    }

    /**
     * Retorna o tpf ajustado para uma Entity genérica.
     */
    public static double getTpfPara(double tpf, Entity entidade) {
        if (isJogador(entidade)) {
            return tpf * getJogadorScale();
        }
        return tpf * getScale();
    }

    /**
     * Reseta o time scale para estado normal (usado ao reiniciar jogo).
     * Garante que não haja velocidade residual do Sandevistan.
     */
    public static void reset() {
        ativo = false;
        jogadorReferencia = null;
    }
}