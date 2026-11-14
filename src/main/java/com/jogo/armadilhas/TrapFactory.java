package com.jogo.armadilhas;

import java.util.Map;

/**
 * Padrão Factory (GoF) para criação de armadilhas.
 *
 * Centraliza a instanciação a partir de coordenadas absolutas do mundo,
 * coordenadas em grid de tiles (tileX, tileY) ou dicionários de propriedades
 * importados de arquivos de mapa/nível (JSON, Tiled, etc.).
 */
public final class TrapFactory {

    private TrapFactory() {
        // Construtor privado para classe utilitária de fábrica
    }

    /**
     * Cria espinhos de chão padrão com ativação por proximidade e 2.0s de telegraphing.
     */
    public static SpikesRetrateis createSpikes(double x, double y, double width, double height) {
        return new SpikesRetrateis(x, y, width, height);
    }

    /**
     * Cria espinhos de chão com temporizadores e dano customizados.
     */
    public static SpikesRetrateis createSpikes(double x, double y, double width, double height,
                                              double inactiveDuration, double telegraphDuration,
                                              double activeDuration, double cooldownDuration, int damage) {
        return new SpikesRetrateis(x, y, width, height, inactiveDuration, telegraphDuration, activeDuration, cooldownDuration, damage);
    }

    /**
     * Cria um bloco esmagador que cai do teto.
     */
    public static BlocoQueCai createFallingBlock(double x, double y, double width, double height, double fallDistance) {
        return new BlocoQueCai(x, y, width, height, fallDistance, 0.5, 45);
    }

    /**
     * Cria uma armadilha a partir de coordenadas de grade (tile map).
     *
     * @param type Tipo da armadilha ("spikes", "falling_block").
     * @param tileX Coluna no grid.
     * @param tileY Linha no grid.
     * @param tileSize Tamanho do tile em pixels (ex: 32 ou 64).
     */
    public static Trap createFromTileCoordinates(String type, int tileX, int tileY, int tileSize, Map<String, Object> properties) {
        double worldX = tileX * tileSize;
        double worldY = tileY * tileSize;
        return createFromTileData(type, worldX, worldY, properties);
    }

    /**
     * Cria dinamicamente uma armadilha a partir de um identificador textual e mapa de propriedades.
     */
    public static Trap createFromTileData(String type, double x, double y, Map<String, Object> properties) {
        if (type == null) {
            throw new IllegalArgumentException("Tipo de armadilha não pode ser nulo.");
        }

        double width = getDouble(properties, "width", 64.0);
        double height = getDouble(properties, "height", 32.0);
        int damage = getInt(properties, "damage", 25);

        return switch (type.toLowerCase().trim()) {
            case "spikes", "espinhos", "spikes_retrateis" -> {
                double inactive = getDouble(properties, "inactiveDuration", -1.0); // Sem ciclo temporal automático
                double telegraph = getDouble(properties, "telegraphDuration", 2.0);  // Pelo menos 2.0s de tremor
                double active = getDouble(properties, "activeDuration", 2.0);
                double cooldown = getDouble(properties, "cooldownDuration", 1.5);
                yield new SpikesRetrateis(x, y, width, height, inactive, telegraph, active, cooldown, damage);
            }
            case "falling_block", "bloco_cai", "bloco_esmagador" -> {
                double fallDist = getDouble(properties, "maxFallDistance", 320.0);
                double telegraph = getDouble(properties, "telegraphDuration", 0.5);
                yield new BlocoQueCai(x, y, width, height, fallDist, telegraph, damage);
            }
            default -> throw new IllegalArgumentException("Tipo de armadilha desconhecido: " + type);
        };
    }

    private static double getDouble(Map<String, Object> props, String key, double defaultVal) {
        if (props == null || !props.containsKey(key)) return defaultVal;
        Object val = props.get(key);
        return (val instanceof Number num) ? num.doubleValue() : defaultVal;
    }

    private static int getInt(Map<String, Object> props, String key, int defaultVal) {
        if (props == null || !props.containsKey(key)) return defaultVal;
        Object val = props.get(key);
        return (val instanceof Number num) ? num.intValue() : defaultVal;
    }
}
