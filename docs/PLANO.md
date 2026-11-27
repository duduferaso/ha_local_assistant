# ha_local_assistant — Plano do projeto aberto

Aprovado em 27/11/2026. Versão editável e comentável: o documento "ha_local_assistant — Plano do projeto aberto" do mantenedor. Este arquivo é a cópia de referência no repositório.

## Visão

O ha_local_assistant (nome na tela: **Home Control**) é um assistente de voz em português para o Home Assistant, rodando num celular Android na parede. A 2.0.0 é o primeiro lançamento aberto: o app de hoje (Jarvis Home 1.15.12), com tudo o que é da casa do mantenedor transformado em configuração e cada função em módulo.

Princípios:

- **Local:** o áudio só vai para o Home Assistant da casa. Nada passa por servidor de terceiros.
- **Modular:** cada casa liga só o que usa. A planta e as funções (agenda, listas, clima, treino…) são opcionais; dá para usar só o personagem e a voz.
- **Sem dados de ninguém no código:** voz, planta, endereço do HA e nomes ficam no celular de cada um e no backup dele.
- **Uma fonte de atualizações:** todo mundo com o APK oficial recebe as versões publicadas pelo mantenedor (duduferaso).
- **Fácil de contribuir:** lógica pura e testável, guia para quem programa (pessoas ou IAs), CI em cada PR.
- **Português do Brasil primeiro:** textos, falas e documentação.

## Decisões tomadas

| Tema | Decisão |
| --- | --- |
| Nome do repositório | `duduferaso/ha_local_assistant` |
| Licença | Apache-2.0 |
| Id do app novo | `io.github.duduferaso.halocalassistant`, próprio desde o início |
| Nome na tela | Home Control |
| Assinatura | Chave nova, só deste projeto, guardada pelo mantenedor |
| App atual | Continua como `br.com.jarvishome` no repositório `jarvis_home`, em uso no Redmi até a troca |
| Testes do app novo | No próprio Redmi, com os dois apps instalados (ids diferentes) e só um ativo por vez: o outro fica em pausa total |
| Configuração nos testes | A real do Redmi: o app atual exporta no formato novo e o app novo importa desde a primeira alfa |
| Planta | Modular: cada pessoa monta a própria casa, com quantos cômodos quiser, num editor de blocos no app |
| Planta opcional | Sim: dá para usar só o personagem |
| Funções | Cada uma vira módulo com chave de ligar e desligar (agenda, listas, clima, timers, treino, avisos, planta, luzes) |
| Andares | Fase 2. O formato da planta já prevê andares desde a 2.0 |
| Frase de ativação | Configurável já na 2.0 |
| Treino da voz | Cada casa treina a própria, com um treino guiado no app (lugares, tom, ruído) e um guia em `docs/` |
| IA para programar | Opcional: quem quiser configura a sua no próprio ambiente |
| Atualizações | Todos com o APK oficial recebem as versões assinadas pela chave do mantenedor |

## Os dois apps durante a transição

Os dois apps andam em paralelo até a 2.0.0 estar aprovada no dia a dia; só então o Redmi fica só com o app novo e o `jarvis_home` é desativado.

| | Jarvis Home (atual) | ha_local_assistant (novo) |
| --- | --- | --- |
| Repositório | `jarvis_home` | `ha_local_assistant` |
| Id | `br.com.jarvishome` | `io.github.duduferaso.halocalassistant` |
| Versões | 1.15.x / 1.16.x | 2.0.0-alfa → 2.0.0 |
| Aparelho | Redmi da parede | Redmi, com o app atual em pausa total |
| Canal de atualização | branch `atualizacoes` do `jarvis_home` | Releases do `ha_local_assistant` |
| O que entra | Correções e pequenas melhorias de uso | Toda a arquitetura nova (módulos, planta, primeira configuração) |

Regras para não fazer o trabalho duas vezes:

