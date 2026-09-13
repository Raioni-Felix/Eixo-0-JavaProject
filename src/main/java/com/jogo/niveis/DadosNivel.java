package com.jogo.niveis;

import java.util.List;

/**
 * Estrutura de uma "sala" (um pedaço do mapa), carregada de um
 * arquivo JSON (ver CarregadorDeNiveis) em vez de tudo hardcoded no
 * Main.initGame() como era antes. Cada sala é carregada e desenhada
 * por vez (ver Main.prepararSala()/trocarSala()), com a câmera
 * travada nos limites dela, e o player passa de uma sala pra outra
 * andando por um gatilho nas bordas, com um fade curto escondendo a
 * troca — sem reload/tela de carregamento, igual um metroidvania de
 * verdade.
 *
 * Os nomes de campo batem exatamente com as chaves do JSON (o Gson
 * mapeia por nome, sem anotação), por isso ficam em inglês mesmo
 * dentro de um projeto com o resto comentado/nomeado em português.
 */
public class DadosNivel {
    public double levelWidth;
    public double levelHeight;
    public DadosJogador player;
    public List<DadosPlataforma> platforms;
    public List<DadosInimigo> enemies;

    //Nome do tema de background animado dessa sala (uma pasta em
    //assets/textures/<nome>/ com os frames_NNN.png, ver
    //Main.getBackgroundFrames()). Opcional — null usa "cryolab" (o
    //único tema com frames de verdade hoje), e é ignorado se
    //backgroundColor estiver preenchido (ver abaixo). Uma sala futura
    //tipo um castelo só precisa de uma pasta nova de frames e desse
    //campo apontando pra ela, sem mexer em código.
    public String background;

    //Cor sólida de fundo dessa sala, em hex (ex.: "#3a3a3a"). Opcional
    //— quando preenchido, tem prioridade sobre "background": a sala
    //usa um retângulo de cor lisa em vez do background animado (ver
    //Main.prepararSala()). Útil pra salas mais simples/diferenciadas
    //sem precisar de uma pasta de frames pra cada uma.
    public String backgroundColor;
}
