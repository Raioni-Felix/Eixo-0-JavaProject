package com.jogo.componentes;

import com.jogo.componentes.TimeScaleManager;

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

    //Depois de bater, o voador sobe por um instante antes de voltar a
    //perseguir ("bate e sobe"). Enquanto > 0, onUpdate() só sobe.
    private static final double DURACAO_RECUO = 0.4;
    private double tempoRecuo = 0;


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
        //Ignora a rajada de ticks logo após despausar (ver RelogioJogo),
        //senão tempoFlutuando dá um "pulo" no voo.
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }
        
        // Aplica time scale do Sandevistan
        double tpfEfetivo = TimeScaleManager.getTpfPara(tpf, this);
        
        //Congelado: fica parado (ver ComponenteInimigo.congelar)
        if (atualizarCongelamento(tpfEfetivo)) {
            return;
        }

        if (tempoRecuo > 0) {
            tempoRecuo -= tpfEfetivo;
            fisica.setLinearVelocity(0, -velocidade); //sobe na velocidade dele
            return;
        }

        if (!agressivo || alvo == null) {
            // Se não for agressivo ou não tiver alvo, apenas flutua
            flutuar(tpfEfetivo);
        } else {
            // Se for agressivo e tiver alvo, segue o alvo
            super.onUpdate(tpfEfetivo); //Persegue e ataca já
            // usando alcanceAtaque
        }
    }

    //Sobrescreve reaproveitando: super.iniciarAtaque() faz o golpe
    //normal de inimigo (dano direto), e depois vem o que é só do
    //voador, o recuo pra cima. Mesma ideia do Paladino da atividade de
    //RPG, que chama parent::atacar() e acrescenta o efeito dele.
    @Override
    public void iniciarAtaque() {
        super.iniciarAtaque();
        tempoRecuo = DURACAO_RECUO;
    }

    @Override
    public int getExperienciaConcedida() {
        return 25;
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
