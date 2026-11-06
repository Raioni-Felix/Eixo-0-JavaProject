# Eixo-0

Trabalho de POO em Java, um metroidvania 2D feito com FXGL. O jogador explora salas conectadas, enfrenta inimigos com padrões de comportamento diferentes, usa um ataque corpo a corpo e um dash, e avança entre salas através de gatilhos nas bordas do mapa.

## Tecnologias

O projeto usa Java 17, FXGL 17.3 (framework em cima do JavaFX voltado pra jogos 2D, cuida de física, câmera, input, entidades e UI) e Gson 2.10.1, usado só pra carregar os dados dos níveis a partir de arquivos JSON. O build é feito com Maven.

## Como rodar

Com o Maven configurado, o comando

```
mvn clean compile exec:java
```

compila o projeto e executa a classe principal, `com.jogo.Main`, que é a que o `exec-maven-plugin` está configurado pra rodar no `pom.xml`. O goal é `exec:java`; escrever com prefixo `java:` dá o erro "No plugin found for prefix 'java'". Também dá pra rodar `Main.java` direto pela IDE, contanto que as dependências do Maven já tenham sido baixadas.

Os testes rodam com

```
mvn test
```

que executa os testes JUnit 5 de `src/test/java` e gera o relatório de cobertura (JaCoCo) em `target/site/jacoco/index.html`. Ver "Testes".

## Executável

```
mvn clean package
java -jar target/Eixo0.jar
```

`mvn package` gera `target/Eixo0.jar` com tudo dentro (FXGL, JavaFX, Gson e as imagens), que roda com `java -jar` ou com dois cliques. Precisa de Java 17 ou mais novo instalado. O JavaFX que vai pro .jar é o do sistema de quem compilou: o .jar gerado no Linux roda no Linux, o gerado no Windows roda no Windows. A classe de entrada do .jar é `com.jogo.Lancador`, que só chama o `Main` (um .jar com o JavaFX dentro não abre direto uma classe que estende `GameApplication`).

Para um executável que não precisa de Java instalado (leva o próprio Java junto), use o `jpackage` do JDK depois do `mvn package`:

```
mkdir -p target/entrada && cp target/Eixo0.jar target/entrada/
jpackage --type app-image --name Eixo0 --input target/entrada --main-jar Eixo0.jar \
  --main-class com.jogo.Lancador --dest target/executavel \
  --add-modules java.base,java.desktop,java.scripting,java.sql,jdk.jfr,jdk.unsupported,java.logging,java.xml,java.naming,java.net.http,jdk.crypto.ec,java.management
```

O jogo fica em `target/executavel/Eixo0/bin/Eixo0` (no Windows, `Eixo0.exe`).

### Executável pronto para Windows e Linux (GitHub Actions)

O workflow `.github/workflows/executaveis.yml` gera os dois no próprio GitHub, cada um com o Java junto:

- na aba **Actions**, em "Executáveis", **Run workflow**: no fim, os arquivos `Eixo0-windows.zip` e `Eixo0-linux.tar.gz` ficam em "Artifacts";
- criando uma tag, por exemplo `git tag v1.0 && git push origin v1.0`: os dois são publicados na aba **Releases**.

No Windows, é só extrair o zip e abrir `Eixo0\Eixo0.exe`. Os executáveis não ficam dentro do repositório porque passam de 100 MB, o limite de arquivo do GitHub.

## Documentação

- [`docs/REQUISITOS.md`](docs/REQUISITOS.md): requisitos funcionais e não funcionais, com onde cada um está no código e qual teste cobre.
- [`docs/uml/diagrama-classes.svg`](docs/uml/diagrama-classes.svg): diagrama de classes UML completo, gerado a partir do código (fonte Mermaid em `docs/uml/diagrama-classes.mmd`).

## Estrutura geral

O código fica em `src/main/java/com/jogo`, dividido em pacotes por responsabilidade:

`Main.java` é o ponto de entrada e a classe que estende `GameApplication` do FXGL. Ela só tem as configurações da janela e repassa cada etapa do FXGL (`initGame`, `initPhysics`, `initInput`, `initUI`, `onUpdate`) pro pacote `controle`. Ver "Separação MVC".

`controle` guarda os controladores: `ControleJogo` (fluxo do jogo: menu, pausa, Game Over, reiniciar, colisões de gatilho/item, arsenal inicial e tratamento das exceções do modelo), `ControleEntrada` (teclas), `Controles` (lista das teclas, mostrada nas Opções) e `GerenciadorSalas` (monta a sala, inimigos e itens, troca de sala e câmera).

`visao` guarda o que aparece na tela: `HudJogo` (barras de vida e energia, ícones dos módulos e da arma, status e avisos), as telas sobrepostas (`TelaSobreposta`, abstrata, e as filhas `TelaMenuPrincipal`, `TelaPausa`, `TelaGameOver`, `TelaInventario` e `TelaOpcoes`), a `CortinaTransicao` da troca de sala e os auxiliares `EstiloUI` e `RecursosVisuais`.

`excecoes` guarda as exceções próprias do jogo. Ver "Exceções".

`componentes` guarda as classes que definem o comportamento de cada entidade, seguindo o padrão de componentes do FXGL: em vez de uma entidade ser uma classe própria, ela é um objeto genérico do FXGL que recebe componentes anexados, e cada componente cuida de um pedaço do comportamento dela (movimento, vida, ataque, IA, e assim por diante).

