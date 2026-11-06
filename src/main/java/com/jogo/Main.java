package com.jogo;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.jogo.controle.ControleEntrada;
import com.jogo.controle.ControleJogo;

/**
 * Ponto de entrada do jogo.
 *
 * Todo jogo FXGL precisa de uma classe que estenda GameApplication. O
 * FXGL chama os métodos init...() e onUpdate() na hora certa, e o Main
 * só repassa cada etapa pro controle (pacote controle). Antes este
 * arquivo tinha cerca de 1.400 linhas misturando telas, HUD, teclas e
 * regras; na separação MVC ficou assim:
 *
 *   Modelo:   componentes, itens, habilidades, inventario, niveis,
 *             entidades, factories, excecoes (dados e regras do jogo)
 *   Visão:    visao (HUD, telas, cortina) e componentes.visual
 *   Controle: controle (ControleJogo, ControleEntrada, GerenciadorSalas)
 *
 * Os controles são criados uma vez só e sobrevivem ao startNewGame()
 * (Reiniciar/Menu), que chama initGame()/initPhysics()/initUI() de novo.
 */
public class Main extends GameApplication {

    private final ControleJogo controle = new ControleJogo();
    private final ControleEntrada entrada = new ControleEntrada(controle);

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Eixo 0");
        settings.setVersion("0.1");

        //Fullscreen disponível (F11), mas abre em janela normal
        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(false);

        //Desliga o menu principal, a intro e o menu interno (ESC) padrões
        //do FXGL: o jogo tem os próprios (pacote visao), e os do FXGL têm
        //um "Exit" que fecha o jogo direto
        settings.setMainMenuEnabled(false);
        settings.setIntroEnabled(false);
        settings.setGameMenuEnabled(false);
    }

    @Override
    protected void initGame() {
        controle.iniciarMundo();
    }

    @Override
    protected void initPhysics() {
        controle.registrarColisoes();
    }

    @Override
    protected void initInput() {
        entrada.registrar();
    }

    @Override
    protected void initUI() {
        controle.montarInterface();
    }

    @Override
    protected void onUpdate(double tpf) {
        controle.atualizar(tpf);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
