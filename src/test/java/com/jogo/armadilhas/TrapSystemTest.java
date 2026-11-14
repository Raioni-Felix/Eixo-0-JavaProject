package com.jogo.armadilhas;

import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.jogo.componentes.ComponenteArmadilha;
import com.jogo.componentes.visual.FallingBlockVisual;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do Sistema Modular de Armadilhas (Trap System).
 *
 * Valida os pilares de POO (Polimorfismo, Encapsulamento, Herança, SOLID),
 * o ciclo de vida com Telegraphing (Game Juice), hitboxes/trigger boxes e aplicação de dano.
 */
public class TrapSystemTest {

    // Alvo de teste desacoplado para simular o jogador em ambiente de testes unitários
    public static class TestDummyTarget implements DamageTarget {
        private double x;
        private double y;
        private final Hitbox hitbox;
        private int currentHealth = 100;
        private double lastKnockbackX = 0;
        private double lastKnockbackY = 0;
        private int hitsReceived = 0;
        private double invulnerabilityTimer = 0.0;

        public TestDummyTarget(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.hitbox = new Hitbox(0, 0, width, height);
        }

        public void update(double deltaTime) {
            if (deltaTime > 0 && invulnerabilityTimer > 0) {
                invulnerabilityTimer = Math.max(0.0, invulnerabilityTimer - deltaTime);
            }
        }

        @Override
        public boolean isInvulnerable() {
            return invulnerabilityTimer > 0;
        }

        @Override
        public void activateInvulnerability(double durationSeconds) {
            this.invulnerabilityTimer = Math.max(this.invulnerabilityTimer, durationSeconds);
        }

        @Override
        public void takeDamage(int damage, DamageSource source) {
            if (isInvulnerable()) {
                return;
            }
            this.currentHealth = Math.max(0, currentHealth - damage);
            this.hitsReceived++;
        }

        @Override
        public void applyKnockback(double forceX, double forceY) {
            this.lastKnockbackX = forceX;
            this.lastKnockbackY = forceY;
        }

        @Override public double getX() { return x; }
        @Override public double getY() { return y; }
        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
        @Override public Hitbox getHitbox() { return hitbox; }
        @Override public boolean isAlive() { return currentHealth > 0; }
        public int getCurrentHealth() { return currentHealth; }
        public int getHitsReceived() { return hitsReceived; }
        public double getLastKnockbackX() { return lastKnockbackX; }
        public double getLastKnockbackY() { return lastKnockbackY; }
    }

    private TestDummyTarget player;

    @BeforeEach
    void setUp() {
        player = new TestDummyTarget(100, 100, 32, 48);
    }

    @Test
    @DisplayName("Hitbox calcula corretamente offsets e previne mortes injustas nas bordas")
    void testHitboxOffsetsAndIntersection() {
        // Objeto na posição (100, 100) com sprite de 50x50, mas hitbox com offset de 5px (40x40 real)
        Hitbox hitbox = new Hitbox(5, 5, 40, 40);

        assertEquals(105.0, hitbox.getMinX(100));
        assertEquals(145.0, hitbox.getMaxX(100));
        assertEquals(105.0, hitbox.getMinY(100));
        assertEquals(145.0, hitbox.getMaxY(100));

        // Alvo raspando no sprite visual (x=102), mas fora da hitbox real (minX=105)
        Hitbox targetHitbox = new Hitbox(0, 0, 2, 40);
        boolean touchesVisual = (100 + 50) > 102; // Visualmente sobreporia
        boolean touchesRealHitbox = hitbox.intersects(100, 100, targetHitbox, 102, 100);

        assertTrue(touchesVisual);
        assertFalse(touchesRealHitbox, "Hitbox com offset deve perdoar toque de raspão na borda visual!");
    }

