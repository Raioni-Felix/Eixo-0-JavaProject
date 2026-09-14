package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;

/**
 * Faz a hitbox do ataque corpo a corpo (entidade "ataque_jogador", ver
 * FabricaEntidades.spawnAtaqueJogador()) seguir o player enquanto ela
 * durar (ExpireCleanComponent remove a entidade sozinha depois de
 * 0.15s, ver lá).
 *
 * Antes, a hitbox nascia numa posição fixa do mundo (calculada uma vez
 * em WeaponComponent.performMeleeAttack()) e ficava parada lá — se o
 * player andasse, pulasse ou desse dash durante essa janela curta, a
 * hitbox "ficava pra trás", desgrudada do personagem. Agora ela
 * recalcula a posição todo frame em cima da posição ATUAL do player,
 * mantendo o mesmo deslocamento (offsetX/offsetY) capturado no
 * instante do golpe — ou seja, gruda no player, mas não muda de lado
 * no meio do golpe se o player virar.
 *
 * overwritePosition() (em vez de entity.setPosition()) porque essa
 * entidade tem PhysicsComponent (KINEMATIC, ver FabricaEntidades) —
 * mexer na posição de um corpo com física ativa direto pelo Entity
 * seria ignorado ou brigaria com o Box2D, mesmo problema documentado
 * em PlayerComponent/Main.trocarSala() pro teleporte de sala.
 */
public class AtaqueJogadorComponent extends Component {

    private final Entity jogador;
    private final double offsetX;
    private final double offsetY;

    private PhysicsComponent physics;

    public AtaqueJogadorComponent(Entity jogador, double offsetX, double offsetY) {
        this.jogador = jogador;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        physics.overwritePosition(new Point2D(jogador.getX() + offsetX, jogador.getY() + offsetY));
    }
}
