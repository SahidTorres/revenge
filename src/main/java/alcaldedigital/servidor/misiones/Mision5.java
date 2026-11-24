package alcaldedigital.servidor.misiones;

import alcaldedigital.compartido.estructuras.avl.ArbolAVLPublicaciones;
import alcaldedigital.compartido.modelo.Publicacion;
import java.util.*;

/**
 * Misión 5 · «La calle del pánico». Autoridad de la misión: reloj de 70 s, noticias falsas,
 * vecinos asustados, jugador y Palomo. No depende de gráficos: el cliente solo dibuja las vistas.
 *
 * <p>Las noticias que llegan al sistema se indexan en el mismo AVL de publicaciones del capítulo 1,
 * con clave (riesgo, ID). El Periodista ve primero la de mayor prioridad (recorrido inverso),
 * una noticia falsa que se viraliza cambia de riesgo mediante reinserción, y al verificarla
 * o esparcirse se elimina del índice.</p>
 */
public final class Mision5 {
    public enum Modo { SOLO, DUO }
    public enum Estado { PREPARACION, EN_CURSO, VICTORIA, DERROTA }
    public enum Motivo { NINGUNO, METAS, TIEMPO, PANICO, DESINFORMACION }
    public enum Fase { ACTIVA, VERIFICANDO, DESMENTIDA, CONFIRMADA, ESPARCIDA }
    public enum Conducta { ALTERADO, HABLANDO, CALMADO, SALIENDO }
    public enum Travesura { BURLA, CHISME, SABOTAJE }

    public static final double DURACION = 70, PREPARACION = 3;
    /** Mundo lógico en píxeles de 16×16 tiles: 27×15 tiles. */
    public static final int ANCHO = 432, ALTO = 242, VISIBLES = 4, MAX_ACTIVAS = 7;
    public static final float X_MIN = 16, X_MAX = 416, Y_MIN = 88, Y_MAX = 226;
    public static final float PERIODISTA_X = 150, PERIODISTA_Y = 84;
    public static final int[] LAMPARAS_X = {44, 100, 332, 388};
    public static final int VARIANTES = 12;
    /** Segundos junto al vecino, alcance en píxeles y velocidad de un vecino alterado. */
    static final double TIEMPO_HABLA = 1.5, ALCANCE = 28, VEL_ALTERADO = 34;

    public record NoticiaVista(long id, String titular, String fuente, int prioridad, boolean viral,
                               Fase fase, double vida, double verificacion) { }
    public record CierreVista(String texto, Fase fase, double restante) { }
    public record VecinoVista(int id, float x, float y, int direccion, int variante, int panico,
                              Conducta conducta, String globo, boolean cerca) { }
    public record JugadorVista(float x, float y, int direccion, boolean caminando, boolean hablando,
                               double avanceHabla, String globo) { }
    public record PalomoVista(float x, float y, String globo, Travesura accion, boolean volando) { }
    public record ZonaVista(float x, float y, float radio, double restante) { }
    public record EfectoVista(String tipo, float x, float y, double edad) { }

    private record Item(String titular, String fuente, boolean falsa) { }

