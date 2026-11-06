package com.jogo.controle;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.texture.Texture;
import com.jogo.componentes.ComponenteInimigo;
import com.jogo.componentes.visual.ComponenteAnimacaoFundo;
import com.jogo.excecoes.NivelInvalidoException;
import com.jogo.niveis.CarregadorDeNiveis;
import com.jogo.niveis.DadosInimigo;
import com.jogo.niveis.DadosItem;
import com.jogo.niveis.DadosNivel;
import com.jogo.niveis.DadosPlataforma;
import com.jogo.visao.CortinaTransicao;
import com.jogo.visao.RecursosVisuais;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;
import static com.almasb.fxgl.dsl.FXGL.getGameScene;
import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.spawn;

//CONTROLE das salas: carrega o JSON da sala (modelo, pacote niveis),
//monta o cenário, cria inimigos e itens, troca de sala pelo gatilho e
//move a câmera. Antes tudo isso ficava espalhado no Main.
public class GerenciadorSalas {

    //Salas (cada uma seu .json em assets/levels/), na ordem da esquerda
    //pra direita. A ordem decide os vizinhos: a sala i tem gatilho de
    //volta pra i-1 (se existir) e de ida pra i+1 (se existir). Sala nova
    //é só criar outro .json e incluir o caminho aqui.
    private static final List<String> SALAS_DO_MAPA = List.of(
            "/assets/levels/nivel1.json",
            "/assets/levels/nivel2.json",
            "/assets/levels/nivel3.json",
            "/assets/levels/nivel4.json"
    );

    //Quão rápido a câmera alcança o jogador a cada frame. Atrasada de
    //propósito (em vez de bindToEntity()), senão o recoil do knockback
    //fica invisível.
    private static final double SUAVIZACAO_CAMERA = 6.0;

    private int salaAtualIndex = 0;

    //Tamanho da sala atual (vem do JSON), usado pra travar a câmera
    private double larguraNivel;
    private double alturaNivel;

    //Trava a troca de sala enquanto a cortina está fechando/abrindo, pra
    //não disparar uma segunda troca se o jogador ficar em cima do gatilho
    private boolean transicionando = false;

    //Itens do mapa já pegos ("sala<i>-item<j>"). Sem isso, voltar pra
    //uma sala recriava os módulos e dava pra pegar de novo infinitamente.
    private final Set<String> itensColetados = new HashSet<>();

    //Jogo novo (Reiniciar/Menu Principal): volta pra primeira sala e
    //esquece os itens pegos. Monta o cenário e devolve os dados da sala
    //pra quem chamou criar o jogador.
    public DadosNivel comecarDoInicio() {
        salaAtualIndex = 0;
        transicionando = false;
        itensColetados.clear();
        return prepararSala(salaAtualIndex);
    }

