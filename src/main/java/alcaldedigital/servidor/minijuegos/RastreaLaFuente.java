package alcaldedigital.servidor.minijuegos;

import alcaldedigital.compartido.estructuras.difusion.ArbolDifusion;
import java.util.*;

/**
 * Minijuego 2 · «Rastrea la fuente» (buscar). Un rumor se reenvió de persona en persona y forma
 * un {@link ArbolDifusion}: la raíz es una cuenta falsa del Palomo. Se empieza en una hoja
 * («¡me llegó esto!») y, preguntando «¿quién te lo mandó?», se sube de padre en padre hasta la raíz.
 * Solo se ve el nombre del padre: hay que encontrar a esa persona en la plaza (los nombres
 * aparecen al acercarse).
 *
 * <p>Ciudadano: pregunta rápido. Periodista: pregunta más lento, pero además averigua la
 * profundidad del nodo (cuántos reenvíos faltan hasta la fuente). El Palomo silencia a la persona
 * que buscan; hay que espantarlo con la acción.</p>
 */
public final class RastreaLaFuente extends Minijuego {
    public enum Marca { NINGUNA, INICIO, BUSCADO, INTERROGADO, DESCARTADO, FUENTE }
    public record PersonaVista(int id, String nombre, float x, float y, int direccion, int variante, boolean caminando,
                               Marca marca, boolean silenciada, boolean nombreVisible, boolean cerca, String globo,
                               boolean desenmascarada) { }

    public static final double DURACION = 85;
    static final double PREGUNTA_CIUDADANO = .5, PREGUNTA_PERIODISTA = .85;
    private static final String[] NOMBRES = {"Ana", "Beto", "Caro", "Dani", "Eli", "Fede", "Gabi", "Hugo", "Iris", "Juan",
        "Kati", "Leo", "Mia", "Nico", "Olga", "Pepe", "Rita", "Sol", "Tito", "Uma"};
    private static final String[] NO_SE = {"¿Rumor? No he visto nada.", "A mí no me ha llegado eso.", "Ni idea, yo no uso Civitas hoy."};

    private static final class Persona {
        final int id; final String nombre; float x, y, cx, cy, tx, ty; int direccion, variante; boolean caminando;
        Marca marca = Marca.NINGUNA; String globo = ""; double globoT, retarget;
        Persona(int id, String nombre) { this.id = id; this.nombre = nombre; }
    }

    private final List<Persona> personas = new ArrayList<>();
    private final int[] tamanos;
    private final double intervaloPalomo;
    private ArbolDifusion arbol;
    private int ronda, inicio, buscado = -1, interrogatorios, errores;
    private final Map<Integer, Integer> profundidadConocida = new HashMap<>();
    private final List<Integer> camino = new ArrayList<>();
    private double pausaRonda, proximoPalomo, silencio;
    private int silenciada = -1;
    private boolean resuelta;
    private final List<ArbolDifusion> resueltos = new ArrayList<>();

    public RastreaLaFuente(Modo modo, long semilla) {
        super(modo, semilla, 196, 222, 236, 222);
        boolean duo = modo == Modo.DUO;
        tamanos = duo ? new int[]{8, 10, 12} : new int[]{7, 9, 11};
        intervaloPalomo = duo ? 8.5 : 10.5;
        proximoPalomo = 6;
        int n = duo ? 17 : 15;
        List<String> nombres = new ArrayList<>(List.of(NOMBRES));
        Collections.shuffle(nombres, azar);
        for (int i = 0; i < n; i++) {
            Persona p = new Persona(i, nombres.get(i));
            p.variante = (i * 5 + 1) % 12;
            personas.add(p);
        }
        ubicar();
        nuevaRonda();
    }

    private void ubicar() {
        float minimo = 46;
        for (int intento = 0; ; intento++) {
            int puestos = 0;
            for (int t = 0; t < 5000 && puestos < personas.size(); t++) {
                float x = 30 + azar.nextFloat() * 372, y = 90 + azar.nextFloat() * 116;
                if (Math.hypot(x - 216, y - 222) < 40) continue;
                boolean libre = true;
                for (int j = 0; j < puestos && libre; j++) libre = Math.hypot(personas.get(j).cx - x, personas.get(j).cy - y) >= minimo;
                if (libre) { Persona p = personas.get(puestos++); p.x = p.cx = p.tx = x; p.y = p.cy = p.ty = y; }
            }
            if (puestos == personas.size()) return;
            minimo -= 4;
            if (intento > 10) throw new IllegalStateException("No se pudo ubicar la plaza");
        }
    }

