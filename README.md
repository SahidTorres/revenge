# Alcalde Digital · La noche del Palomo

Videojuego educativo 2D en Java + Processing para el laboratorio de Estructura de Datos II. Une la **misión 5** («La calle del pánico»), sus **cuatro minijuegos** de estructuras de datos y la **misión 6** («Cielos de campaña») en un solo **modo historia** de seis capítulos, para uno o dos jugadores en el mismo teclado.

> **Guía completa del juego y de la sustentación:** [entregas/historia/README.md](entregas/historia/README.md) (historia, cada misión y minijuego, estructuras, controles, arquitectura, pruebas y preguntas típicas).

## Jugar

Requiere **Java 21 o posterior**. El paquete ya viene compilado:

- Windows: doble clic en `jugar.bat`
- Linux / macOS: `bash jugar.sh`

En el menú, **«Jugar la historia»** → «Nueva historia · 1 jugador» o «· 2 jugadores». Cada capítulo ganado abre el siguiente; el progreso se guarda solo.

| # | Capítulo | Estructura de datos |
|---|---|---|
| 1 | Misión 5 · La calle del pánico | AVL de prioridad |
| 2 | Corta la cadena | Grafo + BFS |
| 3 | Rastrea la fuente | Árbol n-ario con padre |
| 4 | Caza de bots | Tabla hash con encadenamiento |
| 5 | Duelo contra el Palomo (jefe) | AVL + búsqueda del piso |
| 6 | Misión 6 · Cielos de campaña | Máquina de estados + autoguardado |

El panel **Juego libre** del menú permite jugar cada parte por separado (la misión 6 suelta se desbloquea al ganar la misión 5). El **caso extra** «El helipuerto de palomas» es el capítulo de la entrega 1, con su laboratorio de árboles AVL.

## Controles rápidos

- **Un jugador:** WASD o flechas + E / Espacio / Enter. Misión 5: clic o 1–4 para verificar. Misión 6: F disparo, Q misil, Espacio impulso.
- **Dos jugadores:** J1 WASD + E (misión 6: F, Q, Espacio) · J2 flechas + Enter (misión 6: L, K, Mayús).
- **Esc** pausa o vuelve. La ayuda («Cómo jugar») resume todo. El sonido se activa o silencia en el menú.

## Construir y probar

```bash
bash herramientas.sh test       # 138 pruebas automáticas
bash herramientas.sh paquete    # vuelve a generar distribucion/
bash herramientas.sh evidencias # capturas de la entrega 1, misión 5 y minijuegos
```

Con Maven directamente: `mvn test` y `mvn exec:java -Dexec.mainClass=alcaldedigital.app.ClienteApp`. Desde VS Code: ejecutar `ClienteApp`.

Requisitos para compilar: JDK 21+, Maven 3.9+ y acceso inicial a Maven Central (Processing Core 4.5.6 y JUnit 5). `herramientas.sh` busca Maven también dentro de la extensión Java de VS Code.

`distribucion/alcalde-digital.jar` contiene solo las clases; `jugar.sh` y `jugar.bat` agregan `src/main/resources` (imágenes, música y fuente) al classpath, así el arte no se duplica.

## Estructura

```
src/main/java/alcaldedigital/
  app/                     ClienteApp (punto de entrada), EvidenciasApp, autoprueba
  cliente/                 JuegoCliente (navegación) y ClienteSketch (ventana Processing)
    campana/               Campana: modo historia, capítulos, relatos y progreso
    audio/                 MotorAudio, MezclaMusical, DirectorMusical, OpcionesAudio
    interfaz/              RenderJuego, RenderMinijuegos, RenderHistoria, TemaPixel
    mision6/               Motor, dibujo, autoguardado y sonido de la misión 6
    presentacion/          Sprites de Juan y salida de audio por escena
  servidor/misiones/       Mision5 (lógica sin gráficos)
  servidor/minijuegos/     Minijuego base y los cuatro minijuegos
  compartido/estructuras/  AVL, árbol n-ario, grafo, tabla hash, árbol de decisiones
src/test/java/             138 pruebas JUnit 5
entregas/historia/         Guía del juego y de la sustentación (con capturas)
entregas/mision5/          Misión 5
entregas/minijuegos/       Minijuegos
entregas/mision6/          Misión 6 y su arte
entregas/entrega1/         Informe de la entrega 1 (caso extra)
```

## Guardados

Todo se guarda en `~/.local/state/alcalde-digital/` (o en la carpeta de la propiedad `ad.guardados`): `campana.properties` (historia), `mision6.properties` + `.bak` (partida de la misión 6) y `audio.properties` (sonido). Las pruebas usan una carpeta temporal en `target/`.

## Reglas de trabajo

- La rama principal debe permanecer ejecutable.
- La lógica (misiones, minijuegos, historia) es la autoridad; la interfaz solo envía la entrada y dibuja.
- Las estructuras resuelven mecánicas reales y no se agregan solo para mostrarlas.
- No se suben archivos generados por Maven, configuraciones personales ni secretos.
