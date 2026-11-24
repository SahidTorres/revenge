package alcaldedigital.servidor.minijuegos;

import alcaldedigital.compartido.estructuras.hash.TablaHash;
import java.util.*;

/**
 * Minijuego 4 · «Caza de bots» (buscar y detener). Entre la gente de la plaza hay cuentas falsas
 * del Palomo. Cada cuenta lleva una huella visible (ícono + color). Las huellas de las redes de bots
 * conocidas están en una {@link TablaHash}: reportar una cuenta consulta la tabla y, si su huella
 * está registrada, es un bot y se elimina. Reportar a alguien inocente sube la desinformación.
 *
 * <p>Ciudadano: reporta a una cuenta, muy rápido. Periodista: escaneo de zona, más lento, que
 * revisa en la tabla a todas las cuentas cercanas y solo reporta bots (nunca se equivoca).
 * Mientras tanto los bots publican cadenas que suben la desinformación, y el Palomo crea redes
 * nuevas que se registran en la tabla.</p>
 */
public final class CazaDeBots extends Minijuego {
    public static final String[] ICONOS = {"celular", "cámara", "megáfono", "compartir", "alerta", "wifi"};
    public static final String[] COLORES = {"rojo", "azul", "verde", "amarillo"};
    public static final int CAPACIDAD = 11;
    public enum Conducta { PASEA, SALE }
    public record Huella(int icono, int color) {
        public String clave() { return ICONOS[icono] + "·" + COLORES[color]; }
    }
    public record CuentaVista(int id, float x, float y, int direccion, int variante, boolean caminando, Huella huella,
                              boolean bot, boolean reportada, boolean revisada, boolean cerca, String globo, boolean publicando) { }
    /** Familia de bots registrada en la tabla: nombre de la red y momento en que apareció. */
    public record Familia(String red, double desde) { }

    public static final double DURACION = 65;
    static final int ESCANEO = Integer.MAX_VALUE;
    static final double REPORTE_CIUDADANO = .35, ESCANEO_PERIODISTA = .8, RADIO_ESCANEO = 42, ENFRIAMIENTO_ESCANEO = 2.8;
    private static final String[] CADENAS = {"¡¡COMPARTE YA!! Civitas se cae hoy.", "REENVÍA a 10 o pierdes tu cuenta.",
        "Lo que la alcaldía no quiere que sepas…", "¡URGENTE! ¡Pásalo!", "Fuente: confía en mí, bro."};
    private static final String[] NORMALES = {"Qué rico este café.", "¿Alguien vio a mi perro?", "Hoy hay feria en la plaza.",
        "Voy tarde al trabajo :(", "Lindo día para caminar.", "¿A qué hora abre la biblioteca?"};
    private static final String[] REDES = {"Red Cuuu", "Red Plumas", "Red Migaja", "Red Alpiste", "Red Pico"};

    private static final class Cuenta {
        final int id; float x, y, tx, ty; int direccion, variante; boolean caminando; Huella huella; boolean bot;
        Conducta conducta = Conducta.PASEA; double retarget, publicar, globoT, revisada; String globo = ""; boolean reportada;
        Cuenta(int id) { this.id = id; }
    }

    private final TablaHash<Familia> tabla = new TablaHash<>(CAPACIDAD);
    private final List<Cuenta> cuentas = new ArrayList<>();
    private final int meta, maxFamilias;
    private final double intervaloFamilia, subidaPublicacion;
    private double desinformacion = 10, proximaFamilia, enfriamientoEscaneo;
    private int reportados, falsos, publicaciones, siguienteId, familias;
    private final List<Integer> pendientes = new ArrayList<>();
    private double proximaEntrada;
    private Cuenta objetivoPalomo;
    private double tPalomo;
    private String ultimaConsulta = "";
    private TablaHash.Busqueda ultimaBusqueda;

