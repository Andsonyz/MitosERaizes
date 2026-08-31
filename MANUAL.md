# Entre Mitos e Raízes

Jogo educativo 2D em Java sobre as lendas do folclore brasileiro e a proteção
da floresta. A campanha foi planejada para aproximadamente **30 a 40 minutos**,
em quatro capítulos: Saci, Caipora, Iara e Boitatá.

## O que já está no jogo

- Menu inicial com **Jogar**, **Configurações** e **Extras**;
- Mapa 2D pixelado, inspirado na paleta vibrante do Game Boy Advance;
- Controles completos por teclado, sem dependência de mouse;
- Missões de investigação, prevenção de queimadas, proteção de trilhas, água e fauna;
- Diálogos educativos, feedback para cada ação e conclusão narrativa;
- Códice consultável com as lendas que aparecem na história;
- Opções de alto contraste e tamanho de texto;
- Créditos de Antonio Andson e Sophia Hellen.

## Como executar

O projeto usa somente Java 8+ e Swing, portanto não precisa baixar bibliotecas.

```powershell
javac -encoding UTF-8 -d out src/main/java/br/com/entremitoseraizes/*.java
java -cp out br.com.entremitoseraizes.Main
```

Em uma IDE, importe a pasta como um projeto Maven ou execute a classe
`br.com.entremitoseraizes.Main`.

## Verificação rápida

O teste de fluxo abaixo valida as quatro missões sem abrir a janela:

```powershell
javac -encoding UTF-8 -cp out -d out src/test/java/br/com/entremitoseraizes/GameSessionSmokeTest.java
java -cp out br.com.entremitoseraizes.GameSessionSmokeTest
```

## Controles

| Tecla | Ação |
| --- | --- |
| `WASD` ou setas | mover / selecionar opção |
| `E` | interagir com um sinal, personagem ou objeto próximo |
| `Enter` ou `Espaço` | avançar diálogo / confirmar |
| `C` | abrir o Códice durante a exploração |
| `M` | abrir o mapa da região |
| `Esc` | voltar / pausar |

Os símbolos `!` no mapa indicam tarefas da missão atual. Aproxime-se deles e
pressione `E`. As árvores, rios e pontes também fazem parte da navegação.
