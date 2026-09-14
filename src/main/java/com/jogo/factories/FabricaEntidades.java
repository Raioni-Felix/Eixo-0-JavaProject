package com.jogo.factories;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.SensorCollisionHandler;
import com.almasb.fxgl.physics.box2d.dynamics.BodyDef;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import com.almasb.fxgl.physics.box2d.dynamics.Fixture;
import com.jogo.componentes.AtaqueJogadorComponent;
import com.jogo.componentes.EnemyComponent;
import com.jogo.componentes.FlyingEnemyComponent;
import com.jogo.componentes.PlayerComponent;
import com.jogo.componentes.RangedEnemyComponent;
import com.jogo.componentes.MeleeEnemyComponent;
import com.jogo.componentes.WeaponComponent;
import com.jogo.componentes.visual.ProjectileComponent;
import com.jogo.entidades.EntityType;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import com.almasb.fxgl.dsl.components.ExpireCleanComponent;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

/**
 * Fábrica central de entidades do jogo.
 *
 * Registrada uma vez em Main.initGame():
 *   getGameWorld().addEntityFactory(new FabricaEntidades());
 *
 * Cada @Spawns("nome") é chamado via FXGL.spawn("nome", spawnData),
 * onde spawnData carrega x/y (construtor) e os dados extras
 * (.put("chave", valor)). SpawnData.get(key) lança exceção se a chave
 * não existir, por isso todo spawn precisa vir com todos os .put()
 * necessários (não tem valor "default" escondido aqui).
 *
 * Jogador, inimigos e plataformas têm um PhysicsComponent (motor
 * Box2D do FXGL) pra gravidade e colisão de verdade, não mais
 * translate/gravidade manual na mão.
 *  - Jogador, ranged e melee (terrestres): corpo DYNAMIC, caem e
 *    colidem com plataformas.
 *  - Inimigo voador: também corpo DYNAMIC, mas com gravityScale(0),
 *    assim ele tem colisão de verdade (não atravessa mais plataforma)
 *    mas não sofre o puxão da gravidade.
 *  - Plataforma: corpo STATIC, nunca se move, só serve de chão.
 *
 * .collidable() no jogador e nos 3 tipos de inimigo é o que ligava o
 * sistema de CollisionHandler do FXGL pra eles.
 *
 * O player nunca pode ficar em pé em cima de um inimigo (plataformar),
 * mas o empurrão/dano por toque tem que disparar em todo contato.
 * Solução: cada inimigo tem dois fixtures (ver setupContatoComPlayer):
 * o sólido de sempre, com o player tirado do maskBits (não colide mais
 * com ele, então nunca serve de chão), e um sensor extra do mesmo
 * tamanho, sem filtro, que continua vendo o player e chama
 * PlayerComponent.hitBy() a cada frame de contato.
 */
public class FabricaEntidades implements EntityFactory {

    //Bit de categoria exclusivo do player (Box2D Filter.categoryBits/
    //maskBits, ambos int). Todo fixture sólido de inimigo tira esse
    //bit do maskBits (ver setupContatoComPlayer) e para de colidir com
    //o player fisicamente.
    private static final int CATEGORY_PLAYER = 0x0002;

