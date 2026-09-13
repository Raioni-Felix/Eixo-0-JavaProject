package com.jogo.itens;
public class Weapon {
    //atributos da classe
    private String name;
    private int damage;
    private double coolDown;
    private double range;
    private WeaponType type;
    
    private double damageMultiplier;
    private double lastUse;
    public Weapon(String name, int damage, double coolDown, double range, WeaponType type){
        this.name = name;
        this.damage = damage;
        this.coolDown = coolDown;
        this.range = range;
        this.type = type;
        this.damageMultiplier = 1;
        this.lastUse = 0;
    }
    public int getTotalDamage(){
        return (int) Math.round(damage * damageMultiplier);
    }
    // Verifica se o cooldown já passou
    public boolean canAttack() {
        long now = System.currentTimeMillis();
        double secondsUtilLastUse = (now - lastUse) / 1000.0;
        return secondsUtilLastUse >= coolDown;
    }

    // Executa o ataque, se possível
    public boolean attack() {
        if (!canAttack()) {
            System.out.println(name + " ainda em cooldown!");
            return false;
        }
        lastUse = System.currentTimeMillis();
        System.out.println(name + " atacou causando " + getTotalDamage() + " de dano.");
        return true;
    }

    // Quanto tempo falta pro cooldown acabar (útil pra UI, tipo barra de cooldown)
    public double getCooldownRemaining() {
        long now = System.currentTimeMillis();
        double past = (now - lastUse) / 1000.0;
        double remaining = coolDown - past;
        return Math.max(0, remaining);
    }

    // Aplica um upgrade/buff de dano (ex: encontrou um item que aumenta dano em 20%)
    public void damageMultiplier(double multiplier) {
        this.damageMultiplier *= multiplier;
    }

    // Getters básicos
    public String getName() { return name; }
    public int getDamage() { return damage; }
    public double getCooldownSegundos() { return coolDown; }
    public WeaponType getType() { return type ; }
    public double getRange() { return range; }
}
