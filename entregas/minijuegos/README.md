# Minijuegos de la Misión 5 · «La calle del pánico»

Cuatro minijuegos cortos, **sin historia**, que se juegan dentro de la misión 5. Cada uno plantea una situación del tema que trabajamos (rumores, noticias falsas, pánico en la red Civitas y el Palomo saboteando) y se resuelve con una **estructura de datos propia** que de verdad decide lo que pasa en el juego.

Se abren desde el menú principal con el botón **«Minijuegos de la misión 5»** (arriba del botón de la Misión 5).

| # | Minijuego | Tipo de reto | Estructura de datos | Duración |
|---|---|---|---|---|
| 1 | Corta la cadena | Detener | Grafo (listas de adyacencia) + BFS con varios orígenes | 60 s |
| 2 | Rastrea la fuente | Buscar | Árbol n-ario con puntero al padre (camino a la raíz) | 85 s |
| 3 | Duelo contra el Palomo | Pelear | AVL del capítulo 1 + búsqueda del **piso** | 90 s |
| 4 | Caza de bots | Buscar y detener | Tabla hash con encadenamiento separado | 65 s |

## Modos y controles (los dos personajes funcionan siempre)

En el selector, cada minijuego tiene tres botones:

- **Solo · Ciudadano**: tú manejas al Ciudadano; la computadora maneja al Periodista (etiqueta **PC**).
- **Solo · Periodista**: tú manejas al Periodista; la computadora maneja al Ciudadano.
- **En pareja**: dos personas en el mismo teclado.

| | Moverse | Acción |
|---|---|---|
| **J1 · Ciudadano** | W A S D | **E** o **Espacio** |
| **J2 · Periodista** | Flechas | **Enter** (o 0 del teclado numérico) |
| **Un jugador** | WASD o flechas | E, Espacio o Enter |

**Esc** pausa y vuelve al selector (aparece «Continuar»); otro Esc vuelve al menú. Cada partida empieza con una cuenta atrás de 3 s que muestra la meta y lo que hace cada personaje. Alto contraste y movimiento reducido funcionan igual que en el resto del juego.

En cada minijuego el Ciudadano y el Periodista tienen **habilidades distintas**, para que convenga cooperar. El ayudante automático trabaja a ratos y reacciona más lento, así que un jugador que no hace nada pierde casi siempre, aunque tenga ayuda.

---

## 1 · Corta la cadena (detener) · Grafo + BFS

**Situación.** La red de Civitas aparece como un mapa de cuentas (círculos) unidas por contactos (líneas). Un rumor empieza en una cuenta y se riega por los contactos.

**Cómo se juega.**
- Cada **ola** (cada 3,2 s; 2,8 s en pareja) el rumor contagia a todas las cuentas sanas que están **a un contacto** de una contagiada.
- Acércate a una cuenta y pulsa la acción:
  - Cuenta **sana** → la **blindas** (no se contagia y corta el camino del rumor). El blindaje dura 15 s (13 s en pareja).
  - Cuenta **contagiada** → la **desmientes** (vuelve a estar bien y queda blindada).
- **Ciudadano:** blinda rápido (0,45 s) y desmiente lento (1,35 s). **Periodista:** desmiente rápido (0,6 s) y blinda lento (0,95 s).
- El **Palomo** baja cada ~12 s a sembrar un rumor nuevo en una cuenta lejos de ustedes (si está blindada, rompe el blindaje).
- **Ganan** si aguantan los 60 s sin que el rumor llegue a la mitad de la red (7 de 14 cuentas, u 8 de 16 en pareja). **Pierden** si llega a la mitad.

**Estructura de datos.** `GrafoRed` es un grafo **no dirigido** con **listas de adyacencia**. La red se arma conectando cada cuenta con sus 2 más cercanas y luego uniendo las componentes sueltas (se verifica con BFS que el grafo quede **conexo**).
- `distanciasBFS(origenes, bloqueados)` hace un **BFS con varios orígenes**: mete en la cola a todas las cuentas contagiadas con distancia 0 y avanza por niveles. Las cuentas **blindadas** están en el conjunto `bloqueados`: no se visitan y, por eso, cortan el recorrido.
- **Una ola = un nivel del BFS**: se contagian exactamente las cuentas con distancia 1 (la «frontera»).
- En el mapa, cada cuenta sana muestra su distancia `d1`, `d2`… al rumor (las `d1` parpadean en rojo: caen en la próxima ola). El panel lateral agrupa las cuentas por nivel del BFS y lista las que quedaron sin camino («a salvo»).
- Costo: BFS O(V + E) cada vez que algo cambia.

