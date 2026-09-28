package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.component.Component;

/**
 * Componente de animação/visual.
 *
 * Separado dos componentes de gameplay (ComponentePersonagem,
 * ComponenteInimigo, ComponenteJogador) de propósito: esse aqui só cuida
 * de QUAL animação/sprite mostrar, nunca de vida, dano ou XP. Um
 * componente de gameplay muda o estado daqui (ex: ComponenteJogador
 * chama definirEstado(EstadoAnimacao.MACHUCADO) quando toma dano), e
 * esse componente decide qual textura/spritesheet mostrar por causa
 * disso.
 *
 * Ainda não tem os assets (spritesheets) prontos, então por enquanto
 * isso é um esqueleto: o estado já é trocado e rastreado certo, só
 * falta plugar a troca de textura de verdade em onUpdate() quando as
 * imagens existirem (usando AnimationChannel/AnimatedTexture do FXGL,
 * ou entity.getViewComponent() pra texturas simples).
 */

public class ComponenteAnimado extends Component {

    public enum EstadoAnimacao {
        PARADO,
        ANDANDO,
        PULANDO,
        MACHUCADO
    }

    protected EstadoAnimacao estadoAtual = EstadoAnimacao.PARADO;
    private EstadoAnimacao estadoAnterior = null;

    //Troca o estado da animação. Só faz algo se o estado for
    //diferente do atual, pra não reiniciar a mesma animação toda hora.
    public void definirEstado(EstadoAnimacao novoEstado) {
        if (novoEstado == estadoAtual) {
            return;
        }

        estadoAtual = novoEstado;
    }



    public EstadoAnimacao getEstado() {
        return estadoAtual;
    }

    //onUpdate roda a cada frame. Detecta quando o estado mudou desde
    //o último frame e é aqui que entra a troca de textura/spritesheet
    //de verdade assim que os assets existirem.
    @Override
    public void onUpdate(double tpf) {
        if (estadoAtual == estadoAnterior) {
            return;
        }

        // TODO: trocar a textura/AnimationChannel de acordo com estadoAtual
        // ex: entity.getViewComponent().clearChildren();
        //     entity.getViewComponent().addChild(texturaDoEstado(estadoAtual));

        estadoAnterior = estadoAtual;
    }
}
