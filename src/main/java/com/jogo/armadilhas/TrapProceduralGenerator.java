package com.jogo.armadilhas;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.jogo.entidades.TipoEntidade;
import com.jogo.niveis.DadosNivel;
import com.jogo.niveis.DadosPlataforma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Gerador Procedural de Armadilhas ({@code TrapProceduralGenerator}).
 *
 * Varre as plataformas/blocos sólidos do nível (via {@link DadosNivel}, {@link FXGL#getGameWorld()}
 * ou lista de {@link PlatformBounds}) e posiciona armadilhas automaticamente respeitando:
 * <ol>
 *   <li><b>Ancoragem Física (Snap-to-Grid/Surface):</b>
 *     <ul>
 *       <li>{@link SpikesRetrateis}: Gerados apenas apoiados exatamente sobre a superfície superior
 *           de plataformas/chãos sólidos ({@code y = plat.y - altura}), com espaço vazio acima.</li>
 *       <li>{@link BlocoQueCai}: Gerados apenas encostados na face inferior de tetos/plataformas altas
 *           ({@code y = plat.y + plat.altura}), com espaço livre vertical abaixo para queda até o solo.</li>
 *     </ul>
 *   </li>
 *   <li><b>Regras de Fair Play:</b>
 *     <ul>
 *       <li><b>Zona Segura (Safe Zone):</b> Área livre de armadilhas ao redor do ponto de spawn do jogador
 *           e das bordas de transição de sala.</li>
 *       <li><b>Distância Mínima:</b> Espaçamento mínimo entre armadilhas para nunca bloquear passagens inteiras.</li>
 *       <li><b>Parâmetros Configuráveis:</b> Quantidade mínima/máxima, densidade ({@code spawnChance}) e {@code seed}
 *           opcional para reprodução determinística de layouts.</li>
 *     </ul>
 *   </li>
 * </ol>
 */
public class TrapProceduralGenerator {

    /**
     * Representa os limites geométricos de uma plataforma ou bloco sólido do cenário.
     */
    public static class PlatformBounds {
        private final double x;
        private final double y;
        private final double width;
        private final double height;

        public PlatformBounds(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public double getMaxX() { return x + width; }
        public double getMaxY() { return y + height; }

        public boolean intersects(double rx, double ry, double rw, double rh) {
            return this.x < (rx + rw)
                    && (this.x + this.width) > rx
                    && this.y < (ry + rh)
                    && (this.y + this.height) > ry;
        }
    }

    /**
     * Descreve uma armadilha posicionada pelo gerador procedural, pronta para ser instanciada no mundo.
     */
    public static class TrapPlacement {
        private final String spawnType;
        private final Trap trap;
        private final double x;
        private final double y;
        private final double width;
        private final double height;
        private final double fallDistance;
        private final PlatformBounds anchorPlatform;

        public TrapPlacement(String spawnType, Trap trap, double x, double y,
                             double width, double height, double fallDistance,
                             PlatformBounds anchorPlatform) {
            this.spawnType = spawnType;
            this.trap = trap;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.fallDistance = fallDistance;
            this.anchorPlatform = anchorPlatform;
        }

        public String getSpawnType() { return spawnType; }
        public Trap getTrap() { return trap; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public double getFallDistance() { return fallDistance; }
        public PlatformBounds getAnchorPlatform() { return anchorPlatform; }
        public double getCenterX() { return x + width / 2.0; }
        public double getCenterY() { return y + height / 2.0; }
    }

    public static final long DEFAULT_SEED = 12345L;

    private TrapGenerationConfig config;

    /**
     * Construtor padrão: utiliza a {@link #DEFAULT_SEED} ({@code 12345L}) para garantir um layout
     * previsível e consistente de armadilhas toda vez que o jogo iniciar ou a fase reiniciar.
     */
    public TrapProceduralGenerator() {
        this(new TrapGenerationConfig().withSeed(DEFAULT_SEED));
    }

    /**
     * Construtor sobrecarregado para definir uma seed fixa customizada.
     */
    public TrapProceduralGenerator(long customSeed) {
        this(new TrapGenerationConfig().withSeed(customSeed));
    }

    /**
     * Construtor sobrecarregado que permite alternar entre seed aleatória a cada execução
     * ({@code useRandomSeed = true}) ou a seed fixa padrão ({@code useRandomSeed = false}).
     */
    public TrapProceduralGenerator(boolean useRandomSeed) {
        this(useRandomSeed
                ? new TrapGenerationConfig().withRandomSeed()
                : new TrapGenerationConfig().withSeed(DEFAULT_SEED));
    }

    public TrapProceduralGenerator(TrapGenerationConfig config) {
        this.config = (config != null) ? config : new TrapGenerationConfig().withSeed(DEFAULT_SEED);
    }

    public TrapGenerationConfig getConfig() {
        return config;
    }

    public void setConfig(TrapGenerationConfig config) {
        if (config != null) {
            this.config = config;
        }
    }

    /**
     * Gera e instancia automaticamente as entidades de armadilhas no mundo do FXGL
     * utilizando a seed configurada (por padrão {@link #DEFAULT_SEED}).
     *
     * @param sala    Dados do nível carregado.
     * @param jogador Entidade do jogador (para preservar a Safe Zone ao redor do spawn).
     * @return Lista de entidades de armadilhas criadas no mundo.
     */
    public List<Entity> populateWorld(DadosNivel sala, Entity jogador) {
        return populateWorld(sala, jogador, config.getSeed());
    }

    /**
     * Método sobrecarregado para popular o mundo com uma {@code seed} customizada
     * (ou {@code null} para geração aleatória não-determinística).
     */
    public List<Entity> populateWorld(DadosNivel sala, Entity jogador, Long customSeed) {
        if (sala == null) {
            return Collections.emptyList();
        }

        double playerX = (jogador != null) ? jogador.getX() : (sala.jogador != null ? sala.jogador.x : 0.0);
        double playerY = (jogador != null) ? jogador.getY() : (sala.jogador != null ? sala.jogador.y : 0.0);

        List<PlatformBounds> platforms = extractPlatforms(sala);
        List<TrapPlacement> placements = generatePlacements(
                platforms,
                sala.larguraNivel,
                sala.alturaNivel,
                playerX,
                playerY,
                customSeed
        );

        List<Entity> spawnedEntities = new ArrayList<>();
        for (TrapPlacement placement : placements) {
            SpawnData data = new SpawnData(placement.getX(), placement.getY())
                    .put("largura", placement.getWidth())
                    .put("altura", placement.getHeight());

            if ("armadilha_bloco".equals(placement.getSpawnType())) {
                data.put("distanciaQueda", placement.getFallDistance());
            }

            Entity entity = FXGL.spawn(placement.getSpawnType(), data);
            spawnedEntities.add(entity);
        }

        return spawnedEntities;
    }

    /**
     * Gera apenas os modelos de domínio ({@link Trap}) para uma sala (útil para testes sem FXGL ativo).
     */
    public List<Trap> generateTraps(DadosNivel sala, double playerX, double playerY) {
        return generateTraps(sala, playerX, playerY, config.getSeed());
    }

    /**
     * Método sobrecarregado para gerar os modelos de domínio ({@link Trap}) com uma seed específica
     * (ou {@code null} para seed aleatória).
     */
    public List<Trap> generateTraps(DadosNivel sala, double playerX, double playerY, Long customSeed) {
        if (sala == null) {
            return Collections.emptyList();
        }
        List<PlatformBounds> platforms = extractPlatformsFromDados(sala);
        List<TrapPlacement> placements = generatePlacements(
                platforms,
                sala.larguraNivel,
                sala.alturaNivel,
                playerX,
                playerY,
                customSeed
        );
        List<Trap> traps = new ArrayList<>(placements.size());
        for (TrapPlacement p : placements) {
            traps.add(p.getTrap());
        }
        return traps;
    }

    /**
     * Algoritmo principal de geração procedural utilizando a seed padrão/configurada.
     */
    public List<TrapPlacement> generatePlacements(List<PlatformBounds> platforms,
                                                  double levelWidth,
                                                  double levelHeight,
                                                  double playerX,
                                                  double playerY) {
        return generatePlacements(platforms, levelWidth, levelHeight, playerX, playerY, config.getSeed());
    }

    /**
     * Algoritmo principal de geração procedural sobrecarregado para aceitar uma {@code seedOverride}
     * customizada (ou {@code null} para geração puramente aleatória).
     */
    public List<TrapPlacement> generatePlacements(List<PlatformBounds> platforms,
                                                  double levelWidth,
                                                  double levelHeight,
                                                  double playerX,
                                                  double playerY,
                                                  Long seedOverride) {
        if (platforms == null || platforms.isEmpty() || config.getMaxTraps() <= 0) {
            return Collections.emptyList();
        }

        Random random = (seedOverride != null)
                ? new Random(seedOverride)
                : new Random();

        // 1. Coleta todos os pontos candidatos válidos com ancoragem exata nas superfícies
        List<TrapPlacement> ceilingCandidates = findCeilingBlockCandidates(platforms, levelWidth, playerX, playerY);
        List<TrapPlacement> surfaceCandidates = findFloorSpikeCandidates(platforms, levelWidth, playerX, playerY);

        // Embaralha os candidatos usando o gerador (determinístico se houver seed)
        Collections.shuffle(ceilingCandidates, random);
        Collections.shuffle(surfaceCandidates, random);

        // Intercala candidatos de teto (BlocoQueCai) e de chão (SpikesRetrateis) para variar os tipos na fase
        List<TrapPlacement> allCandidates = interleaveCandidates(ceilingCandidates, surfaceCandidates);

        List<TrapPlacement> selected = new ArrayList<>();
        List<TrapPlacement> skippedByChance = new ArrayList<>();

        // 2. Seleção estocástica baseada na densidade (spawnChance) e distância mínima
        for (TrapPlacement candidate : allCandidates) {
            if (selected.size() >= config.getMaxTraps()) {
                break;
            }
            if (!respectsMinDistance(candidate, selected)) {
                continue;
            }

            if (random.nextDouble() <= config.getSpawnChance()) {
                selected.add(candidate);
            } else {
                skippedByChance.add(candidate);
            }
        }

        // 3. Garantia da quantidade mínima (minTraps), caso haja candidatos válidos suficientes
        if (selected.size() < config.getMinTraps()) {
            for (TrapPlacement fallback : skippedByChance) {
                if (selected.size() >= config.getMinTraps() || selected.size() >= config.getMaxTraps()) {
                    break;
                }
                if (respectsMinDistance(fallback, selected)) {
                    selected.add(fallback);
                }
            }
        }

        return selected;
    }

    /**
     * Varre a face inferior do teto superior da fase para ancorar {@link BlocoQueCai}.
     * Prioriza o teto físico no topo da tela ({@code y <= maxCeilingTopY}), garantindo que os blocos
     * tenham toda a altura vertical livre até o piso principal sem plataformas intermediárias no caminho.
     */
    private List<TrapPlacement> findCeilingBlockCandidates(List<PlatformBounds> platforms,
                                                           double levelWidth,
                                                           double playerX,
                                                           double playerY) {
        List<TrapPlacement> candidates = new ArrayList<>();
        double blockW = config.getBlockWidth();
        double blockH = config.getBlockHeight();
        double stepX = Math.max(16.0, config.getGridStepX());

        // Filtra prioritariamente o teto superior da tela (y <= maxCeilingTopY, ex: y = 0)
        List<PlatformBounds> ceilingPlatforms = new ArrayList<>();
        for (PlatformBounds p : platforms) {
            if (p.getY() <= config.getMaxCeilingTopY() && p.getWidth() >= blockW) {
                ceilingPlatforms.add(p);
            }
        }
        // Fallback caso um cenário customizado de teste não possua teto em y <= maxCeilingTopY
        if (ceilingPlatforms.isEmpty()) {
            ceilingPlatforms.addAll(platforms);
        }

        for (PlatformBounds ceiling : ceilingPlatforms) {
            if (ceiling.getWidth() < blockW) {
                continue;
            }

            // Posição Y exatamente encostada na face inferior do teto superior
            double anchorY = ceiling.getMaxY();

            // Gera posições X alinhadas à grade sob o teto
            List<Double> xPositions = new ArrayList<>();
            double startX = ceiling.getX() + Math.min(config.getPlatformEdgeMargin(), (ceiling.getWidth() - blockW) / 2.0);
            double endX = ceiling.getMaxX() - blockW - Math.min(config.getPlatformEdgeMargin(), (ceiling.getWidth() - blockW) / 2.0);

            for (double x = startX; x <= endX + 0.01; x += stepX) {
                xPositions.add(x);
            }
            if (xPositions.isEmpty()) {
                xPositions.add(ceiling.getX() + (ceiling.getWidth() - blockW) / 2.0);
            }

            for (double x : xPositions) {
                if (!isWithinLevelSafeBounds(x, blockW, levelWidth)) {
                    continue;
                }

                // Busca o solo principal abaixo para garantir toda a altura vertical livre de queda
                double groundTopY = findMainGroundBelow(platforms, ceiling, x, anchorY, blockW);
                if (groundTopY == Double.MAX_VALUE) {
                    continue;
                }

                // Distância exata para que o bloco pare alinhado na superfície do solo (y = groundTopY - blockH)
                double fallDistance = (groundTopY - blockH) - anchorY;
                if (fallDistance < config.getMinFallClearance()) {
                    continue;
                }

                // Garante que toda a coluna vertical entre o teto superior e o chão esteja 100% livre de plataformas intermediárias
                if (!isVerticalCorridorClear(platforms, ceiling, x, anchorY, blockW, groundTopY - anchorY)) {
                    continue;
                }

                // Verifica a Zona Segura (Safe Zone) tanto na posição inicial quanto no corredor de queda
                if (isInsidePlayerSafeZone(x, anchorY, blockW, groundTopY - anchorY, playerX, playerY)) {
                    continue;
                }

                BlocoQueCai bloco = TrapFactory.createFallingBlock(x, anchorY, blockW, blockH, fallDistance);
                candidates.add(new TrapPlacement(
                        "armadilha_bloco",
                        bloco,
                        x,
                        anchorY,
                        blockW,
                        blockH,
                        fallDistance,
                        ceiling
                ));
            }
        }

        return candidates;
    }

    /**
     * Varre a superfície superior de plataformas/chãos sólidos para ancorar {@link SpikesRetrateis}.
     * Regra: {@code y = plat.y - spikeHeight} com espaço vazio livre acima, ignorando o teto superior.
     */
    private List<TrapPlacement> findFloorSpikeCandidates(List<PlatformBounds> platforms,
                                                         double levelWidth,
                                                         double playerX,
                                                         double playerY) {
        List<TrapPlacement> candidates = new ArrayList<>();
        double spikeW = config.getSpikeWidth();
        double spikeH = config.getSpikeHeight();
        double margin = config.getPlatformEdgeMargin();
        double stepX = Math.max(16.0, config.getGridStepX());

        for (PlatformBounds floor : platforms) {
            // Ignora o teto superior da fase (espinhos de chão não devem nascer em cima do teto em y <= 0)
            if (floor.getY() <= config.getMaxCeilingTopY() || (floor.getY() - spikeH) < 0) {
                continue;
            }
            if (floor.getWidth() < spikeW + (margin * 2.0)) {
                continue;
            }

            // Posição Y apoiada exatamente sobre a superfície superior da plataforma/chão
            double anchorY = floor.getY() - spikeH;

            double startX = floor.getX() + margin;
            double endX = floor.getMaxX() - spikeW - margin;

            for (double x = startX; x <= endX + 0.01; x += stepX) {
                if (!isWithinLevelSafeBounds(x, spikeW, levelWidth)) {
                    continue;
                }

                // Verifica se há espaço vazio acima da superfície da plataforma (sem teto colado em cima)
                double clearanceHeight = Math.max(spikeH, config.getMinTopClearance());
                if (!isTopClearanceFree(platforms, floor, x, floor.getY() - clearanceHeight, spikeW, clearanceHeight)) {
                    continue;
                }

                // Verifica a Zona Segura (Safe Zone) ao redor do jogador
                if (isInsidePlayerSafeZone(x, anchorY, spikeW, spikeH, playerX, playerY)) {
                    continue;
                }

                SpikesRetrateis spikes = TrapFactory.createSpikes(x, anchorY, spikeW, spikeH);
                candidates.add(new TrapPlacement(
                        "armadilha_espinhos",
                        spikes,
                        x,
                        anchorY,
                        spikeW,
                        spikeH,
                        0.0,
                        floor
                ));
            }
        }

        return candidates;
    }

    /**
     * Localiza a coordenada Y do topo do piso principal abaixo de {@code startY}
     * que suporta horizontalmente a faixa {@code [x, x + width]}, garantindo que o bloco
     * tenha toda a altura vertical da fase para cair (sem parar no meio em plataformas curtas).
     */
    private double findMainGroundBelow(List<PlatformBounds> platforms,
                                       PlatformBounds ceiling,
                                       double x,
                                       double startY,
                                       double width) {
        double lowestGroundY = -1.0;
        for (PlatformBounds plat : platforms) {
            if (plat == ceiling) {
                continue;
            }
            if (plat.getY() <= startY) {
                continue;
            }
            boolean coversHorizontally = plat.getX() <= x && plat.getMaxX() >= (x + width);
            if (coversHorizontally && plat.getY() > lowestGroundY) {
                lowestGroundY = plat.getY();
            }
        }
        return lowestGroundY > 0 ? lowestGroundY : Double.MAX_VALUE;
    }

    /**
     * Verifica se o espaço de queda entre {@code startY} e {@code startY + corridorHeight} está livre
     * de plataformas parciais que bloqueariam metade do bloco.
     */
    private boolean isVerticalCorridorClear(List<PlatformBounds> platforms,
                                            PlatformBounds ceiling,
                                            double x,
                                            double startY,
                                            double width,
                                            double corridorHeight) {
        // Pequena folga vertical de 1px nas extremidades para não colidir com o próprio teto ou o chão de destino
        double checkY = startY + 1.0;
        double checkH = Math.max(1.0, corridorHeight - 2.0);
        for (PlatformBounds plat : platforms) {
            if (plat == ceiling) {
                continue;
            }
            if (plat.intersects(x, checkY, width, checkH)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica se a área acima da plataforma está livre de outras plataformas/obstáculos.
     */
    private boolean isTopClearanceFree(List<PlatformBounds> platforms,
                                       PlatformBounds floor,
                                       double x,
                                       double clearanceY,
                                       double width,
                                       double clearanceHeight) {
        double checkH = Math.max(1.0, clearanceHeight - 1.0);
        for (PlatformBounds plat : platforms) {
            if (plat == floor) {
                continue;
            }
            if (plat.intersects(x, clearanceY, width, checkH)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Garante margem segura nas bordas esquerda/direita da sala (onde ficam os gatilhos de transição).
     */
    private boolean isWithinLevelSafeBounds(double x, double width, double levelWidth) {
        if (levelWidth <= 0) {
            return true;
        }
        double portalMargin = config.getPortalSafeMargin();
        return x >= portalMargin && (x + width) <= (levelWidth - portalMargin);
    }

    /**
     * Verifica se uma região candidata (ou trajetória de queda) invade a Zona Segura (Safe Zone) do jogador.
     * Aplica proteção tanto radial (euclidiana) quanto horizontal completa a partir de {@code playerX},
     * garantindo um início de fase limpo mesmo quando o jogador nasce no ar acima do chão.
     */
    private boolean isInsidePlayerSafeZone(double rx,
                                           double ry,
                                           double rw,
                                           double rh,
                                           double playerX,
                                           double playerY) {
        double radius = config.getSafeZoneRadius();
        if (radius <= 0) {
            return false;
        }

        // Distância horizontal mínima entre a armadilha [rx, rx + rw] e a coordenada inicial X do jogador
        double horizontalDist = 0.0;
        if (playerX < rx) {
            horizontalDist = rx - playerX;
        } else if (playerX > (rx + rw)) {
            horizontalDist = playerX - (rx + rw);
        }
        if (horizontalDist < radius) {
            return true;
        }

        // Distância euclidiana 2D até o ponto mais próximo do retângulo candidato
        double closestX = Math.max(rx, Math.min(playerX, rx + rw));
        double closestY = Math.max(ry, Math.min(playerY, ry + rh));
        double dist = Math.hypot(playerX - closestX, playerY - closestY);
        return dist < radius;
    }

    /**
     * Verifica se o candidato respeita a distância mínima de todas as armadilhas já selecionadas
     * e evita empilhamento vertical na mesma coluna X (Fair Play).
     */
    private boolean respectsMinDistance(TrapPlacement candidate, List<TrapPlacement> selected) {
        double minDist = config.getMinDistanceBetweenTraps();
        for (TrapPlacement existing : selected) {
            double distCenters = Math.hypot(
                    candidate.getCenterX() - existing.getCenterX(),
                    candidate.getCenterY() - existing.getCenterY()
            );
            if (distCenters < minDist) {
                return false;
            }

            // Evita sobreposição horizontal direta (ex: um BlocoQueCai exatamente sobre um SpikesRetrateis)
            double minHorizontalGap = minDist * 0.55;
            boolean horizontalOverlap = (candidate.getX() - minHorizontalGap) < (existing.getX() + existing.getWidth())
                    && (candidate.getX() + candidate.getWidth() + minHorizontalGap) > existing.getX();
            if (horizontalOverlap) {
                return false;
            }
        }
        return true;
    }

    private List<TrapPlacement> interleaveCandidates(List<TrapPlacement> listA, List<TrapPlacement> listB) {
        List<TrapPlacement> result = new ArrayList<>(listA.size() + listB.size());
        int max = Math.max(listA.size(), listB.size());
        for (int i = 0; i < max; i++) {
            if (i < listA.size()) {
                result.add(listA.get(i));
            }
            if (i < listB.size()) {
                result.add(listB.get(i));
            }
        }
        return result;
    }

    /**
     * Extrai as plataformas sólidas do mundo FXGL (se ativo) ou dos dados carregados do JSON ({@link DadosNivel}).
     */
    public List<PlatformBounds> extractPlatforms(DadosNivel sala) {
        try {
            List<Entity> worldPlatforms = FXGL.getGameWorld().getEntitiesByType(TipoEntidade.PLATAFORMA);
            if (!worldPlatforms.isEmpty()) {
                List<PlatformBounds> bounds = new ArrayList<>();
                double levelWidth = (sala != null && sala.larguraNivel > 0) ? sala.larguraNivel : Double.MAX_VALUE;
                for (Entity e : worldPlatforms) {
                    // Ignora paredes laterais invisíveis fora dos limites horizontais da sala
                    if (e.getX() < 0 || e.getX() >= levelWidth) {
                        continue;
                    }
                    if (e.getHeight() >= 400 && e.getWidth() <= 60) {
                        continue;
                    }
                    bounds.add(new PlatformBounds(e.getX(), e.getY(), e.getWidth(), e.getHeight()));
                }
                if (!bounds.isEmpty()) {
                    return bounds;
                }
            }
        } catch (Exception ignored) {
            // Mundo FXGL não inicializado (ex: ambiente de teste unitário)
        }
        return extractPlatformsFromDados(sala);
    }

    /**
     * Converte a lista de {@link DadosPlataforma} de um {@link DadosNivel} em {@link PlatformBounds},
     * incluindo o teto superior em {@code y = 0} caso não esteja declarado explicitamente no JSON.
     */
    public List<PlatformBounds> extractPlatformsFromDados(DadosNivel sala) {
        if (sala == null || sala.plataformas == null) {
            return Collections.emptyList();
        }
        List<PlatformBounds> bounds = new ArrayList<>(sala.plataformas.size() + 1);
        boolean hasCeiling = false;
        for (DadosPlataforma p : sala.plataformas) {
            if (p.y <= 0 && p.largura >= sala.larguraNivel * 0.5) {
                hasCeiling = true;
            }
            bounds.add(new PlatformBounds(p.x, p.y, p.largura, p.altura));
        }
        if (!hasCeiling && sala.larguraNivel > 0) {
            bounds.add(new PlatformBounds(0, 0, sala.larguraNivel, 40.0));
        }
        return bounds;
    }
}