    @Test
    @DisplayName("Espinhos de chão ativam por proximidade, esperam 2.0s de telegraphing e rearmam")
    void testSpikesRetrateisLifecycle() {
        // Espinhos com ativação por proximidade (sem ciclo automático)
        SpikesRetrateis spikes = new SpikesRetrateis(100, 100, 64, 32);

        assertEquals(TrapState.INACTIVE, spikes.getState());
        assertEquals(0.0, spikes.getSpikeExtensionRatio());

        // 1. O tempo passa sem o jogador em cima -> Permanece INACTIVE (sem ciclo automático!)
        spikes.update(5.0);
        assertEquals(TrapState.INACTIVE, spikes.getState(), "Não deve disparar sozinho por ciclo!");

        // 2. Jogador pisa na superfície dos espinhos -> Aciona gatilho de proximidade
        player.setX(110);
        player.setY(95); // Em cima da superfície dos espinhos
        assertTrue(spikes.checkTrigger(player), "Jogador em cima da superfície deve ativar a triggerBox!");
        assertEquals(TrapState.TELEGRAPHING, spikes.getState());
        assertEquals(0.25, spikes.getSpikeExtensionRatio(), 0.01);

        // 3. Durante o aviso (Game Juice): treme e dura pelo menos 2.0 segundos
        spikes.update(0.05);
        assertNotEquals(0.0, spikes.getShakeOffsetX() + spikes.getShakeOffsetY(), "Deve tremer no aviso!");
        spikes.update(1.0);
        assertEquals(TrapState.TELEGRAPHING, spikes.getState(), "Deve continuar no aviso até completar os 2.0s!");

        // 4. Conclui os 2.0 segundos de aviso -> Entra em ACTIVE (letal)
        spikes.update(1.0);
        assertEquals(TrapState.ACTIVE, spikes.getState());
        assertEquals(1.0, spikes.getSpikeExtensionRatio(), 0.01);
        assertEquals(0.0, spikes.getShakeOffsetX(), "Tremor deve zerar ao ativar!");

        // 5. Passa o tempo de dano ativo (2.0s) -> Entra em COOLDOWN
        spikes.update(2.05);
        assertEquals(TrapState.COOLDOWN, spikes.getState());

        // 6. Conclui a recarga (1.5s) -> Retorna a INACTIVE (rearmada e pronta)
        spikes.update(1.55);
        assertEquals(TrapState.INACTIVE, spikes.getState());
        assertEquals(0.0, spikes.getSpikeExtensionRatio());
    }

    @Test
    @DisplayName("Espinhos só causam dano no estado ACTIVE e respeitam colisão")
    void testSpikesDamageOnlyWhenActive() {
        SpikesRetrateis spikes = new SpikesRetrateis(100, 100, 64, 32);
        player.setX(110);
        player.setY(100); // Em cima dos espinhos

        // Inativo: não deve machucar
        assertFalse(spikes.checkCollision(player));
        assertEquals(100, player.getCurrentHealth());

        // Força transição para ACTIVE
        spikes.onTrigger();
        spikes.update(spikes.getTelegraphDuration() + 0.05);
        assertEquals(TrapState.ACTIVE, spikes.getState());

        // Ativo: agora deve aplicar dano e knockback
        assertTrue(spikes.checkCollision(player));
        assertEquals(75, player.getCurrentHealth()); // Dano padrão 25
        assertTrue(player.getLastKnockbackY() < 0, "Knockback vertical deve lançar para cima!");
    }

