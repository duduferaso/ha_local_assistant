#!/usr/bin/env bash
# Publica uma versão do Home Control no GitHub Releases. Só o mantenedor: precisa da chave oficial.
#
#   VERSION_NAME=2.0.0-alfa.1 VERSION_CODE=1 NOTAS="o que mudou" \
#   KEYSTORE=~/chaves/home-control-release.jks KS_PASS=... tools/publicar_versao.sh
#
# - roda a checagem de privacidade e todos os testes antes;
# - compila SEM token (nunca publique um APK com token);
# - cria a tag vVERSION_NAME no commit atual e o Release com o APK HomeControl-<versão>-<código>.apk;
# - versão com "-" (2.0.0-alfa.1) sai como pré-versão: só quem liga "receber versões de teste" recebe.
# O versionCode tem de subir sempre. Requer gh autenticado com permissão de escrita no repositório.
#
# CANAL=branch: em vez do Release, publica na branch órfã "atualizacoes" (versao.json + APK), que o app
# também lê quando o repositório não tem Release. Útil onde não dá para criar Releases. A branch guarda só
# a última versão e vale para todos (inclusive pré-versões).
set -euo pipefail
cd "$(dirname "$0")/.."
: "${VERSION_NAME:?defina VERSION_NAME}" "${VERSION_CODE:?defina VERSION_CODE}" "${KEYSTORE:?defina KEYSTORE (chave oficial)}" "${KS_PASS:?defina KS_PASS}"
NOTAS="${NOTAS:-}"
REPO="${REPO:-duduferaso/ha_local_assistant}"
unset PROVISION_TOKEN PROVISION_URL

tools/checar_privacidade.sh
[ "${PULAR_TESTES:-}" = "1" ] || tools/dev.sh testar

( cd android && VERSION_NAME="$VERSION_NAME" VERSION_CODE="$VERSION_CODE" KEYSTORE="$KEYSTORE" KS_PASS="$KS_PASS" ./build.sh )
APK_LOCAL="android/build/HomeControl-$VERSION_NAME.apk"
NOME="HomeControl-$VERSION_NAME-$VERSION_CODE.apk"
SHA=$(sha256sum "$APK_LOCAL" | cut -d' ' -f1)
PRE=false; [[ "$VERSION_NAME" == *-* ]] && PRE=true
COMMIT=$(git rev-parse HEAD)

if [ "${CANAL:-release}" = "branch" ]; then
  echo "== branch atualizacoes: $NOME"
  W="$(mktemp -d)"
  git -C "$W" init -q -b atualizacoes
  cp "$APK_LOCAL" "$W/$NOME"
  python3 - "$W" "$NOME" "$VERSION_NAME" "$VERSION_CODE" "$NOTAS" <<'PY'
import hashlib, json, os, sys
w, name, vn, vc, notes = sys.argv[1:]
p = os.path.join(w, name)
json.dump({"versionCode": int(vc), "versionName": vn, "notes": notes, "apk": name,
           "size": os.path.getsize(p), "sha256": hashlib.sha256(open(p, "rb").read()).hexdigest()},
          open(os.path.join(w, "versao.json"), "w"), ensure_ascii=False, indent=2)
PY
  printf '# Atualizações do Home Control\n\nSó a última versão. Gerada por `tools/publicar_versao.sh` (CANAL=branch, sem token).\n' > "$W/README.md"
  git -C "$W" add -A
  git -C "$W" -c user.name="${GIT_AUTHOR_NAME:-Home Control}" -c user.email="${GIT_AUTHOR_EMAIL:-noreply@users.noreply.github.com}" commit -q -m "Versão $VERSION_NAME ($VERSION_CODE), commit ${COMMIT:0:7}"
  git -C "$W" push -q --force "https://github.com/$REPO" atualizacoes:atualizacoes
  rm -rf "$W"
  echo "Publicado: https://github.com/$REPO/tree/atualizacoes ($NOME)"
  exit 0
fi

echo "== Release v$VERSION_NAME (pré-versão: $PRE) no commit ${COMMIT:0:7}"
CORPO=$(printf '%s\n\n---\nAPK: `%s` · SHA-256 `%s`' "$NOTAS" "$NOME" "$SHA")
ID=$(gh api "repos/$REPO/releases" -f tag_name="v$VERSION_NAME" -f target_commitish="$COMMIT" \
  -f name="Home Control $VERSION_NAME" -f body="$CORPO" -F prerelease=$PRE --jq '.id')
gh api --method POST -H "Content-Type: application/vnd.android.package-archive" \
  "https://uploads.github.com/repos/$REPO/releases/$ID/assets?name=$NOME" --input "$APK_LOCAL" --jq '.browser_download_url'
echo "Publicado: https://github.com/$REPO/releases/tag/v$VERSION_NAME"
