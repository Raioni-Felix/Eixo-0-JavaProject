package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

/**
 * Move a entidade em linha reta até um ponto fixo (capturado no
 * momento do disparo — não persegue o alvo se ele se mexer depois,
 * é só o efeito visual do "tiro" viajando).
 *
 * Puramente estético: o dano do RangedEnemyComponent já é aplicado na
 * hora do disparo (attack()), direto, sem esperar o projétil chegar.
 * Esse componente só cuida de mostrar alguma coisa viajando na tela.
 * Quando não existir mais projétil "instantâneo" (ou seja, quando
 * alguém decidir implementar dano de verdade na colisão), essa classe
 * já dá a base pra isso — só precisa parar de aplicar o dano direto no
 * attack() e passar a aplicar aqui, ao chegar no alvo.
 */
public class ProjectileVisualComponent extends Component {

    private final Point2D targetPoint;
    private final double speed;

    private double traveled = 0;
    private final double maxDistance;

    public ProjectileVisualComponent(Point2D targetPoint, double speed, double maxDistance) {
        this.targetPoint = targetPoint;
        this.speed = speed;
        this.maxDistance = maxDistance;
    }

    @Override
    public void onUpdate(double tpf) {
        double step = speed * tpf;

        entity.translateTowards(targetPoint, step);
        traveled += step;

        // Chegou perto o suficiente do ponto alvo, ou já andou o
        // suficiente sem chegar (por precaução) — some da tela.
        if (traveled >= maxDistance || entity.getCenter().distance(targetPoint) < 4) {
            entity.removeFromWorld();
        }
    }
}
