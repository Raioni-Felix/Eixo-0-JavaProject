package com.jogo.componentes;

import com.almasb.fxgl.entity.component.Component;
import com.jogo.inventario.Inventariavel;
import com.jogo.itens.Arma;
import javafx.scene.Node;

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

    //Desenho da arma equipada, grudado no jogador (filho do
    //ViewComponent dele). Só visual: quem aplica dano é o ataque que a
    //arma cria. Cada arma diz como é desenhada (Arma.criarVisual()).
    private Node visualArma;

    @Override
    public void onAdded() {
        jogador = entity.getComponent(ComponenteJogador.class);
    }

    @Override
    public void onUpdate(double tpf) {
        if (visualArma == null) {
            return;
        }

        //Espelha e reposiciona a arma conforme o jogador vira. scaleX
        //negativo espelha o desenho, sem precisar de uma segunda imagem
        //"virada pra esquerda".
        double largura = visualArma.getBoundsInLocal().getWidth();
        boolean olhandoDireita = jogador.estaOlhandoDireita();
        visualArma.setTranslateX(olhandoDireita ? entity.getWidth() : -largura);
        visualArma.setScaleX(olhandoDireita ? 1 : -1);
    }

    //Igual ao equiparArma(idArma) do Jogador do barebones: só equipa
    //arma que já está no inventário do jogador. Devolve false se o id
    //não existir ou não for uma Arma (ex: id de um módulo).
    public boolean equiparArma(String idArma) {
        Inventariavel objeto = jogador.getInventario().buscar(idArma);
        if (objeto instanceof Arma armaEncontrada) {
            this.armaEquipada = armaEncontrada;
            trocarVisual();
            return true;
        }
        return false;
    }

    //Todas as armas do inventário, na ordem em que foram pegas. A lista
    //é do tipo da mãe (Arma), mas guarda objetos de várias filhas
    //(Espada, Shotgun, PistolaDuplaDePlasma, RifleSniper): é a coleção
    //polimórfica do projeto.
    public List<Arma> getArsenal() {
        return jogador.getInventario().listarDoTipo(Arma.class);
    }

    //Tecla Q (ver Main.initInput()): equipa a próxima arma do arsenal,
    //voltando pra primeira depois da última.
    public void trocarArma() {
        List<Arma> arsenal = getArsenal();
        if (arsenal.size() < 2) {
            return;
        }
        int atual = arsenal.indexOf(armaEquipada);
        Arma proxima = arsenal.get((atual + 1) % arsenal.size());
        equiparArma(proxima.getId());
    }

    public Arma getArmaEquipada() {
        return armaEquipada;
    }

    //Chamado pelo ComponenteJogador.iniciarAtaque() (tecla Z). A mesma
    //chamada serve pra qualquer arma: quem decide se é corte, tiro,
    //leque de balins, tiro duplo ou tiro que atravessa é a classe real
    //do objeto guardado em armaEquipada.
    public void atacar() {
        if (jogador.estaAtordoado() || armaEquipada == null) {
            return; // atordoado ou sem arma
        }
        armaEquipada.atacar(entity, jogador.estaOlhandoDireita() ? 1 : -1);
    }

    private void trocarVisual() {
        if (visualArma != null) {
            entity.getViewComponent().removeChild(visualArma);
        }
        visualArma = armaEquipada.criarVisual();
        //Encosta a arma na altura do meio do jogador
        visualArma.setTranslateY(entity.getHeight() / 2 - visualArma.getBoundsInLocal().getHeight() / 2);
        entity.getViewComponent().addChild(visualArma);
    }
}
