package com.jogo.niveis;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

//Lê um arquivo JSON de sala (em src/main/resources) e devolve um
//DadosNivel já populado. Usa Gson (com.google.code.gson:gson, ver
//pom.xml) só pra converter o JSON pro objeto DadosNivel/DadosJogador
///DadosPlataforma/DadosInimigo direto, sem parsing manual.
//
//Cada sala é carregada por vez, com as posições exatamente como estão
//no JSON (relativas ao início daquela sala) — ver
//Main.prepararSala()/trocarSala(), que decide qual sala carregar e
//troca o conteúdo em cena quando o player passa por um gatilho, sem
//juntar várias salas num mapa só.
public class CarregadorDeNiveis {

    private CarregadorDeNiveis() {
        //Classe utilitária, só o método estático importa.
    }

    public static DadosNivel carregar(String caminhoRecurso) {
        try (InputStream stream = CarregadorDeNiveis.class.getResourceAsStream(caminhoRecurso)) {
            if (stream == null) {
                throw new IllegalStateException("Nível não encontrado: " + caminhoRecurso);
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            DadosNivel nivel = new Gson().fromJson(reader, DadosNivel.class);

            if (nivel == null) {
                throw new IllegalStateException("Nível vazio ou mal formatado: " + caminhoRecurso);
            }

            return nivel;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar nível: " + caminhoRecurso, e);
        }
    }
}
