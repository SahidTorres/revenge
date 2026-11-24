#!/usr/bin/env bash
# Abre Alcalde Digital · La noche del Palomo (requiere Java 21 o posterior).
set -euo pipefail
cd -- "$(dirname -- "$0")"
if [[ -f distribucion/alcalde-digital.jar && -f distribucion/lib/core-4.5.6.jar ]]; then
  # Las imágenes y la música se leen de src/main/resources: no se duplican dentro del JAR.
  exec java -cp 'distribucion/alcalde-digital.jar:src/main/resources:distribucion/lib/*' alcaldedigital.app.ClienteApp
fi
printf '%s\n' 'Primero ejecuta: bash herramientas.sh paquete'
exit 1