    private static final List<Item> FALSAS = List.of(
        new Item("URGENTE: Civitas cobrará multa por cada paloma que mires.", "Kevin Viral", true),
        new Item("Hackers borrarán todas las fotos de la ciudad a medianoche.", "Cadena reenviada", true),
        new Item("El agua del grifo ya viene con Wi-Fi y con virus, dicen.", "Cleta comenta", true),
        new Item("La alcaldía renunció y las palomas gobiernan desde hoy.", "El Palomar Satírico (recorte)", true),
        new Item("Cierran todos los semáforos hasta que Civitas se arregle.", "Tío Reenvíos", true),
        new Item("Experto de cinco minutos: el internet se acaba el lunes.", "Universidad de Expertos", true),
        new Item("Un bot pide claves en la plaza. ¡Evacuen la calle!", "Chat de la cuadra", true),
        new Item("Civitas leerá tus mensajes y los publicará en el noticiero.", "Kevin Viral", true),
        new Item("Se acabó la comida en todos los supermercados. ¡Corran!", "Cadena reenviada", true),
        new Item("Los buses no circularán mañana por un «error de la nube».", "Tío Reenvíos", true),
        new Item("Los cajeros regalarán dinero si compartes esto 10 veces.", "Cadena reenviada", true),
        new Item("Prohíben hablar de Civitas en la calle. Multas desde hoy.", "Voz del Barrio Wi-Fi", true),
        new Item("Toque de queda en toda Ciudad Nova a partir de las 6 p. m.", "Canal Casi Serio (recorte)", true),
        new Item("La paloma líder del ataque vive en tu mismo barrio.", "Cleta comenta", true));
    private static final List<Item> VERDADERAS = List.of(
        new Item("La alcaldía confirma mantenimiento de Civitas esta noche.", "Alcaldía de Ciudad Nova", false),
        new Item("Periodismo informa un ataque informático; investiga su origen.", "Centro de Periodismo", false),
        new Item("Los buses funcionan con normalidad en todas las rutas.", "Transporte municipal", false),
        new Item("La biblioteca ofrece un taller gratis para verificar noticias.", "Biblioteca municipal", false),
        new Item("La línea ciudadana pide verificar antes de reenviar mensajes.", "Línea ciudadana", false));
    private static final List<String> MENSAJES_CIVICOS = List.of(
        "Respira hondo: verificar antes de compartir nos cuida a todos.",
        "Un rumor no es una noticia. Esperemos la fuente oficial.",
        "Entre vecinos nos cuidamos: la calma también se contagia.",
        "Si dudas, no reenvíes. Pregunta y contrasta.",
        "Nadie está solo en Civitas: hablemos con respeto y calma.",
        "El miedo viaja rápido; la verdad llega con calma. Esperemos.");
    private static final List<String> RESPUESTAS = List.of(
        "Gracias… ya respiro.", "Tienes razón, no lo reenvío.", "Uf, me tranquilicé.",
        "Mejor pregunto antes.", "Cierto, con calma se piensa mejor.");
    private static final List<String> BURLAS = List.of(
        "¡Cuuu! ¿Siguen creyendo todo lo que leen?",
        "Un chisme más y la ciudad explota, jajá.",
        "Yo reenvío más rápido de lo que ustedes verifican.",
        "Qué lindos, tan serios con sus mensajitos de civismo.",
        "¡Suelten el teléfono, humanos!");
    private static final List<String> CHISMES = List.of(
        "¿Ya supieron lo que dicen por ahí?", "Me contaron algo… y no pienso verificarlo.");
    private static final List<String> SABOTAJES = List.of(
        "¡Se apagaron las lámparas! Qué miedo, qué miedo.", "Un poquito de oscuridad no le hace mal a nadie.");

    private static final class Noticia {
        final long id; final String titular, fuente; final boolean falsa, chisme; final double vida;
        int riesgo; double edad, progreso; boolean viral; Fase fase = Fase.ACTIVA;
        Noticia(long id, Item item, boolean chisme, int riesgo, double vida) {
            this.id = id; this.titular = item.titular(); this.fuente = chisme ? "Chisme del Palomo" : item.fuente();
            this.falsa = item.falsa(); this.chisme = chisme; this.riesgo = riesgo; this.vida = vida;
        }
    }
    private static final class Vecino {
        final int id; float x, y, tx, ty; int direccion, variante; double panico, conductaT, retarget, talla;
        Conducta conducta = Conducta.ALTERADO; String globo = ""; double globoT;
        Vecino(int id) { this.id = id; }
    }
    private static final class Cierre {
        final String texto; final Fase fase; double restante = 3.2;
        Cierre(String texto, Fase fase) { this.texto = texto; this.fase = fase; }
    }
    private static final class Zona { float x, y; double restante; }
    private static final class Efecto { String tipo; float x, y; double edad; }

    private final Modo modo;
    private final long semilla;
    private final Random azar;
    private final int metaDesmentidas, metaCalmados, numVecinos;
    private final double intervaloNoticias, vidaNoticia, subidaPanico, intervaloPalomo, tiempoVerificacion;

    private Estado estado = Estado.PREPARACION;
    private Motivo motivo = Motivo.NINGUNO;
    private double preparacion = PREPARACION, tiempo;
    private double panicoCiudad = 15, desinformacion = 15;
    private int desmentidas, calmados, puntos;
    private String mensaje = "Prepárense: las noticias falsas empiezan a llegar.";