    public CazaDeBots(Modo modo, long semilla) {
        super(modo, semilla, 196, 220, 236, 220);
        boolean duo = modo == Modo.DUO;
        meta = duo ? 10 : 9;
        maxFamilias = 4;
        intervaloFamilia = duo ? 12 : 14;
        subidaPublicacion = duo ? 6.5 : 6;
        proximaFamilia = intervaloFamilia;
        int total = duo ? 18 : 16, botsIniciales = duo ? 4 : 3;
        Huella primera = huellaLibre();
        registrar(primera);
        for (int i = 0; i < total; i++) {
            Cuenta c = nuevaCuenta(i < botsIniciales ? primera : null);
            c.x = 28 + azar.nextFloat() * 376; c.y = 88 + azar.nextFloat() * 130;
            if (Math.hypot(c.x - 216, c.y - 220) < 50) c.y -= 50;
            c.publicar = c.bot ? 3 + azar.nextDouble() * 6 : 4 + azar.nextDouble() * 10;
            destino(c);
        }
        mensaje = "La " + tabla.obtener(primera.clave()).red() + " anda suelta. Su huella está en la tabla.";
    }

    private Cuenta nuevaCuenta(Huella deBot) {
        Cuenta c = new Cuenta(siguienteId++);
        c.variante = (c.id * 7 + 3) % 12;
        c.bot = deBot != null;
        c.huella = c.bot ? deBot : huellaInocente();
        cuentas.add(c);
        return c;
    }

    /** Huella que no está registrada como bot (puede compartir ícono o color con una de bot). */
    private Huella huellaInocente() {
        Huella h;
        do h = new Huella(azar.nextInt(ICONOS.length), azar.nextInt(COLORES.length)); while (tabla.contiene(h.clave()));
        return h;
    }
    /** Huella que nadie usa todavía, para una red nueva. */
    private Huella huellaLibre() {
        Set<String> usadas = new HashSet<>();
        for (Cuenta c : cuentas) usadas.add(c.huella.clave());
        Huella h;
        do h = new Huella(azar.nextInt(ICONOS.length), azar.nextInt(COLORES.length)); while (usadas.contains(h.clave()) || tabla.contiene(h.clave()));
        return h;
    }
    private void registrar(Huella h) {
        tabla.poner(h.clave(), new Familia(REDES[familias % REDES.length], tiempo));
        familias++;
        historial.add("Tabla hash: se registra «" + h.clave() + "» en la cubeta " + tabla.indice(h.clave()) + ".");
    }

    // ───────────────────────── Contrato ─────────────────────────
    @Override public Tipo tipo() { return Tipo.BOTS; }
    @Override public double duracion() { return DURACION; }
    @Override public String objetivo() { return "Reporten " + meta + " bots antes de que la desinformación llegue a 100."; }
    @Override public String rol(Jugador j) {
        return j == Jugador.CIUDADANO ? "Reporta una cuenta al instante (si es inocente, se paga)" : "Escaneo de zona: revisa a todos los cercanos y solo reporta bots";
    }
    @Override protected float[] limites() { return new float[]{16, 416, 80, 228}; }
    @Override protected double iaPausa() { return 2.6; }

    @Override protected void paso(double dt) {
        enfriamientoEscaneo = Math.max(0, enfriamientoEscaneo - dt);
        desinformacion = Math.max(0, desinformacion - .25 * dt);
        for (Avatar a : avatares()) {
            if (!a.pulso || a.ocupado()) continue;
            if (a.quien == Jugador.CIUDADANO) {
                Cuenta c = cercana(a);
                if (c == null) { if (a.humano) mensaje = "Acércate a una cuenta y compara su huella con la tabla."; continue; }
                if (otro(a).objetivo == c.id) continue;
                iniciarTarea(a, c.id, REPORTE_CIUDADANO, "reportar");
            } else {
                if (enfriamientoEscaneo > 0) { if (a.humano) mensaje = "El escáner se está recargando…"; continue; }
                iniciarTarea(a, ESCANEO, ESCANEO_PERIODISTA, "escanear");
                decir(a, "Escaneando huellas…");
            }
        }
        cuentas(dt);
        palomo(dt);
    }

