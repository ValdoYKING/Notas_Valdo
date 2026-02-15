@echo off
echo ====================================
echo   Compilando Notas Inteligentes
echo   Con funcionalidad de Boveda
echo ====================================
echo.

echo [1/3] Limpiando proyecto...
call gradlew clean

echo.
echo [2/3] Sincronizando dependencias...
call gradlew --refresh-dependencies

echo.
echo [3/3] Compilando aplicacion...
call gradlew assembleDebug

echo.
echo ====================================
echo   Compilacion completada
echo ====================================
echo.
echo El APK se encuentra en:
echo app\build\outputs\apk\debug\app-debug.apk
echo.

pause