    private final ArbolAVLPublicaciones radar = new ArbolAVLPublicaciones();
    private final Map<Long, Noticia> noticias = new LinkedHashMap<>();
    private final List<Cierre> cierres = new ArrayList<>();
    private final List<String> historial = new ArrayList<>();
    private final List<Efecto> efectos = new ArrayList<>();
    private long siguienteNoticia = 1;
    private double proximaNoticia = .4;
    private int cursorFalsas, cursorVerdaderas, cursorCivico;
    private final List<Integer> ordenFalsas = new ArrayList<>(), ordenVerdaderas = new ArrayList<>();
    private Noticia verificando;

    private final List<Vecino> vecinos = new ArrayList<>();
    private float jx = 240, jy = 150;
    private int jdireccion;
    private boolean moviendo, caminando;
    private Vecino hablando;
    private String globoJugador = "";
    private double globoJugadorT;

    private float px = 216, py = 52;
    private Travesura travesura;
    private double tTravesura, proximoPalomo;
    private boolean ejecutada;
    private float objetivoX, objetivoY;
    private Vecino victimaPalomo;
    private String globoPalomo = "";
    private double globoPalomoT;
    private Zona zona;
    private double lamparasApagadas;

    public Mision5(Modo modo, long semilla) {
        this.modo = Objects.requireNonNull(modo);
        this.semilla = semilla;
        this.azar = new Random(semilla * 31 + modo.ordinal());
        boolean solo = modo == Modo.SOLO;
        metaDesmentidas = solo ? 8 : 11;
        metaCalmados = solo ? 7 : 10;
        numVecinos = solo ? 6 : 7;
        intervaloNoticias = solo ? 3.0 : 2.6;
        vidaNoticia = solo ? 10 : 9;
        subidaPanico = solo ? 9 : 10;
        intervaloPalomo = solo ? 11 : 8.5;
        tiempoVerificacion = 1.1;
        proximoPalomo = solo ? 9 : 7;
        for (int i = 0; i < FALSAS.size(); i++) ordenFalsas.add(i);
        for (int i = 0; i < VERDADERAS.size(); i++) ordenVerdaderas.add(i);
        Collections.shuffle(ordenFalsas, azar);
        Collections.shuffle(ordenVerdaderas, azar);
        for (int i = 0; i < numVecinos; i++) {
            Vecino v = new Vecino(i);
            v.variante = (i * 5) % VARIANTES;
            v.x = 40 + azar.nextFloat() * (ANCHO - 80);
            v.y = Y_MIN + 10 + azar.nextFloat() * (Y_MAX - Y_MIN - 20);
            if (Math.hypot(v.x - jx, v.y - jy) < 60) v.x = (v.x + 180) % (ANCHO - 60) + 30;
            v.panico = 12 + azar.nextDouble() * 30;
            elegirDestino(v);
            vecinos.add(v);
        }
        historial.add("Inicio: Civitas está en pánico. Dos voces, una calle, 70 segundos.");
    }

    // ───────────────────────── Consultas ─────────────────────────
    public Modo modo() { return modo; }
    public long semilla() { return semilla; }
    public Estado estado() { return estado; }
    public Motivo motivo() { return motivo; }
    public boolean terminada() { return estado == Estado.VICTORIA || estado == Estado.DERROTA; }
    public double restante() { return Math.max(0, DURACION - tiempo); }
    public double transcurrido() { return tiempo; }
    public int cuentaAtras() { return (int) Math.ceil(Math.max(0, preparacion)); }
    public int metaDesmentidas() { return metaDesmentidas; }
    public int metaCalmados() { return metaCalmados; }
    public int desmentidas() { return desmentidas; }
    public int calmados() { return calmados; }
    public int panico() { return (int) Math.round(panicoCiudad); }
    public int desinformacion() { return (int) Math.round(desinformacion); }
    public int puntos() { return puntos; }
    public String mensaje() { return mensaje; }
    public List<String> historial() { return List.copyOf(historial); }
    public ArbolAVLPublicaciones radar() { return radar; }
    public boolean verificando() { return verificando != null; }
    public double lamparasApagadas() { return lamparasApagadas; }

