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
import com.jogo.componentes.visual.BackgroundAnimationComponent;
import com.almasb.fxgl.texture.Texture;
import com.jogo.factories.FabricaEntidades;
import javafx.animation.Interpolator;
//import javafx.animation.KeyFrame;
//import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
//import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

    //Barra de vida (HUD): fundo escuro fixo + barra colorida que
    //encolhe conforme o player perde vida.
    private Rectangle hpBarBackground;
    private Rectangle hpBarFill;
    private static final double HP_BAR_WIDTH = 200;
    private static final double HP_BAR_HEIGHT = 20;
    private static final double LEVEL_WIDTH = 3000;
    private static final double LEVEL_HEIGHT = 720;

    //Quão rápido a câmera alcança o player a cada frame, maior gruda
    //mais rápido. Atrasada de propósito (em vez de bindToEntity()),
    //senão o recoil do knockback fica invisível.
    private static final double CAMERA_SMOOTHING = 6.0;

    //Tela de Game Over: painel escondido por padrão, aparece quando o
    //player morre (ver onUpdate()). gameOverShown evita mostrar de
    //novo a cada frame enquanto o player continuar morto.
    private StackPane gameOverOverlay;
    private boolean gameOverShown = false;

    //Menu principal: aparece visível desde o início (ver initUI()),
    //com o motor já pausado, e só libera o jogo quando aperta
    //"Jogar". Só aparece uma vez no começo do app, não reaparece ao
    //reiniciar depois de um Game Over (initUI() roda só uma vez).
    private StackPane mainMenuOverlay;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Eixo 0");
        settings.setVersion("0.1");

        //Deixa o fullscreen disponível (atalho padrão do FXGL é F11).
        //setFullScreenFromStart em false só pra já abrir em janela
        //normal, quem quiser tela cheia aperta o atalho.
        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(false);

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

    //Carrega um frame do background já pedindo pro JavaFX decodificar
    //em 1000x180 em vez do 4000x720 que o arquivo tem de verdade. O
    //arquivo é um upscale 4x nearest-neighbor do desenho original, e
    //smooth=false aqui desfaz esse upscale de volta pro tamanho real
    //sem borrar nada, só que gastando 16x menos memória por frame.
    private Image loadCryoLabFrame(int index) {
        String path = "/assets/textures/cryolab/frame_" + String.format("%03d", index) + ".png";

        try (InputStream stream = Main.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Frame não encontrado: " + path);
            }
            return new Image(stream, 1000, 180, true, false);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar frame do background: " + path, e);
        }
    }

    //Carrega a arte de fundo do menu principal (corredor de cryo-lab,
    //320x180... na real 320x240). É uma imagem só, pequena, então
    //carrega no tamanho nativo mesmo (sem downscale, diferente do
    //loadCryoLabFrame) e a exibição (ImageView) que estica pro
    //tamanho da tela, sem suavizar, pra manter o pixel art nítido.
    private Image loadMenuBackgroundImage() {
        String path = "/assets/textures/menu/scifi-lab.png";

        try (InputStream stream = Main.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Imagem não encontrada: " + path);
            }
            return new Image(stream);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar fundo do menu: " + path, e);
        }
    }

    @Override
    protected void initGame() {
        //Registra a fábrica que sabe montar cada tipo de entidade
        //(padrão trazido da versão do NetBeans: EntityFactory +
        //@Spawns). A partir daqui entidades nascem via spawn("nome",
        //dados) em vez de entityBuilder() solto aqui no Main.
        getGameWorld().addEntityFactory(new FabricaEntidades());

        //Background animado (cryo lab), 60 frames em loop a 15fps.
        //Cada frame já vem em 4000x720 (upscale 4x de 1000x180), mas
        //carregar isso tudo em memória é muito pesado (uns 700MB só
        //de background, e trocar imagem gigante 15-30x por segundo
        //trava o jogo). loadCryoLabFrame() decodifica direto em
        //1000x180 (a resolução real do desenho) e a Texture reestica
        //isso pra 4000x720 na exibição, então fica leve sem perder
        //nitidez. Fica parado no nível e repete lado a lado (tiling)
        //se o LEVEL_WIDTH passar de 4000 (hoje com 3000 só precisa de
        //uma cópia). As imagens só são carregadas uma vez e
        //reaproveitadas entre as cópias, pra não multiplicar memória.
        List<Image> cryoLabFrames = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cryoLabFrames.add(loadCryoLabFrame(i));
        }

        int backgroundTileWidth = 4000;
        int backgroundTiles = (int) Math.ceil(LEVEL_WIDTH / backgroundTileWidth);

        for (int i = 0; i < backgroundTiles; i++) {
            BackgroundAnimationComponent cryoLabBackground = new BackgroundAnimationComponent(cryoLabFrames, 15);

            Texture bgView = cryoLabBackground.getView();
            bgView.setFitWidth(backgroundTileWidth);
            bgView.setFitHeight(LEVEL_HEIGHT);
            bgView.setSmooth(false); //nearest neighbor, pixel art sem borrão

            entityBuilder()
                    .at(i * backgroundTileWidth, 0)
                    .view(bgView)
                    .zIndex(-1) // fundo atrás de tudo
                    .with(cryoLabBackground)
                    .buildAndAttach();
        }

        //O mapa: chão cobrindo o nível inteiro + plataformas
        //espalhadas. Plataforma é um corpo estático, não se move, ver
        //FabricaEntidades.spawnPlataforma.
        //Chão reposicionado pro fundo do nível (LEVEL_HEIGHT - 40), e as
        //plataformas soltas deslocadas +120 em Y junto com ele (mesma
        //distância que o LEVEL_HEIGHT cresceu de 600 pra 720), pra
        //manter a mesma cara do level de antes, só mais alto.
        spawn("plataforma", new SpawnData(0, LEVEL_HEIGHT - 40)
                .put("width", LEVEL_WIDTH)
                .put("height", 40.0));

        spawn("plataforma", new SpawnData(300, 540).put("width", 150.0).put("height", 20.0));
        spawn("plataforma", new SpawnData(900, 520).put("width", 150.0).put("height", 20.0));
        spawn("plataforma", new SpawnData(1500, 570).put("width", 200.0).put("height", 20.0));
        spawn("plataforma", new SpawnData(2200, 500).put("width", 150.0).put("height", 20.0));

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
        player = spawn("jogador", new SpawnData(400, 420)
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
        Entity flyingEnemy = spawn("inimigo_voador", new SpawnData(1100, 290)
                .put("name", "Morcego")
                .put("maxHealth", 30)
                .put("moveSpeed", 80.0)
                .put("damage", 5)
                .put("attackRange", 40.0)
                .put("detectionRange", 250.0));
        flyingEnemy.getComponent(FlyingEnemyComponent.class).setTarget(player);
        Entity rangedEnemy = spawn("inimigo_ranged", new SpawnData(950, 480)
                .put("name", "Atirador")
                .put("maxHealth", 20)
                .put("moveSpeed", 60.0)
                .put("damage", 10)
                .put("attackRange", 40.0)
                .put("detectionRange", 300.0));
        rangedEnemy.getComponent(RangedEnemyComponent.class).setTarget(player);

        Entity meleeEnemy = spawn("inimigo_melee", new SpawnData(800, 570)
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
        hpBarBackground = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.DARKSLATEGRAY);
        hpBarBackground.setTranslateX(20);
        hpBarBackground.setTranslateY(20);

        hpBarFill = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.LIMEGREEN);
        hpBarFill.setTranslateX(20);
        hpBarFill.setTranslateY(20);

        // 1. Resgatamos o componente do jogador (que já foi instanciado no initGame)
        PlayerComponent pComponent = player.getComponent(PlayerComponent.class);

        // mudança por evento:
        // Amarramos a propriedade 'width' do retângulo à equação da vida do jogador.
        // O JavaFX recalcula a expressão matemática SOMENTE quando currentHealth sofrer um .set().
        hpBarFill.widthProperty().bind(
            pComponent.currentHealthProperty()
                .multiply(HP_BAR_WIDTH)
                .divide(pComponent.getMaxHealth())
        );


        // Adicionamos um "sensor que dispara uma ação toda vez que o valor muda.
        // Isso resolve a troca de cores (verde para vermelho).
        pComponent.currentHealthProperty().addListener((observable, oldValue, newValue) -> {
            double percent = newValue.doubleValue() / pComponent.getMaxHealth();
            hpBarFill.setFill(percent <= 0.3 ? Color.CRIMSON : Color.LIMEGREEN);
        });

        getGameScene().addUINode(hpBarBackground);
        getGameScene().addUINode(hpBarFill);
        // Instancia a tela de Game Over e adiciona à cena
        gameOverOverlay = buildGameOverOverlay();
        getGameScene().addUINode(gameOverOverlay);

        //Menu principal, já visível por cima de tudo, e pausa o
        //motor aqui (initUI roda depois do initGame/initPhysics,
        //então o mundo já existe e pausar não quebra nada). Só
        //despausa no startGame(), quando aperta "Jogar".
        mainMenuOverlay = buildMainMenuOverlay();
        getGameScene().addUINode(mainMenuOverlay);
        getGameController().pauseEngine();
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

    //Painel do menu principal: aparece visível desde o início (o
    //motor já nasce pausado, ver initUI()), some quando aperta
    //"Jogar". Layout baseado no mockup "EIXO Game Main Menu" que o
    //Lincoln mandou (imagem de fundo do corredor de cryo-lab +
    //título com aberração cromática + linhas/scanlines/vinheta +
    //itens de menu estilo HUD). A fonte "Press Start 2P" do mockup
    //não é uma fonte instalada por padrão, então usa Consolas em
    //negrito no lugar (mesma família usada no resto da UI).
    private StackPane buildMainMenuOverlay() {
        double width = getAppWidth();
        double height = getAppHeight();

        //Camada 1: imagem de fundo (corredor de cryo-lab), esticada
        //pra cobrir a tela toda sem suavizar (mantém o pixel art
        //nítido, mesma técnica do background animado do nível).
        ImageView background = new ImageView(loadMenuBackgroundImage());
        background.setFitWidth(width);
        background.setFitHeight(height);
        background.setSmooth(false);

        //Camada 2: vinheta (escurece as bordas, deixa o centro mais
        //visível) via gradiente radial.
        Rectangle vignette = new Rectangle(width, height);
        vignette.setFill(new RadialGradient(
                0, 0, 0.5, 0.45, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.28, Color.rgb(6, 16, 26, 0)),
                new Stop(1.0, Color.rgb(4, 10, 18, 0.86))
        ));
        vignette.setMouseTransparent(true);

        //Camada 3: scanlines (listras horizontais bem sutis), efeito
        //de monitor CRT antigo/holograma.
        Node scanlines = buildScanlineOverlay(width, height);

        //Camada 4: uma faixa de brilho ciano que desce a tela em
        //loop, feito só pra dar uma sensação de "scanner" ativo.
        Rectangle sweep = new Rectangle(width, 160);
        sweep.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(77, 232, 212, 0)),
                new Stop(0.5, Color.rgb(77, 232, 212, 0.055)),
                new Stop(1, Color.rgb(77, 232, 212, 0))
        ));
        sweep.setMouseTransparent(true);
        sweep.setTranslateY(-160);

        TranslateTransition sweepAnim = new TranslateTransition(Duration.seconds(7), sweep);
        sweepAnim.setFromY(-160);
        sweepAnim.setToY(height + 160);
        sweepAnim.setInterpolator(Interpolator.LINEAR);
        sweepAnim.setCycleCount(TranslateTransition.INDEFINITE);
        sweepAnim.play();

        //Linha "CRYOGENIC DIVISION" com tracinhos dos dois lados.
        Rectangle divisionLineLeft = new Rectangle(56, 1, Color.web("#4de8d4"));
        Text divisionLabel = new Text(spaced("CRYOGENIC DIVISION"));
        divisionLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        divisionLabel.setFill(Color.web("#4de8d4"));
        divisionLabel.setOpacity(0.7);
        Rectangle divisionLineRight = new Rectangle(56, 1, Color.web("#4de8d4"));
        HBox divisionRow = new HBox(18, divisionLineLeft, divisionLabel, divisionLineRight);
        divisionRow.setAlignment(Pos.CENTER);

        //Título "EIXO 0" com aberração cromática (cópias azul/magenta
        //levemente deslocadas atrás do texto ciano principal) +
        //brilho neon, igual o mockup.
        Node eixoText = buildAberratedText("EIXO", 64, "#dffdf7", 22);
        Rectangle titleDivider = new Rectangle(34, 6, Color.web("#4de8d4"));
        titleDivider.setEffect(new DropShadow(16, Color.web("#4de8d4")));
        Node zeroText = buildAberratedText("0", 64, "#4de8d4", 26);

        HBox titleRow = new HBox(22, eixoText, titleDivider, zeroText);
        titleRow.setAlignment(Pos.CENTER);

        Text protocolLabel = new Text(spaced("SUBJECT ZERO PROTOCOL"));
        protocolLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        protocolLabel.setFill(Color.web("#6fb3c4"));

        VBox titleContent = new VBox(18, titleRow, protocolLabel);
        titleContent.setAlignment(Pos.CENTER);

        //Painel do título: fundo escuro translúcido, borda superior
        //ciano e inferior azul-petróleo, com 4 "cantos" decorativos
        //(estilo mira/HUD) por cima.
        StackPane titlePanel = new StackPane(titleContent);
        titlePanel.setPadding(new Insets(26, 44, 26, 44));
        titlePanel.setStyle(
                "-fx-background-color: rgba(6,20,30,0.72);"
                + " -fx-border-color: #4de8d4 transparent #1c5f74 transparent;"
                + " -fx-border-width: 2 0 2 0;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 24, 0, 0, 0);"
        );

        AnchorPane titleWrapper = new AnchorPane(titlePanel);
        AnchorPane.setTopAnchor(titlePanel, 0.0);
        AnchorPane.setLeftAnchor(titlePanel, 0.0);
        AnchorPane.setRightAnchor(titlePanel, 0.0);
        AnchorPane.setBottomAnchor(titlePanel, 0.0);

        Node cornerTopLeft = buildCornerBracket("#4de8d4");
        Node cornerTopRight = buildCornerBracket("#4de8d4");
        Node cornerBottomLeft = buildCornerBracket("#1c5f74");
        Node cornerBottomRight = buildCornerBracket("#1c5f74");
        AnchorPane.setTopAnchor(cornerTopLeft, -6.0);
        AnchorPane.setLeftAnchor(cornerTopLeft, -6.0);
        AnchorPane.setTopAnchor(cornerTopRight, -6.0);
        AnchorPane.setRightAnchor(cornerTopRight, -6.0);
        AnchorPane.setBottomAnchor(cornerBottomLeft, -6.0);
        AnchorPane.setLeftAnchor(cornerBottomLeft, -6.0);
        AnchorPane.setBottomAnchor(cornerBottomRight, -6.0);
        AnchorPane.setRightAnchor(cornerBottomRight, -6.0);
        titleWrapper.getChildren().addAll(cornerTopLeft, cornerTopRight, cornerBottomLeft, cornerBottomRight);

        VBox titleBlock = new VBox(14, divisionRow, titleWrapper);
        titleBlock.setAlignment(Pos.CENTER);

        //Itens do menu: linhas estilo HUD (indicador + label + tecla
        //de atalho), em vez de botões redondos comuns.
        Region playRow = buildMenuRow("PLAY", "ENTER", true, this::startGame);
        Region optionsRow = buildMenuRow("OPTIONS", "O", false, null);
        Region quitRow = buildMenuRow("QUIT", "ESC", false, () -> getGameController().exit());
        VBox menuRows = new VBox(10, playRow, optionsRow, quitRow);
        menuRows.setAlignment(Pos.CENTER);

        VBox centerContent = new VBox(76, titleBlock, menuRows);
        centerContent.setAlignment(Pos.CENTER);

        Text footerLeft = new Text(spaced("CRYOBAY 7 · SYS v0.4.1"));
        footerLeft.setFont(Font.font("Consolas", 10));
        footerLeft.setFill(Color.web("#3f7a96"));

        Text footerRight = new Text(spaced("SELECT ENTER CONFIRM"));
        footerRight.setFont(Font.font("Consolas", 10));
        footerRight.setFill(Color.web("#3f7a96"));

        StackPane overlay = new StackPane(background, vignette, scanlines, sweep, centerContent, footerLeft, footerRight);
        overlay.setPrefSize(width, height);
        StackPane.setAlignment(sweep, Pos.TOP_LEFT);
        StackPane.setAlignment(centerContent, Pos.CENTER);
        StackPane.setAlignment(footerLeft, Pos.BOTTOM_LEFT);
        StackPane.setMargin(footerLeft, new Insets(0, 0, 22, 28));
        StackPane.setAlignment(footerRight, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(footerRight, new Insets(0, 28, 22, 0));

        return overlay;
    }

    //Overlay de scanlines: um monte de linhas horizontais finas e bem
    //translúcidas, geradas uma vez só na construção do menu (não é
    //por frame, então não pesa nada).
    private Node buildScanlineOverlay(double width, double height) {
        Pane pane = new Pane();
        pane.setPrefSize(width, height);
        pane.setMouseTransparent(true);

        for (double y = 0; y < height; y += 4) {
            Rectangle line = new Rectangle(width, 2, Color.rgb(0, 0, 0, 0.3));
            line.setLayoutY(y);
            pane.getChildren().add(line);
        }

        return pane;
    }

    //Texto com "aberração cromática": duas cópias levemente
    //deslocadas (azul e magenta) atrás do texto principal, técnica
    //visual do mockup pra dar aquele efeito de tela/holograma com
    //leve desalinhamento de cor.
    private Node buildAberratedText(String text, double size, String mainColorHex, double glowRadius) {
        Text ghostBlue = new Text(text);
        ghostBlue.setFont(Font.font("Consolas", FontWeight.BLACK, size));
        ghostBlue.setFill(Color.web("#1f4e8c"));
        ghostBlue.setOpacity(0.85);
        ghostBlue.setTranslateX(-3);
        ghostBlue.setTranslateY(2);

        Text ghostMagenta = new Text(text);
        ghostMagenta.setFont(Font.font("Consolas", FontWeight.BLACK, size));
        ghostMagenta.setFill(Color.web("#7a2f4a"));
        ghostMagenta.setOpacity(0.7);
        ghostMagenta.setTranslateX(3);
        ghostMagenta.setTranslateY(-2);

        Text front = new Text(text);
        front.setFont(Font.font("Consolas", FontWeight.BLACK, size));
        front.setFill(Color.web(mainColorHex));
        DropShadow glow = new DropShadow(glowRadius, Color.web("#4de8d4"));
        glow.setSpread(0.2);
        front.setEffect(glow);

        StackPane stack = new StackPane(ghostBlue, ghostMagenta, front);
        stack.setAlignment(Pos.CENTER);
        return stack;
    }

    //Cantinho decorativo estilo "mira de HUD": um traço horizontal +
    //um vertical formando um "L", usado nos 4 cantos do painel de
    //título.
    private Node buildCornerBracket(String colorHex) {
        Color color = Color.web(colorHex);
        Rectangle horizontal = new Rectangle(16, 2, color);
        Rectangle vertical = new Rectangle(2, 16, color);
        return new Group(horizontal, vertical);
    }

    //Item de menu estilo HUD: quadradinho indicador + label + tecla
    //de atalho à direita, com destaque (borda/fundo mais forte) pro
    //item primário (PLAY) e hover que clareia o fundo. Passar
    //action=null deixa o item só visual (é o caso de "OPTIONS", que
    //ainda não tem tela própria).
    private Region buildMenuRow(String label, String hotkey, boolean primary, Runnable action) {
        Rectangle indicator = new Rectangle(8, 8, Color.web(primary ? "#4de8d4" : "#1c5f74"));
        if (primary) {
            indicator.setEffect(new DropShadow(10, Color.web("#4de8d4")));
        }

        Text labelText = new Text(spaced(label));
        labelText.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        labelText.setFill(Color.web(primary ? "#dffdf7" : "#8fc3d1"));

        Text hotkeyText = new Text(spaced(hotkey));
        hotkeyText.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        hotkeyText.setFill(Color.web(primary ? "#4de8d4" : "#3f7a96"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, indicator, labelText, spacer, hotkeyText);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(340);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.setCursor(Cursor.HAND);

        String baseStyle = primary
                ? "-fx-background-color: rgba(77,232,212,0.14); -fx-border-color: transparent transparent transparent #4de8d4;"
                        + " -fx-border-width: 0 0 0 4; -fx-effect: dropshadow(gaussian, rgba(77,232,212,0.18), 24, 0, 0, 0);"
                : "-fx-background-color: rgba(10,26,38,0.66); -fx-border-color: transparent transparent transparent #1c5f74;"
                        + " -fx-border-width: 0 0 0 4;";
        String hoverStyle = primary
                ? "-fx-background-color: rgba(77,232,212,0.22); -fx-border-color: transparent transparent transparent #4de8d4;"
                        + " -fx-border-width: 0 0 0 4; -fx-effect: dropshadow(gaussian, rgba(77,232,212,0.25), 28, 0, 0, 0);"
                : "-fx-background-color: rgba(77,232,212,0.14); -fx-border-color: transparent transparent transparent #4de8d4;"
                        + " -fx-border-width: 0 0 0 4;";

        row.setStyle(baseStyle);
        row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
        row.setOnMouseExited(e -> row.setStyle(baseStyle));

        if (action != null) {
            row.setOnMouseClicked(e -> action.run());
        }

        return row;
    }

    //Insere espaço entre cada caractere, pra simular o "letter
    //spacing" bem aberto do mockup (JavaFX não tem uma propriedade
    //nativa de espaçamento de letra em Text).
    private String spaced(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append(text.charAt(i));
            if (i < text.length() - 1) {
                result.append(' ');
            }
        }
        return result.toString();
    }

    //Botão de menu com visual consistente (cantos arredondados, texto
    //branco em negrito) e um hover simples (troca de cor ao passar o
    //mouse), sem precisar de um arquivo .css separado, só inline via
    //setStyle(). Overload de 2 cores (usado pelo Game Over) mantém o
    //comportamento de antes, só usando a própria cor de hover como
    //brilho.
    private Button buildMenuButton(String label, String baseColorHex, String hoverColorHex) {
        return buildMenuButton(label, baseColorHex, hoverColorHex, hoverColorHex);
    }

    //Versão "estilizada" do botão, usada no menu principal: borda fina
    //na cor de destaque (glowColorHex), leve brilho e um crescimento
    //suave (scale) ao passar o mouse, em vez de só trocar a cor.
    private Button buildMenuButton(String label, String baseColorHex, String hoverColorHex, String glowColorHex) {
        Button button = new Button(label);
        button.setPrefWidth(220);
        button.setPrefHeight(50);
        button.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        button.setTextFill(Color.WHITE);

        String baseStyle = "-fx-background-color: " + baseColorHex + "; -fx-background-radius: 6;"
                + " -fx-border-color: " + glowColorHex + "; -fx-border-radius: 6; -fx-border-width: 1; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + hoverColorHex + "; -fx-background-radius: 6;"
                + " -fx-border-color: " + glowColorHex + "; -fx-border-radius: 6; -fx-border-width: 1.5; -fx-cursor: hand;";

        button.setStyle(baseStyle);

        DropShadow glow = new DropShadow(18, Color.web(glowColorHex));

        ScaleTransition growIn = new ScaleTransition(Duration.millis(120), button);
        growIn.setToX(1.06);
        growIn.setToY(1.06);

        ScaleTransition shrinkBack = new ScaleTransition(Duration.millis(120), button);
        shrinkBack.setToX(1.0);
        shrinkBack.setToY(1.0);

        button.setOnMouseEntered(e -> {
            button.setStyle(hoverStyle);
            button.setEffect(glow);
            growIn.playFromStart();
        });
        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setEffect(null);
            shrinkBack.playFromStart();
        });

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

    //Botão "Jogar" do menu principal: some com o menu e despausa o
    //motor, que nasceu pausado em initUI(). Só roda uma vez, no
    //começo do app.
    private void startGame() {
        mainMenuOverlay.setVisible(false);
        mainMenuOverlay.setMouseTransparent(true);
        getGameController().resumeEngine();
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

        if (!player.isActive()) {
            // REMOVIDO: hpBarFill.setWidth(0); 
            // Motivo: hpBarFill está "bound" (vinculada) à vida. Como takeDamage() 
            // zera a vida ao morrer, a barra diminui automaticamente. Tentar 
            // alterar a largura manualmente aqui causaria uma RuntimeException.
            
            if (!gameOverShown) {
                gameOverShown = true;
                gameOverOverlay.setVisible(true);
                gameOverOverlay.setMouseTransparent(false);
                getGameController().pauseEngine();
            }
            return;
        }

        // REMOVIDO: Todo o bloco final que calculava healthPercent e atualizava hpBarFill
        // Motivo: As ações hpBarFill.widthProperty().bind(...) e .addListener(...)
        // no initUI() já fazem isso de forma puramente orientada a eventos.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