1. **Funções grandes só no novo.** O app atual recebe só correções e ajustes de uso.
2. **Correção feita no atual também vai para o novo** no mesmo dia, quando o código afetado ainda existir lá. O commit no novo cita o do atual.
3. **O núcleo comum** (detector de voz, comandos de luz, timers, listas) é o mesmo código nos dois até o novo reorganizar. Depois disso, correções de núcleo nascem no novo e são copiadas para o atual só se forem urgentes.
4. **Commits nos dois repositórios**, cada um com o seu histórico. O novo começa sem histórico do antigo.

**Testar com a casa de verdade desde o começo.** O app novo é testado com a configuração real do Redmi, não com uma casa de exemplo:

1. **Formato primeiro.** O formato do pacote de configuração (planta, módulos, voz, comandos treinados, ajustes) é definido no M0 e vale para os dois apps.
2. **O app atual exporta no formato novo.** A 1.16.0 ganha "Exportar para o app novo": converte a planta para `planta.json`, transforma as chaves de hoje (agenda, listas, modo noite…) nas chaves dos módulos e junta voz, comandos e ajustes num arquivo só.
3. **O app novo importa desde a primeira alfa.** A primeira configuração oferece "Importar do Jarvis Home".
4. **Cada peça modular nova ganha a exportação no app atual antes de ser testada.** Se o M3 muda algo na planta, sai uma 1.16.x que exporta o campo novo.
5. **Mesmo microfone:** como os dois apps rodam no Redmi, os exemplos de voz importados funcionam no app novo como funcionam hoje.

**Dois apps no mesmo Redmi.** O Android deixa um só app ouvir o microfone por vez, e hoje o repouso continua segurando o microfone, lendo e apagando os avisos do HA e tocando timers. Por isso os dois apps ganham uma **pausa total** (a 1.16.0 no atual, a primeira alfa no novo):

- solta o microfone de verdade;
- para de ler e apagar `input_text.jarvis_aviso`;
- não toca timers nem alarmes (os pendentes são avisados na tela antes de pausar);
- não liga sozinho quando o celular reinicia;
- mostra na tela "Em pausa: outro app está ativo", com um toque para voltar.

Um botão "Trocar para o outro app" pausa este e abre o outro, que sai da pausa. Os dois usam o mesmo HA; sugestão de um usuário `jarvis_teste` no HA para o app novo, para os logs do HA mostrarem qual app fez o quê.

**Fim da transição:**

1. A importação já foi usada desde a primeira alfa; na 2.0.0 aprovada, uma última exportação do app atual.
2. Alguns dias com os dois instalados e o antigo em pausa total, para voltar se algo falhar.
3. Desinstalar o antigo e desativar o `jarvis_home` (decisão em aberto: privado ou apagado).

## Arquitetura da 2.0

A 2.0 mantém o caminho da voz de hoje (detector no celular → Whisper no HA → comandos no app → agente do HA) e reorganiza o resto em módulos, uma tela em peças e uma planta feita de dados.

### Funções em módulos

Cada módulo tem nome, chave de ligar e desligar, comandos de voz, peças de tela e ajustes próprios. Módulo desligado some de verdade: o comando cai no agente do HA e a peça sai da tela. Uma função nova (Mi Box, rotinas) entra como módulo novo, sem mexer nos outros.

| Módulo | Voz | Tela | Precisa no HA |
| --- | --- | --- | --- |
| Luzes | ligar, desligar, só o nome da luz, treino de comandos | botões de luz | `light.*` / `switch.*` |
| Planta da casa | cômodos por nome falado | planta 3D, editor | luzes |
| Agenda | consultar, criar, apagar compromissos | quadro da agenda | `calendar.*` |
| Listas | compras e tarefas | — | `todo.*` |
| Clima | previsão da semana | clima | `weather.*` |
| Timers e alarmes | timers, alarmes, parar o toque | contagem e toque | nada |
| Treino intervalado | iniciar, pausar, quanto falta | painel do treino | nada |
| Avisos do HA | fala os avisos das automações | aviso na tela | `input_text` |

Núcleo sempre presente (não é módulo): microfone e detector da frase de ativação, sessão com o HA, corretor de texto, desfazer, filtro de alucinação, voz das respostas, modo noite, atualização.

### Tela em peças

