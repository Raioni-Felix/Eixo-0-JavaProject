package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/* Classe mestra pra qualquer bicho vivo no jogo.
Podendo tomar dano e se mover pelo mapa.
As subclasses (Inimigo, Jogador, etc) vão herdar oq está aqui.

Regra Base:
    Só entra nessa superclasse o que faz sentido ter em QUALQUER personagem, sem
    exceção. Coisa específica (IA, dmg, controles, etc) Ficam nas sublcasses.

*/

public class ComponentePersonagem extends Component implements Atacavel {
    // ATRIBUTOS
    //OBS: (Protected -> Private, mas as subclasses tem acesso.)
    protected String nome;
    protected int vidaMaxima;
    protected double velocidade;
    // Substituição do int por uma Propriedade Observável
    protected IntegerProperty vidaAtual = new SimpleIntegerProperty();


    //CONSTRUTOR
    public ComponentePersonagem (String nome, int vidaMaxima, double velocidade) {
        //Validação: erro de quem escreveu o código (ou o JSON), não algo
        //que acontece jogando. Por isso IllegalArgumentException
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Personagem precisa de um nome");
        }
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("Vida máxima deve ser maior que zero: " + vidaMaxima);
        }
        if (velocidade < 0) {
            throw new IllegalArgumentException("Velocidade não pode ser negativa: " + velocidade);
        }
        this.nome = nome; //Usa o this pra diff a var da classe da do construtor
        this.vidaMaxima = vidaMaxima;
        this.velocidade = velocidade;

        //Inicialização do valor interno da propriedade com vida cehia
        this.vidaAtual.set(vidaMaxima);
    }

    //COMPORTAMENTOS COMUNS
    //Subclasses podem herdar como está aqui ou sobrescrever usando @override
    //se precisarem de algo diferente.
    //EX: Frames de invencibilidade no jogador no tomarDano
    @Override
    public void tomarDano(int quant) {
        // Atualizamos o estado interno através dos métodos da Propriedade
        int novaVida = vidaAtual.get() - quant;
        vidaAtual.set(Math.max(novaVida, 0));

        if (estaMorto()) {
            entity.removeFromWorld();
        }
    }

    //Trazido do barebones: a vida só sobe por aqui, e o próprio
    //personagem garante as regras (não passa da máxima, não revive
    //morto). Quem cura (ex: ModuloCura) só diz quanto.
    public void curar(int quantidade) {
        if (quantidade <= 0 || estaMorto()) {
            return;
        }
        vidaAtual.set(Math.min(vidaMaxima, vidaAtual.get() + quantidade));
    }

    // A UI pode observar as mudanças, mas não pode usar o método .set() para trapacear.
    public ReadOnlyIntegerProperty vidaAtualProperty() {
        return vidaAtual;
    }

    @Override
    public boolean estaMorto() {
        // CORREÇÃO: Necessário usar .get() para extrair o primitivo int antes de comparar
        return vidaAtual.get() <= 0;
    }

    public void mover() {
        //Preencher com as coisas do FXGL
    }

    //GETTERS (Coisa pra pegar coisa)
    public String getNome() {
        return nome;
    }

    public int getVidaAtual() {
        return vidaAtual.get();
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    /**
     * Define a vida atual diretamente (usado por efeitos como Cyberpsicose).
     * Público para uso por efeitos de status em pacotes diferentes.
     */
    public void setVidaAtual(int vida) {
        this.vidaAtual.set(Math.max(0, Math.min(vida, vidaMaxima)));
    }

    public double getVelocidade() {
        return velocidade;
    }

    /**
     * Define a velocidade de movimento (usado por efeitos como Cyberpsicose).
     */
    public void setVelocidade(double velocidade) {
        if (velocidade < 0) {
            throw new IllegalArgumentException("Velocidade não pode ser negativa: " + velocidade);
        }
        this.velocidade = velocidade;
    }

    @Override
    public String toString() {
        // Usar .get() para evitar imprimir a referência de memória do objeto Property
        return "Personagem{" +
                "nome='" + nome + '\'' +
                "vida=" + vidaAtual.get() + "/" + vidaMaxima +
                '}';
    }
    //FXGL guarda componente pela classe EXATA, então
    //getComponent(ComponentePersonagem.class) nunca acha nada (só
    //existem subclasses). Esse helper varre e acha por instanceof.
    public static ComponentePersonagem obterDe(Entity alvo) {
        for (Component c : alvo.getComponents()) {
            if (c instanceof ComponentePersonagem) {
                return (ComponentePersonagem) c;
            }
        }
        return null;
    }
}
