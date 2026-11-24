package alcaldedigital.reglas;

import alcaldedigital.compartido.modelo.Publicacion;
import alcaldedigital.servidor.misiones.Mision5;
import alcaldedigital.servidor.misiones.Mision5.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Mision5Test {
    private static final double DT = 1 / 60.0;

    private static void avanzar(Mision5 m, double segundos) {
        for (double t = 0; t < segundos && !m.terminada(); t += DT) m.actualizar(DT);
    }
    private static Mision5 enCurso(Modo modo, long semilla) {
        Mision5 m = new Mision5(modo, semilla);
        avanzar(m, Mision5.PREPARACION + .1);
        assertEquals(Estado.EN_CURSO, m.estado());
        return m;
    }
    /** Jugador constante: verifica la noticia más urgente y camina hacia el vecino más asustado. */
    private static void jugarUnCuadro(Mision5 m) {
        if (!m.verificando() && !m.noticias().isEmpty()) m.verificar(0);
        var jug = m.jugador();
        if (!jug.hablando()) {
            VecinoVista objetivo = null;
            for (var v : m.vecinos()) if (v.conducta() == Conducta.ALTERADO && (objetivo == null || v.panico() > objetivo.panico())) objetivo = v;
            if (objetivo != null) {
                float dx = objetivo.x() - jug.x(), dy = objetivo.y() - jug.y();
                double d = Math.hypot(dx, dy);
                if (d > 20) m.mover((float) (dx / d * 70 * DT), (float) (dy / d * 70 * DT));
                else m.interactuar();
            }
        }
        m.actualizar(DT);
    }

    @Test void empiezaConCuentaAtrasYElRelojNoCorreAntes() {
        var m = new Mision5(Modo.SOLO, 1);
        assertEquals(Estado.PREPARACION, m.estado());
        assertEquals(3, m.cuentaAtras());
        avanzar(m, 1);
        assertEquals(0, m.transcurrido(), 1e-9);
        assertFalse(m.verificar(0));
        assertFalse(m.interactuar());
        avanzar(m, 2.2);
        assertEquals(Estado.EN_CURSO, m.estado());
        assertEquals(70, m.restante() + m.transcurrido(), 1e-9);
    }

    @Test void lasNoticiasSeOrdenanPorPrioridadConElAVL() {
        var m = enCurso(Modo.DUO, 7);
        avanzar(m, 12);
        List<Publicacion> orden = m.radar().recorrer("inverso");
        assertTrue(orden.size() >= 2);
        for (int i = 1; i < orden.size(); i++) assertTrue(orden.get(i - 1).clave().compareTo(orden.get(i).clave()) > 0);
        var vistas = m.noticias();
        assertTrue(vistas.size() <= Mision5.VISIBLES);
        for (int i = 0; i < vistas.size(); i++) assertEquals(orden.get(i).id(), vistas.get(i).id());
        assertEquals(m.activas(), m.radar().cantidad());
        assertEquals(Math.max(0, orden.size() - Mision5.VISIBLES), m.enCola());
    }

    @Test void unaNoticiaFalsaSeViralizaYSubeDePrioridad() {
        var m = enCurso(Modo.SOLO, 3);
        java.util.Map<Long, Integer> riesgoInicial = new java.util.HashMap<>();
        boolean subio = false;
        for (double t = 0; t < 20 && !m.terminada(); t += DT) {
            m.actualizar(DT);
            for (Publicacion p : m.radar().recorrer("inorden")) if (!p.estado().equals("Viralizándose")) riesgoInicial.putIfAbsent(p.id(), p.riesgo());
            for (var n : m.noticias()) {
                Integer antes = riesgoInicial.get(n.id());
                if (antes != null && n.viral()) {
                    subio = true;
                    assertTrue(n.prioridad() == Math.min(100, antes + 25), "La reinserción sube el riesgo 25 puntos");
                    assertEquals(n.prioridad(), m.radar().buscar(n.id()).riesgo());
                }
            }
        }
        assertTrue(subio, "Alguna noticia falsa debe viralizarse");
    }

    @Test void verificarDesmienteLaFalsaYSacaLaNoticiaDelIndice() {
        var m = enCurso(Modo.SOLO, 11);
        avanzar(m, .5);
        int antes = m.radar().cantidad();
        long id = m.noticias().get(0).id();
        assertTrue(m.verificar(0));
        assertTrue(m.verificando());
        assertFalse(m.verificar(0), "Solo una verificación a la vez");
        avanzar(m, 1.3);
        assertFalse(m.verificando());
        assertNull(m.radar().buscar(id));
        assertTrue(m.radar().cantidad() <= antes);
        assertTrue(m.desmentidas() == 1 || m.puntos() == 2, "Falsa: +10 y desmentida; verdadera: +2 sin desmentir");
        assertFalse(m.cierres().isEmpty());
    }

    @Test void verificarPosicionInvalidaOFueraDeCursoNoHaceNada() {
        var m = enCurso(Modo.SOLO, 5);
        assertFalse(m.verificar(-1));
        assertFalse(m.verificar(99));
    }

    @Test void calmarExigeCercaniaYTiempoDeConversacion() {
        var m = enCurso(Modo.SOLO, 2);
        assertFalse(m.interactuar(), "Sin vecino cerca no hay conversación");
        int intentos = 0;
        while (!m.jugador().hablando() && intentos++ < 60 * 20) jugarUnCuadro(m);
        assertTrue(m.jugador().hablando());
        assertEquals(0, m.calmados());
        avanzar(m, 1.0);
        assertEquals(0, m.calmados(), "Calmar toma más de un segundo");
        avanzar(m, .8);
        assertEquals(1, m.calmados());
        assertTrue(m.puntos() >= 10);
    }

    @Test void alejarseInterrumpeLaConversacion() {
        var m = enCurso(Modo.SOLO, 2);
        int intentos = 0;
        while (!m.jugador().hablando() && intentos++ < 60 * 20) jugarUnCuadro(m);
        assertTrue(m.jugador().hablando());
        for (int i = 0; i < 90; i++) { m.mover(0, i % 2 == 0 ? 3 : -3); m.mover(3, 0); m.actualizar(DT); }
        assertEquals(0, m.calmados());
    }

    @Test void elJugadorNoSaleDeLaCalle() {
        var m = enCurso(Modo.SOLO, 4);
        for (int i = 0; i < 400; i++) m.mover(-4, -4);
        assertEquals(Mision5.X_MIN, m.jugador().x(), 1e-4);
        assertEquals(Mision5.Y_MIN, m.jugador().y(), 1e-4);
        for (int i = 0; i < 400; i++) m.mover(4, 4);
        assertEquals(Mision5.X_MAX, m.jugador().x(), 1e-4);
        assertEquals(Mision5.Y_MAX, m.jugador().y(), 1e-4);
    }

    @Test void quedarseInmovilPierdeElPanicoColapsaLaCalle() {
        for (Modo modo : Modo.values()) {
            var m = enCurso(modo, 9);
            avanzar(m, 80);
            assertEquals(Estado.DERROTA, m.estado());
            assertEquals(Motivo.PANICO, m.motivo());
            assertTrue(m.panico() >= 100);
            assertFalse(m.mensaje().isBlank());
        }
    }

    @Test void soloCalmarSinDesmentirTerminaEnDerrota() {
        var m = enCurso(Modo.SOLO, 21);
        for (int i = 0; i < 60 * 80 && !m.terminada(); i++) {
            var jug = m.jugador();
            if (!jug.hablando()) {
                VecinoVista objetivo = null;
                for (var v : m.vecinos()) if (v.conducta() == Conducta.ALTERADO && (objetivo == null || v.panico() > objetivo.panico())) objetivo = v;
                if (objetivo != null) {
                    float dx = objetivo.x() - jug.x(), dy = objetivo.y() - jug.y();
                    double d = Math.hypot(dx, dy);
                    if (d > 20) m.mover((float) (dx / d * 70 * DT), (float) (dy / d * 70 * DT)); else m.interactuar();
                }
            }
            m.actualizar(DT);
        }
        assertEquals(Estado.DERROTA, m.estado());
        assertEquals(0, m.desmentidas());
        assertTrue(m.motivo() == Motivo.DESINFORMACION || m.motivo() == Motivo.TIEMPO || m.motivo() == Motivo.PANICO);
    }

    @Test void unJugadorAtentoGanaYDejaPistaYPuntos() {
        int victorias = 0;
        for (long semilla = 1; semilla <= 6; semilla++) {
            for (Modo modo : Modo.values()) {
                var m = enCurso(modo, semilla);
                for (int i = 0; i < 60 * 75 && !m.terminada(); i++) jugarUnCuadro(m);
                if (m.estado() == Estado.VICTORIA) {
                    victorias++;
                    assertEquals(Motivo.METAS, m.motivo());
                    assertTrue(m.desmentidas() >= m.metaDesmentidas());
                    assertTrue(m.calmados() >= m.metaCalmados());
                    assertTrue(m.panico() < 100 && m.desinformacion() < 100);
                    assertTrue(m.restante() > 0, "Se gana a tiempo");
                    assertTrue(m.puntos() >= 10 * (m.desmentidas() + m.calmados()));
                }
            }
        }
        assertTrue(victorias >= 10, "Un jugador atento debe poder ganar casi siempre; ganó " + victorias + "/12");
    }

    @Test void trasTerminarYaNoSeAceptanAcciones() {
        var m = enCurso(Modo.SOLO, 9);
        avanzar(m, 80);
        assertTrue(m.terminada());
        int puntos = m.puntos();
        assertFalse(m.verificar(0));
        assertFalse(m.interactuar());
        m.mover(1, 1);
        m.actualizar(1);
        assertEquals(puntos, m.puntos());
    }

    @Test void esDeterministaPorSemillaYModo() {
        var a = enCurso(Modo.DUO, 42);
        var b = enCurso(Modo.DUO, 42);
        for (int i = 0; i < 60 * 30; i++) { jugarUnCuadro(a); jugarUnCuadro(b); }
        assertEquals(a.puntos(), b.puntos());
        assertEquals(a.panico(), b.panico());
        assertEquals(a.desinformacion(), b.desinformacion());
        assertEquals(a.noticias().size(), b.noticias().size());
        assertEquals(a.historial(), b.historial());
    }

    @Test void elModoEnParejaEsMasExigente() {
        var solo = new Mision5(Modo.SOLO, 1);
        var duo = new Mision5(Modo.DUO, 1);
        assertTrue(duo.metaDesmentidas() > solo.metaDesmentidas());
        assertTrue(duo.metaCalmados() > solo.metaCalmados());
        assertThrows(IllegalArgumentException.class, () -> solo.actualizar(-1));
    }
}
