package alcaldedigital.cliente.interfaz;

import alcaldedigital.cliente.JuegoCliente;
import alcaldedigital.cliente.campana.Campana;
import alcaldedigital.cliente.campana.Campana.Capitulo;
import alcaldedigital.servidor.minijuegos.Minijuego;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

/**
 * Pantallas del modo historia: mapa de capítulos, relato antes de cada capítulo, epílogo y
 * la pausa de la misión 6. Usa los mismos marcos, fuente y sprites del resto del juego.
 */
final class RenderHistoria {
    private static final int SX = 24, SY = 133, SW = 866, SH = 465;
    private static final Color VERDE = new Color(0x8FD7B6), MORADO = new Color(0xB892E6), CIELO = new Color(0x2B1C46);
    private final RenderJuego r;
    RenderHistoria(RenderJuego r) { this.r = r; }

    private JuegoCliente j() { return r.juego; }
    private Campana c() { return r.juego.campana; }
    private double t() { return r.juego.movimientoReducido ? 0 : r.juego.animacion; }
    static boolean propia(JuegoCliente.Pantalla p) {
        return p == JuegoCliente.Pantalla.HISTORIA || p == JuegoCliente.Pantalla.CAPITULO || p == JuegoCliente.Pantalla.EPILOGO
            || p == JuegoCliente.Pantalla.MISION6 || p == JuegoCliente.Pantalla.MISION6_PAUSA;
    }

    // ───────────────────────── Mapa de la historia ─────────────────────────
    void mapa() {
        var camp = c();
        r.cabecera("Modo historia · La noche del Palomo", "Seis capítulos encadenados: la misión 5, cuatro minijuegos y la misión 6. Cada victoria abre el siguiente.", false);
        int y = 560;
        if (!camp.iniciada()) {
            r.boton("Nueva historia · 1 jugador  →", "hnueva:1", 28, y, 400, true);
            r.boton("Nueva historia · 2 jugadores  →", "hnueva:2", 440, y, 400, true);
        } else {
            boolean enCurso = j().capituloEnCurso();
            if (enCurso) r.boton("Volver al capítulo en curso  →", "hreanudar", 28, y, 400, true);
            else r.boton(camp.completada() ? "Ver el epílogo  →" : "Continuar: capítulo " + camp.actual().numero() + "  →", "hcontinuar", 28, y, 400, true);
            r.boton("Nueva · 1 jugador", "hnueva:1", 440, y, 196, false);
            r.boton("Nueva · 2 jugadores", "hnueva:2", 644, y, 196, false);
        }
        r.boton("Menú [Esc]", "menu", 1080, 35, 170, false);
        Capitulo[] caps = Capitulo.values();
        for (int i = 0; i < caps.length; i++) tarjeta(caps[i], 28 + (i % 3) * 410, 132 + (i / 3) * 208);
        r.rect(848, 556, 404, 2, TemaPixel.CIAN);
        if (camp.iniciada()) {
            r.texto((camp.dosJugadores() ? "DOS JUGADORES" : "UN JUGADOR") + "  ·  " + camp.superados() + " / 6 capítulos", 856, 580, 14, r.oro, true);
            r.texto(camp.puntosTotales() + " puntos  ·  " + camp.derrotasTotales() + " reintentos", 856, 604, 14, r.papel, false);
        } else {
            r.texto("Elige cuántos juegan: se usa en toda la historia.", 856, 580, 14, r.papel, false);
            r.texto("Dos jugadores = mismo teclado (WASD / flechas).", 856, 604, 14, r.tenue, false);
        }
        String nota = camp.error.isBlank() ? "Un rumor, una memoria USB, un ejército de bots y un dron en el cielo. ¿Pueden salvar las elecciones de Ciudad Nova?" : camp.error;
        r.parrafo(nota, 30, 640, 1210, 15, camp.error.isBlank() ? r.tenue : RenderJuego.ROJO);
        r.pie("Tab y Enter recorren los botones · Esc vuelve al menú · El progreso se guarda solo");
    }

