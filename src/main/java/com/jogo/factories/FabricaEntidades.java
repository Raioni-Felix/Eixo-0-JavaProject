package com.jogo.factories;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyDef;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import com.jogo.componentes.FlyingEnemyComponent;
import com.jogo.componentes.PlayerComponent;
import com.jogo.componentes.RangedEnemyComponent;
import com.jogo.componentes.visual.ProjectileComponent;
import com.jogo.entidades.EntityType;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

/**
 * Fábrica central de entidades do jogo.
 *
 * Ideia trazida da versão que o time estava fazendo no NetBeans
 * (EntityFactory + @Spawns) — em vez de descartar, foi incorporada
 * aqui em cima do que já tínhamos: ela só organiza a CRIAÇÃO das
 * entidades num lugar só (via FXGL.spawn("nome", dados)), delegando o
 * comportamento de cada uma pros componentes que já existiam
 * (PlayerComponent, FlyingEnemyComponent, RangedEnemyComponent,
 * ProjectileComponent).
 *
 * Registrada uma vez em Main.initGame():
 *   getGameWorld().addEntityFactory(new FabricaEntidades());
 *
 * Cada @Spawns("nome") é chamado via FXGL.spawn("nome", spawnData),
 * onde spawnData carrega x/y (construtor) e os dados extras
 * (.put("chave", valor)). SpawnData.get(key) lança exceção se a chave
 * não existir — por isso todo spawn precisa vir com todos os .put()
 * necessários (não tem valor "default" escondido aqui).
 *
 * NOVO (migração pra física real): jogador e plataformas agora ganham
 * um PhysicsComponent (motor Box2D do FXGL) — é ele quem lida com
 * gravidade e colisão de verdade agora, não mais a gravidade manual
 * que tínhamos no PlayerComponent. Os inimigos (voador/ranged) ainda
 * NÃO têm física: continuam se movendo na mão (translateX/Y dentro de
 * EnemyComponent/FlyingEnemyComponent/RangedEnemyComponent), porque
 * dar PhysicsComponent a eles exigiria reescrever aquela lógica de
 * perseguição pra usar velocidade em vez de translate — fica pra um
 * próximo passo, se/quando fizer sentido eles também pisarem em
 * plataforma.
 */
public class FabricaEntidades implements EntityFactory {

    @Spawns("jogador")
    public Entity spawnJogador(SpawnData data) {
        String name = data.get("name");
        int maxHealth = data.get("maxHealth");
        double moveSpeed = data.get("moveSpeed");

        // Corpo dinâmico: sofre gravidade e colide com as plataformas
        // (corpos estáticos). fixedRotation impede o player de "tombar"
        // ao encostar de lado numa parede/plataforma — sem isso, o
        // Box2D deixaria ele girar como se fosse uma caixa caindo de
        // ponta-cabeça. friction(0) evita que ele grude em paredes
        // verticais (fica livre pra escorregar, como member num
        // metroidvania de verdade).
        var physics = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        physics.setBodyDef(bodyDef);
        physics.setFixtureDef(new FixtureDef().friction(0f).density(1f));

        // Sensor fino colado nos pés (não é uma colisão de verdade, só
        // "sente" quando algo está embaixo) — é o que isOnGround() usa
        // em PlayerComponent.jump() pra saber se pode pular. Sem isso,
        // o jogo não tem como distinguir "no chão" de "no ar".
        double w = 32; // tamanho do sprite provisório player.png
        double h = 32;
        physics.addGroundSensor(new HitBox("GROUND_SENSOR", new Point2D(2, h - 1), BoundingShape.box(w - 4, 4)));

        return entityBuilder(data)
                .type(EntityType.JOGADOR)
                .viewWithBBox("player.png")
                .with(physics)
                .with(new PlayerComponent(name, maxHealth, moveSpeed))
                .build();
    }

    @Spawns("inimigo_voador")
    public Entity spawnInimigoVoador(SpawnData data) {
        String name = data.get("name");
        int maxHealth = data.get("maxHealth");
        double moveSpeed = data.get("moveSpeed");
        int damage = data.get("damage");
        double attackRange = data.get("attackRange");
        double detectionRange = data.get("detectionRange");

        return entityBuilder(data)
                .type(EntityType.INIMIGO_VOADOR)
                .viewWithBBox("enemy.png")
                .with(new FlyingEnemyComponent(name, maxHealth, moveSpeed, damage, attackRange, detectionRange))
                .build();
    }

    @Spawns("inimigo_ranged")
    public Entity spawnInimigoRanged(SpawnData data) {
        String name = data.get("name");
        int maxHealth = data.get("maxHealth");
        double moveSpeed = data.get("moveSpeed");
        int damage = data.get("damage");
        double attackRange = data.get("attackRange");
        double detectionRange = data.get("detectionRange");

        return entityBuilder(data)
                .type(EntityType.INIMIGO_RANGED)
                .viewWithBBox("enemy.png")
                .with(new RangedEnemyComponent(name, maxHealth, moveSpeed, damage, attackRange, detectionRange))
                .build();
    }

    @Spawns("projetil")
    public Entity spawnProjetil(SpawnData data) {
        Entity origin = data.get("origin");
        Entity target = data.get("target");
        int damage = data.get("damage");
        double speed = data.get("speed");

        return entityBuilder(data)
                .type(EntityType.PROJETIL)
                .viewWithBBox("projectile.png")
                .with(new ProjectileComponent(origin, target, damage, speed))
                .build();
    }

    // Plataforma: corpo ESTÁTICO (não cai, não se move — só existe pra
    // outros corpos colidirem com ela). Retângulo colorido por
    // enquanto, no lugar de um tileset de verdade.
    @Spawns("plataforma")
    public Entity spawnPlataforma(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        var physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(EntityType.PLATAFORMA)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(new Rectangle(width, height, Color.SADDLEBROWN))
                .with(physics)
                .build();
    }
}