    //Monta o cenário de uma sala (background, plataformas e bordas:
    //parede sólida numa ponta sem vizinho, gatilho de passagem na ponta
    //que tiver). Não mexe no jogador nem nos inimigos.
    private DadosNivel prepararSala(int index) {
        DadosNivel sala;
        try {
            sala = CarregadorDeNiveis.carregar(SALAS_DO_MAPA.get(index));
        } catch (NivelInvalidoException e) {
            //Sem a sala não tem como o jogo continuar: transforma numa
            //exceção não-checked com a causa junto, pra aparecer clara no
            //log em vez de o jogo seguir num estado quebrado
            throw new IllegalStateException("Não foi possível montar a sala " + index, e);
        }
        larguraNivel = sala.larguraNivel;
        alturaNivel = sala.alturaNivel;

        //Background: cor sólida se o JSON tiver "backgroundColor" (mais
        //leve), senão o background animado do tema ("cryolab" padrão)
        if (sala.corFundo != null) {
            Rectangle backgroundSolido = new Rectangle(larguraNivel, alturaNivel, Color.web(sala.corFundo));
            entityBuilder()
                    .at(0, 0)
                    .view(backgroundSolido)
                    .zIndex(-1) // fundo atrás de tudo
                    .buildAndAttach();
        } else {
            //O quadro é esticado até a altura da sala (sem borrar) e
            //repetido lado a lado pela largura dela. Ex.: cryolab
            //1000x180 vira 4000x720; cidade 1479x240 vira 4437x720.
            //Os quadros vêm do cache (RecursosVisuais)
            String tema = sala.fundo != null ? sala.fundo : "cryolab";
            List<Image> quadros = RecursosVisuais.quadrosFundo(tema);

            Image primeiro = quadros.get(0);
            double larguraTileBackground = primeiro.getWidth() * alturaNivel / primeiro.getHeight();
            int tilesBackground = (int) Math.ceil(larguraNivel / larguraTileBackground);

            for (int i = 0; i < tilesBackground; i++) {
                ComponenteAnimacaoFundo background = new ComponenteAnimacaoFundo(quadros, RecursosVisuais.fpsFundo(tema));

                Texture visaoBg = background.getVisao();
                visaoBg.setFitWidth(larguraTileBackground);
                visaoBg.setFitHeight(alturaNivel);
                visaoBg.setSmooth(false); //nearest neighbor, pixel art sem borrão

                entityBuilder()
                        .at(i * larguraTileBackground, 0)
                        .view(visaoBg)
                        .zIndex(-1)
                        .with(background)
                        .buildAndAttach();
            }
        }

        //Cada plataforma do JSON (o chão é só a primeira, larga)
        for (DadosPlataforma plataforma : sala.plataformas) {
            spawn("plataforma", new SpawnData(plataforma.x, plataforma.y)
                    .put("largura", plataforma.largura)
                    .put("altura", plataforma.altura));
        }

        //Borda esquerda: parede na primeira sala, gatilho pra anterior nas outras
        if (index == 0) {
            spawn("parede", new SpawnData(-50, 0).put("largura", 50.0).put("altura", alturaNivel));
        } else {
            spawn("gatilho", new SpawnData(0, 0)
                    .put("largura", 40.0)
                    .put("altura", alturaNivel)
                    .put("indiceDestino", index - 1)
                    .put("ladoEntrada", "right"));
        }

        //Borda direita: parede na última sala, gatilho pra próxima nas outras
        if (index == SALAS_DO_MAPA.size() - 1) {
            spawn("parede", new SpawnData(larguraNivel, 0).put("largura", 50.0).put("altura", alturaNivel));
        } else {
            spawn("gatilho", new SpawnData(larguraNivel - 40, 0)
                    .put("largura", 40.0)
                    .put("altura", alturaNivel)
                    .put("indiceDestino", index + 1)
                    .put("ladoEntrada", "left"));
        }

        return sala;
    }

    //Inimigos da sala ("enemies" do JSON). "type" vira o spawn
    //"inimigo_<type>" da FabricaEntidades. Precisa do jogador pronto,
    //porque cada inimigo recebe ele como alvo.
    public void spawnInimigos(DadosNivel sala, Entity jogador) {
        for (DadosInimigo dadosInimigo : sala.inimigos) {
            Entity inimigo = spawn("inimigo_" + dadosInimigo.tipo, new SpawnData(dadosInimigo.x, dadosInimigo.y)
                    .put("nome", dadosInimigo.nome)
                    .put("vidaMaxima", dadosInimigo.vidaMaxima)
                    .put("velocidade", dadosInimigo.velocidade)
                    .put("dano", dadosInimigo.dano)
                    .put("alcanceAtaque", dadosInimigo.alcanceAtaque)
                    .put("alcanceDeteccao", dadosInimigo.alcanceDeteccao));

            ComponenteInimigo.obterDe(inimigo).definirAlvo(jogador);
        }
    }

    //Módulos de cura/energia da sala ("items" do JSON), pulando os já pegos
    public void spawnItens(DadosNivel sala) {
        if (sala.itens == null) {
            return; //sala sem "items" no JSON
        }
        for (int i = 0; i < sala.itens.size(); i++) {
            String idColeta = "sala" + salaAtualIndex + "-item" + i;
            if (itensColetados.contains(idColeta)) {
                continue;
            }
            DadosItem dadosItem = sala.itens.get(i);
            spawn("item", new SpawnData(dadosItem.x, dadosItem.y)
                    .put("tipoItem", dadosItem.tipo)
                    .put("quantidade", dadosItem.quantidade)
                    .put("idColeta", idColeta));
        }
    }