O personagem está sempre na tela. As outras peças (planta, botões de luz, clima, agenda, relógio, timers, treino) aparecem conforme os módulos ligados e as escolhas em Engrenagem → Tela. Três usos típicos: só personagem; personagem e painéis; completo, como o Redmi hoje.

### Planta modular

Um editor de blocos no app:

- **"+ Cômodo":** escolhe o tipo (sala, cozinha, quarto, banheiro, varanda, escritório, garagem, corredor, outro) e o nome. O bloco se arrasta e se redimensiona numa grade de 0,5 m.
- **"Estender":** junta mais um retângulo ao mesmo cômodo, para cômodos em L ou irregulares.
- **Paredes automáticas:** nascem entre cômodos diferentes e no contorno da casa; somem entre partes do mesmo cômodo e em "ambientes integrados".
- **Pontos de luz** no centro de cada parte, arrastáveis.
- **Luzes do HA e nomes falados por cômodo.** O tipo sugere nomes falados e palavras para achar as luzes no HA.
- **"Ver em 3D"** mostra a vista isométrica de hoje, montada a partir do desenho.
- Fase 2: andares em abas, portas desenhadas, imagem da planta como fundo, outros aparelhos por cômodo.

### Frase de ativação configurável

O detector já aprende qualquer frase pelos exemplos gravados. Fica configurável só a parte de texto: o nome que aparece na tela e nas falas, e a lista de formas que o corretor tira do começo da transcrição (hoje fixas para "Hey Tony", "Ei Tony", "Reitone"…). O app gera as formas parecidas a partir da frase escolhida.

### Treino guiado da voz

Cada casa treina a própria voz com um passo a passo no app, sem exemplos de fábrica. O guia transforma em telas o que aprendemos no Redmi: treinar no lugar e no barulho em que a pessoa chama é o que mais melhora a chamada difícil.

1. **Quem mora na casa:** cada pessoa treina a sua voz, uma de cada vez, com o nome dela.
2. **Onde se chama:** a pessoa marca os lugares de onde costuma chamar (perto do celular, sofá, cozinha, a 3 m). Com planta, escolhe os cômodos nela.
3. **Gravações por lugar:** 3 a 4 por lugar, em tom normal, um pouco mais alto e mais baixo. O app mostra o que falta, lugar por lugar.
4. **Barulho do dia a dia:** uma rodada com TV ligada, coifa ou o barulho comum da casa, e outra rodada de conversa ou TV *sem* a frase. Assim o app aprende o que não é chamada.
5. **Retorno a cada gravação:** "baixo demais", "cortou o começo", "muito barulho, chegue mais perto", "parecida demais com outra, grave de outro jeito".
6. **Teste final:** a pessoa chama algumas vezes de cada lugar e vê quantas pegaram. O app sugere onde gravar mais e escolhe o limiar (no máximo 0,20 e 0,03 abaixo da fala parecida mais próxima, como hoje).

O mesmo guia vale para reforçar depois ("está falhando na cozinha" → grava só lá) e para o treino de comandos. O texto completo, com dicas de tom, distância e ruído, fica também em `docs/treino-de-voz.md`.

### Primeira configuração

Uma sequência de telas na primeira abertura:

1. Endereço e token do HA, com teste de conexão — ou "Importar do Jarvis Home".
2. Frase de ativação e treino da voz de cada pessoa da casa.
3. Funções: o app olha o que o HA tem e sugere as chaves (sem `calendar.*`, a Agenda vem desligada).
4. Planta: montar agora, importar um arquivo ou "depois" (fica no modo só personagem).
5. Luzes de cada cômodo, se houver planta.

### Backup

Um arquivo leva a casa inteira: exemplos de voz, comandos treinados, planta, chaves das funções e configurações. O token entra só se a pessoa escolher "com token". Restaurar num celular novo deixa tudo igual.

### Atualização

O app confere os Releases do `ha_local_assistant` a cada 12 h e oferece a versão nova. O Android pede confirmação para instalar. A versão só instala se tiver o mesmo id e a mesma assinatura. O canal fica configurável, para quem mantém uma versão própria.

## Formato da planta (planta.json)

