@echo off
setlocal
for %%? in ("%~dp0..") do set JLITE_HOME=%%~f?
call "%JLITE_HOME%\cli\set-classpath.bat"

java -cp %CLASSPATH% jlite.cli.JobOutput %*