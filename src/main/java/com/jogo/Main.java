package com.jogo;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import com.jogo.componentes.PlayerComponent;
import javafx.scene.input.KeyCode;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * Ponto de entrada do jogo.
 *
 * Todo jogo FXGL precisa de uma classe que estenda GameApplication
 * e implemente initSettings(). O método main() só chama launch(),
 * que dispara a inicialização do motor.
 */
public class Main extends GameApplication {

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("Eixo 0");
        settings.setVersion("0.1");

        // Desliga menu principal e intro por enquanto: pra prototipar
        // e testar rápido, é melhor cair direto no jogo. Reativa isso
        // quando já tiver tela de menu/assets pra ele.
        settings.setMainMenuEnabled(false);
        settings.setIntroEnabled(false);
    }

    @Override
    protected void initGame() {
        // Player com textura provisória (quadrado azul) só pra já dar
        // pra ver na tela e testar movimento e XP. Troca por sprite
        // de verdade depois, em src/main/resources/assets/textures/.
        player = entityBuilder()
                .at(400, 300)
                .viewWithBBox("player.png")
                .with(new PlayerComponent("Herói", 100, 200))
                .buildAndAttach();
    }

    @Override
    protected void initInput() {
        // DEBUG: println em onActionBegin() só pra confirmar que a tecla
        // está mesmo chegando no jogo. Se apertar e NADA aparecer no
        // terminal, o problema é a janela sem foco (a tecla nem chega
        // no FXGL). Se aparecer o println mas o quadrado não mover, o
        // problema é no movimento em si. Remove isso depois de testar.
        getInput().addAction(new UserAction("Mover Cima") {
            @Override
            protected void onActionBegin() {
                System.out.println("[DEBUG] W pressionado");
            }

            @Override
            protected void onAction() {
                player.getComponent(PlayerComponent.class).moveUp(tpf());
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Mover Baixo") {
            @Override
            protected void onActionBegin() {
                System.out.println("[DEBUG] S pressionado");
            }

            @Override
            protected void onAction() {
                player.getComponent(PlayerComponent.class).moveDown(tpf());
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Mover Esquerda") {
            @Override
            protected void onActionBegin() {
                System.out.println("[DEBUG] A pressionado");
            }

            @Override
            protected void onAction() {
                player.getComponent(PlayerComponent.class).moveLeft(tpf());
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Mover Direita") {
            @Override
            protected void onActionBegin() {
                System.out.println("[DEBUG] D pressionado");
            }

            @Override
            protected void onAction() {
                player.getComponent(PlayerComponent.class).moveRight(tpf());
            }
        }, KeyCode.D);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
