@echo off

@REM 获取脚本所在的目录
set SCRIPT_DIR=%~dp0
if "%SCRIPT_DIR%" == "" set SCRIPT_DIR=.\

@REM 查找 jTool.jar 的位置
set JAR_LOCATION=
for %%i in (%SCRIPT_DIR%jTool-*.jar) do (
    set "JAR_LOCATION=%%i"
    goto :foundJar
)
for %%i in (%SCRIPT_DIR%include\jTool-*.jar) do (
    set "JAR_LOCATION=%%i"
    goto :foundJar
)
for %%i in (%SCRIPT_DIR%jar\jTool-*.jar) do (
    set "JAR_LOCATION=%%i"
    goto :foundJar
)
for %%i in (%SCRIPT_DIR%java\jTool-*.jar) do (
    set "JAR_LOCATION=%%i"
    goto :foundJar
)
:foundJar

@REM 检查是否找到了 jTool.jar
if not defined JAR_LOCATION (
  echo Error: jTool-*.jar not found
  exit /b 1
)

@REM 执行 jTool.jar
java -jar "%JAR_LOCATION%" %*
