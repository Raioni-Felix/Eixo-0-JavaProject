# Eixo-0

Trabalho de POO em Java, um metroidvania 2D feito com FXGL. O jogador explora salas conectadas, enfrenta inimigos com padrões de comportamento diferentes, usa um ataque corpo a corpo e um dash, e avança entre salas através de gatilhos nas bordas do mapa.

## Tecnologias

O projeto usa Java 17, FXGL 17.3 (framework em cima do JavaFX voltado pra jogos 2D, cuida de física, câmera, input, entidades e UI) e Gson 2.10.1, usado só pra carregar os dados dos níveis a partir de arquivos JSON. O build é feito com Maven.

## Como rodar

Com o Maven configurado, o comando

```
mvn compile exec:java
```

compila o projeto e executa a classe principal, `com.jogo.Main`, que é a que o `exec-maven-plugin` está configurado pra rodar no `pom.xml`. Também dá pra rodar `Main.java` direto pela IDE, contanto que as dependências do Maven já tenham sido baixadas.

## Estrutura geral

O código fica em `src/main/java/com/jogo`, dividido em pacotes por responsabilidade:

`Main.java` é o ponto de entrada e a classe que estende `GameApplication` do FXGL. É onde ficam as configurações da janela, o carregamento inicial do jogo, os binds de teclado, a montagem da UI (barra de vida, menus, tela de game over) e a lógica de troca de salas.

`componentes` guarda as classes que definem o comportamento de cada entidade, seguindo o padrão de componentes do FXGL: em vez de uma entidade ser uma classe própria, ela é um objeto genérico do FXGL que recebe componentes anexados, e cada componente cuida de um pedaço do comportamento dela (movimento, vida, ataque, IA, e assim por diante).

`componentes/visual` guarda componentes ligados só à parte visual, sem lógica de gameplay: animação de fundo e o esqueleto de animação de sprite do personagem.

`factories` guarda `FabricaEntidades`, responsável por criar de fato as entidades do jogo (jogador, inimigos, plataformas, projéteis, gatilhos de sala) e configurar a física de cada uma.

`itens` guarda as classes relacionadas a armas, hoje representadas pela classe `Weapon` e pelo enum `WeaponType`.

`niveis` guarda as classes que espelham a estrutura dos arquivos JSON de nível, usadas pelo Gson pra desserializar os dados de cada sala.

Os arquivos JSON com os dados de cada nível ficam em `src/main/resources/assets/levels`, um arquivo por sala (`nivel1.json` até `nivel4.json`). As texturas (imagens dos personagens, inimigos, projéteis, fundos animados) ficam em `src/main/resources/assets/textures`.

## O sistema de componentes

A base de todo personagem vivo é `CharacterComponent`. Ele guarda nome, vida máxima, velocidade de movimento e a vida atual, essa última como uma `IntegerProperty` do JavaFX, o que permite que a UI (a barra de vida, por exemplo) fique automaticamente amarrada ao valor sem precisar de atualização manual a cada frame. `CharacterComponent` também cuida de receber dano (`takeDamage`), zerando a vida no mínimo em zero e removendo a entidade do mundo quando ela morre.

`PlayerComponent` estende `CharacterComponent` e adiciona tudo que é específico do jogador: movimento horizontal, pulo, dash, invencibilidade temporária depois de tomar dano, e a lógica de reação ao toque de um inimigo.

`EnemyComponent` também estende `CharacterComponent` e adiciona o que é comum a todo inimigo: dano de ataque, alcance de ataque, alcance de detecção do jogador, se é um inimigo voador, se ataca à distância, e a lógica de perseguir o alvo quando ele está dentro do alcance de detecção. Essa classe é abstrata: ela nunca é instanciada sozinha, só existe pra dar base às subclasses reais, e o método `attack(Entity target)` nela é abstrato de propósito, sem corpo, obrigando cada subclasse a dizer explicitamente como o próprio ataque funciona. A lógica de causar dano direto (achar o `CharacterComponent` do alvo e aplicar dano nele, com tratamento especial se o alvo for o jogador) fica disponível num método auxiliar, `applyDirectDamage`, que qualquer subclasse pode chamar de dentro do seu próprio `attack()`. A partir dela existem três subclasses, cada uma ajustando os parâmetros do construtor e implementando `attack()` do seu jeito:

