#!/bin/bash
set -u

# -------------------------
# Déclarations (déjà définies)
# -------------------------
SRC_DIR="$(pwd)"                      # répertoire courant (racine du projet/framework)
PROJECT_NAME="$(basename "$SRC_DIR")" # nom du répertoire courant
LIB_DIR="$SRC_DIR/lib"                # ici on place le jar du framework
WEB_DIR="$SRC_DIR/src/main/webapp"    # webapp du framework (templates, web.xml, ...)
CLASSES_DIR="$WEB_DIR/WEB-INF/classes"
BUILD_DIR="$SRC_DIR/build"
WAR_FILE="$SRC_DIR/${PROJECT_NAME}.war"
TOMCAT_DIR="/opt/tomcat"
TOMCAT_WEBAPPS_DIR="$TOMCAT_DIR/webapps"
TOMCAT_BIN="$TOMCAT_DIR/bin/startup.sh"

# Dossier test (espace de travail des devs) - contient webapp qui sera transformée en WAR
TEST_DIR="$SRC_DIR/test"
TEST_WEBAPP_DIR="$TEST_DIR/webapp"
TEST_SRC_DIR="$TEST_DIR/src"
TEST_WEBINF_LIB="$TEST_WEBAPP_DIR/WEB-INF/lib"
TEST_WEBINF_CLASSES="$TEST_WEBAPP_DIR/WEB-INF/classes"

# -------------------------
# Préparations
# -------------------------
mkdir -p "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"
mkdir -p "$LIB_DIR"

echo "Project: $PROJECT_NAME"
echo "SRC_DIR: $SRC_DIR"
echo "LIB_DIR: $LIB_DIR"
echo "TEST_DIR: $TEST_DIR"

# -------------------------
# Étape 1 : Compiler la partie framework (src/main/java)
#            -> classes dans src/main/webapp/WEB-INF/classes
# -------------------------
echo
echo "=== Étape 1 : Compile framework sources (src/main/java) ==="
JAVA_SOURCES=$(find "$SRC_DIR/src/main/java" -name "*.java" 2>/dev/null || true)
if [ -z "$JAVA_SOURCES" ]; then
    echo "No framework Java sources found under src/main/java (skipping framework compilation)."
else
    javac -cp "$LIB_DIR/servlet-api.jar" -d "$CLASSES_DIR" $JAVA_SOURCES
    if [ $? -ne 0 ]; then
        echo "Compilation of framework sources failed!"
        exit 1
    fi
    echo "Framework sources compiled to $CLASSES_DIR"
fi

# -------------------------
# Étape 2 : Créer le JAR des servlets (framework) dans LIB_DIR
# -------------------------
echo
echo "=== Étape 2 : Build framework JAR into $LIB_DIR ==="
JAR_NAME="${PROJECT_NAME}-servlets.jar"
JAR_PATH="$LIB_DIR/$JAR_NAME"

if [ -d "$CLASSES_DIR/com" ]; then
    jar -cvf "$JAR_PATH" -C "$CLASSES_DIR" com
    if [ $? -ne 0 ]; then
        echo "Failed to create servlet JAR at $JAR_PATH"
        exit 1
    fi
    echo "JAR created at: $JAR_PATH"
else
    echo "No 'com' classes found in $CLASSES_DIR - skipping jar creation."
fi

# -------------------------
# Étape 2.5 : Préparer la zone de test (test/webapp)
#   - copier la webapp de test (si elle existe) ou initialiser
#   - copier les jars de LIB_DIR dans test/webapp/WEB-INF/lib
# -------------------------
echo
echo "=== Étape 2.5 : Prepare test webapp and copy libs ==="
mkdir -p "$TEST_WEBAPP_DIR"
mkdir -p "$TEST_WEBINF_LIB"
mkdir -p "$TEST_WEBINF_CLASSES"

