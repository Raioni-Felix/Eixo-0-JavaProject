package com.jogo.armadilhas;

/**
 * Parâmetros configuráveis para o sistema de Geração Procedural de Armadilhas.
 *
 * Permite ajustar densidade (chance de spawn), limites mínimo/máximo por sala,
 * raio da Zona Segura (Safe Zone) ao redor do jogador, distância mínima entre
 * armadilhas (Fair Play) e uma {@code seed} opcional para layouts determinísticos.
 */
public class TrapGenerationConfig {

    private int minTraps = 3;
    private int maxTraps = 8;
    private double spawnChance = 0.65;
    private Long seed = TrapProceduralGenerator.DEFAULT_SEED;

    // Regras de Fair Play
    private double safeZoneRadius = 550.0;
    private double minDistanceBetweenTraps = 300.0;
    private double portalSafeMargin = 200.0;

    // Dimensões padrão e alinhamento (Snap-to-Grid / Surface)
    private double gridStepX = 64.0;
    private double platformEdgeMargin = 16.0;
    private double maxCeilingTopY = 80.0;

    private double spikeWidth = 128.0;
    private double spikeHeight = 64.0;
    private double minTopClearance = 96.0;

    private double blockWidth = 64.0;
    private double blockHeight = 64.0;
    private double minFallClearance = 250.0;

    public TrapGenerationConfig() {
    }

    public TrapGenerationConfig(int minTraps, int maxTraps, double spawnChance, Long seed) {
        setMinTraps(minTraps);
        setMaxTraps(maxTraps);
        setSpawnChance(spawnChance);
        this.seed = seed;
    }

    public static TrapGenerationConfig padrao() {
        return new TrapGenerationConfig();
    }

    public static TrapGenerationConfig comSeed(long seed) {
        return new TrapGenerationConfig().withSeed(seed);
    }

    // === Fluent Builders ===

    public TrapGenerationConfig withMinTraps(int minTraps) {
        setMinTraps(minTraps);
        return this;
    }

    public TrapGenerationConfig withMaxTraps(int maxTraps) {
        setMaxTraps(maxTraps);
        return this;
    }

    public TrapGenerationConfig withSpawnChance(double spawnChance) {
        setSpawnChance(spawnChance);
        return this;
    }

    public TrapGenerationConfig withSeed(Long seed) {
        this.seed = seed;
        return this;
    }

    public TrapGenerationConfig withRandomSeed() {
        this.seed = null;
        return this;
    }

    public TrapGenerationConfig withSafeZoneRadius(double safeZoneRadius) {
        setSafeZoneRadius(safeZoneRadius);
        return this;
    }

    public TrapGenerationConfig withMinDistanceBetweenTraps(double minDistanceBetweenTraps) {
        setMinDistanceBetweenTraps(minDistanceBetweenTraps);
        return this;
    }

    public TrapGenerationConfig withPortalSafeMargin(double portalSafeMargin) {
        this.portalSafeMargin = Math.max(0.0, portalSafeMargin);
        return this;
    }

    public TrapGenerationConfig withGridStepX(double gridStepX) {
        this.gridStepX = Math.max(16.0, gridStepX);
        return this;
    }

    public TrapGenerationConfig withSpikeDimensions(double width, double height) {
        this.spikeWidth = Math.max(16.0, width);
        this.spikeHeight = Math.max(16.0, height);
        return this;
    }

    public TrapGenerationConfig withBlockDimensions(double width, double height) {
        this.blockWidth = Math.max(16.0, width);
        this.blockHeight = Math.max(16.0, height);
        return this;
    }

    public TrapGenerationConfig withMinTopClearance(double minTopClearance) {
        this.minTopClearance = Math.max(16.0, minTopClearance);
        return this;
    }

    public TrapGenerationConfig withMinFallClearance(double minFallClearance) {
        this.minFallClearance = Math.max(16.0, minFallClearance);
        return this;
    }

    // === Getters & Setters ===

    public int getMinTraps() {
        return minTraps;
    }

    public void setMinTraps(int minTraps) {
        this.minTraps = Math.max(0, minTraps);
        if (this.maxTraps < this.minTraps) {
            this.maxTraps = this.minTraps;
        }
    }

    public int getMaxTraps() {
        return maxTraps;
    }

    public void setMaxTraps(int maxTraps) {
        this.maxTraps = Math.max(this.minTraps, maxTraps);
    }

    public double getSpawnChance() {
        return spawnChance;
    }

    public void setSpawnChance(double spawnChance) {
        this.spawnChance = Math.max(0.0, Math.min(1.0, spawnChance));
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public double getSafeZoneRadius() {
        return safeZoneRadius;
    }

    public void setSafeZoneRadius(double safeZoneRadius) {
        this.safeZoneRadius = Math.max(0.0, safeZoneRadius);
    }

    public double getMinDistanceBetweenTraps() {
        return minDistanceBetweenTraps;
    }

    public void setMinDistanceBetweenTraps(double minDistanceBetweenTraps) {
        this.minDistanceBetweenTraps = Math.max(0.0, minDistanceBetweenTraps);
    }

    public double getPortalSafeMargin() {
        return portalSafeMargin;
    }

    public void setPortalSafeMargin(double portalSafeMargin) {
        this.portalSafeMargin = Math.max(0.0, portalSafeMargin);
    }

    public double getGridStepX() {
        return gridStepX;
    }

    public void setGridStepX(double gridStepX) {
        this.gridStepX = Math.max(16.0, gridStepX);
    }

    public double getPlatformEdgeMargin() {
        return platformEdgeMargin;
    }

    public void setPlatformEdgeMargin(double platformEdgeMargin) {
        this.platformEdgeMargin = Math.max(0.0, platformEdgeMargin);
    }

    public double getSpikeWidth() {
        return spikeWidth;
    }

    public double getSpikeHeight() {
        return spikeHeight;
    }

    public double getMinTopClearance() {
        return minTopClearance;
    }

    public double getBlockWidth() {
        return blockWidth;
    }

    public double getBlockHeight() {
        return blockHeight;
    }

    public double getMinFallClearance() {
        return minFallClearance;
    }

    public double getMaxCeilingTopY() {
        return maxCeilingTopY;
    }

    public void setMaxCeilingTopY(double maxCeilingTopY) {
        this.maxCeilingTopY = Math.max(0.0, maxCeilingTopY);
    }

    public TrapGenerationConfig withMaxCeilingTopY(double maxCeilingTopY) {
        setMaxCeilingTopY(maxCeilingTopY);
        return this;
    }
}