    private void tarjeta(Capitulo cap, int x, int y) {
        var camp = c();
        boolean superado = camp.superado(cap), siguiente = camp.iniciada() && camp.actual() == cap, abierto = camp.disponible(cap);
        r.panel(x, y, 396, 196, new Color(0x22394D));
        Color acento = superado ? VERDE : siguiente ? r.oro : r.tenue;
        r.rect(x + 8, y + 8, 6, 180, acento);
        r.texto("CAPÍTULO " + cap.numero() + "  ·  " + cap.etiqueta.toUpperCase(Locale.ROOT), x + 26, y + 30, 13, acento, true);
        r.texto(cap.titulo, x + 26, y + 60, 22, abierto || !camp.iniciada() ? r.papel : r.tenue, true);
        r.texto(r.ajustar(cap.lugar, 280, 13, false), x + 26, y + 86, 13, r.tenue, false);
        r.texto(r.ajustar(cap.estructura, 280, 13, false), x + 26, y + 108, 13, MORADO, false);
        icono(cap, x + 312, y + 24, 64);
        String estado = superado ? "Superado · " + camp.puntos(cap) + " pts" + (camp.derrotas(cap) > 0 ? " · " + camp.derrotas(cap) + " reint." : "")
            : siguiente ? "Siguiente capítulo" : camp.iniciada() ? "Bloqueado: gana el anterior" : "Empieza una historia";
        r.texto(estado, x + 26, y + 134, 13, superado ? VERDE : siguiente ? r.oro : r.tenue, superado || siguiente);
        if (abierto) r.boton(superado ? "Repetir" : "Jugar", "hcap:" + cap.name(), x + 26, y + 144, 170, siguiente);
        else r.imagen("iconos/21_candado", x + 26, y + 150, 28, 28);
    }

    private void icono(Capitulo cap, int x, int y, int s) {
        switch (cap) {
            case M5 -> { r.imagen("actores/periodista_00", x - 18, y, s, s); r.imagen("actores/ciudadano_00", x + 10, y, s, s); }
            case CADENA -> r.imagen("iconos/23_conexion", x + 8, y + 8, s - 16, s - 16);
            case FUENTE -> r.imagen("iconos/06_fuente", x + 8, y + 8, s - 16, s - 16);
            case BOTS -> r.imagen("iconos/09_reportar", x + 8, y + 8, s - 16, s - 16);
            case DUELO -> r.imagen("actores/noti_08", x, y, s, s);
            case M6 -> r.g.drawImage(r.recursos.imagen("../mision6/helicoptero"), x - 10, y + 8, s + 20, (s + 20) / 2, null);
        }
    }

    // ───────────────────────── Relato antes de cada capítulo ─────────────────────────
    void capitulo() {
        Capitulo cap = j().capitulo != null ? j().capitulo : c().actual() != null ? c().actual() : Capitulo.M5;
        List<Campana.Linea> relato = cap.relato();
        int p = Math.max(0, Math.min(relato.size() - 1, j().paginaCapitulo));
        boolean ultima = p == relato.size() - 1, dos = c().dosJugadores();
        r.cabecera("Capítulo " + cap.numero() + " · " + cap.titulo, cap.etiqueta + "  ·  " + cap.lugar, false);
        escena(cap);
        dialogo(relato.get(p), SX + 14, SY + SH - 138, SW - 28);
        // Panel derecho: ficha del capítulo y botones (el primero recibe el foco de Enter).
        r.panel(912, SY, 344, SH, new Color(0x22394D));
        r.texto("CAPÍTULO " + cap.numero() + " DE 6", 936, 168, 14, r.oro, true);
        r.rect(932, 178, 303, 2, TemaPixel.CIAN);
        int y = r.parrafo(cap.titulo, 936, 212, 296, 22, r.papel);
        r.texto("ESTRUCTURA / MECÁNICA", 936, y + 6, 12, r.oro, true);
        y = r.parrafo(cap.estructura, 936, y + 30, 296, 14, MORADO);
        r.texto("CONTROLES · " + (dos ? "2 JUGADORES" : "1 JUGADOR"), 936, y + 6, 12, r.oro, true);
        r.parrafo(controles(cap, dos), 936, y + 30, 296, 13, r.tenue);
        r.texto((p + 1) + " / " + relato.size(), 936, 436, 13, r.tenue, false);
        if (!ultima) {
            r.boton("Continuar  →", "hsig", 932, 446, 303, true);
            r.boton("Saltar el relato", "hsaltar", 932, 494, 303, false);
        } else if (cap.minijuego != null && !dos) {
            var rec = Campana.modoRecomendado(cap);
            r.boton("Jugar como Ciudadano" + (rec == Minijuego.Modo.SOLO_CIUDADANO ? " ★" : ""), "hjugar:SOLO_CIUDADANO", 932, 446, 303, rec == Minijuego.Modo.SOLO_CIUDADANO);
            r.boton("Jugar como Periodista" + (rec == Minijuego.Modo.SOLO_PERIODISTA ? " ★" : ""), "hjugar:SOLO_PERIODISTA", 932, 494, 303, rec == Minijuego.Modo.SOLO_PERIODISTA);
        } else {
            r.boton("¡Empezar el capítulo!  →", "hjugar", 932, 446, 303, true);
            r.boton("Volver a leer", "hcap:" + cap.name(), 932, 494, 303, false);
        }
        r.boton("Mapa [Esc]", "volver", 932, 542, 303, false);
        progreso(cap);
    }