A planta é um arquivo JSON no celular, com versão de formato, andares e cômodos; medidas em metros, origem no canto de cima à esquerda. As paredes não são guardadas: o app calcula a partir dos cômodos.

```json
{
  "formato": 1,
  "nome": "Minha casa",
  "andares": [
    {
      "id": "terreo",
      "nome": "Térreo",
      "comodos": [
        {
          "id": "cozinha",
          "nome": "Cozinha",
          "tipo": "cozinha",
          "partes": [[0, 1.5, 2, 7]],
          "luzes": [[1, 3.3], [1, 6.3]],
          "entidades": ["light.cozinha", "switch.luz_servico"],
          "nomesFalados": ["cozinha", "área de serviço", "lavanderia"],
          "mostrarNome": true
        }
      ],
      "integrados": [["cozinha", "sala"]],
      "portas": []
    }
  ]
}
```

| Campo | O que é |
| --- | --- |
| `formato` | Versão do formato. O app converte formatos antigos ao abrir. |
| `partes` | Retângulos do cômodo: x1, y1, x2, y2 em metros. Mais de um = cômodo em L ou irregular. |
| `luzes` | Pontos de luz (x, y) desenhados na planta. |
| `entidades` | Luzes do HA ligadas ao cômodo. Vazio = o app procura pelo nome. |
| `nomesFalados` | Jeitos de falar o cômodo; o primeiro é o usado nas respostas. |
| `integrados` | Pares de cômodos sem parede entre eles. |
| `portas` | Fase 2: vãos nas paredes, só para o desenho. |

Regras checadas pelo app e pelos testes: partes de cômodos diferentes não se sobrepõem; parte mínima de 0,5 × 0,5 m; ids únicos; cada entidade em um cômodo só. O formato fica documentado em `docs/planta.md`, e o projeto traz uma planta de exemplo em `examples/planta.json`. A planta real do mantenedor é convertida para este formato pelo app atual, fora deste repositório.

## Privacidade e segurança

Nada que identifique a casa do mantenedor entra neste repositório, e uma checagem automática barra o push se algo escapar.

| No app atual | Neste repositório |
| --- | --- |
| Exemplos de voz de fábrica, tirados das vozes da casa | Não existem: a primeira configuração pede o treino |
| Planta do apartamento escrita no código | Planta de exemplo genérica; a planta real só no celular |
| Endereço do HA da casa | Campo vazio na primeira configuração |
| Canal de atualização do `jarvis_home` | Releases do `ha_local_assistant` |
| Frases reais dos logs e nomes de pessoas nos testes | Frases genéricas |
| Gravações da casa usadas nos testes do detector | Ficam fora do repositório |
| Orquestrador Python sem uso | Sai do projeto |

Outras regras:

- **Histórico novo:** este repositório começa sem os commits do `jarvis_home`.
- **Checagem antes de cada push e no CI** (`tools/checar_privacidade.sh`): procura IPs privados, tokens, e-mails, nomes da casa e arquivos de voz e planta, e barra o push se achar algum.
- **Chave de assinatura só com o mantenedor.** Nunca no Git nem no CI na 2.0. Só ela publica versões que atualizam os celulares.
- **Token do HA** guardado criptografado no celular, fora do backup por padrão, nunca em APK publicado.
- **`SECURITY.md`:** como avisar uma falha em particular, sem colar tokens em issue.
- **Nenhuma telemetria.** O app não manda nada para ninguém, nem relatório de erro. Logs e diagnóstico só saem do celular quando a pessoa exporta.
- **Aparelhos suportados na 2.0:** Android 8.0 ou mais novo, processador de 64 bits (arm64), porque o motor de voz só vem compilado assim. Outros processadores na fase 2.

## Repositório, fluxo de trabalho e testes

Tudo entra na `main` por PR com o CI verde, inclusive o trabalho do mantenedor e das IAs; só o mantenedor publica versões.

