package alcaldedigital.cliente.interfaz;

import alcaldedigital.cliente.JuegoCliente;
import alcaldedigital.compartido.estructuras.avl.ArbolAVLPublicaciones;
import alcaldedigital.compartido.estructuras.difusion.ArbolDifusion;
import alcaldedigital.servidor.minijuegos.*;
import alcaldedigital.servidor.minijuegos.Minijuego.Jugador;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Pantallas de los minijuegos de la misión 5: selector, partida y resultado. Usa los mismos
 * materiales del juego (marcos, fuente, sprites e iconos) a través de {@link RenderJuego}.
 * Mundo lógico de 432×242 dibujado a escala 2 en (24, 114), igual que la misión 5.
 */
final class RenderMinijuegos {
    private static final int VX = 24, VY = 114, VW = 866, VH = 484;
    private static final Color VERDE = new Color(0x6ABCAA), VERDE_CLARO = new Color(0x8FD7B6), GRIS = new Color(0x405461),
        MORADO = new Color(0x6B3FA0), MORADO_CLARO = new Color(0xB892E6);
    private static final Color[] COLORES_HUELLA = {new Color(0xE0574A), new Color(0x3B82D6), new Color(0x3FAE6A), new Color(0xF2C14E)};
    private static final String[] ICONOS_HUELLA = {"01_celular", "02_camara", "03_megafono", "07_compartir", "16_desinformacion", "23_conexion"};

    private final RenderJuego r;
    RenderMinijuegos(RenderJuego r) { this.r = r; }

    // ───────────────────────── Atajos a los materiales compartidos ─────────────────────────
    private Graphics2D g() { return r.g; }
    private JuegoCliente j() { return r.juego; }
    private void rect(int x, int y, int w, int h, Color c) { r.rect(x, y, w, h, c); }
    private void panel(int x, int y, int w, int h, Color c) { r.panel(x, y, w, h, c); }
    private void texto(String t, int x, int y, int s, Color c, boolean b) { r.texto(t, x, y, s, c, b); }
    private int parrafo(String t, int x, int y, int w, int s, Color c) { return r.parrafo(t, x, y, w, s, c); }
    private void imagen(String rec, int x, int y, int w, int h) { r.imagen(rec, x, y, w, h); }
    private void boton(String t, String a, int x, int y, int w, boolean p) { r.boton(t, a, x, y, w, p); }
    private int ancho(String t, int s, boolean b) { return r.tema.ancho(r.g, t, r.tema.fuente(s, b)); }
    private boolean reducido() { return j().movimientoReducido; }
    private double t() { return j().animacion; }

