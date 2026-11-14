package com.jogo.armadilhas;

/**
 * Alias especializado para {@link TrapProceduralGenerator}, permitindo instanciar
 * o sistema de geração e spawn procedural tanto como {@code TrapSpawner} quanto
 * como {@code TrapProceduralGenerator}.
 */
public class TrapSpawner extends TrapProceduralGenerator {

    public TrapSpawner() {
        super();
    }

    public TrapSpawner(long customSeed) {
        super(customSeed);
    }

    public TrapSpawner(boolean useRandomSeed) {
        super(useRandomSeed);
    }

    public TrapSpawner(TrapGenerationConfig config) {
        super(config);
    }
}
