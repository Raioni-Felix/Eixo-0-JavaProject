package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import com.jogo.componentes.TimeScaleManager;
import javafx.scene.image.Image;

import java.util.List;

//Background animado: troca a imagem a cada duracaoQuadro segundos,
//pra dar aquele efeito de vídeo em loop (os quadros vêm de uma
//sequência de PNGs, não de um spritesheet). Fica parado no nível
//(posição fixa, igual o tiling estático de antes), não segue a
//câmera, então repete lado a lado normalmente conforme o jogador anda.
//Não faz parte da física do nível, é só um fundo visual atrás de
//tudo.
public class ComponenteAnimacaoFundo extends Component {

    private final List<Image> quadros;
    private final double duracaoQuadro; //segundos por quadro (1 / fps)
    private final Texture visao;

    private double tempoDecorrido = 0;
    private int quadroAtual = 0;

    public ComponenteAnimacaoFundo(List<Image> quadros, double fps) {
        this.quadros = quadros;
        this.duracaoQuadro = 1.0 / fps;
        this.visao = new Texture(quadros.get(0));
    }

    //Essa é a Texture que vai pro .view() do entityBuilder(), pega
    //aqui antes de criar a entidade (ver controle.GerenciadorSalas.prepararSala()).
    public Texture getVisao() {
        return visao;
    }

    @Override
    public void onUpdate(double tpf) {
        // Aplica time scale do Sandevistan (fundo também entra no slow-motion)
        double tpfEfetivo = TimeScaleManager.getTpfPara(tpf, entity);
        
        tempoDecorrido += tpfEfetivo;

        //Avança um quadro por vez em vez de pular direto pro quadro
        //"certo" com base no tempo total, assim funciona igual mesmo
        //se o jogo engasgar um pouco num frame (tpf grande).
        if (tempoDecorrido >= duracaoQuadro) {
            tempoDecorrido -= duracaoQuadro;
            quadroAtual = (quadroAtual + 1) % quadros.size();
            visao.setImage(quadros.get(quadroAtual));
        }
    }
}
