package com.jogo.controle;

import java.util.List;

//Lista de todas as teclas do jogo, num lugar só: a tela de Opções
//mostra esta lista, e o README tem a mesma tabela. Se uma tecla mudar
//no ControleEntrada, muda aqui também.
public final class Controles {

    public static final List<String[]> LISTA = List.of(
            new String[]{"← / →", "andar"},
            new String[]{"Espaço / ↑", "pular (de novo no ar: pulo duplo)"},
            new String[]{"Shift", "dash"},
            new String[]{"Z", "atacar com a arma equipada"},
            new String[]{"Q", "trocar de arma"},
            new String[]{"X", "Bola de Fogo (gasta energia)"},
            new String[]{"C", "Apara: bloqueia o próximo golpe"},
            new String[]{"V", "Raio de Gelo: congela o inimigo (nível 2)"},
            new String[]{"B", "Buraco Negro: toda a energia, destrói a sala (nível 3)"},
            new String[]{"1 / 2", "usar Módulo de Cura / Energia"},
            new String[]{"I / TAB", "inventário"},
            new String[]{"ESC", "pausa"},
            new String[]{"F11", "tela cheia"});

    private Controles() {
    }
}
