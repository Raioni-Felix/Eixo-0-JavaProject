package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.jogo.componentes.CharacterComponent;
import com.jogo.componentes.PlayerComponent;
import javafx.geometry.Point2D;

/**
 * Projétil "bala de verdade": NÃO é homing. A direção é calculada UMA
 * ÚNICA VEZ, no momento em que o projétil é criado (com base em onde o
 * alvo estava naquele instante). Depois disso ele só anda em linha
 * reta nessa direção fixa, mesmo que o alvo se mova pra outro lugar.
 *
 * Isso é diferente de "não checar se acertou": a cada frame ele ainda
 * olha a distância ATUAL até o alvo pra saber se colidiu (isso é só
 * detecção de colisão, não perseguição/homing, ele não muda de rumo
 * por causa disso).
 */
public class ProjectileComponent extends Component {

    private static final double HIT_DISTANCE = 10;
    private static final double MAX_LIFETIME_SECONDS = 3;

    private final Entity origin;
    private final Entity target;
    private final int damage;
    private final double speed;

    //Não é mais final, calculada em onAdded() (ver comentário lá embaixo).
    private Point2D direction;

    private double lifetime = 0;

    public ProjectileComponent(Entity origin, Entity target, int damage, double speed) {
        this.origin = origin;
        this.target = target;
        this.damage = damage;
        this.speed = speed;
    }

    //Calcula a direção aqui, não no construtor. Usar origin.getCenter()
    //(centro do atirador) como origem da mira dava um desvio
    //constante, pq o projétil nasce no canto (x,y) do atirador, não no
    //centro dele. entity.getCenter() usa a posição real de nascimento
    //do projétil, sem esse desvio.
    @Override
    public void onAdded() {
        Point2D from = entity.getCenter();
        Point2D to = target.getCenter();
        Point2D raw = to.subtract(from);

        // Se por algum motivo origem e alvo estiverem no mesmo ponto
        // exato (magnitude 0), usa uma direção padrão pra não dividir
        // por zero no normalize().
        direction = raw.magnitude() > 0 ? raw.normalize() : new Point2D(1, 0);
    }

    @Override
    public void onUpdate(double tpf) {
        lifetime += tpf;

        if (lifetime >= MAX_LIFETIME_SECONDS) {
            entity.removeFromWorld();
            return;
        }

        // Sempre na mesma direção, isso que faz ser "bala" e não mais
        // "míssil teleguiado".
        entity.translate(direction.multiply(speed * tpf));

        // Só checa colisão se o alvo ainda existir. Se o alvo morreu no
        // meio do caminho, a bala não desaparece por causa disso, ela
        // continua reto até bater no lifetime (bala não sabe que o
        // alvo morreu, ela só ia numa direção).
        if (target != null && target.isActive()
                && entity.getCenter().distance(target.getCenter()) <= HIT_DISTANCE) {
            CharacterComponent character = CharacterComponent.getFrom(target);

            //Se o alvo é o player, usa hitBy(), ganha knockback e
            //piscada igual ao toque físico. Qualquer outro alvo,
            //takeDamage() normal.
            if (character instanceof PlayerComponent) {
                ((PlayerComponent) character).hitBy(origin, damage);
            } else if (character != null) {
                character.takeDamage(damage);
            }

            entity.removeFromWorld();
        }
    }
}