```
ha_local_assistant/
├─ android/            app + build.sh
├─ tests/              testes JVM + HA falso (mock_ha.py)
├─ tools/              dev.sh, publicar versão, checagem de privacidade
├─ docs/               PLANO, guia de uso, configurar o HA, planta, treino de voz, módulos
├─ examples/           planta de exemplo
├─ .github/            CI, modelos de issue e PR
├─ .devcontainer/      ambiente pronto (Codespaces/Docker)
├─ README.md  LICENSE  NOTICE  CHANGELOG.md
├─ CONTRIBUTING.md  SECURITY.md  CODE_OF_CONDUCT.md
└─ AGENTS.md / CLAUDE.md   guia para quem programa, sem dados de nenhuma casa
```

### Fluxo de trabalho

1. Cada tarefa vira uma issue com rótulo (`módulo:agenda`, `planta`, `bug`, `ideia`, `boa-primeira-tarefa`).
2. O trabalho acontece numa branch e vira PR.
3. O CI compila o APK sem assinatura, roda todos os testes JVM e a checagem de privacidade.
4. O mantenedor revisa e aprova. A `main` fica protegida: sem push direto.
5. Para publicar: tag `vX.Y.Z`, APK assinado na máquina do mantenedor, Release no GitHub com notas do `CHANGELOG.md`.

### Versões

- **Números:** `MAIOR.MENOR.CORREÇÃO`. A 2.0.0 é o primeiro lançamento aberto, com pré-versões `2.0.0-alfa.N`.
- **O `versionCode` sempre sobe.** O app novo começa do 1, porque o id é outro.
- **Pré-versões** saem como "pre-release" no GitHub. Só aparelhos com "receber versões de teste" ligado as recebem.

### Build

Na 2.0 o `build.sh` continua (sem Gradle), com as regras atuais: Kotlin 1.9.25, `-Xlambdas=class`, sem `compareBy(vararg)`. O CI usa o mesmo script num runner Ubuntu.

### Testes

| Tipo | Onde roda | O que cobre |
| --- | --- | --- |
| Unitários JVM | CI e local | comandos, timers, listas, agenda, corretor, VAD, treino, planta, módulos |
| Integração com HA falso | CI e local | sessão de voz completa contra `mock_ha.py` |
| Planta | CI e local | formato, conversão de versão, paredes automáticas, sobreposição, nomes falados |
| Detector com gravações reais | só local | chamadas, conversa, TV, alarme; gravações fora do repositório |
| No aparelho | manual | lista de conferida por versão em `docs/testes-no-aparelho.md` |

Toda lógica nova fica em código puro e vem com teste. O CI barra PR com teste falhando.

## Modo de desenvolvimento com IA

Quem contribui pode programar com a IA que preferir (Claude Code, Codex, Gemini CLI, Copilot, Cursor…). O repositório prepara o terreno, mas o projeto não depende de nenhuma: a IA é opcional. Quem quiser configura a sua no próprio ambiente (conta e chave da ferramenta escolhida) e usa quando precisar; sem IA, tudo funciona do mesmo jeito com o `tools/dev.sh`.

| Peça no repositório | Para quê |
| --- | --- |
| `AGENTS.md` | Guia único que a maioria das IAs lê: arquitetura, regras (Kotlin 1.9, sem dados de nenhuma casa, português, lógica pura com teste) e os comandos do projeto |
| `CLAUDE.md` e `.claude/` | Aponta para o `AGENTS.md` e traz comandos prontos para o Claude Code: testar, compilar, novo módulo, checar privacidade |
| `tools/dev.sh` | Um comando para tudo: `preparar` (baixa Kotlin 1.9.25, android.jar e ferramentas), `testar`, `compilar`, `privacidade`. É o mesmo que o CI roda |
| `.devcontainer/` | Ambiente pronto no GitHub Codespaces ou em Docker: abrir e a IA já compila e testa, sem instalar nada |
| HA falso (`tests/mock_ha.py`) | A IA testa a sessão de voz inteira sem um Home Assistant real |
| `docs/modulos.md` | Receita de um módulo novo (arquivos, chave, voz, tela, teste), para pessoas e IAs seguirem |

**Fluxo de quem programa com IA:**

