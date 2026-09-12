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
    private final double hoverAmplitude = 30; // Amplitude do movimento,
    // agora em pixels/segundo (velocidade), não mais pixels por frame —
    // physics.setLinearVelocity trabalha com velocidade, não com
    // deslocamento direto.
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

    // Antes: entity.translateY(offset) direto, deslocando a posição na
    // mão a cada frame. Isso não funciona mais com PhysicsComponent
    // anexado (a posição da entidade é controlada pelo corpo físico,
    // que sobrescreveria o translate todo frame). Agora seta uma
    // VELOCIDADE vertical oscilante (derivada do seno = cosseno) — o
    // corpo é KINEMATIC (ver FabricaEntidades), então tem física de
    // verdade mas ignora gravidade, exatamente como o voador precisa.
    private void hover(double tpf) {
        hoverTime += tpf * hoverSpeed;
        double verticalVelocity = Math.cos(hoverTime) * hoverAmplitude;
        physics.setLinearVelocity(0, verticalVelocity);
    }
}
