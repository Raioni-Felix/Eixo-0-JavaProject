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
import com.jogo.componentes.ComponenteInimigo;
import com.jogo.componentes.ComponenteInimigoVoador;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.ComponenteInimigoDistancia;
import com.jogo.componentes.ComponenteInimigoMelee;
import com.jogo.componentes.ComponenteArma;
import com.jogo.componentes.ComponenteDisparoJogador;
import com.jogo.componentes.visual.ComponenteProjetil;
import com.jogo.entidades.TipoEntidade;
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
 * As chaves usadas em .put()/.get() (ex.: "nome", "dano") são um
 * contrato interno nosso (Main/ComponenteArma/ComponenteInimigoDistancia
 * escrevem, FabricaEntidades lê) — não têm relação com as chaves do
 * JSON dos níveis (essas continuam em inglês, ver os "Dados*" em
 * com.jogo.niveis com @SerializedName), por isso podem ficar em
 * português livremente. A ÚNICA exceção é a string do próprio
 * @Spawns("inimigo_" + tipo): esse "tipo" vem direto do JSON
 * ("voador"/"ranged"/"melee") e não foi traduzido, então os nomes dos
 * métodos @Spawns("inimigo_ranged")/("inimigo_melee")/("inimigo_voador")
 * continuam com essas strings originais de propósito.
 *
 * Jogador, inimigos e plataformas têm um PhysicsComponent (motor
 * Box2D do FXGL) pra gravidade e colisão de verdade, não mais
 * translate/gravidade manual na mão.
 *  - Jogador, atirador e melee (terrestres): corpo DYNAMIC, caem e
 *    colidem com plataformas.
 *  - Inimigo voador: também corpo DYNAMIC, mas com gravityScale(0),
 *    assim ele tem colisão de verdade (não atravessa mais plataforma)
 *    mas não sofre o puxão da gravidade.
 *  - Plataforma: corpo STATIC, nunca se move, só serve de chão.
 *
 * .collidable() no jogador e nos 3 tipos de inimigo é o que ligava o
 * sistema de CollisionHandler do FXGL pra eles.
 *
 * O jogador nunca pode ficar em pé em cima de um inimigo (plataformar),
 * mas o empurrão/dano por toque tem que disparar em todo contato.
 * Solução: cada inimigo tem dois fixtures (ver configurarContatoComJogador):
 * o sólido de sempre, com o jogador tirado do maskBits (não colide mais
 * com ele, então nunca serve de chão), e um sensor extra do mesmo
 * tamanho, sem filtro, que continua vendo o jogador e chama
 * ComponenteJogador.serAtingido() a cada frame de contato.
 */
public class FabricaEntidades implements EntityFactory {

    //Bit de categoria exclusivo do jogador (Box2D Filter.categoryBits/
    //maskBits, ambos int). Todo fixture sólido de inimigo tira esse
    //bit do maskBits (ver configurarContatoComJogador) e para de
    //colidir com o jogador fisicamente.
    private static final int CATEGORY_PLAYER = 0x0002;

