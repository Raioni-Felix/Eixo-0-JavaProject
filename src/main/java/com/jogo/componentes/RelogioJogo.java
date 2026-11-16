package com.jogo.componentes;

//"Período de graça" logo após despausar. Depois de um tempo pausado, o
//FXGL recupera o atraso disparando vários ticks em rajada, e isso fazia
//tudo que depende de tpf (câmera, voo dos voadores, timers do dash)
//"acelerar" por um instante. Os componentes perguntam
//emPeriodoDeGraca() e ignoram esse instante.
//Antes ficava no Main. Fica no modelo (componentes), e não no
//controle, porque quem lê são os componentes: o controle só avisa
//quando despausou (marcarRetomada), e o modelo não depende do controle.
public final class RelogioJogo {

    private static final long GRACA_NANOS = 400_000_000L; // 0.4s
    private static long gracaAteNanos = 0;

    private RelogioJogo() {
    }

    public static boolean emPeriodoDeGraca() {
        return System.nanoTime() < gracaAteNanos;
    }

    //Zera o período de graça (usado nos testes: sem isso, um teste que
    //chama marcarRetomada() fazia o próximo, rodado logo depois, falhar)
    public static void zerar() {
        gracaAteNanos = 0;
    }

    //Chamado sempre que o motor é despausado (ver controle.ControleJogo)
    public static void marcarRetomada() {
        gracaAteNanos = System.nanoTime() + GRACA_NANOS;
    }
}