`componentes/visual` guarda componentes ligados só à parte visual, sem lógica de gameplay: animação de fundo e o esqueleto de animação de sprite do personagem.

`factories` guarda `FabricaEntidades`, responsável por criar de fato as entidades do jogo (jogador, inimigos, plataformas, projéteis, gatilhos de sala) e configurar a física de cada uma.

`habilidades` guarda as habilidades com tempo de espera (cooldown) trazidas da versão barebones: `Habilidade` (abstrata), `GolpeDeEnergia` (abstrata), `BolaDeFogo` e `Apara`.

`inventario` guarda o inventário do jogador e os itens que vão nele, também trazidos do barebones: `Inventario`, `Item` (abstrata), as interfaces `Inventariavel` e `Consumivel`, o enum `Categoria` e os itens `ModuloCura` e `ModuloEnergia`.

`itens` guarda as armas, com a mesma hierarquia da versão barebones: `Arma` (abstrata, implementa `Inventariavel`), `Espada` e a filha `AdagaDupla`, `ArmaDeFogo` e as filhas dela `Shotgun`, `PistolaDuplaDePlasma` e `RifleSniper`, além da interface `Perfurante` e do `CatalogoArmas` (cria uma arma pelo nome usado no JSON). Ver "Armas, interfaces de combate e polimorfismo".

`niveis` guarda as classes que espelham a estrutura dos arquivos JSON de nível, usadas pelo Gson pra desserializar os dados de cada sala.

Os arquivos JSON com os dados de cada nível ficam em `src/main/resources/assets/levels`, um arquivo por sala (`nivel1.json` até `nivel4.json`). As texturas (imagens dos personagens, inimigos, projéteis, fundos animados) ficam em `src/main/resources/assets/textures`.

## Nomes em português

Os nomes de classes, métodos e campos do projeto estão em português, no mesmo padrão da versão barebones (por exemplo `ComponenteJogador`, `tomarDano`, `estaMorto`, `moverEsquerda`, `obterDe`). Algumas coisas ficaram em inglês de propósito: a classe `Main` (ponto de entrada, referenciada no `pom.xml`), termos de jogo como "dash", "XP", "fade" e "overlay", a string `@Spawns("inimigo_ranged")` (precisa bater com `"type": "ranged"` no JSON) e os nomes de API do FXGL e do JavaFX.

Nas classes `Dados*` de `niveis`, os campos estão em português com `@SerializedName` apontando para as chaves em inglês dos arquivos JSON, então os JSON não precisaram mudar. As chaves de `SpawnData`, que só são usadas dentro do nosso código, foram traduzidas (`"nome"`, `"vidaMaxima"`, `"dano"`, `"largura"`, `"indiceDestino"`, `"ladoEntrada"`...).

## O sistema de componentes

A base de todo personagem vivo é `ComponentePersonagem`. Ele guarda nome, vida máxima, velocidade de movimento e a vida atual, essa última como uma `IntegerProperty` do JavaFX, o que permite que a UI (a barra de vida, por exemplo) fique automaticamente amarrada ao valor sem precisar de atualização manual a cada frame. `ComponentePersonagem` também cuida de receber dano (`tomarDano`), zerando a vida no mínimo em zero e removendo a entidade do mundo quando ela morre.

`ComponenteJogador` estende `ComponentePersonagem` e adiciona tudo que é específico do jogador: movimento horizontal, pulo, dash, invencibilidade temporária depois de tomar dano, e a lógica de reação ao toque de um inimigo.

`ComponenteInimigo` também estende `ComponentePersonagem` e adiciona o que é comum a todo inimigo: dano de ataque, alcance de ataque, alcance de detecção do jogador, se é um inimigo voador, se ataca à distância, e a lógica de perseguir o alvo quando ele está dentro do alcance de detecção. Essa classe é abstrata: ela nunca é instanciada sozinha, só existe pra dar base às subclasses reais. Ela implementa as interfaces `Atacante` e `ConcedeExperiencia` (as mesmas do `Inimigo` do barebones). O `iniciarAtaque()` tem um comportamento padrão aqui, o dano direto no alvo (`aplicarDanoDireto`: acha o `ComponentePersonagem` do alvo e aplica dano, com tratamento especial se o alvo for o jogador). Já o `getExperienciaConcedida()` não é escrito em `ComponenteInimigo`, então cada subclasse é obrigada a dizer quanto XP vale. `ComponenteInimigo` também sobrescreve o `tomarDano()`: chama `super.tomarDano()` pra parte comum e, se o inimigo morreu, dá o XP dele ao jogador. A partir dela existem três subclasses, cada uma tratando o ataque de um jeito:

`ComponenteInimigoMelee` é o inimigo terrestre mais simples, sem alcance à distância. Ele não sobrescreve o `iniciarAtaque()`: herda o ataque padrão da mãe, aplicando o dano assim que o alvo está perto o suficiente. Vale 20 de XP.

`ComponenteInimigoVoador` se move livremente em duas dimensões (não só no eixo X) e, quando não está perseguindo o jogador, fica flutuando num movimento senoidal em vez de ficar parado. Ele sobrescreve o `iniciarAtaque()` reaproveitando o da mãe: chama `super.iniciarAtaque()` (o golpe normal) e depois sobe por 0,4 segundo antes de voltar a perseguir ("bate e sobe"). Vale 25 de XP.