    @Spawns("jogador")
    public Entity spawnJogador(SpawnData data) {
        String nome = data.get("nome");
        int vidaMaxima = data.get("vidaMaxima");
        double velocidade = data.get("velocidade");
        int energiaMaxima = data.get("energiaMaxima");

        //Corpo dinâmico, sofre gravidade e colide com as plataformas
        //(corpos estáticos). fixedRotation impede o jogador de tombar
        //ao encostar de lado numa parede/plataforma, senão o Box2D
        //deixaria ele girar tipo caixa caindo de ponta cabeça.
        //friction(0) evita que ele grude em parede vertical, fica
        //livre pra escorregar, como num metroidvania de verdade.
        var fisica = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        fisica.setBodyDef(bodyDef);
        fisica.setFixtureDef(new FixtureDef().friction(0f).density(1f));

        //Sensor fino colado nos pés, não é colisão de verdade, só
        //sente quando tem algo embaixo. É o que isOnGround() usa em
        //ComponenteJogador.pular() pra saber se pode pular.
        double w = 32; // tamanho do sprite provisório player.png
        double h = 32;
        fisica.addGroundSensor(new HitBox("GROUND_SENSOR", new Point2D(2, h - 1), BoundingShape.box(w - 4, 4)));

        //Marca todos os fixtures do jogador com CATEGORY_PLAYER. É essa
        //marca que cada inimigo usa pra excluir o jogador do maskBits
        //do seu fixture sólido (ver configurarContatoComJogador).
        fisica.setOnPhysicsInitialized(() -> {
            for (Fixture fixture : fisica.getBody().getFixtures()) {
                var filtro = fixture.getFilterData();
                filtro.categoryBits = CATEGORY_PLAYER;
                fixture.setFilterData(filtro);
            }
        });

        return entityBuilder(data)
                .type(TipoEntidade.JOGADOR)
                .viewWithBBox("player.png")
                .collidable()
                .with(fisica)
                .with(new ComponenteJogador(nome, vidaMaxima, velocidade, energiaMaxima))
                .with(new ComponenteArma())
                .build();
    }
    // Construtor da Hitbox do Ataque Corpo a Corpo
    @Spawns("ataque_jogador")
    public Entity spawnAtaqueJogador(SpawnData data) {
        int dano = data.get("dano");
        double tamanho = data.get("tamanho"); // vem de Arma.getAlcance(), ver Espada

        // jogador/deslocamentoX/deslocamentoY vêm de
        // Espada.executarAtaque() e alimentam
        // AtaqueJogadorComponent, que gruda essa hitbox no jogador
        // todo frame (ver comentário lá) — sem isso ela nascia numa
        // posição fixa e ficava pra trás se o jogador se mexesse
        // durante o golpe.
        Entity jogador = data.get("jogador");
        double deslocamentoX = data.get("deslocamentoX");
        double deslocamentoY = data.get("deslocamentoY");

        var fisica = new PhysicsComponent();
        // KINEMATIC: move-se livremente mas não sofre influência da gravidade
        fisica.setBodyType(BodyType.KINEMATIC);

        // Sensor invisível do tamanho do alcance da arma equipada
        fisica.addSensor(new HitBox("MELEE_HIT", BoundingShape.box(tamanho, tamanho)), new SensorCollisionHandler() {
            @Override
            protected void onCollisionBegin(Entity other) {
                ComponenteInimigo inimigo = ComponenteInimigo.obterDe(other);
                if (inimigo != null) {
                    inimigo.tomarDano(dano);
                }
            }
        });

        return entityBuilder(data)
                .type(TipoEntidade.ATAQUE_JOGADOR)
                .view(new Rectangle(tamanho, tamanho, Color.rgb(255, 255, 255, 0.6)))
                .with(fisica)
                .with(new AtaqueJogadorComponent(jogador, deslocamentoX, deslocamentoY))
                // Remove a caixa de colisão do jogo automaticamente após 0.15 segundos
                .with(new ExpireCleanComponent(Duration.seconds(0.15)))
                .build();
    }

    @Spawns("inimigo_voador")
    public Entity spawnInimigoVoador(SpawnData data) {
        String nome = data.get("nome");
        int vidaMaxima = data.get("vidaMaxima");
        double velocidade = data.get("velocidade");
        int dano = data.get("dano");
        double alcanceAtaque = data.get("alcanceAtaque");
        double alcanceDeteccao = data.get("alcanceDeteccao");

        //DYNAMIC + gravityScale(0), tem colisão de verdade (é sólido,
        //não atravessa plataforma) mas ignora a gravidade do mundo,
        //continua voando livre só que agora esbarra em coisa de
        //verdade. fixedRotation pelo mesmo motivo de sempre.
        var fisica = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        bodyDef.setGravityScale(0f);
        fisica.setBodyDef(bodyDef);
        fisica.setFixtureDef(new FixtureDef().friction(0f).density(1f));

        //Cor própria pra cada tipo de inimigo, pra dar pra distinguir
        //de longe. Roxo pro voador.
        double tamanhoInimigo = 32;
        Entity inimigo = entityBuilder(data)
                .type(TipoEntidade.INIMIGO_VOADOR)
                .bbox(new HitBox(BoundingShape.box(tamanhoInimigo, tamanhoInimigo)))
                .view(new Rectangle(tamanhoInimigo, tamanhoInimigo, Color.MEDIUMPURPLE))
                .collidable()
                .with(fisica)
                .with(new ComponenteInimigoVoador(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao))
                .build();

        configurarContatoComJogador(fisica, inimigo, tamanhoInimigo);
        return inimigo;
    }

