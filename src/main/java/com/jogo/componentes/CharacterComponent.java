package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;

/* Classe mestra pra qualquer bicho vivo no jogo.
Podendo tomar dano e se mover pelo mapa.
As subclasses (Enemy, Player, etc) vão herdar oq está aqui.

Regra Base:
    Só entra nessa superclasse o que faz sentido ter em QUALQUER personagem, sem
    exceção. Coisa específica (IA, dmg, controles, etc) Ficam nas sublcasses.

*/

public class CharacterComponent extends Component {
    // ATRIBUTOS
    //OBS: (Protected -> Private, mas as subclasses tem acesso.)
    protected String name;
    protected int maxHealth;
    protected int currentHealth;
    protected double moveSpeed;


    //CONSTRUTOR
    public CharacterComponent (String name, int maxHealth, double moveSpeed) {
        this.name = name; //Usa o this pra diff a var da classe da do construtor
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth; //cmc com vida cheia
        this.moveSpeed = moveSpeed;
    }

    //COMPORTAMENTOS COMUNS
    //Subclasses podem herdar como está aqui ou sobrescrever usando @override
    //se precisarem de algo diferente.
    //EX: Frames de invencibilidade no player no takeDamage
    public void takeDamage (int quant) {
        currentHealth -= quant;
        if (currentHealth < 0) {
            currentHealth = 0;
        }

        if (isDead()) {
            entity.removeFromWorld();
        }
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public void move() {
        //Preencher com as coisas do FXGL
    }

    //GETTERS (Coisa pra pegar coisa)
    public String getName() {
        return name;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    @Override
    public String toString() {
        return "Character{" +
                "name='" + name + '\'' +
                "hp=" + currentHealth + "/" + maxHealth +
                '}';
    }

    //FXGL guarda componente pela classe EXATA, então
    //getComponent(CharacterComponent.class) nunca acha nada (só
    //existem subclasses). Esse helper varre e acha por instanceof.
    public static CharacterComponent getFrom(Entity target) {
        for (Component c : target.getComponents()) {
            if (c instanceof CharacterComponent) {
                return (CharacterComponent) c;
            }
        }
        return null;
    }
}
