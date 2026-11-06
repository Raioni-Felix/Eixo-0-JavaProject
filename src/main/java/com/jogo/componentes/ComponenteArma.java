package com.jogo.componentes;

import com.almasb.fxgl.entity.component.Component;
import com.jogo.excecoes.ItemIncompativelException;
import com.jogo.excecoes.ItemNaoEncontradoException;
import com.jogo.inventario.Inventariavel;
import com.jogo.itens.Arma;

import java.util.List;

/**
 * Componente de combate do jogador: guarda a arma equipada, troca de
 * arma e manda a arma atacar. Fica separado do ComponenteJogador de
 * propósito: vida/movimento/progressão são preocupações diferentes de
 * "com o que eu ataco".
 *
 * Antes este componente tinha um switch no TipoArma (MELEE/DISTANCIA)
 * e montava o ataque aqui dentro. Agora ele não sabe COMO nenhuma arma
 * ataca: só chama armaEquipada.atacar(), e cada subclasse de Arma faz o
 * seu (polimorfismo). Arma nova não muda nada aqui.
 *
 * O cooldown mora dentro da própria Arma (atacar() só faz algo se já
 * passou o cooldown), então esse componente não guarda timer.
 *
 * Depende do ComponenteJogador estar na mesma Entity (pra saber
 * olhandoDireita/estaAtordoado e achar o inventário), por isso a ordem
 * dos .with() na FabricaEntidades importa: ComponenteJogador primeiro,
 * ComponenteArma depois.
 */
public class ComponenteArma extends Component {

    private Arma armaEquipada;
    private ComponenteJogador jogador;

    @Override
    public void onAdded() {
        jogador = entity.getComponent(ComponenteJogador.class);
    }

    //Igual ao equiparArma(idArma) do Jogador do barebones: só equipa
    //arma que já está no inventário do jogador. Uma exceção pra cada
    //problema: o id não existe, ou existe mas não é Arma (ex: um módulo).
    public void equiparArma(String idArma) throws ItemNaoEncontradoException, ItemIncompativelException {
        Inventariavel objeto = jogador.getInventario().buscar(idArma);
        if (objeto == null) {
            throw new ItemNaoEncontradoException(idArma);
        }
        if (!(objeto instanceof Arma armaEncontrada)) {
            throw new ItemIncompativelException(idArma, "equipar como arma");
        }
        equipar(armaEncontrada);
    }

    //A arma aparece na mão da jogadora pelo próprio sprite (cada
    //empunhadura tem as suas animações, ver ComponenteAnimado)
    private void equipar(Arma arma) {
        this.armaEquipada = arma;
    }

    //Todas as armas do inventário, na ordem em que foram pegas. A lista
    //é do tipo da mãe (Arma), mas guarda objetos de várias filhas
    //(Espada, Shotgun, PistolaDuplaDePlasma, RifleSniper): é a coleção
    //polimórfica do projeto.
    public List<Arma> getArsenal() {
        return jogador.getInventario().listarDoTipo(Arma.class);
    }

    //Tecla Q (ver controle.ControleEntrada): equipa a próxima arma do arsenal,
    //voltando pra primeira depois da última.
    public void trocarArma() {
        List<Arma> arsenal = getArsenal();
        if (arsenal.size() < 2) {
            return;
        }
        int atual = arsenal.indexOf(armaEquipada);
        //A próxima veio do próprio inventário, então não tem como dar
        //erro: equipa direto, sem passar pela busca por id
        equipar(arsenal.get((atual + 1) % arsenal.size()));
    }

    public Arma getArmaEquipada() {
        return armaEquipada;
    }

    //Chamado pelo ComponenteJogador.iniciarAtaque() (tecla Z). A mesma
    //chamada serve pra qualquer arma: quem decide se é corte, tiro,
    //leque de balins, tiro duplo ou tiro que atravessa é a classe real
    //do objeto guardado em armaEquipada.
    //Devolve se atacou de verdade (fora do cooldown), pra animação de
    //ataque só tocar quando sai o golpe
    public boolean atacar() {
        if (jogador.estaAtordoado() || armaEquipada == null) {
            return false; // atordoado ou sem arma
        }
        return armaEquipada.atacar(entity, jogador.estaOlhandoDireita() ? 1 : -1);
    }
}
