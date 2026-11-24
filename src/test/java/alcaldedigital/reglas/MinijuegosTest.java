package alcaldedigital.reglas;

import alcaldedigital.compartido.estructuras.grafos.GrafoRed;
import alcaldedigital.servidor.minijuegos.*;
import alcaldedigital.servidor.minijuegos.Minijuego.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reglas de los cuatro minijuegos: reloj, modos, estructuras y finales. */
class MinijuegosTest {
    private static final double DT = 1 / 60.0;

    private static void avanzar(Minijuego m, double segundos) {
        for (double t = 0; t < segundos && !m.terminada(); t += DT) m.actualizar(DT);
    }
    private static Minijuego enCurso(Tipo tipo, Modo modo, long semilla) {
        Minijuego m = Minijuego.crear(tipo, modo, semilla);
        avanzar(m, Minijuego.PREPARACION + .05);
        assertEquals(Estado.EN_CURSO, m.estado());
        return m;
    }
    /** Lleva a un personaje humano hasta (x, y) caminando con la entrada normal. */
    private static void caminarHasta(Minijuego m, Jugador q, float x, float y) {
        for (int i = 0; i < 60 * 10; i++) {
            var a = m.avatarVista(q);
            float dx = x - a.x(), dy = y - a.y();
            if (Math.hypot(dx, dy) < 3) break;
            m.entrada(q, Math.abs(dx) > 1.5 ? Math.signum(dx) : 0, Math.abs(dy) > 1.5 ? Math.signum(dy) : 0, false);
            m.actualizar(DT);
        }
        m.entrada(q, 0, 0, false);
    }
    private static void pulsar(Minijuego m, Jugador q) {
        m.entrada(q, 0, 0, true); m.actualizar(DT);
        m.entrada(q, 0, 0, false); m.actualizar(DT);
    }

    @Test void todosEmpiezanConCuentaAtrasYElRelojNoCorreAntes() {
        for (Tipo t : Tipo.values()) for (Modo modo : Modo.values()) {
            Minijuego m = Minijuego.crear(t, modo, 3);
            assertEquals(Estado.PREPARACION, m.estado());
            assertEquals(3, m.cuentaAtras());
            avanzar(m, 1);
            assertEquals(0, m.transcurrido(), 1e-9, t + " no debe gastar tiempo en la cuenta atrás");
            avanzar(m, 2.1);
            assertEquals(Estado.EN_CURSO, m.estado());
            assertEquals(m.duracion(), m.restante() + m.transcurrido(), 1e-9);
        }
    }

    @Test void losModosDecidenQuienEsHumanoYQuienLoManejaLaComputadora() {
        var m = Minijuego.crear(Tipo.CADENA, Modo.SOLO_CIUDADANO, 1);
        assertTrue(m.humano(Jugador.CIUDADANO)); assertFalse(m.humano(Jugador.PERIODISTA));
        m = Minijuego.crear(Tipo.CADENA, Modo.SOLO_PERIODISTA, 1);
        assertFalse(m.humano(Jugador.CIUDADANO)); assertTrue(m.humano(Jugador.PERIODISTA));
        m = Minijuego.crear(Tipo.CADENA, Modo.DUO, 1);
        assertTrue(m.humano(Jugador.CIUDADANO) && m.humano(Jugador.PERIODISTA));
    }

    @Test void enParejaCadaJugadorMueveSoloASuPersonaje() {
        for (Tipo t : Tipo.values()) {
            var m = enCurso(t, Modo.DUO, 4);
            var c0 = m.avatarVista(Jugador.CIUDADANO); var p0 = m.avatarVista(Jugador.PERIODISTA);
            for (int i = 0; i < 30; i++) { m.entrada(Jugador.CIUDADANO, -1, 0, false); m.entrada(Jugador.PERIODISTA, 1, 0, false); m.actualizar(DT); }
            assertTrue(m.avatarVista(Jugador.CIUDADANO).x() < c0.x(), t + ": el Ciudadano va a la izquierda");
            assertTrue(m.avatarVista(Jugador.PERIODISTA).x() > p0.x(), t + ": el Periodista va a la derecha");
        }
    }