    private void cuentas(double dt) {
        for (Iterator<Cuenta> it = cuentas.iterator(); it.hasNext(); ) {
            Cuenta c = it.next();
            c.globoT = Math.max(0, c.globoT - dt);
            c.revisada = Math.max(0, c.revisada - dt);
            if (c.conducta == Conducta.SALE) {
                caminar(c, dt, 40);
                if (c.x < -10 || c.x > ANCHO + 10) { it.remove(); pendientes.add(0); }
                continue;
            }
            boolean quieta = c.id == ciudadano.objetivo && ciudadano.tareaTipo.equals("reportar");
            if (!quieta) {
                c.retarget -= dt;
                if (c.retarget <= 0 || Math.hypot(c.tx - c.x, c.ty - c.y) < 2) destino(c);
                caminar(c, dt, c.bot ? 22 : 18);
            } else c.caminando = false;
            c.publicar -= dt;
            if (c.publicar <= 0) {
                if (c.bot) {
                    c.globo = CADENAS[azar.nextInt(CADENAS.length)]; c.globoT = 1.8;
                    desinformacion = Math.min(100, desinformacion + subidaPublicacion); publicaciones++;
                    efecto("rumor", c.x, c.y - 20);
                    c.publicar = 7 + azar.nextDouble() * 4;
                } else {
                    c.globo = NORMALES[azar.nextInt(NORMALES.length)]; c.globoT = 1.6;
                    c.publicar = 8 + azar.nextDouble() * 10;
                }
            }
        }
        // Entran cuentas nuevas por los costados; a veces son bots de una red ya registrada.
        if (!pendientes.isEmpty()) {
            proximaEntrada -= dt;
            if (proximaEntrada <= 0) {
                pendientes.remove(0);
                List<String> claves = tabla.claves();
                Huella deBot = null;
                if (azar.nextDouble() < .45 && !claves.isEmpty()) deBot = huellaDe(claves.get(azar.nextInt(claves.size())));
                Cuenta c = nuevaCuenta(deBot);
                boolean izq = azar.nextBoolean();
                c.x = izq ? 4 : ANCHO - 4; c.y = 90 + azar.nextFloat() * 130;
                c.publicar = 4 + azar.nextDouble() * 5;
                destino(c);
                proximaEntrada = 1.2;
            }
        }
    }

    private static Huella huellaDe(String clave) {
        String[] p = clave.split("·");
        return new Huella(List.of(ICONOS).indexOf(p[0]), List.of(COLORES).indexOf(p[1]));
    }

    private void palomo(double dt) {
        if (objetivoPalomo == null) {
            moverPalomo((float) (216 + 170 * Math.sin(tiempo * .4)), 40, 80, dt);
            proximaFamilia -= dt;
            if (proximaFamilia <= 0 && familias < maxFamilias) {
                List<Cuenta> candidatas = new ArrayList<>();
                for (Cuenta c : cuentas) if (!c.bot && c.conducta == Conducta.PASEA && c.id != ciudadano.objetivo) candidatas.add(c);
                if (candidatas.size() < 3) { proximaFamilia = 3; return; }
                objetivoPalomo = candidatas.get(azar.nextInt(candidatas.size())); tPalomo = 0;
                palomoDice("¡Hora de crear una red nueva de bots!");
            } else if (proximaFamilia <= 0) proximaFamilia = 999;
            return;
        }
        tPalomo += dt;
        moverPalomo(objetivoPalomo.x, objetivoPalomo.y - 30, 150, dt);
        if (tPalomo < 1.4) return;
        // La red nueva: una huella nueva registrada en la tabla y 2–3 cuentas convertidas en bots.
        Huella h = huellaLibre();
        registrar(h);
        Familia f = tabla.obtener(h.clave());
        int convertidas = 0, cupo = 2 + azar.nextInt(2);
        List<Cuenta> orden = new ArrayList<>(cuentas);
        orden.sort(Comparator.comparingDouble(c -> Math.hypot(c.x - objetivoPalomo.x, c.y - objetivoPalomo.y)));
        for (Cuenta c : orden) {
            if (convertidas >= cupo) break;
            if (c.bot || c.conducta != Conducta.PASEA || c.id == ciudadano.objetivo) continue;
            c.bot = true; c.huella = h; c.publicar = 3 + azar.nextDouble() * 3; c.revisada = 0;
            efecto("rumor", c.x, c.y - 18); convertidas++;
        }
        mensaje = "El Palomo creó la «" + f.red() + "» (" + h.clave() + "): " + convertidas + " cuentas hackeadas.";
        palomoDice("¡" + f.red() + " en línea!");
        objetivoPalomo = null;
        proximaFamilia = intervaloFamilia * (.85 + .3 * azar.nextDouble());
    }

