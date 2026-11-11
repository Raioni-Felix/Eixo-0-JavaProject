package com.jogo.componentes;

import com.almasb.fxgl.entity.SpawnData;
import com.jogo.componentes.TimeScaleManager;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.spawn;

/**
 * Chefe da última sala: NEXUS, um "logo de IA" flutuante. Filho de
 * ComponenteInimigo (vida, dano de toque, XP, congelar), mas com o
 * comportamento próprio numa máquina de estados, no estilo dos chefes
 * de Metroid: paira, avisa (brilha) e ataca, sempre na mesma rotação
 * de 3 ataques (ver AtaqueChefe):
 *  - RAJADA: leque de tiros na direção da jogadora;
 *  - INVESTIDA: mergulha em cima de onde ela estava e volta pro alto;
 *  - CHUVA: marca colunas no chão e solta blocos do teto nelas.
 * Com metade da vida ou menos entra na fase 2: espera menos entre os
 * ataques, avisa mais rápido e solta mais tiros/blocos.
 *
 * Sobrescreve também como as magias o afetam (polimorfismo): congela
 * só metade do tempo, não é puxado pelo Buraco Negro e só perde 25% da
 * vida máxima nele, em vez de morrer na hora.
 */
public class ComponenteChefe extends ComponenteInimigo {

    public enum Estado { PAIRANDO, AVISANDO, ATACANDO, VOLTANDO }

    private static final double ALTURA_PAIRANDO = 240;
    private static final double MARGEM_ARENA = 180;
    private static final double VELOCIDADE_INVESTIDA = 720;
    private static final double VELOCIDADE_TIRO = 380;
    private static final double VELOCIDADE_BLOCO = 520;
    private static final double TEMPO_ALERTA_CHUVA = 0.9;

    private final double larguraArena;
    private Estado estado = Estado.PAIRANDO;
    private AtaqueChefe proximoAtaque = AtaqueChefe.RAJADA;
    private AtaqueChefe ataqueAtual;
    private double tempoNoEstado = 0;
    private double tempoTotal = 0;

    //Alvo da investida e colunas da chuva, guardados no começo do aviso
    private Point2D destinoInvestida;
    private final List<Double> colunasChuva = new ArrayList<>();

    public ComponenteChefe(String nome, int vidaMaxima, double velocidade, int dano, double larguraArena) {
        //voador (sem gravidade), "distância" e sempre agressivo
        super(nome, vidaMaxima, velocidade, dano, 0, 2000, true, true, true);
        if (larguraArena <= 2 * MARGEM_ARENA) {
            throw new IllegalArgumentException("Arena pequena demais pro chefe");
        }
        this.larguraArena = larguraArena;
    }

    //=== Fases ===

    public int getFase() {
        return getVidaAtual() * 2 <= getVidaMaxima() ? 2 : 1;
    }

    public double getTempoPairando() {
        return getFase() == 1 ? 2.2 : 1.3;
    }

    public double getTempoAviso() {
        return getFase() == 1 ? 0.7 : 0.45;
    }

    public int getQuantidadeTiros() {
        return getFase() == 1 ? 5 : 7;
    }

    public int getQuantidadeBlocos() {
        return getFase() == 1 ? 4 : 6;
    }

    //=== Máquina de estados ===

    @Override
    public void onUpdate(double tpf) {
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }
        
        // Aplica time scale do Sandevistan
        double tpfEfetivo = TimeScaleManager.getTpfPara(tpf, this);
        
        if (atualizarCongelamento(tpfEfetivo)) {
            return;
        }
        if (alvo == null || !alvo.isActive()) {
            fisica.setLinearVelocity(0, 0);
            return;
        }
        tempoNoEstado += tpfEfetivo;
        tempoTotal += tpfEfetivo;
        olhandoDireita = alvo.getX() > entity.getX();

