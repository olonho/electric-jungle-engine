@echo off
if not '%JAVA_HOME%'=='' set PATH=%JAVA_HOME%\bin;%PATH%

:args
if '%1'=='run' goto run
if '%1'=='clean' goto clean
if '%1'=='-h' goto help
if '%1'=='-?' goto help
if '%1'=='/?' goto help

:build
if not exist ..\dist mkdir ..\dist
if exist ..\dist\universum\Main.class del ..\dist\universum\Main.class
javac -source 1.5 -classpath ..\src -d ..\dist ..\src\universum\*.java ..\src\universum\beings\*.java
if not exist ..\dist\resources xcopy /e /q /i ..\resources\resources ..\dist\resources
if exist ..\dist\universum\Main.class goto run

:error
pause
exit

:help
echo Usage:
echo   make [run|clean|-h]
echo   run    launch the application without recompilation
echo   clean  remove compiled binaries
echo   -h     this help
exit

:clean
rd /s /q ..\dist
exit

:run
java -ea -classpath ..\dist universum.Main
