# Home Control — guia para quem programa

Vale para pessoas e para IAs (Claude Code, Codex, Gemini CLI, Copilot, Cursor…). Leia antes de mudar o código.

O **Home Control** (repositório `ha_local_assistant`) é um assistente de voz em português para o Home Assistant, rodando num celular Android fixo na parede. Ele fala **direto com o Home Assistant** da casa, sem servidor próprio no meio. O plano do projeto está em [`docs/PLANO.md`](docs/PLANO.md); o formato do pacote de configuração e da planta, em [`docs/pacote-configuracao.md`](docs/pacote-configuracao.md). **Os erros da primeira versão e o que impede que voltem estão em [`docs/LICOES.md`](docs/LICOES.md): leia antes de mexer em voz, limiar, reforço ou alarme.**

> Estado: **2.0.0 em construção** (marco M1 do plano). O app veio do Jarvis Home 1.16 e ainda carrega nomes antigos em classes (`JarvisForegroundService`, `JarvisApi`) e no nome das preferências (`"jarvis"`). Trocar esses nomes é tarefa à parte; não misture com outra mudança.

## Comandos

```bash
tools/dev.sh preparar      # baixa Kotlin 1.9.25 e android.jar para .ferramentas/
tools/dev.sh testar        # todos os testes na JVM (o mesmo do CI)
tools/dev.sh compilar      # APK de teste em android/build/ (chave descartável)
tools/dev.sh privacidade   # barra dados de casa, tokens e chaves
```

Ferramentas do sistema (Debian/Ubuntu): `openjdk-21-jdk-headless aapt dalvik-exchange zipalign apksigner python3-aiohttp unzip zip`. O `.devcontainer/` já traz tudo pronto (GitHub Codespaces ou Docker).

## Como um comando de voz anda

```
frase de ativação ──► detector pessoal no celular (Silero VAD + DTW contra os exemplos de voz da casa)
                        │  bipe; o VAD de energia do app decide o fim da fala
                        ▼
     áudio 16 kHz ──WebSocket──► Home Assistant  assist_pipeline/run  stt→stt  (Whisper)
                        ▼  texto
     o APP decide:  desfazer · timers/alarmes · treino intervalado · listas (todo.*) ·
                    previsão (weather.*) · agenda (calendar.*) · correções aprendidas ·
                    luzes (light.* / switch.*)  ──REST──► homeassistant.turn_on/turn_off
                        │  o que sobrar
                        ▼
     Home Assistant  assist_pipeline/run  intent→tts  (agente de conversa do HA)
                        ▼
     resposta falada SEMPRE com o TTS do HA (voz do Android só como reserva)
```

Pontos que importam (cada um tem teste):

- **Detector pessoal** (`wakeword/`): não há exemplos de fábrica. Sem treino, o detector fica parado e a tela pede o treino ou a importação de um pacote. A decisão fica em `WakeMatch.decide` (pura). Trechos maiores que 1,9 s também são comparados pelo fim (`SegmentEnd`). O limiar automático do treino fica 0,03 abaixo da fala/TV mais parecida e **no máximo 0,20**.
- **Reforço** (exemplos aprendidos no uso, até 10, 15% mais exigentes): só aprende quando a resposta traz `confirms` = true (luz acionada de verdade ou `action_done` do HA). Conversa nunca pode virar exemplo.
- **Frase única × abre o microfone:** "frase de ativação + comando" de uma vez manda o áudio da chamada junto; com pausa, só o que vem depois do bipe. Nunca mande chamada + pausa + comando juntos (o Whisper devolve só a chamada).
- **Sessão** (`network/HaDirectBackend`): STT → tira a chamada do texto (`TextCorrector.stripWakeWord`) → descarta alucinações do Whisper → desfazer → timers → treino → listas → previsão → agenda → correções aprendidas → corretor → luzes → agente do HA.
- **Luzes:** comando sempre `turn_on`/`turn_off` explícito, nunca `toggle`. A regra do que é luz fica só em `LightCommands.isLight`; a normalização de texto, só em `TextCorrector.normalize`. "Só o nome da luz" e treino sem verbo invertem o estado atual.
- **Planta** (`home/Planta`, `home/FloorPlan`, `home/PlantaStore`): vem do `planta.json` de cada casa; sem planta, a tela mostra só o personagem. As paredes saem dos cômodos (`Planta.paredes`).
- **Pacote de configuração** (`settings/PacoteConfig` puro + `settings/Backup`): exporta e importa a casa inteira; importa também o pacote do Jarvis Home.
- **Convivência com o Jarvis Home** durante a transição: pausa total (`Prefs.pausedForOther`) e broadcasts `io.github.duduferaso.halocalassistant.PAUSAR` / `br.com.jarvishome.PAUSAR`.
- **Alarme:** o toque se cala 3,5 s quando alguém fala (`AlarmHold`), para a chamada ser reconhecida por cima do bipe.

## Mapa do código

`android/app/src/main/java/io/github/duduferaso/halocalassistant/`:

