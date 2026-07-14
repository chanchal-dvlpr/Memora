@echo off
setlocal

REM Launches the packaged Context Engine daemon on Windows.
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "DIST_DIR=%%~fI"
set "CONFIG_DIR=%CONTEXT_ENGINE_CONFIG_DIR%"
if "%CONFIG_DIR%"=="" set "CONFIG_DIR=%DIST_DIR%\config"
set "JAR_PATH=%DIST_DIR%\context-engine-backend.jar"

set "JAVA_COMMAND=java"
if not "%JAVA_HOME%"=="" if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_COMMAND=%JAVA_HOME%\bin\java.exe"

"%JAVA_COMMAND%" -version >nul 2>&1
if errorlevel 1 (
    echo Context Engine requires Java 21. Set JAVA_HOME or add Java 21 to PATH. 1>&2
    exit /b 1
)

"%JAVA_COMMAND%" -version 2>&1 | findstr /R /C:"version \"21\." >nul
if errorlevel 1 (
    echo Context Engine requires Java 21. 1>&2
    "%JAVA_COMMAND%" -version 1>&2
    exit /b 1
)

if not exist "%JAR_PATH%" (
    echo Context Engine JAR was not found: %JAR_PATH% 1>&2
    exit /b 1
)

REM Run Java directly so Ctrl+C is delivered to Spring Boot for graceful shutdown.
"%JAVA_COMMAND%" -jar "%JAR_PATH%" "--spring.config.additional-location=optional:file:%CONFIG_DIR%/" %*
exit /b %ERRORLEVEL%
