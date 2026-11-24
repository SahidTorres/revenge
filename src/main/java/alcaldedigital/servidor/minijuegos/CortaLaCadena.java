package alcaldedigital.servidor.minijuegos;

import alcaldedigital.compartido.estructuras.grafos.GrafoRed;
import java.util.*;

/**
 * Minijuego 1 · «Corta la cadena» (detener). Un rumor se propaga por la red de contactos de
 * Civitas, modelada como un {@link GrafoRed}. Cada «ola» el rumor avanza exactamente un nivel del
 * BFS: contagia a todas las cuentas sanas a distancia 1 de alguna cuenta contagiada. Las cuentas
 * blindadas no se contagian y cortan el recorrido.
 *
 * <p>Ciudadano: blinda cuentas sanas rápido (mensaje de civismo). Periodista: desmiente rápido las
 * contagiadas. Los dos pueden hacer ambas cosas, pero cada uno es lento en la del otro.</p>
 */
public final class CortaLaCadena extends Minijuego {
    public enum EstadoNodo { SANO, BLINDADO, CONTAGIADO }
    public record NodoVista(int id, float x, float y, EstadoNodo estado, int distancia, double blindaje,
                            boolean objetivo, boolean cerca) { }

    public static final double DURACION = 60;
    static final double BLINDAR_CIUDADANO = .45, BLINDAR_PERIODISTA = .95,
        DESMENTIR_CIUDADANO = 1.35, DESMENTIR_PERIODISTA = .6;

    private final GrafoRed red = new GrafoRed();
    private final float[] nx, ny;
    private final EstadoNodo[] estados;
    private final double[] blindaje;
    private final int limite;
    private final double intervaloOla, duracionBlindaje, intervaloPalomo;
    private double proximaOla = 4.5, proximoPalomo;
    private int olas, contagiosTotales, objetivoPalomo = -1;
    private double tPalomo;
    private int[] distancias;

    public CortaLaCadena(Modo modo, long semilla) {
        super(modo, semilla, 190, 212, 242, 212);
        boolean duo = modo == Modo.DUO;
        int n = duo ? 16 : 14;
        limite = (n + 1) / 2;
        intervaloOla = duo ? 2.8 : 3.2;
        duracionBlindaje = duo ? 13 : 15;
        intervaloPalomo = duo ? 10.5 : 12.5;
        proximoPalomo = duo ? 10 : 12;
        nx = new float[n]; ny = new float[n];
        estados = new EstadoNodo[n]; blindaje = new double[n];
        ubicarNodos(n);
        conectar(n);
        Arrays.fill(estados, EstadoNodo.SANO);
        // El rumor nace en la cuenta más alejada de los dos personajes.
        int origen = 0; double lejos = -1;
        for (int i = 0; i < n; i++) { double d = Math.hypot(nx[i] - 216, ny[i] - 212); if (d > lejos) { lejos = d; origen = i; } }
        estados[origen] = EstadoNodo.CONTAGIADO; contagiosTotales = 1;
        recalcular();
        historial.add("Inicio: el rumor nace en la cuenta #" + (origen + 1) + ".");
        mensaje = "Un rumor empieza a moverse por la red de Civitas.";
    }

    private void ubicarNodos(int n) {
        float minimo = 54;
        for (int intento = 0; ; intento++) {
            int puestos = 0;
            for (int t = 0; t < 4000 && puestos < n; t++) {
                float x = 34 + azar.nextFloat() * 364, y = 84 + azar.nextFloat() * 124;
                boolean libre = true;
                for (int j = 0; j < puestos && libre; j++) libre = Math.hypot(nx[j] - x, ny[j] - y) >= minimo;
                if (libre) { nx[puestos] = x; ny[puestos] = y; puestos++; }
            }
            if (puestos == n) return;
            minimo -= 4;
            if (intento > 10) throw new IllegalStateException("No se pudo ubicar la red");
        }
    }

