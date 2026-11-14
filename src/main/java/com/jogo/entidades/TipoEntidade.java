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
    CHEFE,         // NEXUS, o chefe da última sala
    PROJETIL,
    PLATAFORMA,
    ATAQUE_JOGADOR,
    GATILHO,
    DISPARO_JOGADOR, // tiro das armas de fogo e do GolpeDeEnergia
    ITEM,          // módulo de cura/energia caído no mapa
    ARMADILHA      // armadilhas do mapa (espinhos e blocos que caem)
}
