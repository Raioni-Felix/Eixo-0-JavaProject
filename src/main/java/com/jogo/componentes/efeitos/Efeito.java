package com.jogo.componentes.efeitos;

import com.jogo.componentes.ComponentePersonagem;

/**
 * Interface para efeitos de status que podem ser aplicados a personagens.
 * Efeitos têm duração, podem ser atualizados a cada frame e sabem se aplicar/remover.
 */
public interface Efeito {

    /**
     * Aplica o efeito no alvo (ex: reduz velocidade, inicia dano contínuo).
     */
    void aplicar(ComponentePersonagem alvo);

    /**
     * Remove o efeito do alvo (ex: restaura velocidade, para dano contínuo).
     */
    void remover(ComponentePersonagem alvo);

    /**
     * Atualiza o efeito a cada frame. Retorna true se o efeito deve continuar,
     * false se terminou naturalmente e deve ser removido.
     */
    boolean atualizar(ComponentePersonagem alvo, double tpf);

    /**
     * Se o efeito está ativo no momento.
     */
    boolean estaAtivo();

    /**
     * Nome identificador do efeito (para debug/HUD).
     */
    String getNome();
}