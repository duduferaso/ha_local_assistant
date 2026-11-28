#!/usr/bin/env bash
# Roda todos os testes na JVM (sem Android): unitários, atualização e integração com o HA falso.
# É o que o CI roda. Uso: tools/testar.sh   (ou tools/dev.sh testar, que prepara as ferramentas)
#
# Requer: KOTLINC (Kotlin 1.9.x), java, python3 com aiohttp (para a integração).
# Testes do detector com gravações reais (tests/jvm_pessoal) só rodam com DADOS_VOZ=<pasta com calls/ e negs/>.
set -uo pipefail
cd "$(dirname "$0")/.."
KOTLINC="${KOTLINC:-kotlinc}"
export PATH="$(dirname "$(readlink -f "$(command -v "$KOTLINC")")"):$PATH"
A=android/app/src/main/java/io/github/duduferaso/halocalassistant
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
FALHAS=0
PASSOU=0
SUITES_RUINS=()

COMMON=$(ls $A/commands/*.kt $A/calendar/*.kt)
COMMON="$COMMON $A/network/TextCorrector.kt $A/network/CommandMatcher.kt $A/network/MiniWebSocket.kt \
 $A/home/Forecast.kt $A/home/FloorPlan.kt $A/home/Planta.kt $A/home/HomeParser.kt $A/home/HomeApi.kt \
 $A/settings/PacoteConfig.kt $A/domain/AssistantState.kt $A/wakeword/PersonalDetector.kt \
 $A/audio/VoiceActivityDetector.kt tests/jvm/OrgJson.kt tests/jvm/JLogStub.kt tests/jvm/PlantaTeste.kt"

conta() {  # $1 = nome, $2 = saída
  local p f
  p=$(grep -c '^PASSOU' <<<"$2" || true); f=$(grep -c '^FALHOU' <<<"$2" || true)
  PASSOU=$((PASSOU + p)); FALHAS=$((FALHAS + f))
  if [ "$f" -gt 0 ] || ! grep -q 'TODOS OS TESTES PASSARAM' <<<"$2"; then
    SUITES_RUINS+=("$1"); echo "✗ $1: $p passaram, $f falharam"; grep '^FALHOU' <<<"$2" | head -10 | sed 's/^/    /'
    grep -q 'TODOS OS TESTES PASSARAM' <<<"$2" || { [ "$f" -gt 0 ] || echo "    (não terminou: $(tail -1 <<<"$2"))"; }
  else
    echo "✓ $1: $p"
  fi
}

echo "== unitários"
for d in comandos timers treino listas tempo aprendizado corretor planta vad agenda pacote; do
  if ! "$KOTLINC" $COMMON tests/jvm_$d/*.kt -include-runtime -d "$OUT/$d.jar" 2>"$OUT/$d.err"; then
    SUITES_RUINS+=("jvm_$d (não compilou)"); echo "✗ jvm_$d não compilou"; grep -E "error" "$OUT/$d.err" | head -5; continue
  fi
  for f in tests/jvm_$d/*.kt; do
    grep -q '^fun main' "$f" || continue
    c=$(basename "$f" .kt)Kt
    conta "jvm_$d/$c" "$(cd tests && java -Dfile.encoding=UTF-8 -Duser.timezone=America/Sao_Paulo -cp "$OUT/$d.jar" "$c" 2>&1)"
  done
done

echo "== atualização"
PORTA=$((20000 + RANDOM % 20000))
python3 tests/jvm_update/server.py "$PORTA" & SRV=$!
"$KOTLINC" $A/update/Updater.kt tests/jvm/OrgJson.kt tests/jvm_update/UpdaterTest.kt -include-runtime -d "$OUT/upd.jar" 2>/dev/null
sleep 0.5
conta "jvm_update" "$(java -cp "$OUT/upd.jar" UpdaterTestKt "http://127.0.0.1:$PORTA" 2>&1)"
kill $SRV 2>/dev/null

echo "== integração com o HA falso"
if python3 -c "import aiohttp" 2>/dev/null; then
  for t in test_casa test_direto; do
    out=$(KOTLINC="$KOTLINC" bash tests/$t.sh 2>&1)
    # o HA falso às vezes demora a subir na primeira vez
    grep -q "Connection refused" <<<"$out" && { sleep 2; out=$(KOTLINC="$KOTLINC" bash tests/$t.sh 2>&1); }
    conta "$t" "$out"
  done
else
  echo "! python3 sem aiohttp: integração pulada (pip install aiohttp)"; SUITES_RUINS+=("integração (sem aiohttp)")
fi

if [ -n "${DADOS_VOZ:-}" ]; then
  echo "== detector com gravações reais ($DADOS_VOZ)"
  for t in ChamadaEmendadaTest ConversaTest TakeTrimTest; do
    "$KOTLINC" $A/wakeword/PersonalDetector.kt tests/jvm_pessoal/$t.kt tests/jvm/JLogStub.kt -include-runtime -d "$OUT/$t.jar" 2>/dev/null
    conta "jvm_pessoal/$t" "$(java -cp "$OUT/$t.jar" ${t}Kt "$DADOS_VOZ" 2>&1)"
  done
fi

echo
if [ ${#SUITES_RUINS[@]} -eq 0 ]; then
  echo "TODOS OS TESTES PASSARAM ($PASSOU checagens)"
else
  echo "FALHOU: ${SUITES_RUINS[*]} ($FALHAS checagens falharam, $PASSOU passaram)"; exit 1
fi
