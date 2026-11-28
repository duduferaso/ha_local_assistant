# Home Control (ha_local_assistant)

Assistente de voz em português para o [Home Assistant](https://www.home-assistant.io/), rodando num celular Android na parede.

> **Em construção (2.0.0-alfa).** O projeto está sendo aberto a partir de um app que já roda numa casa de verdade. O primeiro lançamento estável será a 2.0.0. Plano: [`docs/PLANO.md`](docs/PLANO.md).

## O que ele faz

- **Palavra de ativação treinada com a voz de quem mora na casa.** Funciona com sotaque, de longe e com a TV ligada.
- **Tudo local.** O áudio só vai para o seu Home Assistant (transcrição pelo Whisper do HA; agente e voz do próprio HA). Nenhuma telemetria.
- **Comandos rápidos resolvidos no celular:** luzes, timers e alarmes, listas, previsão do tempo, agenda e treino intervalado. O resto vai para o agente de conversa do HA.
- **Tela de parede:** personagem animado e, se você quiser, a planta da sua casa em 3D: tocar no cômodo acende a luz.
- **Seus dados ficam com você:** voz, planta e ajustes ficam no celular; um pacote de configuração leva tudo para outro aparelho.

## Requisitos

- Home Assistant com um assistente de voz configurado: STT (por exemplo, faster-whisper), TTS (por exemplo, Piper) e o agente de conversa.
- Um usuário do Home Assistant **sem administrador** e um token de longa duração dele.
- Celular **Android 8.0 ou mais novo, com processador de 64 bits (arm64)**.

## Instalar (pré-versão)

1. Baixe o APK `HomeControl-…apk` da [página de versões](https://github.com/duduferaso/ha_local_assistant/releases) e instale (o Android pede para permitir a instalação).
2. Abra o app, toque na engrenagem e informe o endereço do Home Assistant e o token.
3. Em **Voz**, treine a frase de ativação com a voz de cada pessoa da casa, de onde vocês costumam chamar. Quem vem do Jarvis Home pode, em vez disso, usar **Sistema → Importar pacote**.

As próximas versões chegam pelo próprio app (Engrenagem → Sistema → Procurar atualização).

## Contribuir

Veja [`CONTRIBUTING.md`](CONTRIBUTING.md) e o guia para quem programa, [`AGENTS.md`](AGENTS.md). Em resumo: `tools/dev.sh testar`, `tools/dev.sh privacidade`, PR. Dá para programar com a sua IA preferida, se quiser.

Falhas de segurança: [`SECURITY.md`](SECURITY.md).

## Licença

[Apache-2.0](LICENSE). Componentes de terceiros em [`NOTICE`](NOTICE).