    /** Cada cuenta se conecta con sus dos más cercanas y luego se unen las componentes sueltas. */
    private void conectar(int n) {
        for (int i = 0; i < n; i++) red.agregarVertice();
        for (int i = 0; i < n; i++) {
            Integer[] orden = new Integer[n];
            for (int j = 0; j < n; j++) orden[j] = j;
            final int origen = i;
            Arrays.sort(orden, Comparator.comparingDouble(j -> dist(origen, j)));
            for (int k = 1; k <= 2; k++) red.agregarArista(i, orden[k]);
        }
        List<List<Integer>> comp;
        while ((comp = red.componentes()).size() > 1) {
            int mejorA = -1, mejorB = -1; double mejor = Double.MAX_VALUE;
            for (int a : comp.get(0)) for (int k = 1; k < comp.size(); k++) for (int b : comp.get(k))
                if (dist(a, b) < mejor) { mejor = dist(a, b); mejorA = a; mejorB = b; }
            red.agregarArista(mejorA, mejorB);
        }
    }
    private double dist(int a, int b) { return Math.hypot(nx[a] - nx[b], ny[a] - ny[b]); }

    // ───────────────────────── Contrato ─────────────────────────
    @Override public Tipo tipo() { return Tipo.CADENA; }
    @Override public double duracion() { return DURACION; }
    @Override public String objetivo() { return "Aguanten " + (int) DURACION + " s sin que el rumor contagie " + limite + " cuentas."; }
    @Override public String rol(Jugador j) {
        return j == Jugador.CIUDADANO ? "Blinda cuentas sanas (rápido) · desmiente (lento)" : "Desmiente cuentas contagiadas (rápido) · blinda (lento)";
    }

    @Override protected void paso(double dt) {
        // Acciones: blindar una cuenta sana o desmentir una contagiada.
        for (Avatar a : avatares()) {
            if (!a.pulso || a.ocupado()) continue;
            int nodo = cercano(a);
            if (nodo < 0) { if (a.humano) mensaje = "Acércate a una cuenta (círculo) para actuar."; continue; }
            if (ocupadoPorOtro(a, nodo)) { mensaje = "Tu compañero ya está en esa cuenta."; continue; }
            boolean ciu = a.quien == Jugador.CIUDADANO;
            switch (estados[nodo]) {
                case SANO -> { iniciarTarea(a, nodo, ciu ? BLINDAR_CIUDADANO : BLINDAR_PERIODISTA, "blindar");
                    decir(a, ciu ? "¡Verifica antes de compartir!" : "Te comparto la fuente oficial."); }
                case CONTAGIADO -> { iniciarTarea(a, nodo, ciu ? DESMENTIR_CIUDADANO : DESMENTIR_PERIODISTA, "desmentir");
                    decir(a, ciu ? "Eso no es cierto, mira…" : "Desmentido: aquí están los datos."); }
                case BLINDADO -> { if (a.humano) mensaje = "La cuenta #" + (nodo + 1) + " ya está blindada."; }
            }
        }
        for (int i = 0; i < estados.length; i++) if (estados[i] == EstadoNodo.BLINDADO) {
            blindaje[i] -= dt;
            if (blindaje[i] <= 0) { estados[i] = EstadoNodo.SANO; mensaje = "El blindaje de #" + (i + 1) + " se desvaneció."; }
        }
        proximaOla -= dt;
        if (proximaOla <= 0) { ola(); proximaOla = intervaloOla; }
        palomo(dt);
        recalcular();
    }

    /** Una ola = un nivel de BFS desde todas las cuentas contagiadas. */
    private void ola() {
        recalcular();
        List<Integer> nuevos = new ArrayList<>();
        for (int i = 0; i < estados.length; i++) if (estados[i] == EstadoNodo.SANO && distancias[i] == 1) nuevos.add(i);
        olas++;
        if (nuevos.isEmpty()) { mensaje = "Ola " + olas + ": el rumor no encontró por dónde pasar."; return; }
        for (int i : nuevos) { contagiar(i); efecto("rumor", nx[i], ny[i] - 10); }
        StringBuilder sb = new StringBuilder();
        for (int i : nuevos) sb.append(sb.length() == 0 ? "#" : ", #").append(i + 1);
        historial.add("Ola " + olas + ": el rumor llegó a " + sb + ".");
        mensaje = "Ola " + olas + ": el rumor saltó a " + sb + ".";
    }