    @Test
    @DisplayName("Bloco que Cai possui gatilho exato, hitbox 100% e dano contínuo em todas as faces no ar e no solo")
    void testFallingBlockTriggerAndCrush() {
        BlocoQueCai block = new BlocoQueCai(200, 50, 64, 64, 300, 0.5, 45);

        // 1. Jogador ao lado do bloco (x=150): NÃO DEVE DISPARAR O GATILHO!
        player.setX(150);
        player.setY(150);
        assertFalse(block.checkTrigger(player), "Jogador ao lado não pode ativar o bloco esmagador!");
        assertEquals(TrapState.INACTIVE, block.getState());

        // 2. Jogador do outro lado (x=270): NÃO DEVE DISPARAR!
        player.setX(270);
        player.setY(150);
        assertFalse(block.checkTrigger(player), "Jogador do outro lado não pode ativar!");
        assertEquals(TrapState.INACTIVE, block.getState());

        // 3. Jogador exatamente embaixo da projeção do bloco (x=210, y=200): DEVE DISPARAR!
        player.setX(210);
        player.setY(200);
        assertTrue(block.checkTrigger(player), "Jogador exatamente abaixo do bloco deve acionar o gatilho!");
        assertEquals(TrapState.TELEGRAPHING, block.getState());

        // Durante o telegraphing (0.3s): não cai ainda, apenas treme
        block.update(0.3);
        assertEquals(50.0, block.getY(), 0.01, "Não deve cair durante o aviso!");
        assertNotEquals(0.0, block.getShakeOffsetX());

        // Fim do telegraphing (0.5s) -> Começa a despencar (ACTIVE)
        block.update(0.25);
        assertEquals(TrapState.ACTIVE, block.getState());

        // Cai com gravidade acelerada
        block.update(0.1);
        assertTrue(block.getY() > 50.0, "Bloco deve acelerar em direção ao solo!");

        // 4. Dano em todas as direções (Hitbox cobre 100% da área/perímetro do bloco)
        assertEquals(0.0, block.getDamageHitbox().getOffsetX());
        assertEquals(0.0, block.getDamageHitbox().getOffsetY());
        assertEquals(64.0, block.getDamageHitbox().getWidth());
        assertEquals(64.0, block.getDamageHitbox().getHeight());

        // Atinge o jogador durante a queda e continua em estado ACTIVE até atingir o solo
        player.setX(block.getX());
        player.setY(block.getY());
        assertTrue(block.checkCollision(player));
        assertEquals(55, player.getCurrentHealth()); // 100 - 45 = 55
        assertEquals(TrapState.ACTIVE, block.getState(), "Continua ativo até completar o ciclo no solo");

        // Bloco atinge o chão (topoDoChao = 414 -> y = 414 - 64 = 350) sem afundar, zerando velocityY e permanecendo ACTIVE
        block.alignWithGroundSurface(414.0);
        block.update(1.0);
        assertTrue(block.hasHitGround(), "Bloco deve ter atingido o chão");
        assertEquals(350.0, block.getY(), 0.01, "Posição Y deve travar exatamente em topoDoChao - alturaDoBloco");
        assertEquals(0.0, block.getVelocityY(), 0.001, "velocityY deve zerar ao atingir o chão");
        assertEquals(TrapState.ACTIVE, block.getState(), "Permanece ativo no solo após o impacto");

        // 5. Mesmo após atingir o chão, contato em qualquer face (esquerda, direita, topo, baixo) causa dano e knockback
        TestDummyTarget dummyLeft = new TestDummyTarget(block.getX() - 32, block.getY() + 8, 32, 48);
        assertTrue(block.checkCollision(dummyLeft), "Face esquerda no solo deve causar dano!");
        assertEquals(55, dummyLeft.getCurrentHealth());
        assertTrue(dummyLeft.getLastKnockbackX() < 0, "Colisão lateral esquerda deve empurrar para a esquerda!");

        TestDummyTarget dummyRight = new TestDummyTarget(block.getX() + block.getWidth(), block.getY() + 8, 32, 48);
        assertTrue(block.checkCollision(dummyRight), "Face direita no solo deve causar dano!");
        assertEquals(55, dummyRight.getCurrentHealth());
        assertTrue(dummyRight.getLastKnockbackX() > 0, "Colisão lateral direita deve empurrar para a direita!");

        TestDummyTarget dummyTop = new TestDummyTarget(block.getX() + 16, block.getY() - 48, 32, 48);
        assertTrue(block.checkCollision(dummyTop), "Face superior no solo deve causar dano!");
        assertEquals(55, dummyTop.getCurrentHealth());
        assertTrue(dummyTop.getLastKnockbackY() < 0, "Colisão superior deve lançar para cima!");
    }