    private static String controles(Capitulo cap, boolean dos) {
        return switch (cap) {
            case M5 -> dos ? "J1 Ciudadano: WASD + E para calmar. J2 Periodista: ↑ ↓ + Enter, teclas 1–4 o clic para verificar."
                : "WASD o flechas: caminar. E: calmar vecinos. Clic o teclas 1–4: verificar noticias. Enter: la más urgente.";
            case M6 -> dos ? "J1 Juan: WASD, F disparo, Q misil, Espacio impulso. J2 Influencer: flechas, L disparo, K misil, Mayús impulso."
                : "WASD o flechas: mover. Mantén F: disparar. Q: misil (fase 2). Espacio: impulso. Enter: hablar y confirmar.";
            default -> dos ? "J1 Ciudadano: WASD + E. J2 Periodista: flechas + Enter. Cada uno tiene una habilidad distinta."
                : "WASD o flechas: caminar. E, Espacio o Enter: acción. La computadora maneja al otro personaje. ★ = recomendado.";
        };
    }

    private void progreso(Capitulo cap) {
        r.panel(24, 608, 1232, 72, new Color(0x294357));
        Capitulo[] caps = Capitulo.values();
        for (int i = 0; i < caps.length; i++) {
            int x = 42 + i * 200;
            boolean hecho = c().superado(caps[i]), aqui = caps[i] == cap;
            r.rect(x, 640, 18, 18, hecho ? VERDE : aqui ? r.oro : new Color(0x405461));
            if (i < caps.length - 1) r.rect(x + 22, 648, 172, 2, hecho ? VERDE : new Color(0x405461));
            r.texto(r.ajustar(caps[i].titulo, 170, 12, aqui), x + 24, 636, 12, aqui ? r.oro : r.tenue, aqui);
        }
        r.texto("Enter / Espacio: continuar  ·  Esc: mapa de la historia", 42, 674, 12, r.tenue, false);
    }

    /** Cuadro de diálogo con retrato, igual en los relatos y el epílogo. */
    private void dialogo(Campana.Linea l, int x, int y, int w) {
        TemaPixel.marco(r.g, x, y, w, 124, new Color(0x1A2A3D), r.oro, true);
        r.rect(x + 12, y + 12, 96, 100, new Color(0x294357));
        retrato(l.quien(), x + 16, y + 16, 88);
        r.texto(l.quien(), x + 124, y + 34, 15, r.oro, true);
        r.parrafo(l.texto(), x + 124, y + 60, w - 140, 15, r.papel);
    }

    private void retrato(String quien, int x, int y, int s) {
        String q = quien.toUpperCase(Locale.ROOT);
        String sprite = q.contains("PALOMO") ? "actores/noti_08" : q.contains("PERIODISTA") ? "actores/periodista_00"
            : q.contains("CIUDADANO") ? "actores/ciudadano_00" : q.contains("INFLUENCER") ? "actores/influencer_00"
            : q.contains("JUAN") ? "actores/candidato_00" : null;
        if (sprite != null) r.imagen(sprite, x, y, s, s);
        else r.imagen(q.contains("CÓMO") || q.contains("MISIÓN") ? "iconos/22_ayuda" : "iconos/04_libreta", x + 14, y + 14, s - 28, s - 28);
    }

