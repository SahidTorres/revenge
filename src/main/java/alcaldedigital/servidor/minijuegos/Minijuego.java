package alcaldedigital.servidor.minijuegos;

import java.util.*;

/**
 * Base común de los cuatro minijuegos de la misión 5. Igual que {@code Mision5}, no dibuja nada:
 * es la autoridad del reloj, de los dos personajes (Ciudadano y Periodista) y del Palomo.
 *
 * <p>Cada cuadro el cliente entrega la entrada de los jugadores humanos con
 * {@link #entrada(Jugador, float, float, boolean)} y llama a {@link #actualizar(double)}. Si el modo
 * es de un jugador, el otro personaje lo controla un ayudante automático ({@link #ia}), con la
 * misma interfaz de entrada que un humano: dirección y botón de acción.</p>
 */
public abstract class Minijuego {
    public enum Tipo {
        CADENA("Corta la cadena", "Grafo · BFS"),
        FUENTE("Rastrea la fuente", "Árbol n-ario · camino a la raíz"),
        DUELO("Duelo contra el Palomo", "AVL · búsqueda del piso"),
        BOTS("Caza de bots", "Tabla hash · encadenamiento");
        public final String titulo, estructura;
        Tipo(String titulo, String estructura) { this.titulo = titulo; this.estructura = estructura; }
    }
    public enum Modo {
        SOLO_CIUDADANO("Un jugador · Ciudadano"), SOLO_PERIODISTA("Un jugador · Periodista"), DUO("Dos jugadores");
        public final String texto;
        Modo(String texto) { this.texto = texto; }
        public boolean humano(Jugador j) {
            return this == DUO || (this == SOLO_CIUDADANO) == (j == Jugador.CIUDADANO);
        }
    }
    public enum Jugador { CIUDADANO, PERIODISTA }
    public enum Estado { PREPARACION, EN_CURSO, VICTORIA, DERROTA }

    public static final double PREPARACION = 3;
    /** Mundo lógico de 432×242 px que el cliente dibuja a escala 2, como la misión 5. */
    public static final int ANCHO = 432, ALTO = 242;
    /** Velocidad al caminar (px/s) y factor del ayudante automático. */
    public static final float VELOCIDAD = 76, FACTOR_IA = .78f;
    /** Ritmo del ayudante automático: segundos de trabajo antes de cada pausa. */
    public static final double IA_TRABAJO = 3.2;
    /** Distancia para interactuar con algo. */
    public static final float ALCANCE = 24;

    public record AvatarVista(Jugador quien, float x, float y, int direccion, boolean caminando, boolean humano,
                              double progreso, boolean ocupado, boolean aturdido, boolean parpadeo, String globo, int acciones) { }
    public record PalomoVista(float x, float y, boolean volando, boolean mirarIzquierda, String globo) { }
    public record EfectoVista(String tipo, float x, float y, double edad) { }

    /** Estado interno de un personaje. Los minijuegos leen {@code pulso}, {@code sostenida} y {@code soltada}. */
    protected static final class Avatar {
        final Jugador quien; final boolean humano;
        float x, y; int direccion; boolean caminando;
        float dx, dy; boolean accion, accionAntes;
        boolean pulso, soltada;
        double aturdido, invulnerable, globoT, reaccion, ciclo;
        String globo = "";
        int acciones;
        // Tarea en curso: interactuar durante un tiempo con un objetivo cercano sin moverse.
        int objetivo = -1; double tareaDuracion, tareaProgreso; String tareaTipo = "";
        // Libre para cada minijuego (p. ej. la carga del disparo en el duelo).
        double valor;
        Avatar(Jugador quien, boolean humano, float x, float y) { this.quien = quien; this.humano = humano; this.x = x; this.y = y; }
        boolean ocupado() { return objetivo >= 0; }
        boolean sostenida() { return accion; }
        float distancia(float ox, float oy) { return (float) Math.hypot(ox - x, oy - y); }
    }
    private static final class Efecto { String tipo; float x, y; double edad; }

