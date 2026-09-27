@echo off
setlocal enabledelayedexpansion
set "ROOT_DIR=%~dp0"
if exist "%ROOT_DIR%.tools\apache-maven-3.9.6\bin\mvn.cmd" (
    "%ROOT_DIR%.tools\apache-maven-3.9.6\bin\mvn.cmd" %*
) else (
    mvn %*
)