    @Test
    @DisplayName("TrapFactory cria armadilhas com valores consistentes e polimorfismo")
    void testTrapFactory() {
        Trap spikes = TrapFactory.createSpikes(10, 20, 64, 32);
        assertTrue(spikes instanceof SpikesRetrateis);
        assertEquals("Espinhos Retráteis", spikes.getName());
        assertEquals(2.0, spikes.getTelegraphDuration(), 0.01);

        Trap block = TrapFactory.createFallingBlock(50, 60, 64, 64, 250);
        assertTrue(block instanceof BlocoQueCai);

        Trap fromMap = TrapFactory.createFromTileData("spikes", 0, 0, Map.of("damage", 40));
        assertEquals(40, fromMap.getDamage());
        assertEquals(2.0, fromMap.getTelegraphDuration(), 0.01);
    }

    @Test
    @DisplayName("TrapListener recebe notificações de ciclo de vida e Game Juice")
    void testTrapListenerNotifications() {
        SpikesRetrateis spikes = new SpikesRetrateis(0, 0, 64, 32);
        List<String> events = new ArrayList<>();

        spikes.addListener(new TrapListener() {
            @Override
            public void onStateChanged(Trap trap, TrapState oldState, TrapState newState) {
                events.add("STATE:" + newState);
            }

            @Override
            public void onTelegraphStarted(Trap trap, double duration) {
                events.add("TELEGRAPH:" + duration);
            }

            @Override
            public void onTriggered(Trap trap) {
                events.add("TRIGGERED");
            }
        });

        spikes.onTrigger();
        spikes.update(0.01);
        assertTrue(events.contains("STATE:TELEGRAPHING"));
        assertTrue(events.stream().anyMatch(e -> e.startsWith("TELEGRAPH:")));
    }

    @Test
    @DisplayName("AnimatedTexture recorta spritesheet em 4 quadros e ComponenteArmadilha alterna por TrapState")
    void testFallingBlockVisualAnimationSlicingAndStateSwitching() {
        Map<TrapState, AnimationChannel> canais = FallingBlockVisual.getCanais();
        assertNotNull(canais, "Canais de animação não devem ser nulos");
        assertEquals(4, canais.size(), "Devem existir exatamente 4 canais mapeados");
        assertTrue(canais.containsKey(TrapState.INACTIVE), "Deve mapear Frame 0 para INACTIVE");
        assertTrue(canais.containsKey(TrapState.TELEGRAPHING), "Deve mapear Frame 1 para TELEGRAPHING");
        assertTrue(canais.containsKey(TrapState.ACTIVE), "Deve mapear Frame 2 para ACTIVE");
        assertTrue(canais.containsKey(TrapState.COOLDOWN), "Deve mapear Frame 3 para COOLDOWN");

        BlocoQueCai bloco = TrapFactory.createFallingBlock(100, 50, 64, 64, 200);
        AnimatedTexture texture = FallingBlockVisual.criarTextura(64, 64);
        assertNotNull(texture, "AnimatedTexture deve ser criada");

        ComponenteArmadilha comp = new ComponenteArmadilha(bloco);
        comp.configurarAnimacao(texture, canais, 64, 64);

        // 1. Estado inicial: INACTIVE (Frame 0)
        assertEquals(TrapState.INACTIVE, bloco.getState());
        assertEquals(canais.get(TrapState.INACTIVE), texture.getAnimationChannel());

        // 2. Dispara gatilho -> TELEGRAPHING (Frame 1)
        bloco.onTrigger();
        assertEquals(TrapState.TELEGRAPHING, bloco.getState());
        comp.onUpdate(0.016);
        assertEquals(canais.get(TrapState.TELEGRAPHING), texture.getAnimationChannel());

        // 3. Completa aviso de 0.5s -> ACTIVE (Frame 2, caindo)
        bloco.update(0.40);
        assertEquals(TrapState.TELEGRAPHING, bloco.getState());
        bloco.update(0.12);
        assertEquals(TrapState.ACTIVE, bloco.getState());
        comp.onUpdate(0.016);
        assertEquals(canais.get(TrapState.ACTIVE), texture.getAnimationChannel());

        // 4. Impacto no chão/alvo -> COOLDOWN (Frame 3)
        bloco.onImpact();
        assertEquals(TrapState.COOLDOWN, bloco.getState());
        comp.onUpdate(0.016);
        assertEquals(canais.get(TrapState.COOLDOWN), texture.getAnimationChannel());

        // 5. Conclui subida e cooldown -> Retorna a INACTIVE (Frame 0)
        bloco.update(5.0);
        assertEquals(TrapState.INACTIVE, bloco.getState());
        comp.onUpdate(0.016);
        assertEquals(canais.get(TrapState.INACTIVE), texture.getAnimationChannel());
    }

