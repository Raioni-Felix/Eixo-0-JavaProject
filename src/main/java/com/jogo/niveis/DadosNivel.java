package com.jogo.niveis;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Estrutura de uma "sala" (um pedaço do mapa), carregada de um
 * arquivo JSON (ver CarregadorDeNiveis) em vez de tudo hardcoded no
 * Main.initGame() como era antes. Cada sala é carregada e desenhada
 * por vez (ver Main.prepararSala()/trocarSala()), com a câmera
 * travada nos limites dela, e o jogador passa de uma sala pra outra
 * andando por um gatilho nas bordas, com um fade curto escondendo a
 * troca — sem reload/tela de carregamento, igual um metroidvania de
 * verdade.
 *
 * As chaves do JSON continuam em inglês (os arquivos .json não foram
 * tocados), mas os campos Java aqui ficam em português — cada um usa
 * @SerializedName pra apontar pra chave original, então o Gson mapeia
 * certo mesmo com o nome do campo diferente.
 */
public class DadosNivel {
    @SerializedName("levelWidth")
    public double larguraNivel;
    @SerializedName("levelHeight")
    public double alturaNivel;
    @SerializedName("player")
    public DadosJogador jogador;
    @SerializedName("platforms")
    public List<DadosPlataforma> plataformas;
    @SerializedName("enemies")
    public List<DadosInimigo> inimigos;
    //Módulos de cura/energia da sala. Opcional: sala sem "items" no
    //JSON fica com null aqui (ver Main.spawnItensDaSala()).
    @SerializedName("items")
    public List<DadosItem> itens;

    //Nome do tema de background animado dessa sala (uma pasta em
    //assets/textures/<nome>/ com os frames_NNN.png, ver
    //Main.getBackgroundFrames()). Opcional — null usa "cryolab" (o
    //único tema com frames de verdade hoje), e é ignorado se
    //corFundo estiver preenchido (ver abaixo). Uma sala futura
    //tipo um castelo só precisa de uma pasta nova de frames e desse
    //campo apontando pra ela, sem mexer em código.
    @SerializedName("background")
    public String fundo;

    //Cor sólida de fundo dessa sala, em hex (ex.: "#3a3a3a"). Opcional
    //— quando preenchido, tem prioridade sobre "fundo": a sala
    //usa um retângulo de cor lisa em vez do background animado (ver
    //Main.prepararSala()). Útil pra salas mais simples/diferenciadas
    //sem precisar de uma pasta de frames pra cada uma.
    @SerializedName("backgroundColor")
    public String corFundo;
}
