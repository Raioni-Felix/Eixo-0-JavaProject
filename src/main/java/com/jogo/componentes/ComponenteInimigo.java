package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.jogo.Main;


/**
 * Classe base pra todos os inimigos (dano de ataque, alcance,
 * voador/distância/agressivo). Abstrata: nunca instanciada sozinha, só
 * dá base às subclasses (ComponenteInimigoMelee/Voador/Distancia).
 * atacar(Entity) é abstrato de propósito, cada subclasse decide como
 * ataca (dano direto ou projétil).
 */

public abstract class ComponenteInimigo extends ComponentePersonagem {

    //Atributos específicos do inimigo
    protected int dano;
    protected double alcanceAtaque;
    protected double alcanceDeteccao;

    protected boolean voador;
    protected boolean distancia;
    protected boolean agressivo;

    //Alvo que o inimigo vai perseguir/atacar
    //Não preenche no construtor pq quando é criado não possui alvo até ser agressivo
    //definirAlvo é pra isso
    protected Entity alvo;

    //tempo mínimo entre os ataques (cooldown)
    protected double cooldownAtaqueSegundos = 1.0;
    protected double tempoDesdeUltimoAtaque = 0;

    //PhysicsComponent do FXGL, movimento (perseguição, hover) passa
    //por aqui agora, via setLinearVelocity/setVelocityX.
    protected PhysicsComponent fisica;

    //CONSTRUTOR
    //Super chama o construtor da superclasse ComponentePersonagem
    //Sempre primeira linha, pq a parte comum do objeto
    //precisa existir antes de fazer a específica do inimigo.
    public ComponenteInimigo(String nome, int vidaMaxima, double velocidade, int dano,
            double alcanceAtaque, double alcanceDeteccao,
            boolean voador, boolean distancia, boolean agressivo) {

        super(nome, vidaMaxima, velocidade);

        this.dano = dano;
        this.alcanceAtaque = alcanceAtaque;
        this.alcanceDeteccao = alcanceDeteccao;
        this.voador = voador;
        this.distancia = distancia;
        this.agressivo = agressivo;
    }

    @Override
    public void onAdded() {
        fisica = entity.getComponent(PhysicsComponent.class);
    }

    //COMPORTAMENTO DO INIMIGO

    public void definirAlvo(Entity alvo) {
        this.alvo = alvo;
    }

    //Quanto de dano esse inimigo causa (usado no contato com o jogador)
    public int getDano() {
        return dano;
    }

    //Cada subclasse decide como ataca, ver aplicarDanoDireto() pro
    //caso comum de melee/voador.
    public abstract void atacar(Entity alvo);

    //Dano direto (acha o ComponentePersonagem do alvo e aplica),
    //reusado por ComponenteInimigoMelee e ComponenteInimigoVoador.
    //ComponenteInimigoDistancia não usa: o dano dele só aplica quando o
    //projétil acerta de verdade.
    protected void aplicarDanoDireto(Entity alvo) {
        if (alvo == null || !alvo.isActive()) {
            return;
        }

        ComponentePersonagem personagem = ComponentePersonagem.obterDe(alvo);

        //Se o alvo é o jogador, usa serAtingido() (dá knockback e
        //invencibilidade também, igual o toque físico e o projétil).
        if (personagem instanceof ComponenteJogador) {
            ((ComponenteJogador) personagem).serAtingido(entity, dano);
        } else if (personagem != null) {
            personagem.tomarDano(dano);
        }
    }

    //FXGL guarda componente pela classe EXATA (ComponenteInimigoVoador
    //etc.), então getComponent(ComponenteInimigo.class) nunca acha nada.
    //Esse helper varre os componentes reais e devolve o que É/herda
    //de ComponenteInimigo, não importa o tipo concreto.
    public static ComponenteInimigo obterDe(Entity alvo) {
        for (Component c : alvo.getComponents()) {
            if (c instanceof ComponenteInimigo) {
                return (ComponenteInimigo) c;
            }
        }
        return null;
    }

    //onUpdate roda cada frame do jogo
    @Override
    public void onUpdate(double tpf) {
        //Ignora a rajada de ticks logo após despausar (ver Main.java).
        if (Main.emPeriodoDeGraca()) {
            return;
        }

        // !alvo.isActive() cobre o caso do alvo já ter morrido
        // (removido do mundo), sem isso, o inimigo continua
        // perseguindo/atacando um alvo que não existe mais.
        if (!agressivo || alvo == null || !alvo.isActive()) {
            return;
        }

        perseguirAlvo(tpf, alcanceAtaque);
    }

    //Persegue o alvo até distanciaParada, depois para e ataca. Voador
    //(voador) persegue nos dois eixos (física própria, sem
    //gravidade); terrestre só no X (Y é a gravidade do corpo DYNAMIC).
    protected void perseguirAlvo(double tpf, double distanciaParada) {
        double distancia = entity.distance(alvo);

        if (distancia > distanciaParada) {
            double dx = alvo.getX() - entity.getX();
            double dy = alvo.getY() - entity.getY();

            if (voador) {
                double comprimento = Math.hypot(dx, dy);
                double vx = comprimento > 0 ? (dx / comprimento) * velocidade : 0;
                double vy = comprimento > 0 ? (dy / comprimento) * velocidade : 0;
                fisica.setLinearVelocity(vx, vy);
            } else {
                fisica.setVelocityX(dx > 0 ? velocidade : -velocidade);
            }
        } else {
            // Perto o suficiente pra atacar: para de andar. Pro
            // terrestre só zera a horizontal (deixa a gravidade seguir
            // seu curso, senão ele "flutuaria" parado no ar).
            if (voador) {
                fisica.setLinearVelocity(0, 0);
            } else {
                fisica.setVelocityX(0);
            }
        }

        tempoDesdeUltimoAtaque += tpf;
        boolean noAlcance = distancia <= distanciaParada;
        boolean fimCooldown = tempoDesdeUltimoAtaque >= cooldownAtaqueSegundos;

        if (noAlcance && fimCooldown) {
            atacar(alvo);
            tempoDesdeUltimoAtaque = 0;
        }
    }
}
