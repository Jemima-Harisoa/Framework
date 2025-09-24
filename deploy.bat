@echo off
setlocal

REM -------------------------
REM Déclarations (adaptées Windows)
REM -------------------------
set "SRC_DIR=%CD%"
for %%a in ("%SRC_DIR%") do set "PROJECT_NAME=%%~nxa"

set "LIB_DIR=%SRC_DIR%\lib"
set "WEB_DIR=%SRC_DIR%\src\main\webapp"
set "CLASSES_DIR=%WEB_DIR%\WEB-INF\classes"
set "BUILD_DIR=%SRC_DIR%\build"
set "WAR_FILE=%SRC_DIR%\%PROJECT_NAME%.war"

REM Ajuste ce chemin selon ton installation Tomcat Windows
set "TOMCAT_DIR=C:\opt\tomcat"
set "TOMCAT_WEBAPPS_DIR=%TOMCAT_DIR%\webapps"
set "TOMCAT_BIN=%TOMCAT_DIR%\bin\startup.bat"

REM Dossier test (espace de travail des devs)
set "TEST_DIR=%SRC_DIR%\test"
set "TEST_WEBAPP_DIR=%TEST_DIR%\webapp"
set "TEST_SRC_DIR=%TEST_DIR%\src"
set "TEST_WEBINF_LIB=%TEST_WEBAPP_DIR%\WEB-INF\lib"
set "TEST_WEBINF_CLASSES=%TEST_WEBAPP_DIR%\WEB-INF\classes"

REM -------------------------
REM Préparations
REM -------------------------
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%CLASSES_DIR%" mkdir "%CLASSES_DIR%"
if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"

echo Project: %PROJECT_NAME%
echo SRC_DIR: %SRC_DIR%
echo LIB_DIR: %LIB_DIR%
echo TEST_DIR: %TEST_DIR%
echo.

REM -------------------------
REM Étape 1 : Compiler la partie framework (src\main\java)
REM -------------------------
echo === Étape 1 : Compile framework sources (src\main\java) ===

dir /b /s "%SRC_DIR%\src\main\java\*.java" >nul 2>&1
if errorlevel 1 (
    echo No framework Java sources found under src\main\java (skipping framework compilation).
) else (
    for /R "%SRC_DIR%\src\main\java" %%f in (*.java) do (
        echo Compiling %%f
        javac -cp "%LIB_DIR%\servlet-api.jar" -d "%CLASSES_DIR%" "%%f"
        if errorlevel 1 (
            echo Compilation of framework source failed for %%f
            exit /b 1
        )
    )
    echo Framework sources compiled to %CLASSES_DIR%
)
echo.

REM -------------------------
REM Étape 2 : Créer le JAR des servlets (framework) dans LIB_DIR
REM -------------------------
echo === Étape 2 : Build framework JAR into %LIB_DIR% ===
set "JAR_NAME=%PROJECT_NAME%-servlets.jar"
set "JAR_PATH=%LIB_DIR%\%JAR_NAME%"

if exist "%CLASSES_DIR%\com\" (
    jar -cvf "%JAR_PATH%" -C "%CLASSES_DIR%" com
    if errorlevel 1 (
        echo Failed to create servlet JAR at %JAR_PATH%
        exit /b 1
    )
    echo JAR created at: %JAR_PATH%
) else (
    echo No "com" classes found in %CLASSES_DIR% - skipping jar creation.
)
echo.

REM -------------------------
REM Étape 2.5 : Préparer la zone de test (test\webapp) et copier libs
REM -------------------------
echo === Étape 2.5 : Prepare test webapp and copy libs ===
if not exist "%TEST_WEBAPP_DIR%" mkdir "%TEST_WEBAPP_DIR%"
if not exist "%TEST_WEBINF_LIB%" mkdir "%TEST_WEBINF_LIB%"
if not exist "%TEST_WEBINF_CLASSES%" mkdir "%TEST_WEBINF_CLASSES%"

REM Optionnel : si tu veux copier la webapp du framework comme base, décommente
REM if exist "%WEB_DIR%" xcopy /E /I /Y "%WEB_DIR%\*" "%TEST_WEBAPP_DIR%\"