    protected final Modo modo;
    protected final long semilla;
    protected final Random azar;
    protected final Avatar ciudadano, periodista;
    protected Estado estado = Estado.PREPARACION;
    protected double preparacion = PREPARACION, tiempo;
    protected int puntos;
    protected String mensaje = "Prepárense…", motivo = "";
    protected final List<String> historial = new ArrayList<>();
    private final List<Efecto> efectos = new ArrayList<>();
    // Palomo: cada minijuego decide qué hace; aquí solo su posición y su globo.
    protected float px = 216, py = 40;
    protected boolean palomoVolando = true, palomoIzquierda;
    protected String globoPalomo = "";
    protected double globoPalomoT;

    protected Minijuego(Modo modo, long semilla, float xCiudadano, float yCiudadano, float xPeriodista, float yPeriodista) {
        this.modo = Objects.requireNonNull(modo);
        this.semilla = semilla;
        this.azar = new Random(semilla * 7919 + modo.ordinal() * 31L + getClass().getSimpleName().hashCode());
        ciudadano = new Avatar(Jugador.CIUDADANO, modo.humano(Jugador.CIUDADANO), xCiudadano, yCiudadano);
        periodista = new Avatar(Jugador.PERIODISTA, modo.humano(Jugador.PERIODISTA), xPeriodista, yPeriodista);
        ciudadano.direccion = periodista.direccion = 3;
    }

    // ───────────────────────── Contrato de cada minijuego ─────────────────────────
    public abstract Tipo tipo();
    /** Segundos de juego, sin contar la cuenta atrás. */
    public abstract double duracion();
    /** Una línea con la meta, visible antes de empezar y durante el juego. */
    public abstract String objetivo();
    /** Qué hace cada personaje en este minijuego. */
    public abstract String rol(Jugador j);
    protected abstract void paso(double dt);
    protected abstract void ia(Avatar a, double dt);
    /** Se llama cada cuadro tras {@link #paso}; puede terminar la partida. */
    protected abstract void comprobarFin();
    /** Se llama cuando se acaba el reloj sin haber terminado antes. */
    protected abstract void finDelTiempo();
    /** Área en la que pueden caminar los personajes: {xMin, xMax, yMin, yMax}. */
    protected float[] limites() { return new float[]{16, 416, 72, 228}; }
    /** Movimiento ambiental durante la cuenta atrás. */
    protected void reposo(double dt) { }
    /** Posición de un objetivo de tarea, o null si ya no existe. */
    protected float[] posicionObjetivo(int id) { return null; }
    protected void completarTarea(Avatar a, int id, String tipo) { }
    protected void tareaCancelada(Avatar a, int id, String tipo) { mensaje = "Te alejaste antes de terminar."; }
    /** Segundos que descansa el ayudante automático tras cada racha de trabajo. */
    protected double iaPausa() { return 1.8; }
    /** Multiplicador de velocidad (p. ej. más lento mientras carga). */
    protected float velocidad(Avatar a) { return 1; }

    // ───────────────────────── Entrada y simulación ─────────────────────────
    /** Entrada de un jugador humano para el próximo cuadro. Se ignora para el personaje automático. */
    public void entrada(Jugador j, float dx, float dy, boolean accion) {
        Avatar a = avatar(j);
        if (!a.humano || !Float.isFinite(dx) || !Float.isFinite(dy)) return;
        float largo = (float) Math.hypot(dx, dy);
        if (largo > 1) { dx /= largo; dy /= largo; }
        a.dx = dx; a.dy = dy; a.accion = accion;
    }

