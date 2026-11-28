# Mudanças

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões: `MAIOR.MENOR.CORREÇÃO`; pré-versões com `-alfa.N`.

## [Não publicado] — 2.0.0

Primeiro lançamento aberto: o Jarvis Home 1.16 virando Home Control.

### Adicionado
- Pacote de configuração (formato 1): importa a casa do Jarvis Home ou de um backup e exporta a sua.
- Planta de cada casa em `planta.json`, com paredes calculadas a partir dos cômodos; sem planta, só o personagem.
- Pausa total e troca com o Jarvis Home, para os dois apps conviverem no mesmo celular.
- Atualização pelo GitHub Releases, com conferência do SHA-256.
- Ferramentas (`tools/dev.sh`, checagem de privacidade, publicação), CI, ambiente pronto e guias para quem programa.

### Mudado
- Id do app `io.github.duduferaso.halocalassistant`, nome Home Control, chave de assinatura própria.
- Sem exemplos de voz de fábrica: cada casa treina a própria voz (ou importa).
- Endereço do Home Assistant começa vazio.

### Removido
- Orquestrador Python (sem uso).
