package com.jogo.componentes;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.jogo.armadilhas.BlocoQueCai;
import com.jogo.armadilhas.DamageTarget;
import com.jogo.armadilhas.FxglTrapFeedbackListener;
import com.jogo.armadilhas.PlayerDamageTargetAdapter;
import com.jogo.armadilhas.SpikesRetrateis;
import com.jogo.armadilhas.Trap;
import com.jogo.armadilhas.TrapState;
import com.jogo.componentes.visual.SpikeTrapView;
import com.jogo.entidades.TipoEntidade;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Componente do FXGL que envelopa qualquer instância de {@link Trap}.
 *
 * Faz a ponte perfeita entre a arquitetura desacoplada de Orientação a Objetos (SOLID)
 * e o motor de jogos FXGL 17.3, permitindo que armadilhas sejam adicionadas ao mundo
 * como entidades nativas com ciclo de vida, renderização e detecção de colisão física.
 */
public class ComponenteArmadilha extends Component {

    private final Trap trap;
    private final FxglTrapFeedbackListener feedbackListener;
    private Entity jogador;
    private DamageTarget damageTarget;
    private Node visualNode;

    private AnimatedTexture animatedTexture;
    private final Map<TrapState, AnimationChannel> canaisAnimacao = new EnumMap<>(TrapState.class);
    private double visualWidth;
    private double visualHeight;
    private boolean terrenoSincronizado = false;

    public ComponenteArmadilha(Trap trap) {
        if (trap == null) {
            throw new IllegalArgumentException("Armadilha não pode ser nula.");
        }
        this.trap = trap;
        this.feedbackListener = new FxglTrapFeedbackListener();
        this.trap.addListener(this.feedbackListener);
        this.visualWidth = trap.getWidth();
        this.visualHeight = trap.getHeight();
    }

    public ComponenteArmadilha(Trap trap, AnimatedTexture animatedTexture, Map<TrapState, AnimationChannel> canais) {
        this(trap);
        configurarAnimacao(animatedTexture, canais, trap.getWidth(), trap.getHeight());
    }

    /**
     * Vincula a {@link AnimatedTexture} e os {@link AnimationChannel} mapeados por {@link TrapState}.
     */
    public void configurarAnimacao(AnimatedTexture texture, Map<TrapState, AnimationChannel> canais, double largura, double altura) {
        this.animatedTexture = texture;
        this.visualWidth = largura > 0 ? largura : (trap != null ? trap.getWidth() : 0);
        this.visualHeight = altura > 0 ? altura : (trap != null ? trap.getHeight() : 0);
        this.canaisAnimacao.clear();
        if (canais != null) {
            this.canaisAnimacao.putAll(canais);
        }
        if (this.visualNode == null && texture != null) {
            this.visualNode = texture;
        }
    }

    public void configurarAnimacao(AnimatedTexture texture, Map<TrapState, AnimationChannel> canais) {
        configurarAnimacao(texture, canais, trap.getWidth(), trap.getHeight());
    }

    /**
     * Configura a spritesheet única contendo os 4 quadros (frames) de expressão/estado
     * recortando automaticamente por código através do AnimationChannel e AnimatedTexture do FXGL:
     * - Frame 0: INACTIVE
     * - Frame 1: TELEGRAPHING
     * - Frame 2: ACTIVE
     * - Frame 3: COOLDOWN
     */
    public void configurarSpritesheet(Image spritesheet, int framesPerRow, int frameWidth, int frameHeight, double largura, double altura) {
        if (spritesheet == null) {
            return;
        }
        this.visualWidth = largura;
        this.visualHeight = altura;
        this.canaisAnimacao.clear();

        AnimationChannel animInactive = new AnimationChannel(spritesheet, framesPerRow, frameWidth, frameHeight, Duration.seconds(1), 0, 0);
        AnimationChannel animTelegraph = new AnimationChannel(spritesheet, framesPerRow, frameWidth, frameHeight, Duration.seconds(1), 1, 1);
        AnimationChannel animActive = new AnimationChannel(spritesheet, framesPerRow, frameWidth, frameHeight, Duration.seconds(1), 2, 2);
        AnimationChannel animCooldown = new AnimationChannel(spritesheet, framesPerRow, frameWidth, frameHeight, Duration.seconds(1), 3, 3);

        this.canaisAnimacao.put(TrapState.INACTIVE, animInactive);
        this.canaisAnimacao.put(TrapState.TELEGRAPHING, animTelegraph);
        this.canaisAnimacao.put(TrapState.ACTIVE, animActive);
        this.canaisAnimacao.put(TrapState.COOLDOWN, animCooldown);

        if (this.animatedTexture == null) {
            this.animatedTexture = new AnimatedTexture(animInactive);
        } else {
            this.animatedTexture.loopAnimationChannel(animInactive);
        }
        if (visualWidth > 0 && visualHeight > 0) {
            this.animatedTexture.setFitWidth(visualWidth);
            this.animatedTexture.setFitHeight(visualHeight);
        }
        if (this.visualNode == null) {
            this.visualNode = this.animatedTexture;
        }
    }