    @Test void laEntradaParaElPersonajeAutomaticoSeIgnora() {
        var m = enCurso(Tipo.BOTS, Modo.SOLO_CIUDADANO, 2);
        assertFalse(m.avatarVista(Jugador.PERIODISTA).humano());
        // Mantener «escanear» para el Periodista automático no debe forzar un escaneo inmediato.
        for (int i = 0; i < 3; i++) { m.entrada(Jugador.PERIODISTA, 1, 1, i % 2 == 0); m.entrada(Jugador.CIUDADANO, 0, 0, false); m.actualizar(DT); }
        assertEquals(0, m.avatarVista(Jugador.PERIODISTA).acciones());
    }

    @Test void sinJugarSePierdeEnTodos() {
        for (Tipo t : Tipo.values()) {
            var m = Minijuego.crear(t, Modo.DUO, 11);
            avanzar(m, 200);
            assertTrue(m.terminada(), t + " debe terminar");
            assertEquals(Estado.DERROTA, m.estado(), t + ": dos jugadores inactivos pierden");
            assertFalse(m.motivo().isBlank());
        }
    }

    @Test void elAyudanteSoloNoGanaPorSiMismoEnLaMayoriaDePartidas() {
        for (Tipo t : Tipo.values()) for (Modo modo : new Modo[]{Modo.SOLO_CIUDADANO, Modo.SOLO_PERIODISTA}) {
            int victorias = 0;
            for (long s = 0; s < 10; s++) { var m = Minijuego.crear(t, modo, 500 + s); avanzar(m, 200); if (m.estado() == Estado.VICTORIA) victorias++; }
            assertTrue(victorias <= 5, t + " " + modo + ": con el humano quieto gana " + victorias + "/10");
        }
    }

    // ───────────── Corta la cadena ─────────────
    @Test void cadenaElRumorAvanzaUnNivelDeBFSPorOla() {
        var c = (CortaLaCadena) enCurso(Tipo.CADENA, Modo.DUO, 8);
        assertTrue(c.red().esConexo(), "La red de Civitas es conexa");
        int[] antes = c.distancias();
        Set<Integer> frontera = new HashSet<>(c.frontera());
        assertFalse(frontera.isEmpty());
        for (int i = 0; i < antes.length; i++) assertEquals(antes[i] == 1, frontera.contains(i));
        avanzar(c, c.proximaOla() + .02);
        for (int i : frontera) assertEquals(CortaLaCadena.EstadoNodo.CONTAGIADO, c.estadoNodo(i), "La frontera cae en la ola");
        for (int i = 0; i < antes.length; i++)
            if (antes[i] >= 2) assertNotEquals(CortaLaCadena.EstadoNodo.CONTAGIADO, c.estadoNodo(i), "Más lejos no se contagia aún");
    }

    @Test void cadenaBlindarCortaElCaminoDelRumor() {
        var c = (CortaLaCadena) enCurso(Tipo.CADENA, Modo.DUO, 8);
        int objetivo = c.frontera().get(0);
        var n = c.nodos().get(objetivo);
        caminarHasta(c, Jugador.CIUDADANO, n.x(), n.y());
        pulsar(c, Jugador.CIUDADANO);
        avanzar(c, .6);
        assertEquals(CortaLaCadena.EstadoNodo.BLINDADO, c.estadoNodo(objetivo));
        assertEquals(GrafoRed.INALCANZABLE, c.distancias()[objetivo], "Una cuenta blindada no se visita en el BFS");
        assertTrue(c.puntos() >= 10);
    }

    @Test void cadenaElPeriodistaDesmienteRapidoUnaCuentaContagiada() {
        var c = (CortaLaCadena) enCurso(Tipo.CADENA, Modo.DUO, 8);
        int contagiada = -1;
        for (int i = 0; i < c.red().cantidadVertices(); i++) if (c.estadoNodo(i) == CortaLaCadena.EstadoNodo.CONTAGIADO) contagiada = i;
        var n = c.nodos().get(contagiada);
        caminarHasta(c, Jugador.PERIODISTA, n.x(), n.y());
        pulsar(c, Jugador.PERIODISTA);
        avanzar(c, .7);
        assertEquals(CortaLaCadena.EstadoNodo.BLINDADO, c.estadoNodo(contagiada));
    }

