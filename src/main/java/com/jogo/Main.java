package com.jogo;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.jogo.componentes.FlyingEnemyComponent;
import com.jogo.componentes.MeleeEnemyComponent;
import com.jogo.componentes.PlayerComponent;
import com.jogo.componentes.RangedEnemyComponent;
import com.jogo.factories.FabricaEntidades;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.texture.Texture;

/**
 * Ponto de entrada do jogo.
 *
 * Todo jogo FXGL precisa de uma classe que estenda GameApplication
 * e implemente initSettings(). O método main() só chama launch(),
 * que dispara a inicialização do motor.
 */
public class Main extends GameApplication {

    private Entity player;

    //Barra de vida (HUD): fundo escuro fixo + barra colorida que
    //encolhe conforme o player perde vida.
    private Rectangle hpBarBackground;
    private Rectangle hpBarFill;
    private static final double HP_BAR_WIDTH = 200;
    private static final double HP_BAR_HEIGHT = 20;
    private static final double LEVEL_WIDTH = 3000;
    private static final double LEVEL_HEIGHT = 600;

    //Quão rápido a câmera alcança o player a cada frame, maior gruda
    //mais rápido. Atrasada de propósito (em vez de bindToEntity()),
    //senão o recoil do knockback fica invisível.
    private static final double CAMERA_SMOOTHING = 6.0;

    //Tela de Game Over: painel escondido por padrão, aparece quando o
    //player morre (ver onUpdate()). gameOverShown evita mostrar de
    //novo a cada frame enquanto o player continuar morto.
    private StackPane gameOverOverlay;
    private boolean gameOverShown = false;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("Eixo 0");
        settings.setVersion("0.1");