    /** Ilustración de cada capítulo dentro del recuadro izquierdo. */
    private void escena(Capitulo cap) {
        var g = r.g;
        Shape antes = g.getClip();
        g.clipRect(SX, SY, SW, SH);
        double t = t();
        switch (cap) {
            case M5 -> {
                r.imagen("portada", SX - 40, SY - 30, SW + 80, SH + 60);
                r.rect(SX, SY, SW, SH, new Color(0, 20, 40, 90));
                r.imagen("actores/periodista_00", SX + 300, SY + 150, 128, 128);
                r.imagen("actores/ciudadano_00", SX + 420, SY + 150, 128, 128);
                r.imagen("actores/noti_" + "%02d".formatted(8 + (int) (t * 8) % 4), SX + 610, SY + 40 + (int) (Math.sin(t * 2) * 6), 96, 96);
                for (int i = 0; i < 5; i++) r.imagen("efectos/rumor_%02d".formatted((int) (t * 8 + i) % 8), SX + 60 + i * 150, SY + 30 + (i % 2) * 50, 56, 56);
            }
            case CADENA -> {
                r.rect(SX, SY, SW, SH, new Color(0x10223A));
                int[][] n = {{120, 80}, {260, 60}, {400, 110}, {540, 70}, {700, 100}, {180, 200}, {330, 230}, {480, 210}, {640, 220}, {780, 190}, {260, 300}, {560, 300}};
                int[][] e = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {0, 5}, {5, 6}, {6, 7}, {7, 8}, {8, 9}, {4, 9}, {2, 7}, {6, 10}, {7, 11}, {1, 6}, {3, 8}};
                int ola = (int) (t / 1.2) % 5;
                int[] nivel = {0, 1, 2, 3, 4, 1, 2, 3, 4, 4, 3, 4};
                g.setStroke(new BasicStroke(3));
                for (int[] a : e) { g.setColor(new Color(0x3E6A8A)); g.drawLine(SX + n[a[0]][0], SY + n[a[0]][1], SX + n[a[1]][0], SY + n[a[1]][1]); }
                for (int i = 0; i < n.length; i++) {
                    boolean rumor = nivel[i] <= ola, blindada = i == 6 || i == 8;
                    Color col = blindada ? new Color(0x3FAE6A) : rumor ? RenderJuego.ROJO : new Color(0xACD9DF);
                    g.setColor(col); g.fillOval(SX + n[i][0] - 16, SY + n[i][1] - 16, 32, 32);
                    if (blindada) { g.setColor(Color.WHITE); g.drawOval(SX + n[i][0] - 21, SY + n[i][1] - 21, 42, 42); }
                }
                r.texto("ola " + (ola + 1) + " del BFS", SX + 30, SY + 30, 14, r.oro, true);
                r.imagen("iconos/05_usb", SX + 760, SY + 20, 48, 48);
            }
            case FUENTE -> {
                r.rect(SX, SY, SW, SH, new Color(0x173328));
                int[][] n = {{433, 50}, {250, 130}, {620, 130}, {150, 220}, {340, 220}, {540, 220}, {720, 220}, {280, 300}};
                int[] padre = {-1, 0, 0, 1, 1, 2, 2, 4};
                int sube = (int) (t / .8) % 4;
                int[] camino = {7, 4, 1, 0};
                g.setStroke(new BasicStroke(3));
                for (int i = 1; i < n.length; i++) { g.setColor(new Color(0x5E8C6A)); g.drawLine(SX + n[i][0], SY + n[i][1], SX + n[padre[i]][0], SY + n[padre[i]][1]); }
                for (int i = 0; i < n.length; i++) {
                    boolean marcado = false; for (int k = 0; k <= sube; k++) marcado |= camino[k] == i;
                    if (i == 0) r.imagen("actores/noti_08", SX + n[i][0] - 28, SY + n[i][1] - 30, 56, 56);
                    else r.imagen(i % 2 == 0 ? "actores/influencer_00" : "actores/candidato_00", SX + n[i][0] - 24, SY + n[i][1] - 28, 48, 48);
                    if (marcado) { g.setColor(r.oro); g.drawOval(SX + n[i][0] - 30, SY + n[i][1] - 32, 60, 60); }
                }
                r.texto("«¿Quién te lo mandó?»  →  sube al padre", SX + 30, SY + 30, 14, r.oro, true);
            }
            case BOTS -> {
                r.rect(SX, SY, SW, SH, new Color(0x1E3A2C));
                for (int i = 0; i < 18; i++) {
                    int x = SX + 70 + (i % 6) * 128, y = SY + 50 + (i / 6) * 100;
                    boolean bot = i % 4 == 1;
                    r.imagen(i % 2 == 0 ? "actores/candidato_00" : "actores/influencer_00", x, y + 20, 56, 56);
                    r.imagen(bot ? "iconos/03_megafono" : "iconos/01_celular", x + 16, y, 24, 24);
                    if (bot && (int) (t * 2) % 2 == 0) { r.g.setColor(RenderJuego.ROJO); r.g.drawRect(x - 4, y - 4, 64, 84); }
                }
                r.texto("h(huella) mod 11  →  ¿está en la tabla?", SX + 30, SY + 30, 14, r.oro, true);
            }
            case DUELO -> {
                r.imagen("portada", SX - 40, SY - 30, SW + 80, SH + 60);
                r.rect(SX, SY, SW, SH, new Color(30, 0, 30, 120));
                int cx = SX + 433, cy = SY + 140;
                for (int i = 0; i < 7; i++) {
                    double a = t * .8 + i * Math.PI * 2 / 7;
                    int x = cx + (int) (Math.cos(a) * 150), y = cy + (int) (Math.sin(a) * 70);
                    r.panel(x - 22, y - 16, 44, 30, new Color(0x6B3FA0));
                    r.texto("" + (20 + i * 11), x - 12, y + 6, 13, r.papel, true);
                }
                r.imagen("actores/noti_%02d".formatted(8 + (int) (t * 8) % 4), cx - 64, cy - 64, 128, 128);
                r.imagen("actores/periodista_00", SX + 280, SY + 250, 88, 88);
                r.imagen("actores/ciudadano_00", SX + 500, SY + 250, 88, 88);
            }
            case M6 -> {
                r.g.drawImage(r.recursos.imagen("../mision6/cielo"), SX - 30 + (int) (Math.sin(t * .2) * 20), SY, SW + 60, SH, null);
                r.rect(SX, SY, SW, SH, new Color(28, 18, 51, 60));
                BufferedImage atlas = r.recursos.imagen("../mision6/dron_atlas");
                int cw = atlas.getWidth() / 5, ch = atlas.getHeight() / 3, f = (int) (t * 10) % 5;
                r.g.drawImage(atlas.getSubimage(f * cw, 0, cw, ch), SX + 560, SY + 30 + (int) (Math.sin(t * 2) * 10), 220, 220 * ch / cw, null);
                r.g.drawImage(r.recursos.imagen("../mision6/helicoptero"), SX + 90, SY + 110 + (int) (Math.sin(t * 3) * 4), 260, 130, null);
            }
        }
        g.setClip(antes);
        g.setStroke(new BasicStroke(2));
        g.setColor(r.papel); g.drawRect(SX, SY, SW, SH);
    }

    // ───────────────────────── Epílogo ─────────────────────────
    void epilogo() {
        var camp = c();
        List<Campana.Linea> lineas = camp.epilogo();
        int p = j().paginaEpilogo;
        r.cabecera("Epílogo · Ciudad Nova respira", camp.completada() ? camp.rango() : "Termina la misión 6 para ver el final completo.", false);
        var g = r.g;
        Shape antes = g.getClip();
        g.clipRect(SX, SY, SW, SH);
        g.drawImage(r.recursos.imagen("../mision6/casa"), SX, SY, SW, SH, null);
        r.rect(SX, SY, SW, SH, new Color(10, 10, 30, 70));
        g.setClip(antes);
        g.setStroke(new BasicStroke(2)); g.setColor(r.papel); g.drawRect(SX, SY, SW, SH);
        if (p < lineas.size()) dialogo(lineas.get(p), SX + 14, SY + SH - 138, SW - 28);
        else {
            TemaPixel.marco(g, SX + 120, SY + 120, SW - 240, 200, new Color(0x1A2A3D), r.oro, true);
            r.texto("FIN", SX + 160, SY + 190, 48, r.oro, true);
            r.texto(camp.rango(), SX + 160, SY + 240, 24, r.papel, true);
            r.parrafo("Gracias por jugar. Antes de compartir, verifica.", SX + 160, SY + 280, SW - 320, 16, r.tenue);
        }
        r.panel(912, SY, 344, SH, new Color(0x22394D));
        r.texto("RESUMEN DE LA HISTORIA", 936, 168, 14, r.oro, true);
        r.rect(932, 178, 303, 2, TemaPixel.CIAN);
        int y = 206;
        for (Capitulo cap : Capitulo.values()) {
            r.texto(r.ajustar(cap.numero() + ". " + cap.titulo, 200, 13, false), 936, y, 13, camp.superado(cap) ? r.papel : r.tenue, false);
            r.texto(camp.superado(cap) ? camp.puntos(cap) + " pts" : "—", 1150, y, 13, camp.superado(cap) ? VERDE : r.tenue, true);
            y += 24;
        }
        r.rect(932, y - 8, 303, 2, TemaPixel.CIAN);
        r.texto("TOTAL: " + camp.puntosTotales() + " puntos", 936, y + 18, 16, r.oro, true);
        r.texto("Reintentos: " + camp.derrotasTotales(), 936, y + 42, 13, r.tenue, false);
        String ruta = "ESCUCHA".equals(camp.ruta()) ? "Escuchar y adaptar" : "CONVICCION".equals(camp.ruta()) ? "Convicción y diálogo" : "—";
        r.texto("Entrevista: " + ruta, 936, y + 64, 13, MORADO, false);
        if (p < lineas.size()) r.boton("Continuar  →", "esig", 932, 494, 303, true);
        else { r.boton("Volver al menú", "menu", 932, 494, 303, true); }
        r.boton("Mapa de la historia", "historia", 932, 542, 303, false);
        r.panel(24, 608, 1232, 72, new Color(0x294357));
        r.texto(p < lineas.size() ? (p + 1) + " / " + lineas.size() + "   ·   Enter / Espacio: continuar" : "Fin de la historia   ·   Enter: volver al menú", 42, 640, 14, r.tenue, false);
        r.texto("Alcalde Digital · La noche del Palomo — misión 5, cuatro minijuegos de estructuras de datos y misión 6.", 42, 666, 13, r.oro, false);
    }

    // ───────────────────────── Misión 6: pausa y respaldo sin Processing ─────────────────────────
    void mision6() {
        r.rect(0, 0, 1280, 720, CIELO);
        r.texto("Misión 6 · Cielos de campaña", 60, 120, 34, r.papel, true);
        r.parrafo("Esta misión se dibuja directamente con Processing en la ventana del juego.", 60, 170, 900, 18, r.tenue);
    }

    void pausa6() {
        r.rect(0, 0, 1280, 720, CIELO);
        r.cabecera("Misión 6 · Pausa", "La partida se guardó automáticamente (con copia de seguridad).", false);
        r.boton("Continuar [Esc]", "seguir6", 932, 446, 303, true);
        r.boton("Guardar y salir", "salir6", 932, 494, 303, false);
        r.panel(24, SY, 866, SH, new Color(0x22394D));
        r.texto("CONTROLES", 48, 172, 16, r.oro, true);
        String[][] filas = {
            {"Acción", "Un jugador", "Dos jugadores"},
            {"Mover", "WASD / flechas", "J1 WASD · J2 flechas"},
            {"Disparar (mantener)", "F", "J1 F · J2 L"},
            {"Misil (fase 2 y 3)", "Q", "J1 Q · J2 K o M"},
            {"Impulso", "Espacio", "J1 Espacio · J2 Mayús"},
            {"Hablar / confirmar", "Enter o E", "Enter o E"},
            {"Pausa", "Esc o P", "Esc o P"}};
        int y = 214;
        for (int i = 0; i < filas.length; i++) {
            boolean cab = i == 0;
            r.texto(filas[i][0], 48, y, 15, cab ? r.oro : r.papel, cab);
            r.texto(filas[i][1], 330, y, 15, cab ? r.oro : r.papel, cab);
            r.texto(filas[i][2], 560, y, 15, cab ? r.oro : r.papel, cab);
            y += 38;
        }
        r.parrafo("Toca los corazones para recuperar vida. En la fase 3 el dron tiene un halo: no lo toques.", 48, y + 10, 800, 15, r.tenue);
        r.panel(912, SY, 344, 300, new Color(0x22394D));
        var m = j().seis == null ? null : j().seis.motor();
        r.texto("ESTADO", 936, 172, 14, r.oro, true);
        if (m != null) {
            r.texto("Escena: " + m.fase.name().toLowerCase(Locale.ROOT).replace('_', ' '), 936, 206, 14, r.papel, false);
            r.texto("Dron: " + m.energia + " / 180 · fase " + m.etapa(), 936, 232, 14, r.papel, false);
            r.texto("Vida J1: " + m.naves[0].vida + (m.modo == alcaldedigital.cliente.mision6.MisionSeisMotor.Modo.DUO ? "  ·  J2: " + m.naves[1].vida : ""), 936, 258, 14, r.papel, false);
        }
        r.pie(j().enHistoria() ? "Capítulo 6 de la historia · Esc continúa" : "Misión 6 suelta · Esc continúa");
    }
}
