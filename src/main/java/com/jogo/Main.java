package com.jogo;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import com.jogo.componentes.FlyingEnemyComponent;
import com.jogo.componentes.PlayerComponent;
import com.jogo.componentes.RangedEnemyComponent;
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

        // TESTE: um voador em cima-direita e um ranged embaixo-esquerda,
        // os dois com o player como alvo, pra ver os dois tipos de
        // EnemyComponent perseguindo/atacando ao mesmo tempo.
        // (usam enemy.png, o quadrado vermelho que já tínhamos)

        Entity flyingEnemy = entityBuilder()
                .at(600, 150)
                .viewWithBBox("enemy.png")
                .with(new FlyingEnemyComponent("Morcego", 30, 80, 5, 40, 250))
                .buildAndAttach();
        flyingEnemy.getComponent(FlyingEnemyComponent.class).setTarget(player);

        Entity rangedEnemy = entityBuilder()
                .at(150, 450)
                .viewWithBBox("enemy.png")
                .with(new RangedEnemyComponent("Atirador", 20, 60, 10, 40, 300))
                .buildAndAttach();
        rangedEnemy.getComponent(RangedEnemyComponent.class).setTarget(player);
    }

    @Override
    protected void initInput() {
        getInput().addAction(new UserAction("Mover Cima") {
            @Override
            protected void onAction() {
                player.getComponent(PlayerComponent.class).moveUp(tpf());
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Mover Baixo") {
            @Override
            protected void onAction() {
                player.getComponent(PlayerComponent.class).moveDown(tpf());
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Mover Esquerda") {
            @Override
            protected void onAction() {
                player.getComponent(PlayerComponent.class).moveLeft(tpf());
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Mover Direita") {
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
