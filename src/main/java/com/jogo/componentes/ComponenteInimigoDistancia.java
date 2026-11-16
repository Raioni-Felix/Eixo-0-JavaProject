package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.jogo.componentes.TimeScaleManager;

import static com.almasb.fxgl.dsl.FXGL.spawn;


//Inimigo atirador herda de ComponenteInimigo
//Tem que parar de andar e atirar assim que o jogador estiver
//no alcanceDeteccao (q é maior)
//Sobrescreve o onUpdate pra parar de andar e atirar, e não perseguir o jogador
public class ComponenteInimigoDistancia extends ComponenteInimigo {

        //CONSTRUTOR
    //Super chama o construtor da superclasse ComponenteInimigo, já fixando
    //voador=false, distancia=true, agressivo=true

    public ComponenteInimigoDistancia(String nome, int vidaMaxima, double velocidade,
            int dano, double alcanceAtaque, double alcanceDeteccao) {

        super(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao,
                false, true, true);

        //Cadência mais lenta que o padrão (1.0s, pensado pro corpo a
        //corpo), dá tempo do jogador se esquivar entre um tiro e outro.
        cooldownAtaqueSegundos = 2.5;
    }

    @Override
    public void onUpdate(double tpf) {
        //Ignora a rajada de ticks logo após despausar (ver RelogioJogo).
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }
        
        // Aplica time scale do Sandevistan
        double tpfEfetivo = TimeScaleManager.getTpfPara(tpf, this);
        
        //Congelado: fica parado (ver ComponenteInimigo.congelar)
        if (atualizarCongelamento(tpfEfetivo)) {
            return;
        }

        // !alvo.isActive() cobre o alvo já ter morrido, sem isso o
        // atirador continua "atirando" num jogador que já não existe
        // mais (e ficaria assim pra sempre, já que agressivo nunca
        // vira false sozinho).
        if (!agressivo || alvo == null || !alvo.isActive()) {
            return;
        }

        // Usa alcanceDeteccao como distância de parada, em vez de
        // alcanceAtaque (que seria corpo a corpo). Persegue até ficar
        // "de longe o suficiente" pra atirar, não precisa encostar
        perseguirAlvo(tpfEfetivo, alcanceDeteccao);
    }


    //Sobrescreve por inteiro, sem super: o atirador não bate, atira. O
    //dano NÃO é aplicado aqui, só o ComponenteProjetil aplica, e só
    //quando o projétil realmente alcança o alvo.
    @Override
    public void iniciarAtaque() {
        dispararProjetil(alvo);
    }

    @Override
    public int getExperienciaConcedida() {
        return 35;
    }

    // Cria o projétil de verdade: agora passa pela FabricaEntidades
    // (@Spawns("projetil")) em vez de montar a entidade na mão aqui,
    // essa é a parte do padrão do NetBeans (EntityFactory + @Spawns)
    // que foi incorporada. O SpawnData carrega a posição (x, y) e os
    // dados extras que o ComponenteProjetil precisa: origem (pra
    // calcular a direção fixa da bala), alvo, dano e velocidade.
    private void dispararProjetil(Entity alvo) {
        SpawnData dados = new SpawnData(entity.getX(), entity.getY())
                .put("origem", entity)
                .put("alvo", alvo)
                .put("dano", dano)
                .put("velocidade", 500.0);

        spawn("projetil", dados);
    }
}