    private void contagiar(int i) {
        estados[i] = EstadoNodo.CONTAGIADO; blindaje[i] = 0; contagiosTotales++;
        for (Avatar a : avatares()) if (a.objetivo == i && a.tareaTipo.equals("blindar")) {
            cancelarTarea(a); if (a.humano) mensaje = "¡Tarde! La cuenta #" + (i + 1) + " se contagió mientras la blindabas.";
        }
    }

    private void palomo(double dt) {
        if (objetivoPalomo < 0) {
            moverPalomo((float) (216 + 160 * Math.sin(tiempo * .4)), 40, 70, dt);
            proximoPalomo -= dt;
            if (proximoPalomo <= 0) {
                // Siembra un rumor nuevo lejos de los personajes.
                int mejor = -1; double lejos = -1;
                for (int i = 0; i < estados.length; i++) {
                    if (estados[i] == EstadoNodo.CONTAGIADO) continue;
                    double d = Math.min(ciudadano.distancia(nx[i], ny[i]), periodista.distancia(nx[i], ny[i]))
                        + (estados[i] == EstadoNodo.SANO ? 60 : 0) + azar.nextDouble() * 40;
                    if (d > lejos) { lejos = d; mejor = i; }
                }
                if (mejor < 0) { proximoPalomo = 3; return; }
                objetivoPalomo = mejor; tPalomo = 0;
                palomoDice(azar.nextBoolean() ? "¡Un chisme fresquito para la red!" : "¿Y si les cuento algo… inventado?");
                historial.add("El Palomo baja a sembrar un rumor en #" + (mejor + 1) + ".");
            }
            return;
        }
        tPalomo += dt;
        moverPalomo(nx[objetivoPalomo], ny[objetivoPalomo] - 26, 140, dt);
        if (tPalomo >= 1.6) {
            int i = objetivoPalomo;
            if (estados[i] != EstadoNodo.CONTAGIADO) {
                boolean rompe = estados[i] == EstadoNodo.BLINDADO;
                contagiar(i); efecto("rumor", nx[i], ny[i] - 10);
                mensaje = rompe ? "¡El Palomo rompió el blindaje de #" + (i + 1) + "!" : "El Palomo sembró un rumor en #" + (i + 1) + ".";
            }
            objetivoPalomo = -1;
            proximoPalomo = intervaloPalomo * (.85 + .3 * azar.nextDouble());
        }
    }

    @Override protected float[] posicionObjetivo(int id) { return new float[]{nx[id], ny[id]}; }

    @Override protected void completarTarea(Avatar a, int id, String tipo) {
        if (tipo.equals("blindar") && estados[id] == EstadoNodo.SANO) {
            estados[id] = EstadoNodo.BLINDADO; blindaje[id] = duracionBlindaje; puntos += 10;
            mensaje = nombre(a.quien) + " blindó la cuenta #" + (id + 1) + ".";
            efecto("verificacion", nx[id], ny[id] - 10);
        } else if (tipo.equals("desmentir") && estados[id] == EstadoNodo.CONTAGIADO) {
            estados[id] = EstadoNodo.BLINDADO; blindaje[id] = duracionBlindaje; puntos += 15;
            mensaje = nombre(a.quien) + " desmintió el rumor en #" + (id + 1) + ".";
            historial.add(nombre(a.quien) + " desmintió #" + (id + 1) + ".");
            efecto("verificacion", nx[id], ny[id] - 10);
        }
        recalcular();
    }

    @Override protected void comprobarFin() {
        if (contagiadas() >= limite) terminar(Estado.DERROTA, "El rumor contagió " + contagiadas() + " de " + estados.length + " cuentas.");
    }
    @Override protected void finDelTiempo() {
        puntos += sanas() * 5;
        terminar(Estado.VICTORIA, "Aguantaron " + (int) DURACION + " s: el rumor quedó contenido en " + contagiadas() + " cuentas.");
    }