    public String motivoTexto() {
        return switch (motivo) {
            case METAS -> "Verificaste y calmaste a tiempo: la calle respira.";
            case TIEMPO -> "Se acabaron los 70 segundos sin completar ambas tareas.";
            case PANICO -> "El pánico desbordó la calle: demasiados vecinos sin calmar.";
            case DESINFORMACION -> "La desinformación se esparció por toda la red de Civitas.";
            case NINGUNO -> "";
        };
    }

    public JugadorVista jugador() {
        return new JugadorVista(jx, jy, jdireccion, caminando, hablando != null,
            hablando == null ? 0 : Math.min(1, hablando.talla / TIEMPO_HABLA), globoJugadorT > 0 ? globoJugador : "");
    }
    public PalomoVista palomo() {
        return new PalomoVista(px, py, globoPalomoT > 0 ? globoPalomo : "", travesura, travesura != null);
    }
    public ZonaVista zona() { return zona == null ? null : new ZonaVista(zona.x, zona.y, 52, zona.restante); }
    public int vecinoCercano() {
        Vecino v = masCercano();
        return v == null ? -1 : v.id;
    }
    public List<VecinoVista> vecinos() {
        int cerca = vecinoCercano();
        List<VecinoVista> salida = new ArrayList<>();
        for (Vecino v : vecinos)
            salida.add(new VecinoVista(v.id, v.x, v.y, v.direccion, v.variante, (int) Math.round(Math.min(100, v.panico)),
                v.conducta, v.globoT > 0 ? v.globo : "", v.id == cerca));
        return salida;
    }
    public List<EfectoVista> efectos() {
        List<EfectoVista> salida = new ArrayList<>();
        for (Efecto e : efectos) salida.add(new EfectoVista(e.tipo, e.x, e.y, e.edad));
        return salida;
    }
    public List<CierreVista> cierres() {
        List<CierreVista> salida = new ArrayList<>();
        for (Cierre c : cierres) salida.add(new CierreVista(c.texto, c.fase, c.restante));
        return salida;
    }
    /** Las cuatro noticias de mayor prioridad según el AVL. */
    public List<NoticiaVista> noticias() {
        List<NoticiaVista> salida = new ArrayList<>();
        for (Noticia n : visibles())
            salida.add(new NoticiaVista(n.id, n.titular, n.fuente, n.riesgo, n.viral, n.fase,
                Math.max(0, 1 - n.edad / n.vida), n.fase == Fase.VERIFICANDO ? Math.min(1, n.progreso) : 0));
        return salida;
    }
    public int enCola() { return Math.max(0, radar.cantidad() - VISIBLES); }
    public int activas() { return noticias.size(); }

    private List<Noticia> visibles() {
        List<Noticia> lista = new ArrayList<>();
        for (Publicacion p : radar.recorrer("inverso")) {
            Noticia n = noticias.get(p.id());
            if (n != null) lista.add(n);
            if (lista.size() == VISIBLES) break;
        }
        return lista;
    }