## 2 · Rastrea la fuente (buscar) · Árbol n-ario

**Situación.** Un rumor se reenvió de persona en persona en una plaza. Alguien dice «¡me llegó esto!». Hay que encontrar quién lo inventó.

**Cómo se juega.**
- Acércate a una persona y pulsa la acción para preguntarle «¿quién te mandó ese rumor?». Te dice **solo el nombre** de quien se lo mandó.
- Los nombres de la gente **solo se ven cuando te acercas**: hay que buscar a esa persona por la plaza.
- Sigue la cadena hasta la persona que no lo recibió de nadie: es una **cuenta falsa del Palomo** (se le cae el disfraz). Son **3 rondas**, cada una con un árbol más grande.
- **Ciudadano:** pregunta rápido (0,5 s). **Periodista:** pregunta más lento (0,85 s), pero además averigua la **profundidad** («estoy a N reenvíos de la fuente»).
- Si preguntas a alguien que no recibió el rumor, pierdes tiempo (cuenta como error).
- El **Palomo** se para junto a la persona que buscan y la **silencia**; hay que **espantarlo** con la acción.
- **Ganan** al encontrar las 3 fuentes antes de 85 s.

**Estructura de datos.** `ArbolDifusion` es un **árbol n-ario**: cada nodo tiene una lista de hijos (a quiénes reenvió el rumor) y una referencia a su **padre** (de quién lo recibió). La **raíz** es la cuenta del Palomo.
- El árbol se genera con una cadena larga (para que haya profundidad) y el resto de nodos colgados al azar. El juego empieza en la **hoja más profunda**.
- Preguntar = `padre(id)`, **O(1)**. Llegar a la fuente = recorrer `caminoARaiz(hoja)`, **O(profundidad)**.
- También tiene `profundidad`, `altura`, `tamano`, `hojas`, `preorden` y `porNiveles`. Al resolver una ronda, el panel dibuja el árbol completo con su altura y número de nodos.

## 3 · Duelo contra el Palomo (pelear) · AVL + piso

**Situación.** El Palomo flota frente al Centro de Periodismo protegido por un **escudo de chismes**, cada uno con una fuerza (número). Les lanza chismes desde arriba.

**Cómo se juega.**
- **Mantén** la acción para cargar un dato verificado: la fuerza **sube y baja** (como el medidor del golf). **Suelta** para disparar hacia arriba.
- Si el dato golpea al Palomo, desmiente **el chisme más fuerte que tu dato alcanza a romper** (el de mayor fuerza que sea ≤ tu dato). Si tu dato es menor que todos, **rebota**.
- Con el escudo vacío, cada golpe le quita **1 de vida** al Palomo (tiene 3).
- **Ciudadano:** medidor lento y fácil, pero llega solo a **70**. **Periodista:** llega a **99**, pero el medidor va más rápido. Los chismes de fuerza > 70 (color vino) solo los puede romper el Periodista.
- Cada ~7 s el Palomo inventa un chisme nuevo (máximo 8). Los chismes que caen quitan corazones al equipo (8 en total); hay que esquivarlos caminando.
- **Ganan** si dejan al Palomo sin vida. **Pierden** si se quedan sin corazones o se acaban los 90 s.

