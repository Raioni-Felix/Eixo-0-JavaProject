package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.jogo.Main;

//Bixinho voador, herda de ComponenteInimigo,
// mas não precisa de nada específico, pq
//ComponenteInimigo já tem o atributo voador,
//  que é o que diferencia um inimigo voador de um inimigo terrestre.

public class ComponenteInimigoVoador extends ComponenteInimigo {

    //Só pra efeito visual e fazer o inimigo voar subindo e descendo
    //em loop. Nada a ver com perseguir o alvo, só aesthetic mesmo.

    //Tudo fixo (por agora) pra facilitar minha vida
    private double tempoFlutuando = 0;
    private final double amplitudeFlutuacao = 30; //Amplitude, em pixels/segundo (velocidade)
    private final double velocidadeFlutuacao = 2; //"frequência" da subida e descida


    //CONSTRUTOR
    //Super chama o construtor da superclasse ComponenteInimigo, já fixando
    //voador=true, distancia=false, agressivo=true — um
    //ComponenteInimigoVoador SEMPRE é assim, quem cria não escolhe isso.
    public ComponenteInimigoVoador(String nome, int vidaMaxima, double velocidade,
            int dano, double alcanceAtaque, double alcanceDeteccao) {

        super(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao,
                true, false, true);
    }

    @Override
    public void onUpdate(double tpf) {
        //Ignora a rajada de ticks logo após despausar (ver Main.java),
        //senão tempoFlutuando dá um "pulo" no voo.
        if (Main.emPeriodoDeGraca()) {
            return;
        }

        if (!agressivo || alvo == null) {
            // Se não for agressivo ou não tiver alvo, apenas flutua
            flutuar(tpf);
        } else {
            // Se for agressivo e tiver alvo, segue o alvo
            super.onUpdate(tpf); //Persegue e ataca já
            // usando alcanceAtaque
        }
    }

    @Override
    public void atacar(Entity alvo) {
        aplicarDanoDireto(alvo);
    }

    //Seta uma velocidade vertical oscilante (corpo é DYNAMIC com
    //gravityScale 0, ver FabricaEntidades física de verdade, sem
    //gravidade).
    private void flutuar(double tpf) {
        tempoFlutuando += tpf * velocidadeFlutuacao;
        double velocidadeVertical = Math.cos(tempoFlutuando) * amplitudeFlutuacao;
        fisica.setLinearVelocity(0, velocidadeVertical);
    }
}
