#!/usr/bin/env bash
# Barra o nome antigo (Jarvis) em textos que aparecem para quem usa o app: telas, avisos, falas, manifesto.
# Lição da 2.0.0-alfa.1: a troca de nome procurou "Jarvis" e deixou passar "JARVIS HOME" no título.
# Por isso a busca não diferencia maiúsculas. Nomes internos (classes, temas, chaves de preferência,
# threads, o broadcast e o pacote do app antigo, a entidade input_text.jarvis_aviso) estão liberados abaixo.
set -uo pipefail
cd "$(dirname "$0")/.."
SRC=android/app/src/main
LIBERADO='Jarvis Home|br\.com\.jarvishome|jarvis_aviso|Jarvis aviso|X-Jarvis|"jarvis"|"jarvis_secure"|"jarvis_terminal_key"|"jarvis_service"|"jarvis_updates"|"jarvis_update"|"jarvis\.apk"|"jarvis-[a-z-]*"|vindo_do_jarvis|jarvis-tpl-1|\(voz\|comando\|jarvis\)|"Jarvis, liga'
ACHOU=0
# 1) literais de texto no Kotlin
hits=$(grep -rnI --include=*.kt -iE '"[^"]*jarvis[^"]*"' "$SRC/java" | grep -vE "JLog\.|^\S+:\s*import " | grep -vE "$LIBERADO" || true)
# 2) recursos e manifesto: só rótulos e textos (android:label, android:text, <string>)
hits2=$(grep -rnI -iE '(android:(label|text|description)="[^"]*jarvis)|(<string[^>]*>[^<]*jarvis)' "$SRC/res" "$SRC/AndroidManifest.xml" || true)
for h in "$hits" "$hits2"; do
  if [ -n "$h" ]; then ACHOU=1; echo "$h" | sed 's/^/    /'; fi
done
if [ $ACHOU -eq 0 ]; then echo "Textos: nenhum nome antigo à vista."; else
  echo "✗ Nome antigo (Jarvis) em texto visível. Troque por Home Control ou, se for interno, libere em tools/checar_textos.sh."; exit 1; fi
