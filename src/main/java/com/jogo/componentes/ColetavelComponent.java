package com.jogo.componentes;

import com.almasb.fxgl.entity.component.Component;
import com.jogo.interfaces.IConsumivel;
import com.jogo.interfaces.IHabilidade;

public class ColetavelComponent extends Component {
    private IConsumivel consumivel;
    private IHabilidade habilidade;

    // Sobrecarga de construtor para aceitar múltiplos tipos de contratos
    public ColetavelComponent(IConsumivel consumivel) {
        this.consumivel = consumivel;
    }

    public ColetavelComponent(IHabilidade habilidade) {
        this.habilidade = habilidade;
    }

    public void transferirPara(PlayerComponent player) {
        InventarioComponent inventario = player.getEntity().getComponent(InventarioComponent.class);
        if (inventario != null) {
            if (consumivel != null) {
                inventario.adicionarItem(consumivel);
            }
            if (habilidade != null) {
                inventario.desbloquearHabilidade(habilidade);
            }
            entity.removeFromWorld();
        }
    }
}