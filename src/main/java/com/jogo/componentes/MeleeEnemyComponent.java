package com.jogo.componentes;

import static com.almasb.fxgl.dsl.FXGL.spawn;

//Inimigo corpo a corpo, herda de EnemyComponent,
// mas não adiciona nada além do que já existe lá.
//Literalmente um ranged sem range.
//então não precisa sobrescrever
//o attack() do EnemyComponent, porque o do
// EnemyComponent já é "corpo a corpo"
//Só adicionar as flags
//No construtor e deixa o resto pra superclasse

public class MeleeEnemyComponent extends EnemyComponent {
    private boolean droppou = false;
    //CONSTRUTOR
    public MeleeEnemyComponent(String name, int maxHealth,
        double moveSpeed, int damage, double attackRange, double detectionRange) {
        super(name, maxHealth, moveSpeed, damage, attackRange, detectionRange,
            false, false, true);
    }

    //Eeeee mais nada
    //O resto tá na mãe
    //Se quiser add algo dps, adiciona aqui

    @Override
    public void takeDamage(int quant) {
        super.takeDamage(quant);
        
        if (isDead() && !droppou) {
            droppou = true;
            // Ejeta os espólios levemente deslocados do centro do inimigo
            spawn("drop_cura", entity.getX() - 20, entity.getY());
            spawn("drop_dash", entity.getX() + 20, entity.getY());
        }
    }
}