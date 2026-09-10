package com.jogo.entidades;

/**
 * Tipos de entidade do jogo — usado pela FabricaEntidades (.type(...))
 * e disponível pra quem precisar filtrar/consultar entidades por tipo
 * (ex.: colisões, getGameWorld().getEntitiesByType(...)).
 *
 * Ideia trazida da versão que o time estava fazendo no NetBeans (que
 * tinha JOGADOR/INIMIGO/PLATAFORMA genéricos). Aqui os inimigos já são
 * tipos concretos diferentes (voador, ranged), então cada um ganhou
 * seu próprio valor em vez de um "INIMIGO" único.
 */
public enum EntityType {
    JOGADOR,
    INIMIGO_VOADOR,
    INIMIGO_RANGED,
    PROJETIL,
    PLATAFORMA
}
