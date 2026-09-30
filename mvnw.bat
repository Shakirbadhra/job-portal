@REM Maven Wrapper Script for JobConnect Portal
@echo off
setlocal
if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_211"
)
set "PATH=%JAVA_HOME%\bin;C:\Users\krish\.tools\apache-maven-3.8.8\bin;%PATH%"
cmd /c "C:\Users\krish\.tools\apache-maven-3.8.8\bin\mvn.cmd" %*
endlocal