    @Override protected void reposo(double dt) { moverPalomo((float) (216 + 160 * Math.sin(preparacion)), 40, 70, dt); }

    // ───────────────────────── Ayudante automático ─────────────────────────
    @Override protected void ia(Avatar a, double dt) {
        a.dx = a.dy = 0;
        if (a.ocupado() || a.aturdido > 0) return;
        boolean ciu = a.quien == Jugador.CIUDADANO;
        int meta = -1; double mejor = Double.MAX_VALUE;
        for (int i = 0; i < estados.length; i++) {
            if (ocupadoPorOtro(a, i) || estados[i] == EstadoNodo.BLINDADO) continue;
            double d = a.distancia(nx[i], ny[i]);
            boolean contagiada = estados[i] == EstadoNodo.CONTAGIADO;
            boolean frontera = estados[i] == EstadoNodo.SANO && distancias[i] == 1;
            if (!contagiada && !frontera) continue;
            // Cada uno prefiere su especialidad.
            double costo = d + ((ciu ? contagiada : frontera) ? 150 : 0);
            if (costo < mejor) { mejor = costo; meta = i; }
        }
        if (meta < 0) { irHacia(a, 216, 160, 30); return; }
        boolean llego = irHacia(a, nx[meta], ny[meta], ALCANCE - 8);
        if (reaccionar(a, dt, llego && cercano(a) == meta, .45)) a.accion = true;
    }

    // ───────────────────────── Consultas ─────────────────────────
    private void recalcular() {
        List<Integer> origen = new ArrayList<>();
        Set<Integer> bloqueados = new HashSet<>();
        for (int i = 0; i < estados.length; i++) {
            if (estados[i] == EstadoNodo.CONTAGIADO) origen.add(i);
            if (estados[i] == EstadoNodo.BLINDADO) bloqueados.add(i);
        }
        distancias = red.distanciasBFS(origen, bloqueados);
    }
    private int cercano(Avatar a) {
        int mejor = -1; double d = ALCANCE;
        for (int i = 0; i < estados.length; i++) { double di = a.distancia(nx[i], ny[i]); if (di < d) { d = di; mejor = i; } }
        return mejor;
    }
    private boolean ocupadoPorOtro(Avatar a, int nodo) { return otro(a).objetivo == nodo; }

    public GrafoRed red() { return red; }
    public int limite() { return limite; }
    public int olas() { return olas; }
    public int contagiadas() { int c = 0; for (EstadoNodo e : estados) if (e == EstadoNodo.CONTAGIADO) c++; return c; }
    public int blindadas() { int c = 0; for (EstadoNodo e : estados) if (e == EstadoNodo.BLINDADO) c++; return c; }
    public int sanas() { return estados.length - contagiadas(); }
    public int contagiosTotales() { return contagiosTotales; }
    public double proximaOla() { return Math.max(0, proximaOla); }
    public double intervaloOla() { return intervaloOla; }
    public EstadoNodo estadoNodo(int i) { return estados[i]; }
    public int[] distancias() { return distancias.clone(); }
    /** Frontera del BFS: las cuentas que caerán en la próxima ola si nadie las blinda. */
    public List<Integer> frontera() {
        List<Integer> f = new ArrayList<>();
        for (int i = 0; i < estados.length; i++) if (estados[i] == EstadoNodo.SANO && distancias[i] == 1) f.add(i);
        return f;
    }
    public List<NodoVista> nodos() {
        int cc = cercano(ciudadano), cp = cercano(periodista);
        List<NodoVista> salida = new ArrayList<>();
        for (int i = 0; i < estados.length; i++)
            salida.add(new NodoVista(i, nx[i], ny[i], estados[i], distancias[i],
                estados[i] == EstadoNodo.BLINDADO ? blindaje[i] / duracionBlindaje : 0,
                i == objetivoPalomo, (ciudadano.humano && cc == i) || (periodista.humano && cp == i)));
        return salida;
    }
    /** Fuerza el contagio de una cuenta (solo pruebas). */
    void contagiarParaPrueba(int i) { contagiar(i); recalcular(); }
}
