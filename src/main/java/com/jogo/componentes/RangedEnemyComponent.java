package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.jogo.componentes.visual.ProjectileComponent;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;


//Enemy sniper herda de EnemyComponent
//Tem que parar de andar e atirar assim que o player estiver
//no detectionRange (q é maior)
//Sobrescreve o onUpdate pra parar de andar e atirar, e não perseguir o player
public class RangedEnemyComponent extends EnemyComponent {

        //CONSTRUTOR
    //Super chama o construtor da superclasse EnemyComponent, já fixando
    //isFlying=false, isRanged=true, isAggressive=true

    public RangedEnemyComponent(String name, int maxHealth, double moveSpeed,
            int damage, double attackRange, double detectionRange) {

        super(name, maxHealth, moveSpeed, damage, attackRange, detectionRange,
                false, true, true);
    }

    @Override
    public void onUpdate(double tpf) {
        // !target.isActive() cobre o alvo já ter morrido — sem isso o
        // atirador continua "atirando" num player que já não existe
        // mais (e ficaria assim pra sempre, já que isAggressive nunca
        // vira false sozinho).
        if (!isAggressive || target == null || !target.isActive()) {
            return;
        }

        // Usa detectionRange como distância de parada, em vez de
        // attackRange (que seria corpo a corpo) — persegue até ficar
        // "de longe o suficiente" pra atirar, não precisa encostar
        followTarget(tpf, detectionRange);
    }


    @Override
    public void attack(Entity target) {
        // O dano NÃO é mais aplicado aqui — só o ProjectileComponent
        // aplica, e só quando o projétil realmente alcança o alvo.
        // attack() agora só dispara o projétil.
        spawnProjectile(target);
    }

    // Cria o projétil de verdade: agora é uma bala reta (não homing) —
    // passa "entity" (quem atirou) como origem, pra ProjectileComponent
    // calcular a direção fixa no momento do disparo.
    private void spawnProjectile(Entity target) {
        entityBuilder()
                .at(entity.getX(), entity.getY())
                .viewWithBBox("projectile.png")
                .with(new ProjectileComponent(entity, target, damage, 500))
                .buildAndAttach();
    }
}
