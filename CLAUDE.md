# Home Control — instruções para o Claude Code

O guia do projeto está em `AGENTS.md` (vale para pessoas e para qualquer IA):

@AGENTS.md

## Específico do Claude Code

- Comandos prontos em `.claude/commands/`: `/testar`, `/compilar`, `/privacidade`, `/novo-modulo <descrição>`.
- Antes de propor um commit, rode `tools/dev.sh testar` e `tools/dev.sh privacidade`.
- Trabalhe numa branch e abra PR para a `main`; nunca faça push direto nela.
- Termine as mensagens de commit com a linha de coautoria que a sessão indicar.