    @Spawns("inimigo_ranged")
    public Entity spawnInimigoDistancia(SpawnData data) {
        String nome = data.get("nome");
        int vidaMaxima = data.get("vidaMaxima");
        double velocidade = data.get("velocidade");
        int dano = data.get("dano");
        double alcanceAtaque = data.get("alcanceAtaque");
        double alcanceDeteccao = data.get("alcanceDeteccao");

        //DYNAMIC igual o jogador, sofre gravidade, cai e pisa nas
        //plataformas. fixedRotation pelo mesmo motivo do jogador.
        var fisica = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        fisica.setBodyDef(bodyDef);
        fisica.setFixtureDef(new FixtureDef().friction(0.2f).density(1f));

        //Laranja pro atirador.
        double tamanhoInimigo = 32;
        Entity inimigo = entityBuilder(data)
                .type(TipoEntidade.INIMIGO_DISTANCIA)
                .bbox(new HitBox(BoundingShape.box(tamanhoInimigo, tamanhoInimigo)))
                .view(new Rectangle(tamanhoInimigo, tamanhoInimigo, Color.DARKORANGE))
                .collidable()
                .with(fisica)
                .with(new ComponenteInimigoDistancia(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao))
                .build();

        configurarContatoComJogador(fisica, inimigo, tamanhoInimigo);
        return inimigo;
    }

    @Spawns("inimigo_melee")
    public Entity spawnInimigoMelee(SpawnData data) {
        String nome = data.get("nome");
        int vidaMaxima = data.get("vidaMaxima");
        double velocidade = data.get("velocidade");
        int dano = data.get("dano");
        double alcanceAtaque = data.get("alcanceAtaque");
        double alcanceDeteccao = data.get("alcanceDeteccao");

        //Mesma física do atirador: DYNAMIC (cai, pisa em plataforma) +
        //fixedRotation (não tomba ao encostar do lado em algo).
        var fisica = new PhysicsComponent();

        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        fisica.setBodyDef(bodyDef);
        fisica.setFixtureDef(new FixtureDef().friction(0.2f).density(1f));

        //Vermelho (mais escuro que o CRIMSON da barra de HP baixa, pra
        //não confundir) pro melee.
        double tamanhoInimigo = 32;
        Entity inimigo = entityBuilder(data)
                .type(TipoEntidade.INIMIGO_MELEE)
                .bbox(new HitBox(BoundingShape.box(tamanhoInimigo, tamanhoInimigo)))
                .view(new Rectangle(tamanhoInimigo, tamanhoInimigo, Color.FIREBRICK))
                .collidable()
                .with(fisica)
                .with(new ComponenteInimigoMelee(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao))
                .build();

        configurarContatoComJogador(fisica, inimigo, tamanhoInimigo);
        return inimigo;
    }

