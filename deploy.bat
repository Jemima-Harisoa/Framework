@echo off
REM -------------------------------
REM Variables de répertoire
REM -------------------------------
set SRC_DIR=%CD%
for %%a in (%SRC_DIR%) do set PROJECT_NAME=%%~nxa

set LIB_DIR=%SRC_DIR%\lib
set WEB_DIR=%SRC_DIR%\src\main\webapp
set CLASSES_DIR=%WEB_DIR%\WEB-INF\classes
set BUILD_DIR=%SRC_DIR%\build
set WAR_FILE=%SRC_DIR%\%PROJECT_NAME%.war
set TOMCAT_DIR=C:\Users\Admin\Downloads\apache-tomcat-10.1.39
set TOMCAT_WEBAPPS_DIR=%TOMCAT_DIR%\webapps
set TOMCAT_BIN=%TOMCAT_DIR%\bin\startup.bat

REM -------------------------------
REM Créer les dossiers si nécessaires
REM -------------------------------
if not exist "%BUILD_DIR%" (
    mkdir "%BUILD_DIR%"
)
if not exist "%CLASSES_DIR%" (
    mkdir "%CLASSES_DIR%"
)

REM -------------------------------
REM Étape 1 : Compiler tous les fichiers Java
REM -------------------------------
echo Compiling all Java files...
for /R "%SRC_DIR%\src\main\java" %%f in (*.java) do (
    javac -cp "%LIB_DIR%\servlet-api.jar" -d "%CLASSES_DIR%" "%%f"
    if ERRORLEVEL 1 (
        echo Compilation failed for %%f
        exit /b 1
    )
)

REM -------------------------------
REM Étape 2 : Copier les fichiers dans build
REM -------------------------------
echo Copying files to build directory...
xcopy /E /I "%WEB_DIR%" "%BUILD_DIR%" >nul
if ERRORLEVEL 1 (
    echo Copying files failed!
    exit /b 1
)

REM -------------------------------
REM Étape 3 : Créer le fichier .war
REM -------------------------------
echo Creating the .war file: %PROJECT_NAME%.war
cd /d "%BUILD_DIR%"
jar -cvf "%WAR_FILE%" *
if ERRORLEVEL 1 (
    echo Failed to create the .war file!
    exit /b 1
)

REM -------------------------------
REM Étape 4 : Déployer dans Tomcat
REM -------------------------------
echo Deploying the .war file to Tomcat...
copy "%WAR_FILE%" "%TOMCAT_WEBAPPS_DIR%"
if ERRORLEVEL 1 (
    echo Deployment failed!
    exit /b 1
)

REM -------------------------------
REM Étape 5 : Vérifier si Tomcat est actif
REM -------------------------------
tasklist /FI "IMAGENAME eq java.exe" | find /I "java.exe" >nul
if %ERRORLEVEL% neq 0 (
    echo Starting Tomcat...
    call "%TOMCAT_BIN%"
    timeout /t 5 >nul
) else (
    echo Tomcat is already running.
)

REM -------------------------------
REM Fin
REM -------------------------------
echo Deployment successful! (%PROJECT_NAME%.war deployed)
echo Application available at: http://localhost:8080/%PROJECT_NAME%/
pause
