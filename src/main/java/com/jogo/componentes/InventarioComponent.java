package com.jogo.componentes;

import com.almasb.fxgl.entity.component.Component;
import com.jogo.interfaces.IConsumivel;
import com.jogo.interfaces.IEquipavel;
import com.jogo.interfaces.IHabilidade;
import com.jogo.itens.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador central do inventário do jogador.
 * Delega o comportamento das entidades para suas respectivas interfaces,
 * atuando apenas como controlador de slots e acervo para a UI.
 */
public class InventarioComponent extends Component {
    
    // --- Estruturas de Dados ---
    
    // Armas (Preserva o uso da classe Weapon nativa via IEquipavel)
    private final List<Weapon> acervoArmas = new ArrayList<>();
    private final Weapon[] slotsArmas = new Weapon[2];
    private int slotAtivo = 0;

    // Habilidades (Limite de 3 ativas simultaneamente)
    private final List<IHabilidade> habilidadesDesbloqueadas = new ArrayList<>();
    private final IHabilidade[] slotsHabilidades = new IHabilidade[3];

    // Consumíveis (Uso direto do inventário)
    private final List<IConsumivel> itens = new ArrayList<>();

    private PlayerComponent player;

    @Override
    public void onAdded() {
        player = entity.getComponent(PlayerComponent.class);
    }

    // --- GERENCIAMENTO DE ARMAS ---
    
    public void adicionarArma(Weapon arma) {
        if (!acervoArmas.contains(arma)) {
            acervoArmas.add(arma);
        }
    }

    public void alocarNoSlotArma(int slotIndex, Weapon arma) {
        if (slotIndex >= 0 && slotIndex < slotsArmas.length && acervoArmas.contains(arma)) {
            slotsArmas[slotIndex] = arma;
            
            // Atualiza fisicamente no jogador se substituiu a arma que está em mãos
            if (slotIndex == slotAtivo) {
                ativarSlotArma(slotIndex);
            }
        }
    }

    public void ativarSlotArma(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < slotsArmas.length) {
            slotAtivo = slotIndex;
            Weapon armaSelecionada = slotsArmas[slotAtivo];

            if (armaSelecionada != null) {
                // A arma aciona o próprio WeaponComponent através do contrato
                ((IEquipavel) armaSelecionada).equipar(player);
            }
        }
    }

    // --- GERENCIAMENTO DE HABILIDADES ---
    
    public void desbloquearHabilidade(IHabilidade habilidade) {
        if (!habilidadesDesbloqueadas.contains(habilidade)) {
            habilidadesDesbloqueadas.add(habilidade);
        }
    }

    public void equiparHabilidade(int slotIndex, IHabilidade habilidade) {
        if (slotIndex < 0 || slotIndex >= slotsHabilidades.length) return;
        
        // Se já existe uma habilidade no slot, executa a lógica de remoção (ex: perder status passivo)
        if (slotsHabilidades[slotIndex] != null) {
            slotsHabilidades[slotIndex].desequipar(player);
        }
        
        slotsHabilidades[slotIndex] = habilidade;
        habilidade.equipar(player);
    }

    public void usarHabilidade(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < slotsHabilidades.length && slotsHabilidades[slotIndex] != null) {
            slotsHabilidades[slotIndex].usar(player);
        }
    }

    // --- GERENCIAMENTO DE ITENS (CONSUMÍVEIS) ---
    
    public void adicionarItem(IConsumivel item) {
        itens.add(item);
    }

    public void usarItem(IConsumivel item) {
        if (itens.contains(item)) {
            boolean consumido = item.consumir(player);
            // Remove do inventário apenas se o consumo foi bem-sucedido
            if (consumido) {
                itens.remove(item);
            }
        }
    }

    // --- GETTERS PARA O MENU ---
    public List<Weapon> getAcervoArmas() { return acervoArmas; }
    public Weapon[] getSlotsArmas() { return slotsArmas; }
    public int getSlotAtivo() { return slotAtivo; }
    
    public List<IHabilidade> getHabilidadesDesbloqueadas() { return habilidadesDesbloqueadas; }
    public IHabilidade[] getSlotsHabilidades() { return slotsHabilidades; }
    
    public List<IConsumivel> getItens() { return itens; }
}