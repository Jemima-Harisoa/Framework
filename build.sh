#!/bin/bash
set -euo pipefail
trap 'echo "Erreur à la ligne $LINENO"; exit 1' ERR

# Exécuter depuis Framework/
FRAMEWORK_DIR="$(pwd)"
PROJECT_NAME="$(basename "$FRAMEWORK_DIR")"
SRC_DIR="$FRAMEWORK_DIR/src"
LIB_DIR="$FRAMEWORK_DIR/lib"
BUILD_CLASSES="$FRAMEWORK_DIR/build/classes"
JAR_NAME="${PROJECT_NAME}-servlets.jar"
JAR_PATH="$LIB_DIR/$JAR_NAME"

# Cible test (Test étant au même niveau que Framework) : détecte Test/ ou test/
ROOT_DIR="$(dirname "$FRAMEWORK_DIR")"
if [ -d "$ROOT_DIR/Test" ]; then
    TEST_DIR="$ROOT_DIR/Test"
elif [ -d "$ROOT_DIR/test" ]; then
    TEST_DIR="$ROOT_DIR/test"
else
    echo "Aucun dossier 'Test' ou 'test' trouvé au même niveau que $FRAMEWORK_DIR."
    echo "Crée un dossier 'Test' (ou 'test') avec src/main/webapp/... puis relance."
    exit 1
fi

# Chemin exact webapp dans Test selon ta structure
TEST_WEBINF_LIB="$TEST_DIR/src/webapp/WEB-INF/lib"

echo "Framework dir    : $FRAMEWORK_DIR"
echo "Framework lib    : $LIB_DIR"
echo "Detected TEST dir: $TEST_DIR"
echo "Test webinf lib  : $TEST_WEBINF_LIB"
echo

# Préparations
mkdir -p "$LIB_DIR" "$BUILD_CLASSES"
mkdir -p "$TEST_WEBINF_LIB"

# --- Trouver explicitement un servlet API jar dans $LIB_DIR ---
SERVLET_API_JAR=""
# Patterns dans l'ordre de préférence
patterns=("servlet-api" "jakarta-servlet-api" "jakarta.servlet" "javax.servlet" "jakarta-servlet")
for p in "${patterns[@]}"; do
    # lister candidats, exclure le jar du framework et tout jar contenant "-servlets"
    candidate=$(ls "$LIB_DIR"/*"$p"*.jar 2>/dev/null | grep -v "$JAR_NAME" | grep -v -i "servlet[s-]*jar" | head -n 1 || true)
    if [ -n "$candidate" ]; then
        SERVLET_API_JAR="$candidate"
        break
    fi
done

# fallback : si aucun trouvé, tenter quand même de prendre un jar dont le nom contient 'servlet' 
if [ -z "$SERVLET_API_JAR" ]; then
    candidate=$(ls "$LIB_DIR"/*servlet*.jar 2>/dev/null | grep -v "$JAR_NAME" | head -n 1 || true)
    if [ -n "$candidate" ]; then
        SERVLET_API_JAR="$candidate"
    fi
fi

if [ -z "$SERVLET_API_JAR" ]; then
    echo "⚠️  Aucune bibliothèque d'API servlet trouvée dans $LIB_DIR."
    echo "Place un fichier comme 'servlet-api.jar' ou 'jakarta.servlet-api.jar' dans $LIB_DIR puis relance."
    exit 1
fi

echo "Utilisation du servlet API pour la compilation : $SERVLET_API_JAR"
echo

# 1) Compiler les sources du framework
echo "=== Compilation du framework (src -> $BUILD_CLASSES) ==="
if find "$SRC_DIR" -name "*.java" | grep -q . 2>/dev/null; then
    # robust: find -> xargs -0
    find "$SRC_DIR" -name "*.java" -print0 | xargs -0 javac -cp "$SERVLET_API_JAR" -d "$BUILD_CLASSES"
    echo "Compilation framework terminée."
else
    echo "Aucun fichier .java trouvé dans $SRC_DIR — skip compilation."
fi

# 2) Créer le JAR du framework (si classes présentes)
echo
echo "=== Création du JAR du framework ==="
if [ -d "$BUILD_CLASSES/com" ]; then
    jar -cvf "$JAR_PATH" -C "$BUILD_CLASSES" com
    echo "JAR créé : $JAR_PATH"
else
    echo "Aucune classe 'com' trouvée dans $BUILD_CLASSES — pas de JAR créé."
fi

# 3) Copier le JAR vers Test/src/main/webapp/WEB-INF/lib (si créé)
echo
echo "=== Copie du JAR vers la zone de test ==="
if [ -f "$JAR_PATH" ]; then
    mkdir -p "$TEST_WEBINF_LIB"
    cp -v "$JAR_PATH" "$TEST_WEBINF_LIB/"
    echo "Copie OK -> $TEST_WEBINF_LIB/$(basename "$JAR_PATH")"
else
    echo "Aucun JAR framework à copier."
fi

echo
echo "build terminé."