# Si la webapp de framework (WEB_DIR) doit servir de base pour test, on peut la copier :
# (Si tu préfères que test webapp soit indépendante, commente la ligne ci-dessous)
#if [ -d "$WEB_DIR" ]; then
#    # copie le contenu du webapp framework dans test/webapp (sans écraser libs/test)
#    cp -r "$WEB_DIR"/. "$TEST_WEBAPP_DIR"/
#fi

# Copier tous les jars présents dans LIB_DIR vers test/webapp/WEB-INF/lib
if compgen -G "$LIB_DIR/*.jar" >/dev/null; then
    cp -r "$JAR_PATH" "$TEST_WEBINF_LIB"/
    echo "Copied jars from $LIB_DIR to $TEST_WEBINF_LIB"
else
    echo "No jars found in $LIB_DIR to copy to test webapp."
fi

# -------------------------
# Étape 2.6 : Compiler les sources de test (test/src)
#   -> classes dans test/webapp/WEB-INF/classes
# -------------------------
echo
echo "=== Étape 2.6 : Compile test sources (test/src) using libs from $TEST_WEBINF_LIB ==="
TEST_JAVA_SOURCES=$(find "$TEST_SRC_DIR" -name "*.java" 2>/dev/null || true)
if [ -z "$TEST_JAVA_SOURCES" ]; then
    echo "No test Java sources found under $TEST_SRC_DIR (skipping test compilation)."
else
    # Construire classpath : toutes les jars dans test/webapp/WEB-INF/lib
    # (javac accepte le wildcard classpath)
    mkdir -p "$TEST_WEBINF_CLASSES"
    javac -cp "$LIB_DIR" -d "$TEST_WEBINF_CLASSES" $TEST_JAVA_SOURCES
    if [ $? -ne 0 ]; then
        echo "Compilation of test sources failed!"
        exit 1
    fi
    echo "Test sources compiled to $TEST_WEBINF_CLASSES"
fi

# -------------------------
# Étape 3 : Créer le WAR à partir du contenu de la zone de test (test/webapp)
# -------------------------
echo
echo "=== Étape 3 : Create WAR from test/webapp ==="
if [ ! -d "$TEST_WEBAPP_DIR" ]; then
    echo "Test webapp directory $TEST_WEBAPP_DIR does not exist. Nothing to pack."
    exit 1
fi

# On supprime l'ancien WAR s'il existe (optionnel)
if [ -f "$WAR_FILE" ]; then
    rm -f "$WAR_FILE"
fi

# Créer le WAR : aller dans test/webapp et empaqueter tout le contenu
pushd "$TEST_WEBAPP_DIR" >/dev/null || exit 1
jar -cvf "$WAR_FILE" .
WAR_RESULT=$?
popd >/dev/null

if [ $WAR_RESULT -ne 0 ]; then
    echo "Failed to create the .war file!"
    exit 1
fi
echo "WAR created at: $WAR_FILE"

# -------------------------
# Étape 4 : Déployer le WAR dans Tomcat
# -------------------------
echo
echo "=== Étape 4 : Deploy WAR to Tomcat ==="
cp "$WAR_FILE" "$TOMCAT_WEBAPPS_DIR/"
if [ $? -ne 0 ]; then
    echo "Deployment failed!"
    exit 1
fi
echo "Copied $WAR_FILE to $TOMCAT_WEBAPPS_DIR"

# -------------------------
# Étape 5 : Démarrer Tomcat si besoin
# -------------------------
echo
echo "=== Étape 5 : Ensure Tomcat running ==="
if pgrep -f "org.apache.catalina.startup.Bootstrap" > /dev/null; then
    echo "Tomcat is already running."
else
    echo "Starting Tomcat..."
    "$TOMCAT_BIN"
    sleep 5
fi

# -------------------------
# Fin
# -------------------------
echo
echo "Deployment successful!"
echo "➡ Application available at: http://localhost:8080/${PROJECT_NAME}/"
