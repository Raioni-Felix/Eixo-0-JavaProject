package com.jogo;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.jogo.componentes.FlyingEnemyComponent;
import com.jogo.componentes.PlayerComponent;
import com.jogo.componentes.RangedEnemyComponent;
import com.jogo.componentes.MeleeEnemyComponent;
import com.jogo.factories.FabricaEntidades;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

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

    // Barra de vida (HUD): fundo escuro fixo + barra colorida que
    // encolhe conforme o player perde vida.
    private Rectangle hpBarBackground;
    private Rectangle hpBarFill;
    private static final double HP_BAR_WIDTH = 200;
    private static final double HP_BAR_HEIGHT = 20;

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

    // Gravidade do MUNDO de física (Box2D), não mais calculada na mão
    // dentro do PlayerComponent. Só afeta quem tem PhysicsComponent —
    // por enquanto, só o player e as plataformas. Valor em
    // pixels/s², igual à gravidade manual que tínhamos antes (1200),
    // pra manter a mesma "sensação" de queda.
    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 1200);
    }

    @Override
    protected void initGame() {
        // Registra a fábrica que sabe montar cada tipo de entidade
        // (padrão trazido da versão do NetBeans: EntityFactory +
        // @Spawns). A partir daqui, entidades nascem via spawn("nome",
        // dados) em vez de entityBuilder() solto aqui no Main.
        getGameWorld().addEntityFactory(new FabricaEntidades());

        // O MAPA: por enquanto só um chão comprido cobrindo a largura
        // da tela e uma plataforma flutuando no meio, só pra dar pra
        // testar o pulo subindo em algo. Plataforma é um corpo
        // ESTÁTICO (não se move) — ver FabricaEntidades.spawnPlataforma.
        spawn("plataforma", new SpawnData(0, 560)
                .put("width", 800.0)
                .put("height", 40.0));

        spawn("plataforma", new SpawnData(300, 420)
                .put("width", 150.0)
                .put("height", 20.0));

        // Player com textura provisória (quadrado azul) só pra já dar
        // pra ver na tela e testar movimento e XP. Troca por sprite
        // de verdade depois, em src/main/resources/assets/textures/.
        // Spawna acima do chão — cai e já pousa em cima dele sozinho,
        // por causa da gravidade/física de verdade.
        player = spawn("jogador", new SpawnData(400, 300)
                .put("name", "Herói")
                .put("maxHealth", 100)
                .put("moveSpeed", 200.0));

        // TESTE: um voador em cima-direita e um ranged embaixo-esquerda,
        // os dois com o player como alvo, pra ver os dois tipos de
        // EnemyComponent perseguindo/atacando ao mesmo tempo.
        // (usam enemy.png, o quadrado vermelho que já tínhamos)
        // OBS: nenhum dos dois tem física ainda — continuam se movendo
        // na mão (ver comentário em FabricaEntidades), então não caem
        // nem colidem com o chão/plataformas.

        Entity flyingEnemy = spawn("inimigo_voador", new SpawnData(600, 150)
                .put("name", "Morcego")
                .put("maxHealth", 30)
                .put("moveSpeed", 80.0)
                .put("damage", 5)
                .put("attackRange", 40.0)
                .put("detectionRange", 250.0));
        flyingEnemy.getComponent(FlyingEnemyComponent.class).setTarget(player);

        Entity rangedEnemy = spawn("inimigo_ranged", new SpawnData(150, 450)
                .put("name", "Atirador")
                .put("maxHealth", 20)
                .put("moveSpeed", 60.0)
                .put("damage", 10)
                .put("attackRange", 40.0)
                .put("detectionRange", 300.0));
        rangedEnemy.getComponent(RangedEnemyComponent.class).setTarget(player);

        Entity meleeEnemy = spawn("inimigo_melee", new SpawnData(400, 450)
                .put("name", "Espadachim")
                .put("maxHealth", 50)
                .put("moveSpeed", 70.0)
                .put("damage", 15)
                .put("attackRange", 30.0)
                .put("detectionRange", 200.0));
        meleeEnemy.getComponent(MeleeEnemyComponent.class).setTarget(player);
    }

    @Override
    protected void initInput() {
        // Side-view: não existe mais "mover cima/baixo" livre — a
        // vertical é gravidade + pulo. onActionBegin() dispara só uma
        // vez quando a tecla é apertada (não a cada frame como
        // onAction()), que é o certo pra jump() — senão ele tentaria
        // pular todo frame enquanto a tecla ficasse segurada.
        getInput().addAction(new UserAction("Pular") {
            @Override
            protected void onActionBegin() {
                if (!player.isActive()) {
                    return;
                }
                player.getComponent(PlayerComponent.class).jump();
            }
        }, KeyCode.SPACE);

        // Mover Esquerda/Direita: onAction roda todo frame enquanto a
        // tecla estiver segurada (mantém a velocidade constante);
        // onActionEnd roda uma vez quando solta, zerando a velocidade
        // horizontal (sem isso o player deslizaria pra sempre depois
        // de soltar a tecla, já que física de verdade não para sozinha
        // como o translate manual de antes).
        getInput().addAction(new UserAction("Mover Esquerda") {
            @Override
            protected void onAction() {
                if (!player.isActive()) {
                    return;
                }
                player.getComponent(PlayerComponent.class).moveLeft();
            }

            @Override
            protected void onActionEnd() {
                if (!player.isActive()) {
                    return;
                }
                player.getComponent(PlayerComponent.class).stopHorizontal();
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Mover Direita") {
            @Override
            protected void onAction() {
                if (!player.isActive()) {
                    return;
                }
                player.getComponent(PlayerComponent.class).moveRight();
            }

            @Override
            protected void onActionEnd() {
                if (!player.isActive()) {
                    return;
                }
                player.getComponent(PlayerComponent.class).stopHorizontal();
            }
        }, KeyCode.D);
    }


    @Override
    protected void initUI() {
        // Fundo da barra: cinza escuro, tamanho fixo, sempre no canto
        // superior esquerdo da tela (coordenadas de UI, não do mundo —
        // não se mexe quando o player anda).
        hpBarBackground = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.DARKSLATEGRAY);
        hpBarBackground.setTranslateX(20);
        hpBarBackground.setTranslateY(20);

        // Barra de vida em si: verde, começa cheia (mesma largura do
        // fundo) e vai encolhendo conforme currentHealth cai.
        hpBarFill = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.LIMEGREEN);
        hpBarFill.setTranslateX(20);
        hpBarFill.setTranslateY(20);

        getGameScene().addUINode(hpBarBackground);
        getGameScene().addUINode(hpBarFill);
    }

    @Override
    protected void onUpdate(double tpf) {
        // Se o player já morreu (removido do mundo), não tem
        // PlayerComponent pra consultar — força a barra a ficar
        // zerada (em vez de deixar como estava, que podia travar num
        // valor > 0 dependendo do frame em que a morte aconteceu).
        if (!player.isActive()) {
            hpBarFill.setWidth(0);
            return;
        }

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
        double healthPercent = (double) playerComponent.getCurrentHealth() / playerComponent.getMaxHealth();

        hpBarFill.setWidth(HP_BAR_WIDTH * healthPercent);

        // Fica vermelha quando a vida está baixa (abaixo de 30%), pra
        // dar um aviso visual de perigo.
        hpBarFill.setFill(healthPercent <= 0.3 ? Color.CRIMSON : Color.LIMEGREEN);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