    // ───────────────────────── Acciones del jugador ─────────────────────────
    /** Ciudadano: desplazamiento ya escalado por el cliente (como SesionLocal.mover). */
    public void mover(float dx, float dy) {
        if (estado != Estado.EN_CURSO || !Float.isFinite(dx) || !Float.isFinite(dy)) return;
        if (dx == 0 && dy == 0) return;
        float largo = (float) Math.hypot(dx, dy);
        if (largo > 4) { dx *= 4 / largo; dy *= 4 / largo; }
        jx = Math.max(X_MIN, Math.min(X_MAX, jx + dx));
        jy = Math.max(Y_MIN, Math.min(Y_MAX, jy + dy));
        jdireccion = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 2 : 1) : (dy > 0 ? 0 : 3);
        moviendo = true;
    }

    /** Ciudadano: comienza a calmar al vecino alterado más cercano con un mensaje de civismo. */
    public boolean interactuar() {
        if (estado != Estado.EN_CURSO) return false;
        if (hablando != null) return false;
        Vecino v = masCercano();
        if (v == null) { mensaje = "Acércate a un vecino alterado para calmarlo."; return false; }
        hablando = v; v.conducta = Conducta.HABLANDO; v.talla = 0;
        globoJugador = MENSAJES_CIVICOS.get(cursorCivico++ % MENSAJES_CIVICOS.size());
        globoJugadorT = 2.2;
        mensaje = "Hablas con un vecino: «" + globoJugador + "»";
        return true;
    }

    /** Periodista: verifica la noticia visible en la posición indicada (0 = la más prioritaria). */
    public boolean verificar(int posicion) {
        if (estado != Estado.EN_CURSO) return false;
        List<Noticia> lista = visibles();
        if (posicion < 0 || posicion >= lista.size()) return false;
        Noticia n = lista.get(posicion);
        if (verificando != null) { mensaje = "Ya estás verificando otra noticia."; return false; }
        if (n.fase != Fase.ACTIVA) return false;
        n.fase = Fase.VERIFICANDO; n.progreso = 0; verificando = n;
        mensaje = "Verificando fuentes de «" + recorte(n.titular) + "»";
        return true;
    }

    // ───────────────────────── Simulación ─────────────────────────
    public void actualizar(double dt) {
        if (!Double.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Tiempo inválido");
        if (terminada()) { avanzarEfectos(dt); return; }
        dt = Math.min(.1, dt);
        caminando = moviendo; moviendo = false;
        avanzarEfectos(dt);
        if (estado == Estado.PREPARACION) {
            preparacion -= dt;
            actualizarVecinos(dt, false);
            actualizarPalomoReposo(dt);
            if (preparacion <= 0) { estado = Estado.EN_CURSO; mensaje = "¡Ya! Desmiente y calma antes de que se acabe el tiempo."; }
            return;
        }
        tiempo += dt;
        panicoCiudad = Math.max(0, panicoCiudad - .5 * dt);
        actualizarNoticias(dt);
        actualizarHabla(dt);
        actualizarVecinos(dt, true);
        actualizarPalomo(dt);
        globoJugadorT = Math.max(0, globoJugadorT - dt);
        for (Iterator<Cierre> it = cierres.iterator(); it.hasNext(); ) {
            Cierre c = it.next(); c.restante -= dt; if (c.restante <= 0) it.remove();
        }
        comprobarFin();
    }

    private void comprobarFin() {
        if (panicoCiudad >= 100) terminar(Estado.DERROTA, Motivo.PANICO);
        else if (desinformacion >= 100) terminar(Estado.DERROTA, Motivo.DESINFORMACION);
        else if (desmentidas >= metaDesmentidas && calmados >= metaCalmados) terminar(Estado.VICTORIA, Motivo.METAS);
        else if (tiempo >= DURACION) terminar(Estado.DERROTA, Motivo.TIEMPO);
    }

    private void terminar(Estado resultado, Motivo causa) {
        estado = resultado; motivo = causa; hablando = null;
        if (resultado == Estado.VICTORIA) {
            puntos += (int) restante() * 2;
            historial.add("Victoria: el Palomo huye y deja caer una pista sobre Civitas.");
            mensaje = "Menos mal logramos frenar esta histeria colectiva antes de que empeorara en la red.";
        } else {
            historial.add("Derrota: " + motivoTexto());
            mensaje = "La ciudad cayó en la mentira. Perdiste.";
        }
        if (verificando != null) { verificando.fase = Fase.ACTIVA; verificando = null; }
        for (Vecino v : vecinos) if (v.conducta == Conducta.HABLANDO) v.conducta = Conducta.ALTERADO;
    }

    private void actualizarNoticias(double dt) {
        proximaNoticia -= dt;
        if (proximaNoticia <= 0 && noticias.size() < MAX_ACTIVAS) {
            crearNoticia(false);
            proximaNoticia = intervaloNoticias * (.75 + .5 * azar.nextDouble());
        }
        for (Noticia n : new ArrayList<>(noticias.values())) {
            if (n.fase == Fase.VERIFICANDO) {
                n.progreso += dt / tiempoVerificacion;
                if (n.progreso >= 1) cerrarVerificacion(n);
                continue;
            }
            n.edad += dt;
            if (n.falsa && !n.viral && n.edad >= n.vida * .5) {
                n.viral = true; n.riesgo = Math.min(100, n.riesgo + 25);
                radar.actualizar(publicacion(n, "Viralizándose"));
                historial.add("Viral: «" + recorte(n.titular) + "» sube a prioridad " + n.riesgo + ".");
            }
            if (n.edad >= n.vida) esparcirse(n);
        }
    }

    private void cerrarVerificacion(Noticia n) {
        verificando = null;
        if (n.falsa) {
            desmentidas++; puntos += 10; desinformacion = Math.max(0, desinformacion - 4);
            cerrar(n, Fase.DESMENTIDA, "Desmentida: «" + recorte(n.titular) + "»");
            historial.add("Desmentida: «" + recorte(n.titular) + "» · +10 puntos.");
            mensaje = "¡Desmentida! La mentira no se esparce.";
            efecto("verificacion", PERIODISTA_X, PERIODISTA_Y - 14);
        } else {
            puntos += 2;
            cerrar(n, Fase.CONFIRMADA, "Verdadera, no se desmiente: «" + recorte(n.titular) + "»");
            historial.add("Verificada y verdadera: «" + recorte(n.titular) + "» · +2 puntos.");
            mensaje = "Era verdadera: verificar también evita desmentir de más.";
        }
    }

    private void esparcirse(Noticia n) {
        if (n.falsa) {
            desinformacion = Math.min(100, desinformacion + 6 + n.riesgo / 8.0);
            cerrar(n, Fase.ESPARCIDA, "Se esparció: «" + recorte(n.titular) + "»");
            historial.add("Se esparció: «" + recorte(n.titular) + "» · desinformación sube.");
            mensaje = "Una mentira se esparció por la red.";
        } else cerrar(n, Fase.ESPARCIDA, "Caducó una noticia verdadera sin consultar.");
    }

    private void cerrar(Noticia n, Fase fase, String texto) {
        n.fase = fase; radar.eliminar(n.id); noticias.remove(n.id);
        cierres.add(0, new Cierre(texto, fase));
        while (cierres.size() > 2) cierres.remove(cierres.size() - 1);
    }

    private void crearNoticia(boolean chisme) {
        Item item; int riesgo; double vida = vidaNoticia;
        if (chisme) {
            item = FALSAS.get(ordenFalsas.get(cursorFalsas++ % ordenFalsas.size()));
            riesgo = 55 + azar.nextInt(25); vida = 11;
        } else if (azar.nextDouble() < .78) {
            item = FALSAS.get(ordenFalsas.get(cursorFalsas++ % ordenFalsas.size()));
            riesgo = 25 + azar.nextInt(40);
        } else {
            item = VERDADERAS.get(ordenVerdaderas.get(cursorVerdaderas++ % ordenVerdaderas.size()));
            riesgo = 8 + azar.nextInt(20);
        }
        Noticia n = new Noticia(siguienteNoticia++, item, chisme, riesgo, vida);
        noticias.put(n.id, n);
        radar.insertar(publicacion(n, "Sin comprobar"));
        if (!chisme) mensaje = "Llegó una publicación nueva a Civitas.";
    }

    private Publicacion publicacion(Noticia n, String estadoTexto) {
        return new Publicacion(n.id, n.riesgo, n.fuente, n.titular, estadoTexto);
    }

    private Vecino masCercano() {
        Vecino mejor = null; double d = ALCANCE;
        for (Vecino v : vecinos) {
            if (v.conducta != Conducta.ALTERADO) continue;
            double dist = Math.hypot(v.x - jx, v.y - jy);
            if (dist < d) { d = dist; mejor = v; }
        }
        return mejor;
    }

    private void actualizarHabla(double dt) {
        if (hablando == null) return;
        Vecino v = hablando;
        if (Math.hypot(v.x - jx, v.y - jy) > ALCANCE + 14) {
            v.conducta = Conducta.ALTERADO; hablando = null;
            mensaje = "Te alejaste antes de terminar: el vecino sigue alterado."; return;
        }
        v.talla += dt;
        if (v.talla >= TIEMPO_HABLA) {
            v.conducta = Conducta.CALMADO; v.panico = 0; v.conductaT = 4.5;
            v.globo = RESPUESTAS.get(azar.nextInt(RESPUESTAS.size())); v.globoT = 2.2;
            calmados++; puntos += 10; panicoCiudad = Math.max(0, panicoCiudad - 7);
            historial.add("Calmaste a un vecino con un mensaje de civismo · +10 puntos.");
            mensaje = "Vecino calmado (" + calmados + "/" + metaCalmados + ").";
            efecto("verificacion", v.x, v.y - 14);
            hablando = null;
        }
    }

    private void actualizarVecinos(double dt, boolean activo) {
        for (Vecino v : vecinos) {
            v.globoT = Math.max(0, v.globoT - dt);
            switch (v.conducta) {
                case ALTERADO -> {
                    if (activo) {
                        v.panico += subidaPanico * dt * (.85 + .15 * (v.id % 3));
                        if (zona != null && Math.hypot(v.x - zona.x, v.y - zona.y) < 52) v.panico += 9 * dt;
                        if (v.panico >= 100) {
                            v.panico = 62; panicoCiudad = Math.min(100, panicoCiudad + 9);
                            efecto("rumor", v.x, v.y - 14);
                            historial.add("Un vecino entró en pánico: la calle se agita.");
                            mensaje = "¡Un vecino entró en pánico! Ve a calmarlo.";
                        }
                    }
                    caminar(v, dt, VEL_ALTERADO);
                }
                case HABLANDO -> { }
                case CALMADO -> {
                    caminar(v, dt, 12);
                    v.conductaT -= dt;
                    if (v.conductaT <= 0) {
                        v.conducta = Conducta.SALIENDO;
                        v.tx = v.x < ANCHO / 2f ? -16 : ANCHO + 16; v.ty = v.y;
                    }
                }
                case SALIENDO -> {
                    caminar(v, dt, 34);
                    if (v.x < -8 || v.x > ANCHO + 8) reaparecer(v);
                }
            }
        }
        if (zona != null) { zona.restante -= dt; if (zona.restante <= 0) zona = null; }
        lamparasApagadas = Math.max(0, lamparasApagadas - dt);
    }

    private void reaparecer(Vecino v) {
        boolean izquierda = azar.nextBoolean();
        v.x = izquierda ? -10 : ANCHO + 10;
        v.y = Y_MIN + azar.nextFloat() * (Y_MAX - Y_MIN);
        v.variante = (v.variante + 1 + azar.nextInt(VARIANTES - 1)) % VARIANTES;
        v.panico = 18 + azar.nextDouble() * 28; v.conducta = Conducta.ALTERADO;
        elegirDestino(v);
    }

    private void elegirDestino(Vecino v) {
        v.tx = X_MIN + azar.nextFloat() * (X_MAX - X_MIN);
        v.ty = Y_MIN + azar.nextFloat() * (Y_MAX - Y_MIN);
        v.retarget = 1.6 + azar.nextDouble() * 2.4;
    }

    private void caminar(Vecino v, double dt, double velocidad) {
        float dx = v.tx - v.x, dy = v.ty - v.y;
        double dist = Math.hypot(dx, dy);
        boolean saliendo = v.conducta == Conducta.SALIENDO;
        v.retarget -= dt;
        if (!saliendo && (dist < 3 || v.retarget <= 0)) { elegirDestino(v); return; }
        if (dist < 1) return;
        double paso = Math.min(dist, velocidad * dt);
        v.x += (float) (dx / dist * paso); v.y += (float) (dy / dist * paso);
        if (!saliendo || (v.x >= X_MIN && v.x <= X_MAX)) v.y = Math.max(Y_MIN, Math.min(Y_MAX, v.y));
        v.direccion = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 2 : 1) : (dy > 0 ? 0 : 3);
    }

    // ───────────────────────── Palomo ─────────────────────────
    private void actualizarPalomoReposo(double dt) {
        px += (float) ((216 + 120 * Math.sin(tiempo * .45) - px) * Math.min(1, dt * 2));
    }

    private void actualizarPalomo(double dt) {
        globoPalomoT = Math.max(0, globoPalomoT - dt);
        if (travesura == null) {
            proximoPalomo -= dt;
            float meta = (float) (216 + 150 * Math.sin(tiempo * .45));
            px += Math.signum(meta - px) * Math.min(Math.abs(meta - px), 90 * (float) dt);
            py += Math.signum(52 - py) * Math.min(Math.abs(52 - py), 60 * (float) dt);
            if (proximoPalomo <= 0) iniciarTravesura();
            return;
        }
        tTravesura += dt;
        if (victimaPalomo != null && travesura == Travesura.BURLA) { objetivoX = victimaPalomo.x; objetivoY = Math.max(48, victimaPalomo.y - 62); }
        float ddx = objetivoX - px, ddy = objetivoY - py;
        double dist = Math.hypot(ddx, ddy);
        if (dist > 1) {
            double paso = Math.min(dist, 150 * dt);
            px += (float) (ddx / dist * paso); py += (float) (ddy / dist * paso);
        }
        if (!ejecutada && tTravesura >= 1.0) { ejecutada = true; ejecutarTravesura(); }
        if (tTravesura >= 3.2) {
            travesura = null; victimaPalomo = null;
            proximoPalomo = intervaloPalomo * (.8 + .4 * azar.nextDouble());
        }
    }

    private void iniciarTravesura() {
        double r = azar.nextDouble();
        travesura = r < .4 ? Travesura.BURLA : r < .75 ? Travesura.CHISME : Travesura.SABOTAJE;
        tTravesura = 0; ejecutada = false;
        switch (travesura) {
            case BURLA -> {
                globoPalomo = BURLAS.get(azar.nextInt(BURLAS.size()));
                List<Vecino> candidatos = new ArrayList<>();
                for (Vecino v : vecinos) if (v.conducta == Conducta.ALTERADO || v.conducta == Conducta.CALMADO) candidatos.add(v);
                victimaPalomo = candidatos.isEmpty() ? null : candidatos.get(azar.nextInt(candidatos.size()));
                objetivoX = victimaPalomo == null ? 216 : victimaPalomo.x; objetivoY = 60;
            }
            case CHISME -> {
                globoPalomo = CHISMES.get(azar.nextInt(CHISMES.size()));
                objetivoX = 60 + azar.nextFloat() * 310; objetivoY = 56;
            }
            case SABOTAJE -> {
                globoPalomo = SABOTAJES.get(azar.nextInt(SABOTAJES.size()));
                objetivoX = 70 + azar.nextFloat() * 290; objetivoY = 90;
            }
        }
        globoPalomoT = 2.8;
        historial.add("El Palomo interrumpe: " + switch (travesura) {
            case BURLA -> "burla."; case CHISME -> "suelta un chisme."; case SABOTAJE -> "sabotea la calle."; });
    }

    private void ejecutarTravesura() {
        switch (travesura) {
            case BURLA -> {
                if (victimaPalomo != null) {
                    victimaPalomo.panico = Math.min(99, victimaPalomo.panico + 28);
                    if (victimaPalomo.conducta == Conducta.CALMADO) { victimaPalomo.conducta = Conducta.ALTERADO; victimaPalomo.panico = 45; }
                    efecto("rumor", victimaPalomo.x, victimaPalomo.y - 14);
                }
                mensaje = "El Palomo se burla y asusta a un vecino.";
            }
            case CHISME -> {
                crearNoticia(true);
                efecto("rumor", px, py + 20);
                mensaje = "El Palomo soltó un chisme: ¡nueva noticia falsa en Civitas!";
            }
            case SABOTAJE -> {
                zona = new Zona(); zona.x = objetivoX; zona.y = Math.max(Y_MIN + 20, Math.min(Y_MAX - 10, 150 + azar.nextFloat() * 50 - 25)); zona.restante = 5;
                lamparasApagadas = 6; panicoCiudad = Math.min(100, panicoCiudad + 3);
                efecto("rumor", zona.x, zona.y - 14);
                mensaje = "El Palomo apagó las lámparas: la zona oscura asusta a los vecinos.";
            }
        }
    }

    private void efecto(String tipo, float x, float y) {
        Efecto e = new Efecto(); e.tipo = tipo; e.x = x; e.y = y; efectos.add(e);
    }
    private void avanzarEfectos(double dt) {
        for (Iterator<Efecto> it = efectos.iterator(); it.hasNext(); ) {
            Efecto e = it.next(); e.edad += dt; if (e.edad > .8) it.remove();
        }
    }
    private static String recorte(String t) { return t.length() > 38 ? t.substring(0, 36) + "…" : t; }
}
