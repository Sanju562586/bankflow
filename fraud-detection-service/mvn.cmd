@echo off
setlocal enabledelayedexpansion
set "SCRIPT_DIR=%~dp0"
set "MVN_BIN=%SCRIPT_DIR%..\.tools\apache-maven-3.9.6\bin\mvn.cmd"
if exist "%MVN_BIN%" (
    "%MVN_BIN%" %*
) else (
    mvn %*
)