1. Fork do repositório e uma branch para a tarefa.
2. A IA programa e roda `tools/dev.sh testar` e `privacidade` até tudo passar.
3. Commit em nome da pessoa, com a linha de coautoria da IA quando houver.
4. PR para o `ha_local_assistant`; o CI roda de novo.
5. O mantenedor revisa e aprova. Ninguém faz push direto na `main`, nem pessoa nem IA.

**Regras no `CONTRIBUTING.md`:**

- Dizer no PR que usou IA. Quem abre o PR responde pelo código.
- Testes obrigatórios e checagem de privacidade passando.
- Chaves e contas de IA ficam na máquina de quem contribui: nunca no repositório, no CI, em logs ou em conversas coladas em issues.
- O app publicado não chama IA de terceiros. Perguntas gerais via agente de IA do HA (fase 3) são outra coisa: rodam pela conta de cada casa no próprio HA.

**No app, "Modo desenvolvedor"** (Engrenagem → Sistema): receber pré-versões, logs detalhados e "Exportar diagnóstico". O diagnóstico junta logs, últimos comandos, versão e linhas do detector num arquivo, sem token e sem áudio, para entregar à IA ou anexar numa issue.

## Fases e marcos

O trabalho segue sete marcos em ordem, e cada marco só termina com testes verdes e uma versão testada no Redmi.

| Fase | Marco | O que entrega |
| --- | --- | --- |
| 0 · transição (app atual) | **M0** · Versão 1.16.0 | Exporta no formato novo (planta, módulos, voz) e ganha a pausa total |
| 1 · abertura (app novo) | **M1** · Base limpa | Código sem dados da casa, id novo, LICENSE, CI, checagem de privacidade, ambiente para IAs |
| | **M2** · Funções em módulos | Cada função com chave; desligada some da voz e da tela |
| | **M3** · Planta modular | Formato `planta.json`, paredes automáticas, editor de blocos no app |
| | **M4** · Tela, treino e primeira configuração | Tela em peças, modo só personagem, frase de ativação, treino guiado da voz |
| | **M5** · Lançamento 2.0.0 | Alfas aprovadas no Redmi, documentação, Release assinado |
| | **M6** · Fim da transição | Só o app novo no Redmi; `jarvis_home` desativado |
| 2 · facilitar para outros | — | Andares, portas, imagem de fundo, Gradle, outros celulares além do arm64 |
| 3 · funções novas | — | Mi Box, rotinas, quem está falando, perguntas ao agente de IA, traduções |

Cada marco vira um conjunto de issues neste repositório. Sem datas: o ritmo segue o tempo do mantenedor.

## Decisões em aberto e riscos

| Decisão | Opções | Recomendação | Precisa até |
| --- | --- | --- | --- |
| Idioma | Só português; ou preparar para traduções | Só português na 2.0; traduções na fase 3 | M1 |
| Build | Só `build.sh`; ou também Gradle | Só `build.sh` na 2.0; Gradle na fase 2 | M1 |
| Frase de ativação padrão | "Hey Tony"; outra | "Hey Tony" | M4 |
| Repositório antigo | Privado; ou apagado | Privado por 3 meses, depois apagado | M6 |

| Risco | Efeito | Como evitar |
| --- | --- | --- |
| O editor de planta crescer demais | Atrasa a 2.0 | Editor mínimo na 2.0 (blocos, estender, luzes, nomes); andares, portas e imagem de fundo na fase 2 |
| Correções divergirem entre os dois apps | Bug corrigido num app volta no outro | Regras da transição; cada correção no atual cita o commit no novo |
| Dado da casa escapar para o repositório aberto | Planta ou voz públicas | Histórico novo, checagem de privacidade no push e no CI |
| Backup incompleto na troca | Perder treino ou planta | A importação é testada desde a primeira alfa; o app antigo fica instalado até o fim |
| Detector pior sem os exemplos de fábrica | Primeira experiência ruim para quem instala | Treino guiado na primeira configuração, com dicas (lugar e barulho do dia a dia) |
| Perder a chave de assinatura | Ninguém recebe mais atualizações | Duas cópias da chave fora do computador, com a senha guardada à parte |
| Dois apps disputando o microfone | Ativações falhando | Pausa total nos dois apps: o pausado solta o microfone e para avisos, timers e partida automática |
