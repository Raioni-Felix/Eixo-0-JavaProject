package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;


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
        if (!isAggressive || target == null) {
            return;
        }

        // Usa detectionRange como distância de parada, em vez de
        // attackRange (que seria corpo a corpo) — persegue até ficar
        // "de longe o suficiente" pra atirar, não precisa encostar
        followTarget(tpf, detectionRange);
    }


    @Override
    public void attack(Entity target) {
        //Não tem projetil ainda
        //Deixar Hiago tomar de conta disso, pq as armas tão com ele
        //Até lá, dano instantâneo, só pra efeito de teste mesmo

        //getCharacterComponent() é herdado do EnemyComponent — procura
        //manualmente por instanceof, porque o FXGL não acha componente
        //por superclasse (só pelo tipo exato).
        CharacterComponent character = getCharacterComponent(target);
        if (character != null) {
            character.takeDamage(damage);
        }
    }
}
