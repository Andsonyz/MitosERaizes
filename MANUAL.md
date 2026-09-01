# Entre Mitos e Raízes

Jogo 2D em Java, com arte em pixel art e controles por teclado. Esta versão
implementa a fase de teste **O Chamado do Curupira**, em que uma pessoa humana
entra na floresta, conhece o guardião e ajuda a proteger a mata.

## Recursos implementados

- Fundo animado no menu, usando `Image/Cenarios/MENU_loop.gif`;
- Arte de título, cenários do Curupira e personagem fornecidos em `Image/`;
- Criação de personagem por apelido, usado nos diálogos;
- Dois slots de save locais; os dados não são gravados no projeto ou Git;
- Menu inicial com novo jogo, continuar em cada slot, configurações, extras e saída;
- Menu de pausa durante a partida: continuar, salvar, configurações, lobby e sair;
- Três cenários conectados pelas imagens fornecidas: `CRPR-pt1` (trilha),
  `CRPR-pt2` (entrada da cabana) e `CRPR-pt3` (interior);
- Navegação direta entre cenários pelas bordas da tela e pela porta da cabana;
- Personagem jogável Greg, com sprite parado e sprite andando;
- Transição preta circular ao iniciar/trocar cenas;
- Ajustes de alto contraste e tamanho de texto;
- Fase de teste: chegada à mata, descoberta da cabana e conversa com o Curupira.

## Executar

Requer Java 8 ou superior. Execute a partir da raiz do projeto, para que as
imagens da pasta `Image/` sejam encontradas.

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Path src/main/java -Filter *.java | ForEach-Object { $_.FullName })
java -cp out br.com.entremitoseraizes.Main
```

## Controles

| Tecla | Ação |
| --- | --- |
| `WASD` ou setas | mover / selecionar |
| `Enter` ou `Espaço` | confirmar / avançar diálogo |
| `E` | interagir |
| `M` | abrir mapa dos três cenários |
| `F5` | salvar no slot atual |
| `Esc` | abrir menu da partida / voltar |

## Privacidade dos saves

O jogo usa dois arquivos locais na pasta `.saves/`, que é ignorada pelo Git.
Apenas apelido, progresso e posição são salvos; nada é enviado pela internet ou
incluído no repositório. Evite usar nome completo se o computador for compartilhado.

## Verificação técnica

```powershell
javac -encoding UTF-8 -cp out -d out (Get-ChildItem -Recurse -Path src/test/java -Filter *.java | ForEach-Object { $_.FullName })
java -ea -cp out br.com.entremitoseraizes.GameSessionSmokeTest
```
