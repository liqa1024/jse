@echo off

@REM Get the directory where the script is located
set SCRIPT_DIR=%~dp0
if "%SCRIPT_DIR%" == "" set SCRIPT_DIR=.\

@REM Find jse.jar
set JAR_LOCATION=
for %%i in (%SCRIPT_DIR%jse-*.jar) do (
    set "JAR_LOCATION=%%i"
    goto :foundJar
)
for %%i in (%SCRIPT_DIR%lib\jse-*.jar) do (
    set "JAR_LOCATION=%%i"
    goto :foundJar
)
:foundJar

@REM Check if jse.jar has been found
if not defined JAR_LOCATION (
  echo Error: jse-*.jar not found
  exit /b 1
)


@REM Eliminate the every 30s prefs warning
set JAVA_OPTS=%JAVA_OPTS% -Djava.util.prefs.syncInterval=1000000

@REM GROOVY-6453: groovysh in Windows 7/8/10 doesn't support arrow keys and Del
set JAVA_OPTS=%JAVA_OPTS% -Djline.terminal=none

@REM Execute jse.jar
java %JAVA_OPTS% -jar "%JAR_LOCATION%" CMD %*
