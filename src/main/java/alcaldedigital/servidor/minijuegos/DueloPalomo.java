package alcaldedigital.servidor.minijuegos;

import alcaldedigital.compartido.estructuras.avl.ArbolAVLPublicaciones;
import alcaldedigital.compartido.modelo.Publicacion;
import java.util.*;

/**
 * Minijuego 3 · «Duelo contra el Palomo» (pelear). El Palomo se protege con un escudo de chismes
 * guardado en el mismo {@link ArbolAVLPublicaciones} del capítulo 1: cada chisme es una
 * publicación cuya clave es (fuerza, ID).
 *
 * <p>Mantener la acción carga un dato verificado cuya fuerza sube y baja (como un medidor de
 * golf). Al soltar se dispara. Si el dato golpea al Palomo, el AVL busca el <b>piso</b> de esa
 * fuerza: el chisme más fuerte que el dato puede desmentir (fuerza ≤ dato) y lo elimina. Si no hay
 * ninguno, el dato rebota. Con el escudo vacío, cada golpe le quita vida al Palomo.</p>
 *
 * <p>Ciudadano: medidor lento y fácil, pero solo llega a 70. Periodista: llega a 99, pero el
 * medidor es más rápido. Los chismes de fuerza &gt; 70 solo los puede desmentir el Periodista.</p>
 */
public final class DueloPalomo extends Minijuego {
    public record DisparoVista(float x, float y, int fuerza, Jugador quien) { }
    public record ProyectilVista(float x, float y) { }
    /** Explicación de la última búsqueda del piso para el panel. */
    public record Golpe(Jugador quien, int fuerza, Publicacion eliminado, boolean danio, boolean fallo) { }

    public static final double DURACION = 90;
    public static final int MAX_CIUDADANO = 70, MAX_PERIODISTA = 99, MAX_ESCUDO = 8;
    static final double PERIODO_CIUDADANO = 1.9, PERIODO_PERIODISTA = 1.3;
    static final float Y_PALOMO = 62, VEL_DISPARO = 300, VEL_PROYECTIL = 86;

    private static final String[] CHISMES = {
        "Dicen que el alcalde es un robot.", "El parque será un helipuerto.", "El Wi-Fi da sueño.",
        "Civitas vende tus fotos.", "Mañana no hay buses.", "Las palomas votan.", "Se cae internet el lunes.",
        "El agua trae virus.", "Cierran el mercado.", "Prohíben los memes.", "La biblioteca cobra entrada.",
        "Los semáforos espían.", "Regalan celulares en la plaza.", "El periodista es un bot."};

    private static final class Disparo { float x, y; int fuerza; Jugador quien; }
    private static final class Proyectil { float x, y, vx; }

    private final ArbolAVLPublicaciones escudo = new ArbolAVLPublicaciones();
    private final List<Disparo> disparos = new ArrayList<>();
    private final List<Proyectil> proyectiles = new ArrayList<>();
    private final int vidaMaxima, corazonesMaximos;
    private final double intervaloAtaque, intervaloChisme;
    private int vida, corazones, siguienteId = 1, desmentidos, rebotes, fallos;
    private double proximoAtaque = 2.2, proximoChisme, enfriamientoC, enfriamientoP, golpeado, faseX;
    private int objetivoAtaque;
    private Golpe ultimoGolpe;

    public DueloPalomo(Modo modo, long semilla) {
        super(modo, semilla, 170, 214, 262, 214);
        boolean duo = modo == Modo.DUO;
        vidaMaxima = vida = 3;
        corazonesMaximos = corazones = 8;
        intervaloAtaque = duo ? 1.75 : 2.1;
        intervaloChisme = duo ? 6.5 : 7.5;
        proximoChisme = intervaloChisme;
        int inicial = duo ? 6 : 5;
        // Escudo inicial variado: al menos un chisme que solo el Periodista puede desmentir.
        int[] base = duo ? new int[]{18, 34, 50, 63, 79, 92} : new int[]{20, 38, 55, 68, 84};
        for (int i = 0; i < inicial; i++) insertarChisme(Math.min(96, Math.max(12, base[i] + azar.nextInt(9) - 4)));
        px = 216; py = Y_PALOMO;
        mensaje = "El Palomo se cubre con un escudo de " + escudo.cantidad() + " chismes.";
        historial.add("Inicio: escudo de " + escudo.cantidad() + " chismes ordenado en un AVL por fuerza.");
    }