    /** Arma el árbol de la ronda: una cadena larga para garantizar profundidad y el resto colgado al azar. */
    private void nuevaRonda() {
        for (Persona p : personas) { p.marca = Marca.NINGUNA; p.globo = ""; }
        profundidadConocida.clear(); camino.clear(); resuelta = false; silenciada = -1; silencio = 0;
        List<Integer> ids = new ArrayList<>();
        for (Persona p : personas) ids.add(p.id);
        Collections.shuffle(ids, azar);
        int tam = tamanos[ronda], cadena = ronda == 0 ? 4 : 5;
        arbol = new ArbolDifusion(ids.get(0));
        for (int i = 1; i < cadena; i++) arbol.agregar(ids.get(i - 1), ids.get(i));
        int hoja = ids.get(cadena - 1);
        for (int i = cadena; i < tam; i++) {
            int padre;
            do padre = ids.get(azar.nextInt(i)); while (padre == hoja || arbol.profundidad(padre) >= cadena - 1);
            arbol.agregar(padre, ids.get(i));
        }
        inicio = arbol.hojaMasProfunda();
        personas.get(inicio).marca = Marca.INICIO;
        decirPersona(personas.get(inicio), "¡Me llegó este rumor! ¿De dónde salió?", 3.5);
        buscado = inicio;
        camino.add(inicio);
        mensaje = "Ronda " + (ronda + 1) + ": pregúntale a " + personas.get(inicio).nombre + " quién le mandó el rumor.";
        historial.add("Ronda " + (ronda + 1) + ": el rumor llegó a " + arbol.tamano() + " personas; empieza en " + personas.get(inicio).nombre + ".");
    }

    // ───────────────────────── Contrato ─────────────────────────
    @Override public Tipo tipo() { return Tipo.FUENTE; }
    @Override public double duracion() { return DURACION; }
    @Override public String objetivo() { return "Encuentren la cuenta que inventó el rumor en 3 rondas."; }
    @Override public String rol(Jugador j) {
        return j == Jugador.CIUDADANO ? "Pregunta rápido «¿quién te lo mandó?»" : "Pregunta y descubre a cuántos reenvíos está la fuente";
    }
    @Override protected float[] limites() { return new float[]{16, 416, 80, 228}; }

    @Override protected void paso(double dt) {
        caminarPersonas(dt);
        if (pausaRonda > 0) {
            pausaRonda -= dt;
            if (pausaRonda <= 0) {
                ronda++;
                if (ronda >= tamanos.length) terminar(Estado.VICTORIA, "Encontraron las 3 fuentes: eran cuentas del Palomo.");
                else nuevaRonda();
            }
            return;
        }
        for (Avatar a : avatares()) {
            if (!a.pulso || a.ocupado()) continue;
            // Espantar al Palomo tiene prioridad si está cerca.
            if (silenciada >= 0 && a.distancia(px, py + 20) < ALCANCE + 10) { espantar(a); continue; }
            Persona p = cercana(a);
            if (p == null) { if (a.humano) mensaje = "Acércate a alguien para preguntarle."; continue; }
            if (otro(a).objetivo == p.id) { mensaje = "Tu compañero ya está hablando con " + p.nombre + "."; continue; }
            if (p.id == silenciada) { decirPersona(p, "Ahora no… el Palomo me está mirando.", 1.6); if (a.humano) mensaje = "¡Espanten al Palomo para que " + p.nombre + " hable!"; continue; }
            iniciarTarea(a, p.id, a.quien == Jugador.CIUDADANO ? PREGUNTA_CIUDADANO : PREGUNTA_PERIODISTA, "preguntar");
            decir(a, "Hola, " + p.nombre + ": ¿quién te mandó ese rumor?");
        }
        palomo(dt);
    }

    @Override protected float[] posicionObjetivo(int id) {
        if (pausaRonda > 0 || id == silenciada) return null;
        Persona p = personas.get(id); return new float[]{p.x, p.y};
    }