    public void setJogadorAlvo(Entity jogador) {
        this.jogador = jogador;
        if (jogador != null) {
            this.damageTarget = new PlayerDamageTargetAdapter(jogador, entity);
        }
    }

    @Override
    public void onAdded() {
        // Inicializa a posição da entidade com base na armadilha
        entity.setPosition(trap.getX(), trap.getY());

        // Se houver nós visuais na entidade, guarda referência para aplicar efeitos
        if (!entity.getViewComponent().getChildren().isEmpty()) {
            this.visualNode = entity.getViewComponent().getChildren().get(0);
            if (this.visualNode instanceof AnimatedTexture animTex && this.animatedTexture == null) {
                this.animatedTexture = animTex;
            }
        }

        // Alinha armadilhas com a superfície exata das plataformas/chão do mundo
        sincronizarComTerreno();

        // Tenta vincular o jogador já no momento de adição ao mundo
        buscarJogadorAlvo();
    }

    @Override
    public void onUpdate(double tpf) {
        if (RelogioJogo.emPeriodoDeGraca()) {
            return;
        }

        if (!terrenoSincronizado) {
            sincronizarComTerreno();
        }

        // Garante que o jogador esteja referenciado para checagens contínuas
        if (damageTarget == null) {
            buscarJogadorAlvo();
        }

        // 1. Atualiza temporizadores e máquina de estados da armadilha
        trap.update(tpf);

        // Se for um BlocoQueCai, garante continuamente que nunca ultrapasse o topo do chão
        if (trap instanceof BlocoQueCai bloco) {
            verificarLimiteSoloBloco(bloco);
        }

        // 2. Se a armadilha se moveu no mundo (ex: BlocoQueCai despencando), sincroniza com a física e posição
        if (entity != null && entity.hasComponent(PhysicsComponent.class)) {
            PhysicsComponent fisica = entity.getComponent(PhysicsComponent.class);
            try {
                if (fisica.getBody() != null) {
                    fisica.overwritePosition(new Point2D(trap.getX(), trap.getY()));
                }
            } catch (Exception ignored) {
            }
        }
        if (entity != null) {
            entity.setX(trap.getX());
            entity.setY(trap.getY());
        }

        // 3. Game Juice e Sincronização Visual do Sprite com a Máquina de Estados
        if (visualNode != null) {
            visualNode.setTranslateX(trap.getShakeOffsetX());

            // Alternância automática de frame/canal no AnimatedTexture conforme o TrapState
            if (animatedTexture != null) {
                visualNode.setTranslateY(trap.getShakeOffsetY());
                AnimationChannel canalAlvo = canaisAnimacao.get(trap.getState());
                if (canalAlvo != null && animatedTexture.getAnimationChannel() != canalAlvo) {
                    animatedTexture.loopAnimationChannel(canalAlvo);
                    if (visualWidth > 0 && visualHeight > 0) {
                        animatedTexture.setFitWidth(visualWidth);
                        animatedTexture.setFitHeight(visualHeight);
                    }
                }
                animatedTexture.onUpdate(tpf);
                if (visualWidth > 0 && visualHeight > 0) {
                    animatedTexture.setFitWidth(visualWidth);
                    animatedTexture.setFitHeight(visualHeight);
                }
            } else if (visualNode instanceof SpikeTrapView spikeView) {
                spikeView.atualizar(trap.getState(), trap.getStateTimer(), tpf);
                visualNode.setTranslateY(trap.getShakeOffsetY() + spikeView.getExtensionOffsetY());
            } else if (visualNode instanceof Rectangle rect) {
                visualNode.setTranslateY(trap.getShakeOffsetY());
                if (trap.getState() == TrapState.TELEGRAPHING) {
                    rect.setStroke(Color.RED);
                    rect.setStrokeWidth(2.0);
                } else {
                    rect.setStroke(Color.TRANSPARENT);
                }
            } else {
                visualNode.setTranslateY(trap.getShakeOffsetY());
            }
        }

        // 4. Verificação de gatilho de proximidade e colisão de dano pelo modelo da Trap
        if (damageTarget instanceof PlayerDamageTargetAdapter playerAdapter) {
            playerAdapter.update(tpf);
        }
        if (damageTarget != null && damageTarget.isAlive()) {
            trap.checkTrigger(damageTarget);
            trap.checkCollision(damageTarget);
        }
    }

