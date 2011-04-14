@echo off
set CLASSPATH=%JLITE_HOME%;%JLITE_HOME%\bin
FOR %%f IN (%JLITE_HOME%\lib\*.jar) DO (call :append_classpath %%f)
FOR %%f IN (%JLITE_HOME%\lib\glite\*.jar) DO (call :append_classpath %%f)
FOR %%f IN (%JLITE_HOME%\lib\external\*.jar) DO (call :append_classpath %%f)

:append_classpath
set CLASSPATH=%CLASSPATH%;%1