package com.jogo.niveis;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.jogo.excecoes.NivelInvalidoException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

//Lê um arquivo JSON de sala (em src/main/resources) e devolve um
//DadosNivel já populado. Usa Gson (com.google.code.gson:gson, ver
//pom.xml) só pra converter o JSON pro objeto DadosNivel/DadosJogador
///DadosPlataforma/DadosInimigo direto, sem parsing manual.
//
//Cada sala é carregada por vez, com as posições exatamente como estão
//no JSON (relativas ao início daquela sala) — ver
//controle.GerenciadorSalas (prepararSala()/trocarSala()), que decide qual sala carregar e
//troca o conteúdo em cena quando o player passa por um gatilho, sem
//juntar várias salas num mapa só.
public class CarregadorDeNiveis {

    private CarregadorDeNiveis() {
        //Classe utilitária, só o método estático importa.
    }

    //Antes qualquer problema virava RuntimeException genérica. Agora é
    //uma exceção própria e "checked": quem carrega uma sala é obrigado a
    //decidir o que fazer se o arquivo não existir ou estiver quebrado.
    public static DadosNivel carregar(String caminhoRecurso) throws NivelInvalidoException {
        try (InputStream stream = CarregadorDeNiveis.class.getResourceAsStream(caminhoRecurso)) {
            if (stream == null) {
                throw new NivelInvalidoException(caminhoRecurso, "arquivo não encontrado");
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            DadosNivel nivel = new Gson().fromJson(reader, DadosNivel.class);

            if (nivel == null) {
                throw new NivelInvalidoException(caminhoRecurso, "arquivo vazio");
            }
            //Sem jogador ou sem plataformas a sala não tem como ser montada
            if (nivel.jogador == null || nivel.plataformas == null || nivel.plataformas.isEmpty()) {
                throw new NivelInvalidoException(caminhoRecurso, "faltam \"player\" ou \"platforms\"");
            }
            if (nivel.inimigos == null) {
                nivel.inimigos = new ArrayList<>(); //sala sem inimigos é válida
            }

            return nivel;
        } catch (JsonParseException | IOException e) {
            throw new NivelInvalidoException(caminhoRecurso, e);
        }
    }
}