    // ───────────── Rastrea la fuente ─────────────
    @Test void fuentePreguntarSubeAlPadreHastaLaRaiz() {
        var f = (RastreaLaFuente) enCurso(Tipo.FUENTE, Modo.DUO, 21);
        var arbol = f.arbol();
        int inicio = f.inicio();
        assertTrue(arbol.esHoja(inicio));
        List<Integer> esperado = arbol.caminoARaiz(inicio);
        int paso = 0;
        for (int intento = 0; intento < 40 && f.rondasResueltas() == 0; intento++) {
            if (f.silenciada() >= 0) {   // si el Palomo interrumpe, se le espanta
                var pal = f.palomo();
                caminarHasta(f, Jugador.PERIODISTA, pal.x(), pal.y() + 20);
                pulsar(f, Jugador.PERIODISTA);
                continue;
            }
            int id = f.buscado();
            assertEquals((int) esperado.get(paso), id, "Se busca al siguiente ancestro");
            hablarCon(f, Jugador.PERIODISTA, id);
            if (f.buscado() == id) continue;            // la interrumpieron: se repite
            if (f.rondasResueltas() == 0) {
                assertEquals(Integer.valueOf(arbol.profundidad(id)), f.profundidadConocida(id), "El Periodista descubre la profundidad");
                assertEquals(arbol.padre(id), f.buscado(), "Ahora se busca a su padre");
            }
            paso++;
        }
        assertEquals(1, f.rondasResueltas());
        assertEquals(esperado.size(), paso, "Se recorrió todo el camino hasta la raíz");
        assertEquals((int) esperado.get(esperado.size() - 1), f.ultimoArbolResuelto().raiz());
    }

    /** Camina siguiendo a la persona (que pasea) y le pregunta. */
    private static void hablarCon(RastreaLaFuente f, Jugador q, int id) {
        for (int i = 0; i < 60 * 8; i++) {
            var p = f.personas().get(id); var a = f.avatarVista(q);
            float dx = p.x() - a.x(), dy = p.y() + 4 - a.y();
            if (Math.hypot(dx, dy) < 6) break;
            f.entrada(q, Math.abs(dx) > 1.5 ? Math.signum(dx) : 0, Math.abs(dy) > 1.5 ? Math.signum(dy) : 0, false);
            f.actualizar(DT);
        }
        pulsar(f, q);
        avanzar(f, 1.0);
    }

    @Test void fuentePreguntarAQuienNoLoRecibioEsUnError() {
        var f = (RastreaLaFuente) enCurso(Tipo.FUENTE, Modo.DUO, 21);
        var ajena = f.personas().stream().filter(p -> !f.arbol().contiene(p.id())).findFirst().orElseThrow();
        hablarCon(f, Jugador.CIUDADANO, ajena.id());
        assertEquals(1, f.errores());
        assertEquals(RastreaLaFuente.Marca.DESCARTADO, f.personas().get(ajena.id()).marca());
    }

    // ───────────── Duelo contra el Palomo ─────────────
    @Test void dueloElMedidorSubeYBajaYRespetaElMaximoDeCadaPersonaje() {
        int maxC = 0, maxP = 0;
        for (double t = 0; t < 4; t += .01) {
            maxC = Math.max(maxC, DueloPalomo.fuerza(Jugador.CIUDADANO, t));
            maxP = Math.max(maxP, DueloPalomo.fuerza(Jugador.PERIODISTA, t));
        }
        assertEquals(DueloPalomo.MAX_CIUDADANO, maxC);
        assertEquals(DueloPalomo.MAX_PERIODISTA, maxP);
        assertEquals(0, DueloPalomo.fuerza(Jugador.CIUDADANO, 0));
        assertTrue(DueloPalomo.fuerza(Jugador.CIUDADANO, .5) > DueloPalomo.fuerza(Jugador.CIUDADANO, .2));
    }