    @Override protected float[] posicionObjetivo(int id) {
        if (id == ESCANEO) return new float[]{periodista.x, periodista.y}; // el escaneo es sobre el propio Periodista
        Cuenta c = buscar(id);
        return c == null ? null : new float[]{c.x, c.y};
    }

    @Override protected void completarTarea(Avatar a, int id, String tipo) {
        if (tipo.equals("reportar")) {
            Cuenta c = buscar(id);
            if (c == null) return;
            Familia f = tabla.obtener(c.huella.clave());
            ultimaConsulta = consultaTexto();
            if (f != null) { reportar(a, c, f); }
            else {
                falsos++; c.revisada = 6; puntos = Math.max(0, puntos - 5);
                desinformacion = Math.min(100, desinformacion + 5);
                decir(a, "Uy… era una cuenta real.");
                c.globo = "¡Oye! ¡Yo soy real!"; c.globoT = 1.6;
                mensaje = "«" + c.huella.clave() + "» no está en la tabla: reportaste a alguien inocente.";
            }
        } else {
            int encontrados = 0, revisadas = 0;
            for (Cuenta c : new ArrayList<>(cuentas)) {
                if (c.conducta != Conducta.PASEA || Math.hypot(c.x - a.x, c.y - a.y) > RADIO_ESCANEO) continue;
                revisadas++;
                Familia f = tabla.obtener(c.huella.clave());
                if (f != null) { reportar(a, c, f); encontrados++; } else c.revisada = 5;
            }
            ultimaConsulta = consultaTexto();
            enfriamientoEscaneo = ENFRIAMIENTO_ESCANEO;
            efecto("verificacion", a.x, a.y - 16);
            mensaje = "Escaneo de zona: " + revisadas + " cuentas consultadas en la tabla, " + encontrados + " bots.";
        }
    }

    private void reportar(Avatar a, Cuenta c, Familia f) {
        c.reportada = true; c.conducta = Conducta.SALE; c.globo = "¡Cuenta suspendida!"; c.globoT = 1.4;
        c.tx = c.x < ANCHO / 2f ? -20 : ANCHO + 20; c.ty = c.y;
        reportados++; puntos += 12;
        efecto("verificacion", c.x, c.y - 18);
        if (a.quien == Jugador.CIUDADANO) mensaje = "¡Bot de la «" + f.red() + "» reportado! (" + reportados + "/" + meta + ")";
        historial.add(nombre(a.quien) + " reportó un bot de la " + f.red() + ".");
    }

    private String consultaTexto() {
        var b = tabla.ultimaBusqueda();
        if (b == null) return "";
        ultimaBusqueda = b;
        return "h(«" + b.clave() + "») mod " + CAPACIDAD + " = " + b.cubeta() + " · " + b.comparaciones() + " comparación"
            + (b.comparaciones() == 1 ? "" : "es") + " · " + (b.encontrada() ? "¡BOT!" : "no registrada");
    }

    @Override protected void comprobarFin() {
        if (desinformacion >= 100) terminar(Estado.DERROTA, "Los bots inundaron Civitas de cadenas: desinformación al 100.");
        else if (reportados >= meta) terminar(Estado.VICTORIA, "Reportaron " + reportados + " bots: la red del Palomo se cayó.");
    }
    @Override protected void finDelTiempo() {
        terminar(Estado.DERROTA, "Se acabó el tiempo: reportaron " + reportados + " de " + meta + " bots.");
    }

    @Override protected void reposo(double dt) {
        for (Cuenta c : cuentas) { c.retarget -= dt; if (c.retarget <= 0) destino(c); caminar(c, dt, 16); }
        moverPalomo((float) (216 + 170 * Math.sin(preparacion)), 40, 80, dt);
    }

