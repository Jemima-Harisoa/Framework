@echo off
setlocal enabledelayedexpansion

REM === Configuration simple ===
set "ROOT=%cd%"
set "TARGET_DIR=%ROOT%\target"

REM Détection du projet Test/ ou test/
set "TEST_DIR=%~dp0"
if not exist "!TEST_DIR!" set "TEST_DIR=%~dp0"

set "TEST_LIB=!TEST_DIR!\src\main\webapp\WEB-INF\lib"

echo Framework: !ROOT!
echo Test dir : !TEST_DIR!
echo.

REM === 1) Build Maven ===
echo 1) Compilation Maven...
mvn -q clean package
if errorlevel 1 (
    echo ❌ Erreur Maven.
    exit /b 1
)
echo OK: compilation terminée.
echo.

REM === 2) Trouver le JAR le plus récent dans target/ ===
echo 2) Recherche du JAR dans !TARGET_DIR! ...
set "JAR="
for %%F in (!TARGET_DIR!\*.jar) do (
    set "JAR=%%F"
)

if "!JAR!"=="" (
    echo ❌ Aucun JAR trouvé dans !TARGET_DIR!. Vérifie ton pom.xml.
    exit /b 1
)

echo JAR trouvé : !JAR!
echo.

REM === 3) Copier vers Test/src/main/webapp/WEB-INF/lib ===
echo 3) Copie vers !TEST_LIB! ...
if exist "!TEST_DIR!" (
    if not exist "!TEST_LIB!" mkdir "!TEST_LIB!"
    copy /Y "!JAR!" "!TEST_LIB!\"
    echo ✅ Copie terminée.
) else (
    echo ⚠️ Dossier Test introuvable à l'emplacement attendu : !TEST_DIR!
    echo Crée un projet Test/ ou test/ au même niveau que Framework/ puis relance.
    exit /b 1
)

echo.
echo Build + copie terminés.
pause