    @Test void dueloUnAciertoEliminaElPisoDelEscudo() {
        var d = (DueloPalomo) enCurso(Tipo.DUELO, Modo.DUO, 9);
        int antes = d.escudo().cantidad();
        List<Integer> fuerzas = new ArrayList<>();
        for (var p : d.escudo().recorrer("inorden")) fuerzas.add(p.riesgo());
        // Mantener hasta una carga alta, alineado con el Palomo, y soltar.
        Jugador q = Jugador.PERIODISTA;
        for (int i = 0; i < 600 && d.ultimoGolpe() == null && !d.terminada(); i++) {
            var yo = d.avatarVista(q); var pal = d.palomo();
            float dx = Math.abs(pal.x() - yo.x()) > 4 ? Math.signum(pal.x() - yo.x()) : 0;
            boolean soltar = d.carga(q) >= 60;
            d.entrada(q, dx, 0, !soltar);
            d.entrada(Jugador.CIUDADANO, -1, 0, false);
            d.actualizar(DT);
        }
        var golpe = d.ultimoGolpe();
        assertNotNull(golpe, "El dato llegó al Palomo");
        assertFalse(golpe.danio());
        Integer piso = fuerzas.stream().filter(f -> f <= golpe.fuerza()).max(Integer::compare).orElse(null);
        if (piso == null) assertTrue(golpe.fallo());
        else {
            assertEquals((int) piso, golpe.eliminado().riesgo(), "Se elimina el piso de la fuerza del dato");
            assertTrue(d.escudo().cantidad() <= antes);
            assertNull(d.escudo().buscar(golpe.eliminado().id()));
        }
    }

    @Test void dueloLosChismesQuitanCorazonesSiNadieEsquiva() {
        var d = (DueloPalomo) enCurso(Tipo.DUELO, Modo.DUO, 9);
        int corazones = d.corazones();
        avanzar(d, 8);
        assertTrue(d.corazones() < corazones);
    }

    // ───────────── Caza de bots ─────────────
    @Test void botsSeIdentificanConsultandoLaTablaHash() {
        var b = (CazaDeBots) enCurso(Tipo.BOTS, Modo.DUO, 13);
        assertEquals(1, b.tabla().cantidad(), "Empieza con una red registrada");
        for (var c : b.cuentas()) assertEquals(c.bot(), b.tabla().contiene(c.huella().clave()), "Bot ⇔ huella en la tabla");
        var bot = b.cuentas().stream().filter(CazaDeBots.CuentaVista::bot).findFirst().orElseThrow();
        int reportados = b.reportados();
        for (int i = 0; i < 300 && b.reportados() == reportados; i++) {
            var objetivo = b.cuentas().stream().filter(c -> c.id() == bot.id()).findFirst().orElseThrow();
            var yo = b.avatarVista(Jugador.CIUDADANO);
            float dx = objetivo.x() - yo.x(), dy = objetivo.y() - yo.y();
            boolean cerca = Math.hypot(dx, dy) < 10;
            b.entrada(Jugador.CIUDADANO, cerca ? 0 : Math.signum(dx), cerca ? 0 : Math.signum(dy), cerca && i % 2 == 0);
            b.actualizar(DT);
        }
        assertEquals(reportados + 1, b.reportados());
        assertTrue(b.ultimaBusqueda().encontrada());
    }

    @Test void botsReportarAUnInocenteSubeLaDesinformacion() {
        var b = (CazaDeBots) enCurso(Tipo.BOTS, Modo.DUO, 13);
        var inocente = b.cuentas().stream().filter(c -> !c.bot()).findFirst().orElseThrow();
        int falsos = b.falsos();
        for (int i = 0; i < 400 && b.falsos() == falsos; i++) {
            var objetivo = b.cuentas().stream().filter(c -> c.id() == inocente.id()).findFirst().orElseThrow();
            var yo = b.avatarVista(Jugador.CIUDADANO);
            float dx = objetivo.x() - yo.x(), dy = objetivo.y() - yo.y();
            boolean cerca = Math.hypot(dx, dy) < 8;
            b.entrada(Jugador.CIUDADANO, cerca ? 0 : Math.signum(dx), cerca ? 0 : Math.signum(dy), cerca && i % 2 == 0);
            b.actualizar(DT);
        }
        assertEquals(falsos + 1, b.falsos());
        assertFalse(b.ultimaBusqueda().encontrada());
    }

    @Test void botsElPalomoRegistraRedesNuevasEnLaTabla() {
        var b = (CazaDeBots) enCurso(Tipo.BOTS, Modo.SOLO_PERIODISTA, 13);
        avanzar(b, 20);
        assertTrue(b.tabla().cantidad() >= 2 || b.terminada());
    }
}
