package com.jogo.componentes;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import com.jogo.entidades.EntityType;
import com.jogo.itens.Weapon;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.spawn;
import static com.almasb.fxgl.dsl.FXGL.texture;

/**
 * Componente de combate do player: guarda a arma equipada e decide
 * COMO o ataque acontece dependendo do WeaponType. Fica separado do
 * PlayerComponent de propósito — vida/movimento/progressão são
 * preocupações diferentes de "com o que eu ataco".
 *
 * O cooldown de verdade mora dentro do próprio Weapon
 * (Weapon.attack() só retorna true se já passou o coolDown), então
 * esse componente não guarda nenhum timer duplicado.
 *
 * Depende do PlayerComponent estar na mesma Entity (pra saber
 * facingRight/isStunned), por isso a ordem dos .with() na
 * FabricaEntidades importa: PlayerComponent primeiro, WeaponComponent
 * depois.
 */
public class WeaponComponent extends Component {

    private Weapon equippedWeapon;
    private PlayerComponent player;

    //Sprite da arma equipada, grudado no player (filho do
    //ViewComponent dele). Só existe visualmente — não tem física
    //nem colisão própria, é o hitbox_ataque/ataque_jogador quem
    //aplica o dano de verdade.
    private Texture weaponSprite;

    @Override
    public void onAdded() {
        player = entity.getComponent(PlayerComponent.class);

        weaponSprite = texture("sword.png");
        //Encosta a espada na altura do meio do player; ajusta esses
        //números se o sprite de vocês tiver proporção diferente do
        //placeholder 32x16.
        weaponSprite.setTranslateY(entity.getHeight() / 2 - weaponSprite.getHeight() / 2);
        entity.getViewComponent().addChild(weaponSprite);
    }

    @Override
    public void onUpdate(double tpf) {
        if (weaponSprite == null) {
            return;
        }

        //Espelha e reposiciona a espada conforme o player vira. scaleX
        //negativo espelha o sprite (inverte a lâmina), sem precisar
        //de uma segunda imagem "virada pra esquerda".
        boolean facingRight = player.isFacingRight();
        weaponSprite.setTranslateX(facingRight ? entity.getWidth() : -weaponSprite.getWidth());
        weaponSprite.setScaleX(facingRight ? 1 : -1);
    }

    public void equipWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
    }

    public void attack() {
        if (player.isStunned() || equippedWeapon == null || !equippedWeapon.attack()) {
            return; // atordoado, sem arma, ou ainda em cooldown da arma
        }

        switch (equippedWeapon.getType()) {
            case MELEE -> performMeleeAttack();
            case RANGED -> performRangedAttack();
            case MAGIC -> { /* futuro */ }
        }
    }

    // Reaproveita o "ataque_jogador" que já existe na FabricaEntidades
    // (sensor + ExpireCleanComponent cuidando de sumir sozinho). Só
    // manda o alcance da arma como tamanho do sensor, em vez do 40
    // fixo que estava hardcoded lá antes.
    private void performMeleeAttack() {
        double range = equippedWeapon.getRange();
        boolean facingRight = player.isFacingRight();

        //offsetX/offsetY são relativos ao player, não posições
        //absolutas do mundo — manda o próprio player (entity) junto,
        //pra AtaqueJogadorComponent conseguir seguir ele todo frame
        //(ver comentário lá). Sem isso a hitbox nascia numa posição
        //fixa e ficava pra trás se o player andasse/pulasse/desse
        //dash durante os 0.15s do golpe.
        double offsetX = facingRight ? entity.getWidth() : -range;
        double offsetY = 0;

        SpawnData data = new SpawnData(entity.getX() + offsetX, entity.getY() + offsetY)
                .put("size", range)
                .put("damage", equippedWeapon.getTotalDamage())
                .put("jogador", entity)
                .put("offsetX", offsetX)
                .put("offsetY", offsetY);

        spawn("ataque_jogador", data);
    }

    private void performRangedAttack() {
        Entity target = findNearestEnemyInRange(equippedWeapon.getRange());
        if (target == null) {
            return; // sem inimigo à vista, sem tiro
        }

        SpawnData data = new SpawnData(entity.getX(), entity.getY())
                .put("origin", entity)
                .put("target", target)
                .put("damage", equippedWeapon.getTotalDamage())
                .put("speed", 500.0);

        spawn("projetil", data);
    }

    // Acha o inimigo ativo mais próximo, dentro do alcance, do lado
    // pra onde o player está olhando. O ProjectileComponent precisa
    // de um alvo (Entity) pra calcular a direção da bala, então o
    // ataque ranged do player tem que escolher um alvo antes de
    // atirar.
    private Entity findNearestEnemyInRange(double range) {
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        boolean facingRight = player.isFacingRight();

        EntityType[] tiposInimigo = {
                EntityType.INIMIGO_VOADOR, EntityType.INIMIGO_RANGED, EntityType.INIMIGO_MELEE
        };

        for (EntityType tipo : tiposInimigo) {
            for (Entity candidate : getGameWorld().getEntitiesByType(tipo)) {
                if (!candidate.isActive()) continue;

                double dist = entity.distance(candidate);
                if (dist > range) continue;

                boolean naFrente = facingRight
                        ? candidate.getX() >= entity.getX()
                        : candidate.getX() <= entity.getX();
                if (!naFrente) continue;

                if (dist < nearestDist) {
                    nearest = candidate;
                    nearestDist = dist;
                }
            }
        }
        return nearest;
    }
}