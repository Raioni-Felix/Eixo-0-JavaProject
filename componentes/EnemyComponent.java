package com.jogo.componentes;

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
    private double timeSinceLastAttack = 0;
    
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
    
    //COMPORTAMENTO DO INIMIGO
    
    public void setTarget(Entity target) {
        this.target = target;
    }
    
       
    
    public void attack(Entity target) {
        //Logica de ataque integrado do FXGL
        //Só ataque se estiver no range
        if (entity.distance(target) > attackRange) {
            return;
        }
        
        
        //IMPORTANTE ISSO. Explicar depois
        target.getComponent(CharacterComponent.class).takeDamage(damage);
    }
    
    //onUpdate roda cada frame do jogo
    @Override
    public void onUpdate (double tpf) {
        if (!isAggressive) {
            return;
        }
        
        if (target == null) {
            return;
        }
        
        timeSinceLastAttack += tpf;
        
        boolean inRange = entity.distance(target) <= attackRange;
        boolean endCooldown = timeSinceLastAttack >= attackCooldownSeconds;
        
        if (inRange && endCooldown) {
            attack(target);
            timeSinceLastAttack = 0;
        }
    }
    
    //GETTERS DO INIMIGO
    public int getDamage() {
        return damage;
    }
    
    public double getAttackRange() {
        return attackRange;
    }
    
    public double getDetectionRange(){
        return detectionRange;
    }
    
    public boolean isFlying() {
        return isFlying;
    }
    
    public boolean isRanged() {
        return isRanged;
    }
    
    public boolean isAggressive() {
        return isAggressive;
    }
    
    
    @Override
    public String toString() {
            return "Enemy{" +
                "name='" + name + '\'' +
                ", hp=" + currentHealth + "/" + maxHealth +
                ", damage=" + damage +
                ", flying=" + isFlying +
                ", ranged=" + isRanged +
                '}';
    }
}
