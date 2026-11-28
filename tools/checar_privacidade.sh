#!/usr/bin/env bash
# Barra o que não pode ir para um repositório público: endereços da rede de casa, tokens, chaves,
# e-mails, exemplos de voz, plantas e pacotes de configuração de alguém.
# Roda no CI e antes de cada push (instale o gancho: tools/checar_privacidade.sh --instalar).
#
# Palavras só suas (nomes da família, da rua, das luzes…): ponha uma expressão regular por linha em
# .privacidade-local (fica fora do Git). A checagem também barra essas palavras.
set -uo pipefail
cd "$(dirname "$0")/.."

if [ "${1:-}" = "--instalar" ]; then
  mkdir -p .git/hooks
  printf '#!/bin/sh\nexec tools/checar_privacidade.sh\n' > .git/hooks/pre-push && chmod +x .git/hooks/pre-push
  echo "Gancho instalado: a checagem roda antes de cada push."; exit 0
fi

ARQS=$( (git ls-files; git diff --cached --name-only --diff-filter=AM) 2>/dev/null | sort -u | while read -r f; do [ -f "$f" ] && echo "$f"; done)
[ -z "$ARQS" ] && ARQS=$(find . -type f -not -path './.git/*' -not -path './.ferramentas/*' -not -path './android/build/*' | sed 's#^\./##')
ACHOU=0
avisa() { echo "✗ $1"; ACHOU=1; }

# 1) arquivos que nunca entram
while read -r f; do
  case "$f" in
    *.jks|*.keystore|*.p12|*/provision.json|provision.json|.env|*/.env|*.senha) avisa "arquivo secreto: $f" ;;
    *.tpl) avisa "exemplos de voz (pessoais): $f" ;;
    *home-control-pacote*.json|*jarvis-backup*.json) avisa "pacote/backup de configuração de alguém: $f" ;;
    *planta.json) [[ "$f" == examples/* ]] || avisa "planta de uma casa fora de examples/: $f" ;;
    *.wav|*.m4a|*.mp3|*.ogg|*.f32) avisa "gravação de áudio: $f" ;;
  esac
done <<<"$ARQS"

# 2) conteúdo (só arquivos de texto)
padroes=(
  '192\.168\.[0-9]{1,3}\.[0-9]{1,3}|(^|[^0-9.])10\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}|172\.(1[6-9]|2[0-9]|3[01])\.[0-9]{1,3}\.[0-9]{1,3}::endereço da rede de casa'
  'eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}::token (JWT) do Home Assistant'
  'gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}::token do GitHub'
  '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}::e-mail'
)
if [ -f .privacidade-local ]; then
  while read -r l; do [ -n "$l" ] && [[ "$l" != \#* ]] && padroes+=("$l::palavra de .privacidade-local"); done < .privacidade-local
fi
for p in "${padroes[@]}"; do
  re="${p%%::*}"; nome="${p##*::}"
  # e-mails permitidos: noreply de coautoria e exemplos
  hits=$(echo "$ARQS" | xargs -d '\n' grep -nIE "$re" 2>/dev/null \
    | grep -vE 'noreply@anthropic\.com|@example\.(com|org)|@users\.noreply\.github\.com|tools/checar_privacidade\.sh:' || true)
  if [ -n "$hits" ]; then avisa "$nome:"; echo "$hits" | head -10 | sed 's/^/    /'; fi
done

if [ $ACHOU -eq 0 ]; then echo "Privacidade: nada encontrado."; else
  echo; echo "Tire esses dados antes de enviar (ou, se for falso positivo, explique no PR)."; exit 1; fi