    @Test
    @DisplayName("Gerador Procedural ancora BlocoQueCai no teto superior (y=0), Spikes no chão e aplica Safe Zone ampla")
    void testProceduralTrapGeneratorAnchoringAndFairPlay() {
        List<TrapProceduralGenerator.PlatformBounds> platforms = List.of(
                new TrapProceduralGenerator.PlatformBounds(0, 0, 3000, 40),      // Teto físico superior da fase (y=0..40)
                new TrapProceduralGenerator.PlatformBounds(0, 680, 3000, 40),    // Chão principal (y=680..720)
                new TrapProceduralGenerator.PlatformBounds(300, 560, 180, 20),   // Plataforma perto do spawn (dentro da Safe Zone)
                new TrapProceduralGenerator.PlatformBounds(1100, 520, 200, 20),  // Plataforma intermediária 1
                new TrapProceduralGenerator.PlatformBounds(1700, 500, 220, 20),  // Plataforma intermediária 2
                new TrapProceduralGenerator.PlatformBounds(2300, 540, 200, 20)   // Plataforma intermediária 3
        );

        double playerX = 400.0;
        double playerY = 420.0;

        TrapGenerationConfig config = new TrapGenerationConfig()
                .withMinTraps(4)
                .withMaxTraps(7)
                .withSpawnChance(0.75)
                .withSafeZoneRadius(550.0)
                .withMinDistanceBetweenTraps(300.0)
                .withSeed(42L);

        TrapSpawner spawner = new TrapSpawner(config);
        List<TrapProceduralGenerator.TrapPlacement> placements = spawner.generatePlacements(
                platforms, 3000.0, 720.0, playerX, playerY
        );

        assertTrue(placements.size() >= config.getMinTraps() && placements.size() <= config.getMaxTraps(),
                "Quantidade gerada deve respeitar minTraps e maxTraps configurados!");

        boolean hasSpikes = false;
        boolean hasFallingBlock = false;

        for (int i = 0; i < placements.size(); i++) {
            TrapProceduralGenerator.TrapPlacement p = placements.get(i);
            Trap trap = p.getTrap();
            TrapProceduralGenerator.PlatformBounds anchor = p.getAnchorPlatform();

            // 1. Verifica Ancoragem Física (Snap-to-Surface)
            if (trap instanceof SpikesRetrateis) {
                hasSpikes = true;
                assertTrue(anchor.getY() > config.getMaxCeilingTopY(),
                        "SpikesRetrateis nunca devem nascer no teto superior!");
                assertEquals(anchor.getY() - p.getHeight(), p.getY(), 0.001,
                        "SpikesRetrateis deve estar apoiado exatamente sobre a superfície superior da plataforma!");
            } else if (trap instanceof BlocoQueCai bloco) {
                hasFallingBlock = true;
                assertEquals(0.0, anchor.getY(), 0.001,
                        "BlocoQueCai deve priorizar o teto físico superior (y = 0) como ponto de ancoragem!");
                assertEquals(40.0, p.getY(), 0.001,
                        "BlocoQueCai deve estar encostado exatamente na face inferior do teto superior (y = 40)!");
                // A queda deve percorrer toda a altura vertical livre até o topo do chão principal (y = 680)
                assertEquals(680.0, p.getY() + p.getFallDistance() + p.getHeight(), 0.001,
                        "Distância de queda do BlocoQueCai deve ter toda a altura livre até o topo do chão abaixo!");
                assertEquals(p.getFallDistance(), bloco.getMaxFallDistance(), 0.001);
            }

            // 2. Verifica Zona Segura ampla (Safe Zone horizontal e radial) ao redor do jogador
            double distToPlayer = Math.hypot(p.getCenterX() - playerX, p.getCenterY() - playerY);
            assertTrue(distToPlayer >= config.getSafeZoneRadius(),
                    "Nenhuma armadilha pode nascer dentro do raio da Safe Zone do jogador!");
            assertTrue(p.getX() >= playerX + config.getSafeZoneRadius() || (p.getX() + p.getWidth()) <= playerX - config.getSafeZoneRadius(),
                    "Nenhuma armadilha pode nascer próxima horizontalmente ao ponto inicial de spawn do jogador!");

            // 3. Verifica Distância Mínima entre armadilhas (Fair Play)
            for (int j = i + 1; j < placements.size(); j++) {
                TrapProceduralGenerator.TrapPlacement other = placements.get(j);
                double distBetween = Math.hypot(p.getCenterX() - other.getCenterX(), p.getCenterY() - other.getCenterY());
                assertTrue(distBetween >= config.getMinDistanceBetweenTraps(),
                        "Armadilhas geradas devem respeitar a distância mínima entre si!");
            }
        }

        assertTrue(hasSpikes, "Deve gerar SpikesRetrateis apoiados em superfícies superiores");
        assertTrue(hasFallingBlock, "Deve gerar BlocoQueCai ancorados na face inferior do teto superior");

        // 4. Verifica reprodutibilidade determinística via seed padrão (DEFAULT_SEED = 12345L) e seed customizada
        TrapProceduralGenerator defaultGen1 = new TrapProceduralGenerator();
        TrapProceduralGenerator defaultGen2 = new TrapProceduralGenerator();
        assertEquals(TrapProceduralGenerator.DEFAULT_SEED, defaultGen1.getConfig().getSeed());

        List<TrapProceduralGenerator.TrapPlacement> defaultRun1 = defaultGen1.generatePlacements(
                platforms, 3000.0, 720.0, playerX, playerY
        );
        List<TrapProceduralGenerator.TrapPlacement> defaultRun2 = defaultGen2.generatePlacements(
                platforms, 3000.0, 720.0, playerX, playerY
        );
        assertEquals(defaultRun1.size(), defaultRun2.size(), "Instâncias padrão com DEFAULT_SEED devem gerar o mesmo layout!");
        for (int i = 0; i < defaultRun1.size(); i++) {
            assertEquals(defaultRun1.get(i).getSpawnType(), defaultRun2.get(i).getSpawnType());
            assertEquals(defaultRun1.get(i).getX(), defaultRun2.get(i).getX(), 0.001);
            assertEquals(defaultRun1.get(i).getY(), defaultRun2.get(i).getY(), 0.001);
        }

        List<TrapProceduralGenerator.TrapPlacement> placementsSameSeed = spawner.generatePlacements(
                platforms, 3000.0, 720.0, playerX, playerY, 42L
        );
        assertEquals(placements.size(), placementsSameSeed.size());
        for (int i = 0; i < placements.size(); i++) {
            assertEquals(placements.get(i).getSpawnType(), placementsSameSeed.get(i).getSpawnType());
            assertEquals(placements.get(i).getX(), placementsSameSeed.get(i).getX(), 0.001);
            assertEquals(placements.get(i).getY(), placementsSameSeed.get(i).getY(), 0.001);
        }
    }