`MeleeEnemyComponent` é o inimigo terrestre mais simples, sem alcance à distância. O `attack()` dele só chama `applyDirectDamage`, aplicando o dano assim que o alvo está perto o suficiente.

`FlyingEnemyComponent` se move livremente em duas dimensões (não só no eixo X) e, quando não está perseguindo o jogador, fica flutuando num movimento senoidal em vez de ficar parado. O `attack()` dele também usa `applyDirectDamage`, igual ao inimigo melee, só muda a forma como ele se movimenta até chegar perto.

`RangedEnemyComponent` mantém distância do jogador em vez de se aproximar até o alcance de ataque corpo a corpo, e o `attack()` dele não usa `applyDirectDamage`: em vez de causar dano direto, ele cria um projétil na direção do alvo, e o dano só é aplicado de verdade se o projétil realmente acertar. O tempo entre ataques desse inimigo é maior que o padrão, de propósito, pra dar tempo do jogador reagir.

Por que existe um `getFrom(Entity)` estático tanto em `CharacterComponent` quanto em `EnemyComponent`: o FXGL indexa os componentes de uma entidade pela classe exata deles em tempo de execução, então pedir `entity.getComponent(CharacterComponent.class)` nunca encontra nada se a entidade na verdade tem um `PlayerComponent` anexado, porque `PlayerComponent` é uma classe diferente de `CharacterComponent`, mesmo sendo subclasse dela. O `getFrom` contorna isso percorrendo todos os componentes da entidade e testando com `instanceof`.

## A interface Danificavel

Existe uma interface chamada `Danificavel`, em `componentes`, com dois métodos: `takeDamage(int quant)` e `isDead()`. `CharacterComponent` implementa essa interface, e como ele já tinha esses dois métodos escritos, não precisou mudar nada na lógica, só declarar que cumpre o contrato.

A ideia por trás dela é separar "saber tomar dano" de "ser um personagem vivo". Hoje só quem estende `CharacterComponent` consegue receber dano, e isso significa carregar junto nome, vida máxima e velocidade de movimento, coisas que fazem sentido pra um personagem mas não pra, por exemplo, um baú ou uma caixa destrutível do cenário. Se esse tipo de objeto entrar no jogo no futuro, ele pode implementar `Danificavel` diretamente, sem herdar de `CharacterComponent` e sem carregar nada que não precisa.

## Dados de nível em JSON

Os valores de balanceamento do jogo, como vida máxima, velocidade e dano, não ficam escritos direto no código Java. Eles ficam nos arquivos JSON de cada nível, e são carregados em classes espelho como `DadosJogador` e `DadosInimigo`, que o Gson preenche automaticamente a partir do JSON. Esses dados então são repassados para os construtores dos componentes na hora em que `Main` e `FabricaEntidades` montam a sala.

A razão principal é que as mesmas classes de componente (por exemplo `MeleeEnemyComponent`) são reaproveitadas em várias salas diferentes, com valores diferentes em cada uma. Um inimigo melee da sala 1 pode ter vida bem menor que um da sala 4, sem precisar de uma classe Java separada pra cada variação. Isso também significa que ajustar o balanceamento do jogo (deixar um inimigo mais forte, mudar a vida do jogador) não exige recompilar nada, só editar o JSON.

Cada nível também pode definir seu próprio tema visual de fundo: a sala 1 usa um fundo animado (uma sequência de frames em loop), enquanto as outras salas usam uma cor sólida definida no próprio JSON.

## Sistema de salas

O jogo é dividido em salas, e só o conteúdo da sala atual fica carregado e visível por vez, no estilo metroidvania tradicional. A câmera fica travada nos limites da sala atual em vez de seguir livremente por um mapa contínuo. A ordem das salas é definida em `Main` e determina implicitamente qual é a vizinha da esquerda e da direita de cada uma.

Nas bordas de cada sala existem entidades gatilho, que ao serem tocadas pelo jogador disparam uma transição pra sala vizinha: a tela escurece com um fade, a sala nova é montada por trás do fade, e o jogador é reposicionado na entrada correspondente do lado oposto. O jogador em si não é recriado nessa troca, só reposicionado, então o estado dele (vida atual, por exemplo) é preservado entre salas. Nas extremidades do mapa, onde não existe mais sala vizinha, o que existe no lugar do gatilho é uma parede sólida invisível, barrando a passagem.

## Combate

