#!/usr/bin/env bash
# Um comando para o dia a dia do projeto (pessoas ou IAs). É o mesmo que o CI roda.
#
#   tools/dev.sh preparar      baixa Kotlin 1.9.25 e o android.jar (API 33) para .ferramentas/
#   tools/dev.sh testar        roda todos os testes na JVM
#   tools/dev.sh compilar      gera o APK de teste em android/build/ (chave descartável)
#   tools/dev.sh privacidade   procura dados que não podem ir para o repositório
#   tools/dev.sh tudo          privacidade + testar + compilar
#
# Ferramentas do sistema (Debian/Ubuntu):
#   sudo apt-get install -y openjdk-21-jdk-headless aapt dalvik-exchange zipalign apksigner python3-aiohttp unzip zip
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
FERR="$RAIZ/.ferramentas"
KOTLIN_VERSAO=1.9.25

preparar() {
  mkdir -p "$FERR"
  if [ ! -x "$FERR/kotlinc/bin/kotlinc" ]; then
    echo "== baixando Kotlin $KOTLIN_VERSAO (o 2.x quebra o app no Android)"
    curl -fsSL -o "$FERR/kotlin.zip" "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSAO/kotlin-compiler-$KOTLIN_VERSAO.zip"
    unzip -q -o "$FERR/kotlin.zip" -d "$FERR" && rm "$FERR/kotlin.zip"
  fi
  if [ ! -f "$FERR/android-33.jar" ]; then
    if [ -n "${ANDROID_HOME:-}" ] && [ -f "$ANDROID_HOME/platforms/android-33/android.jar" ]; then
      cp "$ANDROID_HOME/platforms/android-33/android.jar" "$FERR/android-33.jar"
    else
      echo "== baixando android.jar (API 33)"
      curl -fsSL -o "$FERR/android-33.jar" "https://github.com/Sable/android-platforms/raw/master/android-33/android.jar"
    fi
  fi
  local falta=()
  for t in java aapt dalvik-exchange zipalign apksigner python3 unzip zip; do command -v "$t" >/dev/null || falta+=("$t"); done
  python3 -c "import aiohttp" 2>/dev/null || falta+=("python3-aiohttp")
  if [ ${#falta[@]} -gt 0 ]; then
    echo "! faltam no sistema: ${falta[*]}"
    echo "  sudo apt-get install -y openjdk-21-jdk-headless aapt dalvik-exchange zipalign apksigner python3-aiohttp unzip zip"
  fi
  echo "Pronto: KOTLINC=$FERR/kotlinc/bin/kotlinc ANDROID_JAR=$FERR/android-33.jar"
}

ambiente() {
  export KOTLINC="${KOTLINC:-$FERR/kotlinc/bin/kotlinc}"
  export ANDROID_JAR="${ANDROID_JAR:-$FERR/android-33.jar}"
  if [ ! -x "$KOTLINC" ] || [ ! -f "$ANDROID_JAR" ]; then preparar; fi
}

case "${1:-}" in
  preparar) preparar ;;
  testar) ambiente; "$RAIZ/tools/testar.sh" ;;
  compilar) ambiente; cd "$RAIZ/android" && VERSION_NAME="${VERSION_NAME:-0.0.0-dev}" VERSION_CODE="${VERSION_CODE:-1}" ./build.sh ;;
  privacidade) "$RAIZ/tools/checar_privacidade.sh" ;;
  tudo) "$RAIZ/tools/checar_privacidade.sh"; ambiente; "$RAIZ/tools/testar.sh"
        cd "$RAIZ/android" && VERSION_NAME="${VERSION_NAME:-0.0.0-dev}" VERSION_CODE="${VERSION_CODE:-1}" ./build.sh ;;
  *) sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
