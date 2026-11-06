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
import com.jogo.componentes.ComponenteBuracoNegro;
import com.jogo.componentes.ComponenteChefe;
import com.jogo.componentes.ComponenteInimigo;
import com.jogo.componentes.ComponenteInimigoVoador;
import com.jogo.componentes.ComponenteJogador;
import com.jogo.componentes.ComponenteInimigoDistancia;
import com.jogo.componentes.ComponenteInimigoMelee;
import com.jogo.componentes.ComponenteArma;
import com.jogo.componentes.ComponenteDisparoJogador;
import com.jogo.componentes.visual.ComponenteAnimado;
import com.jogo.componentes.visual.ComponenteAnimado.EstadoAnimacao;
import com.jogo.componentes.visual.ComponenteProjetil;
import com.jogo.componentes.visual.Sprites;
import com.almasb.fxgl.texture.AnimatedTexture;
import java.util.Map;
import com.jogo.entidades.TipoEntidade;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.scene.shape.Rectangle;
import com.almasb.fxgl.dsl.components.ExpireCleanComponent;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

/**
 * Fábrica central de entidades do jogo.
 *
 * Registrada uma vez em controle.ControleJogo.iniciarMundo():
 *   getGameWorld().addEntityFactory(new FabricaEntidades());
 *
 * Cada @Spawns("nome") é chamado via FXGL.spawn("nome", spawnData),
 * onde spawnData carrega x/y (construtor) e os dados extras
 * (.put("chave", valor)). SpawnData.get(key) lança exceção se a chave
 * não existir, por isso todo spawn precisa vir com todos os .put()
 * necessários (não tem valor "default" escondido aqui).
 *
 * As chaves usadas em .put()/.get() (ex.: "nome", "dano") são um
 * contrato interno nosso (controle/ComponenteArma/ComponenteInimigoDistancia
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

    //Os sprites são pixel art pequeno (a personagem tem ~50 px de altura
    //no arquivo); são carregados 1,5x maiores (ver Sprites). As caixas
    //de colisão abaixo já estão nesse tamanho.
    private static final double ESCALA_SPRITES = 1.5;

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
        //Caixa de colisão do corpo da personagem (o quadro do sprite é
        //71x67, com sobra transparente; ver ComponenteAnimado)
        double w = 36;
        double h = 72;
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

        var jogador = new ComponenteJogador(nome, vidaMaxima, velocidade, energiaMaxima);

        var arma = new ComponenteArma();

        //Um conjunto de animações por empunhadura (Arma.getEmpunhadura()):
        //trocar de arma troca os sprites. Ver sprites/CREDITOS.txt
        Map<String, Map<EstadoAnimacao, com.almasb.fxgl.texture.AnimationChannel>> conjuntos = Map.of(
                "espada", animacoesJogadora("espada", 6, 2, 5, 6, 3),
                "adagas", animacoesJogadora("adagas", 6, 3, 4, 6, 3),
                "arma", animacoesJogadora("arma", 6, 2, 3, 6, 3));
        var animacao = new ComponenteAnimado(conjuntos,
                () -> arma.getArmaEquipada() == null ? "espada" : arma.getArmaEquipada().getEmpunhadura(),
                130, 65, 80, ESCALA_SPRITES,
                () -> ComponenteAnimado.escolherEstadoJogadora(jogador.estaNoDash(), jogador.estaUsandoMagia(),
                        jogador.estaAtordoado(), jogador.estaAtacando(), jogador.estaNoChao(),
                        jogador.getVelocidadeX(), jogador.getVelocidadeY()),
                jogador::estaOlhandoDireita);

        //A animação entra antes da arma, pra arma ficar desenhada por cima
        return entityBuilder(data)
                .type(TipoEntidade.JOGADOR)
                .bbox(new HitBox(BoundingShape.box(w, h)))
                .collidable()
                .with(fisica)
                .with(jogador)
                .with(animacao)
                .with(arma)
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

        //Drone do Warped City. Só tem a animação de voo (vale pra parado
        //e andando) e a piscada branca de dano.
        double largura = 48, altura = 48;
        var voador = new ComponenteInimigoVoador(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao);
        var voo = Sprites.animacao("drone", "voar", 4, 0.4, ESCALA_SPRITES);
        Entity inimigo = entityBuilder(data)
                .type(TipoEntidade.INIMIGO_VOADOR)
                .bbox(new HitBox(BoundingShape.box(largura, altura)))
                .collidable()
                .with(fisica)
                .with(voador)
                .with(animacaoInimigo(voador, Map.of(
                        EstadoAnimacao.PARADO, voo,
                        EstadoAnimacao.ANDANDO, voo,
                        EstadoAnimacao.MACHUCADO, Sprites.animacao("drone", "dano", 1, 0.2, ESCALA_SPRITES)),
                        55, 29, 43)) //sem pé: 43 deixa o corpo no meio da caixa
                .build();

        configurarContatoComJogador(fisica, inimigo, largura, altura);
        return inimigo;
    }

    //Chefe da última sala (tipo "chefe" no JSON). Voa como o drone
    //(DYNAMIC sem gravidade), é bem maior e tem as animações em dois
    //conjuntos: "normal" e "furia" (fase 2, com metade da vida)
    @Spawns("inimigo_chefe")
    public Entity spawnChefe(SpawnData data) {
        String nome = data.get("nome");
        int vidaMaxima = data.get("vidaMaxima");
        double velocidade = data.get("velocidade");
        int dano = data.get("dano");
        double larguraSala = data.get("larguraSala");

        var fisica = new PhysicsComponent();
        var bodyDef = new BodyDef();
        bodyDef.setType(BodyType.DYNAMIC);
        bodyDef.setFixedRotation(true);
        bodyDef.setGravityScale(0f);
        fisica.setBodyDef(bodyDef);
        fisica.setFixtureDef(new FixtureDef().friction(0f).density(1f));

        double lado = 110; //caixa do corpo; o sprite (96 x 1,5 = 144) é um pouco maior
        var chefe = new ComponenteChefe(nome, vidaMaxima, velocidade, dano, larguraSala);
        Map<String, Map<EstadoAnimacao, com.almasb.fxgl.texture.AnimationChannel>> conjuntos = Map.of(
                "normal", animacoesChefe("normal"),
                "furia", animacoesChefe("furia"));
        var animacao = new ComponenteAnimado(conjuntos,
                () -> chefe.getFase() == 1 ? "normal" : "furia",
                //quadro de 96 px; centro do sprite no centro da caixa
                96, 48, (lado / 2 + 72) / ESCALA_SPRITES, ESCALA_SPRITES,
                () -> ComponenteAnimado.escolherEstado(chefe.estaMachucado(), chefe.estaAtacando(),
                        true, 0, 0),
                () -> true)
                .comCongelamento(chefe::estaCongelado);

        Entity entidade = entityBuilder(data)
                .type(TipoEntidade.CHEFE)
                .bbox(new HitBox(BoundingShape.box(lado, lado)))
                .collidable()
                .with(fisica)
                .with(chefe)
                .with(animacao)
                .build();
        configurarContatoComJogador(fisica, entidade, lado, lado);
        return entidade;
    }

    private static Map<EstadoAnimacao, com.almasb.fxgl.texture.AnimationChannel> animacoesChefe(String conjunto) {
        return Map.of(
                EstadoAnimacao.PARADO, Sprites.animacao("chefe", conjunto + "-parado", 8, 0.9, ESCALA_SPRITES),
                EstadoAnimacao.ATACANDO, Sprites.animacao("chefe", conjunto + "-carga", 4, 0.25, ESCALA_SPRITES),
                EstadoAnimacao.MACHUCADO, Sprites.animacao("chefe", conjunto + "-dano", 1, 0.1, ESCALA_SPRITES));
    }

    //Tiro do chefe (leque) e bloco da chuva: projétil com direção fixa
    @Spawns("projetil_chefe")
    public Entity spawnProjetilChefe(SpawnData data) {
        Entity origem = data.get("origem");
        Entity alvo = data.get("alvo");
        int dano = data.get("dano");
        double velocidade = data.get("velocidade");
        Point2D direcao = data.get("direcao");
        String sprite = data.get("sprite");

        AnimatedTexture visual = new AnimatedTexture(Sprites.animacao("chefe", sprite, 2, 0.2, ESCALA_SPRITES));
        visual.loop();
        return entityBuilder(data)
                .type(TipoEntidade.PROJETIL)
                .view(visual)
                .zIndex(40)
                .with(new ComponenteProjetil(origem, alvo, dano, velocidade, direcao, 30))
                .build();
    }

    //Aviso da chuva: coluna vermelha piscando onde o bloco vai cair
    @Spawns("alerta_chefe")
    public Entity spawnAlertaChefe(SpawnData data) {
        double duracao = data.get("duracao");
        Rectangle coluna = new Rectangle(28, 720, Color.rgb(255, 60, 110, 0.18));
        Rectangle chao = new Rectangle(28, 8, Color.rgb(255, 90, 140, 0.9));
        chao.setTranslateY(672);
        Group visual = new Group(coluna, chao);
        FadeTransition piscar = new FadeTransition(Duration.seconds(0.15), visual);
        piscar.setFromValue(1);
        piscar.setToValue(0.35);
        piscar.setAutoReverse(true);
        piscar.setCycleCount(javafx.animation.Animation.INDEFINITE);
        piscar.play();
        return entityBuilder(data)
                .view(visual)
                .zIndex(5)
                .with(new ExpireCleanComponent(Duration.seconds(duracao)))
                .build();
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

        //Robô atirador sobre esteira (desenhado pro projeto)
        double largura = 45, altura = 54;
        var atirador = new ComponenteInimigoDistancia(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao);
        Entity inimigo = entityBuilder(data)
                .type(TipoEntidade.INIMIGO_DISTANCIA)
                .bbox(new HitBox(BoundingShape.box(largura, altura)))
                .collidable()
                .with(fisica)
                .with(atirador)
                .with(animacaoInimigo(atirador, Map.of(
                        EstadoAnimacao.PARADO, Sprites.animacao("robo-atirador", "parado", 4, 0.8, ESCALA_SPRITES),
                        EstadoAnimacao.ANDANDO, Sprites.animacao("robo-atirador", "andar", 4, 0.4, ESCALA_SPRITES),
                        EstadoAnimacao.ATACANDO, Sprites.animacao("robo-atirador", "tiro", 3, 0.35, ESCALA_SPRITES),
                        EstadoAnimacao.MACHUCADO, Sprites.animacao("robo-atirador", "dano", 1, 0.2, ESCALA_SPRITES)),
                        48, 23, 47))
                .build();

        configurarContatoComJogador(fisica, inimigo, largura, altura);
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

        //Robô bípede com garra (desenhado pro projeto)
        double largura = 42, altura = 60;
        var melee = new ComponenteInimigoMelee(nome, vidaMaxima, velocidade, dano, alcanceAtaque, alcanceDeteccao);
        Entity inimigo = entityBuilder(data)
                .type(TipoEntidade.INIMIGO_MELEE)
                .bbox(new HitBox(BoundingShape.box(largura, altura)))
                .collidable()
                .with(fisica)
                .with(melee)
                .with(animacaoInimigo(melee, Map.of(
                        EstadoAnimacao.PARADO, Sprites.animacao("robo-melee", "parado", 4, 0.8, ESCALA_SPRITES),
                        EstadoAnimacao.ANDANDO, Sprites.animacao("robo-melee", "andar", 6, 0.6, ESCALA_SPRITES),
                        EstadoAnimacao.ATACANDO, Sprites.animacao("robo-melee", "ataque", 4, 0.35, ESCALA_SPRITES),
                        EstadoAnimacao.MACHUCADO, Sprites.animacao("robo-melee", "dano", 1, 0.2, ESCALA_SPRITES)),
                        48, 24, 47))
                .build();

        configurarContatoComJogador(fisica, inimigo, largura, altura);
        return inimigo;
    }

    //Configura, pra qualquer inimigo, o esquema de dois fixtures do
    //comentário lá em cima da classe: tira o jogador do maskBits do
    //fixture sólido e adiciona um sensor extra (mesmo tamanho) que
    //chama serAtingido() no ComponenteJogador a cada frame de contato.
    //Animações da jogadora com uma empunhadura. Os arquivos seguem o
    //padrão "<empunhadura>-<ação>-<n>.png"; os números são quantos
    //quadros cada ação tem. O ataque da arma de fogo se chama "atirar".
    private Map<EstadoAnimacao, com.almasb.fxgl.texture.AnimationChannel> animacoesJogadora(String emp,
            int correr, int pular, int ataque, int ataqueCorrendo, int ataqueNoAr) {
        String atacar = emp.equals("arma") ? "atirar" : "ataque";
        String atacarCorrendo = emp.equals("arma") ? "atirar-correndo" : "ataque-correndo";
        String atacarNoAr = emp.equals("arma") ? "atirar-ar" : "ataque-ar";
        return Map.ofEntries(
                Map.entry(EstadoAnimacao.PARADO, Sprites.animacao("jogador", emp + "-parado", 4, 0.9, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.ANDANDO, Sprites.animacao("jogador", emp + "-correr", correr, 0.6, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.PULANDO, Sprites.animacao("jogador", emp + "-pular", pular, 0.3, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.CAINDO, Sprites.animacao("jogador", emp + "-cair", 1, 0.2, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.ATACANDO, Sprites.animacao("jogador", emp + "-" + atacar, ataque, 0.3, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.ATACANDO_ANDANDO, Sprites.animacao("jogador", emp + "-" + atacarCorrendo, ataqueCorrendo, 0.45, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.ATACANDO_NO_AR, Sprites.animacao("jogador", emp + "-" + atacarNoAr, ataqueNoAr, 0.3, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.MACHUCADO, Sprites.animacao("jogador", "dano", 1, 0.2, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.DASH, Sprites.animacao("jogador", emp + "-dash", 3, 0.2, ESCALA_SPRITES)),
                Map.entry(EstadoAnimacao.MAGIA, Sprites.animacao("jogador", emp + "-magia", 3, 0.3, ESCALA_SPRITES)));
    }

    //Animação de qualquer inimigo: mesma regra de estado do jogador,
    //lendo o ComponenteInimigo
    private ComponenteAnimado animacaoInimigo(ComponenteInimigo inimigo, Map<EstadoAnimacao, com.almasb.fxgl.texture.AnimationChannel> animacoes,
            double larguraQuadro, double centroX, double peY) {
        return new ComponenteAnimado(animacoes, larguraQuadro, centroX, peY, ESCALA_SPRITES,
                () -> ComponenteAnimado.escolherEstado(inimigo.estaMachucado(), inimigo.estaAtacando(),
                        inimigo.estaNoChao(), inimigo.getVelocidadeX(), inimigo.getVelocidadeY()),
                inimigo::estaOlhandoDireita)
                .comCongelamento(inimigo::estaCongelado);
    }

    //Explosão quando um inimigo morre (ver ComponenteInimigo.tomarDano).
    //Só visual: toca a animação uma vez e some. x/y = centro do inimigo.
    @Spawns("explosao")
    public Entity spawnExplosao(SpawnData data) {
        AnimatedTexture explosao = new AnimatedTexture(Sprites.animacao("explosao", "explosao", 6, 0.5, ESCALA_SPRITES));
        explosao.setTranslateX(-27 * ESCALA_SPRITES); //quadro 55x52: centraliza no ponto
        explosao.setTranslateY(-26 * ESCALA_SPRITES);
        explosao.play();
        return entityBuilder(data)
                .view(explosao)
                .with(new ExpireCleanComponent(Duration.seconds(0.5)))
                .build();
    }

    private void configurarContatoComJogador(PhysicsComponent fisica, Entity entidadeInimigo, double largura, double altura) {
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

        fisica.addSensor(new HitBox("PLAYER_CONTACT", BoundingShape.box(largura, altura)), new SensorCollisionHandler() {
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
                //Congelado (RaioDeGelo / Buraco Negro) não machuca no toque
                if (inimigo == null || inimigo.estaCongelado()) {
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

        //Magias mandam um sprite animado (sprites/magias, ver
        //GolpeDeEnergia.getSprite()); tiro de arma é um quadrado da cor dela
        Node visual;
        if (data.hasKey("sprite")) {
            AnimatedTexture magia = new AnimatedTexture(
                    Sprites.animacao("magias", data.get("sprite"), 4, 0.3, ESCALA_SPRITES));
            magia.loop();
            //quadro 28x18 (x1,5 = 42x27): a frente do orbe na frente da caixa
            magia.setTranslateX(direcaoX > 0 ? tamanho - 42 : 0);
            magia.setTranslateY(tamanho / 2 - 13.5);
            magia.setScaleX(direcaoX > 0 ? 1 : -1);
            visual = magia;
        } else {
            visual = new Rectangle(tamanho, tamanho, cor);
        }

        return entityBuilder(data)
                .type(TipoEntidade.DISPARO_JOGADOR)
                .bbox(new HitBox(BoundingShape.box(tamanho, tamanho)))
                .view(visual)
                .with(new ComponenteDisparoJogador(dano, direcaoX, direcaoY, velocidade, tempoVida, alvosMaximos,
                        data.hasKey("congelamento") ? data.<Double>get("congelamento") : 0.0))
                .build();
    }

    //Buraco negro (habilidades.BuracoNegro). x/y = centro. Sem física:
    //só o sprite girando e crescendo; quem puxa e destrói os inimigos é
    //o ComponenteBuracoNegro
    @Spawns("buraco_negro")
    public Entity spawnBuracoNegro(SpawnData data) {
        double duracao = data.get("duracao");
        AnimatedTexture sprite = new AnimatedTexture(
                Sprites.animacao("magias", "buraco-negro", 8, 0.5, 3));
        sprite.loop();
        double lado = 64 * 3;
        sprite.setTranslateX(-lado / 2);
        sprite.setTranslateY(-lado / 2);
        Group visual = new Group(sprite);
        visual.setScaleX(0.1);
        visual.setScaleY(0.1);
        ScaleTransition abrir = new ScaleTransition(Duration.seconds(0.35), visual);
        abrir.setToX(1);
        abrir.setToY(1);
        abrir.play();
        return entityBuilder(data)
                .view(visual)
                .zIndex(50)
                .with(new ComponenteBuracoNegro(duracao))
                .build();
    }

    //Módulo de cura ou de energia parado no mapa (lista "items" do JSON
    //da sala, ver controle.GerenciadorSalas.spawnItens()). Igual ao gatilho: sem
    //física, só bounding box + collidable(), e quem trata o toque é o
    //CollisionHandler(JOGADOR, ITEM) em controle.ControleJogo.registrarColisoes(). Verde =
    //cura, azul = energia.
    @Spawns("item")
    public Entity spawnItem(SpawnData data) {
        String tipoItem = data.get("tipoItem"); //"cura", "energia" ou "arma"
        int quantidade = data.get("quantidade");
        String arma = data.get("arma");
        String idColeta = data.get("idColeta");

        //Ícone do item (sprites/itens) flutuando devagar sobre um brilho,
        //pra se destacar do cenário
        double tamanho = 24;
        String icone = "arma".equals(tipoItem) ? arma
                : "energia".equals(tipoItem) ? "modulo_energia" : "modulo_cura";
        Color corBrilho = "arma".equals(tipoItem) ? Color.web("#ffd800")
                : "energia".equals(tipoItem) ? Color.web("#00fff0") : Color.web("#50e678");
        ImageView imagem = new ImageView(Sprites.quadros("itens", icone, 1, 1).get(0));
        Circle brilho = new Circle(tamanho / 2, tamanho / 2, tamanho * 0.7, corBrilho.deriveColor(0, 1, 1, 0.25));
        brilho.setEffect(new javafx.scene.effect.GaussianBlur(8));
        Group visual = new Group(brilho, imagem);
        TranslateTransition flutuar = new TranslateTransition(Duration.seconds(0.9), visual);
        flutuar.setFromY(0);
        flutuar.setToY(-5);
        flutuar.setAutoReverse(true);
        flutuar.setCycleCount(TranslateTransition.INDEFINITE);
        flutuar.play();

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
        item.setProperty("arma", arma);
        item.setProperty("idColeta", idColeta);
        return item;
    }

    //Plataforma: corpo estático, não cai nem se move, só existe pra
    //outros corpos colidirem com ela. Retângulo colorido por
    //enquanto, no lugar de um tileset de verdade.
    //Faísca de acerto (shot-hit do Warped City): toca uma vez e some
    @Spawns("impacto")
    public Entity spawnImpacto(SpawnData data) {
        AnimatedTexture impacto = new AnimatedTexture(Sprites.animacao("magias", "impacto", 3, 0.2, ESCALA_SPRITES));
        impacto.setTranslateX(-impacto.getImage().getWidth() / 2);
        impacto.setTranslateY(-impacto.getImage().getHeight() / 2);
        impacto.play();
        return entityBuilder(data)
                .view(impacto)
                .zIndex(10)
                .with(new ExpireCleanComponent(Duration.seconds(0.2)))
                .build();
    }

    @Spawns("plataforma")
    public Entity spawnPlataforma(SpawnData data) {
        double largura = data.get("largura");
        double altura = data.get("altura");

        var fisica = new PhysicsComponent();
        fisica.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(TipoEntidade.PLATAFORMA)
                .bbox(new HitBox(BoundingShape.box(largura, altura)))
                .view(visualPlataforma(largura, altura))
                .with(fisica)
                .build();
    }

    //Visual da plataforma com as peças do tileset do Warped City
    //(sprites/cenario). Baixa (até 24 px): plataforma flutuante com as
    //duas pontas e o meio repetido. Alta (o chão): blocos repetidos com
    //a faixa ciano no topo. ImagePattern repete a imagem sozinho.
    private Node visualPlataforma(double largura, double altura) {
        Group grupo = new Group();
        if (altura <= 24) {
            double escala = altura / 16; //a peça tem 16 px de altura
            Image esquerda = Sprites.quadros("cenario", "plataforma-esquerda", 1, escala).get(0);
            Image meio = Sprites.quadros("cenario", "plataforma-meio", 1, escala).get(0);
            Image direita = Sprites.quadros("cenario", "plataforma-direita", 1, escala).get(0);
            Rectangle centro = new Rectangle(largura, altura);
            centro.setFill(new ImagePattern(meio, 0, 0, meio.getWidth(), meio.getHeight(), false));
            ImageView pontaEsquerda = new ImageView(esquerda);
            ImageView pontaDireita = new ImageView(direita);
            pontaDireita.setTranslateX(largura - direita.getWidth());
            grupo.getChildren().addAll(centro, pontaEsquerda, pontaDireita);
        } else {
            Image bloco = Sprites.quadros("cenario", "chao", 1, ESCALA_SPRITES).get(0);
            Image borda = Sprites.quadros("cenario", "chao-borda", 1, ESCALA_SPRITES).get(0);
            Rectangle corpo = new Rectangle(largura, altura);
            corpo.setFill(new ImagePattern(bloco, 0, 0, bloco.getWidth(), bloco.getHeight(), false));
            Rectangle topo = new Rectangle(largura, borda.getHeight());
            topo.setFill(new ImagePattern(borda, 0, 0, borda.getWidth(), borda.getHeight(), false));
            grupo.getChildren().addAll(corpo, topo);
        }
        return grupo;
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
    //controle.ControleJogo.registrarColisoes(). indiceDestino/ladoEntrada guardam pra qual
    //sala (índice em GerenciadorSalas.SALAS_DO_MAPA) e por qual lado o jogador
    //deve reaparecer (ver GerenciadorSalas.iniciarTransicao()/trocarSala()).
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
        //CollisionHandler em controle.ControleJogo.registrarColisoes() conseguir ler de volta
        //no momento da colisão.
        gatilho.setProperty("indiceDestino", indiceDestino);
        gatilho.setProperty("ladoEntrada", ladoEntrada);

        return gatilho;
    }
}
