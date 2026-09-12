package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import javafx.scene.image.Image;

import java.util.List;

//Background animado: troca a imagem a cada frame_duration segundos,
//pra dar aquele efeito de vídeo em loop (os frames vêm de uma
//sequência de PNGs, não de um spritesheet). Fica parado no nível
//(posição fixa, igual o tiling estático de antes), não segue a
//câmera, então repete lado a lado normalmente conforme o player anda.
//Não faz parte da física do nível, é só um fundo visual atrás de
//tudo.
public class BackgroundAnimationComponent extends Component {

    private final List<Image> frames;
    private final double frameDuration; //segundos por frame (1 / fps)
    private final Texture view;

    private double elapsed = 0;
    private int currentFrame = 0;

    public BackgroundAnimationComponent(List<Image> frames, double fps) {
        this.frames = frames;
        this.frameDuration = 1.0 / fps;
        this.view = new Texture(frames.get(0));
    }

    //Essa é a Texture que vai pro .view() do entityBuilder(), pega
    //aqui antes de criar a entidade (ver Main.initGame()).
    public Texture getView() {
        return view;
    }

    @Override
    public void onUpdate(double tpf) {
        elapsed += tpf;

        //Avança um frame por vez em vez de pular direto pro frame
        //"certo" com base no tempo total, assim funciona igual mesmo
        //se o jogo engasgar um pouco num frame (tpf grande).
        if (elapsed >= frameDuration) {
            elapsed -= frameDuration;
            currentFrame = (currentFrame + 1) % frames.size();
            view.setImage(frames.get(currentFrame));
        }
    }
}
