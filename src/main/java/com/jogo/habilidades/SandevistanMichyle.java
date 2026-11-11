package com.jogo.habilidades;

import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.TimeScaleManager;
import com.jogo.componentes.efeitos.Cyberpsicose;

/**
 * SandevistanMichyle (tecla D): Modo de slow-motion estilo Cyberpunk 2077.
 * 
 * - Toggle: ativa/desativa com tecla D
 * - Consome 20 energia/segundo enquanto ativo
 * - Time scale 0.25x (25% velocidade) para TUDO exceto movimento do jogador
 * - 10% chance de aplicar Cyberpsicose ao ativar
 * - Cooldown de 30s após desativar (por energia acabando ou cancelando)
 * - Para automaticamente quando energia chega a 0
 */
public class SandevistanMichyle extends Habilidade {

    private static final double CONSUMO_ENERGIA_POR_SEGUNDO = 20.0;
    private static final double CHANCE_CYBERPSICOSE = 0.25; // 25%
    private static final double COOLDOWN_POS_DESATIVAR = 30.0; // 30 segundos

    private boolean ativo = false;
    private double acumuladorConsumo = 0;
    private double cooldownPosDesativar = 0;
    private Cyberpsicose cyberpsicose;

    public SandevistanMichyle(boolean aprendida) {
        super(aprendida, COOLDOWN_POS_DESATIVAR, 0, 0); // custo 0 no construtor, duração 0 (toggle)
        this.cyberpsicose = new Cyberpsicose();
    }

    @Override
    public int custoPara(ComponenteJogador jogador) {
        // Custo é dinâmico: consome por segundo enquanto ativo
        // Retornamos 1 só para a verificação de podeUsar() funcionar
        return ativo ? 0 : 1;
    }

    @Override
    public boolean podeUsar(ComponenteJogador jogador) {
        if (!isAprendida()) {
            return false;
        }
        // Se está ativo, pode sempre desativar (toggle off)
        if (ativo) {
            return true;
        }
        // Se não ativo: precisa ter energia > 0 e não estar em cooldown pós-desativação
        return estaDisponivel() && jogador.getEnergiaAtual() > 0 && cooldownPosDesativar <= 0;
    }

    /**
     * Método público para alternar o Sandevistan (chamado pelo key listener).
     * Não sobrescreve usar() porque ele é final na classe pai.
     */
    public boolean toggle(ComponenteJogador jogador) {
        if (!podeUsar(jogador)) {
            return false;
        }

        if (ativo) {
            // Toggle OFF: desativa
            desativar(jogador);
        } else {
            // Toggle ON: ativa
            if (!ativar(jogador)) {
                return false; // não tinha energia suficiente
            }
        }
        return true;
    }

    private boolean ativar(ComponenteJogador jogador) {
        if (jogador.getEnergiaAtual() <= 0) {
            return false;
        }

        ativo = true;
        acumuladorConsumo = 0;
        
        // Ativa time scale global
        TimeScaleManager.setAtivo(true, jogador);

        // 25% chance de Cyberpsicose
        if (Math.random() < CHANCE_CYBERPSICOSE) {
            cyberpsicose.aplicar(jogador);
            System.out.println("[Sandevistan] CYBERPSICOSE ATIVADA!");
            // Feedback visual no HUD (se tiver acesso ao controle)
            // O ControleJogo.atualizar() vai mostrar aviso via hud.mostrarAviso()
        } else {
            System.out.println("[Sandevistan] Ativado sem Cyberpsicose (random: " + Math.random() + ")");
        }

        return true;
    }

    private void desativar(ComponenteJogador jogador) {
        ativo = false;
        
        // Desativa time scale global
        TimeScaleManager.setAtivo(false, null);

        // Remove Cyberpsicose se ativa
        if (cyberpsicose.estaAtivo()) {
            cyberpsicose.remover(jogador);
        }

        // Inicia cooldown pós-desativação
        cooldownPosDesativar = COOLDOWN_POS_DESATIVAR;
        // Usa o setter protegido da classe pai para definir o tempo restante
        setTempoRestante(COOLDOWN_POS_DESATIVAR);
    }

    /**
     * Chamado todo frame pelo ComponenteJogador.onUpdate().
     * Consome energia enquanto ativo e gerencia cooldown pós-desativação.
     */
    public void atualizarSandevistan(ComponenteJogador jogador, double tpf) {
        // Atualiza cooldown pós-desativação
        if (cooldownPosDesativar > 0) {
            cooldownPosDesativar -= tpf;
            if (cooldownPosDesativar <= 0) {
                setTempoRestante(0); // libera para usar de novo
            }
        }

        if (!ativo) {
            return;
        }

        // Consome energia por segundo
        acumuladorConsumo += CONSUMO_ENERGIA_POR_SEGUNDO * tpf;
        if (acumuladorConsumo >= 1.0) {
            int pontos = (int) acumuladorConsumo;
            acumuladorConsumo -= pontos;
            jogador.consumirEnergia(pontos);
        }

        // Atualiza Cyberpsicose se ativa
        if (cyberpsicose.estaAtivo()) {
            if (!cyberpsicose.atualizar(jogador, tpf)) {
                // Cyberpsicose terminou naturalmente
                cyberpsicose.remover(jogador);
            }
        }

        // Desliga automaticamente se energia acabou
        if (jogador.getEnergiaAtual() <= 0) {
            desativar(jogador);
        }
    }

    @Override
    protected void executar(ComponenteJogador usuario) {
        // Não usado - a lógica está no toggle()
        // Mantido vazio porque Habilidade.usar() é final e chama executar()
        // Mas nós não usamos o usar() da classe pai
    }

    public boolean isAtivo() {
        return ativo;
    }

    public double getCooldownRestante() {
        return cooldownPosDesativar > 0 ? cooldownPosDesativar : getTempoRestante();
    }

    public Cyberpsicose getCyberpsicose() {
        return cyberpsicose;
    }

    @Override
    public double getDuracao() {
        return ativo ? Double.POSITIVE_INFINITY : 0; // duração indefinida enquanto tiver energia
    }
}