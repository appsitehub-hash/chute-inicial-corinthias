#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

USAGE="Usage: ./run.sh [--gui|--cli]"
MODE="--gui"
if [ "$#" -gt 0 ]; then
  case "$1" in
    --gui|-g) MODE="--gui" ;;
    --cli|-c) MODE="" ;;
    -h|--help) echo "$USAGE"; exit 0 ;;
    *) echo "Unknown arg: $1"; echo "$USAGE"; exit 1 ;;
  esac
fi

# ensure DB dir exists
mkdir -p DB

# prefer kotlinc if available
if command -v kotlinc >/dev/null 2>&1; then
  echo "Found kotlinc. Compiling Kotlin sources..."
  rm -f app.jar
  # include both src and GUI directories
  kotlinc -d app.jar -include-runtime $(find src GUI -name "*.kt")
  echo "Running jar (mode=$MODE)"
  # run by specifying the generated Kotlin top-level main class
  MAIN_CLASS="com.corinthians.app.AppCorinthiansKt"
  if [ "$MODE" = "--gui" ]; then
    java -cp app.jar "$MAIN_CLASS" --gui
  else
    java -cp app.jar "$MAIN_CLASS"
  fi
  exit 0
fi

# else prefer gradle (wrapper if present)
if [ -x "./gradlew" ]; then
  echo "Using Gradle wrapper ./gradlew"
  if [ "$MODE" = "--gui" ]; then
    ./gradlew run --args="--gui"
  else
    ./gradlew run --args=""
  fi
  exit 0
fi

if command -v gradle >/dev/null 2>&1; then
  echo "Using system gradle"
  if [ "$MODE" = "--gui" ]; then
    gradle run --args="--gui"
  else
    gradle run --args=""
  fi
  exit 0
fi

cat <<EOF
No Kotlin compiler (kotlinc) or Gradle found.
Install one of these options and retry:
- SDKMAN (recommended): https://sdkman.io/ (then: sdk install kotlin; sdk install gradle)
- or install kotlinc/kotlin and gradle via your package manager.
EOF
exit 2