    @Override protected void completarTarea(Avatar a, int id, String tipo) {
        Persona p = personas.get(id);
        interrogatorios++;
        if (!arbol.contiene(id)) {
            errores++;
            p.marca = Marca.DESCARTADO;
            decirPersona(p, NO_SE[azar.nextInt(NO_SE.length)], 2.2);
            mensaje = p.nombre + " no recibió el rumor. Busquen a otra persona.";
            return;
        }
        int padre = arbol.padre(id);
        if (a.quien == Jugador.PERIODISTA) profundidadConocida.put(id, arbol.profundidad(id));
        if (padre < 0) {
            // Llegaron a la raíz: la fuente.
            p.marca = Marca.FUENTE; resuelta = true;
            if (!camino.contains(id)) camino.add(id);
            decirPersona(p, "Cuuu… ¡me descubrieron! Era una cuenta del Palomo.", 2.4);
            puntos += 40;
            resueltos.add(arbol);
            efecto("verificacion", p.x, p.y - 14);
            historial.add("Fuente " + (ronda + 1) + ": " + p.nombre + " (cuenta falsa). Altura del árbol " + arbol.altura() + ", " + arbol.tamano() + " personas.");
            mensaje = "¡Fuente encontrada! El rumor nació en la cuenta falsa de " + p.nombre + ".";
            buscado = -1; silenciada = -1; pausaRonda = 2.4;
            return;
        }
        if (p.marca != Marca.INICIO) p.marca = Marca.INTERROGADO;
        Persona sig = personas.get(padre);
        String extra = a.quien == Jugador.PERIODISTA ? " (estoy a " + arbol.profundidad(id) + " reenvíos de la fuente)" : "";
        decirPersona(p, "Me lo mandó " + sig.nombre + "." + extra, 2.6);
        puntos += 8;
        if (id == buscado || camino.contains(id)) {
            if (!camino.contains(padre)) camino.add(padre);
            if (sig.marca == Marca.NINGUNA || sig.marca == Marca.DESCARTADO) sig.marca = Marca.BUSCADO;
            buscado = padre;
            mensaje = p.nombre + " lo recibió de " + sig.nombre + ". ¡Encuentren a " + sig.nombre + "!";
        } else mensaje = p.nombre + " lo recibió de " + sig.nombre + ", pero esa rama no es la que siguen.";
    }

    @Override protected void tareaCancelada(Avatar a, int id, String tipo) {
        if (id == silenciada) mensaje = "El Palomo interrumpió la conversación.";
        else if (pausaRonda <= 0) mensaje = "Te alejaste antes de terminar la pregunta.";
    }

    private void espantar(Avatar a) {
        silenciada = -1; silencio = 0; a.acciones++; puntos += 5;
        palomoDice("¡Cuuu! Está bien, está bien… me voy.");
        decir(a, "¡Fuera de aquí, Palomo!");
        mensaje = nombre(a.quien) + " espantó al Palomo.";
        proximoPalomo = intervaloPalomo;
        efecto("verificacion", px, py + 10);
    }

    private void palomo(double dt) {
        if (silenciada >= 0) {
            Persona p = personas.get(silenciada);
            moverPalomo(p.x + 18, p.y - 22, 150, dt);
            silencio -= dt;
            if (silencio <= 0) { silenciada = -1; palomoDice("Bah, me aburrí."); proximoPalomo = intervaloPalomo; }
            return;
        }
        moverPalomo((float) (216 + 170 * Math.sin(tiempo * .35)), 40, 80, dt);
        proximoPalomo -= dt;
        if (proximoPalomo <= 0 && buscado >= 0 && buscado != inicio) {
            for (Avatar a : avatares()) if (a.objetivo == buscado) cancelarTarea(a);
            silenciada = buscado; silencio = 7;
            Persona p = personas.get(buscado);
            palomoDice("Shhh, " + p.nombre + ". Tú no has visto nada.");
            mensaje = "¡El Palomo está silenciando a " + p.nombre + "! Espántenlo con la acción.";
            historial.add("El Palomo intentó silenciar a " + p.nombre + ".");
        } else if (proximoPalomo <= 0) proximoPalomo = 2;
    }

