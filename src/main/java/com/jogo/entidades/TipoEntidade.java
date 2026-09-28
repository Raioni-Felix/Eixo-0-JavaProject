package com.jogo.entidades;

/**
 * Tipos de entidade do jogo, usado pela FabricaEntidades (.type(...))
 * e disponível pra quem precisar filtrar/consultar entidades por tipo
 * (ex.: colisões, getGameWorld().getEntitiesByType(...)).
 */


public enum TipoEntidade {
    JOGADOR,
    INIMIGO_VOADOR,
    INIMIGO_DISTANCIA,
    INIMIGO_MELEE,
    PROJETIL,
    PLATAFORMA,
    ATAQUE_JOGADOR,
    GATILHO
}