| Pacote | Papel |
|---|---|
| `service/` | `JarvisForegroundService` (microfone, estados IDLE → LISTENING → BUSY → RESPONDING, sessões, falas, timers, reforço), `WorkoutCoach`, `AlarmBeeper`, `BootReceiver`, `PauseReceiver` |
| `wakeword/` | `PersonalWakeWord` + `PersonalStore` (exemplos em `files/personal/vozes.tpl`, aprendidos em `aprendidos.tpl`), `PersonalDetector` (log-mel + DTW, `WakeMatch`, `SegmentEnd`, `TakeTrim`, `PersonalTrainer`) |
| `audio/` | `VoiceActivityDetector` (VAD de energia), `AudioPlayer` |
| `network/` | `HaDirectBackend` (sessão de voz), `MiniWebSocket`, `TextCorrector`, `CommandMatcher`, `JarvisApi` (orquestrador antigo, sem uso: a remover) |
| `commands/` | **Lógica pura, testável na JVM:** `LightCommands`, `Timers`, `AlarmHold`, `ScreenRules`, `Workout`, `Lists`, `Learning` |
| `home/` | `HomeApi` (REST), `HomeParser`, `HomeModel`, `HomeMonitor`, `FloorPlan`, `Planta`, `PlantaStore`, `Forecast`, `LightControl` |
| `calendar/` | `CalendarParser`, `CalendarAssistant`, `CalendarModel` |
| `settings/` | `Prefs`, `Backup` (pacote), `PacoteConfig`, `Provisioning`, `SecureStore` |
| `ui/` | `MainActivity`, `SettingsActivity`, `FloorPlanView`, `CharacterView`, `VoiceTrainingActivity`, `CommandTrainingActivity`, `AgendaActivity`, `WorkoutActivity` |
| `update/` | `Updater` (GitHub Releases), `UpdateFlow`, `ApkInstaller` |
| `domain/` | `StateBus`, `JLog`, `CommandHistory`, `CrashLog` |

## O que o app usa do Home Assistant

Usuário **sem administrador** com token de longa duração. Só chamadas que um usuário comum pode fazer.

- REST: `GET /api/states`, `GET /api/states/<id>`, `POST /api/services/<domínio>/<serviço>`, `/api/calendars/...`.
- WebSocket `/api/websocket`: `auth`, `assist_pipeline/run`, `config/area_registry/list`, `calendar/event/delete`. Ids crescentes em cada conexão.
- Entidades: `light.*`/`switch.*` (switch só conta se o nome tiver "luz", "lâmpada" ou "ilumina"), `weather.*` (`weather.get_forecasts`), `todo.*`, `calendar.*`, `input_text.jarvis_aviso` (avisos falados; o nome fica assim por compatibilidade).

## Build (sem Gradle)

`android/build.sh`, chamado por `tools/dev.sh compilar`. Regras obrigatórias:

- **Kotlin 1.9.x, nunca 2.x.** A stdlib do 2.x usa `invokedynamic`, que o dalvik-exchange não converte: o app quebra no aparelho (`NoSuchMethodError metafactory`). O `build.sh` barra isso, e também `compareBy(vararg)`, `thenBy` e `nullsFirst/Last` (use `sortedBy`/`sortedByDescending`).
- Compilador com `-Xlambdas=class -Xsam-conversions=class -Xstring-concat=inline`.
- Só arm64 (`jniLibs/arm64-v8a/libsherpa-onnx-jni.so`), Android 8.0+ (minSdk 26), targetSdk 33.
- **Assinatura:** sem `KEYSTORE`, sai um APK de teste com chave descartável. A chave oficial fica só com o mantenedor; versões publicadas saem por `tools/publicar_versao.sh`.

## Testes

`tools/dev.sh testar` roda tudo: unitários (`tests/jvm_*`), atualização (`tests/jvm_update`) e integração com um Home Assistant falso (`tests/mock_ha.py`, aiohttp: REST + WebSocket + `todo` + `get_forecasts`). A planta de teste é genérica (`tests/jvm/PlantaTeste.kt`). Os testes do detector com gravações reais (`tests/jvm_pessoal`) só rodam com `DADOS_VOZ=<pasta>`: gravações de voz nunca entram no repositório.

Todo teste imprime `PASSOU`/`FALHOU` e termina com `TODOS OS TESTES PASSARAM`.

## Regras

- **Português do Brasil** em textos, falas e comentários. Falas curtas e naturais.
- **Lógica nova em código puro** (`commands/` e afins) **com teste junto.** Android só onde precisa.
- **Voz:** toda fala usa a voz do HA (`speakHa`/`voiceFile`). Não crie caminhos de fala paralelos.
- **Threads** em segundo plano capturam `Throwable`, não só `Exception`.
- **Modo noite:** `Night.isNight`, volume por `Night.volume(prefs)`. Timers e alarmes pedidos falam sempre.
- **Privacidade:** nada de token, `.jks`, `provision.json`, `.env`, exemplos de voz, planta ou pacote de uma casa, endereços da rede, e-mails ou nomes de pessoas. `tools/dev.sh privacidade` barra o push (instale o gancho: `tools/checar_privacidade.sh --instalar`). Palavras só suas vão em `.privacidade-local` (fora do Git).
- **Fluxo:** branch → PR → CI verde → revisão do mantenedor. Ninguém faz push direto na `main`.
- **IA:** opcional. Diga no PR que usou; quem abre o PR responde pelo código. Chaves de IA ficam na sua máquina, nunca no repositório.
- **Commits** em português, explicando o porquê.
- **Todo bug corrigido ganha um teste** que falharia sem a correção (veja `docs/LICOES.md`). Uma mudança por PR.
- **Não afirme como o código funciona sem conferir:** cite o arquivo e a linha.
