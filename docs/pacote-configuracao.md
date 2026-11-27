# Pacote de configuração (formato 1)

O pacote leva a casa inteira num arquivo JSON: conexão, frase de ativação, exemplos de voz, módulos com os seus ajustes, planta, som, modo noite, tela e aprendizado. Ele serve para três coisas:

1. **Migrar do Jarvis Home** (app antigo): a versão 1.16.0 em diante exporta neste formato ("Exportar para o Home Control").
2. **Backup**: o Home Control exporta e importa o mesmo formato.
3. **Levar a casa para outro celular.**

Nome sugerido do arquivo: `home-control-pacote-AAAA-MM-DD-HHMM.json`.

## Regras gerais

- Codificação UTF-8. Nomes de campos em português, sem acento, em camelCase.
- **Campos desconhecidos são ignorados** por quem importa: versões novas podem acrescentar campos sem mudar o `formato`.
- **Campo que falta usa o padrão** do app.
- O `formato` só sobe quando um campo muda de sentido ou some. Quem importa recusa pacote de formato maior que o seu, com a mensagem "pacote feito por uma versão mais nova; atualize o app".
- **O token do Home Assistant só vai se a pessoa escolher** "com token" na exportação. Sem token, a importação pede o token.
- **Nada de áudio:** o pacote leva só os exemplos de voz já processados (características do som), nunca gravações.

## Estrutura

```json
{
  "tipo": "home-control-pacote",
  "formato": 1,
  "criadoEm": "2026-11-27T19:00:00-03:00",
  "origem": { "app": "JarvisHome", "versao": "1.16.0" },
  "conexao": { "endereco": "http://homeassistant.local:8123", "assistente": "", "token": "(só com token)" },
  "ativacao": { "frase": "Hey Tony", "aprenderComUso": true },
  "voz": { "formatoExemplos": "jarvis-tpl-1", "exemplos": "<base64>", "aprendidos": "<base64>" },
  "modulos": {
    "luzes":  { "ligado": true, "naTela": true, "comandosTreinados": [], "corretor": { "ligado": true, "vocabularioExtra": "" } },
    "planta": { "ligado": true, "naTela": true, "planta": { "formato": 1, "...": "ver Planta" } },
    "agenda": { "ligado": true, "naTela": true, "agendaNova": "", "lembretes": true, "antecedenciaMin": 0 },
    "listas": { "ligado": true, "compras": "", "tarefas": "" },
    "clima":  { "ligado": true, "naTela": true },
    "timers": { "ligado": true },
    "treino": { "ligado": true, "minutos": 30, "execucaoSeg": 30, "descansoSeg": 30, "series": 5, "exercicios": 5, "listaExercicios": "" },
    "avisos": { "ligado": true }
  },
  "som":   { "bipe": true, "volume": 90, "silencioMs": 900 },
  "noite": { "ligado": true, "inicio": 23, "fim": 6, "respostasBaixas": true, "volume": 35, "falarAvisos": false },
  "tela":  { "manterLigada": true, "brilhoMinimo": false, "relogio": true, "reduzirMovimento": false },
  "sistema": { "iniciarAoLigar": true, "atualizarSozinho": true },
  "aprendizado": { "correcoes": [] },
  "extras": { "servidor": { "limiteTemperatura": 75, "sensores": "" } }
}
```