    @Test
    @DisplayName("I-Frames de 1.0s protegem contra hits múltiplos e FxglTrapFeedbackListener dispara Camera Shake no impacto e no dano")
    void testInvulnerabilityFramesAndCameraShakeFeedback() {
        BlocoQueCai block = new BlocoQueCai(200, 40, 64, 64, 300, 0.5, 30);
        FxglTrapFeedbackListener feedbackListener = new FxglTrapFeedbackListener();
        block.addListener(feedbackListener);

        // Aciona o bloco e aguarda entrar em queda no ar (ACTIVE)
        block.onTrigger();
        block.update(0.30);
        block.update(0.21);
        assertEquals(TrapState.ACTIVE, block.getState());
        assertFalse(block.hasHitGround(), "Bloco ainda está caindo no ar");

        // 1. Jogador sofre o primeiro hit da armadilha
        player.setX(block.getX());
        player.setY(block.getY());
        assertFalse(player.isInvulnerable());
        assertTrue(block.checkCollision(player), "Primeiro contato deve aplicar dano");
        assertEquals(70, player.getCurrentHealth());
        assertEquals(1, player.getHitsReceived());
        assertTrue(player.isInvulnerable(), "Jogador deve entrar em I-Frames (1.0s) imediatamente após o dano!");
        assertEquals(1, feedbackListener.getDamageShakeCount(), "Deve acionar Camera Shake em onDamageDealt!");

        // 2. Tentativas de hit nos frames seguintes dentro de 1.0s devem ser bloqueadas pelos I-Frames
        player.update(0.5);
        assertTrue(player.isInvulnerable(), "Após 0.5s ainda deve estar invulnerável (1.0s total)");
        assertFalse(block.checkCollision(player), "Não deve sofrer segundo hit durante a janela de 1.0s de I-Frames!");
        assertEquals(70, player.getCurrentHealth());
        assertEquals(1, player.getHitsReceived());
        assertEquals(1, feedbackListener.getDamageShakeCount(), "Não deve repetir Camera Shake durante I-Frames");

        // 3. Bloco atinge o chão -> Dispara onImpact e Camera Shake de impacto
        assertEquals(0, feedbackListener.getImpactShakeCount());
        block.alignWithGroundSurface(340.0);
        block.update(1.0);
        assertTrue(block.hasHitGround());
        assertEquals(1, feedbackListener.getImpactShakeCount(), "Deve acionar Camera Shake quando o BlocoQueCai atingir o chão (onImpact)!");

        // 4. Após expirar 1.0s de I-Frames, o jogador volta a ficar vulnerável
        player.update(0.6);
        assertFalse(player.isInvulnerable(), "Após completar >1.0s, os I-Frames devem encerrar");
        player.setX(block.getX());
        player.setY(block.getY());
        assertTrue(block.checkCollision(player), "Após o término dos I-Frames, um novo contato deve voltar a causar dano");
        assertEquals(40, player.getCurrentHealth());
        assertEquals(2, player.getHitsReceived());
        assertEquals(2, feedbackListener.getDamageShakeCount());
    }