REM Copier tous les jars de LIB_DIR vers test\webapp\WEB-INF\lib (si présents)
if exist "%LIB_DIR%\*.jar" (
    echo Copying jars from %LIB_DIR% to %TEST_WEBINF_LIB%...
    copy /Y "%LIB_DIR%\*.jar" "%TEST_WEBINF_LIB%\" >nul
    if errorlevel 1 (
        echo Failed copying jars to %TEST_WEBINF_LIB%
        exit /b 1
    )
    echo Copied jars to %TEST_WEBINF_LIB%
) else (
    echo No jars found in %LIB_DIR% to copy to test webapp.
)
echo.

REM -------------------------
REM Étape 2.6 : Compiler les sources de test (test\src)
REM -------------------------
echo === Étape 2.6 : Compile test sources (test\src) using libs from %TEST_WEBINF_LIB% ===
dir /b /s "%TEST_SRC_DIR%\*.java" >nul 2>&1
if errorlevel 1 (
    echo No test Java sources found under %TEST_SRC_DIR% (skipping test compilation).
) else (
    REM Construire classpath : utiliser les jars dans TEST_WEBINF_LIB
    if exist "%TEST_WEBINF_LIB%\*" (
        set "TEST_CLASSPATH=%TEST_WEBINF_LIB%\*"
    ) else (
        set "TEST_CLASSPATH=%LIB_DIR%\*"
    )
    if not exist "%TEST_WEBINF_CLASSES%" mkdir "%TEST_WEBINF_CLASSES%"

    for /R "%TEST_SRC_DIR%" %%f in (*.java) do (
        echo Compiling test %%f
        javac -cp "%TEST_CLASSPATH%" -d "%TEST_WEBINF_CLASSES%" "%%f"
        if errorlevel 1 (
            echo Test compilation failed for %%f
            exit /b 1
        )
    )
    echo Test sources compiled to %TEST_WEBINF_CLASSES%
)
echo.

REM -------------------------
REM Étape 3 : Créer le WAR à partir de test\webapp
REM -------------------------
echo === Étape 3 : Create WAR from test\webapp ===
if not exist "%TEST_WEBAPP_DIR%" (
    echo Test webapp directory %TEST_WEBAPP_DIR% does not exist. Nothing to pack.
    exit /b 1
)

if exist "%WAR_FILE%" del /F /Q "%WAR_FILE%"

pushd "%TEST_WEBAPP_DIR%" >nul
jar -cvf "%WAR_FILE%" .
set "WAR_RESULT=%ERRORLEVEL%"
popd >nul

if "%WAR_RESULT%"=="0" (
    echo WAR created at: %WAR_FILE%
) else (
    echo Failed to create the .war file!
    exit /b 1
)
echo.

REM -------------------------
REM Étape 4 : Déployer le WAR dans Tomcat
REM -------------------------
echo === Étape 4 : Deploy WAR to Tomcat ===
if not exist "%TOMCAT_WEBAPPS_DIR%" (
    echo Tomcat webapps dir %TOMCAT_WEBAPPS_DIR% does not exist. Check TOMCAT_DIR.
    REM continue but warn
) 

copy /Y "%WAR_FILE%" "%TOMCAT_WEBAPPS_DIR%\" >nul
if errorlevel 1 (
    echo Deployment failed copying WAR to %TOMCAT_WEBAPPS_DIR% (permission?)
    exit /b 1
)
echo Copied %WAR_FILE% to %TOMCAT_WEBAPPS_DIR%
echo.

REM -------------------------
REM Étape 5 : Démarrer Tomcat si besoin
REM -------------------------
echo === Étape 5 : Ensure Tomcat running ===
tasklist /FI "IMAGENAME eq java.exe" | find /I "java.exe" >nul
if errorlevel 1 (
    echo Starting Tomcat...
    if exist "%TOMCAT_BIN%" (
        call "%TOMCAT_BIN%"
        timeout /t 5 >nul
    ) else (
        echo Tomcat startup script not found at %TOMCAT_BIN%. Start Tomcat manually.
    )
) else (
    echo Tomcat is already running.
)
echo.

REM -------------------------
REM Fin
REM -------------------------
echo Deployment successful!
echo ^> Application available at: http://localhost:8080/%PROJECT_NAME%/
endlocal
pause