`ComponenteInimigoDistancia` mantém distância do jogador em vez de se aproximar até o alcance de ataque corpo a corpo, e sobrescreve o `iniciarAtaque()` por inteiro, sem `super`: em vez de causar dano direto, ele cria um projétil na direção do alvo, e o dano só é aplicado de verdade se o projétil realmente acertar. Vale 35 de XP. O tempo entre ataques desse inimigo é maior que o padrão, de propósito, pra dar tempo do jogador reagir.

Por que existe um `obterDe(Entity)` estático tanto em `ComponentePersonagem` quanto em `ComponenteInimigo`: o FXGL indexa os componentes de uma entidade pela classe exata deles em tempo de execução, então pedir `entity.getComponent(ComponentePersonagem.class)` nunca encontra nada se a entidade na verdade tem um `ComponenteJogador` anexado, porque `ComponenteJogador` é uma classe diferente de `ComponentePersonagem`, mesmo sendo subclasse dela. O `obterDe` contorna isso percorrendo todos os componentes da entidade e testando com `instanceof`.

## As interfaces de combate

São as mesmas três interfaces da versão barebones, todas em `componentes`:

`Atacavel` tem `tomarDano(int quant)` e `estaMorto()`. `ComponentePersonagem` implementa essa interface. Antes ela se chamava `Danificavel` aqui no FXGL; o contrato é o mesmo, só mudou o nome pra as duas versões do projeto usarem as mesmas interfaces. A ideia é separar "saber tomar dano" de "ser um personagem vivo": um baú ou uma caixa destrutível do cenário poderia implementar `Atacavel` sem herdar de `ComponentePersonagem` e sem carregar nome, vida máxima e velocidade.

`Atacante` tem `iniciarAtaque()`. Implementada por `ComponenteInimigo` e por `ComponenteJogador` (no barebones, por `Inimigo` e `Jogador`). No barebones o método recebe a vítima (`iniciarAtaque(Personagem vitima)`), porque sem posição na tela alguém precisa dizer quem apanha. Aqui não precisa: o inimigo já guarda o alvo dele (`definirAlvo()`) e o jogador ataca pra onde está olhando.

`ConcedeExperiencia` tem `getExperienciaConcedida()`. Implementada por `ComponenteInimigo`, e cada subclasse devolve o próprio valor (melee 20, voador 25, distância 35). É ela que faz o `adicionarXp()` do jogador finalmente ser usado: matar inimigo dá XP, e com XP suficiente o jogador sobe de nível.

## Dados de nível em JSON

Os valores de balanceamento do jogo, como vida máxima, energia máxima, velocidade, dano e os módulos espalhados pela sala, não ficam escritos direto no código Java. Eles ficam nos arquivos JSON de cada nível, e são carregados em classes espelho como `DadosJogador` e `DadosInimigo`, que o Gson preenche automaticamente a partir do JSON. Esses dados então são repassados para os construtores dos componentes na hora em que `GerenciadorSalas` e `FabricaEntidades` montam a sala.

A razão principal é que as mesmas classes de componente (por exemplo `ComponenteInimigoMelee`) são reaproveitadas em várias salas diferentes, com valores diferentes em cada uma. Um inimigo melee da sala 1 pode ter vida bem menor que um da sala 4, sem precisar de uma classe Java separada pra cada variação. Isso também significa que ajustar o balanceamento do jogo (deixar um inimigo mais forte, mudar a vida do jogador) não exige recompilar nada, só editar o JSON.

Cada nível também pode definir seu próprio tema visual de fundo: a sala 1 usa um fundo animado (uma sequência de frames em loop), enquanto as outras salas usam uma cor sólida definida no próprio JSON.

## Sistema de salas

O jogo é dividido em salas, e só o conteúdo da sala atual fica carregado e visível por vez, no estilo metroidvania tradicional. A câmera fica travada nos limites da sala atual em vez de seguir livremente por um mapa contínuo. A ordem das salas é definida em `GerenciadorSalas.SALAS_DO_MAPA` e determina implicitamente qual é a vizinha da esquerda e da direita de cada uma.

Nas bordas de cada sala existem entidades gatilho, que ao serem tocadas pelo jogador disparam uma transição pra sala vizinha: a tela escurece com um fade, a sala nova é montada por trás do fade, e o jogador é reposicionado na entrada correspondente do lado oposto. O jogador em si não é recriado nessa troca, só reposicionado, então o estado dele (vida atual, por exemplo) é preservado entre salas. Nas extremidades do mapa, onde não existe mais sala vizinha, o que existe no lugar do gatilho é uma parede sólida invisível, barrando a passagem.

## Combate

O ataque do jogador começa no `iniciarAtaque()` do `ComponenteJogador` (tecla Z), que chama o `ComponenteArma`, que por sua vez manda a arma equipada atacar (ver "Armas, interfaces de combate e polimorfismo"). A `Espada` cria uma entidade de hitbox separada através de `FabricaEntidades`, usando o componente `AtaqueJogadorComponent`. Essa entidade de ataque acompanha a posição do jogador a cada frame, o que evita que a hitbox fique presa num ponto fixo do cenário caso o jogador pule ou se mova durante o próprio ataque.

Inimigos à distância, como o `ComponenteInimigoDistancia`, atacam criando uma entidade de projétil através de `FabricaEntidades`, usando o `ComponenteProjetil`. O projétil calcula a direção até o alvo uma única vez, no momento em que nasce, e depois disso viaja em linha reta nessa direção fixa, não é teleguiado. A cada frame ele ainda verifica a distância atual até o alvo só pra saber se colidiu, o que é diferente de perseguir o alvo. Se o tempo de vida do projétil passar de três segundos sem acertar nada, ele se remove sozinho.

