# Resumo — Eixo 0 (projeto Java/FXGL)

Projeto: metroidvania 2D em Java 17 + FXGL 17.3, pacote `com.jogo`, em
`/home/lincoln/_UNIVASF/CODE/Java/Eixo0/Eixo-0-JavaProject`, acessado
via ponte com o computador do usuário (sem `device_bash` disponível,
só stage/commit de arquivos).

## O que foi feito nesta conversa

1. **README.md**: criado do zero (o projeto só tinha um placeholder),
   com visão geral, stack, como rodar, arquitetura, sistema de
   componentes, combate, movimento/dash, física, UI e menus. Deve
   continuar sendo atualizado a cada mudança relevante (instrução
   permanente do usuário: sem travessão duplo/em-dash, português
   natural, sem "cara de IA").

2. **Interface `Danificavel`** (`componentes/Danificavel.java`): novo
   contrato com `takeDamage(int quant)` e `isDead()`. `CharacterComponent`
   passou a `implements Danificavel` (zero mudança de lógica, os
   métodos já existiam com esses nomes).

3. **`EnemyComponent` virou classe abstrata**: `attack(Entity target)`
   é abstrato agora; a lógica comum de dano direto (achar o
   `CharacterComponent` do alvo, tratar player via `hitBy()`) foi pra
   um helper `applyDirectDamage()`. `MeleeEnemyComponent` e
   `FlyingEnemyComponent` implementam `attack()` chamando esse helper;
   `RangedEnemyComponent` continua disparando projétil.

4. **Bug de aceleração após pausa (câmera/inimigos "acelerando" por
   alguns segundos)**: causado pelo FXGL tentando "recuperar" ticks em
   rajada depois de um período pausado. Mitigado com um "período de
   graça" (`Main.emPeriodoDeGraca()` / `despausarMotor()`, 0.4s) que
   os componentes ignoram logo após despausar.

5. **Bug grande: "Reiniciar" e "Menu Principal" sempre voltavam pro
   menu principal**. Causa raiz: o comentário original assumia que
   `initUI()` só roda uma vez no app; na real o FXGL chama
   `initGame()+initPhysics()+initUI()` de novo toda vez que
   `startNewGame()` é chamado (todo Reiniciar/Menu Principal). Corrigido
   movendo a decisão de "mostrar o menu e pausar" pra dentro do próprio
   `initUI()`, com uma flag (`pularMenuAoIniciar`) que só
   `restartLevel()` liga, pra pular esse passo. Isso também corrigiu um
   segundo bug relacionado (menu de pausa "congelando" o jogo ao tentar
   sair pro menu principal, causado por mexer no `mainMenuOverlay`
   *depois* de um `startNewGame()` que pode não ser síncrono).

6. Ajustes menores: `shiftHeld` removido de `PlayerComponent` (o
   usuário já tinha tirado, só limpei comentários e código morto);
   arquivo `.code-workspace` perdido dentro de `componentes/` copiado
   pra raiz do projeto (a cópia antiga ainda existe lá e precisa ser
   apagada manualmente, sem acesso de delete nesta sessão); comentários
   das últimas mudanças foram revisados pra ficar mais curtos e diretos
   a pedido do usuário.

## Estado atual

Todo o código foi conferido byte a byte no disco do usuário (o commit
remoto teve alguns "stale writes" ao longo da conversa, sempre
detectados e corrigidos reenviando). Falta o usuário rodar
`mvn clean compile exec:java` e testar os três caminhos: Reiniciar
(deve cair direto no jogo), Menu Principal (Game Over) e Menu (pausa
via ESC) — os últimos dois devem mostrar o menu principal sem travar.

## Pendências

- Apagar manualmente `src/main/java/com/jogo/componentes/FXGL-MavenGradle.code-workspace`
  (cópia antiga, já duplicada na raiz do projeto).
- Confirmar que os três fluxos de restart/menu funcionam depois do
  rebuild limpo.
- Continuar atualizando o README a cada mudança relevante.
