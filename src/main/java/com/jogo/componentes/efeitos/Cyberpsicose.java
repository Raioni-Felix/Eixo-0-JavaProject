package com.jogo.componentes.efeitos;

import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.ComponentePersonagem;

/**
 * Debuff Cyberpsicose: chance de 25% ao ativar o SandevistanMichyle.
 * - Dano contínuo (8 por segundo) até a vida chegar a 50% da vida que
 *   o jogador tinha quando o efeito começou
 * - Velocidade de movimento reduzida em 50%
 * - Cooldowns de dash e habilidades dobrados
 * - Dura até o Sandevistan acabar (energia zerada ou desligado) E a
 *   energia voltar a 50% da máxima. Continua mesmo com o Sandevistan
 *   desligado: é isso que faz ser um "preço" por usar o implante.
 */
public class Cyberpsicose implements Efeito {

    private static final double MULTIPLICADOR_VELOCIDADE = 0.5; // anda na metade da velocidade
    private static final double MULTIPLICADOR_COOLDOWN = 2.0;   // cooldowns 2x mais lentos
    private static final double DANO_POR_SEGUNDO = 8.0;         // dano contínuo por segundo

    private boolean ativo = false;
    private double vidaLimite;       // 50% da vida no momento da ativação
    private double timerDano = 0;
    private boolean sandevistanAcabou = false;

    @Override
    public void aplicar(ComponentePersonagem alvo) {
        if (!(alvo instanceof ComponenteJogador jogador) || ativo) {
            return; // só afeta o jogador, e não acumula
        }
        ativo = true;
        vidaLimite = jogador.getVidaAtual() * 0.5;
        timerDano = 0;
        sandevistanAcabou = false;
        jogador.setMultiplicadorVelocidade(MULTIPLICADOR_VELOCIDADE);
        jogador.setMultiplicadorCooldownCyberpsicose(MULTIPLICADOR_COOLDOWN);
    }

    @Override
    public void remover(ComponentePersonagem alvo) {
        if (!(alvo instanceof ComponenteJogador jogador) || !ativo) {
            return;
        }
        ativo = false;
        jogador.setMultiplicadorVelocidade(1.0);
        jogador.setMultiplicadorCooldownCyberpsicose(1.0);
    }

    //Avisado pelo SandevistanMichyle quando ele desliga (por energia zerada
    //ou pelo jogador). A partir daí, basta a energia voltar a 50%
    public void marcarFimDoSandevistan() {
        sandevistanAcabou = true;
    }

    @Override
    public boolean atualizar(ComponentePersonagem alvo, double tpf) {
        if (!ativo || !(alvo instanceof ComponenteJogador jogador)) {
            return false;
        }

        // Terminou: Sandevistan já acabou e a energia voltou a 50%
        if (sandevistanAcabou && jogador.getEnergiaAtual() >= jogador.getEnergiaMaxima() * 0.5) {
            return false;
        }

        // Dano contínuo até o limite (nunca mata)
        if (jogador.getVidaAtual() > vidaLimite) {
            timerDano += tpf;
            if (timerDano >= 1.0) {
                int dano = (int) Math.ceil(DANO_POR_SEGUNDO * timerDano);
                timerDano = 0;
                jogador.tomarDanoDebuff(dano);
                if (jogador.getVidaAtual() < vidaLimite) {
                    jogador.setVidaAtual((int) Math.ceil(vidaLimite));
                }
            }
        }
        return true;
    }

    @Override
    public boolean estaAtivo() {
        return ativo;
    }

    @Override
    public String getNome() {
        return "Cyberpsicose";
    }

    public boolean estaNoLimiteVida(ComponenteJogador jogador) {
        return jogador.getVidaAtual() <= vidaLimite;
    }
}