Quando um inimigo atinge o jogador, o dano é aplicado de um jeito um pouco diferente do dano comum: em vez de só descontar a vida, o jogo chama `serAtingido` no `ComponenteJogador`, que além do dano aplica um pequeno knockback e um período de invencibilidade temporária, evitando que o jogador tome vários hits seguidos do mesmo toque.

## Controles

| Tecla | Ação |
|---|---|
| ← / → | mover |
| Espaço ou ↑ | pular (com pulo duplo no ar) |
| Shift | dash |
| Z | ataque com a arma equipada |
| Q | trocar de arma (entre as que já foram pegas) |
| X | Golpe de Energia (Bola de Fogo) |
| C | Apara (parry) |
| 1 | usar Módulo de Cura |
| 2 | usar Módulo de Energia |
| I ou TAB | abrir/fechar o inventário |
| ESC | pausar (no inventário ou nas Opções: fechar) |
| F11 | tela cheia |

A mesma lista aparece no jogo, na tela de Opções. Ela fica em `controle.Controles.LISTA`; se uma tecla mudar no `ControleEntrada`, muda lá também.

### Inventário (I / TAB)

`visao.TelaInventario` pausa o jogo e mostra três partes: status (nível, XP, vida, energia, habilidades e dificuldade), a fileira de **armas** (as cinco, na ordem do catálogo; as que ainda não foram pegas aparecem apagadas com "?", e a equipada fica com borda ciano) e a fileira de **módulos** com a quantidade. Clicar numa arma equipa (`ComponenteArma.equiparArma(id)`, exceção tratada no controle); clicar num módulo (ou apertar 1/2) usa. Passar o mouse mostra a descrição embaixo. Os ícones (24x24, desenhados por nós) ficam em `assets/textures/sprites/itens/`.

### Opções

OPTIONS no menu principal e "Opções" na pausa abrem `visao.TelaOpcoes`: **tela cheia** (liga/desliga, igual ao F11) e **dificuldade** (Fácil, Normal, Difícil), além da lista completa de controles. A dificuldade é o enum `componentes.Dificuldade`, que multiplica o dano que a jogadora recebe (0,5x / 1x / 1,5x, nunca menos de 1); vale na hora e continua valendo em "Reiniciar" e "Menu".

## Energia, habilidades e inventário

Essas partes vieram da versão barebones e seguem as mesmas regras de lá.

O jogador tem energia, além da vida. Ela aparece numa barra azul embaixo da barra de vida, amarrada à propriedade `energiaAtual` do `ComponenteJogador` do mesmo jeito que a barra de vida. A energia só muda por três métodos: `consumirEnergia`, `recuperarEnergia` (que nunca passa da máxima) e `energiaSuficiente`, usado pra conferir se dá pra usar uma habilidade. A energia máxima vem do campo `"maxEnergy"` do bloco `"player"` do JSON (50 se não vier). Diferente do barebones, aqui a energia também recarrega devagar sozinha (`RECARGA_ENERGIA_POR_SEGUNDO`, 2 pontos por segundo); pra desligar isso é só pôr 0 nessa constante.

A vida também ganhou um `curar(int)` em `ComponentePersonagem`: ignora quantidade menor ou igual a zero, não cura personagem morto e não passa da vida máxima.

Cada `Habilidade` controla o próprio tempo de espera: o `ComponenteJogador` chama `atualizar(tpf)` dela a cada frame. O método `usar()` é `final` e sempre faz a mesma sequência: confere se pode usar (aprendida, fora do cooldown e com energia), gasta a energia, chama o `executar()` da subclasse e reinicia o cooldown. Cada subclasse só diz o que faz no `executar()`. A diferença pro barebones é que aqui não existe parâmetro de alvo: no FXGL quem decide em quem o golpe acerta é a posição na tela.

`GolpeDeEnergia` é abstrata e já traz o `executar()` pronto: cria um disparo (`"disparo_jogador"` na `FabricaEntidades`, o mesmo das armas de fogo) que anda reto na direção em que o jogador está olhando. Quem move o disparo é o `ComponenteDisparoJogador`, que some ao acertar um inimigo (aplicando o dano), ao bater numa plataforma ou parede, ou depois de 1,5 segundo. `BolaDeFogo` é a subclasse concreta usada no jogo e só troca a cor do disparo (sobrescreve `getCor()`). No `ComponenteJogador` ela fica guardada numa variável do tipo `GolpeDeEnergia`, como exemplo de polimorfismo. Números (os mesmos do teste do barebones): dano 20, custo 15 de energia, espera de 2 segundos.

`Apara` abre uma janela curta (0,3 segundo) em que `serAtingido()` ignora o golpe por inteiro, sem dano e sem empurrão, tanto no toque de inimigo quanto no projétil. Durante a janela aparece um contorno ciano em volta do jogador. Custo de 5 de energia e espera de 1,5 segundo.

O `Inventario` é o mesmo do barebones e guarda tanto os módulos quanto as armas. Ele usa duas tabelas hash (`LinkedHashMap`): uma com os objetos e outra com as quantidades. Itens de categoria empilhável (como `CONSUMIVEL`) com o mesmo id são somados numa entrada só, com contador. Itens não empilháveis (como `ARMA`) ganham um id único na criação (`espada-1`, `espada-2`...), então cada um ocupa sua própria entrada.