    public void registrarColeta(String idColeta) {
        itensColetados.add(idColeta);
    }

    //Chamado quando o jogador encosta no gatilho. A troca real só roda
    //com a tela preta (dentro da cortina), e não direto na colisão:
    //mexer no corpo de física do jogador durante o passo de física
    //(quando a colisão é detectada) trava o Box2D.
    public void iniciarTransicao(int indiceDestino, String ladoEntrada, Entity jogador, CortinaTransicao cortina) {
        if (transicionando) {
            return;
        }
        transicionando = true;
        cortina.fecharEAbrir(
                () -> trocarSala(indiceDestino, ladoEntrada, jogador),
                () -> transicionando = false);
    }

    //Troca o conteúdo da sala sem recriar o jogador (vida e estado
    //atravessam a troca, como num metroidvania). Remove tudo que não é o
    //jogador, monta a sala nova e reposiciona o jogador na borda por onde
    //ele entra. overwritePosition() é o jeito da FXGL 17.3 de teleportar
    //uma entidade com física.
    private void trocarSala(int indiceDestino, String ladoEntrada, Entity jogador) {
        for (Entity entity : new ArrayList<>(getGameWorld().getEntities())) {
            if (entity != jogador) {
                entity.removeFromWorld();
            }
        }

        salaAtualIndex = indiceDestino;
        DadosNivel sala = prepararSala(salaAtualIndex);

        double margemEntrada = 80;
        double x = "left".equals(ladoEntrada) ? margemEntrada : larguraNivel - margemEntrada;
        double y = sala.jogador.y;
        jogador.getComponent(PhysicsComponent.class).overwritePosition(new Point2D(x, y));

        var viewport = getGameScene().getViewport();
        viewport.setBounds(0, 0, (int) larguraNivel, (int) alturaNivel);
        viewport.setX(limitarCameraX(x - getAppWidth() / 2.0));
        viewport.setY(limitarCameraY(y - getAppHeight() / 2.0));

        spawnInimigos(sala, jogador);
        spawnItens(sala);
    }

    //Centraliza a câmera no jogador de uma vez (começo do jogo)
    public void centralizarCamera(Entity jogador) {
        var viewport = getGameScene().getViewport();
        viewport.setBounds(0, 0, (int) larguraNivel, (int) alturaNivel);
        viewport.setX(limitarCameraX(jogador.getX() + jogador.getWidth() / 2 - getAppWidth() / 2.0));
        viewport.setY(limitarCameraY(jogador.getY() + jogador.getHeight() / 2 - getAppHeight() / 2.0));
    }

    //Câmera suave: anda uma fração do caminho até o jogador por frame
    public void atualizarCamera(Entity jogador, double tpf) {
        if (!jogador.isActive()) {
            return;
        }
        var viewport = getGameScene().getViewport();

        double alvoX = limitarCameraX(jogador.getX() + jogador.getWidth() / 2 - getAppWidth() / 2.0);
        double alvoY = limitarCameraY(jogador.getY() + jogador.getHeight() / 2 - getAppHeight() / 2.0);

        double t = Math.min(1.0, SUAVIZACAO_CAMERA * tpf);
        viewport.setX(viewport.getX() + (alvoX - viewport.getX()) * t);
        viewport.setY(viewport.getY() + (alvoY - viewport.getY()) * t);
    }

    //Trava a câmera nas bordas da sala, sem isso dava pra ver o vazio
    private double limitarCameraX(double x) {
        double max = Math.max(0, larguraNivel - getAppWidth());
        return Math.max(0, Math.min(max, x));
    }

    private double limitarCameraY(double y) {
        double max = Math.max(0, alturaNivel - getAppHeight());
        return Math.max(0, Math.min(max, y));
    }
}