    private void insertarChisme(int fuerza) {
        String texto = CHISMES[(siguienteId * 5 + azar.nextInt(3)) % CHISMES.length];
        escudo.insertar(new Publicacion(siguienteId++, fuerza, "Palomo", texto, "Escudo"));
    }

    // ───────────────────────── Contrato ─────────────────────────
    @Override public Tipo tipo() { return Tipo.DUELO; }
    @Override public double duracion() { return DURACION; }
    @Override public String objetivo() { return "Rompan el escudo de chismes y golpeen al Palomo " + vidaMaxima + " veces."; }
    @Override public String rol(Jugador j) {
        return j == Jugador.CIUDADANO ? "Dato cívico: carga lenta, fuerza hasta 70" : "Dato verificado: carga rápida, fuerza hasta 99";
    }
    @Override protected float[] limites() { return new float[]{18, 414, 150, 228}; }
    /** El ayudante no descansa en el duelo (si no, solo recibiría golpes); su debilidad es la puntería. */
    @Override protected double iaPausa() { return 0; }
    @Override protected float velocidad(Avatar a) {
        float base = a.sostenida() ? .55f : 1;
        return a.humano ? base : base / FACTOR_IA; // en el duelo el ayudante se mueve a velocidad normal
    }

    /** Fuerza actual del medidor de un avatar: onda triangular 0 → máximo → 0. */
    public static int fuerza(Jugador j, double carga) {
        double periodo = j == Jugador.CIUDADANO ? PERIODO_CIUDADANO : PERIODO_PERIODISTA;
        int max = j == Jugador.CIUDADANO ? MAX_CIUDADANO : MAX_PERIODISTA;
        double f = (carga % periodo) / periodo;
        double tri = f < .5 ? f * 2 : 2 - f * 2;
        return (int) Math.round(tri * max);
    }

    @Override protected void paso(double dt) {
        enfriamientoC = Math.max(0, enfriamientoC - dt);
        enfriamientoP = Math.max(0, enfriamientoP - dt);
        golpeado = Math.max(0, golpeado - dt);
        for (Avatar a : avatares()) {
            boolean enfriando = (a == ciudadano ? enfriamientoC : enfriamientoP) > 0;
            if (a.aturdido > 0) { a.valor = 0; continue; }
            if (a.sostenida() && !enfriando) a.valor += dt;
            if (a.soltada && a.valor > 0) {
                int f = fuerza(a.quien, a.valor);
                a.valor = 0;
                if (f < 4) continue;
                Disparo d = new Disparo(); d.x = a.x; d.y = a.y - 30; d.fuerza = f; d.quien = a.quien;
                disparos.add(d); a.acciones++;
                double espera = a.humano ? .35 : 1.0; // el ayudante dispara con menos frecuencia
                if (a == ciudadano) enfriamientoC = espera; else enfriamientoP = espera;
            }
            if (!a.sostenida()) a.valor = 0;
        }
        moverDisparos(dt);
        palomo(dt);
        moverProyectiles(dt);
    }

    private void moverDisparos(double dt) {
        for (Iterator<Disparo> it = disparos.iterator(); it.hasNext(); ) {
            Disparo d = it.next();
            d.y -= (float) (VEL_DISPARO * dt);
            if (d.y > py + 14) continue;
            it.remove();
            if (Math.abs(d.x - px) <= 30) impacto(d);
            else { fallos++; mensaje = "El dato pasó de largo. Apunten debajo del Palomo."; }
        }
    }

