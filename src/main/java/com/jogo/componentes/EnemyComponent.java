package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;


/**
 * Classe/Componente base para todos os inimigos do jogo.
 *
 * Herda de GameCharacter (vida, dano recebido, movimento básico) e
 * adiciona apenas o que é específico de inimigo: dano de ataque,
 * alcance, e os parâmetros que diferenciam os tipos (voador, ranged,
 * agressivo...). Tipos específicos (FlyingEnemy, RangedEnemy) vão
 * herdar desta classe usando "extends Enemy".
 */

public class EnemyComponent extends CharacterComponent {

    //Atributos específicos do inimigo
    protected int damage;
    protected double attackRange;
    protected double detectionRange;

    protected boolean isFlying;
    protected boolean isRanged;
    protected boolean isAggressive;

    //Alvo que o inimigo vai perseguir/atacar
    //Não preenche no construtor pq quando é criado não possui alvo até ser agressivo
    //setTarget é pra isso
    protected Entity target;

    //tempo mínimo entre os ataques (cooldown)
    protected double attackCooldownSeconds = 1.0;
    protected double timeSinceLastAttack = 0;

    //PhysicsComponent do FXGL, movimento (perseguição, hover) passa
    //por aqui agora, via setLinearVelocity/setVelocityX.
    protected PhysicsComponent physics;

    //CONSTRUTOR
    //Super chama o construtor da superclasse Character
    //Sempre primeira linha, pq a parte comum do objeto
    //precisa existir antes de fazer a específica do enemy.
    public EnemyComponent(String name, int maxHealth, double moveSpeed, int damage,
            double attackRange, double detectionRange,
            boolean isFlying, boolean isRanged, boolean isAggressive) {

        super(name, maxHealth, moveSpeed);

        this.damage = damage;
        this.attackRange = attackRange;
        this.detectionRange = detectionRange;
        this.isFlying = isFlying;
        this.isRanged = isRanged;
        this.isAggressive = isAggressive;
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }

    //COMPORTAMENTO DO INIMIGO

    public void setTarget(Entity target) {
        this.target = target;
    }

    //Quanto de dano esse inimigo causa (usado no contato com o player)
    public int getDamage() {
        return damage;
    }

    public void attack(Entity target) {
        //Logica de ataque integrado do FXGL
        //Só ataque se estiver no range
        if (entity.distance(target) > attackRange) {
            return;
        }

        //IMPORTANTE ISSO. Explicar depois
        CharacterComponent character = getCharacterComponent(target);
        if (character != null) {
            character.takeDamage(damage);
        }
    }

    //Repassa pro helper estático (getFrom), pra não reescrever toda hora
    protected CharacterComponent getCharacterComponent(Entity target) {
        return CharacterComponent.getFrom(target);
    }

    //FXGL guarda componente pela classe EXATA (FlyingEnemyComponent
    //etc.), então getComponent(EnemyComponent.class) nunca acha nada.
    //Esse helper varre os componentes reais e devolve o que É/herda
    //de EnemyComponent, não importa o tipo concreto.
    public static EnemyComponent getFrom(Entity target) {
        for (Component c : target.getComponents()) {
            if (c instanceof EnemyComponent) {
                return (EnemyComponent) c;
            }
        }
        return null;
    }

    //onUpdate roda cada frame do jogo
    @Override
    public void onUpdate(double tpf) {
        // !target.isActive() cobre o caso do alvo já ter morrido
        // (removido do mundo), sem isso, o inimigo continua
        // perseguindo/atacando um alvo que não existe mais.
        if (!isAggressive || target == null || !target.isActive()) {
            return;
        }

        followTarget(tpf, attackRange);
    }

    //Persegue o alvo até stopDistance, depois para e ataca. Voador
    //(isFlying) persegue nos dois eixos (física própria, sem
    //gravidade); terrestre só no X (Y é a gravidade do corpo DYNAMIC).
    protected void followTarget(double tpf, double stopDistance) {
        double distance = entity.distance(target);

        if (distance > stopDistance) {
            double dx = target.getX() - entity.getX();
            double dy = target.getY() - entity.getY();

            if (isFlying) {
                double length = Math.hypot(dx, dy);
                double vx = length > 0 ? (dx / length) * moveSpeed : 0;
                double vy = length > 0 ? (dy / length) * moveSpeed : 0;
                physics.setLinearVelocity(vx, vy);
            } else {
                physics.setVelocityX(dx > 0 ? moveSpeed : -moveSpeed);
            }
        } else {
            // Perto o suficiente pra atacar: para de andar. Pro
            // terrestre só zera a horizontal (deixa a gravidade seguir
            // seu curso, senão ele "flutuaria" parado no ar).
            if (isFlying) {
                physics.setLinearVelocity(0, 0);
            } else {
                physics.setVelocityX(0);
            }
        }

        timeSinceLastAttack += tpf;
        boolean inRange = distance <= stopDistance;
        boolean endCooldown = timeSinceLastAttack >= attackCooldownSeconds;

        if (inRange && endCooldown) {
            attack(target);
            timeSinceLastAttack = 0;
        }
    }
}
