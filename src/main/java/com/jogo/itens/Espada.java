package com.jogo.itens;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.scene.Node;

import static com.almasb.fxgl.dsl.FXGL.spawn;
import static com.almasb.fxgl.dsl.FXGL.texture;

//Arma corpo a corpo. É o antigo "case MELEE" do ComponenteArma, que
//veio pra cá: cria a hitbox "ataque_jogador" na frente do portador,
//do tamanho do alcance da espada.
public class Espada extends Arma {

    public Espada(String idBase, String nome, String descricao, int dano, double cooldown, double alcance) {
        super(idBase, nome, descricao, dano, cooldown, alcance);
    }

    @Override
    protected void executarAtaque(Entity portador, double direcaoX) {
        double alcance = getAlcance();

        //deslocamentoX/deslocamentoY são relativos ao portador, não
        //posições absolutas do mundo. O portador vai junto pro
        //AtaqueJogadorComponent conseguir seguir ele todo frame (sem
        //isso a hitbox ficava pra trás se o jogador andasse/pulasse/
        //desse dash durante os 0.15s do golpe).
        double deslocamentoX = direcaoX > 0 ? portador.getWidth() : -alcance;
        double deslocamentoY = 0;

        spawn("ataque_jogador", new SpawnData(portador.getX() + deslocamentoX, portador.getY() + deslocamentoY)
                .put("tamanho", alcance)
                .put("dano", getDanoTotal())
                .put("jogador", portador)
                .put("deslocamentoX", deslocamentoX)
                .put("deslocamentoY", deslocamentoY));
    }

    @Override
    public Node criarVisual() {
        return texture("sword.png");
    }

    @Override
    public String getDescricaoCombate() {
        return "corte de " + getDanoTotal();
    }
}