A `Arma` implementa `Inventariavel` com categoria `ARMA`, igual no barebones. O jogo começa só com a espada (`ControleJogo.prepararArsenal()`), que entra no inventário e é equipada com `ComponenteArma.equiparArma(idArma)`, que busca a arma pelo id no inventário e recusa qualquer coisa que não seja `Arma`. As outras quatro armas e os módulos ficam espalhados pelo chão das salas, na lista `"items"` do JSON de cada uma: `"type"` vale `"cura"`, `"energia"` ou `"arma"` (com `"weapon"`: `adagas`, `shotgun`, `pistola` ou `rifle`), e `"amount"` é quanto o módulo devolve. Sala 1 tem as Adagas Duplas, a 2 a Shotgun, a 3 a Pistola e a 4 o Rifle. No mapa, cada item aparece com o ícone e um brilho (verde cura, ciano energia, amarelo arma), flutuando. Ao encostar, o `CollisionHandler(JOGADOR, ITEM)` em `ControleJogo.registrarColisoes()` cria o `ModuloCura`/`ModuloEnergia` (construtor curto, só a quantidade) ou a arma pelo `itens.CatalogoArmas.criar(nome)`, coloca no inventário e mostra uma mensagem amarela no HUD. As teclas 1 e 2 chamam `usarItem(id)`, que só funciona com `Consumivel`: chama `efeito()` e tira um do inventário. Se não tiver o módulo, `usarItem` lança `ItemNaoEncontradoException`, e o controle mostra a mensagem em vermelho no HUD (ver "Exceções"). Embaixo das barras, o HUD mostra os ícones dos módulos com a quantidade e o ícone da arma equipada. `GerenciadorSalas` guarda quais itens já foram pegos (`itensColetados`), pra eles não reaparecerem quando o jogador volta pra uma sala; essa lista zera em "Reiniciar" e "Menu Principal".

## Armas, interfaces de combate e polimorfismo

As armas seguem a mesma hierarquia da versão barebones:

```
Arma (abstrata, implementa Inventariavel)
├── Espada
│   └── AdagaDupla
└── ArmaDeFogo
    ├── Shotgun
    ├── PistolaDuplaDePlasma
    └── RifleSniper (implementa Perfurante)
```

Antes existia uma classe `Arma` só, com um enum `TipoArma` (`MELEE`, `DISTANCIA`, `MAGIA`), e o `ComponenteArma` decidia o ataque num `switch`. Agora quem decide é a própria arma: `Arma.atacar(portador, direcaoX)` é `final` (a regra do cooldown é igual pra todas) e chama `executarAtaque()`, que é abstrato e cada subclasse escreve do seu jeito. O `ComponenteArma` só chama `armaEquipada.atacar(...)`, sem saber que arma é. Uma arma nova é uma classe nova, sem mexer no `ComponenteArma`.

- `Espada`: cria a hitbox `"ataque_jogador"` na frente do jogador (o antigo `case MELEE`). Corte de 20.
- `AdagaDupla`: filha da `Espada`; sobrescreve `executarAtaque()` chamando `super.executarAtaque()` duas vezes (uma lâmina em cada mão). 2 cortes de 8, alcance curto (30), rápida (0,3 s).
- `ArmaDeFogo`: um projétil reto (`"disparo_jogador"`, movido pelo `ComponenteDisparoJogador`) que some quando percorre o alcance da arma. Ela tem métodos-gancho que as filhas podem sobrescrever: quantos alvos o projétil acerta, velocidade, tamanho e cor.
- `Shotgun`: sobrescreve `executarAtaque()` e chama `super.executarAtaque()` pro balim do meio; os outros saem em leque, 9 graus acima e abaixo. 5 balins de 8, alcance curto (220).
- `PistolaDuplaDePlasma`: sobrescreve `executarAtaque()` chamando `super.executarAtaque()` duas vezes, a segunda 0,12 segundo depois (timer `runOnce` do FXGL). 2 tiros de 9, magenta.
- `RifleSniper`: implementa `Perfurante` e sobrescreve só o gancho de quantos alvos o tiro acerta, devolvendo `getAlvosAtravessados()`. O projétil guarda quais inimigos já acertou e só some depois de 3. Tiro de 30, rápido e de longo alcance.

Cada arma também diz como a jogadora a segura (`getEmpunhadura()`, que escolhe as animações; ver "Sprites e animações") e descreve o próprio ataque (`getDescricaoCombate()`), mostrado no HUD.

**Coleção polimórfica:** as armas pegas ficam no inventário, e o `ComponenteArma.getArsenal()` devolve todas numa `List<Arma>`, apesar de cada uma ser de uma classe diferente. A tecla Q passa pra próxima arma da lista, e a tecla Z faz sempre a mesma chamada (`atacar`), que dá um resultado diferente pra cada arma. Ao começar o jogo, o `ControleJogo` percorre as cinco armas do `CatalogoArmas` e imprime no console a `getDescricaoCombate()` de cada arma: mesma chamada, resposta diferente pra cada classe, o equivalente ao `foreach` de `atacar()` da atividade de RPG. A lista vem de `Inventario.listarDoTipo(Arma.class)`, um método genérico (`<T> List<T> listarDoTipo(Class<T> tipo)`) que já devolve a lista no tipo pedido, sem cast.