    private void abrirMundo() {
        Graphics2D nuevo = (Graphics2D) r.g.create();
        pila.push(r.g); r.g = nuevo;
        r.g.clipRect(VX, VY, VW, VH); r.g.translate(VX, VY); r.g.scale(2, 2);
        r.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    private void cerrarMundo() { r.g.dispose(); r.g = pila.pop(); }
    private final Deque<Graphics2D> pila = new ArrayDeque<>();
    private int sx(float x) { return VX + Math.round(x * 2); }
    private int sy(float y) { return VY + Math.round(y * 2); }

    // ═════════════════════════ Selector de minijuegos ═════════════════════════
    void menu() {
        r.cabecera("Minijuegos · La calle del pánico", "Retos cortos sin historia. Solo: la computadora maneja al otro personaje. En pareja: un teclado, dos jugadores.");
        Minijuego.Tipo[] tipos = Minijuego.Tipo.values();
        String[] iconos = {"23_conexion", "19_mapa", "17_conflicto", "09_reportar"};
        String[] verbos = {"DETENER", "BUSCAR", "PELEAR", "BUSCAR Y DETENER"};
        String[] textos = {
            "Un rumor salta de cuenta en cuenta. Blinda las sanas que están junto al rumor y desmiente las contagiadas antes de que caiga la mitad de la red.",
            "Pregunta «¿quién te lo mandó?» y sigue la cadena de reenvíos hasta la cuenta falsa que inventó el rumor. Tres rondas.",
            "Carga datos verificados y dispárale al Palomo. Cada acierto desmiente el chisme más fuerte que tu dato alcanza a romper.",
            "Compara la huella de cada cuenta con la tabla de bots y reporta solo a las falsas antes de que inunden Civitas de cadenas."};
        for (int i = 0; i < 4; i++) {
            int x = 28 + (i % 2) * 624, y = 132 + (i / 2) * 212;
            panel(x, y, 600, 200, new Color(0x22394D));
            TemaPixel.marco(r.g, x + 14, y + 16, 76, 76, TemaPixel.NOCHE, TemaPixel.CIAN, false);
            imagen("iconos/" + iconos[i], x + 24, y + 26, 56, 56);
            texto((i + 1) + " · " + tipos[i].titulo, x + 106, y + 40, 22, r.papel, true);
            texto(verbos[i] + "  ·  " + tipos[i].estructura.toUpperCase(Locale.ROOT), x + 106, y + 64, 12, r.oro, true);
            parrafo(textos[i], x + 106, y + 90, 476, 14, r.tenue);
            String base = "mj:" + tipos[i].name() + ":";
            boton("Solo · Ciudadano", base + "SOLO_CIUDADANO", x + 14, y + 146, 182, false);
            boton("Solo · Periodista", base + "SOLO_PERIODISTA", x + 206, y + 146, 182, false);
            boton("En pareja", base + "DUO", x + 398, y + 146, 186, true);
        }
        boton("Contraste: " + (j().contraste ? "alto" : "normal"), "contraste", 28, 572, 284, false);
        boton("Movimiento: " + (j().movimientoReducido ? "reducido" : "normal"), "movimiento", 328, 572, 300, false);
        if (j().minijuegoEnCurso()) boton("Continuar: " + j().minijuego.tipo().titulo + "  →", "mjcontinuar", 652, 572, 600, true);
        else {
            texto("J1 Ciudadano: WASD + E (o Espacio)", 660, 590, 15, r.papel, true);
            texto("J2 Periodista: flechas + Enter", 660, 614, 15, r.papel, true);
        }
        texto("En un jugador, tu personaje responde a los dos lados del teclado. El otro lo maneja la computadora (etiqueta PC).", 30, 652, 14, r.tenue, false);
        r.pie("Esc vuelve al menú  ·  Tab y Enter recorren los botones  ·  Cada partida empieza con 3 s de cuenta atrás");
    }

    // ═════════════════════════ Partida ═════════════════════════
    void juego() {
        Minijuego m = j().minijuego;
        texto("MINIJUEGO · " + m.tipo().estructura.toUpperCase(Locale.ROOT), 28, 30, 14, r.oro, true);
        texto(m.tipo().titulo, 28, 65, 26, r.papel, true);
        texto(m.modo().texto.toUpperCase(Locale.ROOT) + "  /  " + m.puntos() + " puntos", 28, 90, 14, r.tenue, false);
        double restante = m.restante();
        boolean urgente = restante <= 10 && m.estado() == Minijuego.Estado.EN_CURSO;
        imagen("iconos/20_reloj", 600, 40, 32, 32);
        texto((int) Math.ceil(restante) + " s", 642, 70, 34, urgente && ((int) (t() * 3) % 2 == 0 || reducido()) ? RenderJuego.ROJO : r.papel, true);
        rect(600, 82, 420, 8, GRIS);
        rect(600, 82, (int) (420 * restante / m.duracion()), 8, urgente ? RenderJuego.ROJO : r.oro);
        boton("Pausa [Esc]", "minijuegos", 1060, 35, 192, false);
        escena(m);
        panel(912, 114, 344, 484, new Color(0x22394D));
        switch (m) {
            case CortaLaCadena c -> panelCadena(c);
            case RastreaLaFuente f -> panelFuente(f);
            case DueloPalomo d -> panelDuelo(d);
            case CazaDeBots b -> panelBots(b);
            default -> { }
        }
        panel(24, 608, 1232, 72, new Color(0x294357));
        medidores(m, 630);
        texto(r.ajustar(m.mensaje(), 1190, 13, false), 42, 668, 13, r.oro, false);
        r.pie(controles(m));
        if (m.estado() == Minijuego.Estado.PREPARACION) cuentaAtras(m);
    }

    private String controles(Minijuego m) {
        String accion = switch (m.tipo()) {
            case CADENA -> "blindar / desmentir"; case FUENTE -> "preguntar / espantar al Palomo";
            case DUELO -> "mantener para cargar, soltar para disparar"; case BOTS -> "reportar (Ciudadano) · escanear zona (Periodista)";
        };
        if (m.modo() == Minijuego.Modo.DUO) return "J1 Ciudadano: WASD + E    ·    J2 Periodista: flechas + Enter    ·    Acción: " + accion + "    ·    Esc: pausa";
        return "WASD o flechas: caminar    ·    E, Espacio o Enter: " + accion + "    ·    Esc: pausa";
    }

    private void cuentaAtras(Minijuego m) {
        rect(VX, VY, VW, VH, new Color(2, 18, 32, 190));
        String n = "" + Math.max(1, m.cuentaAtras());
        texto(n, 457 - ancho(n, 96, true) / 2, 270, 96, r.oro, true);
        String o = m.objetivo();
        texto(o, 457 - ancho(o, 20, true) / 2, 320, 20, r.papel, true);
        int y = 372;
        for (Jugador q : Jugador.values()) {
            String quien = (q == Jugador.CIUDADANO ? "Ciudadano" : "Periodista") + " (" + etiquetaLarga(m, q) + ")";
            panel(90, y - 26, 734, 64, new Color(0x22394D));
            imagen("actores/" + (q == Jugador.CIUDADANO ? "ciudadano" : "periodista") + "_00", 100, y - 22, 56, 56);
            texto(quien, 168, y, 17, r.oro, true);
            texto(m.rol(q), 168, y + 24, 14, r.papel, false);
            y += 80;
        }
    }

    private String etiqueta(Minijuego m, Jugador q) {
        if (!m.humano(q)) return "PC";
        if (m.modo() == Minijuego.Modo.DUO) return q == Jugador.CIUDADANO ? "J1" : "J2";
        return "Tú";
    }
    private String etiquetaLarga(Minijuego m, Jugador q) {
        if (!m.humano(q)) return "lo maneja la computadora";
        if (m.modo() == Minijuego.Modo.DUO) return q == Jugador.CIUDADANO ? "J1 · WASD + E" : "J2 · flechas + Enter";
        return "tú · WASD/flechas + E/Enter";
    }

    private void medidor(String nombre, String valor, double fraccion, boolean peligro, int x, int y, int w) {
        texto(nombre, x, y, 13, r.papel, false);
        texto(valor, x + w - ancho(valor, 15, true), y, 13, peligro && fraccion >= .7 ? RenderJuego.ROJO : r.oro, true);
        rect(x, y + 8, w, 6, GRIS);
        double f = Math.max(0, Math.min(1, fraccion));
        Color c = peligro ? (f >= .7 ? RenderJuego.ROJO : f >= .4 ? TemaPixel.ORO : VERDE) : VERDE;
        rect(x, y + 8, Math.max(f > 0 ? 1 : 0, (int) (w * f)), 6, c);
    }

    private void medidores(Minijuego m, int y) {
        switch (m) {
            case CortaLaCadena c -> {
                medidor("Cuentas contagiadas", c.contagiadas() + " / " + c.limite(), (double) c.contagiadas() / c.limite(), true, 42, y, 270);
                medidor("Cuentas blindadas", "" + c.blindadas(), (double) c.blindadas() / c.red().cantidadVertices(), false, 330, y, 270);
                medidor("Próxima ola", String.format(Locale.ROOT, "%.1f s", c.proximaOla()), 1 - c.proximaOla() / c.intervaloOla(), true, 618, y, 270);
                medidor("Olas del rumor", "" + c.olas(), 0, false, 906, y, 320);
            }
            case RastreaLaFuente f -> {
                medidor("Fuentes encontradas", f.rondasResueltas() + " / 3", f.rondasResueltas() / 3.0, false, 42, y, 270);
                medidor("Preguntas hechas", "" + f.interrogatorios(), 0, false, 330, y, 270);
                medidor("No lo habían recibido", "" + f.errores(), Math.min(1, f.errores() / 8.0), true, 618, y, 270);
                medidor("Ronda", (f.ronda() + 1) + " / 3", (f.ronda() + 1) / 3.0, false, 906, y, 320);
            }
            case DueloPalomo d -> {
                medidor("Vida del Palomo", d.vida() + " / " + d.vidaMaxima(), (double) d.vida() / d.vidaMaxima(), true, 42, y, 270);
                medidor("Chismes en el escudo", d.escudo().cantidad() + " / " + DueloPalomo.MAX_ESCUDO, (double) d.escudo().cantidad() / DueloPalomo.MAX_ESCUDO, true, 330, y, 270);
                medidor("Corazones del equipo", d.corazones() + " / " + d.corazonesMaximos(), (double) d.corazones() / d.corazonesMaximos(), false, 618, y, 270);
                medidor("Chismes desmentidos", "" + d.desmentidos(), 0, false, 906, y, 320);
            }
            case CazaDeBots b -> {
                medidor("Bots reportados", b.reportados() + " / " + b.meta(), (double) b.reportados() / b.meta(), false, 42, y, 270);
                medidor("Desinformación", "" + b.desinformacion(), b.desinformacion() / 100.0, true, 330, y, 270);
                medidor("Bots activos en la plaza", "" + b.botsActivos(), Math.min(1, b.botsActivos() / 8.0), true, 618, y, 270);
                medidor("Reportes a inocentes", "" + b.falsos(), Math.min(1, b.falsos() / 5.0), true, 906, y, 320);
            }
            default -> { }
        }
    }

    // ───────────────────────── Escena común ─────────────────────────
    private void escena(Minijuego m) {
        abrirMundo();
        switch (m) {
            case CortaLaCadena c -> mundoCadena(c);
            case RastreaLaFuente f -> mundoFuente(f);
            case DueloPalomo d -> mundoDuelo(d);
            case CazaDeBots b -> mundoBots(b);
            default -> { }
        }
        if (!reducido()) for (var e : m.efectos())
            imagen("efectos/" + e.tipo() + "_%02d".formatted(Math.min(7, (int) (e.edad() * 10))), (int) e.x() - 24, (int) e.y() - 24, 48, 48);
        cerrarMundo();
        globos(m);
    }

    /** Dibuja personajes y Palomo ordenados por la coordenada de los pies. */
    private void actores(Minijuego m, List<Map.Entry<Float, Runnable>> extra, int tamPalomo) {
        List<Map.Entry<Float, Runnable>> orden = new ArrayList<>(extra);
        for (Jugador q : Jugador.values()) {
            var a = m.avatarVista(q);
            orden.add(Map.entry(a.y(), () -> avatar(m, a)));
        }
        var p = m.palomo();
        orden.add(Map.entry(999f, () -> palomo(p, tamPalomo)));
        orden.sort(Map.Entry.comparingByKey());
        orden.forEach(e -> e.getValue().run());
    }

    private void avatar(Minijuego m, Minijuego.AvatarVista a) {
        int x = (int) a.x(), y = (int) a.y();
        boolean ocultar = a.parpadeo() && !reducido() && ((int) (t() * 12)) % 2 == 0;
        if (!ocultar) {
            r.sombra5(a.x(), a.y(), 20);
            boolean ciu = a.quien() == Jugador.CIUDADANO;
            if (a.ocupado() && !reducido()) imagen("acciones/frame_%02d".formatted((ciu ? 0 : 4) + (int) (t() * 6) % 4), x - 24, y - 48, 48, 48);
            else r.sprite(ciu ? "ciudadano" : "periodista", a.direccion() * 4 + (a.caminando() && !reducido() ? (int) (t() * 8) % 4 : 0), x, y);
        }
        if (a.aturdido()) imagen("efectos/rumor_%02d".formatted(reducido() ? 3 : (int) (t() * 10) % 8), x - 16, y - 50, 32, 32);
        r.marcador(etiqueta(m, a.quien()), x, y - 37, false);
        if (a.progreso() > 0) { rect(x - 12, y + 3, 24, 3, GRIS); rect(x - 12, y + 3, (int) (24 * a.progreso()), 3, VERDE); }
    }

    private void palomo(Minijuego.PalomoVista p, int tam) {
        int frame = reducido() ? 8 : 8 + (int) (t() * 8) % 4;
        BufferedImage im = r.recursos.imagen("actores/noti_%02d".formatted(frame));
        int x = (int) p.x(), y = (int) p.y();
        if (p.mirarIzquierda()) r.g.drawImage(im, x + tam / 2, y - tam / 2, x - tam / 2, y + tam / 2, 0, 0, 32, 32, null);
        else r.g.drawImage(im, x - tam / 2, y - tam / 2, tam, tam, null);
    }

    private void globos(Minijuego m) {
        int izq = VX, der = VX + VW, arriba = VY;
        Color fondoPalomo = new Color(0x2A1B2E), bordePalomo = new Color(0xF4BD4F);
        switch (m) {
            case RastreaLaFuente f -> {
                for (var p : f.personas()) if (!p.globo().isBlank())
                    r.globo(p.nombre().toUpperCase(Locale.ROOT), p.globo(), sx(p.x()), sy(p.y() - 44), 236, new Color(0xEAD7AF), r.teal, r.tinta, arriba, izq, der);
            }
            case CazaDeBots b -> {
                for (var c : b.cuentas()) if (!c.globo().isBlank())
                    r.globo(null, c.globo(), sx(c.x()), sy(c.y() - 46), 200, c.publicando() ? new Color(0xF2C9C4) : new Color(0xEAD7AF),
                        c.publicando() ? RenderJuego.ROJO_FUERTE : r.teal, r.tinta, arriba, izq, der);
            }
            default -> { }
        }
        for (Jugador q : Jugador.values()) {
            var a = m.avatarVista(q);
            if (!a.globo().isBlank())
                r.globo(q == Jugador.CIUDADANO ? "CIUDADANO" : "PERIODISTA", a.globo(), sx(a.x()), sy(a.y() - 50), 260, r.papel, r.teal, r.tinta, arriba, izq, der);
        }
        var p = m.palomo();
        if (!p.globo().isBlank()) r.globo("EL PALOMO", p.globo(), sx(p.x()), sy(p.y() - 22), 270, fondoPalomo, bordePalomo, r.papel, arriba, izq, der);
    }

    private void suelo(String tile) {
        for (int row = 0; row < 16; row++) for (int col = 0; col < 27; col++) imagen("tiles/" + tile, col * 16, row * 16, 16, 16);
    }
    private void arboleda() {
        for (int x : new int[]{20, 72, 360, 412}) imagen("objetos/" + (x % 3 == 0 ? "pino" : "arbol"), x - 18, 6, 36, 48);
    }

    // ═════════════════════════ 1 · Corta la cadena ═════════════════════════
    private void mundoCadena(CortaLaCadena c) {
        suelo("cesped");
        for (int col = 0; col < 27; col++) imagen("tiles/camino", col * 16, 56, 16, 16);
        arboleda();
        imagen("objetos/banco", 180, 30, 34, 22); imagen("objetos/lampara", 120, 14, 16, 40); imagen("objetos/lampara", 300, 14, 16, 40);
        rect(0, 0, 432, 242, new Color(6, 38, 60, 70));
        var nodos = c.nodos();
        Graphics2D g = r.g;
        // Aristas: rojo entre contagiadas, naranja punteado donde el rumor puede saltar, gris si hay un blindaje.
        for (int[] e : c.red().aristas()) {
            var a = nodos.get(e[0]); var b = nodos.get(e[1]);
            boolean ac = a.estado() == CortaLaCadena.EstadoNodo.CONTAGIADO, bc = b.estado() == CortaLaCadena.EstadoNodo.CONTAGIADO;
            boolean bloqueo = a.estado() == CortaLaCadena.EstadoNodo.BLINDADO || b.estado() == CortaLaCadena.EstadoNodo.BLINDADO;
            if (ac && bc) { g.setColor(RenderJuego.ROJO_FUERTE); g.setStroke(new BasicStroke(2.5f)); }
            else if ((ac || bc) && !bloqueo) { g.setColor(RenderJuego.ROJO); g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 3}, (float) (reducido() ? 0 : 7 - (t() * 12) % 7))); }
            else if (bloqueo) { g.setColor(new Color(160, 180, 190, 110)); g.setStroke(new BasicStroke(1.2f)); }
            else { g.setColor(new Color(0x54BDCF)); g.setStroke(new BasicStroke(1.5f)); }
            g.drawLine((int) a.x(), (int) a.y(), (int) b.x(), (int) b.y());
        }
        g.setStroke(new BasicStroke(1));
        for (var n : nodos) {
            int x = (int) n.x(), y = (int) n.y();
            Color fondo = switch (n.estado()) { case SANO -> r.papel; case BLINDADO -> new Color(0x197E87); case CONTAGIADO -> new Color(0xC2453F); };
            boolean proxima = n.estado() == CortaLaCadena.EstadoNodo.SANO && n.distancia() == 1;
            if (proxima && !reducido() && (int) (t() * 4) % 2 == 0) { g.setColor(new Color(233, 120, 102, 120)); g.fillOval(x - 16, y - 16, 32, 32); }
            g.setColor(TemaPixel.SOMBRA); g.fillOval(x - 11, y - 9, 24, 22);
            g.setColor(fondo); g.fillOval(x - 12, y - 12, 24, 24);
            g.setColor(proxima ? RenderJuego.ROJO : TemaPixel.NOCHE); g.setStroke(new BasicStroke(proxima ? 2 : 1.5f)); g.drawOval(x - 12, y - 12, 24, 24);
            if (n.estado() == CortaLaCadena.EstadoNodo.BLINDADO) {
                g.setColor(VERDE_CLARO); g.setStroke(new BasicStroke(2));
                g.drawArc(x - 15, y - 15, 30, 30, 90, (int) (360 * n.blindaje()));
            }
            g.setStroke(new BasicStroke(1));
            String icono = switch (n.estado()) { case SANO -> "01_celular"; case BLINDADO -> "13_confianza"; case CONTAGIADO -> "16_desinformacion"; };
            imagen("iconos/" + icono, x - 8, y - 8, 16, 16);
            texto("#" + (n.id() + 1), x - 6, y + 21, 8, r.papel, true);
            // Distancia BFS al rumor (las blindadas cortan el camino).
            if (n.estado() == CortaLaCadena.EstadoNodo.SANO && n.distancia() > 0) {
                String d = "d" + n.distancia();
                rect(x + 7, y - 17, 16, 10, n.distancia() == 1 ? RenderJuego.ROJO_FUERTE : TemaPixel.NOCHE);
                texto(d, x + 9, y - 9, 7, r.papel, true);
            }
            if (n.cerca()) { g.setColor(r.oro); g.setStroke(new BasicStroke(1.5f)); g.drawOval(x - 17, y - 17, 34, 34); g.setStroke(new BasicStroke(1)); }
            if (n.objetivo()) { g.setColor(new Color(0xF4BD4F)); g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{3, 3}, 0)); g.drawOval(x - 19, y - 19, 38, 38); g.setStroke(new BasicStroke(1)); }
        }
        actores(c, List.of(), 44);
    }

    private void panelCadena(CortaLaCadena c) {
        int x = 932;
        texto("RED DE CIVITAS · GRAFO + BFS", x, 146, 13, r.oro, true);
        texto(c.red().cantidadVertices() + " cuentas · " + c.red().cantidadAristas() + " contactos (aristas)", x, 170, 13, r.tenue, false);
        int y = parrafo("Cada ola el rumor avanza un nivel del BFS desde todas las contagiadas. Las blindadas no se visitan: cortan el camino.", x, 198, 300, 13, r.papel);
        int[] dist = c.distancias();
        Map<Integer, List<Integer>> niveles = new TreeMap<>();
        List<Integer> salvo = new ArrayList<>(), rojas = new ArrayList<>();
        for (int i = 0; i < dist.length; i++) {
            var e = c.estadoNodo(i);
            if (e == CortaLaCadena.EstadoNodo.CONTAGIADO) rojas.add(i + 1);
            else if (e == CortaLaCadena.EstadoNodo.SANO) { if (dist[i] < 0) salvo.add(i + 1); else niveles.computeIfAbsent(dist[i], k -> new ArrayList<>()).add(i + 1); }
        }
        y += 6;
        texto("DISTANCIAS BFS AL RUMOR", x, y, 12, r.oro, true); y += 8;
        y = fila("d=0 (contagiadas)", rojas, x, y, new Color(0xC2453F));
        for (var e : niveles.entrySet()) {
            if (y > 470) break;
            y = fila(e.getKey() == 1 ? "d=1 · ¡próxima ola!" : "d=" + e.getKey(), e.getValue(), x, y, e.getKey() == 1 ? RenderJuego.ROJO_FUERTE : TemaPixel.AZUL);
        }
        if (!salvo.isEmpty() && y <= 470) y = fila("Sin camino: a salvo", salvo, x, y, new Color(0x167964));
        texto("Frontera de la cola BFS: " + c.frontera().size() + " cuenta" + (c.frontera().size() == 1 ? "" : "s"), x, Math.max(y + 22, 500), 13, r.papel, true);
        texto("Ciudadano: blinda rápido · desmiente lento", x, 548, 12, r.tenue, false);
        texto("Periodista: desmiente rápido · blinda lento", x, 568, 12, r.tenue, false);
    }

    private int fila(String titulo, List<Integer> ids, int x, int y, Color color) {
        y += 18;
        texto(titulo, x, y, 12, r.papel, false);
        y += 6;
        int cx = x;
        if (ids.isEmpty()) { texto("—", x + 4, y + 14, 12, r.tenue, false); return y + 20; }
        for (int id : ids) {
            String s = "#" + id;
            int w = ancho(s, 12, true) + 12;
            if (cx + w > x + 300) { cx = x; y += 22; }
            rect(cx, y, w, 18, color);
            texto(s, cx + 6, y + 14, 11, r.papel, true);
            cx += w + 4;
        }
        return y + 20;
    }

    // ═════════════════════════ 2 · Rastrea la fuente ═════════════════════════
    private void mundoFuente(RastreaLaFuente f) {
        suelo("camino");
        for (int col = 0; col < 27; col++) { imagen("tiles/cesped", col * 16, 0, 16, 16); imagen("tiles/cesped", col * 16, 16, 16, 16); imagen("tiles/cesped", col * 16, 32, 16, 16); }
        arboleda();
        imagen("objetos/fuente", 188, 6, 56, 56);
        imagen("objetos/banco", 120, 36, 36, 24); imagen("objetos/banco", 276, 36, 36, 24);
        imagen("objetos/flores", 90, 44, 24, 20); imagen("objetos/flores", 318, 44, 24, 20);
        Graphics2D g = r.g;
        // Flechas «se lo mandó a» del camino descubierto.
        var personas = f.personas();
        var camino = f.camino();
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 3}, 0));
        for (int i = 1; i < camino.size(); i++) {
            var hijo = personas.get(camino.get(i - 1)); var padre = personas.get(camino.get(i));
            boolean conocido = padre.marca() != RastreaLaFuente.Marca.BUSCADO;
            if (!conocido) continue;
            g.setColor(new Color(0xF4BD4F));
            g.drawLine((int) padre.x(), (int) padre.y() - 14, (int) hijo.x(), (int) hijo.y() - 14);
        }
        g.setStroke(new BasicStroke(1));
        List<Map.Entry<Float, Runnable>> extra = new ArrayList<>();
        for (var p : personas) extra.add(Map.entry(p.y(), () -> persona(p)));
        actores(f, extra, 40);
    }

    private void persona(RastreaLaFuente.PersonaVista p) {
        int x = (int) p.x(), y = (int) p.y();
        r.sombra5(p.x(), p.y(), 18);
        if (p.desenmascarada()) imagen("actores/noti_%02d".formatted(reducido() ? 0 : (int) (t() * 6) % 4), x - 16, y - 32, 32, 32);
        else r.g.drawImage(r.vecinoImagen(p.variante(), p.direccion() * 4 + (p.caminando() && !reducido() ? (int) (t() * 6 + p.id()) % 4 : 0)), x - 16, y - 32, 32, 32, null);
        String icono = switch (p.marca()) {
            case INICIO -> "01_celular"; case INTERROGADO -> "08_verificar"; case DESCARTADO -> "10_ignorar";
            case FUENTE -> "09_reportar"; default -> null;
        };
        if (icono != null) imagen("iconos/" + icono, x - 7, y - 48, 14, 14);
        if (p.silenciada()) imagen("iconos/21_candado", x - 7, y - 48, 14, 14);
        if (p.nombreVisible()) {
            boolean buscado = p.marca() == RastreaLaFuente.Marca.BUSCADO;
            r.marcador(p.nombre(), x, y + 13, p.marca() == RastreaLaFuente.Marca.INTERROGADO || p.marca() == RastreaLaFuente.Marca.FUENTE);
            if (buscado) { r.g.setColor(r.oro); r.g.setStroke(new BasicStroke(1.5f)); r.g.drawOval(x - 14, y - 6, 28, 10); r.g.setStroke(new BasicStroke(1)); }
        }
        if (p.cerca()) { r.g.setColor(r.oro); r.g.setStroke(new BasicStroke(1)); r.g.drawOval(x - 12, y - 5, 24, 10); }
    }

    private void panelFuente(RastreaLaFuente f) {
        int x = 932;
        texto("ÁRBOL DE DIFUSIÓN · N-ARIO", x, 146, 13, r.oro, true);
        texto("Ronda " + (f.ronda() + 1) + " de 3 · el rumor llegó a " + f.arbol().tamano() + " personas", x, 170, 13, r.tenue, false);
        if (f.buscado() >= 0) {
            texto("BUSCANDO A", x, 200, 12, r.tenue, true);
            texto(f.nombreDe(f.buscado()).toUpperCase(Locale.ROOT), x + 96, 202, 22, r.oro, true);
        } else texto("¡FUENTE ENCONTRADA!", x, 202, 18, VERDE_CLARO, true);
        // Camino descubierto, de la fuente (arriba) a quien recibió el rumor (abajo).
        var camino = new ArrayList<>(f.camino());
        Collections.reverse(camino);
        int y = 222;
        if (!f.rondaResuelta()) { chipCamino("? Fuente desconocida", x, y, GRIS, null); y += 33; }
        for (int i = 0; i < camino.size() && y < 410; i++) {
            int id = camino.get(i);
            boolean buscado = id == f.buscado(), fuente = f.rondaResuelta() && i == 0;
            Integer prof = f.profundidadConocida(id);
            String s = (fuente ? "★ " : buscado ? "→ " : "") + f.nombreDe(id) + (fuente ? " · cuenta del Palomo" : buscado ? " · ¿quién se lo mandó?" : "");
            if (i > 0 || !f.rondaResuelta()) texto("↓", x + 150, y - 1, 10, r.tenue, false);
            chipCamino(s, x, y, fuente ? RenderJuego.ROJO_FUERTE : buscado ? new Color(0x8A6A1E) : TemaPixel.AZUL, prof == null ? null : "prof. " + prof);
            y += 33;
        }
        texto("↓ = «se lo mandó a» · subir al padre cuesta O(1)", x, 420, 11, r.tenue, false);
        // Último árbol resuelto, completo.
        ArbolDifusion ultimo = f.ultimoArbolResuelto();
        rect(x, 428, 304, 2, TemaPixel.CIAN);
        if (ultimo == null) { parrafo("Al encontrar la fuente aparece aquí el árbol completo de la ronda.", x, 452, 300, 12, r.tenue); return; }
        texto("ÚLTIMO ÁRBOL · altura " + ultimo.altura() + " · " + ultimo.tamano() + " nodos", x, 446, 11, r.oro, true);
        dibujarNario(f, ultimo.vista(), x, x + 304, 470, hojas(ultimo.vista()), 0);
    }

    private void chipCamino(String s, int x, int y, Color c, String extra) {
        rect(x, y, 304, 21, c);
        texto(r.ajustar(s, extra == null ? 288 : 220, 12, true), x + 8, y + 15, 12, r.papel, true);
        if (extra != null) texto(extra, x + 300 - ancho(extra, 11, false), y + 15, 11, r.oro, false);
    }

    private int hojas(ArbolDifusion.Vista n) { int s = 0; for (var h : n.hijos()) s += hojas(h); return Math.max(1, s); }
    private void dibujarNario(RastreaLaFuente f, ArbolDifusion.Vista n, int izq, int der, int y, int total, int nivel) {
        int x = (izq + der) / 2, off = 0;
        for (var h : n.hijos()) {
            int peso = hojas(h);
            int l = izq + (der - izq) * off / hojas(n), rr = izq + (der - izq) * (off + peso) / hojas(n);
            off += peso;
            r.g.setColor(new Color(0x69818B)); r.g.setStroke(new BasicStroke(1)); r.g.drawLine(x, y, (l + rr) / 2, y + 26);
            if (nivel < 6) dibujarNario(f, h, l, rr, y + 26, total, nivel + 1);
        }
        boolean raiz = nivel == 0;
        String nombre = f.nombreDe(n.id());
        int w = Math.max(18, Math.min(44, (der - izq) - 2));
        rect(x - w / 2, y - 8, w, 15, raiz ? RenderJuego.ROJO_FUERTE : TemaPixel.AZUL);
        texto(nombre.substring(0, Math.min(nombre.length(), w >= 40 ? 4 : w >= 28 ? 3 : 1)), x - w / 2 + 3, y + 4, 10, r.papel, true);
    }

    // ═════════════════════════ 3 · Duelo contra el Palomo ═════════════════════════
    private void mundoDuelo(DueloPalomo d) {
        suelo("camino");
        Graphics2D g = r.g;
        rect(0, 118, 432, 76, new Color(0x4C5964)); rect(0, 118, 432, 2, r.papel); rect(0, 192, 432, 2, r.papel);
        for (int x = 6; x < 432; x += 40) rect(x, 154, 22, 2, r.papel);
        rect(20, 0, 392, 64, new Color(0x8A5B3C)); rect(20, 58, 392, 6, new Color(0x5A3A28));
        for (int i = 0; i < 4; i++) for (int lado = 0; lado < 2; lado++) {
            int wx = lado == 0 ? 36 + i * 40 : 260 + i * 36;
            rect(wx, 18, 24, 30, new Color(0x3A2A20)); rect(wx + 2, 20, 20, 26, new Color(0x54BDCF));
        }
        rect(194, 24, 44, 40, TemaPixel.ORO); rect(197, 27, 38, 37, TemaPixel.AZUL);
        rect(118, 6, 196, 16, TemaPixel.NOCHE); rect(120, 8, 192, 12, TemaPixel.ORO);
        texto("CENTRO DE PERIODISMO", 160, 17, 8, TemaPixel.NOCHE, true);
        rect(0, 0, 432, 242, new Color(6, 20, 40, 40));
        var p = d.palomo();
        // Escudo de chismes: chips que orbitan al Palomo, en el orden del recorrido inorden del AVL.
        var chismes = d.escudo().recorrer("inorden");
        for (int i = 0; i < chismes.size(); i++) {
            double ang = (reducido() ? 0 : t() * .9) + i * Math.PI * 2 / chismes.size();
            int cx = (int) (p.x() + Math.cos(ang) * 40), cy = (int) (p.y() + Math.sin(ang) * 22);
            int f = chismes.get(i).riesgo();
            Color c = f > DueloPalomo.MAX_CIUDADANO ? new Color(0x8E2F5B) : MORADO;
            g.setColor(TemaPixel.SOMBRA); g.fillRoundRect(cx - 11, cy - 6, 24, 14, 6, 6);
            g.setColor(c); g.fillRoundRect(cx - 12, cy - 7, 24, 14, 6, 6);
            g.setColor(MORADO_CLARO); g.drawRoundRect(cx - 12, cy - 7, 24, 14, 6, 6);
            texto("" + f, cx - (f >= 10 ? 6 : 3), cy + 4, 8, r.papel, true);
        }
        if (d.escudo().cantidad() > 0) {
            g.setColor(new Color(184, 146, 230, reducido() ? 60 : 40 + (int) (20 * Math.sin(t() * 4))));
            g.fillOval((int) p.x() - 30, (int) p.y() - 28, 60, 56);
        }
        // Vida del Palomo.
        for (int i = 0; i < d.vidaMaxima(); i++) rect((int) p.x() - d.vidaMaxima() * 7 + i * 14, (int) p.y() - 40, 11, 5, i < d.vida() ? RenderJuego.ROJO : GRIS);
        for (var pr : d.proyectiles()) imagen("iconos/16_desinformacion", (int) pr.x() - 7, (int) pr.y() - 7, 14, 14);
        for (var s : d.disparos()) {
            Color c = s.quien() == Jugador.CIUDADANO ? new Color(0x8FD7B6) : new Color(0x54BDCF);
            rect((int) s.x() - 2, (int) s.y() - 8, 4, 12, c);
            texto("" + s.fuerza(), (int) s.x() + 4, (int) s.y(), 7, c, true);
        }
        List<Map.Entry<Float, Runnable>> extra = new ArrayList<>();
        for (Jugador q : Jugador.values()) {
            var a = d.avatarVista(q);
            int carga = d.carga(q);
            if (carga > 0) extra.add(Map.entry(a.y() + .1f, () -> medidorCarga(q, a, carga)));
        }
        actores(d, extra, d.golpeado() && !reducido() && (int) (t() * 20) % 2 == 0 ? 60 : 56);
    }

    private void medidorCarga(Jugador q, Minijuego.AvatarVista a, int carga) {
        int x = (int) a.x() - 16, y = (int) a.y() + 6;
        rect(x, y, 32, 5, GRIS);
        int max = q == Jugador.CIUDADANO ? DueloPalomo.MAX_CIUDADANO : DueloPalomo.MAX_PERIODISTA;
        rect(x, y, Math.max(1, 32 * carga / 99), 5, carga > 70 ? new Color(0xE5AA45) : VERDE);
        rect(x + 32 * max / 99, y - 1, 1, 7, r.papel);
        texto("" + carga, x + 34, y + 6, 8, r.oro, true);
    }

    private void panelDuelo(DueloPalomo d) {
        int x = 932;
        texto("ESCUDO DE CHISMES · AVL", x, 146, 13, r.oro, true);
        texto("Clave (fuerza, ID) · " + d.escudo().cantidad() + " nodos · altura " + d.escudo().altura(), x, 170, 13, r.tenue, false);
        var vista = d.escudo().vista();
        if (vista == null) {
            panel(x + 40, 210, 220, 60, new Color(0x167964));
            texto("¡ESCUDO VACÍO!", x + 74, 238, 16, r.papel, true);
            texto("Cada acierto le quita vida", x + 62, 258, 12, r.papel, false);
        } else avl(vista, 1084, 210, 74);
        int y = 400;
        texto("INORDEN", x, y, 11, r.tenue, true);
        StringBuilder sb = new StringBuilder();
        for (var p : d.escudo().recorrer("inorden")) sb.append(sb.length() == 0 ? "" : " · ").append(p.riesgo());
        texto(r.ajustar(sb.length() == 0 ? "(vacío)" : sb.toString(), 230, 12, true), x + 70, y, 12, r.papel, true);
        var golpe = d.ultimoGolpe();
        y += 24;
        texto("ÚLTIMO DATO", x, y, 11, r.tenue, true);
        String explicacion = golpe == null ? "Mantén la acción para cargar y suelta debajo del Palomo."
            : golpe.danio() ? "Dato " + golpe.fuerza() + " sin escudo: −1 de vida."
            : golpe.fallo() ? "Piso(" + golpe.fuerza() + ") = ninguno: rebota."
            : "Piso(" + golpe.fuerza() + ") = " + golpe.eliminado().riesgo() + " → se elimina #" + golpe.eliminado().id() + ".";
        parrafo(explicacion, x, y + 22, 300, 13, r.oro);
        texto("Piso(P): el chisme más fuerte con fuerza ≤ P", x, 498, 12, r.papel, false);
        texto("Ciudadano llega a 70 · Periodista a 99", x, 522, 12, r.tenue, false);
        texto(String.format(Locale.ROOT, "Próximo chisme (inserción) en %.0f s", d.proximoChisme()), x, 546, 12, r.tenue, false);
        var pasos = d.escudo().pasos();
        if (!pasos.isEmpty()) texto(r.ajustar(pasos.get(pasos.size() - 1).operacion(), 300, 11, false), x, 570, 11, r.tenue, false);
    }

    private void avl(ArbolAVLPublicaciones.Vista n, int x, int y, int distancia) {
        if (n == null) return;
        r.g.setStroke(new BasicStroke(2)); r.g.setColor(new Color(0x69818B));
        if (n.izquierdo() != null) r.g.drawLine(x, y, x - distancia, y + 50);
        if (n.derecho() != null) r.g.drawLine(x, y, x + distancia, y + 50);
        avl(n.izquierdo(), x - distancia, y + 50, distancia / 2);
        avl(n.derecho(), x + distancia, y + 50, distancia / 2);
        int f = n.valor().riesgo();
        TemaPixel.marco(r.g, x - 17, y - 15, 34, 30, f > DueloPalomo.MAX_CIUDADANO ? new Color(0x8E2F5B) : MORADO, MORADO_CLARO, false);
        texto("" + f, x - (f >= 10 ? 9 : 4), y + 6, 15, r.papel, true);
        texto("h" + n.altura() + (n.fe() == 0 ? "" : " " + (n.fe() > 0 ? "+" : "") + n.fe()), x - 12, y + 27, 10, r.tenue, false);
    }

    // ═════════════════════════ 4 · Caza de bots ═════════════════════════
    private void mundoBots(CazaDeBots b) {
        suelo("cesped");
        for (int col = 0; col < 27; col++) { imagen("tiles/camino", col * 16, 144, 16, 16); }
        for (int row = 4; row < 16; row++) imagen("tiles/camino", 208, row * 16, 16, 16);
        arboleda();
        imagen("objetos/fuente", 192, 8, 48, 48);
        imagen("objetos/aviso", 120, 22, 34, 38); imagen("objetos/rosa", 290, 14, 40, 48);
        Graphics2D g = r.g;
        var per = b.avatarVista(Jugador.PERIODISTA);
        if (b.escaneando()) {
            float rad = (float) 42;
            g.setColor(new Color(84, 189, 207, 50)); g.fillOval((int) (per.x() - rad), (int) (per.y() - rad * .7), (int) (rad * 2), (int) (rad * 1.4));
            g.setColor(new Color(0x54BDCF)); g.drawOval((int) (per.x() - rad), (int) (per.y() - rad * .7), (int) (rad * 2), (int) (rad * 1.4));
        }
        List<Map.Entry<Float, Runnable>> extra = new ArrayList<>();
        for (var c : b.cuentas()) extra.add(Map.entry(c.y(), () -> cuenta(c)));
        actores(b, extra, 40);
    }

    private void cuenta(CazaDeBots.CuentaVista c) {
        int x = (int) c.x(), y = (int) c.y();
        r.sombra5(c.x(), c.y(), 18);
        r.g.drawImage(r.vecinoImagen(c.variante(), c.direccion() * 4 + (c.caminando() && !reducido() ? (int) (t() * 7 + c.id()) % 4 : 0)), x - 16, y - 32, 32, 32, null);
        huella(c.huella(), x - 7, y - 50, 14);
        if (c.reportada()) imagen("iconos/09_reportar", x + 6, y - 50, 12, 12);
        else if (c.revisada()) imagen("iconos/13_confianza", x + 6, y - 50, 12, 12);
        if (c.cerca()) { r.g.setColor(r.oro); r.g.setStroke(new BasicStroke(1.5f)); r.g.drawOval(x - 12, y - 5, 24, 10); r.g.setStroke(new BasicStroke(1)); }
    }

    private void huella(CazaDeBots.Huella h, int x, int y, int s) {
        r.g.setColor(TemaPixel.SOMBRA); r.g.fillRoundRect(x + 1, y + 1, s, s, 4, 4);
        r.g.setColor(COLORES_HUELLA[h.color()]); r.g.fillRoundRect(x, y, s, s, 4, 4);
        r.g.setColor(TemaPixel.NOCHE); r.g.drawRoundRect(x, y, s, s, 4, 4);
        imagen("iconos/" + ICONOS_HUELLA[h.icono()], x + 1, y + 1, s - 2, s - 2);
    }

    private void panelBots(CazaDeBots b) {
        int x = 932;
        texto("TABLA HASH DE HUELLAS DE BOTS", x, 146, 13, r.oro, true);
        var tabla = b.tabla();
        texto(tabla.cantidad() + " redes registradas · " + tabla.capacidad() + " cubetas · carga " + String.format(Locale.ROOT, "%.2f", tabla.factorCarga()), x, 170, 12, r.tenue, false);
        var ultima = b.ultimaBusqueda();
        int y = 182;
        for (int i = 0; i < tabla.capacidad(); i++) {
            boolean marcada = ultima != null && ultima.cubeta() == i;
            rect(x, y, 26, 24, marcada ? new Color(0x8A6A1E) : TemaPixel.NOCHE);
            texto("" + i, x + (i >= 10 ? 4 : 9), y + 17, 12, r.papel, true);
            int cx = x + 32;
            var cubeta = tabla.cubeta(i);
            if (cubeta.isEmpty()) rect(cx, y + 11, 20, 2, GRIS);
            int dibujadas = 0;
            for (var e : cubeta) {
                var partes = e.clave().split("·");
                var h = new CazaDeBots.Huella(List.of(CazaDeBots.ICONOS).indexOf(partes[0]), List.of(CazaDeBots.COLORES).indexOf(partes[1]));
                if (dibujadas == 2) { texto("+" + (cubeta.size() - 2), cx, y + 16, 12, r.oro, true); break; }
                if (dibujadas == 1) { texto("→", cx - 9, y + 16, 11, r.oro, true); }
                boolean consultada = ultima != null && ultima.encontrada() && e.clave().equals(ultima.clave());
                rect(cx, y, 124, 24, consultada ? new Color(0x5A2A2E) : new Color(0x294357));
                huella(h, cx + 3, y + 3, 18);
                texto(r.ajustar(e.clave(), 96, 10, false), cx + 25, y + 11, 10, r.papel, false);
                texto(e.valor().red(), cx + 25, y + 22, 10, RenderJuego.ROJO, true);
                cx += 136; dibujadas++;
            }
            if (cubeta.size() > 1) texto("colisión", x + 32 + 136 * 2 - 4, y + 9, 8, r.tenue, false);
            y += 28;
        }
        y += 6;
        texto("ÚLTIMA CONSULTA · O(1) en promedio", x, y + 10, 11, r.tenue, true);
        parrafo(b.ultimaConsulta().isBlank() ? "Reporta o escanea: la huella se busca solo en su cubeta (encadenamiento si hay colisión)." : b.ultimaConsulta(), x, y + 30, 304, 12, r.oro);
    }

    // ═════════════════════════ Resultado ═════════════════════════
    void fin() {
        Minijuego m = j().minijuego;
        boolean victoria = m.estado() == Minijuego.Estado.VICTORIA;
        texto("MINIJUEGO · " + m.tipo().titulo.toUpperCase(Locale.ROOT), 28, 30, 14, r.oro, true);
        texto(victoria ? "¡Lo lograron!" : "No esta vez", 28, 65, 26, victoria ? r.papel : RenderJuego.ROJO, true);
        texto(m.modo().texto.toUpperCase(Locale.ROOT) + "  /  " + m.puntos() + " puntos", 28, 90, 14, r.tenue, false);
        escena(m);
        if (victoria) {
            rect(VX, VY, VW, VH, new Color(2, 40, 36, 120));
            TemaPixel.marco(r.g, 150, 250, 614, 150, new Color(0x0E3B36), VERDE_CLARO, true);
            texto("¡VICTORIA!", 190, 316, 44, VERDE_CLARO, true);
            parrafo(m.motivo(), 192, 354, 540, 16, r.papel);
        } else {
            rect(VX, VY, VW, VH, new Color(110, 8, 12, 140));
            TemaPixel.marco(r.g, 150, 250, 614, 150, new Color(0x2A0A0E), RenderJuego.ROJO, true);
            texto("Perdieron.", 190, 316, 44, RenderJuego.ROJO, true);
            parrafo(m.motivo(), 192, 354, 540, 16, r.papel);
        }
        panel(912, 114, 344, 484, new Color(0x22394D));
        int x = 936;
        texto(m.puntos() + " PUNTOS", x, 160, 29, victoria ? r.oro : RenderJuego.ROJO, true);
        int y = 196;
        for (Jugador q : Jugador.values()) {
            var a = m.avatarVista(q);
            imagen("actores/" + (q == Jugador.CIUDADANO ? "ciudadano" : "periodista") + "_00", x - 4, y - 6, 40, 40);
            texto((q == Jugador.CIUDADANO ? "Ciudadano" : "Periodista") + " (" + etiqueta(m, q) + ")", x + 42, y + 12, 14, r.papel, true);
            texto(a.acciones() + " acciones", x + 42, y + 30, 12, r.tenue, false);
            y += 46;
        }
        texto("ESTRUCTURA DE DATOS", x, y + 8, 12, r.oro, true);
        String resumen = switch (m) {
            case CortaLaCadena c -> "Grafo de " + c.red().cantidadVertices() + " vértices y " + c.red().cantidadAristas() + " aristas. El rumor hizo " + c.olas() + " olas de BFS y " + c.contagiosTotales() + " contagios.";
            case RastreaLaFuente f -> "Árbol n-ario por ronda. Fuentes halladas: " + f.rondasResueltas() + "/3 con " + f.interrogatorios() + " preguntas (subir de hijo a padre hasta la raíz).";
            case DueloPalomo d -> "AVL de chismes: " + d.desmentidos() + " eliminados con búsqueda del piso, " + d.rebotes() + " datos rebotaron (menores que el mínimo).";
            case CazaDeBots b -> "Tabla hash de " + b.tabla().capacidad() + " cubetas: " + b.reportados() + " bots reportados, " + b.falsos() + " reportes a inocentes, " + b.publicaciones() + " cadenas publicadas.";
            default -> "";
        };
        y = parrafo(resumen, x, y + 32, 300, 13, r.papel);
        String como = switch (m.tipo()) {
            case CADENA -> "Cómo funciona: BFS con varios orígenes; cada ola visita un nivel nuevo.";
            case FUENTE -> "Cómo funciona: cada nodo guarda a su padre; se sube hasta la raíz.";
            case DUELO -> "Cómo funciona: el piso se halla bajando una sola rama del AVL, O(log n).";
            case BOTS -> "Cómo funciona: h(clave) mod 11 elige la cubeta; las colisiones se encadenan.";
        };
        if (y < 470) parrafo(como, x, y + 8, 300, 12, r.tenue);
        var cap = j().capitulo;
        if (cap != null && cap.minijuego == m.tipo()) {
            if (victoria) { boton("Continuar la historia  →", "hsiguiente", 932, 502, 303, true); boton("Repetir", "mjreintentar", 932, 548, 146, false); }
            else { boton("Reintentar", "mjreintentar", 932, 502, 303, true); boton("Relato", "hcap:" + cap.name(), 932, 548, 146, false); }
            boton("Mapa", "historia", 1089, 548, 146, false);
        } else {
            boton(victoria ? "Jugar de nuevo" : "Reintentar", "mjreintentar", 932, 502, 303, true);
            boton("Otro minijuego", "minijuegos", 932, 548, 146, false); boton("Menú", "menu", 1089, 548, 146, false);
        }
        panel(24, 608, 1232, 72, new Color(0x294357));
        var h = m.historial();
        int hy = 634;
        for (String linea : h.subList(Math.max(0, h.size() - 2), h.size())) { texto(r.ajustar("• " + linea, 1190, 13, false), 42, hy, 13, hy == 634 ? r.tenue : r.oro, false); hy += 26; }
    }
}
