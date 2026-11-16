package com.jogo.npcs;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyDef;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.Fixture;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

public class FabricaNpcs implements EntityFactory {

    private static final class Sprite {
        final String arquivo;
        final int larguraQuadro;
        final int alturaQuadro;
        final int linha;
        final int quadros;
        final double segundos;

        Sprite(String arquivo, int larguraQuadro, int alturaQuadro, int linha, int quadros, double segundos) {
            this.arquivo = arquivo;
            this.larguraQuadro = larguraQuadro;
            this.alturaQuadro = alturaQuadro;
            this.linha = linha;
            this.quadros = quadros;
            this.segundos = segundos;
        }
    }

    private static final Sprite SPRITE_INFORMANTE = new Sprite("informante", 128, 128, 0, 8, 0.8);
    private static final Sprite SPRITE_CURANDEIRO = new Sprite("curandeiro", 128, 128, 0, 7, 0.8);

    private static final int CATEGORY_PLAYER = 0x0002;
    private static final double LARGURA = 20;
    private static final double ALTURA = 72;
    private static final double AJUSTE_Y = -56;

    private static final String PASTA_PNG = "/assets/textures/sprites/npcs/";

    @Spawns("npc_informante")
    public Entity spawnInformante(SpawnData data) {
        String nome = data.get("nome");
        List<String> falas = data.get("falas");
        return criarNpc(data, new NpcInformante(nome, falas), SPRITE_INFORMANTE, Color.web("#4fc3f7"));
    }

    @Spawns("npc_curandeiro")
    public Entity spawnCurandeiro(SpawnData data) {
        String nome = data.get("nome");
        List<String> falas = data.get("falas");
        int curas = data.hasKey("curas") ? data.<Integer>get("curas") : 1;
        return criarNpc(data, new NpcCurandeiro(nome, falas, curas), SPRITE_CURANDEIRO, Color.web("#50e678"));
    }

   private Entity criarNpc(SpawnData data, ComponenteNpc npc, Sprite sprite, Color corReserva) {
        var fisica = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        fisica.setBodyDef(bodyDef);
        fisica.setFixtureDef(new FixtureDef().friction(0.5f).density(1f));

        fisica.setOnPhysicsInitialized(() -> {
            for (Fixture fixture : fisica.getBody().getFixtures()) {
                var filtro = fixture.getFilterData();
                filtro.maskBits = filtro.maskBits & ~CATEGORY_PLAYER;
                fixture.setFilterData(filtro);
            }
        });

        Node visual = criarVisual(sprite, corReserva);

        // Se for AnimatedTexture, chamamos o loop diretamente aqui
        if (visual instanceof AnimatedTexture anim) {
            anim.loop();
        }

        return entityBuilder(data)
                .bbox(new HitBox(BoundingShape.box(LARGURA, ALTURA)))
                .view(visual)
                .zIndex(4)
                .with(fisica)
                .with(npc)
                .build();
    }

   private static Node criarVisual(Sprite sprite, Color corReserva) {
        URL url = FabricaNpcs.class.getResource(PASTA_PNG + sprite.arquivo + ".png");
        if (url != null) {
            Image imagem = new Image(url.toExternalForm());
            if (!imagem.isError() && imagem.getHeight() > 0) {
                Node visual = sprite.quadros > 1 ? visualAnimado(imagem, sprite) : visualParado(imagem);
                if (visual != null) {
                    return visual;
                } else {
                    System.err.println("[NPC] visualAnimado retornou null para: " + sprite.arquivo);
                }
            } else {
                System.err.println("[NPC] Erro ao carregar pixels da imagem ou arquivo corrompido: " + sprite.arquivo);
            }
        } else {
            System.err.println("[NPC] Arquivo NÃO encontrado no classpath: " + PASTA_PNG + sprite.arquivo + ".png");
        }

        // Retorna o retângulo se algo falhar
        Rectangle corpo = new Rectangle(LARGURA, ALTURA, corReserva);
        corpo.setArcWidth(12);
        corpo.setArcHeight(12);
        corpo.setStroke(Color.WHITE);
        corpo.setStrokeWidth(2);
        return corpo;
    }

    private static Node visualParado(Image imagem) {
        ImageView visual = new ImageView(imagem);
        visual.setPreserveRatio(true);
        visual.setFitHeight(ALTURA);
        visual.setSmooth(imagem.getHeight() > 128);
        double larguraVisual = imagem.getWidth() * ALTURA / imagem.getHeight();
        visual.setTranslateX((LARGURA - larguraVisual) / 2);
        visual.setTranslateY(AJUSTE_Y);
        return visual;
    }

    private static Node visualAnimado(Image folha, Sprite sprite) {
        boolean cabe = sprite.larguraQuadro > 0 && sprite.alturaQuadro > 0
                && sprite.quadros * sprite.larguraQuadro <= folha.getWidth()
                && (sprite.linha + 1) * sprite.alturaQuadro <= folha.getHeight();

        if (!cabe) {
            System.err.println("[NPC] A imagem " + sprite.arquivo + ".png tem " + (int) folha.getWidth() + "x"
                    + (int) folha.getHeight() + " px, mas a configuração pede " + sprite.quadros
                    + " quadros de " + sprite.larguraQuadro + "x" + sprite.alturaQuadro
                    + " na linha " + sprite.linha + ". Ajuste SPRITE_* na FabricaNpcs.");
            return null;
        }

        PixelReader leitor = folha.getPixelReader();
        List<Image> quadros = new ArrayList<>();
        for (int i = 0; i < sprite.quadros; i++) {
            quadros.add(new WritableImage(
                    leitor,
                    i * sprite.larguraQuadro,
                    sprite.linha * sprite.alturaQuadro,
                    sprite.larguraQuadro,
                    sprite.alturaQuadro
            ));
        }

        AnimatedTexture visual = new AnimatedTexture(new AnimationChannel(quadros, Duration.seconds(sprite.segundos)));
        visual.loop();
        visual.setPreserveRatio(true);
        visual.setFitHeight(ALTURA);
        visual.setSmooth(sprite.alturaQuadro > 128);

        double larguraVisual = sprite.larguraQuadro * ALTURA / sprite.alturaQuadro;
        visual.setTranslateX((LARGURA - larguraVisual) / 2);
        visual.setTranslateY(AJUSTE_Y);
        return visual;
    }
}