    //Configura, pra qualquer inimigo, o esquema de dois fixtures do
    //comentário lá em cima da classe: tira o jogador do maskBits do
    //fixture sólido e adiciona um sensor extra (mesmo tamanho) que
    //chama serAtingido() no ComponenteJogador a cada frame de contato.
    private void configurarContatoComJogador(PhysicsComponent fisica, Entity entidadeInimigo, double tamanhoInimigo) {
        fisica.setOnPhysicsInitialized(() -> {
            //Pula fixture.isSensor(), só mexe no fixture sólido. Sem
            //isso o sensor "PLAYER_CONTACT" logo abaixo também ficava
            //cego pro jogador, e o knockback parava de disparar.
            for (Fixture fixture : fisica.getBody().getFixtures()) {
                if (fixture.isSensor()) {
                    continue;
                }

                var filtro = fixture.getFilterData();
                filtro.maskBits = filtro.maskBits & ~CATEGORY_PLAYER;
                fixture.setFilterData(filtro);
            }
        });

        fisica.addSensor(new HitBox("PLAYER_CONTACT", BoundingShape.box(tamanhoInimigo, tamanhoInimigo)), new SensorCollisionHandler() {
            //onCollisionBegin e onCollision chamam a mesma coisa, o
            //dano/piscada depende só de estaInvencivel() lá no
            //serAtingido(), não de qual evento disparou.
            @Override
            protected void onCollisionBegin(Entity other) {
                tratarContato(other);
            }

            @Override
            protected void onCollision(Entity other) {
                tratarContato(other);
            }

            @Override
            protected void onCollisionEnd(Entity other) {
                //Nada a fazer, dano/empurrão não têm estado contínuo
                //pra desligar, só disparam nos ticks de contato.
            }

            private void tratarContato(Entity other) {
                if (!other.hasComponent(ComponenteJogador.class)) {
                    return;
                }

                ComponenteInimigo inimigo = ComponenteInimigo.obterDe(entidadeInimigo);
                if (inimigo == null) {
                    return;
                }

                other.getComponent(ComponenteJogador.class).serAtingido(entidadeInimigo, inimigo.getDano());
            }
        });
    }

    @Spawns("projetil")
    public Entity spawnProjetil(SpawnData data) {
        Entity origem = data.get("origem");
        Entity alvo = data.get("alvo");
        int dano = data.get("dano");
        double velocidade = data.get("velocidade");

        return entityBuilder(data)
                .type(TipoEntidade.PROJETIL)
                .viewWithBBox("projectile.png")
                .with(new ComponenteProjetil(origem, alvo, dano, velocidade))
                .build();
    }

    //Projétil do jogador: tiro das armas de fogo (ArmaDeFogo e filhas)
    //e disparo do GolpeDeEnergia. Quem cria escolhe tudo pelo SpawnData
    //(direção, velocidade, alcance via tempoVida, quantos inimigos
    //atravessa, tamanho e cor). Sem PhysicsComponent: não cai nem
    //empurra nada, só anda reto e confere colisão pela bounding box
    //(ver ComponenteDisparoJogador).
    @Spawns("disparo_jogador")
    public Entity spawnDisparoJogador(SpawnData data) {
        int dano = data.get("dano");
        double direcaoX = data.get("direcaoX");
        double direcaoY = data.get("direcaoY");
        double velocidade = data.get("velocidade");
        double tempoVida = data.get("tempoVida");
        int alvosMaximos = data.get("alvosMaximos");
        double tamanho = data.get("tamanho");
        Color cor = data.get("cor");

        return entityBuilder(data)
                .type(TipoEntidade.DISPARO_JOGADOR)
                .bbox(new HitBox(BoundingShape.box(tamanho, tamanho)))
                .view(new Rectangle(tamanho, tamanho, cor))
                .with(new ComponenteDisparoJogador(dano, direcaoX, direcaoY, velocidade, tempoVida, alvosMaximos))
                .build();
    }

