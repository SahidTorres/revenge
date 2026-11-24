# Misión 6 · «Cielos de campaña»

Capítulo 6 y final del modo historia. También se juega suelta desde el menú (**Juego libre → Misión 6**), que se desbloquea al ganar la misión 5.

## Qué ocurre

La Influencer recibe el aviso de la Periodista: el Palomo mandará su **Dron Viral** contra Juan, el candidato. En la plaza escucha el discurso de Juan, se acerca a él y lo acompaña al helicóptero. En el aire practican las maniobras y combaten al dron durante **tres fases** (los misiles se desbloquean en la segunda). Tras la autodestrucción del dron y la transmisión del Palomo, aterrizan en la casa de campaña, exploran la sala y se sientan en el sofá para la **entrevista de tres preguntas**. Sus dos cierres, «Escuchar y adaptar» y «Convicción y diálogo», son válidos y cambian el epílogo.

Fases del motor: `DISCURSO → BUSCAR → ENCUENTRO → CAMINAR → MODO → DESPEGUE → AVISO → ENTRENAMIENTO → COMBATE → RETIRADA → LLEGADA → EXPLORAR_SALA → SALA → ENTREVISTA → RESPUESTA → FINAL` (o `DERROTA`, que vuelve a la práctica). En el modo historia la fase `MODO` se salta sola con el número de jugadores elegido al empezar.

## Controles

| Acción | Un jugador | Dos jugadores |
|---|---|---|
| Mover | WASD / flechas | J1 WASD · J2 flechas |
| Disparar (mantener) | F (también J o Z) | J1 F · J2 L |
| Misil (fase 2 y 3) | Q (también K o X) | J1 Q · J2 K o M |
| Impulso | Espacio (también Mayús) | J1 Espacio · J2 Mayús (o / o 0 numérico) |
| Hablar / confirmar | Enter, E o Espacio | Igual |
| Entrevista | Flechas + Enter, o 1 / 2 | Igual |
| Pausa | Esc o P | Igual |

En derrota, R o Enter vuelve a la práctica. La pausa congela la simulación, muestra los controles y permite «Guardar y salir». Perder el foco de la ventana también pausa.

## Reglas del combate

- Dron: 180 de energía. Fase 1 (más de 120): ráfagas de 3. Fase 2 (61–120): turbulencia, señuelos y misil desbloqueado. Fase 3 (60 o menos): ráfagas de 7 y halo que daña al tocarlo.
- Cada helicóptero: 5 corazones. Disparo normal: 5 de daño en un jugador, 3 en pareja. Misil: 18 de daño, recarga de 4 s.
- Corazones de curación: aparecen cada 6 s (máximo 3), curan 1 al que los toca y no reviven naves.
- Impulso: doble velocidad 0,28 s e invulnerabilidad breve; recarga de 2 s.
- Puntaje en la historia: 250 por completarla + 30 por cada corazón que queda.

## Guardado

Autoguardado cada dos segundos, al cambiar de escena, al pausar y al salir: `~/.local/state/alcalde-digital/mision6.properties`, con copia anterior `.bak`. La escritura es asíncrona y atómica (archivo temporal + reemplazo). `ad.guardados` cambia la carpeta y `ad.mision6.guardar` permite usar otro archivo. Se recuperan posiciones, modo, proyectiles, vida, recargas, corazones y entrevista; las teclas mantenidas nunca se guardan. Se leen las versiones 1 y 2 del formato. Empezar una historia nueva borra la partida guardada de la misión 6 anterior.

## Código

- `cliente/mision6/MisionSeisMotor.java`: lógica pura, sin Processing.
- `cliente/mision6/MisionSeisSketch.java`: dibujo sobre un lienzo del mismo PApplet a 1280 × 720, escalando las coordenadas de 1024 × 576.
- `cliente/mision6/AutoguardadoSeis.java`: guardado asíncrono y atómico con copia `.bak`.
- `cliente/mision6/AudioMisionSeis.java`: observa el motor y dispara sonidos; nunca modifica la simulación.
- Integración: `JuegoCliente` (pantallas `MISION6` y `MISION6_PAUSA`), `ClienteSketch` (dibujo y teclas) y `RenderHistoria` (pausa).
- Pruebas: `MisionSeisTest`, `MisionSeisEntregaTest`, `AudioMisionSeisTest`, `MisionSeisSalaCorazonesTest` (23 pruebas).

Los recursos gráficos se describen en [ARTE_MISION6.md](ARTE_MISION6.md). Imágenes en `src/main/resources/cliente/mision6/`; Juan en `presentacion/`; la Influencer y el Palomo en `mision3/redisenio/`; la plaza y los vecinos en `mision4/`.