**Uso de `super`:** além do `super(...)` nos construtores, o projeto chama a versão da classe mãe dentro de métodos sobrescritos em quatro lugares: `ComponenteInimigoVoador.iniciarAtaque()`, `ComponenteInimigo.tomarDano()`, `Shotgun.executarAtaque()` e `PistolaDuplaDePlasma.executarAtaque()` (este último também dentro de uma lambda). Nos quatro casos a subclasse reaproveita o comportamento da mãe e acrescenta o dela, como o `parent::atacar()` do Paladino na atividade de RPG.

## Movimento e dash

O jogador se move para os lados com as setas esquerda e direita. O pulo é feito pela barra de espaço ou pela seta para cima. O dash é acionado com Shift e dá um impulso rápido de velocidade horizontal na direção em que o jogador está olhando, com um pequeno período de invencibilidade e um tempo de recarga antes de poder ser usado de novo.

Enquanto o dash está em andamento, os comandos normais de movimento não têm efeito, pra evitar que o jogador consiga cancelar ou alterar o impulso do dash segurando uma tecla de movimento ao mesmo tempo. Quando o impulso termina, a velocidade horizontal é zerada de forma abrupta e imediata, de propósito, sem nenhuma desaceleração suave: essa é a sensação de dash comum em metroidvanias do gênero, onde o movimento é seco e responsivo em vez de ter um deslize gradual no final.

Se o jogador continuar segurando Shift depois que o dash termina, a velocidade de movimento normal recebe um pequeno bônus enquanto a tecla continuar pressionada. O bônus é discreto, só o suficiente pra fazer diferença, e some assim que Shift é solto.

Esse controle de Shift não usa o sistema comum de bind de tecla do FXGL (`addAction`), porque o FXGL proíbe vincular ações diretamente a teclas modificadoras como Shift, Control e Alt. Em vez disso, o jogo usa um `TriggerListener`, que é notificado sempre que qualquer tecla é pressionada ou solta, e reage especificamente quando a tecla em questão é o Shift.

## Física e colisão

A física do jogo é toda baseada no Box2D, através do `PhysicsComponent` do FXGL. Jogador e inimigos usam corpos dinâmicos, plataformas e paredes usam corpos estáticos. Existe um sensor de chão embaixo do jogador usado pra saber se ele está no ar ou não, o que determina se o pulo pode ser usado.

O contato de dano entre jogador e inimigos usa uma combinação de duas fixtures: uma fixture física normal, que faz o jogador colidir e ser bloqueado pelo inimigo como um obstáculo sólido, e uma fixture sensora separada, que detecta o toque sem gerar colisão física, usada especificamente pra disparar o dano. Categorias e máscaras de colisão (`Filter.categoryBits` e `maskBits`) são usadas pra controlar quais tipos de entidade colidem fisicamente entre si e quais só se detectam sem colidir.

## Interface e menus

A UI é montada em `ControleJogo.montarInterface()` (chamado pelo `initUI()`), com as classes do pacote `visao`. A barra de vida do `HudJogo` fica amarrada à propriedade de vida do `ComponenteJogador` e à vida máxima: a largura é `vidaAtual * 200 / vidaMaxima`, recalculada sempre que a vida muda. Antes a conta usava a vida máxima do começo do jogo, então depois de subir de nível (vida máxima +20) a barra passava do tamanho. As três telas (menu principal, pausa e Game Over) estendem `TelaSobreposta`, que já traz `mostrar()`, `esconder()` e `estaVisivel()`; cada uma recebe no construtor o que fazer em cada botão (`Runnable`), então a visão não conhece o controle.

## Pausa, Game Over e o "período de graça" ao despausar

O jogo tem três telas que pausam o motor do FXGL (`getGameController().pauseEngine()`): o menu principal (no início), o menu de pausa (ESC) e a tela de Game Over. Os botões "Reiniciar" (Game Over) e "Menu Principal" (Game Over e pausa) usam `startNewGame()` por baixo dos panos pra reconstruir o nível do zero, sempre voltando pro início do mapa (não existe sistema de checkpoint ainda). A diferença entre os dois é só se o menu principal aparece de novo depois ou não: "Reiniciar" volta direto pra dentro do jogo, "Menu Principal" mostra o menu de novo antes.

Sempre que o motor é despausado, em vez de chamar `getGameController().resumeEngine()` direto, o código passa por um método próprio, `ControleJogo.despausarMotor()`. O motivo: quando o jogador fica parado numa tela pausada por um tempo (alguns segundos ou mais), o FXGL tenta recuperar esse tempo processando vários ticks em sequência bem rápida assim que o motor despausa, e isso fazia tudo que depende do tempo entre frames (câmera, o "flutuar" dos inimigos voadores, os timers do dash) avançar rápido demais por um instante, dando a impressão de que o jogo acelerou por alguns segundos antes de se estabilizar. `despausarMotor()` marca um "período de graça" bem curto (0.4 segundo de tempo real) em `RelogioJogo.marcarRetomada()`, e o `ControleJogo.atualizar()`, o `ComponenteJogador` e o `ComponenteInimigo` (e subclasses) perguntam `RelogioJogo.emPeriodoDeGraca()` e ignoram esse instante inicial, deixando o FXGL recuperar esse atraso sem que nada visível reaja a isso.