    // Método main executável standalone para validação imediata no terminal
    public static void main(String[] args) {
        System.out.println("=== EXECUTANDO SUITE DE TESTES DO TRAP SYSTEM ===");
        TrapSystemTest test = new TrapSystemTest();

        runTest("testHitboxOffsetsAndIntersection", () -> {
            test.setUp();
            test.testHitboxOffsetsAndIntersection();
        });

        runTest("testSpikesRetrateisLifecycle", () -> {
            test.setUp();
            test.testSpikesRetrateisLifecycle();
        });

        runTest("testSpikesDamageOnlyWhenActive", () -> {
            test.setUp();
            test.testSpikesDamageOnlyWhenActive();
        });

        runTest("testFallingBlockTriggerAndCrush", () -> {
            test.setUp();
            test.testFallingBlockTriggerAndCrush();
        });

        runTest("testTrapFactory", () -> {
            test.setUp();
            test.testTrapFactory();
        });

        runTest("testTrapListenerNotifications", () -> {
            test.setUp();
            test.testTrapListenerNotifications();
        });

        runTest("testFallingBlockVisualAnimationSlicingAndStateSwitching", () -> {
            test.setUp();
            test.testFallingBlockVisualAnimationSlicingAndStateSwitching();
        });

        runTest("testProceduralTrapGeneratorAnchoringAndFairPlay", () -> {
            test.setUp();
            test.testProceduralTrapGeneratorAnchoringAndFairPlay();
        });

        runTest("testInvulnerabilityFramesAndCameraShakeFeedback", () -> {
            test.setUp();
            test.testInvulnerabilityFramesAndCameraShakeFeedback();
        });

        System.out.println(">>> TODOS OS TESTES PASSARAM COM SUCESSO! 100% OK <<<");
    }

    private static void runTest(String name, Runnable runnable) {
        try {
            runnable.run();
            System.out.println("  [PASSOU] " + name);
        } catch (Throwable t) {
            System.err.println("  [FALHOU] " + name + ": " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
