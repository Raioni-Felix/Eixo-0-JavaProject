package com.jogo.npcs;

import com.jogo.componentes.ComponentePersonagem;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.function.Consumer;

/**
 * O chefe antes da luta: NEXUS começa como um NPC (invulnerável, não
 * ataca) que conversa com a jogadora. Quando ela ouve TODAS as falas, o
 * NPC some e no lugar dele nasce o chefe de verdade (ComponenteChefe),
 * e a luta começa. Se ela se afastar no meio, a conversa recomeça.
 *
 * Quem cria o chefe é quem montou a sala (GerenciadorSalas), através do
 * aoDespertar: o NPC só avisa a posição onde estava. Assim o NPC não
 * precisa saber vida, dano etc. do chefe.
 */
public class NpcChefe extends ComponenteNpc {

    //Ele flutua bem acima do chão, então o alcance da conversa é maior
    public static final double ALCANCE_CONVERSA_CHEFE = 320;

    private final Consumer<Point2D> aoDespertar;
    private boolean conversaCompleta = false;
    private boolean despertou = false;

    public NpcChefe(String nome, List<String> falas, Consumer<Point2D> aoDespertar) {
        super(nome, falas);
        if (aoDespertar == null) {
            throw new IllegalArgumentException("O chefe precisa saber o que fazer ao despertar");
        }
        this.aoDespertar = aoDespertar;
    }

    @Override
    protected void aoConversar(ComponentePersonagem jogador) {
        //só fala; a luta começa quando as falas acabam
    }

    @Override
    protected void aoTerminarFalas() {
        conversaCompleta = true;
    }

    @Override
    protected double getAlcanceConversa() {
        return ALCANCE_CONVERSA_CHEFE;
    }

    public boolean conversaCompleta() {
        return conversaCompleta;
    }

    //Troca o NPC pelo chefe (uma vez só). Separado do onUpdate pra dar pra testar.
    public boolean despertarSePronto(Point2D posicao) {
        if (!conversaCompleta || despertou) {
            return false;
        }
        despertou = true;
        aoDespertar.accept(posicao);
        return true;
    }

    @Override
    public void onUpdate(double tpf) {
        super.onUpdate(tpf);
        if (despertarSePronto(entity.getPosition())) {
            entity.removeFromWorld();
        }
    }
}