| Campo | O que é |
| --- | --- |
| `tipo` | Sempre `home-control-pacote`. Outro valor: não é um pacote. |
| `origem` | App e versão que exportou. Só informativo. |
| `conexao.endereco` | Endereço do Home Assistant. |
| `conexao.assistente` | Id do assistente (pipeline) do HA. Vazio = o preferido. |
| `ativacao.frase` | Frase de ativação mostrada e tirada do começo da transcrição. |
| `ativacao.aprenderComUso` | Reforço: guarda chamadas que deram certo como exemplos extras. |
| `voz.formatoExemplos` | Formato binário dos exemplos. `jarvis-tpl-1` = o `vozes.tpl` do detector pessoal (características log-mel, limiar e exemplos). |
| `voz.exemplos` | Exemplos treinados, em base64. Já trazem o limiar escolhido. |
| `voz.aprendidos` | Exemplos aprendidos no uso (reforço), em base64. Opcional. |
| `modulos.*.ligado` | Chave do módulo. Desligado: comandos de voz vão direto para o agente do HA e as peças saem da tela. |
| `modulos.*.naTela` | Mostra a peça do módulo na tela principal (quando o módulo tem peça). |
| `modulos.luzes.comandosTreinados` | Comandos de luz treinados (frase como o Whisper ouviu → luzes e ação). Formato interno do app, repassado como veio. |
| `modulos.luzes.corretor` | Corretor de texto por vocabulário e frases extras. |
| `modulos.planta.planta` | A planta, no formato abaixo. Sem planta: `ligado` = false. |
| `modulos.agenda.agendaNova` | Entidade `calendar.*` que recebe compromissos novos. Vazio = a primeira que aceitar. |
| `modulos.listas.compras` / `tarefas` | Entidades `todo.*`. Vazio = escolhe pelo nome. |
| `aprendizado.correcoes` | Correções aprendidas ("ele entendeu X, era Y"). Formato interno, repassado como veio. |
| `extras` | Ajustes que ainda não viraram módulo. Quem não conhece ignora. |

## Planta

A planta segue o formato descrito no [plano](PLANO.md#formato-da-planta-plantajson), com três campos opcionais a mais, usados na conversão da planta do Jarvis Home:

| Campo do cômodo | O que é |
| --- | --- |
| `rotulo` | Posição (x, y) do nome do cômodo na planta. Sem ele, o nome vai no centro da maior parte. |
| `palavrasLuz` | Palavras para achar a luz do cômodo no HA pelo nome (por exemplo "servico", "lavanderia"). |
| `evitarPalavras` | Palavras que fazem uma luz **não** ser deste cômodo (por exemplo "jantar" para a sala de estar). |

**Como as paredes saem dos cômodos** (o app não guarda paredes):

1. Toda borda entre dois cômodos diferentes é parede, **menos** entre cômodos listados em `integrados`.
2. Toda borda de cômodo que não encosta em outro cômodo é parede externa.
3. Bordas entre partes do mesmo cômodo não são parede.
4. Cada item de `portas` (x1, y1, x2, y2, sobre uma dessas bordas) abre um vão na parede.

A conversão da planta do Jarvis Home foi conferida assim: as paredes calculadas por essas regras são exatamente as 14 paredes desenhadas à mão no app antigo, com 3 pares integrados e 5 portas.

## Convivência no mesmo celular

Durante a transição, o Jarvis Home e o Home Control ficam instalados no mesmo celular, um ativo por vez: o microfone é de um só, e os dois leem os mesmos avisos do HA.

| Quem | Recebe o pedido de pausa por | Pede ao outro que pause com |
| --- | --- | --- |
| Jarvis Home (`br.com.jarvishome`) | broadcast `br.com.jarvishome.PAUSAR` | broadcast `io.github.duduferaso.halocalassistant.PAUSAR`, com `setPackage` |
| Home Control (`io.github.duduferaso.halocalassistant`) | broadcast `io.github.duduferaso.halocalassistant.PAUSAR` | broadcast `br.com.jarvishome.PAUSAR`, com `setPackage` |

**Pausa total** (os dois apps fazem igual):

- solta o microfone;
- não lê nem apaga `input_text.jarvis_aviso` e não fala lembretes da agenda;
- não toca timers nem alarmes (o fim de um timer aparece só na tela);
- não inicia sozinho quando o celular liga;
- mostra que está em pausa; tocar e segurar no rosto volta a usar este app e pede ao outro que pause.

O botão "Trocar para o outro app" pausa este app e abre o outro com o extra `vindo_do_jarvis` (ou `vindo_do_home_control`), que faz o outro sair da pausa.