Existia um bug maior, relacionado mas separado: "Reiniciar" e "Menu Principal" acabavam sempre voltando pro menu principal, mesmo o "Reiniciar" nunca devendo mostrar o menu. A causa era uma suposição errada de que `initUI()` só roda uma vez, no começo do app. Na prática, o FXGL chama `initGame()` + `initPhysics()` + `initUI()` de novo toda vez que `getGameController().startNewGame()` é chamado, ou seja, em todo "Reiniciar" e todo "Menu Principal" também. Como `initUI()` reconstruía o `overlayMenuPrincipal` do zero e ele fica visível por padrão (diferente do overlay de Game Over e do de pausa, que são escondidos explicitamente na hora de criar), esse menu recém-criado reaparecia por cima de qualquer coisa, e o motor era pausado de novo, independente de qual botão o jogador tivesse apertado.

A primeira correção guardava só "já passamos do primeiro launch" numa flag, e deixava `reiniciarNivel()`/`voltarAoMenuPrincipal()` mexerem no `overlayMenuPrincipal` e chamarem `pauseEngine()` depois de chamar `startNewGame()`. Isso resolveu o "Reiniciar", mas o "Menu"/"Menu Principal" continuou com um bug parecido: o nível reiniciava (dava pra ver o jogo voltando pro início), mas o menu principal nunca aparecia, e o jogo travava sem responder mais a input. A causa provável: não tem confirmação, na fonte do FXGL, de que `startNewGame()` termina o reset do mundo de forma síncrona antes de devolver o controle pro código que chamou. Se não for síncrono, o `pauseEngine()` chamado logo em seguida em `voltarAoMenuPrincipal()` podia pausar o motor antes do reset de verdade acontecer, ou mexer num `overlayMenuPrincipal` que já tinha virado obsoleto.

A correção definitiva move essa decisão pra dentro do próprio `initUI()` (hoje `ControleJogo.montarInterface()`): por padrão, toda vez que ele roda, ele mostra o menu principal e pausa o motor sozinho (certo tanto pro launch do app quanto pro "Menu"/"Menu Principal", que só chamam `startNewGame()` sem mexer em mais nada). Existe uma flag, `pularMenuAoIniciar`, que só o `reiniciarNivel()` liga (antes de chamar `startNewGame()`), avisando "dessa vez não, quero cair direto no jogo"; o próprio `initUI()` desliga a flag de novo depois de consumir. Assim o "mostra o menu e pausa" sempre roda exatamente no momento em que o mundo novo é de fato montado, não importa quando isso aconteça.

De quebra, o ESC ganhou um terceiro guard (hoje em `ControleJogo.podePausar()`): além de não abrir a pausa se o player estiver morto/inativo ou o Game Over estiver na tela, agora também não abre se o menu principal estiver visível — sem isso, como o player novo já nasce ativo assim que o "Menu"/"Menu Principal" termina, apertar ESC bem nesse instante abria a tela de pausa por cima do próprio menu principal.

## Separação MVC

Antes o `Main` tinha cerca de 1.400 linhas e misturava telas, HUD, teclas, troca de sala e regras. Agora cada parte tem seu pacote:

| Camada | Pacotes | O que faz |
|---|---|---|
| Modelo | `componentes`, `itens`, `habilidades`, `inventario`, `niveis`, `entidades`, `factories`, `excecoes` | dados e regras do jogo: vida, energia, XP, armas, inventário, salas |
| Visão | `visao`, `componentes.visual` | o que aparece na tela: HUD, telas, cortina, animações |
| Controle | `controle` (+ `Main`) | liga as duas: teclas, fluxo menu/pausa/Game Over, troca de sala, trata exceções |

O `Main` só repassa cada etapa do FXGL pro controle. O modelo não depende do controle (por isso o `RelogioJogo` fica em `componentes`: quem lê são os componentes, o controle só avisa quando despausou). A visão também não conhece o controle: as telas recebem `Runnable` pros botões e o HUD só lê o modelo.

O FXGL não deixa uma separação 100% pura: os componentes de gameplay herdam de `Component` do FXGL e criam entidades (tiro, hitbox) com `spawn()`. Por isso os testes cobrem melhor as partes do modelo que não dependem do motor.

## Exceções

As exceções próprias ficam em `excecoes`:

```
Exception
└── JogoException (checked, mãe de todas)
    ├── ItemNaoEncontradoException   (id que não está no inventário)
    ├── ItemIncompativelException    (ex.: tentar consumir uma arma)
    └── NivelInvalidoException       (JSON de sala faltando, vazio ou quebrado)
```

São "checked" (herdam de `Exception`): quem chama é obrigado a tratar ou repassar. Onde são lançadas e onde são tratadas:

- `ComponenteJogador.usarItem(id)` e `ComponenteArma.equiparArma(id)` lançam `ItemNaoEncontradoException`/`ItemIncompativelException`. O `ControleJogo.usarModulo()` captura `JogoException` e mostra a mensagem no HUD (`HudJogo.mostrarAviso()`), em vez de derrubar o jogo.
- `CarregadorDeNiveis.carregar()` lança `NivelInvalidoException` se o arquivo não existe, está vazio, não tem `player`/`platforms` ou o JSON está quebrado (a exceção do Gson vai junto como causa). Sala sem `enemies` é válida e vira lista vazia.
- Erros de programação (vida máxima zero, dano negativo, nome vazio) usam `IllegalArgumentException` nos construtores, porque não são situações que o jogo deveria tratar, e sim bugs.

## Testes

70 testes JUnit 5 em `src/test/java`, todos passando. Rodam com `mvn test`.

