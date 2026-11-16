package com.jogo.npcs;

import com.almasb.fxgl.entity.SpawnData;
import com.jogo.niveis.DadosNivel;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.spawn;

/**
 * Cria os NPCs de uma sala. Existe pra ter UM lugar só com os NPCs: a sala
 * inicial é montada no ControleJogo.iniciarMundo() e as outras no
 * GerenciadorSalas.trocarSala(), e os dois chamam isto aqui:
 *
 *   NpcsDasSalas.criar(sala, indiceDaSala, totalDeSalas);
 *
 *  - o Informante só aparece na PRIMEIRA sala, dando as dicas do começo;
 *  - a Curandeira só aparece na PENÚLTIMA sala, perto da porta da arena
 *    do chefe (última chance de se curar). Cada cura custa energia da
 *    jogadora (NpcCurandeiro).
 *  - o próprio chefe começa como NPC (NpcChefe), mas quem cria ele é o
 *    GerenciadorSalas, junto com os inimigos da sala.
 *
 * Os NPCs nascem no ar (Y_INICIAL) e caem sozinhos até o chão (y = 680 em
 * todas as salas). Pra mudar posição, falas ou quantidade de curas, é só
 * editar aqui.
 */
public final class NpcsDasSalas {

    private static final double Y_INICIAL = 560;
    private static final double X_INFORMANTE = 560;
    private static final double DISTANCIA_CURANDEIRA_DO_FIM = 500;
    private static final int CURAS_DA_CURANDEIRA = 3;

    private NpcsDasSalas() {
    }

    public static void criar(DadosNivel sala, int indiceSala, int totalSalas) {
        if (temInformante(indiceSala)) {
            spawn("npc_informante", new SpawnData(X_INFORMANTE, Y_INICIAL)
                    .put("nome", "Informante")
                    .put("falas", List.of(
                            "Olá! Há armas e módulos espalhados pelas salas.",
                            "Aperte C na hora certa pra aparar um golpe sem tomar dano.",
                            "Cuidado com os espinhos e as prensas do caminho.",
                            "Antes do chefe há uma curandeira. Aperte Enter pra passar as falas.")));
        }

        if (temCurandeira(indiceSala, totalSalas)) {
            spawn("npc_curandeiro", new SpawnData(sala.larguraNivel - DISTANCIA_CURANDEIRA_DO_FIM, Y_INICIAL)
                    .put("nome", "Curandeira")
                    .put("falas", List.of(
                            "Fique perto que eu cuido dos seus ferimentos.",
                            "Mas cada cura custa " + NpcCurandeiro.CUSTO_ENERGIA_PADRAO + " da sua energia.",
                            "O NEXUS espera depois daquela porta. Tenho " + CURAS_DA_CURANDEIRA + " curas, use bem."))
                    .put("curas", CURAS_DA_CURANDEIRA));
        }
    }

    //Regras de onde cada NPC aparece (separadas pra dar pra testar sem o motor)
    public static boolean temInformante(int indiceSala) {
        return indiceSala == 0;
    }

    public static boolean temCurandeira(int indiceSala, int totalSalas) {
        return indiceSala == totalSalas - 2;
    }
}
