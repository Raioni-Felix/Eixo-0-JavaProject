# Requisitos — Eixo 0 (FXGL)

Metroidvania 2D em Java com FXGL. O jogador explora 4 salas ligadas, enfrenta três tipos de inimigo, usa armas, habilidades e módulos, e sobe de nível.

Status: ✅ feito · 🧪 com teste automático · 🎮 testado jogando (depende da janela do FXGL).

## Requisitos funcionais

| # | Requisito | Onde está | Verificação |
|---|---|---|---|
| RF01 | Menu principal com Jogar e Sair | `visao.TelaMenuPrincipal`, `ControleJogo.iniciarJogo` | 🎮 |
| RF02 | Mover para os lados, pular (com pulo duplo) e dash (Shift) | `ComponenteJogador`, `ControleEntrada` | 🎮 |
| RF03 | Atacar com a arma equipada (Z) e trocar de arma (Q) entre Espada, Adagas Duplas, Shotgun, Pistola Dupla de Plasma e Rifle Sniper | `ComponenteArma`, pacote `itens` | 🧪 `ArmaTest`, `ArmasDeFogoTest` · 🎮 |
| RF04 | Cada arma ataca do seu jeito: corte, 2 cortes rápidos, 5 balins em leque, 2 tiros, tiro que atravessa 3 inimigos | `Espada`, `AdagaDupla`, `Shotgun`, `PistolaDuplaDePlasma`, `RifleSniper` | 🧪 `ArmasDeFogoTest` · 🎮 |
| RF05 | Toda arma tem tempo de espera entre ataques | `Arma.atacar` | 🧪 `ArmaTest` |
| RF06 | Golpe de Energia (X, Bola de Fogo) e Apara (C) com custo de energia e cooldown | pacote `habilidades` | 🧪 `HabilidadeTest` · 🎮 |
| RF07 | Barra de vida e de energia sempre de acordo com o valor atual e o máximo | `visao.HudJogo` | 🎮 |
| RF08 | Energia recarrega sozinha devagar | `ComponenteJogador.onUpdate` | 🎮 |
| RF09 | Depois de tomar dano, o jogador fica invencível por 1 s e é empurrado | `ComponenteJogador.tomarDano/serAtingido` | 🧪 `ComponenteJogadorTest` · 🎮 |
| RF10 | Três inimigos: Melee (corpo a corpo), Voador (bate e sobe), Distância (atira projétil) | `ComponenteInimigo` e filhas | 🧪 `ComponenteInimigoTest` · 🎮 |
| RF11 | Matar inimigo dá XP (20, 25, 35); com XP suficiente o jogador sobe de nível (+20 vida máx., +5 velocidade, vida cheia) | `ComponenteInimigo.tomarDano`, `ComponenteJogador.adicionarXp` | 🧪 `ComponenteJogadorTest`, `JogadorSalaIntegracaoTest` |
| RF12 | Módulos de cura e energia no mapa vão pro inventário; teclas 1 e 2 usam | `ControleJogo`, `inventario` | 🧪 `ComponenteJogadorTest`, `InventarioTest` · 🎮 |
| RF13 | Módulo já pego não reaparece ao voltar pra sala | `GerenciadorSalas.itensColetados` | 🎮 |
| RF14 | 4 salas lidas de JSON; passar pela borda troca de sala com transição, mantendo vida e inventário | `GerenciadorSalas`, `CarregadorDeNiveis`, `CortinaTransicao` | 🧪 `CarregadorDeNiveisTest` · 🎮 |
| RF15 | Câmera segue o jogador dentro dos limites da sala | `GerenciadorSalas.atualizarCamera` | 🎮 |
| RF16 | Pausa (ESC) com Continuar e Menu | `visao.TelaPausa`, `ControleJogo` | 🎮 |
| RF17 | Game Over com Reiniciar (volta direto pro jogo) e Menu Principal | `visao.TelaGameOver`, `ControleJogo` | 🎮 |
| RF18 | Personagens animados: parado, andar, pular/cair, atacar (parado, correndo, no ar), dano; a jogadora aparece com a arma equipada; inimigos explodem ao morrer; voador é um drone, os outros dois são robôs | `componentes.visual.ComponenteAnimado`, `Sprites` | 🧪 `ComponenteAnimadoTest` · 🎮 |

## Tratamento de erros

| # | Situação | Exceção | Tratada em | Teste |
|---|---|---|---|---|
| RE01 | Usar módulo que o jogador não tem | `ItemNaoEncontradoException` | `ControleJogo.usarModulo`: aviso vermelho no HUD | `ComponenteJogadorTest` 🎮 |
| RE02 | Consumir uma arma / equipar o que não é arma | `ItemIncompativelException` | `ControleJogo` | `ComponenteJogadorTest` |
| RE03 | JSON de sala ausente, vazio, quebrado ou sem `player`/`platforms` | `NivelInvalidoException` (com a causa do Gson) | `GerenciadorSalas.prepararSala` | `CarregadorDeNiveisTest` |
| RE04 | Valores inválidos (nome vazio, vida ≤ 0, dano/cooldown negativo, alcance ≤ 0) | `IllegalArgumentException` | não é tratada: é bug | `ComponenteJogadorTest`, `ArmaTest`, `HabilidadeTest` |

## Requisitos não funcionais

| # | Requisito | Como foi atendido |
|---|---|---|
| RNF01 | Java 17, FXGL 17.3, Gson, Maven | `pom.xml` |
| RNF02 | Separação MVC | modelo (`componentes`, `itens`, `habilidades`, `inventario`, `niveis`, `entidades`, `factories`, `excecoes`), visão (`visao`, `componentes.visual`), controle (`controle`); `Main` só repassa as etapas do FXGL |
| RNF03 | Balanceamento fora do código | vida, dano, velocidade, inimigos e itens de cada sala nos JSON de `assets/levels` |
| RNF04 | Testes automatizados | 60 testes JUnit 5 (`mvn test`), unidade e integração |
| RNF05 | Cobertura | JaCoCo: `inventario` 96%, `niveis` 95%, `excecoes` 100%; total ≈ 20%, porque visão/controle precisam do motor rodando |
| RNF06 | 60 FPS, janela 1280×720 com tela cheia opcional (F11) | `Main.initSettings` |
| RNF07 | Não acelerar depois de despausar | `componentes.RelogioJogo` |
| RNF08 | Código e nomes em português | todo o projeto (exceto `Main` e termos de API) |