- Unitários: `ComponenteAnimadoTest` (qual animação mostrar em cada situação, ampliação dos sprites), `ComponenteJogadorTest` (energia, cura, invencibilidade, subir de nível, usar módulos), `ComponenteInimigoTest` (XP de cada tipo, interfaces), `ArmaTest` (cooldown do `atacar()` com uma arma de teste, `multiplicarDano`, ids únicos), `ArmasDeFogoTest` (ganchos de cada arma, `Perfurante`, coleção polimórfica), `HabilidadeTest`, `InventarioTest` (pilha, duplicata, `listarDoTipo` genérico, sobrecargas), `ComponenteAuxiliaresTest`, `DificuldadeTest` (multiplicador de dano de cada dificuldade), `CatalogoArmasTest` (arma certa pra cada nome do JSON).
- Casos de sucesso e de falha: cada classe testa o caminho certo e os errados (sem energia, em cooldown, item que não existe, valores inválidos no construtor).
- Exceções: `assertThrows` para cada exceção própria e para as `IllegalArgumentException` dos construtores.
- Integração: `CarregadorDeNiveisTest` lê os JSON de verdade das 4 salas com o Gson (e JSON de teste quebrado/incompleto em `src/test/resources`); `JogadorSalaIntegracaoTest` carrega a sala 4, cria o jogador com os dados dela, coleta os módulos e o rifle do chão, soma o XP dos inimigos, sobe de nível e usa os módulos até dar a exceção.

Cobertura (JaCoCo): `inventario` 96% das linhas, `niveis` 95%, `excecoes` 100%. O total do projeto fica baixo (cerca de 20%) porque `visao`, `controle` e boa parte de `componentes` só funcionam com a janela e o motor do FXGL rodando; essas partes foram testadas jogando.

## Sprites e animações

Os sprites ficam em `src/main/resources/assets/textures/sprites`, uma pasta por personagem, com os quadros numerados (`correr-1.png`, `correr-2.png`...). A origem e a licença de cada um estão em `CREDITOS.txt` nessa pasta:

- `jogador`, `drone` (o inimigo voador) e `explosao`: do pacote Warped City, de ansimuz, CC0 (domínio público). As animações com espada, adagas e arma vieram em folhas de sprite ampliadas do pacote e foram reduzidas ao tamanho original; os quadros de "parado", "cair" e "atirar correndo" foram montados a partir delas.
- `robo-melee` e `robo-atirador`: desenhados para este projeto, na paleta do Warped City.

| Personagem | Animações |
|---|---|
| Jogadora (um conjunto por arma: espada, adagas, arma de fogo) | parado, correr, pular, cair, atacar parado, atacar correndo, atacar no ar, dano |
| Robô melee | parado, andar, ataque com a garra, dano |
| Robô atirador | parado, andar na esteira, tiro com recuo, dano |
| Drone | voar (vale pra parado e andando), dano |
| Jogadora (todas as armas) | dash (rastro ciano), conjurar magia (aura e orbe na mão) |
| Golpe de Energia | Bola de Fogo (orbe laranja com rastro); o golpe padrão é um orbe ciano |
| Apara | escudo hexagonal ciano em volta da jogadora |
| Tiros e magias | faísca no ponto do acerto |
| Todo inimigo | explode ao morrer |

O cenário também usa o Warped City: as plataformas flutuantes e o chão são montados com peças do tileset (`sprites/cenario`, repetidas com `ImagePattern` em `FabricaEntidades.visualPlataforma()`), e as salas 2 a 4 têm o fundo animado `cidade` (12 quadros com os letreiros de neon piscando). A sala 1 continua com o laboratório (`cryolab`). O fundo de cada sala é o campo `"background"` do JSON.

A jogadora segura a arma equipada no próprio sprite: cada arma diz como é segurada em `Arma.getEmpunhadura()` (`"espada"`, `"adagas"` ou `"arma"`; Shotgun, Pistola e Rifle herdam `"arma"` da `ArmaDeFogo`), e trocar de arma com Q troca o conjunto de animações. Por isso as armas não desenham mais um retângulo/imagem por cima da jogadora (o antigo `criarVisual()`).

Quem escolhe a animação é o `ComponenteAnimado` (`componentes/visual`), que só lê o estado do personagem: está machucado? atacando? no chão? subindo ou caindo? andando? A regra de prioridade está em `escolherEstado()` (dano vence ataque, ataque vence pulo, pulo vence andar) e é testada sozinha. Se um personagem não tem a animação pedida, `animacaoDisponivel()` usa a mais parecida (o drone não tem "pular", então usa "voar"). Olhar pra esquerda é o mesmo desenho espelhado.

Os quadros são pixel art pequeno e são carregados 1,5x maiores pelo `Sprites`, pixel a pixel, porque ampliar na tela deixaria o desenho borrado. Por isso as caixas de colisão mudaram: jogadora 36x72, robô melee 42x60, robô atirador 45x54, drone 48x48 (antes tudo 32x32).

Pra trocar um sprite, basta substituir os arquivos mantendo os nomes; pra mudar quantos quadros ou a velocidade, é a chamada `Sprites.animacao(pasta, nome, quadros, segundos, escala)` em `FabricaEntidades`.

## Observações sobre o estado atual do projeto

Este README é atualizado conforme o projeto muda. Qualquer alteração de código que mude comportamento, adicione sistema novo ou torne alguma explicação aqui desatualizada deve vir acompanhada da atualização correspondente neste arquivo.
