package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;

import static com.almasb.fxgl.dsl.FXGL.spawn;


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

        //Cadência mais lenta que o padrão (1.0s, pensado pro corpo a
        //corpo), dá tempo do player se esquivar entre um tiro e outro.
        attackCooldownSeconds = 2.5;
    }

    @Override
    public void onUpdate(double tpf) {
        // !target.isActive() cobre o alvo já ter morrido, sem isso o
        // atirador continua "atirando" num player que já não existe
        // mais (e ficaria assim pra sempre, já que isAggressive nunca
        // vira false sozinho).
        if (!isAggressive || target == null || !target.isActive()) {
            return;
        }

        // Usa detectionRange como distância de parada, em vez de
        // attackRange (que seria corpo a corpo). Persegue até ficar
        // "de longe o suficiente" pra atirar, não precisa encostar
        followTarget(tpf, detectionRange);
    }


    @Override
    public void attack(Entity target) {
        // O dano NÃO é mais aplicado aqui, só o ProjectileComponent
        // aplica, e só quando o projétil realmente alcança o alvo.
        // attack() agora só dispara o projétil.
        spawnProjectile(target);
    }

    // Cria o projétil de verdade: agora passa pela FabricaEntidades
    // (@Spawns("projetil")) em vez de montar a entidade na mão aqui,
    // essa é a parte do padrão do NetBeans (EntityFactory + @Spawns)
    // que foi incorporada. O SpawnData carrega a posição (x, y) e os
    // dados extras que o ProjectileComponent precisa: origem (pra
    // calcular a direção fixa da bala), alvo, dano e velocidade.
    private void spawnProjectile(Entity target) {
        SpawnData data = new SpawnData(entity.getX(), entity.getY())
                .put("origin", entity)
                .put("target", target)
                .put("damage", damage)
                .put("speed", 500.0);

        spawn("projetil", data);
    }
}