    //Módulo de cura ou de energia parado no mapa (lista "items" do JSON
    //da sala, ver Main.spawnItensDaSala()). Igual ao gatilho: sem
    //física, só bounding box + collidable(), e quem trata o toque é o
    //CollisionHandler(JOGADOR, ITEM) em Main.initPhysics(). Verde =
    //cura, azul = energia.
    @Spawns("item")
    public Entity spawnItem(SpawnData data) {
        String tipoItem = data.get("tipoItem"); //"cura" ou "energia"
        int quantidade = data.get("quantidade");
        String idColeta = data.get("idColeta");

        double tamanho = 16;
        Color cor = "energia".equals(tipoItem) ? Color.DEEPSKYBLUE : Color.LIMEGREEN;

        Rectangle visual = new Rectangle(tamanho, tamanho, cor);
        visual.setStroke(Color.WHITE);
        visual.setStrokeWidth(2);

        Entity item = entityBuilder(data)
                .type(TipoEntidade.ITEM)
                .bbox(new HitBox(BoundingShape.box(tamanho, tamanho)))
                .view(visual)
                .collidable()
                .build();

        //Mesmo truque do gatilho: guarda no Entity pra o
        //CollisionHandler ler na hora do toque.
        item.setProperty("tipoItem", tipoItem);
        item.setProperty("quantidade", quantidade);
        item.setProperty("idColeta", idColeta);
        return item;
    }

    //Plataforma: corpo estático, não cai nem se move, só existe pra
    //outros corpos colidirem com ela. Retângulo colorido por
    //enquanto, no lugar de um tileset de verdade.
    @Spawns("plataforma")
    public Entity spawnPlataforma(SpawnData data) {
        double largura = data.get("largura");
        double altura = data.get("altura");

        var fisica = new PhysicsComponent();
        fisica.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(TipoEntidade.PLATAFORMA)
                .bbox(new HitBox(BoundingShape.box(largura, altura)))
                .view(new Rectangle(largura, altura, Color.BLACK))
                .with(fisica)
                .build();
    }

    //Parede invisível: mesma física da plataforma (corpo estático,
    //nunca se move), mas sem view nenhuma. Serve só de limite do
    //nível (bordas esquerda/direita), pra impedir o jogador de andar
    //pra fora da área do chão e cair num vazio atrás do cenário. Usa
    //o mesmo TipoEntidade.PLATAFORMA, ninguém trata "parede" de forma
    //especial, ela só existe pra colidir fisicamente igual qualquer
    //plataforma.
    @Spawns("parede")
    public Entity spawnParede(SpawnData data) {
        double largura = data.get("largura");
        double altura = data.get("altura");

        var fisica = new PhysicsComponent();
        fisica.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(TipoEntidade.PLATAFORMA)
                .bbox(new HitBox(BoundingShape.box(largura, altura)))
                .with(fisica)
                .build();
    }

    //Gatilho de passagem entre salas: zona sem PhysicsComponent (só
    //bounding box + collidable(), igual o jogador/inimigos), detectada
    //pelo CollisionHandler(JOGADOR, GATILHO) registrado uma vez em
    //Main.initPhysics(). indiceDestino/ladoEntrada guardam pra qual
    //sala (índice em Main.SALAS_DO_MAPA) e por qual lado o jogador
    //deve reaparecer (ver Main.iniciarTransicaoDeSala()/trocarSala()).
    //Sem view nenhuma (igual a parede) — é só uma zona invisível, sem
    //indicador visual chamando atenção pro "fim da sala".
    @Spawns("gatilho")
    public Entity spawnGatilho(SpawnData data) {
        double largura = data.get("largura");
        double altura = data.get("altura");
        int indiceDestino = data.get("indiceDestino");
        String ladoEntrada = data.get("ladoEntrada");

        Entity gatilho = entityBuilder(data)
                .type(TipoEntidade.GATILHO)
                .bbox(new HitBox(BoundingShape.box(largura, altura)))
                .collidable()
                .build();

        //Guarda os dois dados extras no próprio Entity (não só no
        //SpawnData, que não é acessível depois de construído), pra o
        //CollisionHandler em Main.initPhysics() conseguir ler de volta
        //no momento da colisão.
        gatilho.setProperty("indiceDestino", indiceDestino);
        gatilho.setProperty("ladoEntrada", ladoEntrada);

        return gatilho;
    }
}
