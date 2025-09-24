@echo off
:: Définir les variables de répertoire
set SRC_DIR=C:\Users\Admin\Documents\listenerServlet
set LIB_DIR=%SRC_DIR%\lib
set WEB_DIR=%SRC_DIR%\src\main\webapp
set CLASSES_DIR=%WEB_DIR%\WEB-INF\classes
set BUILD_DIR=%SRC_DIR%\build
set WAR_FILE=%SRC_DIR%\ExempleListener.war
set TOMCAT_WEBAPPS_DIR=C:\Users\Admin\Downloads\apache-tomcat-10.1.39\webapps

:: Créer le répertoire BUILD si nécessaire
if not exist "%BUILD_DIR%" (
    mkdir "%BUILD_DIR%"
)

echo Compiling ExempleListener.java...
javac -cp "%LIB_DIR%\servlet-api.jar" -d "%CLASSES_DIR%" "%SRC_DIR%\src\main\java\com\itu\hello\ExempleListener.java"
if %ERRORLEVEL% neq 0 (
    echo Compilation of ExempleListener.java failed!
    exit /b %ERRORLEVEL%
)



:: Etape 1 : Compiler les fichiers Java



:: Etape 2 : Copier les fichiers dans le répertoire de build
echo Copying files to build directory...
xcopy /E /I "%WEB_DIR%" "%BUILD_DIR%"
if %ERRORLEVEL% neq 0 (
    echo Copying files failed!
    exit /b %ERRORLEVEL%
)

:: Etape 3 : Créer le fichier .war
echo Creating the .war file...
cd "%BUILD_DIR%"
jar -cvf "%WAR_FILE%" *
if %ERRORLEVEL% neq 0 (
    echo Failed to create the .war file!
    exit /b %ERRORLEVEL%
)

:: Etape 4 : Déployer le fichier .war dans Tomcat
echo Deploying the .war file to Tomcat...
copy "%WAR_FILE%" "%TOMCAT_WEBAPPS_DIR%"
if %ERRORLEVEL% neq 0 (
    echo Deployment failed!
    exit /b %ERRORLEVEL%
)

:: Fin
echo Deployment successful!
pause