    public final void actualizar(double dt) {
        if (!Double.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Tiempo inválido");
        dt = Math.min(.1, dt);
        avanzarEfectos(dt);
        globoPalomoT = Math.max(0, globoPalomoT - dt);
        if (terminada()) return;
        if (estado == Estado.PREPARACION) {
            preparacion -= dt;
            reposo(dt);
            for (Avatar a : avatares()) { a.accionAntes = a.accion; a.dx = a.dy = 0; a.caminando = false; }
            if (preparacion <= 0) { estado = Estado.EN_CURSO; mensaje = "¡Ya! " + objetivo(); }
            return;
        }
        tiempo += dt;
        for (Avatar a : avatares()) if (!a.humano) {
            // El ayudante trabaja a ratos: así un jugador inactivo no gana solo con la ayuda automática.
            a.ciclo += dt;
            double ciclo = IA_TRABAJO + iaPausa();
            if (a.ocupado() || a.ciclo % ciclo < IA_TRABAJO) ia(a, dt);
            else { a.dx = a.dy = 0; a.reaccion = 0; }
        }
        for (Avatar a : avatares()) mover(a, dt);
        paso(dt);
        avanzarTareas(dt);
        for (Avatar a : avatares()) {
            a.accionAntes = a.accion;
            if (!a.humano) a.accion = false;
            a.aturdido = Math.max(0, a.aturdido - dt);
            a.invulnerable = Math.max(0, a.invulnerable - dt);
            a.globoT = Math.max(0, a.globoT - dt);
        }
        if (!terminada()) comprobarFin();
        if (!terminada() && tiempo >= duracion()) finDelTiempo();
    }

    private void mover(Avatar a, double dt) {
        a.pulso = a.accion && !a.accionAntes;
        a.soltada = !a.accion && a.accionAntes;
        if (a.aturdido > 0) { a.pulso = false; a.caminando = false; return; }
        boolean quiere = a.dx != 0 || a.dy != 0;
        a.caminando = quiere && !a.ocupado();
        if (!a.caminando) return;
        float v = VELOCIDAD * velocidad(a) * (a.humano ? 1 : FACTOR_IA) * (float) dt;
        float[] l = limites();
        a.x = Math.max(l[0], Math.min(l[1], a.x + a.dx * v));
        a.y = Math.max(l[2], Math.min(l[3], a.y + a.dy * v));
        a.direccion = Math.abs(a.dx) > Math.abs(a.dy) ? (a.dx > 0 ? 2 : 1) : (a.dy > 0 ? 0 : 3);
    }

    // ───────────────────────── Tareas: interactuar quieto durante un tiempo ─────────────────────────
    protected boolean iniciarTarea(Avatar a, int objetivo, double duracion, String tipo) {
        if (a.ocupado() || objetivo < 0) return false;
        a.objetivo = objetivo; a.tareaDuracion = duracion; a.tareaProgreso = 0; a.tareaTipo = tipo;
        return true;
    }
    protected void cancelarTarea(Avatar a) { a.objetivo = -1; a.tareaProgreso = 0; }
    private void avanzarTareas(double dt) {
        for (Avatar a : avatares()) {
            if (!a.ocupado() || terminada()) continue;
            int id = a.objetivo; String tipo = a.tareaTipo;
            float[] p = posicionObjetivo(id);
            if (p == null || a.distancia(p[0], p[1]) > ALCANCE + 14 || a.aturdido > 0) {
                cancelarTarea(a); tareaCancelada(a, id, tipo); continue;
            }
            a.direccion = Math.abs(p[0] - a.x) > Math.abs(p[1] - a.y) ? (p[0] > a.x ? 2 : 1) : (p[1] > a.y ? 0 : 3);
            a.tareaProgreso += dt;
            if (a.tareaProgreso >= a.tareaDuracion) { cancelarTarea(a); a.acciones++; completarTarea(a, id, tipo); }
        }
    }

    // ───────────────────────── Ayudas para el personaje automático ─────────────────────────
    /** Orienta al avatar hacia (tx, ty). Devuelve true si ya llegó. */
    protected boolean irHacia(Avatar a, float tx, float ty, float tolerancia) {
        float dx = tx - a.x, dy = ty - a.y;
        double d = Math.hypot(dx, dy);
        if (d <= tolerancia) { a.dx = a.dy = 0; return true; }
        a.dx = (float) (dx / d); a.dy = (float) (dy / d);
        return false;
    }
    /** Simula el tiempo de reacción: devuelve true una vez cuando lleva {@code espera} s listo. */
    protected boolean reaccionar(Avatar a, double dt, boolean listo, double espera) {
        if (!listo) { a.reaccion = 0; return false; }
        a.reaccion += dt;
        if (a.reaccion < espera) return false;
        a.reaccion = 0;
        return true;
    }

    // ───────────────────────── Utilidades ─────────────────────────
    protected Avatar avatar(Jugador j) { return j == Jugador.CIUDADANO ? ciudadano : periodista; }
    protected Avatar otro(Avatar a) { return a == ciudadano ? periodista : ciudadano; }
    protected List<Avatar> avatares() { return List.of(ciudadano, periodista); }
    protected void decir(Avatar a, String texto) { a.globo = texto; a.globoT = 2.0; }
    protected void palomoDice(String texto) { globoPalomo = texto; globoPalomoT = 2.6; }
    protected void moverPalomo(float tx, float ty, float velocidad, double dt) {
        float dx = tx - px, dy = ty - py;
        double d = Math.hypot(dx, dy);
        if (Math.abs(dx) > .5) palomoIzquierda = dx < 0;
        if (d < .5) return;
        double paso = Math.min(d, velocidad * dt);
        px += (float) (dx / d * paso); py += (float) (dy / d * paso);
    }
    protected void efecto(String tipo, float x, float y) {
        Efecto e = new Efecto(); e.tipo = tipo; e.x = x; e.y = y; efectos.add(e);
    }
    private void avanzarEfectos(double dt) {
        for (Iterator<Efecto> it = efectos.iterator(); it.hasNext(); ) {
            Efecto e = it.next(); e.edad += dt; if (e.edad > .8) it.remove();
        }
    }
    protected void terminar(Estado resultado, String causa) {
        estado = resultado; motivo = causa;
        for (Avatar a : avatares()) cancelarTarea(a);
        if (resultado == Estado.VICTORIA) {
            puntos += (int) restante() * 2;
            mensaje = "¡Lo lograron! " + causa;
        } else mensaje = causa;
        historial.add((resultado == Estado.VICTORIA ? "Victoria: " : "Derrota: ") + causa);
    }
    protected static String nombre(Jugador j) { return j == Jugador.CIUDADANO ? "Ciudadano" : "Periodista"; }

    // ───────────────────────── Consultas para el cliente ─────────────────────────
    public Modo modo() { return modo; }
    public long semilla() { return semilla; }
    public Estado estado() { return estado; }
    public boolean terminada() { return estado == Estado.VICTORIA || estado == Estado.DERROTA; }
    public int cuentaAtras() { return (int) Math.ceil(Math.max(0, preparacion)); }
    public double restante() { return Math.max(0, duracion() - tiempo); }
    public double transcurrido() { return tiempo; }
    public int puntos() { return puntos; }
    public String mensaje() { return mensaje; }
    public String motivo() { return motivo; }
    public List<String> historial() { return List.copyOf(historial); }
    public boolean humano(Jugador j) { return avatar(j).humano; }
    public AvatarVista avatarVista(Jugador j) {
        Avatar a = avatar(j);
        double progreso = a.ocupado() ? Math.min(1, a.tareaProgreso / a.tareaDuracion) : 0;
        return new AvatarVista(j, a.x, a.y, a.direccion, a.caminando, a.humano, progreso, a.ocupado(),
            a.aturdido > 0, a.invulnerable > 0, a.globoT > 0 ? a.globo : "", a.acciones);
    }
    public PalomoVista palomo() { return new PalomoVista(px, py, palomoVolando, palomoIzquierda, globoPalomoT > 0 ? globoPalomo : ""); }
    public List<EfectoVista> efectos() {
        List<EfectoVista> salida = new ArrayList<>();
        for (Efecto e : efectos) salida.add(new EfectoVista(e.tipo, e.x, e.y, e.edad));
        return salida;
    }

    /** Fábrica usada por el cliente y las pruebas. */
    public static Minijuego crear(Tipo tipo, Modo modo, long semilla) {
        return switch (tipo) {
            case CADENA -> new CortaLaCadena(modo, semilla);
            case FUENTE -> new RastreaLaFuente(modo, semilla);
            case DUELO -> new DueloPalomo(modo, semilla);
            case BOTS -> new CazaDeBots(modo, semilla);
        };
    }
}