    /**
     * Detecta as plataformas/chão da sala para:
     * 1) Calibrar a parada exata do BlocoQueCai na superfície superior do chão (y = topoDoChao - alturaDoBloco).
     * 2) Alinhar a base dos SpikesRetrateis exatamente na linha superior do piso para que fiquem ocultos em INACTIVE
     *    e emerjam acima do chão em TELEGRAPHING e ACTIVE.
     */
    private void sincronizarComTerreno() {
        try {
            List<Entity> plataformas = FXGL.getGameWorld().getEntitiesByType(TipoEntidade.PLATAFORMA);
            if (plataformas.isEmpty()) {
                return;
            }

            if (trap instanceof BlocoQueCai bloco) {
                verificarLimiteSoloBloco(bloco);
                terrenoSincronizado = true;
            } else if (trap instanceof SpikesRetrateis espinhos) {
                Entity chaoAlvo = null;
                double menorDistanciaVertical = Double.MAX_VALUE;
                double baseEspinhosY = espinhos.getY() + espinhos.getHeight();

                for (Entity plat : plataformas) {
                    boolean sobrepoeHorizontal = plat.getX() < (espinhos.getX() + espinhos.getWidth())
                            && (plat.getX() + plat.getWidth()) > espinhos.getX();
                    if (sobrepoeHorizontal) {
                        double dist = Math.abs(baseEspinhosY - plat.getY());
                        if (dist <= espinhos.getHeight() && dist < menorDistanciaVertical) {
                            menorDistanciaVertical = dist;
                            chaoAlvo = plat;
                        }
                    }
                }

                if (chaoAlvo != null) {
                    double novoY = chaoAlvo.getY() - espinhos.getHeight();
                    espinhos.setY(novoY);
                    if (entity != null) {
                        entity.setPosition(espinhos.getX(), novoY);
                    }
                }
                terrenoSincronizado = true;
            }
        } catch (Exception ignored) {
            // Isolamento para testes unitários sem FXGL GameWorld ativo
        }
    }

    private void verificarLimiteSoloBloco(BlocoQueCai bloco) {
        try {
            List<Entity> plataformas = FXGL.getGameWorld().getEntitiesByType(TipoEntidade.PLATAFORMA);
            double menorTopoChao = Double.MAX_VALUE;

            for (Entity plat : plataformas) {
                boolean sobrepoeHorizontal = plat.getX() < (bloco.getX() + bloco.getWidth())
                        && (plat.getX() + plat.getWidth()) > bloco.getX();
                if (sobrepoeHorizontal && plat.getY() >= (bloco.getInitialY() + bloco.getHeight())) {
                    if (plat.getY() < menorTopoChao) {
                        menorTopoChao = plat.getY();
                    }
                }
            }

            if (menorTopoChao < Double.MAX_VALUE) {
                bloco.alignWithGroundSurface(menorTopoChao);
            }
        } catch (Exception ignored) {
        }
    }

    private void buscarJogadorAlvo() {
        try {
            List<Entity> jogadores = FXGL.getGameWorld().getEntitiesByType(TipoEntidade.JOGADOR);
            if (!jogadores.isEmpty()) {
                setJogadorAlvo(jogadores.get(0));
            }
        } catch (Exception ignored) {
            // Isolamento para testes unitários sem FXGL GameWorld
        }
    }

    /**
     * Invocado pelo CollisionHandler do FXGL quando a armadilha colide com uma plataforma/chão.
     */
    public void aoColidirComPlataforma(Entity plataformaEntity) {
        if (trap instanceof BlocoQueCai bloco && plataformaEntity != null) {
            if (plataformaEntity.getY() >= bloco.getInitialY() + bloco.getHeight()) {
                bloco.alignWithGroundSurface(plataformaEntity.getY());
                if (entity != null) {
                    entity.setY(bloco.getY());
                    if (entity.hasComponent(PhysicsComponent.class)) {
                        try {
                            PhysicsComponent fisica = entity.getComponent(PhysicsComponent.class);
                            if (fisica.getBody() != null) {
                                fisica.overwritePosition(new Point2D(bloco.getX(), bloco.getY()));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    /**
     * Invocado pelo CollisionHandler do FXGL quando o jogador entra em contato físico com a armadilha.
     */
    public void aoColidirComJogador(Entity jogadorEntity) {
        if (damageTarget == null) {
            setJogadorAlvo(jogadorEntity);
        }
        if (damageTarget != null && damageTarget.isAlive() && !damageTarget.isInvulnerable()) {
            if (trap instanceof BlocoQueCai bloco) {
                // No BlocoQueCai, qualquer contato com qualquer face aplica dano e knockback durante todo o ciclo de perigo
                if (bloco.isDangerous()) {
                    bloco.applyDamage(damageTarget);
                }
            } else if (trap.getState() == TrapState.ACTIVE) {
                trap.applyDamage(damageTarget);
            }
        }
    }

    public Trap getTrap() {
        return trap;
    }

    public FxglTrapFeedbackListener getFeedbackListener() {
        return feedbackListener;
    }

    public AnimatedTexture getAnimatedTexture() {
        return animatedTexture;
    }

    public Map<TrapState, AnimationChannel> getCanaisAnimacao() {
        return canaisAnimacao;
    }
}
