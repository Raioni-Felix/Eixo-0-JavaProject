package com.jogo.componentes.visual;

import com.almasb.fxgl.entity.component.Component;

/**
 * Componente de animação/visual.
 *
 * Separado dos componentes de gameplay (CharacterComponent,
 * EnemyComponent, PlayerComponent) de propósito: esse aqui só cuida
 * de QUAL animação/sprite mostrar, nunca de vida, dano ou XP. Um
 * componente de gameplay muda o estado daqui (ex: PlayerComponent
 * chama setState(AnimState.HURT) quando toma dano), e esse componente
 * decide qual textura/spritesheet mostrar por causa disso.
 *
 * Ainda não tem os assets (spritesheets) prontos, então por enquanto
 * isso é um esqueleto: o estado já é trocado e rastreado certo, só
 * falta plugar a troca de textura de verdade em onUpdate() quando as
 * imagens existirem (usando AnimationChannel/AnimatedTexture do FXGL,
 * ou entity.getViewComponent() pra texturas simples).
 */

public class AnimatedComponent extends Component {

    public enum AnimState {
        IDLE,
        WALKING,
        JUMPING,
        HURT
    }

    protected AnimState currentState = AnimState.IDLE;
    private AnimState previousState = null;

    //Troca o estado da animação. Só faz algo se o estado for
    //diferente do atual, pra não reiniciar a mesma animação toda hora.
    public void setState(AnimState newState) {
        if (newState == currentState) {
            return;
        }

        currentState = newState;
    }



    public AnimState getState() {
        return currentState;
    }

    //onUpdate roda a cada frame. Detecta quando o estado mudou desde
    //o último frame e é aqui que entra a troca de textura/spritesheet
    //de verdade assim que os assets existirem.
    @Override
    public void onUpdate(double tpf) {
        if (currentState == previousState) {
            return;
        }

        // TODO: trocar a textura/AnimationChannel de acordo com currentState
        // ex: entity.getViewComponent().clearChildren();
        //     entity.getViewComponent().addChild(texturaDoEstado(currentState));

        previousState = currentState;
    }
}
