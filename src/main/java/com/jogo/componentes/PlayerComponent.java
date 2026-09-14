package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.ViewComponent;
import com.almasb.fxgl.input.KeyTrigger;
import com.almasb.fxgl.input.TriggerListener;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.scene.input.KeyCode;

import static com.almasb.fxgl.dsl.FXGL.getInput;

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


    // Controle do Sistema de Habilidades
    private boolean canDoubleJump = true; // Flag para autorizar o pulo no ar
    private boolean hasDashed = false; // Evita que o player dê dashes infinitos sem pisar no chão
    private double dashCooldownTimer = 0;
    private static final double DASH_SPEED = 600;
    private static final double DASH_COOLDOWN = 1.0;

    //Quanto tempo dura a "rajada" do dash (estilo Hollow Knight/Silksong:
    //um impulso curto e fixo, não uma velocidade que fica pra sempre).
    //Enquanto dashTimer > 0 o player está "trancado" nessa velocidade —
    //nem moveLeft/moveRight nem stopHorizontal mexem nela (ver
    //isDashing() abaixo) — e ao zerar o controle normal volta. Sem
    //isso, como o player não tem fricção nenhuma (todo movimento é
    //velocidade setada na mão, ver moveLeft/moveRight/stopHorizontal),
    //a velocidade do dash nunca era desfeita e o player ficava
    //deslizando na velocidade do dash pra sempre.
    private double dashTimer = 0;
    private static final double DASH_DURATION = 0.15;

    //Pequeno bônus de velocidade pra recompensar quem segura o Shift
    //depois do dash em vez de soltar na hora (tipo um "sprint" curto
    //puxado pelo próprio golpe do dash). shiftHeld é atualizado pelo
    //TriggerListener (onKeyBegin/onKeyEnd) abaixo — precisa saber se o
    //Shift AINDA está pressionado, não só o instante do toque, já que
    //isso não existe como consulta pronta nessa versão do FXGL (ver
    //comentário mais abaixo). dashBoostActive só fica true se o
    //último dash realmente aconteceu (dash() só chega a ligar isso se
    //passar pelas travas dele) e o Shift continuar segurado sem soltar
    //depois — soltando o Shift (onKeyEnd) desliga o bônus na hora.
    private boolean shiftHeld = false;
    private boolean dashBoostActive = false;
    private static final double DASH_BOOST_MULTIPLIER = 1.12; // bem pequeno, só pra fazer diferença

    //O dash não é um UserAction registrado em Main.initInput() (o FXGL
    //proíbe addAction em teclas modificadoras como SHIFT/CONTROL/ALT —
    //ver comentário em Main.initInput() — e, nessa versão do FXGL,
    //nem existe um Input.isHeld(KeyCode) pra consulta crua; só
    //descobri isso depois de tentar e o Maven acusar "cannot find
    //symbol"). O jeito certo, confirmado na fonte do próprio FXGL
    //17.3 (Input.kt/TriggerListener.kt), é um TriggerListener: ele é
    //avisado de TODA tecla pressionada no jogo (onKeyBegin/onKeyEnd),
    //então o listener registrado em onAdded() só reage quando a tecla
    //for Shift — dash() no toque (onKeyBegin dispara uma vez só, sem
    //precisar controlar "borda de subida" na mão) e desliga o boost
    //ao soltar (onKeyEnd).
    public PlayerComponent(String name, int maxHealth, double moveSpeed) {
        super(name, maxHealth, moveSpeed);
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
        view = entity.getComponent(ViewComponent.class);

        getInput().addTriggerListener(new TriggerListener() {
            @Override
            protected void onKeyBegin(KeyTrigger keyTrigger) {
                if (keyTrigger.getKey() == KeyCode.SHIFT) {
                    shiftHeld = true;
                    dash();
                }
            }

            @Override
            protected void onKeyEnd(KeyTrigger keyTrigger) {
                if (keyTrigger.getKey() == KeyCode.SHIFT) {
                    shiftHeld = false;
                    dashBoostActive = false;
                }
            }
        });
    }

    @Override
    public void onUpdate(double tpf) {

        if (dashCooldownTimer > 0) dashCooldownTimer -= tpf;

        //Conta a janela do dash. Ao terminar, corta a velocidade na
        //hora — sem desaceleração suave. Pesquisei o comportamento
        //documentado do dash de Hollow Knight/Silksong (não dá pra ver
        //o código-fonte deles, é fechado) e o padrão do gênero é
        //exatamente esse: uma rajada curta e "seca", onde o controle
        //volta instantaneamente ao normal assim que ela termina — não
        //um "glide" suave de saída. Tentei uma desaceleração suave
        //antes (smoothstep) e ficou "flutuando" demais pro estilo
        //pretendido; isso aqui é mais fiel.
        if (dashTimer > 0) {
            dashTimer -= tpf;

            if (dashTimer <= 0) {
                physics.setVelocityX(0);
            }
        }

        //reseta as habilidades aéreas sempre que tocar o chão
        if (physics.isOnGround()) {
            canDoubleJump = true;
            hasDashed = false;
        }
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
    //de HIT_STUN_DURATION. Público porque o WeaponComponent também
    //usa (não pode atacar durante o hit-stun).
    public boolean isStunned() {
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
    

    //Enquanto isStunned() (janela bem mais curta que a invencibilidade
    //inteira, ver HIT_STUN_DURATION), o player ignora comando de
    //movimento horizontal. Sem isso, segurar A ou D no momento do hit
    //sobrescreveria o recoil no frame seguinte e o soco nunca
    //apareceria de verdade. Depois desse tempinho curto o controle
    //volta, mesmo ainda invencível/piscando, pra dar tempo real do
    //player sair andando de perto de outros inimigos.

    
    // Só pula se estiver no chão ou se tiver o pulo duplo disponível. 
    // Evita pulo infinito no ar.
    public void jump() {
        if (isStunned()) return; // Ignora comando se estiver no hit-stun do knockback

        if (physics.isOnGround()) {
            physics.setVelocityY(JUMP_SPEED);
        } else if (canDoubleJump) {
            // Pulo duplo é levemente mais fraco e consome a flag até pisar no chão de novo
            physics.setVelocityY(JUMP_SPEED * 0.9); 
            canDoubleJump = false;
        }
    }

    // Investida horizontal rápida. Concede invencibilidade temporária.
    // Pode ser usada andando ou no meio de um pulo (só trava o eixo Y
    // e "sequestra" o controle horizontal por DASH_DURATION, ver
    // isDashing()/onUpdate() e moveLeft/moveRight/stopHorizontal).
    public void dash() {
        // Bloqueia se atordoado, se já usou no ar sem pisar no chão, ou se está em cooldown
        if (isStunned() || hasDashed || dashCooldownTimer > 0) return;

        double dashDirection = facingRight ? DASH_SPEED : -DASH_SPEED;
        physics.setVelocityX(dashDirection);
        physics.setVelocityY(0); // Zera o eixo Y para o dash ser perfeitamente reto no ar

        hasDashed = true;
        dashCooldownTimer = DASH_COOLDOWN;
        dashTimer = DASH_DURATION;
        invincibilityTimer = 0.2; // Pequena janela de invencibilidade (i-frames) durante o dash

        //Só chega aqui se o dash realmente aconteceu (passou pelas
        //travas lá em cima). Como dash() só roda a partir do toque no
        //Shift (ver TriggerListener em onAdded()), shiftHeld já está
        //true nesse instante — o bônus passa a valer em moveLeft/
        //moveRight() enquanto o Shift continuar segurado sem soltar.
        dashBoostActive = true;
    }

    //Enquanto o dash está "rolando" (ver dash()/onUpdate()), o
    //controle normal de horizontal não deve sobrescrever a velocidade
    //dele — segurar A/D (que chamam moveLeft/moveRight todo frame,
    //ver Main.initInput()) sobrescrevia a velocidade do dash no frame
    //seguinte e cancelava a investida quase inteira. Exposto também
    //pro WeaponComponent, se um dia quiser bloquear ataque durante o
    //dash.
    public boolean isDashing() {
        return dashTimer > 0;
    }

    // Dispara o ataque agora vive em WeaponComponent (attack()), que
    // usa esse isStunned() e o facingRight abaixo pra saber se pode
    // agir e pra que lado golpear/mirar.

    //Exposto pro WeaponComponent saber pra que lado o player está
    //olhando (mira do ataque melee/ranged).
    public boolean isFacingRight() {
        return facingRight;
    }

    public void moveLeft() {
        if (isStunned() || isDashing()) {
            return;
        }
        facingRight = false;
        physics.setVelocityX(-velocidadeComBoost());
    }

    public void moveRight() {
        if (isStunned() || isDashing()) {
            return;
        }
        facingRight = true;
        physics.setVelocityX(velocidadeComBoost());
    }

    //Aplica o pequeno bônus do dash (ver dashBoostActive/shiftHeld
    //acima) em cima da moveSpeed normal, sem duplicar essa conta nos
    //dois métodos de cima.
    private double velocidadeComBoost() {
        return dashBoostActive ? moveSpeed * DASH_BOOST_MULTIPLIER : moveSpeed;
    }

    //Chamado quando a seta de movimento é solta (onActionEnd em
    //Main.initInput()), zera a velocidade horizontal. Também respeita
    //o hit-stun curto,
    //mesmo motivo de moveLeft/moveRight: soltar a tecla no meio do
    //recoil não pode zerar o empurrão.
    public void stopHorizontal() {
        if (isStunned() || isDashing()) {
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