@echo off
rem Abre Alcalde Digital - La noche del Palomo (requiere Java 21 o posterior).
cd /d "%~dp0"
java -cp "distribucion/alcalde-digital.jar;src/main/resources;distribucion/lib/*" alcaldedigital.app.ClienteApp
if errorlevel 1 pause
