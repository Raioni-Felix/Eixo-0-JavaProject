package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;


//Isso gera a hitbox do ataque do jogador de forma a esta ficar grudada no jogador em si
//Ao invés de gerar por eventos ou 1a vez por frame, ele analisa a posição do jogador naquele frame
//E adiciona o deslocamento ao próprio.
public class AtaqueJogadorComponent extends Component {

    private final Entity jogador;
    private final double deslocamentoX;
    private final double deslocamentoY;

    private PhysicsComponent fisica;

    public AtaqueJogadorComponent(Entity jogador, double deslocamentoX, double deslocamentoY) {
        this.jogador = jogador;
        this.deslocamentoX = deslocamentoX;
        this.deslocamentoY = deslocamentoY;
    }

    @Override
    public void onAdded() {
        fisica = entity.getComponent(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        fisica.overwritePosition(new Point2D(jogador.getX() + deslocamentoX, jogador.getY() + deslocamentoY));
    }
}
