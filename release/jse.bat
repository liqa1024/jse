@echo off

@REM 获取脚本所在的目录
set SCRIPT_DIR=%~dp0
if "%SCRIPT_DIR%" == "" set SCRIPT_DIR=.\

@REM 查找 jse.jar 的位置
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

@REM 检查是否找到了 jse.jar
if not defined JAR_LOCATION (
  echo Error: jse-*.jar not found
  exit /b 1
)

@REM 设置 UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8

@REM 消除每 30s 一次的 prefs 的警告
set JAVA_OPTS=%JAVA_OPTS% -Djava.util.prefs.syncInterval=1000000

@REM GROOVY-6453: groovysh in Windows 7/8/10 doesn't support arrow keys and Del
set JAVA_OPTS=%JAVA_OPTS% -Djline.terminal=none

@REM 执行 jse.jar
java %JAVA_OPTS% -jar "%JAR_LOCATION%" %*
