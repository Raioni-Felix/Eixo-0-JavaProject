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
 *   NpcsDasSalas.criar(sala);
 *
 * Em cada sala (menos a arena do chefe) aparecem:
 *  - o Informante, perto do começo, dando dicas;
 *  - a Curandeira, perto do fim da sala (antes da porta pra próxima), com
 *    1 cura. Como as salas são recriadas a cada troca, ela volta a ter a
 *    cura quando o jogador reentra na sala.
 *
 * Os NPCs nascem no ar (Y_INICIAL) e caem sozinhos até o chão (y = 680 em
 * todas as salas). Pra mudar posição, falas ou quantidade de curas, é só
 * editar aqui.
 */
public final class NpcsDasSalas {

    private static final double Y_INICIAL = 560;
    private static final double X_INFORMANTE = 560;
    private static final double DISTANCIA_CURANDEIRO_DO_FIM = 500;

    private NpcsDasSalas() {
    }

    public static void criar(DadosNivel sala) {
        if (temChefe(sala)) {
            return; //arena do chefe fica sem NPCs
        }

        spawn("npc_informante", new SpawnData(X_INFORMANTE, Y_INICIAL)
                .put("nome", "Informante")
                .put("falas", List.of(
                        "Olá! Há armas e módulos espalhados pelas salas.",
                        "Aperte C na hora certa pra aparar um golpe sem tomar dano.",
                        "No fim do caminho existe um chefe. Chegue com a vida cheia!")));

        spawn("npc_curandeiro", new SpawnData(sala.larguraNivel - DISTANCIA_CURANDEIRO_DO_FIM, Y_INICIAL)
                .put("nome", "Curandeira")
                .put("falas", List.of(
                        "Você parece ferida. Fique perto que eu cuido disso.",
                        "Só tenho uma cura por sala, então use com sabedoria."))
                .put("curas", 1));
    }

    //Mesma regra que o GerenciadorSalas usa pra reconhecer a sala do chefe
    private static boolean temChefe(DadosNivel sala) {
        return sala.inimigos.stream().anyMatch(i -> "chefe".equals(i.tipo));
    }
}