    // ───────────────────────── Ayudante automático ─────────────────────────
    @Override protected void ia(Avatar a, double dt) {
        a.dx = a.dy = 0;
        if (a.ocupado() || a.aturdido > 0) return;
        Cuenta meta = null; double mejor = Double.MAX_VALUE;
        for (Cuenta c : cuentas) {
            if (c.conducta != Conducta.PASEA || !tabla.contiene(c.huella.clave()) || otro(a).objetivo == c.id) continue;
            double d = a.distancia(c.x, c.y);
            if (d < mejor) { mejor = d; meta = c; }
        }
        if (meta == null) { irHacia(a, 216, 150, 30); return; }
        if (a.quien == Jugador.PERIODISTA) {
            boolean llego = irHacia(a, meta.x, meta.y, (float) RADIO_ESCANEO - 16);
            if (reaccionar(a, dt, llego && enfriamientoEscaneo == 0, 1.4)) a.accion = true;
        } else {
            boolean llego = irHacia(a, meta.x, meta.y, ALCANCE - 12);
            if (reaccionar(a, dt, llego && cercana(a) == meta, .7)) a.accion = true;
        }
    }

    // ───────────────────────── Utilidades y consultas ─────────────────────────
    private void destino(Cuenta c) {
        boolean dentro = c.x >= 24 && c.x <= 408;
        c.tx = dentro ? Math.max(24, Math.min(408, c.x + (azar.nextFloat() - .5f) * 140)) : 40 + azar.nextFloat() * 352;
        c.ty = Math.max(88, Math.min(222, c.y + (azar.nextFloat() - .5f) * 80));
        c.retarget = 3 + azar.nextDouble() * 3;
    }
    private void caminar(Cuenta c, double dt, double vel) {
        float dx = c.tx - c.x, dy = c.ty - c.y; double d = Math.hypot(dx, dy);
        c.caminando = d > 1;
        if (!c.caminando) return;
        double p = Math.min(d, vel * dt);
        c.x += (float) (dx / d * p); c.y += (float) (dy / d * p);
        c.direccion = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 2 : 1) : (dy > 0 ? 0 : 3);
    }
    private Cuenta cercana(Avatar a) {
        Cuenta mejor = null; double d = ALCANCE;
        for (Cuenta c : cuentas) {
            if (c.conducta != Conducta.PASEA) continue;
            double dc = a.distancia(c.x, c.y); if (dc < d) { d = dc; mejor = c; }
        }
        return mejor;
    }
    private Cuenta buscar(int id) { for (Cuenta c : cuentas) if (c.id == id && c.conducta == Conducta.PASEA) return c; return null; }

    public TablaHash<Familia> tabla() { return tabla; }
    public int meta() { return meta; }
    public int reportados() { return reportados; }
    public int falsos() { return falsos; }
    public int desinformacion() { return (int) Math.round(desinformacion); }
    public int publicaciones() { return publicaciones; }
    public int botsActivos() { int n = 0; for (Cuenta c : cuentas) if (c.bot && c.conducta == Conducta.PASEA) n++; return n; }
    public double enfriamientoEscaneo() { return enfriamientoEscaneo; }
    public String ultimaConsulta() { return ultimaConsulta; }
    /** Última búsqueda hecha por un jugador (no las consultas internas del juego). */
    public TablaHash.Busqueda ultimaBusqueda() { return ultimaBusqueda; }
    public boolean escaneando() { return periodista.ocupado(); }
    public List<CuentaVista> cuentas() {
        Cuenta cc = ciudadano.humano ? cercana(ciudadano) : null;
        List<CuentaVista> s = new ArrayList<>();
        for (Cuenta c : cuentas)
            s.add(new CuentaVista(c.id, c.x, c.y, c.direccion, c.variante, c.caminando, c.huella, c.bot, c.reportada,
                c.revisada > 0, c == cc, c.globoT > 0 ? c.globo : "", c.bot && c.globoT > 0));
        return s;
    }
}
