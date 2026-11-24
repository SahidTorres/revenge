# Misión 5 · «La calle del pánico»

Misión de 70 segundos para **uno o dos jugadores** (Ciudadano y Periodista), integrada al menú del juego (botón **Misión 5 · La calle del pánico**, a la derecha del menú principal). Usa los mismos personajes, tiles, objetos, iconos, efectos, fuente y marcos del capítulo 1.

## Qué ocurre

1. **Selector de modo** (un jugador / dos jugadores) y **historia** en cuatro páginas con los textos del guion: el encuentro frente al Centro de Periodismo y la burla del Palomo.
2. **Cuenta atrás de 3 s** y 70 s de misión:
   - **Periodista**: verifica y desmiente las noticias falsas que llegan a Civitas. Las cuatro más urgentes aparecen ordenadas por prioridad; verificar toma 1,1 s. Si la noticia resulta verdadera, no se desmiente (+2 puntos, pero se pierde tiempo).
   - **Ciudadano**: se acerca a los vecinos asustados y los calma con un mensaje de civismo (1,5 s de conversación sin alejarse).
   - **El Palomo** interrumpe cada pocos segundos con una **burla** (asusta a un vecino), un **chisme** (inyecta una noticia falsa) o un **sabotaje** (apaga las lámparas y crea una zona oscura que dispara el pánico).
3. **Victoria**: se alcanzan ambas metas sin que el pánico ni la desinformación lleguen a 100. El Palomo huye, deja caer una pista (USB) y dice «Esto no se queda así».
4. **Derrota**: pánico o desinformación en 100, o fin del tiempo sin completar ambas tareas → mensaje rojo «La ciudad cayó en la mentira. Perdiste.»

| | Un jugador | Dos jugadores |
|---|---|---|
| Metas | 8 noticias · 7 vecinos | 11 noticias · 10 vecinos |
| Vecinos / noticias | 6 / cada ~3,0 s | 7 / cada ~2,6 s |

## Controles

- **Ciudadano**: WASD (en un jugador también flechas) · **E** o **Espacio**: calmar al vecino cercano.
- **Periodista**: clic sobre una noticia, teclas **1–4**, o (dos jugadores) **↑ ↓** para elegir y **Enter** para verificar. En un jugador, Enter verifica la más urgente.
- **Esc**: pausa y vuelve al menú (el menú ofrece «Continuar misión 5»). Contraste alto y movimiento reducido funcionan igual que en el capítulo 1.

## Estructura de datos

Las noticias entran al mismo **AVL de publicaciones** del capítulo 1 (`ArbolAVLPublicaciones`, clave `(riesgo, ID)`): se insertan al llegar, se **reinsertan** cuando una noticia falsa se viraliza (+25 de riesgo), se **eliminan** al verificarse o esparcirse, y el panel del Periodista usa el recorrido **inverso** (mayor prioridad primero). La ventana muestra 4; el resto queda «en cola».

## Código

- `servidor/misiones/Mision5.java`: toda la lógica (sin gráficos). Parámetros de dificultad al inicio del constructor y constantes `TIEMPO_HABLA`, `ALCANCE`, `VEL_ALTERADO`.
- `cliente/JuegoCliente.java`: pantallas `MISION5_MODO`, `MISION5_INTRO`, `MISION5`, `MISION5_FIN`, controles y navegación.
- `cliente/interfaz/RenderJuego.java`: sección «Misión 5» al final del archivo.
- Pruebas: `reglas/Mision5Test.java` y `integracion/Mision5InterfazTest.java`.
- Evidencias reproducibles: `bash herramientas.sh evidencias` (genera también `entregas/mision5/evidencias`).

## Balance

La dificultad se calibró con jugadores automáticos de distinta rapidez de reacción: un jugador inactivo siempre pierde (por pánico) y uno atento gana casi siempre; para ajustar, cambia las metas, `subidaPanico`, `intervaloNoticias` y `vidaNoticia`.

## Texto añadido por el equipo de desarrollo

El guion no define la «pista clave». El texto actual («Civitas ordena lo que ves por reacciones, no por verdad…») conecta con el radar de prioridad del capítulo 1 y puede cambiarse en `RenderJuego.mision5Fin()`. Los titulares falsos/verdaderos y las frases del Palomo y de civismo están en `Mision5.java`.