        //Desliga menu principal e intro por enquanto, pra prototipar e
        //testar rápido é melhor cair direto no jogo. Reativa quando já
        //tiver tela de menu/assets pra ele.
        settings.setMainMenuEnabled(false);
        settings.setIntroEnabled(false);
    }

    //Gravidade do mundo de física (Box2D), só afeta quem tem
    //PhysicsComponent (voador ignora, gravityScale 0).
    //Dano/empurrão por toque não é mais CollisionHandler aqui, ver
    //FabricaEntidades (CATEGORY_PLAYER / setupContatoComPlayer).
    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 1200);
    }

    @Override
    protected void initGame() {
        //Registra a fábrica que sabe montar cada tipo de entidade
        //(padrão trazido da versão do NetBeans: EntityFactory +
        //@Spawns). A partir daqui entidades nascem via spawn("nome",
        //dados) em vez de entityBuilder() solto aqui no Main.
        getGameWorld().addEntityFactory(new FabricaEntidades());

        //Background repetido lado a lado (tiling) cobrindo o nível
        //inteiro. A imagem original é 320x240, então estica só a
        //altura pra 600 (LEVEL_HEIGHT) e repete na horizontal até
        //cobrir os 3000 de largura (LEVEL_WIDTH).
        int tileWidth = 320;
        int tiles = (int) Math.ceil(LEVEL_WIDTH / tileWidth);

        for (int i = 0; i < tiles; i++) {
            Texture bg = new Texture(image("background.png"));
            bg.setFitHeight(LEVEL_HEIGHT);

            entityBuilder()
                    .at(i * tileWidth, 0)
                    .view(bg)
                    .zIndex(-1) // fundo atrás de tudo
                    .buildAndAttach();
        }

        //O mapa: chão cobrindo o nível inteiro + plataformas
        //espalhadas. Plataforma é um corpo estático, não se move, ver
        //FabricaEntidades.spawnPlataforma.
        spawn("plataforma", new SpawnData(0, 560)
                .put("width", LEVEL_WIDTH)
                .put("height", 40.0));

        spawn("plataforma", new SpawnData(300, 420).put("width", 150.0).put("height", 20.0));
        spawn("plataforma", new SpawnData(900, 400).put("width", 150.0).put("height", 20.0));
        spawn("plataforma", new SpawnData(1500, 450).put("width", 200.0).put("height", 20.0));
        spawn("plataforma", new SpawnData(2200, 380).put("width", 150.0).put("height", 20.0));

        //Paredes invisíveis nas bordas do nível (ver
        //FabricaEntidades.spawnParede). Sem elas o player andava pra
        //fora da área do chão e caía no vazio atrás do cenário.
        spawn("parede", new SpawnData(-50, 0).put("width", 50.0).put("height", LEVEL_HEIGHT));
        spawn("parede", new SpawnData(LEVEL_WIDTH, 0).put("width", 50.0).put("height", LEVEL_HEIGHT));

        //Player com textura provisória (quadrado azul) só pra já dar
        //pra ver na tela e testar movimento e XP. Troca por sprite de
        //verdade depois, em src/main/resources/assets/textures/.
        //Spawna acima do chão, cai e já pousa em cima dele sozinho por
        //causa da gravidade/física de verdade.
        player = spawn("jogador", new SpawnData(400, 300)
                .put("name", "Herói")
                .put("maxHealth", 100)
                .put("moveSpeed", 200.0));

        //Câmera: centraliza no player já de cara (sem suavização
        //inicial), o acompanhamento suave de verdade é o
        //updateCamera() chamado por onUpdate(). setBounds() sozinho
        //não trava a câmera aqui (só vale pra bindToEntity()), quem
        //trava de verdade é o clampCameraX/Y lá embaixo.
        getGameScene().getViewport().setBounds(0, 0, (int) LEVEL_WIDTH, (int) LEVEL_HEIGHT);
        var viewport = getGameScene().getViewport();
        viewport.setX(clampCameraX(player.getX() + player.getWidth() / 2 - getAppWidth() / 2.0));
        viewport.setY(clampCameraY(player.getY() + player.getHeight() / 2 - getAppHeight() / 2.0));

        //Inimigos espalhados pelo nível, cada um com física de
        //verdade agora (ver FabricaEntidades): o voador colide com
        //plataforma em vez de atravessar, o ranged fica em cima da
        //plataforma de x=900 em vez de flutuando no vazio.
        Entity flyingEnemy = spawn("inimigo_voador", new SpawnData(1100, 170)
                .put("name", "Morcego")
                .put("maxHealth", 30)
                .put("moveSpeed", 80.0)
                .put("damage", 5)
                .put("attackRange", 40.0)
                .put("detectionRange", 250.0));
        flyingEnemy.getComponent(FlyingEnemyComponent.class).setTarget(player);
        Entity rangedEnemy = spawn("inimigo_ranged", new SpawnData(950, 360)
                .put("name", "Atirador")
                .put("maxHealth", 20)
                .put("moveSpeed", 60.0)
                .put("damage", 10)
                .put("attackRange", 40.0)
                .put("detectionRange", 300.0));
        rangedEnemy.getComponent(RangedEnemyComponent.class).setTarget(player);

        Entity meleeEnemy = spawn("inimigo_melee", new SpawnData(800, 450)
                .put("name", "Espadachim")
                .put("maxHealth", 50)
                .put("moveSpeed", 70.0)
                .put("damage", 15)
                .put("attackRange", 30.0)
                .put("detectionRange", 200.0));
        meleeEnemy.getComponent(MeleeEnemyComponent.class).setTarget(player);

        //Reseta o estado da tela de Game Over, importante pro
        //"Reiniciar" funcionar: initGame() roda de novo nesse botão
        //(via getGameController().startNewGame()), então sem isso o
        //gameOverShown continuaria true e a tela nunca mais apareceria
        //numa segunda morte.
        gameOverShown = false;
    }

    @Override
    protected void initInput() {
        //Side-view: vertical é só gravidade + pulo. onActionBegin
        //dispara uma vez só (não todo frame), certo pra jump().
        getInput().addAction(new UserAction("Pular") {
            @Override
            protected void onActionBegin() {
                if (!player.isActive()) {
                    return;
                }
                player.getComponent(PlayerComponent.class).jump();
            }
        }, KeyCode.SPACE);

        //onAction roda todo frame segurado, onActionEnd zera a
        //velocidade ao soltar (senão deslizaria pra sempre).
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
        //Fundo da barra: cinza escuro, tamanho fixo, sempre no canto
        //superior esquerdo da tela (coordenadas de UI, não do mundo,
        //não se mexe quando o player anda).
        hpBarBackground = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.DARKSLATEGRAY);
        hpBarBackground.setTranslateX(20);
        hpBarBackground.setTranslateY(20);

        //Barra de vida em si: verde, começa cheia (mesma largura do
        //fundo) e vai encolhendo conforme currentHealth cai.
        hpBarFill = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.LIMEGREEN);
        hpBarFill.setTranslateX(20);
        hpBarFill.setTranslateY(20);

        getGameScene().addUINode(hpBarBackground);
        getGameScene().addUINode(hpBarFill);

        //Tela de Game Over: criada uma vez só (initUI roda só no
        //início do app, não em cada novo jogo), começa escondida.
        gameOverOverlay = buildGameOverOverlay();
        getGameScene().addUINode(gameOverOverlay);
    }

    //Painel de Game Over: fundo escuro semi transparente cobrindo a
    //tela toda + título + botão de reiniciar + botão de sair.
    //setMouseTransparent(true) enquanto escondido é importante, senão
    //mesmo invisível ele ficaria por cima bloqueando clique no resto
    //do jogo.
    private StackPane buildGameOverOverlay() {
        //Fundo bem escuro (quase preto) em vez de cinza preto puro,
        //dá uma sensação mais de vinheta que um cinza chapado.
        Rectangle background = new Rectangle(getAppWidth(), getAppHeight(), Color.rgb(8, 6, 10, 0.88));

        Text title = new Text("GAME OVER");
        title.setFill(Color.web("#e63946"));
        title.setFont(Font.font("Arial", FontWeight.BLACK, 64));
        DropShadow titleShadow = new DropShadow(24, Color.rgb(0, 0, 0, 0.9));
        titleShadow.setOffsetY(3);
        title.setEffect(titleShadow);

        Text subtitle = new Text("Você foi derrotado");
        subtitle.setFill(Color.web("#cfcfcf"));
        subtitle.setFont(Font.font("Arial", 18));

        Button restartButton = buildMenuButton("Reiniciar", "#2e7d32", "#3fa043");
        restartButton.setOnAction(e -> restartLevel());

        Button quitButton = buildMenuButton("Sair", "#7a1f1f", "#9c2b2b");
        quitButton.setOnAction(e -> getGameController().exit());

        VBox buttons = new VBox(14, restartButton, quitButton);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(30, title, subtitle, buttons);
        box.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(background, box);
        overlay.setPrefSize(getAppWidth(), getAppHeight());
        overlay.setVisible(false);
        overlay.setMouseTransparent(true);
        return overlay;
    }

    //Botão de menu com visual consistente (cantos arredondados, texto
    //branco em negrito) e um hover simples (troca de cor ao passar o
    //mouse), sem precisar de um arquivo .css separado, só inline via
    //setStyle().
    private Button buildMenuButton(String label, String baseColorHex, String hoverColorHex) {
        Button button = new Button(label);
        button.setPrefWidth(200);
        button.setPrefHeight(46);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        button.setTextFill(Color.WHITE);

        String baseStyle = "-fx-background-color: " + baseColorHex + "; -fx-background-radius: 8; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + hoverColorHex + "; -fx-background-radius: 8; -fx-cursor: hand;";

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));

        return button;
    }

    //Botão "Reiniciar": esconde a tela de Game Over, despausa o motor
    //(fica pausado desde a morte, ver onUpdate()) e manda o FXGL
    //recomeçar o jogo. startNewGame() limpa o mundo e chama initGame()
    //de novo, então tudo (player, inimigos, plataformas) nasce do
    //zero.
    private void restartLevel() {
        gameOverOverlay.setVisible(false);
        gameOverOverlay.setMouseTransparent(true);
        getGameController().resumeEngine();
        getGameController().startNewGame();
    }

    //Câmera suave: anda uma fração do caminho até o alvo por frame,
    //em vez de teleportar (bindToEntity()).
    private void updateCamera(double tpf) {
        if (!player.isActive()) {
            return;
        }

        var viewport = getGameScene().getViewport();

        double targetX = clampCameraX(player.getX() + player.getWidth() / 2 - getAppWidth() / 2.0);
        double targetY = clampCameraY(player.getY() + player.getHeight() / 2 - getAppHeight() / 2.0);

        double t = Math.min(1.0, CAMERA_SMOOTHING * tpf);
        viewport.setX(viewport.getX() + (targetX - viewport.getX()) * t);
        viewport.setY(viewport.getY() + (targetY - viewport.getY()) * t);
    }

    //Trava a câmera nas bordas do nível, sem isso dava pra ver o
    //vazio além do cenário.
    private double clampCameraX(double x) {
        double max = Math.max(0, LEVEL_WIDTH - getAppWidth());
        return Math.max(0, Math.min(max, x));
    }

    private double clampCameraY(double y) {
        double max = Math.max(0, LEVEL_HEIGHT - getAppHeight());
        return Math.max(0, Math.min(max, y));
    }

    @Override
    protected void onUpdate(double tpf) {
        updateCamera(tpf);

        //Se o player já morreu (removido do mundo) não tem
        //PlayerComponent pra consultar, força a barra a ficar zerada
        //em vez de deixar como estava.
        if (!player.isActive()) {
            hpBarFill.setWidth(0);

            //Só mostra a tela de Game Over (e só pausa o motor) uma
            //vez, sem o gameOverShown isso rodaria de novo a cada
            //frame enquanto o player continuasse inativo.
            if (!gameOverShown) {
                gameOverShown = true;
                gameOverOverlay.setVisible(true);
                gameOverOverlay.setMouseTransparent(false);
                getGameController().pauseEngine();
            }
            return;
        }

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
        double healthPercent = (double) playerComponent.getCurrentHealth() / playerComponent.getMaxHealth();

        hpBarFill.setWidth(HP_BAR_WIDTH * healthPercent);

        //Fica vermelha quando a vida está baixa (abaixo de 30%), pra
        //dar um aviso visual de perigo.
        hpBarFill.setFill(healthPercent <= 0.3 ? Color.CRIMSON : Color.LIMEGREEN);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
