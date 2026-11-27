# Home Control (ha_local_assistant)

Assistente de voz em português para o [Home Assistant](https://www.home-assistant.io/), rodando num celular Android na parede.

> **Em construção.** O projeto está sendo aberto a partir de um app que já roda numa casa de verdade. O primeiro lançamento aberto será a 2.0.0. O plano completo está em [`docs/PLANO.md`](docs/PLANO.md).

## O que ele faz

- **Palavra de ativação treinada com a voz de quem mora na casa.** Funciona com sotaque, de longe e com a TV ligada.
- **Tudo local.** O áudio só vai para o seu Home Assistant: a transcrição usa o Whisper, e o agente e a voz são os do próprio HA.
- **Comandos rápidos resolvidos no celular:** luzes, timers e alarmes, listas, previsão do tempo, agenda e treino intervalado.
- **Modular.** Cada casa liga só o que usa. A planta da casa é opcional, e dá para usar só o personagem com a voz.
- **Tela de parede.** Personagem animado e uma planta 3D da casa montada por você, em que tocar no cômodo acende a luz.

## Requisitos

- Home Assistant com um assistente de voz (STT, por exemplo faster-whisper; TTS, por exemplo Piper).
- Celular Android 8.0 ou mais novo, com processador de 64 bits (arm64).

## Licença

[Apache-2.0](LICENSE).