    /** Búsqueda del piso en el AVL: el chisme más fuerte con fuerza ≤ dato. */
    private void impacto(Disparo d) {
        if (escudo.cantidad() == 0) {
            vida--; golpeado = .6; puntos += 30;
            ultimoGolpe = new Golpe(d.quien, d.fuerza, null, true, false);
            palomoDice(vida > 0 ? "¡Auch! ¡Eso era un dato verificado!" : "¡Cuuuu! ¡Me rindo!");
            efecto("verificacion", px, py);
            mensaje = "¡Golpe directo! Al Palomo le queda " + vida + " de vida.";
            historial.add(nombre(d.quien) + " golpeó al Palomo sin escudo (dato " + d.fuerza + ").");
            if (vida <= 0) terminar(Estado.VICTORIA, "El Palomo huyó sin escudo ni excusas.");
            return;
        }
        Publicacion p = escudo.piso(d.fuerza);
        if (p == null) {
            rebotes++;
            ultimoGolpe = new Golpe(d.quien, d.fuerza, null, false, true);
            palomoDice("Jajá, ese dato es muy débil.");
            mensaje = "Dato " + d.fuerza + " rebotó: el chisme más débil tiene fuerza " + escudo.minimo().riesgo() + ".";
            return;
        }
        escudo.eliminar(p.id());
        desmentidos++; puntos += 10 + p.riesgo() / 5;
        ultimoGolpe = new Golpe(d.quien, d.fuerza, p, false, false);
        efecto("verificacion", px, py + 6);
        mensaje = nombre(d.quien) + " desmintió «" + p.texto() + "» (" + p.riesgo() + ") con un dato de " + d.fuerza + ".";
        historial.add("Piso(" + d.fuerza + ") = " + p.riesgo() + ": se elimina el chisme #" + p.id() + ".");
        if (escudo.cantidad() == 0) {
            proximoChisme = Math.max(proximoChisme, 5);
            palomoDice("¡Mi escudo! No, no, no…");
            mensaje = "¡Escudo roto! Ahora cada dato que acierte le quita vida.";
        }
    }

    private void palomo(double dt) {
        double rapidez = .55 + .25 * (vidaMaxima - vida);
        faseX += dt * rapidez;
        float meta = (float) (216 + 170 * Math.sin(faseX));
        moverPalomo(meta, Y_PALOMO + (float) Math.sin(tiempo * 2.2) * 4, 200, dt);
        proximoAtaque -= dt;
        if (proximoAtaque <= 0) {
            Avatar blanco = (objetivoAtaque++ % 2 == 0) ? ciudadano : periodista;
            Proyectil pr = new Proyectil(); pr.x = px; pr.y = py + 18;
            float t = (blanco.y - pr.y) / VEL_PROYECTIL;
            pr.vx = (blanco.x + (azar.nextFloat() - .5f) * 10 - pr.x) / Math.max(.5f, t);
            proyectiles.add(pr);
            proximoAtaque = intervaloAtaque * (.8 + .4 * azar.nextDouble());
        }
        proximoChisme -= dt;
        if (proximoChisme <= 0) {
            proximoChisme = intervaloChisme;
            if (escudo.cantidad() < MAX_ESCUDO) {
                int f = 15 + azar.nextInt(76);
                insertarChisme(f);
                var pasos = escudo.pasos();
                String op = pasos.get(pasos.size() - 1).operacion();
                palomoDice("¡Chisme nuevo! Fuerza " + f + ".");
                mensaje = "El Palomo inventó un chisme de fuerza " + f + " · " + op;
                historial.add(op);
            }
        }
    }

    private void moverProyectiles(double dt) {
        for (Iterator<Proyectil> it = proyectiles.iterator(); it.hasNext(); ) {
            Proyectil pr = it.next();
            pr.y += (float) (VEL_PROYECTIL * dt); pr.x += (float) (pr.vx * dt);
            if (pr.y > 240) { it.remove(); continue; }
            for (Avatar a : avatares()) {
                if (a.invulnerable > 0 || Math.abs(pr.x - a.x) > 9 || Math.abs(pr.y - (a.y - 14)) > 13) continue;
                it.remove();
                corazones--; a.aturdido = .7; a.invulnerable = 1.6; a.valor = 0;
                efecto("rumor", a.x, a.y - 20);
                decir(a, "¡Puaj, un chisme!");
                mensaje = "Un chisme golpeó al " + nombre(a.quien) + ". Corazones: " + corazones + ".";
                if (corazones <= 0) terminar(Estado.DERROTA, "Los chismes del Palomo los dejaron sin corazones.");
                break;
            }
        }
    }