    @Spawns("jogador")
    public Entity spawnJogador(SpawnData data) {
        String name = data.get("name");
        int maxHealth = data.get("maxHealth");
        double moveSpeed = data.get("moveSpeed");

        //Corpo dinâmico, sofre gravidade e colide com as plataformas
        //(corpos estáticos). fixedRotation impede o player de tombar
        //ao encostar de lado numa parede/plataforma, senão o Box2D
        //deixaria ele girar tipo caixa caindo de ponta cabeça.
        //friction(0) evita que ele grude em parede vertical, fica
        //livre pra escorregar, como num metroidvania de verdade.
        var physics = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        physics.setBodyDef(bodyDef);
        physics.setFixtureDef(new FixtureDef().friction(0f).density(1f));

        //Sensor fino colado nos pés, não é colisão de verdade, só
        //sente quando tem algo embaixo. É o que isOnGround() usa em
        //PlayerComponent.jump() pra saber se pode pular.
        double w = 32; // tamanho do sprite provisório player.png
        double h = 32;
        physics.addGroundSensor(new HitBox("GROUND_SENSOR", new Point2D(2, h - 1), BoundingShape.box(w - 4, 4)));

        //Marca todos os fixtures do player com CATEGORY_PLAYER. É essa
        //marca que cada inimigo usa pra excluir o player do maskBits
        //do seu fixture sólido (ver setupContatoComPlayer).
        physics.setOnPhysicsInitialized(() -> {
            for (Fixture fixture : physics.getBody().getFixtures()) {
                var filter = fixture.getFilterData();
                filter.categoryBits = CATEGORY_PLAYER;
                fixture.setFilterData(filter);
            }
        });

        return entityBuilder(data)
                .type(EntityType.JOGADOR)
                .viewWithBBox("player.png")
                .collidable()
                .with(physics)
                .with(new PlayerComponent(name, maxHealth, moveSpeed))
                .with(new WeaponComponent())
                .build();
    }
    // Construtor da Hitbox do Ataque Corpo a Corpo
    @Spawns("ataque_jogador")
    public Entity spawnAtaqueJogador(SpawnData data) {
        int damage = data.get("damage");
        double size = data.get("size"); // vem do Weapon.getRange(), ver WeaponComponent

        // jogador/offsetX/offsetY vêm de WeaponComponent.performMeleeAttack()
        // e alimentam AtaqueJogadorComponent, que gruda essa hitbox no
        // player todo frame (ver comentário lá) — sem isso ela nascia
        // numa posição fixa e ficava pra trás se o player se mexesse
        // durante o golpe.
        Entity jogador = data.get("jogador");
        double offsetX = data.get("offsetX");
        double offsetY = data.get("offsetY");

        var physics = new PhysicsComponent();
        // KINEMATIC: move-se livremente mas não sofre influência da gravidade
        physics.setBodyType(BodyType.KINEMATIC); 
        
        // Sensor invisível do tamanho do alcance da arma equipada
        physics.addSensor(new HitBox("MELEE_HIT", BoundingShape.box(size, size)), new SensorCollisionHandler() {
            @Override
            protected void onCollisionBegin(Entity other) {
                EnemyComponent enemy = EnemyComponent.getFrom(other);
                if (enemy != null) {
                    enemy.takeDamage(damage); 
                }
            }
        });

        return entityBuilder(data)
                .type(EntityType.ATAQUE_JOGADOR)
                .view(new Rectangle(size, size, Color.rgb(255, 255, 255, 0.6)))
                .with(physics)
                .with(new AtaqueJogadorComponent(jogador, offsetX, offsetY))
                // Remove a caixa de colisão do jogo automaticamente após 0.15 segundos
                .with(new ExpireCleanComponent(Duration.seconds(0.15)))
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

        //DYNAMIC + gravityScale(0), tem colisão de verdade (é sólido,
        //não atravessa plataforma) mas ignora a gravidade do mundo,
        //continua voando livre só que agora esbarra em coisa de
        //verdade. fixedRotation pelo mesmo motivo de sempre.
        var physics = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        bodyDef.setGravityScale(0f);
        physics.setBodyDef(bodyDef);
        physics.setFixtureDef(new FixtureDef().friction(0f).density(1f));

        //Cor própria pra cada tipo de inimigo, pra dar pra distinguir
        //de longe. Roxo pro voador.
        double enemySize = 32;
        Entity enemy = entityBuilder(data)
                .type(EntityType.INIMIGO_VOADOR)
                .bbox(new HitBox(BoundingShape.box(enemySize, enemySize)))
                .view(new Rectangle(enemySize, enemySize, Color.MEDIUMPURPLE))
                .collidable()
                .with(physics)
                .with(new FlyingEnemyComponent(name, maxHealth, moveSpeed, damage, attackRange, detectionRange))
                .build();

        setupContatoComPlayer(physics, enemy, enemySize);
        return enemy;
    }

    @Spawns("inimigo_ranged")
    public Entity spawnInimigoRanged(SpawnData data) {
        String name = data.get("name");
        int maxHealth = data.get("maxHealth");
        double moveSpeed = data.get("moveSpeed");
        int damage = data.get("damage");
        double attackRange = data.get("attackRange");
        double detectionRange = data.get("detectionRange");

        //DYNAMIC igual o player, sofre gravidade, cai e pisa nas
        //plataformas. fixedRotation pelo mesmo motivo do player.
        var physics = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        physics.setBodyDef(bodyDef);
        physics.setFixtureDef(new FixtureDef().friction(0.2f).density(1f));

        //Laranja pro ranged.
        double enemySize = 32;
        Entity enemy = entityBuilder(data)
                .type(EntityType.INIMIGO_RANGED)
                .bbox(new HitBox(BoundingShape.box(enemySize, enemySize)))
                .view(new Rectangle(enemySize, enemySize, Color.DARKORANGE))
                .collidable()
                .with(physics)
                .with(new RangedEnemyComponent(name, maxHealth, moveSpeed, damage, attackRange, detectionRange))
                .build();

        setupContatoComPlayer(physics, enemy, enemySize);
        return enemy;
    }

