#!/bin/bash

# Définir les variables de répertoire
SRC_DIR="$(pwd)"                      # répertoire courant
PROJECT_NAME="$(basename "$SRC_DIR")" # nom du répertoire courant
LIB_DIR="$SRC_DIR/lib"
WEB_DIR="$SRC_DIR/src/main/webapp"
CLASSES_DIR="$WEB_DIR/WEB-INF/classes"
BUILD_DIR="$SRC_DIR/build"
WAR_FILE="$SRC_DIR/${PROJECT_NAME}.war"
TOMCAT_DIR="/opt/tomcat"
TOMCAT_WEBAPPS_DIR="$TOMCAT_DIR/webapps"
TOMCAT_BIN="$TOMCAT_DIR/bin/startup.sh"

# Créer le répertoire BUILD si nécessaire
if [ ! -d "$BUILD_DIR" ]; then
    mkdir -p "$BUILD_DIR"
fi

# Créer le répertoire des classes si nécessaire
if [ ! -d "$CLASSES_DIR" ]; then
    mkdir -p "$CLASSES_DIR"
fi

# Étape 1 : Compiler tous les fichiers Java
echo "Compiling all Java files..."
javac -cp "$LIB_DIR/servlet-api.jar" -d "$CLASSES_DIR" $(find "$SRC_DIR/src/main/java" -name "*.java")
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Étape 2 : Copier les fichiers dans le répertoire de build
echo "Copying files to build directory..."
cp -r "$WEB_DIR/"* "$BUILD_DIR/"
if [ $? -ne 0 ]; then
    echo "Copying files failed!"
    exit 1
fi

# Étape 3 : Créer le fichier .war
echo "Creating the .war file: ${PROJECT_NAME}.war"
cd "$BUILD_DIR" || exit 1
jar -cvf "$WAR_FILE" *
if [ $? -ne 0 ]; then
    echo "Failed to create the .war file!"
    exit 1
fi

# Étape 4 : Déployer le fichier .war dans Tomcat
echo "Deploying the .war file to Tomcat..."
cp "$WAR_FILE" "$TOMCAT_WEBAPPS_DIR/"
if [ $? -ne 0 ]; then
    echo "Deployment failed!"
    exit 1
fi

# Étape 5 : Vérifier si Tomcat est actif, sinon le démarrer
if pgrep -f "org.apache.catalina.startup.Bootstrap" > /dev/null; then
    echo "Tomcat is already running."
else
    echo "Starting Tomcat..."
    "$TOMCAT_BIN"
    sleep 5  # attendre un peu que Tomcat démarre
fi

# Fin + lien de déploiement
echo "Deployment successful!"
echo "➡ Application available at: http://localhost:8080/${PROJECT_NAME}/"
