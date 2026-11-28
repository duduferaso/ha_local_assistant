# Lições da primeira versão

O Home Control nasceu do Jarvis Home, que passou por mais de 50 versões numa casa de verdade. Cada erro abaixo aconteceu de fato. A coluna da direita diz o que impede que ele volte. **Antes de mexer numa dessas partes, leia a linha dela.**

## Voz e chamada

| O que deu errado | Por quê | O que protege hoje |
|---|---|---|
| O app passou a **ativar sozinho** depois de treinar com a TV ligada (1.15.6) | O limiar automático ficava 0,01 abaixo da TV (~0,22) | Limiar automático no máximo 0,20 e 0,03 abaixo da fala/TV mais parecida (`PersonalTrainer.choose`). Teste `tests/jvm_treino/LimiarTreinoTest.kt` |
| Com a TV **parecida demais** com a chamada, o limiar ficava acima da TV (achado na 2.0.0-alfa) | O piso de 0,14 passava por cima da folga | A folga vale abaixo de 0,14, com piso absoluto de 0,08, e a tela de treino avisa. Mesmo teste |
| **Conversa virou "Hey Tony"** e o app ativava sozinho (até 1.15.9) | O reforço aprendia qualquer resposta `action_done`, inclusive de listas e perguntas | Só aprende com `confirms` = true (luz acionada ou ação do HA). Testes `tests/jvm/Main.kt` (seção 7) e `tests/jvm_pessoal/ConversaTest.kt` |
| **Bipe duplo**, e o Whisper devolvia só "Reitone" (1.12.6 → 1.15.2) | O áudio da chamada ia junto com o comando mesmo com pausa entre eles | Só "frase única" leva o áudio da chamada (`PersonalWakeWord`, `oneBreath`). *Falta teste puro: está na lista abaixo* |
| O **próprio bipe contava como fala** e a sessão terminava cedo (1.15.1) | O VAD de energia ouvia o bipe | O VAD ignora os primeiros 300 ms com bipe. Teste `tests/jvm_vad/VadTest.kt` |
| **Treinar longe ou com TV** gerava exemplos cheios de fundo (até 1.15.4) | Cada gravação entrava inteira | Recorte pelo Silero, como ao vivo (`TakeTrim`). Teste `tests/jvm_pessoal/TakeTrimTest.kt` (gravações locais) |
| A chamada **não pegava quando alguém falava logo antes** | O Silero emendava as falas e a chamada não ficava no começo do trecho | Comparação também pelo fim do trecho (`SegmentEnd`). Teste `ChamadaEmendadaTest.kt` (gravações locais) |
| **"Hey Tony" não parava o alarme**: só tocando na tela (até 1.15.10) | O bipe, colado no microfone, cobria a chamada | Pausa maior entre bipes e toque calado ao ouvir fala (`AlarmHold`). Teste `tests/jvm_timers/AlarmHoldTest.kt` |

## Tela e uso

| O que deu errado | Por quê | O que protege hoje |
|---|---|---|
| A tela **escurecia no meio do treino** à noite (1.15.11) | O treino contava como "casa quieta" no modo noite | `ScreenRules`: treino e alarme impedem o escurecimento. Teste `tests/jvm_treino/ScreenRulesTest.kt` |
| "Luz da cozinha" **não fazia nada** | Só o nome, sem verbo, ia para o HA | "Só o nome da luz" inverte o estado (`LightCommands.nameOnly`). Teste `ComodosTest.kt` |

## Processo

| O que deu errado | Por quê | O que protege hoje |
|---|---|---|
| **"JARVIS HOME" continuou no título** da 2.0.0-alfa.1 | A troca de nome procurou "Jarvis" diferenciando maiúsculas | `tools/checar_textos.sh` (sem diferenciar maiúsculas) roda nos testes e no CI |
| Teste da previsão **falhava perto da meia-noite** no CI | O HA falso usava a data do servidor (UTC) | O mock usa o fuso da casa |
| **Afirmei que o repouso segurava o microfone** e não segurava | Conclusão tirada sem ler o código | Regra: antes de afirmar como algo funciona, conferir no código ou num teste e citar onde |
| Publicação **sem conferir** se o celular enxerga a versão | — | Depois de publicar, conferir `versao.json` ou o Release pela API, como o app faz |
| **Dados da casa** num repositório público (planta, exemplos de voz) | O repositório antigo nasceu privado na prática e virou público | `tools/checar_privacidade.sh` no CI e no gancho de push; `.privacidade-local` para nomes da família |

## Ainda sem teste automático

- Decisão "frase única × abre o microfone" (`PersonalWakeWord.accept`): extrair para código puro e testar com áudio sintético.
- Pausa total entre dois apps: só testada no aparelho.
- Importar um pacote real do Jarvis Home de ponta a ponta: hoje só o formato é testado (`PacoteConfigTest`).

## Regras que saem disso

1. **Todo bug corrigido ganha um teste** que falharia sem a correção. Antes de dar a correção por feita, rode o teste com o erro de volta e veja falhar.
2. **Quanto mais perto do microfone, mais cuidado:** limiar, reforço e recorte mexem na experiência inteira. Mudança ali vem com números (antes × depois) nas gravações de teste.
3. **Nada de afirmar sem conferir:** "o código faz X" vem com o arquivo e a linha.
4. **Uma mudança por PR.** Renomear e mudar comportamento no mesmo PR esconde erro.
