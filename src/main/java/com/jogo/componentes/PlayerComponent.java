package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.ViewComponent;
import com.almasb.fxgl.physics.PhysicsComponent;

/**
 * Componente do jogador.
 *
 * Herda de CharacterComponent (vida, dano recebido, movimento básico) e
 * adiciona o que é específico do player: frames de invencibilidade
 * depois de tomar dano, o sistema de XP/level (metroidvania, sem
 * score), o movimento (via física de verdade) e o empurrão (recoil)
 * de contato com inimigos.
 */


public class PlayerComponent extends CharacterComponent {

    //Atributos de invencibilidade
    protected double invincibilityDuration = 1.0; //segundos de invencibilidade após tomar dano
    private double invincibilityTimer = 0;



    //Atributos de progressão (XP / level)
    protected int level = 1;
    protected int xp = 0;
    protected int xpToNextLevel = 100;



    //Velocidade vertical do pulo (negativo é pra cima, no FXGL/Box2D o
    //eixo Y cresce pra baixo, igual tela). A gravidade quem cuida é o
    //motor de física agora (Main.initPhysics()).
    private static final double JUMP_SPEED = -500;



    //Força do empurrão de contato (recoil). Sempre pra direção oposta
    //de quem causou o hit (ver knockback()), não da direção que o
    //player tá olhando.
    private static final double KNOCKBACK_HORIZONTAL_SPEED = 220;
    private static final double KNOCKBACK_UPWARD_SPEED = 260;



    //Player não tem atrito nenhum (friction 0, ver FabricaEntidades),
    //então sem isso o empurrão do knockback deslizava pra sempre se
    //não apertasse nenhuma tecla. Corta a velocidade horizontal quando
    //zera (mesma janela do hit-stun).
    private double knockbackRecoveryTimer = 0;



    //"Hit-stun" de verdade, só trava o CONTROLE do player (ver
    //isStunned()) por essa janela bem curta, bem menor que
    //invincibilityDuration (que continua sendo o cooldown de
    //dano/invencibilidade, 1s). Sem essa separação o movimento ficava
    //travado o segundo inteiro, e piorava o "preso entre inimigos".
    private static final double HIT_STUN_DURATION = 0.2;



    //Por quanto tempo o player fica piscando depois de um hit. Só
    //efeito visual, não mexe em colisão nem física. O empurrão roda
    //sempre, piscando ou não.
    private static final double BLINK_DURATION = 0.6;
    private double blinkTimer = 0;
    private static final double BLINK_INTERVAL = 0.08;
    private static final double BLINK_DIM_OPACITY = 0.3;

    //PhysicsComponent do FXGL, quem move a entidade de verdade agora
    //(gravidade, colisão com plataforma). Pego em onAdded(), que roda
    //depois que o PhysicsComponent já foi anexado (ver ordem dos
    //.with() em FabricaEntidades.spawnJogador()).
    private PhysicsComponent physics;

    //Componente que guarda a view (o retângulo azul do player por
    //enquanto), usado só pra mexer na opacidade e fazer o efeito de
    //piscar durante os frames de invencibilidade.
    private ViewComponent view;

    //Pra qual lado o player está olhando, atualizado em
    //moveLeft()/moveRight(). Só usado pra saber se o player pode se
    //mexer (isStunned()), o knockback agora usa a posição de quem
    //bateu, não isso aqui.
    private boolean facingRight = true;