    @Override protected void comprobarFin() { }
    @Override protected void finDelTiempo() {
        terminar(Estado.DERROTA, "Se acabó el tiempo y el Palomo sigue en pie (" + vida + " de vida, " + escudo.cantidad() + " chismes).");
    }
    @Override protected void reposo(double dt) { moverPalomo((float) (216 + 60 * Math.sin(preparacion * 2)), Y_PALOMO, 120, dt); }

    // ───────────────────────── Ayudante automático ─────────────────────────
    @Override protected void ia(Avatar a, double dt) {
        a.dx = a.dy = 0;
        if (a.aturdido > 0) return;
        // 1) Esquivar el proyectil más peligroso.
        for (Proyectil pr : proyectiles) {
            float llegada = (a.y - 14 - pr.y) / VEL_PROYECTIL;
            if (llegada < 0 || llegada > 1f) continue;
            float xImpacto = pr.x + pr.vx * llegada;
            if (Math.abs(xImpacto - a.x) < 20) { a.dx = xImpacto > a.x ? -1 : 1; if (a.x < 40) a.dx = 1; if (a.x > 392) a.dx = -1; return; }
        }
        // 2) Ponerse debajo de donde estará el Palomo cuando llegue el disparo.
        float vuelo = (a.y - 30 - py) / VEL_DISPARO;
        float futuro = (float) (216 + 170 * Math.sin(faseX + vuelo * (.55 + .25 * (vidaMaxima - vida))));
        float separacion = a == ciudadano ? -14 : 14;
        irHacia(a, Math.max(24, Math.min(408, futuro + separacion)), a.y, 6);
        // 3) Cargar y soltar con la fuerza justa para el chisme más fuerte que puede romper.
        int max = a.quien == Jugador.CIUDADANO ? MAX_CIUDADANO : MAX_PERIODISTA;
        Publicacion blanco = escudo.piso(max);
        int deseo = escudo.cantidad() == 0 ? max / 2 : blanco == null ? -1 : blanco.riesgo();
        if (deseo < 0) return; // no puede romper nada: deja ese trabajo al compañero
        boolean alineado = Math.abs(futuro - a.x) < 24;
        // El ayudante suelta la acción al final de cada cuadro: «accionAntes» indica si estaba cargando.
        boolean cargando = a.accionAntes && a.valor > 0;
        if (!cargando) { a.accion = alineado && (a == ciudadano ? enfriamientoC : enfriamientoP) == 0; return; }
        int f = fuerza(a.quien, a.valor);
        boolean enRango = f >= deseo && f <= deseo + 9;
        a.accion = !(enRango && alineado) && a.valor < 3;
    }

    // ───────────────────────── Consultas ─────────────────────────
    public ArbolAVLPublicaciones escudo() { return escudo; }
    public int vida() { return vida; }
    public int vidaMaxima() { return vidaMaxima; }
    public int corazones() { return corazones; }
    public int corazonesMaximos() { return corazonesMaximos; }
    public int desmentidos() { return desmentidos; }
    public int rebotes() { return rebotes; }
    public int fallos() { return fallos; }
    public boolean golpeado() { return golpeado > 0; }
    public double proximoChisme() { return Math.max(0, proximoChisme); }
    public Golpe ultimoGolpe() { return ultimoGolpe; }
    /** Fuerza que tendría el disparo si se soltara ahora (0 si no está cargando). */
    public int carga(Jugador j) { Avatar a = avatar(j); return a.valor > 0 ? fuerza(j, a.valor) : 0; }
    public List<DisparoVista> disparos() {
        List<DisparoVista> s = new ArrayList<>();
        for (Disparo d : disparos) s.add(new DisparoVista(d.x, d.y, d.fuerza, d.quien));
        return s;
    }
    public List<ProyectilVista> proyectiles() {
        List<ProyectilVista> s = new ArrayList<>();
        for (Proyectil p : proyectiles) s.add(new ProyectilVista(p.x, p.y));
        return s;
    }
}