        switch (estado) {
            case PAIRANDO -> pairar();
            case AVISANDO -> avisar();
            case ATACANDO -> atacar();
            case VOLTANDO -> voltar();
        }
    }

    private void mudarPara(Estado novo) {
        estado = novo;
        tempoNoEstado = 0;
    }

    //Flutua acima da jogadora (sem sair da arena), balançando
    private void pairar() {
        double alvoX = limitarX(alvo.getCenter().getX());
        double alvoY = ALTURA_PAIRANDO + Math.sin(tempoTotal * 2.4) * 30;
        irPara(new Point2D(alvoX, alvoY), velocidade);

        if (tempoNoEstado >= getTempoPairando()) {
            ataqueAtual = proximoAtaque;
            proximoAtaque = proximoAtaque.proximo();
            comecarAviso();
            mudarPara(Estado.AVISANDO);
        }
    }

    //Parado e brilhando (animação de carga). A investida guarda o alvo
    //aqui; a chuva já marca as colunas no chão
    private void comecarAviso() {
        fisica.setLinearVelocity(0, 0);
        if (ataqueAtual == AtaqueChefe.INVESTIDA) {
            destinoInvestida = alvo.getCenter();
        } else if (ataqueAtual == AtaqueChefe.CHUVA) {
            colunasChuva.clear();
            double xJogadora = alvo.getCenter().getX();
            int n = getQuantidadeBlocos();
            for (int i = 0; i < n; i++) {
                //Uma coluna em cima da jogadora e as outras espalhadas
                double x = xJogadora + (i - n / 2) * 110 + (i % 2 == 0 ? 0 : 25);
                x = Math.max(40, Math.min(larguraArena - 40, x));
                colunasChuva.add(x);
                spawn("alerta_chefe", new SpawnData(x - 14, 0).put("duracao", TEMPO_ALERTA_CHUVA));
            }
        }
    }

    private void avisar() {
        double tempoAviso = ataqueAtual == AtaqueChefe.CHUVA ? TEMPO_ALERTA_CHUVA : getTempoAviso();
        if (tempoNoEstado < tempoAviso) {
            //Treme um pouco enquanto carrega
            fisica.setLinearVelocity(Math.sin(tempoTotal * 60) * 60, 0);
            return;
        }
        switch (ataqueAtual) {
            case RAJADA -> dispararRajada();
            case INVESTIDA -> iniciarInvestida();
            case CHUVA -> soltarChuva();
        }
        mudarPara(Estado.ATACANDO);
    }

    private void atacar() {
        if (ataqueAtual == AtaqueChefe.INVESTIDA) {
            //Mergulha até chegar no ponto (ou bater no chão) ou 1 s passar
            boolean chegou = entity.getCenter().distance(destinoInvestida) < 30;
            if (chegou || tempoNoEstado > 1.0) {
                mudarPara(Estado.VOLTANDO);
            }
            return;
        }
        fisica.setLinearVelocity(0, 0);
        if (tempoNoEstado >= 0.5) {
            mudarPara(Estado.PAIRANDO);
        }
    }

    //Sobe de volta pra altura de pairar
    private void voltar() {
        Point2D topo = new Point2D(limitarX(entity.getCenter().getX()), ALTURA_PAIRANDO);
        irPara(topo, velocidade * 1.6);
        if (entity.getCenter().distance(topo) < 20 || tempoNoEstado > 1.5) {
            mudarPara(Estado.PAIRANDO);
        }
    }

    //=== Ataques ===

    //Leque de tiros centrado na direção da jogadora
    private void dispararRajada() {
        Point2D centro = entity.getCenter();
        Point2D mira = alvo.getCenter().subtract(centro);
        double anguloBase = Math.atan2(mira.getY(), mira.getX());
        int n = getQuantidadeTiros();
        double abertura = Math.toRadians(14);
        for (int i = 0; i < n; i++) {
            double angulo = anguloBase + (i - (n - 1) / 2.0) * abertura;
            spawnTiro(centro, new Point2D(Math.cos(angulo), Math.sin(angulo)), VELOCIDADE_TIRO, "tiro");
        }
    }

    private void iniciarInvestida() {
        Point2D direcao = destinoInvestida.subtract(entity.getCenter());
        if (direcao.magnitude() > 0) {
            direcao = direcao.normalize().multiply(VELOCIDADE_INVESTIDA);
            fisica.setLinearVelocity(direcao.getX(), direcao.getY());
        }
    }

    //Um bloco caindo do teto em cada coluna marcada
    private void soltarChuva() {
        for (double x : colunasChuva) {
            spawnTiro(new Point2D(x, -10), new Point2D(0, 1), VELOCIDADE_BLOCO, "dado");
        }
    }

    private void spawnTiro(Point2D centro, Point2D direcao, double velocidadeTiro, String sprite) {
        spawn("projetil_chefe", new SpawnData(centro.getX() - 7, centro.getY() - 7)
                .put("origem", entity)
                .put("alvo", alvo)
                .put("dano", dano)
                .put("velocidade", velocidadeTiro)
                .put("direcao", direcao)
                .put("sprite", sprite));
    }

    //=== Auxiliares ===

    private void irPara(Point2D ponto, double vel) {
        Point2D delta = ponto.subtract(entity.getCenter());
        if (delta.magnitude() < 6) {
            fisica.setLinearVelocity(0, 0);
            return;
        }
        //Mais devagar perto do ponto, pra não ficar tremendo
        double v = Math.min(vel, delta.magnitude() * 3);
        Point2D velocidadeFinal = delta.normalize().multiply(v);
        fisica.setLinearVelocity(velocidadeFinal.getX(), velocidadeFinal.getY());
    }

    public double limitarX(double x) {
        return Math.max(MARGEM_ARENA, Math.min(larguraArena - MARGEM_ARENA, x));
    }

    //=== O que muda em relação aos outros inimigos ===

    //Na investida o toque dói o dobro
    @Override
    public int getDano() {
        boolean investindo = estado == Estado.ATACANDO && ataqueAtual == AtaqueChefe.INVESTIDA;
        return investindo ? dano * 2 : dano;
    }

    //Pose de ataque = brilho de carga (aviso) e o instante do ataque
    @Override
    public boolean estaAtacando() {
        return estado == Estado.AVISANDO || (estado == Estado.ATACANDO && ataqueAtual != AtaqueChefe.INVESTIDA)
                || super.estaAtacando();
    }

    //Gelo segura o chefe só metade do tempo
    @Override
    public void congelar(double segundos) {
        super.congelar(segundos / 2);
    }

    //O Buraco Negro não arrasta o chefe...
    @Override
    public void puxarPara(Point2D ponto, double forca) {
    }

    //...e tira só 25% da vida máxima
    @Override
    public int danoDeBuracoNegro() {
        return Math.max(1, getVidaMaxima() / 4);
    }

    //Ao morrer, várias explosões em volta (a do centro já vem da mãe)
    @Override
    public void tomarDano(int quant) {
        Point2D centro = entity != null ? entity.getCenter() : null;
        super.tomarDano(quant);
        if (estaMorto() && centro != null) {
            for (int i = 0; i < 6; i++) {
                double angulo = i * Math.PI / 3;
                spawn("explosao", centro.getX() + Math.cos(angulo) * 50, centro.getY() + Math.sin(angulo) * 50);
            }
        }
    }

    @Override
    public int getExperienciaConcedida() {
        return 300;
    }

    public Estado getEstado() {
        return estado;
    }

    public AtaqueChefe getProximoAtaque() {
        return proximoAtaque;
    }
}