    public PlayerComponent(String name, int maxHealth, double moveSpeed) {
        super(name, maxHealth, moveSpeed);
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
        view = entity.getComponent(ViewComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        if (invincibilityTimer > 0) {
            invincibilityTimer -= tpf;
        }

        if (blinkTimer > 0) {
            blinkTimer -= tpf;

            if (blinkTimer <= 0) {
                //Acabou a piscada, garante que a view volta 100%
                //opaca (senão podia travar semi transparente se o
                //timer zerasse no meio de uma piscada apagada).
                view.setOpacity(1.0);
            } else {
                //Pisca, alterna a opacidade a cada BLINK_INTERVAL
                //segundos enquanto durar a invencibilidade visível.
                boolean dim = ((int) Math.floor(blinkTimer / BLINK_INTERVAL)) % 2 == 0;
                view.setOpacity(dim ? BLINK_DIM_OPACITY : 1.0);
            }
        }

        //Corta o deslize do knockback se o player não mexeu em nada
        if (knockbackRecoveryTimer > 0) {
            knockbackRecoveryTimer -= tpf;

            if (knockbackRecoveryTimer <= 0) {
                physics.setVelocityX(0);
            }
        }
    }

    //Controla só o controle do player (move/stop), bem mais curto
    //que isInvincible() (que rege o cooldown de dano). Ver comentário
    //de HIT_STUN_DURATION.
    private boolean isStunned() {
        return invincibilityTimer > (invincibilityDuration - HIT_STUN_DURATION);
    }

    //Sobrescreve takeDamage pra ignorar dano enquanto estiver invencível
    //e reiniciar o timer sempre que um hit for realmente aplicado
    @Override
    public void takeDamage(int quant) {
        if (isInvincible()) {
            return;
        }

        super.takeDamage(quant);
        invincibilityTimer = invincibilityDuration;
    }

    public boolean isInvincible() {
        return invincibilityTimer > 0;
    }

    //Chamado pelo sensor de contato de cada inimigo (ver
    //FabricaEntidades.setupContatoComPlayer) e pelo projétil do ranged
    //(ver ProjectileComponent) quando acertam o player. O empurrão
    //acontece sempre, dano e piscada só se não estiver invencível
    //ainda.
    public void hitBy(Entity source, int damage) {
        knockback(source);

        if (!isInvincible()) {
            takeDamage(damage);
            blinkTimer = BLINK_DURATION;
        }
    }

    //Empurra pro lado oposto de quem bateu (baseado na posição de
    //quem causou o hit, não mais em facingRight). Antes só olhava pra
    //onde o player tava olhando, então levar um hit pelas costas
    //empurrava errado (pra trás de novo, na cara de quem bateu). Agora
    //funciona igual pro contato de inimigo e pro projétil do ranged
    //(os dois passam o "source" certo aqui). setLinearVelocity troca
    //a velocidade toda de uma vez, pro recoil ficar nítido tipo soco,
    //em vez de só somar em cima do que já tinha.
    private void knockback(Entity source) {
        double pushX = source.getCenter().getX() < entity.getCenter().getX()
                ? KNOCKBACK_HORIZONTAL_SPEED
                : -KNOCKBACK_HORIZONTAL_SPEED;
        physics.setLinearVelocity(pushX, -KNOCKBACK_UPWARD_SPEED);
        knockbackRecoveryTimer = HIT_STUN_DURATION;
    }

    //MOVIMENTO
    //Agora tudo passa pelo PhysicsComponent (setVelocityX/Y). Com
    //física de verdade a posição é controlada pelo motor, então mexer
    //nela na mão (entity.translateX, setX...) seria ignorado ou
    //brigaria com o Box2D.

    //Só pula se physics.isOnGround() disser que está no chão (sensor
    //colado nos pés, configurado na FabricaEntidades). Evita pulo
    //infinito no ar.
    public void jump() {
        if (physics.isOnGround()) {
            physics.setVelocityY(JUMP_SPEED);
        }
    }

    //Enquanto isStunned() (janela bem mais curta que a invencibilidade
    //inteira, ver HIT_STUN_DURATION), o player ignora comando de
    //movimento horizontal. Sem isso, segurar A ou D no momento do hit
    //sobrescreveria o recoil no frame seguinte e o soco nunca
    //apareceria de verdade. Depois desse tempinho curto o controle
    //volta, mesmo ainda invencível/piscando, pra dar tempo real do
    //player sair andando de perto de outros inimigos.
    public void moveLeft() {
        if (isStunned()) {
            return;
        }
        facingRight = false;
        physics.setVelocityX(-moveSpeed);
    }

    public void moveRight() {
        if (isStunned()) {
            return;
        }
        facingRight = true;
        physics.setVelocityX(moveSpeed);
    }

    //Chamado quando A ou D é solto (onActionEnd em Main.initInput()),
    //zera a velocidade horizontal. Também respeita o hit-stun curto,
    //mesmo motivo de moveLeft/moveRight: soltar a tecla no meio do
    //recoil não pode zerar o empurrão.
    public void stopHorizontal() {
        if (isStunned()) {
            return;
        }
        physics.setVelocityX(0);
    }

    //XP / LEVEL
    //Metroidvania, então a progressão vem de subir de
    //nível: ganhar XP suficiente aumenta o level e melhora os
    //atributos do personagem (vida e velocidade), curando ele.
    public void addXp(int quant) {
        xp += quant;

        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel;
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        maxHealth += 20;
        
        // MODIFICADO: Substitui "currentHealth = maxHealth;"
        // Motivo: Usar "=" sobrescreve a referência na memória e desliga a UI 
        // dos eventos. O método .set() altera apenas o valor interno notificado.
        currentHealth.set(maxHealth); 
        
        moveSpeed += 5;
        xpToNextLevel = 100 + (level - 1) * 50;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                "hp=" + currentHealth.get() + "/" + maxHealth +
                ", lvl=" + level +
                ", xp=" + xp + "/" + xpToNextLevel +
                '}';
    }
}
