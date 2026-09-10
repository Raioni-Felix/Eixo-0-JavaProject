package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.jogo.componentes.CharacterComponent;
import javafx.geometry.Point2D;

/**
 * Projétil "bala de verdade": NÃO é homing. A direção é calculada UMA
 * ÚNICA VEZ, no momento em que o projétil é criado (com base em onde o
 * alvo estava naquele instante) — depois disso ele só anda em linha
 * reta nessa direção fixa, mesmo que o alvo se mova pra outro lugar.
 *
 * Isso é diferente de "não checar se acertou": a cada frame ele ainda
 * olha a distância ATUAL até o alvo pra saber se colidiu (isso é
 * detecção de colisão, não perseguição/homing — ele não muda de rumo
 * por causa disso).
 */
public class ProjectileComponent extends Component {

    private static final double HIT_DISTANCE = 10;
    private static final double MAX_LIFETIME_SECONDS = 3;

    private final Entity target;
    private final int damage;
    private final double speed;
    private final Point2D direction;

    private double lifetime = 0;

    public ProjectileComponent(Entity origin, Entity target, int damage, double speed) {
        this.target = target;
        this.damage = damage;
        this.speed = speed;

        // Calcula a direção AGORA (no disparo) e guarda ela fixa.
        Point2D from = origin.getCenter();
        Point2D to = target.getCenter();
        Point2D raw = to.subtract(from);

        // Se por algum motivo origem e alvo estiverem no mesmo ponto
        // exato (magnitude 0), usa uma direção padrão pra não dividir
        // por zero no normalize().
        this.direction = raw.magnitude() > 0 ? raw.normalize() : new Point2D(1, 0);
    }

    @Override
    public void onUpdate(double tpf) {
        lifetime += tpf;

        if (lifetime >= MAX_LIFETIME_SECONDS) {
            entity.removeFromWorld();
            return;
        }

        // Sempre na mesma direção — isso que faz ser "bala" e não mais
        // "míssil teleguiado".
        entity.translate(direction.multiply(speed * tpf));

        // Só checa colisão se o alvo ainda existir. Se o alvo morreu no
        // meio do caminho, a bala não desaparece por causa disso — ela
        // continua reto até bater no lifetime (bala não sabe que o
        // alvo morreu, ela só ia numa direção).
        if (target != null && target.isActive()
                && entity.getCenter().distance(target.getCenter()) <= HIT_DISTANCE) {
            CharacterComponent character = CharacterComponent.getFrom(target);
            if (character != null) {
                character.takeDamage(damage);
            }
            entity.removeFromWorld();
        }
    }
}
