package com.jogo.itens;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//Mesmo pacote das armas: dá pra testar os "ganchos" protected que cada
//filha da ArmaDeFogo sobrescreve
class ArmasDeFogoTest {

    private final ArmaDeFogo comum = new ArmaDeFogo("f", "Fogo", "", 10, 0.5, 400);
    private final Shotgun shotgun = new Shotgun("s", "Shotgun", "", 8, 5, 0.9, 220);
    private final PistolaDuplaDePlasma pistola = new PistolaDuplaDePlasma("p", "Pistola", "", 9, 0.45, 450);
    private final RifleSniper rifle = new RifleSniper("r", "Rifle", "", 30, 1.4, 900, 3);

    @Test
    @DisplayName("Valores padrão da ArmaDeFogo")
    void padrao() {
        assertEquals(1, comum.getAlvosPorDisparo());
        assertEquals(650, comum.getVelocidadeProjetil());
        assertEquals(8, comum.getTamanhoProjetil());
        assertEquals(Color.GOLD, comum.getCorProjetil());
    }

    @Test
    @DisplayName("RifleSniper é Perfurante: o projétil acerta N alvos e é mais rápido")
    void rifle() {
        Perfurante perfurante = rifle;
        assertEquals(3, perfurante.getAlvosAtravessados());
        assertEquals(3, rifle.getAlvosPorDisparo());
        assertTrue(rifle.getVelocidadeProjetil() > comum.getVelocidadeProjetil());
    }

    @Test
    @DisplayName("Cada filha muda só o que precisa")
    void sobrescritas() {
        assertEquals(Color.MAGENTA, pistola.getCorProjetil());
        assertEquals(1, pistola.getAlvosPorDisparo()); //herdado
        assertEquals(6, shotgun.getTamanhoProjetil());
        assertEquals(Color.GOLD, shotgun.getCorProjetil()); //herdado
    }

    @Test
    @DisplayName("Coleção polimórfica: mesma chamada, descrição diferente")
    void descricoes() {
        List<Arma> arsenal = List.of(new Espada("e", "Espada", "", 20, 0.5, 40), shotgun, pistola, rifle);
        List<String> esperado = List.of("corte de 20", "5 balins de 8", "2 tiros de 9", "tiro de 30 que atravessa 3");
        for (int i = 0; i < arsenal.size(); i++) {
            assertEquals(esperado.get(i), arsenal.get(i).getDescricaoCombate());
        }
    }
}