    private void caminarPersonas(double dt) {
        for (Persona p : personas) {
            p.globoT = Math.max(0, p.globoT - dt);
            boolean quieta = p.id == silenciada || p.id == ciudadano.objetivo || p.id == periodista.objetivo;
            p.retarget -= dt;
            if (p.retarget <= 0) {
                p.tx = p.cx + (azar.nextFloat() - .5f) * 30; p.ty = p.cy + (azar.nextFloat() - .5f) * 16;
                p.retarget = 2 + azar.nextDouble() * 3;
            }
            float dx = p.tx - p.x, dy = p.ty - p.y; double d = Math.hypot(dx, dy);
            p.caminando = !quieta && d > 1;
            if (!p.caminando) continue;
            double v = Math.min(d, 14 * dt);
            p.x += (float) (dx / d * v); p.y += (float) (dy / d * v);
            p.direccion = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 2 : 1) : (dy > 0 ? 0 : 3);
        }
    }

    @Override protected void reposo(double dt) {
        caminarPersonas(dt);
        moverPalomo((float) (216 + 170 * Math.sin(preparacion)), 40, 80, dt);
    }

    @Override protected void comprobarFin() { }
    @Override protected void finDelTiempo() {
        terminar(Estado.DERROTA, "Se acabó el tiempo: encontraron " + resueltos.size() + " de 3 fuentes.");
    }

    // ───────────────────────── Ayudante automático ─────────────────────────
    @Override protected void ia(Avatar a, double dt) {
        a.dx = a.dy = 0;
        if (a.ocupado() || pausaRonda > 0) return;
        if (silenciada >= 0) {
            boolean llego = irHacia(a, px, py + 20, ALCANCE);
            if (reaccionar(a, dt, llego, .5)) a.accion = true;
            return;
        }
        if (buscado < 0 || otro(a).objetivo == buscado) { irHacia(a, 216, 150, 40); return; }
        Persona p = personas.get(buscado);
        boolean llego = irHacia(a, p.x, p.y + 8, ALCANCE - 10);
        if (reaccionar(a, dt, llego && cercana(a) == p, .6)) a.accion = true;
    }

    // ───────────────────────── Consultas ─────────────────────────
    private Persona cercana(Avatar a) {
        Persona mejor = null; double d = ALCANCE;
        for (Persona p : personas) { double dp = a.distancia(p.x, p.y); if (dp < d) { d = dp; mejor = p; } }
        return mejor;
    }
    private void decirPersona(Persona p, String texto, double segundos) { p.globo = texto; p.globoT = segundos; }

    public ArbolDifusion arbol() { return arbol; }
    public int ronda() { return Math.min(ronda, tamanos.length - 1); }
    public int rondasResueltas() { return resueltos.size(); }
    /** Árbol completo de la última ronda resuelta, o null. */
    public ArbolDifusion ultimoArbolResuelto() { return resueltos.isEmpty() ? null : resueltos.get(resueltos.size() - 1); }
    public boolean rondaResuelta() { return resuelta; }
    public int interrogatorios() { return interrogatorios; }
    public int errores() { return errores; }
    public int silenciada() { return silenciada; }
    public String nombreDe(int id) { return personas.get(id).nombre; }
    /** Persona que hay que encontrar ahora (el último padre revelado), o -1. */
    public int buscado() { return buscado; }
    public int inicio() { return inicio; }
    /** Camino descubierto desde la hoja inicial hacia la raíz. */
    public List<Integer> camino() { return List.copyOf(camino); }
    public Integer profundidadConocida(int id) { return profundidadConocida.get(id); }
    public List<PersonaVista> personas() {
        Persona cc = ciudadano.humano ? cercana(ciudadano) : null, cp = periodista.humano ? cercana(periodista) : null;
        List<PersonaVista> salida = new ArrayList<>();
        for (Persona p : personas) {
            boolean visible = p.marca != Marca.NINGUNA && p.marca != Marca.BUSCADO
                || ciudadano.distancia(p.x, p.y) < 56 || periodista.distancia(p.x, p.y) < 56;
            salida.add(new PersonaVista(p.id, p.nombre, p.x, p.y, p.direccion, p.variante, p.caminando, p.marca,
                p.id == silenciada, visible, p == cc || p == cp, p.globoT > 0 ? p.globo : "", p.marca == Marca.FUENTE));
        }
        return salida;
    }
}