O ataque corpo a corpo do jogador é iniciado por `WeaponComponent`, que ao ser acionado cria uma entidade de hitbox separada através de `FabricaEntidades`, usando o componente `AtaqueJogadorComponent`. Essa entidade de ataque acompanha a posição do jogador a cada frame, o que evita que a hitbox fique presa num ponto fixo do cenário caso o jogador pule ou se mova durante o próprio ataque.

Inimigos à distância, como o `RangedEnemyComponent`, atacam criando uma entidade de projétil através de `FabricaEntidades`, usando o `ProjectileComponent`. O projétil calcula a direção até o alvo uma única vez, no momento em que nasce, e depois disso viaja em linha reta nessa direção fixa, não é teleguiado. A cada frame ele ainda verifica a distância atual até o alvo só pra saber se colidiu, o que é diferente de perseguir o alvo. Se o tempo de vida do projétil passar de três segundos sem acertar nada, ele se remove sozinho.

Quando um inimigo atinge o jogador, o dano é aplicado de um jeito um pouco diferente do dano comum: em vez de só descontar a vida, o jogo chama `hitBy` no `PlayerComponent`, que além do dano aplica um pequeno knockback e um período de invencibilidade temporária, evitando que o jogador tome vários hits seguidos do mesmo toque.

## Movimento e dash

O jogador se move para os lados com as setas esquerda e direita. O pulo é feito pela barra de espaço ou pela seta para cima. O dash é acionado com Shift e dá um impulso rápido de velocidade horizontal na direção em que o jogador está olhando, com um pequeno período de invencibilidade e um tempo de recarga antes de poder ser usado de novo.

Enquanto o dash está em andamento, os comandos normais de movimento não têm efeito, pra evitar que o jogador consiga cancelar ou alterar o impulso do dash segurando uma tecla de movimento ao mesmo tempo. Quando o impulso termina, a velocidade horizontal é zerada de forma abrupta e imediata, de propósito, sem nenhuma desaceleração suave: essa é a sensação de dash comum em metroidvanias do gênero, onde o movimento é seco e responsivo em vez de ter um deslize gradual no final.

Se o jogador continuar segurando Shift depois que o dash termina, a velocidade de movimento normal recebe um pequeno bônus enquanto a tecla continuar pressionada. O bônus é discreto, só o suficiente pra fazer diferença, e some assim que Shift é solto.

Esse controle de Shift não usa o sistema comum de bind de tecla do FXGL (`addAction`), porque o FXGL proíbe vincular ações diretamente a teclas modificadoras como Shift, Control e Alt. Em vez disso, o jogo usa um `TriggerListener`, que é notificado sempre que qualquer tecla é pressionada ou solta, e reage especificamente quando a tecla em questão é o Shift.

## Física e colisão

A física do jogo é toda baseada no Box2D, através do `PhysicsComponent` do FXGL. Jogador e inimigos usam corpos dinâmicos, plataformas e paredes usam corpos estáticos. Existe um sensor de chão embaixo do jogador usado pra saber se ele está no ar ou não, o que determina se o pulo pode ser usado.

O contato de dano entre jogador e inimigos usa uma combinação de duas fixtures: uma fixture física normal, que faz o jogador colidir e ser bloqueado pelo inimigo como um obstáculo sólido, e uma fixture sensora separada, que detecta o toque sem gerar colisão física, usada especificamente pra disparar o dano. Categorias e máscaras de colisão (`Filter.categoryBits` e `maskBits`) são usadas pra controlar quais tipos de entidade colidem fisicamente entre si e quais só se detectam sem colidir.

## Interface e menus

A UI é construída em `Main.initUI()`. A barra de vida do jogador fica amarrada diretamente à propriedade de vida do `PlayerComponent`, atualizando sozinha sempre que a vida muda. Existem overlays separados para o menu principal, a tela de pausa e a tela de game over, cada um com seus próprios botões e, no caso do menu principal, alguma animação decorativa no título.

## Pausa, Game Over e o "período de graça" ao despausar

O jogo tem três telas que pausam o motor do FXGL (`getGameController().pauseEngine()`): o menu principal (no início), o menu de pausa (ESC) e a tela de Game Over. Os botões "Reiniciar" (Game Over) e "Menu Principal" (Game Over e pausa) usam `startNewGame()` por baixo dos panos pra reconstruir o nível do zero, sempre voltando pro início do mapa (não existe sistema de checkpoint ainda). A diferença entre os dois é só se o menu principal aparece de novo depois ou não: "Reiniciar" volta direto pra dentro do jogo, "Menu Principal" mostra o menu de novo antes.

