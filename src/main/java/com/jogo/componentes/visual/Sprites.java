package com.jogo.componentes.visual;

import com.almasb.fxgl.texture.AnimationChannel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Carrega os quadros das animações (pasta assets/textures/sprites, ver
//CREDITOS.txt lá). Cada animação é uma sequência de arquivos
//"<nome>-1.png", "<nome>-2.png"... dentro da pasta do personagem.
//Os quadros ficam em cache: Reiniciar e trocar de sala não decodificam
//as imagens de novo.
//
//A ampliação é feita aqui, pixel a pixel (vizinho mais próximo). Se a
//imagem fosse ampliada na tela (setScaleX/Y), o JavaFX suavizaria e o
//pixel art ficaria borrado.
public final class Sprites {

    private static final Map<String, List<Image>> cache = new HashMap<>();

    private Sprites() {
    }

    //Ex.: animacao("jogador", "correr", 8, 0.7, 1.5) = 8 quadros em 0,7 s,
    //1,5x maiores que o arquivo
    public static AnimationChannel animacao(String pasta, String nome, int quadros, double segundos, double escala) {
        return new AnimationChannel(quadros(pasta, nome, quadros, escala), Duration.seconds(segundos));
    }

    public static List<Image> quadros(String pasta, String nome, int quantidade, double escala) {
        if (escala <= 0) {
            throw new IllegalArgumentException("Escala precisa ser positiva");
        }
        return cache.computeIfAbsent(pasta + "/" + nome + "/" + quantidade + "/" + escala, chave -> {
            List<Image> lista = new ArrayList<>();
            for (int i = 1; i <= quantidade; i++) {
                Image original = carregar("/assets/textures/sprites/" + pasta + "/" + nome + "-" + i + ".png");
                lista.add(escala == 1 ? original : ampliar(original, escala));
            }
            return lista;
        });
    }

    //Vizinho mais próximo: cada pixel novo copia o pixel original mais perto
    static Image ampliar(Image original, double escala) {
        int largura = (int) Math.round(original.getWidth() * escala);
        int altura = (int) Math.round(original.getHeight() * escala);
        PixelReader leitor = original.getPixelReader();
        WritableImage nova = new WritableImage(largura, altura);
        for (int y = 0; y < altura; y++) {
            int yOriginal = Math.min((int) (y / escala), (int) original.getHeight() - 1);
            for (int x = 0; x < largura; x++) {
                int xOriginal = Math.min((int) (x / escala), (int) original.getWidth() - 1);
                nova.getPixelWriter().setArgb(x, y, leitor.getArgb(xOriginal, yOriginal));
            }
        }
        return nova;
    }

    private static Image carregar(String caminho) {
        try (InputStream stream = Sprites.class.getResourceAsStream(caminho)) {
            if (stream == null) {
                throw new IllegalStateException("Sprite não encontrado: " + caminho);
            }
            //smooth=false: pixel art não pode borrar
            return new Image(stream, 0, 0, true, false);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao carregar o sprite " + caminho, e);
        }
    }
}
