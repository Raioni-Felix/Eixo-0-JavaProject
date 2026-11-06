package com.jogo.visao;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Carrega as imagens grandes do jogo (quadros do background animado e o
//fundo do menu). Antes ficava no Main. Classe utilitária (só static).
public final class RecursosVisuais {

    private RecursosVisuais() {
    }

    //Cache dos quadros por tema de background, decodificados só uma vez
    //pra vida inteira do app. Antes isso rodava em todo startNewGame()
    //(Reiniciar, voltar pro menu) e redecodificava as 60 imagens do zero,
    //a maior causa do carregamento longo. Hoje só existe o tema
    //"cryolab"; um tema novo é só criar a pasta de quadros com o mesmo
    //padrão de nome e usar o nome no campo "background" do JSON da sala.
    private static final Map<String, List<Image>> cacheQuadrosFundo = new HashMap<>();

    //Temas de fundo animado. Cada um é uma pasta em assets/textures com
    //frame_000.png, frame_001.png... (quantos houver):
    // - "cryolab": laboratório da sala 1, 60 quadros, arquivos 4000x720
    //   (upscale 4x) decodificados em 1000x180;
    // - "cidade": cidade do Warped City (ver sprites/CREDITOS.txt), 12
    //   quadros 1479x240 no tamanho original, com os letreiros piscando.
    private static final Map<String, double[]> TAMANHO_DECODIFICADO = Map.of("cryolab", new double[]{1000, 180});
    private static final Map<String, Double> FPS = Map.of("cryolab", 15.0, "cidade", 8.0);

    public static List<Image> quadrosFundo(String tema) {
        return cacheQuadrosFundo.computeIfAbsent(tema, chave -> {
            List<Image> quadros = new ArrayList<>();
            for (int i = 0; existeQuadro(chave, i); i++) {
                quadros.add(carregarQuadroFundo(chave, i));
            }
            if (quadros.isEmpty()) {
                throw new IllegalStateException("Tema de fundo sem quadros: " + chave);
            }
            return quadros;
        });
    }

    public static double fpsFundo(String tema) {
        return FPS.getOrDefault(tema, 10.0);
    }

    private static String caminhoQuadro(String tema, int index) {
        return "/assets/textures/" + tema + "/frame_" + String.format("%03d", index) + ".png";
    }

    private static boolean existeQuadro(String tema, int index) {
        return RecursosVisuais.class.getResource(caminhoQuadro(tema, index)) != null;
    }

    //Carrega um quadro já pedindo pro JavaFX decodificar em 1000x180 em
    //vez do 4000x720 do arquivo. O arquivo é um upscale 4x
    //nearest-neighbor do desenho original, e smooth=false desfaz esse
    //upscale sem borrar, gastando 16x menos memória por quadro.
    private static Image carregarQuadroFundo(String tema, int index) {
        String caminho = caminhoQuadro(tema, index);
        double[] tamanho = TAMANHO_DECODIFICADO.getOrDefault(tema, new double[]{0, 0}); //0 = tamanho do arquivo

        try (InputStream stream = RecursosVisuais.class.getResourceAsStream(caminho)) {
            if (stream == null) {
                throw new IllegalStateException("Quadro não encontrado: " + caminho);
            }
            return new Image(stream, tamanho[0], tamanho[1], true, false);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar quadro do background (" + tema + "): " + caminho, e);
        }
    }

    //Arte de fundo do menu principal (corredor de cryo-lab). Imagem
    //pequena, carregada no tamanho nativo; quem estica é o ImageView.
    public static Image imagemFundoMenu() {
        String caminho = "/assets/textures/menu/scifi-lab.png";

        try (InputStream stream = RecursosVisuais.class.getResourceAsStream(caminho)) {
            if (stream == null) {
                throw new IllegalStateException("Imagem não encontrada: " + caminho);
            }
            return new Image(stream);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar fundo do menu: " + caminho, e);
        }
    }
}