    @Spawns("inimigo_melee")
    public Entity spawnInimigoMelee(SpawnData data) {
        String name = data.get("name");
        int maxHealth = data.get("maxHealth");
        double moveSpeed = data.get("moveSpeed");
        int damage = data.get("damage");
        double attackRange = data.get("attackRange");
        double detectionRange = data.get("detectionRange");

        //Mesma física do ranged: DYNAMIC (cai, pisa em plataforma) +
        //fixedRotation (não tomba ao encostar do lado em algo).
        var physics = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        physics.setBodyDef(bodyDef);
        physics.setFixtureDef(new FixtureDef().friction(0.2f).density(1f));

        //Vermelho (mais escuro que o CRIMSON da barra de HP baixa, pra
        //não confundir) pro melee.
        double enemySize = 32;
        Entity enemy = entityBuilder(data)
                .type(EntityType.INIMIGO_MELEE)
                .bbox(new HitBox(BoundingShape.box(enemySize, enemySize)))
                .view(new Rectangle(enemySize, enemySize, Color.FIREBRICK))
                .collidable()
                .with(physics)
                .with(new MeleeEnemyComponent(name, maxHealth, moveSpeed, damage, attackRange, detectionRange))
                .build();

        setupContatoComPlayer(physics, enemy, enemySize);
        return enemy;
    }

    //Configura, pra qualquer inimigo, o esquema de dois fixtures do
    //comentário lá em cima da classe: tira o player do maskBits do
    //fixture sólido e adiciona um sensor extra (mesmo tamanho) que
    //chama hitBy() no PlayerComponent a cada frame de contato.
    private void setupContatoComPlayer(PhysicsComponent physics, Entity enemyEntity, double enemySize) {
        physics.setOnPhysicsInitialized(() -> {
            //Pula fixture.isSensor(), só mexe no fixture sólido. Sem
            //isso o sensor "PLAYER_CONTACT" logo abaixo também ficava
            //cego pro player, e o knockback parava de disparar.
            for (Fixture fixture : physics.getBody().getFixtures()) {
                if (fixture.isSensor()) {
                    continue;
                }

                var filter = fixture.getFilterData();
                filter.maskBits = filter.maskBits & ~CATEGORY_PLAYER;
                fixture.setFilterData(filter);
            }
        });

        physics.addSensor(new HitBox("PLAYER_CONTACT", BoundingShape.box(enemySize, enemySize)), new SensorCollisionHandler() {
            //onCollisionBegin e onCollision chamam a mesma coisa, o
            //dano/piscada depende só de isInvincible() lá no hitBy(),
            //não de qual evento disparou.
            @Override
            protected void onCollisionBegin(Entity other) {
                handleContato(other);
            }

            @Override
            protected void onCollision(Entity other) {
                handleContato(other);
            }

            @Override
            protected void onCollisionEnd(Entity other) {
                //Nada a fazer, dano/empurrão não têm estado contínuo
                //pra desligar, só disparam nos ticks de contato.
            }

            private void handleContato(Entity other) {
                if (!other.hasComponent(PlayerComponent.class)) {
                    return;
                }

                EnemyComponent enemy = EnemyComponent.getFrom(enemyEntity);
                if (enemy == null) {
                    return;
                }

                other.getComponent(PlayerComponent.class).hitBy(enemyEntity, enemy.getDamage());
            }
        });
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

    //Plataforma: corpo estático, não cai nem se move, só existe pra
    //outros corpos colidirem com ela. Retângulo colorido por
    //enquanto, no lugar de um tileset de verdade.
    @Spawns("plataforma")
    public Entity spawnPlataforma(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        var physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(EntityType.PLATAFORMA)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(new Rectangle(width, height, Color.BLACK))
                .with(physics)
                .build();
    }

    //Parede invisível: mesma física da plataforma (corpo estático,
    //nunca se move), mas sem view nenhuma. Serve só de limite do
    //nível (bordas esquerda/direita), pra impedir o player de andar
    //pra fora da área do chão e cair num vazio atrás do cenário. Usa
    //o mesmo EntityType.PLATAFORMA, ninguém trata "parede" de forma
    //especial, ela só existe pra colidir fisicamente igual qualquer
    //plataforma.
    @Spawns("parede")
    public Entity spawnParede(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        var physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(EntityType.PLATAFORMA)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .with(physics)
                .build();
    }

    //Gatilho de passagem entre salas: zona sem PhysicsComponent (só
    //bounding box + collidable(), igual o player/inimigos), detectada
    //pelo CollisionHandler(JOGADOR, GATILHO) registrado uma vez em
    //Main.initPhysics(). targetIndex/entrySide guardam pra qual sala
    //(índice em Main.SALAS_DO_MAPA) e por qual lado o player deve
    //reaparecer (ver Main.iniciarTransicaoDeSala()/trocarSala()).
    //Sem view nenhuma (igual a parede) — é só uma zona invisível, sem
    //indicador visual chamando atenção pro "fim da sala".
    @Spawns("gatilho")
    public Entity spawnGatilho(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");
        int targetIndex = data.get("targetIndex");
        String entrySide = data.get("entrySide");

        Entity gatilho = entityBuilder(data)
                .type(EntityType.GATILHO)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .collidable()
                .build();

        //Guarda os dois dados extras no próprio Entity (não só no
        //SpawnData, que não é acessível depois de construído), pra o
        //CollisionHandler em Main.initPhysics() conseguir ler de volta
        //no momento da colisão.
        gatilho.setProperty("targetIndex", targetIndex);
        gatilho.setProperty("entrySide", entrySide);

        return gatilho;
    }
}