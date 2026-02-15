@echo off
echo ===============================================
echo   GENERADOR DE RELEASE - NOTAS INTELIGENTES
echo ===============================================
echo.

REM Verificar que existen las variables de entorno
if "%KEYSTORE_PASSWORD%"=="" (
    echo [ERROR] No se encontro la variable de entorno KEYSTORE_PASSWORD
    echo Por favor configura las variables de entorno antes de continuar.
    echo.
    echo Instrucciones en: RELEASE_INSTRUCTIONS.md
    echo.
    pause
    exit /b 1
)

if "%KEY_PASSWORD%"=="" (
    echo [ERROR] No se encontro la variable de entorno KEY_PASSWORD
    echo Por favor configura las variables de entorno antes de continuar.
    echo.
    echo Instrucciones en: RELEASE_INSTRUCTIONS.md
    echo.
    pause
    exit /b 1
)

echo [OK] Variables de entorno configuradas
echo.

REM Verificar que existe el keystore
if not exist "notas-inteligentes-release.jks" (
    echo [ERROR] No se encontro el archivo keystore: notas-inteligentes-release.jks
    echo Por favor crea el keystore primero.
    echo.
    echo Instrucciones en: RELEASE_INSTRUCTIONS.md
    echo.
    pause
    exit /b 1
)

echo [OK] Keystore encontrado
echo.

echo ===============================================
echo   PASO 1: Limpiando proyecto
echo ===============================================
echo.
call gradlew clean
if errorlevel 1 (
    echo.
    echo [ERROR] Fallo al limpiar el proyecto
    pause
    exit /b 1
)

echo.
echo ===============================================
echo   PASO 2: Generando App Bundle (AAB)
echo ===============================================
echo.
call gradlew bundleRelease
if errorlevel 1 (
    echo.
    echo [ERROR] Fallo al generar el App Bundle
    pause
    exit /b 1
)

echo.
echo ===============================================
echo   PASO 3: Generando APK
echo ===============================================
echo.
call gradlew assembleRelease
if errorlevel 1 (
    echo.
    echo [ERROR] Fallo al generar el APK
    pause
    exit /b 1
)

echo.
echo ===============================================
echo   GENERACION COMPLETADA EXITOSAMENTE
echo ===============================================
echo.
echo Archivos generados:
echo.
echo [APP BUNDLE para Play Store]
echo   app\build\outputs\bundle\release\app-release.aab
echo.
echo [APK para instalacion directa]
echo   app\build\outputs\apk\release\app-release.apk
echo.

REM Mostrar tamaño de archivos
echo Tamano de archivos:
for %%F in (app\build\outputs\bundle\release\app-release.aab) do (
    echo   AAB: %%~zF bytes
)
for %%F in (app\build\outputs\apk\release\app-release.apk) do (
    echo   APK: %%~zF bytes
)
echo.

echo ===============================================
echo   SIGUIENTES PASOS
echo ===============================================
echo.
echo 1. Probar el APK en un dispositivo:
echo    adb install app\build\outputs\apk\release\app-release.apk
echo.
echo 2. Subir el AAB a Google Play Console:
echo    https://play.google.com/console
echo.
echo 3. Completar informacion requerida en Play Console
echo.
echo Consulta PLAY_STORE_CHECKLIST.md para mas detalles
echo.

pause

