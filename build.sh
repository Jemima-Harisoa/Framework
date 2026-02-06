#!/usr/bin/env bash
set -euo pipefail
trap 'echo "Erreur à la ligne $LINENO"; exit 1' ERR

# --- configuration simple ---
ROOT="$(pwd)"                     # dossier Framework/
TARGET_DIR="$ROOT/target"
TEST_DIR="$(dirname "$ROOT")/ControlTower/controlTowerBackoffice"    # prise en charge ControlTower/controlTowerBackoffice
if [ ! -d "$TEST_DIR" ]; then TEST_DIR="$(dirname "$ROOT")/ControlTower"; fi
if [ ! -d "$TEST_DIR" ]; then TEST_DIR="$(dirname "$ROOT")/controlTowerBackoffice"; fi
TEST_LIB="$TEST_DIR/src/main/webapp/WEB-INF/lib"

echo "Framework: $ROOT"
echo "Test dir : $TEST_DIR"
echo

# --- 1) build Maven (package) ---
echo "1) Compilation Maven..."
mvn -q clean package
echo "OK: compilation terminée."
echo

# --- 2) trouver le JAR le plus récent dans target/ ---
echo "2) Recherche du JAR dans $TARGET_DIR ..."
JAR=$(ls -t "$TARGET_DIR"/*.jar 2>/dev/null | head -n 1 || true)

if [ -z "$JAR" ]; then
  echo "❌ Aucun JAR trouvé dans $TARGET_DIR. Vérifie 'mvn package' et ton pom.xml."
  exit 1
fi

echo "JAR trouvé : $JAR"
echo

# --- 3) copier vers ControlTower/controlTowerBackoffice/src/main/webapp/WEB-INF/lib ---
echo "3) Copie vers $TEST_LIB ..."
if [ -d "$TEST_DIR" ]; then
  mkdir -p "$TEST_LIB"
  cp -v "$JAR" "$TEST_LIB/"
  echo "✅ Copie terminée."
else
  echo "⚠️ Dossier ControlTower/controlTowerBackoffice introuvable à l'emplacement attendu : $TEST_DIR"
  echo "   Crée un projet ControlTower/controlTowerBackoffice/ au même niveau que Framework/ puis relance."
  exit 1
fi

echo
echo "Build + copie terminés."
