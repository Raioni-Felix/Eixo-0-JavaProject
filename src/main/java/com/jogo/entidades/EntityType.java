package com.jogo.entidades;

/**
 * Tipos de entidade do jogo, usado pela FabricaEntidades (.type(...))
 * e disponível pra quem precisar filtrar/consultar entidades por tipo
 * (ex.: colisões, getGameWorld().getEntitiesByType(...)).
 */


public enum EntityType {
    JOGADOR,
    INIMIGO_VOADOR,
    INIMIGO_RANGED,
    INIMIGO_MELEE,
    PROJETIL,
    PLATAFORMA
}