Sempre que o motor é despausado, em vez de chamar `getGameController().resumeEngine()` direto, o código passa por um método próprio, `despausarMotor()`. O motivo: quando o jogador fica parado numa tela pausada por um tempo (alguns segundos ou mais), o FXGL tenta recuperar esse tempo processando vários ticks em sequência bem rápida assim que o motor despausa, e isso fazia tudo que depende do tempo entre frames (câmera, o "flutuar" dos inimigos voadores, os timers do dash) avançar rápido demais por um instante, dando a impressão de que o jogo acelerou por alguns segundos antes de se estabilizar. `despausarMotor()` marca um "período de graça" bem curto (0.4 segundo de tempo real) logo depois de despausar, e o `onUpdate` do `Main`, do `PlayerComponent` e do `EnemyComponent` (e subclasses) ignoram esse instante inicial, deixando o FXGL recuperar esse atraso sem que nada visível reaja a isso.

Existia um bug maior, relacionado mas separado: "Reiniciar" e "Menu Principal" acabavam sempre voltando pro menu principal, mesmo o "Reiniciar" nunca devendo mostrar o menu. A causa era uma suposição errada de que `initUI()` só roda uma vez, no começo do app. Na prática, o FXGL chama `initGame()` + `initPhysics()` + `initUI()` de novo toda vez que `getGameController().startNewGame()` é chamado, ou seja, em todo "Reiniciar" e todo "Menu Principal" também. Como `initUI()` reconstruía o `mainMenuOverlay` do zero e ele fica visível por padrão (diferente do overlay de Game Over e do de pausa, que são escondidos explicitamente na hora de criar), esse menu recém-criado reaparecia por cima de qualquer coisa, e o motor era pausado de novo, independente de qual botão o jogador tivesse apertado.

A primeira correção guardava só "já passamos do primeiro launch" numa flag, e deixava `restartLevel()`/`quitToMainMenu()` mexerem no `mainMenuOverlay` e chamarem `pauseEngine()` depois de chamar `startNewGame()`. Isso resolveu o "Reiniciar", mas o "Menu"/"Menu Principal" continuou com um bug parecido: o nível reiniciava (dava pra ver o jogo voltando pro início), mas o menu principal nunca aparecia, e o jogo travava sem responder mais a input. A causa provável: não tem confirmação, na fonte do FXGL, de que `startNewGame()` termina o reset do mundo de forma síncrona antes de devolver o controle pro código que chamou. Se não for síncrono, o `pauseEngine()` chamado logo em seguida em `quitToMainMenu()` podia pausar o motor antes do reset de verdade acontecer, ou mexer num `mainMenuOverlay` que já tinha virado obsoleto.

A correção definitiva move essa decisão pra dentro do próprio `initUI()`: por padrão, toda vez que `initUI()` roda, ele mostra o menu principal e pausa o motor sozinho (certo tanto pro launch do app quanto pro "Menu"/"Menu Principal", que só chamam `startNewGame()` sem mexer em mais nada). Existe uma flag, `pularMenuAoIniciar`, que só o `restartLevel()` liga (antes de chamar `startNewGame()`), avisando "dessa vez não, quero cair direto no jogo"; o próprio `initUI()` desliga a flag de novo depois de consumir. Assim o "mostra o menu e pausa" sempre roda exatamente no momento em que o mundo novo é de fato montado, não importa quando isso aconteça.

De quebra, o ESC (`initInput()`) ganhou um terceiro guard: além de não abrir a pausa se o player estiver morto/inativo ou o Game Over estiver na tela, agora também não abre se `mainMenuOverlay` estiver visível — sem isso, como o player novo já nasce ativo assim que o "Menu"/"Menu Principal" termina, apertar ESC bem nesse instante abria a tela de pausa por cima do próprio menu principal.

## Observações sobre o estado atual do projeto

Existe um componente `AnimatedComponent`, em `componentes/visual`, pensado pra trocar a textura do personagem conforme o estado dele (parado, andando, pulando, tomando dano), mas a troca de textura em si ainda não foi implementada, é um ponto em aberto pra quando existirem os spritesheets necessários.

Este README é atualizado conforme o projeto muda. Qualquer alteração de código que mude comportamento, adicione sistema novo ou torne alguma explicação aqui desatualizada deve vir acompanhada da atualização correspondente neste arquivo.
