package com.jogo.componentes;


//Bixinho voador, herda de EnemyComponent,
// mas não precisa de nada específico, pq
//EnemyComponent já tem o atributo isFlying,
//  que é o que diferencia um inimigo voado de um inimigo terrestre.

public class FlyingEnemyComponent extends EnemyComponent {

    //Só pra efeito visual e fazer o enemy voar subindo e descendo
    //em loop. Nada a ver com perseguir o alvo, só aesthetic mesmo.

    //Tudo fixo (por agora) pra facilitar minha vida
    private double hoverTime = 0;
    private final double hoverAmplitude = 30; //Amplitude, em pixels/segundo (velocidade)
    private final double hoverSpeed = 2; //"frequência" da subida e descida


    //CONSTRUTOR
    //Super chama o construtor da superclasse EnemyComponent, já fixando
    //isFlying=true, isRanged=false, isAggressive=true — um
    //FlyingEnemyComponent SEMPRE é assim, quem cria não escolhe isso.
    public FlyingEnemyComponent(String name, int maxHealth, double moveSpeed,
            int damage, double attackRange, double detectionRange) {

        super(name, maxHealth, moveSpeed, damage, attackRange, detectionRange,
                true, false, true);
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isAggressive || target == null) {
            // Se não for agressivo ou não tiver alvo, apenas flutua
            hover(tpf);
        } else {
            // Se for agressivo e tiver alvo, segue o alvo
            super.onUpdate(tpf); //Persegue e ataca já
            // usando attackRange
        }
    }

    //Seta uma velocidade vertical oscilante (corpo é DYNAMIC com
    //gravityScale 0, ver FabricaEntidades física de verdade, sem
    //gravidade).
    private void hover(double tpf) {
        hoverTime += tpf * hoverSpeed;
        double verticalVelocity = Math.cos(hoverTime) * hoverAmplitude;
        physics.setLinearVelocity(0, verticalVelocity);
    }
}