**Estructura de datos.** Es el **mismo `ArbolAVLPublicaciones` del capítulo 1**: cada chisme es una `Publicacion` con clave `(fuerza, ID)`.
- Se agregó `piso(fuerza)`: baja **una sola rama** del árbol; si el nodo cabe (fuerza ≤ dato) lo guarda como candidato y sigue a la derecha buscando uno mayor; si no cabe, va a la izquierda. Costo **O(log n)** porque el AVL está balanceado.
- Acierto = `piso(dato)` + `eliminar(id)`. Chisme nuevo = `insertar(...)` con sus **rotaciones** (el panel muestra la última operación, por ejemplo «Insertar #6 · riesgo 94 · rotación izquierda en #2»).
- El panel dibuja el AVL con la fuerza, altura y factor de equilibrio de cada nodo, y el recorrido **inorden** (los chismes de menor a mayor fuerza).

## 4 · Caza de bots (buscar y detener) · Tabla hash

**Situación.** En el parque hay gente y, mezcladas, cuentas falsas del Palomo. Cada cuenta lleva una **huella** visible sobre la cabeza (un ícono y un color). Los bots publican cadenas («¡¡COMPARTE YA!!») que suben la desinformación.

**Cómo se juega.**
- El panel muestra la **tabla de huellas de bots** registradas. Busca en la plaza a las cuentas cuya huella está en la tabla y repórtalas.
- **Ciudadano:** reporta a **una** cuenta casi al instante (0,35 s). Si la huella no está en la tabla, era una **persona real**: sube la desinformación.
- **Periodista:** **escaneo de zona** (0,8 s, se recarga en 2,8 s): consulta en la tabla a todas las cuentas cercanas y reporta solo a los bots. Nunca se equivoca, pero es más lento.
- Hay huellas «trampa»: gente real con el mismo ícono o el mismo color que un bot (no los dos).
- Cada ~14 s el **Palomo** crea una **red nueva**: registra una huella nueva en la tabla y hackea 2 o 3 cuentas.
- **Ganan** al reportar 9 bots (10 en pareja) antes de 65 s. **Pierden** si la desinformación llega a 100 o se acaba el tiempo.

**Estructura de datos.** `TablaHash<V>` propia, con **11 cubetas** (número primo) y **encadenamiento separado** (cada cubeta es una lista).
- Clave: el texto de la huella, por ejemplo `megáfono·rojo`. Función hash **polinómica**: `h = (h·31 + carácter) mod 11` para cada carácter.
- Reportar o escanear = `obtener(huella)`: se calcula la cubeta y se compara solo con las claves de esa cubeta. **O(1) en promedio.**
- Cuando dos huellas caen en la misma cubeta hay una **colisión**: quedan encadenadas en la misma lista (el panel lo muestra con «→» y la palabra «colisión»).
- El panel muestra las 11 cubetas, el factor de carga y la última consulta, por ejemplo: `h(«megáfono·azul») mod 11 = 8 · 1 comparación · ¡BOT!`.

---

## Código

Archivos nuevos:

- `compartido/estructuras/grafos/GrafoRed.java`: grafo no dirigido, BFS con orígenes múltiples y bloqueos, componentes conexas.
- `compartido/estructuras/difusion/ArbolDifusion.java`: árbol n-ario con padre, camino a la raíz, profundidad, altura, recorridos.
- `compartido/estructuras/hash/TablaHash.java`: tabla hash con encadenamiento y registro de la última búsqueda.
- `servidor/minijuegos/Minijuego.java`: base común (reloj, cuenta atrás, los dos personajes, entrada humana, ayudante automático, tareas «acercarse y mantener», Palomo, efectos). No dibuja nada.
- `servidor/minijuegos/CortaLaCadena.java`, `RastreaLaFuente.java`, `DueloPalomo.java`, `CazaDeBots.java`: la lógica de cada minijuego.
- `cliente/interfaz/RenderMinijuegos.java`: selector, partida (escena + panel de la estructura + medidores) y resultado.

Archivos modificados:

- `ArbolAVLPublicaciones.java`: se agregaron `piso`, `minimo` y `maximo` (no cambia nada de lo anterior).
- `JuegoCliente.java`: pantallas `MINIJUEGOS`, `MINIJUEGO`, `MINIJUEGO_FIN`, teclado compartido (J1 / J2) y navegación.
- `ClienteSketch.java`: en la partida, Enter es la acción de J2 (no activa botones); título de la ventana.
- `RenderJuego.java`: botón en el menú principal y ayudantes de dibujo visibles para `RenderMinijuegos`.
- `EvidenciasApp.java`: genera las capturas de `entregas/minijuegos/evidencias`.

La lógica sigue la misma regla del proyecto: el servidor (aquí, la clase del minijuego) es la autoridad y el cliente solo envía la entrada (dirección + botón) y dibuja las vistas.

## Pruebas

- `estructuras/EstructurasMinijuegosTest.java`: grafo y BFS (orígenes múltiples, bloqueos, componentes), árbol n-ario (padre, camino, altura, recorridos), piso del AVL (comparado con búsqueda lineal tras inserciones y eliminaciones) y tabla hash (función, colisiones, comparaciones).
- `reglas/MinijuegosTest.java`: cuenta atrás, modos, teclado por jugador, que sin jugar se pierde, que el ayudante solo no gana la mayoría, olas de BFS, blindar/desmentir, subir el árbol hasta la raíz, piso al acertar, consulta de la tabla al reportar, redes nuevas del Palomo.
- `integracion/MinijuegosInterfazTest.java`: menú → selector (12 opciones), pausa y continuar, teclado compartido, pulsación corta, reintentar, y que todas las pantallas se dibujen dentro de la ventana en todos los modos, con y sin contraste.

En total el proyecto queda con **97 pruebas automatizadas** y todas pasan (`bash herramientas.sh test`).

## Balance

Se calibró con jugadores automáticos (40 partidas por fila). «Atento» simula a un jugador que reacciona bien; «quieto» no toca el teclado.

| Minijuego | En pareja, atentos | Solo (con ayudante), atento | Solo, quieto |
|---|---|---|---|
| Corta la cadena | gana 100 % | gana 100 % | gana 0 % |
| Rastrea la fuente | gana 100 % | gana 100 % | gana 7–22 % |
| Duelo contra el Palomo | gana 75 % | gana 67–70 % | gana 0–5 % |
| Caza de bots | gana 77 % | gana 90 % | gana 2–7 % |

El duelo es el más difícil a propósito (es el «jefe»). Los parámetros de dificultad están al principio del constructor de cada minijuego.

## Evidencias

`bash herramientas.sh evidencias` regenera las capturas en `entregas/minijuegos/evidencias/`: menú, selector, y para cada minijuego la cuenta atrás, una partida en curso y el resultado.

## Preguntas típicas para la sustentación

- **¿Por qué BFS y no DFS en «Corta la cadena»?** Porque el rumor se propaga por niveles: en cada ola llega a todos los que están a un contacto más. BFS visita exactamente en ese orden (distancia 1, luego 2, …) y con varios orígenes da la distancia al contagio más cercano.
- **¿Cómo «corta» el blindaje?** Las cuentas blindadas están en el conjunto de bloqueados del BFS: no entran a la cola, así que lo que está detrás no es alcanzable (distancia −1).
- **¿Por qué el árbol de «Rastrea la fuente» guarda el padre?** Porque la pregunta del juego es «¿quién te lo mandó?», que es justo el padre. Con esa referencia subir cuesta O(1) por paso; sin ella habría que buscar desde la raíz.
- **¿Qué es el piso en el AVL?** El mayor elemento que es menor o igual a un valor. Se encuentra bajando una sola rama (O(log n)) porque el AVL está ordenado por (fuerza, ID) y balanceado.
- **¿Por qué el duelo usa el mismo AVL del capítulo 1?** Porque ya ordenaba publicaciones por (riesgo, ID); un chisme es una publicación cuyo riesgo es su fuerza. Solo se agregó la operación `piso`.
- **¿Por qué la tabla hash tiene 11 cubetas?** Un tamaño primo reparte mejor las claves con la función polinómica. Las colisiones se resuelven con encadenamiento (una lista por cubeta); con 4 redes como máximo el factor de carga es bajo y la búsqueda es O(1) en promedio.
- **¿Cómo funcionan los dos jugadores en un teclado?** Cada cuadro el cliente lee las teclas de J1 (WASD + E) y de J2 (flechas + Enter) y le entrega a cada personaje su dirección y su botón. En un jugador, el personaje que no maneja el humano recibe esa misma entrada desde un ayudante automático.
