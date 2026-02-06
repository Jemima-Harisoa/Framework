@echo off
setlocal enabledelayedexpansion

REM === Configuration simple ===
set "ROOT=%cd%"
set "TARGET_DIR=%ROOT%\target"

REM Détection du projet ControlTower\controlTowerBackoffice
set "TEST_DIR=%~dp0..\ControlTower\controlTowerBackoffice"
if not exist "!TEST_DIR!" set "TEST_DIR=%~dp0..\ControlTower"
if not exist "!TEST_DIR!" set "TEST_DIR=%~dp0..\controlTowerBackoffice"

set "TEST_LIB=!TEST_DIR!\src\main\webapp\WEB-INF\lib"

echo Framework: !ROOT!
echo Test dir : !TEST_DIR!
echo.

REM === 1) Build Maven ===
echo 1) Compilation Maven...
call mvn -q clean package
if errorlevel 1 (
    echo ❌ Erreur Maven.
    pause
    exit /b 1
)
echo OK: compilation terminee.
echo.

REM === 2) Trouver le JAR le plus récent dans target\ ===
echo 2) Recherche du JAR dans !TARGET_DIR! ...
set "JAR="
for %%F in (!TARGET_DIR!\*.jar) do (
    set "JAR=%%F"
)

if "!JAR!"=="" (
    echo ❌ Aucun JAR trouve dans !TARGET_DIR!. Verifie ton pom.xml.
    pause
    exit /b 1
)

echo JAR trouve : !JAR!
echo.

REM === 3) Copier vers ControlTower\controlTowerBackoffice\src\main\webapp\WEB-INF\lib ===
echo 3) Copie vers !TEST_LIB! ...
if exist "!TEST_DIR!" (
    if not exist "!TEST_LIB!" mkdir "!TEST_LIB!"
    copy /Y "!JAR!" "!TEST_LIB!\"
    if errorlevel 1 (
        echo ❌ Erreur lors de la copie
        pause
        exit /b 1
    )
    echo ✅ Copie terminee.
) else (
    echo ⚠️ Dossier ControlTower\controlTowerBackoffice introuvable a l'emplacement attendu : !TEST_DIR!
    echo    Cree un projet ControlTower\controlTowerBackoffice/ au meme niveau que Framework/ puis relance.
    pause
    exit /b 1
)

echo.
echo Build + copie termines.
pause