@echo off
setlocal enabledelayedexpansion

REM === Configuration simple ===
set "ROOT=%cd%"
set "TARGET_DIR=%ROOT%\target"

REM Détection du projet Test-Framework
set "TEST_DIR=%~dp0..\Test-Framework"
if not exist "!TEST_DIR!" set "TEST_DIR=%~dp0..\TEST-FRAMEWORK"
if not exist "!TEST_DIR!" set "TEST_DIR=%~dp0..\test-framework"

set "TEST_LIB=!TEST_DIR!\lib"

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

REM === 3) Copier vers Test-Framework\src\webapp\WEB-INF\lib ===
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
    echo ⚠️ Dossier Test-Framework introuvable a l'emplacement attendu : !TEST_DIR!
    echo    Cree un projet Test-Framework/ au meme niveau que Framework/ puis relance.
    pause
    exit /b 1
)

echo.
echo Build + copie termines.
pause