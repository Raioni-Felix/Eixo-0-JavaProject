package com.jogo.itens;

import java.util.List;

//Cria cada arma do jogo pelo nome usado no JSON das salas ("weapon")
//e nos ícones (sprites/itens/<nome>-1.png). Antes os números das armas
//ficavam soltos no ControleJogo; agora a arma achada no chão e a do
//começo saem do mesmo lugar.
public final class CatalogoArmas {

    public static final List<String> NOMES = List.of("espada", "adagas", "shotgun", "pistola", "rifle");

    private CatalogoArmas() {
    }

    public static Arma criar(String nome) {
        return switch (nome) {
            case "espada" -> new Espada("espada", "Espada", "Arma inicial: corte equilibrado", 20, 0.5, 40);
            case "adagas" -> new AdagaDupla("adagas", "Adagas Duplas", "Dois cortes rápidos, de perto", 8, 0.3, 30);
            case "shotgun" -> new Shotgun("shotgun", "Shotgun", "Balins em leque, forte de perto", 8, 5, 0.9, 220);
            case "pistola" -> new PistolaDuplaDePlasma("pistola", "Pistola Dupla de Plasma", "Dois tiros por ataque", 9, 0.45, 450);
            case "rifle" -> new RifleSniper("rifle", "Rifle Sniper", "Tiro que atravessa inimigos", 30, 1.4, 900, 3);
            default -> throw new IllegalArgumentException("Arma desconhecida: " + nome);
        };
    }

    //"shotgun-3" -> "shotgun": o nome no catálogo de uma arma já criada
    //(o número no fim é o contador que deixa cada id único)
    public static String nomeDe(Arma arma) {
        return arma.getId().replaceAll("-\\d+$", "");
    }
}
