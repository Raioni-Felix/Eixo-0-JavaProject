package com.jogo.componentes.efeitos;

import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.ComponentePersonagem;

/**
 * Debuff Cyberpsicose: chance de 10% ao ativar SandevistanMichyle.
 * - Dano contínuo até a vida chegar a 50% da vida atual no momento da ativação
 * - Velocidade de movimento reduzida em 50%
 * - Cooldowns de dash e habilidades dobrados
 * - Dura até a energia zerar E recuperar 50% da energia máxima
 */
public class Cyberpsicose implements Efeito {

    private static final double REDUCAO_VELOCIDADE = 0.5; // 50% da velocidade
    private static final double MULTIPLICADOR_COOLDOWN = 2.0; // cooldowns 2x mais lentos
    private static final double DANO_POR_SEGUNDO = 8.0; // dano contínuo por segundo

    private boolean ativo = false;
    private double vidaLimite; // 50% da vida atual no momento da ativação
    private double velocidadeOriginal;
    private double timerDano = 0;
    private boolean energiaZerou = false;

    @Override
    public void aplicar(ComponentePersonagem alvo) {
        if (!(alvo instanceof ComponenteJogador jogador)) {
            return; // Cyberpsicose só afeta o jogador
        }

        if (ativo) {
            return; // já aplicado
        }

        ativo = true;
        vidaLimite = jogador.getVidaAtual() * 0.5; // 50% da vida ATUAL
        velocidadeOriginal = jogador.getVelocidade();

        // Reduz velocidade
        jogador.setVelocidade(velocidadeOriginal * REDUCAO_VELOCIDADE);

        // Dobra cooldowns: vamos usar um multiplicador no jogador
        jogador.setMultiplicadorCooldownCyberpsicose(MULTIPLICADOR_COOLDOWN);

        energiaZerou = false;
    }

    @Override
    public void remover(ComponentePersonagem alvo) {
        if (!(alvo instanceof ComponenteJogador jogador)) {
            return;
        }

        if (!ativo) {
            return;
        }

        ativo = false;

        // Restaura velocidade
        jogador.setVelocidade(velocidadeOriginal);

        // Remove multiplicador de cooldown
        jogador.setMultiplicadorCooldownCyberpsicose(1.0);
    }

    @Override
    public boolean atualizar(ComponentePersonagem alvo, double tpf) {
        if (!ativo || !(alvo instanceof ComponenteJogador jogador)) {
            return false;
        }

        // Verifica se energia zerou
        if (jogador.getEnergiaAtual() <= 0) {
            energiaZerou = true;
        }

        // Se energia zerou E recuperou 50% da energia máxima, remove o efeito
        if (energiaZerou && jogador.getEnergiaAtual() >= jogador.getEnergiaMaxima() * 0.5) {
            return false; // sinaliza para remover
        }

        // Aplica dano contínuo até chegar no limite (50% da vida original)
        if (jogador.getVidaAtual() > vidaLimite) {
            timerDano += tpf;
            if (timerDano >= 1.0) { // a cada 1 segundo
                int dano = (int) Math.ceil(DANO_POR_SEGUNDO * timerDano);
                timerDano = 0;
                // Usa tomarDano direto (ignora invencibilidade, é dano do debuff)
                jogador.tomarDanoDebuff(dano);
                // Garante não passar do limite
                if (jogador.getVidaAtual() < vidaLimite) {
                    jogador.setVidaAtual((int) Math.ceil(vidaLimite));
                }
            }
        }

        return true; // continua ativo
    }

    @Override
    public boolean estaAtivo() {
        return ativo;
    }

    @Override
    public String getNome() {
        return "Cyberpsicose";
    }

    /**
     * Verifica se o jogador está com vida no limite crítico (50%).
     */
    public boolean estaNoLimiteVida(ComponenteJogador jogador) {
        return jogador.getVidaAtual() <= vidaLimite;
    }
}