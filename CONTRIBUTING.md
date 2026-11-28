# Como contribuir

Que bom que você quer ajudar! O Home Control é feito para casas de verdade, então o que mais importa é: **funcionar bem no dia a dia, sem expor a casa de ninguém.**

## Antes de começar

- Leia o [`AGENTS.md`](AGENTS.md): arquitetura, regras e comandos.
- Veja o [plano](docs/PLANO.md) e as [issues](https://github.com/duduferaso/ha_local_assistant/issues). As marcadas `boa-primeira-tarefa` são um bom começo.
- Para algo grande (módulo novo, mudança de formato), abra uma issue antes para combinar o caminho.

## Preparar o ambiente

O jeito mais fácil é abrir no **GitHub Codespaces** (ou no VS Code com Dev Containers): o `.devcontainer/` instala tudo.

Na sua máquina (Debian/Ubuntu):

```bash
sudo apt-get install -y openjdk-21-jdk-headless aapt dalvik-exchange zipalign apksigner python3-aiohttp unzip zip
tools/dev.sh preparar                      # Kotlin 1.9.25 + android.jar em .ferramentas/
tools/checar_privacidade.sh --instalar     # checagem de privacidade antes de cada push
```

## O ciclo

1. Faça um fork e crie uma branch para a tarefa.
2. Programe. **Lógica nova em código puro, com teste** (veja `commands/` e `tests/jvm_*`).
3. `tools/dev.sh testar` e `tools/dev.sh privacidade` até tudo passar.
4. Abra o PR para a `main` preenchendo o modelo. O CI roda privacidade, testes e compilação.
5. O mantenedor revisa. Ajustes pedidos entram na mesma branch.

Para testar no celular: `tools/dev.sh compilar` gera um APK de teste (chave descartável). Ele instala ao lado do app oficial só se você desinstalar o oficial antes, porque têm o mesmo id e assinaturas diferentes.

## Regras que o CI e a revisão cobram

- **Português do Brasil** em textos, falas, comentários e commits.
- **Kotlin 1.9.x** (o 2.x quebra o app no Android). Sem `compareBy(vararg)`, `thenBy`, `nullsFirst/Last`.
- **Privacidade:** nada de token, endereço da sua rede, e-mail, nome de pessoa, gravação, exemplos de voz (`.tpl`), planta ou pacote de configuração de uma casa. Use exemplos genéricos (`examples/planta.json`, `tests/jvm/PlantaTeste.kt`).
- **Luzes:** sempre `turn_on`/`turn_off`, nunca `toggle`. Regra do que é luz só em `LightCommands.isLight`.
- **Voz:** toda fala pela voz do Home Assistant.

## Usando IA para programar

Pode, e o projeto está preparado para isso (`AGENTS.md`, `CLAUDE.md`, `.claude/commands/`, `tools/dev.sh`). A IA é **opcional**: configure a sua (conta e chave da ferramenta) no seu ambiente e use quando quiser.

- **Diga no PR** que usou IA e qual. **Quem abre o PR responde pelo código:** revise o que a IA fez.
- Chaves e contas de IA ficam na sua máquina: nunca no repositório, no CI, em logs ou colados em issues.
- O app publicado não chama IA de terceiros.

## Publicar versões

Só o mantenedor publica (`tools/publicar_versao.sh`), porque só ele tem a chave que os celulares aceitam para atualizar.
