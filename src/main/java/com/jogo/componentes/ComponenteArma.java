package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import com.jogo.entidades.TipoEntidade;
import com.jogo.itens.Arma;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.spawn;
import static com.almasb.fxgl.dsl.FXGL.texture;

/**
 * Componente de combate do jogador: guarda a arma equipada e decide
 * COMO o ataque acontece dependendo do TipoArma. Fica separado do
 * ComponenteJogador de propósito — vida/movimento/progressão são
 * preocupações diferentes de "com o que eu ataco".
 *
 * O cooldown de verdade mora dentro da própria Arma
 * (Arma.atacar() só retorna true se já passou o cooldown), então
 * esse componente não guarda nenhum timer duplicado.
 *
 * Depende do ComponenteJogador estar na mesma Entity (pra saber
 * olhandoDireita/estaAtordoado), por isso a ordem dos .with() na
 * FabricaEntidades importa: ComponenteJogador primeiro, ComponenteArma
 * depois.
 */
public class ComponenteArma extends Component {

    private Arma armaEquipada;
    private ComponenteJogador jogador;

    //Sprite da arma equipada, grudado no jogador (filho do
    //ViewComponent dele). Só existe visualmente — não tem física
    //nem colisão própria, é o ComponenteAtaqueJogador quem
    //aplica o dano de verdade.
    private Texture spriteArma;

    @Override
    public void onAdded() {
        jogador = entity.getComponent(ComponenteJogador.class);

        spriteArma = texture("sword.png");
        //Encosta a espada na altura do meio do jogador; ajusta esses
        //números se o sprite de vocês tiver proporção diferente do
        //placeholder 32x16.
        spriteArma.setTranslateY(entity.getHeight() / 2 - spriteArma.getHeight() / 2);
        entity.getViewComponent().addChild(spriteArma);
    }

    @Override
    public void onUpdate(double tpf) {
        if (spriteArma == null) {
            return;
        }

        //Espelha e reposiciona a espada conforme o jogador vira. scaleX
        //negativo espelha o sprite (inverte a lâmina), sem precisar
        //de uma segunda imagem "virada pra esquerda".
        boolean olhandoDireita = jogador.estaOlhandoDireita();
        spriteArma.setTranslateX(olhandoDireita ? entity.getWidth() : -spriteArma.getWidth());
        spriteArma.setScaleX(olhandoDireita ? 1 : -1);
    }

    public void equiparArma(Arma arma) {
        this.armaEquipada = arma;
    }

    public void atacar() {
        if (jogador.estaAtordoado() || armaEquipada == null || !armaEquipada.atacar()) {
            return; // atordoado, sem arma, ou ainda em cooldown da arma
        }

        switch (armaEquipada.getTipo()) {
            case MELEE -> executarAtaqueMelee();
            case DISTANCIA -> executarAtaqueDistancia();
            case MAGIA -> { /* futuro */ }
        }
    }

    // Reaproveita o "ataque_jogador" que já existe na FabricaEntidades
    // (sensor + ExpireCleanComponent cuidando de sumir sozinho). Só
    // manda o alcance da arma como tamanho do sensor, em vez do 40
    // fixo que estava hardcoded lá antes.
    private void executarAtaqueMelee() {
        double alcance = armaEquipada.getAlcance();
        boolean olhandoDireita = jogador.estaOlhandoDireita();

        //deslocamentoX/deslocamentoY são relativos ao jogador, não
        //posições absolutas do mundo — manda o próprio jogador (entity)
        //junto, pra ComponenteAtaqueJogador conseguir seguir ele todo
        //frame (ver comentário lá). Sem isso a hitbox nascia numa
        //posição fixa e ficava pra trás se o jogador andasse/pulasse/
        //desse dash durante os 0.15s do golpe.
        double deslocamentoX = olhandoDireita ? entity.getWidth() : -alcance;
        double deslocamentoY = 0;

        SpawnData dados = new SpawnData(entity.getX() + deslocamentoX, entity.getY() + deslocamentoY)
                .put("tamanho", alcance)
                .put("dano", armaEquipada.getDanoTotal())
                .put("jogador", entity)
                .put("deslocamentoX", deslocamentoX)
                .put("deslocamentoY", deslocamentoY);

        spawn("ataque_jogador", dados);
    }

    private void executarAtaqueDistancia() {
        Entity alvo = encontrarInimigoMaisProximo(armaEquipada.getAlcance());
        if (alvo == null) {
            return; // sem inimigo à vista, sem tiro
        }

        SpawnData dados = new SpawnData(entity.getX(), entity.getY())
                .put("origem", entity)
                .put("alvo", alvo)
                .put("dano", armaEquipada.getDanoTotal())
                .put("velocidade", 500.0);

        spawn("projetil", dados);
    }

    // Acha o inimigo ativo mais próximo, dentro do alcance, do lado
    // pra onde o jogador está olhando. O ComponenteProjetil precisa
    // de um alvo (Entity) pra calcular a direção da bala, então o
    // ataque à distância do jogador tem que escolher um alvo antes de
    // atirar.
    private Entity encontrarInimigoMaisProximo(double alcance) {
        Entity maisProximo = null;
        double distanciaMaisProxima = Double.MAX_VALUE;
        boolean olhandoDireita = jogador.estaOlhandoDireita();

        TipoEntidade[] tiposInimigo = {
                TipoEntidade.INIMIGO_VOADOR, TipoEntidade.INIMIGO_DISTANCIA, TipoEntidade.INIMIGO_MELEE
        };

        for (TipoEntidade tipo : tiposInimigo) {
            for (Entity candidato : getGameWorld().getEntitiesByType(tipo)) {
                if (!candidato.isActive()) continue;

                double distancia = entity.distance(candidato);
                if (distancia > alcance) continue;

                boolean naFrente = olhandoDireita
                        ? candidato.getX() >= entity.getX()
                        : candidato.getX() <= entity.getX();
                if (!naFrente) continue;

                if (distancia < distanciaMaisProxima) {
                    maisProximo = candidato;
                    distanciaMaisProxima = distancia;
                }
            }
        }
        return maisProximo;
    }
}
