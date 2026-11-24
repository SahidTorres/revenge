package alcaldedigital.integracion;

import alcaldedigital.cliente.JuegoCliente;
import alcaldedigital.cliente.JuegoCliente.Pantalla;
import alcaldedigital.cliente.campana.Campana;
import alcaldedigital.cliente.campana.Campana.Capitulo;
import alcaldedigital.cliente.interfaz.RenderJuego;
import alcaldedigital.cliente.mision6.MisionSeisMotor;
import alcaldedigital.servidor.minijuegos.Minijuego;
import alcaldedigital.servidor.misiones.Mision5;
import alcaldedigital.servidor.sesion.SesionLocal;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Recorre el modo historia de punta a punta a través del mismo control que usa la ventana. */
class HistoriaTest {
    @TempDir Path tmp;
    private static final double DT = 1 / 60.0;
    private JuegoCliente juego() { return new JuegoCliente(SesionLocal::new, new Campana(tmp.resolve("campana.properties"))); }
    private static void avanzar(JuegoCliente j, double s) { for (double t = 0; t < s; t += DT) j.actualizar(DT); }
    private static boolean hay(RenderJuego r, String accion) { return r.botones().stream().anyMatch(b -> b.accion().equals(accion)); }

    /** Jugador atento de la misión 5 (el mismo de Mision5Test), manejado por el control del juego. */
    private static void ganarMision5(JuegoCliente j) {
        for (int i = 0; i < 60 * 80 && j.pantalla == Pantalla.MISION5; i++) {
            var m = j.mision5;
            if (m.estado() == Mision5.Estado.EN_CURSO) {
                if (!m.verificando() && !m.noticias().isEmpty()) m.verificar(0);
                var jug = m.jugador();
                if (!jug.hablando()) {
                    Mision5.VecinoVista objetivo = null;
                    for (var v : m.vecinos()) if (v.conducta() == Mision5.Conducta.ALTERADO && (objetivo == null || v.panico() > objetivo.panico())) objetivo = v;
                    if (objetivo != null) {
                        float dx = objetivo.x() - jug.x(), dy = objetivo.y() - jug.y();
                        double d = Math.hypot(dx, dy);
                        if (d > 20) m.mover((float) (dx / d * 70 * DT), (float) (dy / d * 70 * DT)); else m.interactuar();
                    }
                }
            }
            j.actualizar(DT);
        }
    }

    @Test void elMenuAbreLaHistoriaYSeEligenLosJugadores() {
        var j = juego(); var r = new RenderJuego();
        r.dibujar(j);
        assertEquals("historia", r.botones().get(0).accion(), "La historia es el primer botón del menú (Enter la abre)");
        j.accion("historia"); assertEquals(Pantalla.HISTORIA, j.pantalla);
        r.dibujar(j);
        assertTrue(hay(r, "hnueva:1")); assertTrue(hay(r, "hnueva:2"));
        assertFalse(hay(r, "hcap:M5"), "Sin historia iniciada no hay capítulos para jugar");
        j.accion("hnueva:2");
        assertTrue(j.campana.dosJugadores());
        assertEquals(Pantalla.CAPITULO, j.pantalla); assertEquals(Capitulo.M5, j.capitulo);
        j.tecla(27, (char) 27); assertEquals(Pantalla.HISTORIA, j.pantalla, "Esc vuelve al mapa");
        j.tecla(27, (char) 27); assertEquals(Pantalla.MENU, j.pantalla);
    }

    @Test void relatoAvanzaYEmpiezaLaMision5EnElModoDeLaHistoria() {
        var j = juego(); j.accion("hnueva:2");
        int paginas = Capitulo.M5.relato().size();
        for (int i = 0; i < paginas - 1; i++) { assertEquals(i, j.paginaCapitulo); j.tecla(32, ' '); }
        assertEquals(paginas - 1, j.paginaCapitulo);
        var r = new RenderJuego(); r.dibujar(j);
        assertEquals("hjugar", r.botones().get(0).accion(), "En la última página el primer botón empieza el capítulo");
        j.tecla(32, ' ');
        assertEquals(Pantalla.MISION5_INTRO, j.pantalla, "La misión 5 conserva su propio guion");
        assertEquals(Mision5.Modo.DUO, j.mision5.modo(), "Dos jugadores en la historia = misión 5 en pareja");
        j.tecla(27, (char) 27); assertEquals(Pantalla.HISTORIA, j.pantalla, "Esc en la historia vuelve al mapa, no al menú");
        assertTrue(j.capituloEnCurso());
        j.accion("hreanudar"); assertEquals(Pantalla.MISION5_INTRO, j.pantalla);
    }

    @Test void ganarLaMision5LlevaAlPrimerMinijuegoYPerderloCuentaUnReintento() {
        var j = juego(); j.accion("hnueva:1"); j.accion("hsaltar"); j.accion("hjugar");
        j.accion("m5empezar"); assertEquals(Pantalla.MISION5, j.pantalla);
        ganarMision5(j);
        assertEquals(Pantalla.MISION5_FIN, j.pantalla);
        assertEquals(Mision5.Estado.VICTORIA, j.mision5.estado(), "El jugador atento gana la misión 5");
        assertTrue(j.campana.superado(Capitulo.M5)); assertEquals(j.mision5.puntos(), j.campana.puntos(Capitulo.M5));
        assertTrue(j.campana.mision6Desbloqueada());
        var r = new RenderJuego(); r.dibujar(j);
        assertTrue(hay(r, "hsiguiente")); assertFalse(hay(r, "m5otromodo"), "En la historia no se cambia de modo a mitad");
        j.accion("hsiguiente");
        assertEquals(Pantalla.CAPITULO, j.pantalla); assertEquals(Capitulo.CADENA, j.capitulo);
        j.accion("hsaltar"); r.dibujar(j);
        assertTrue(hay(r, "hjugar:SOLO_CIUDADANO")); assertTrue(hay(r, "hjugar:SOLO_PERIODISTA"));
        j.accion("hjugar:SOLO_PERIODISTA");
        assertEquals(Pantalla.MINIJUEGO, j.pantalla); assertEquals(Minijuego.Tipo.CADENA, j.minijuego.tipo());
        assertEquals(Minijuego.Modo.SOLO_PERIODISTA, j.minijuego.modo());
        avanzar(j, 80);
        assertEquals(Pantalla.MINIJUEGO_FIN, j.pantalla);
        assertEquals(Minijuego.Estado.DERROTA, j.minijuego.estado(), "Sin tocar el teclado se pierde");
        assertEquals(1, j.campana.derrotas(Capitulo.CADENA)); assertEquals(Capitulo.CADENA, j.campana.actual());
        r.dibujar(j); assertTrue(hay(r, "mjreintentar")); assertFalse(hay(r, "hsiguiente"));
        j.accion("mjreintentar"); assertEquals(Pantalla.MINIJUEGO, j.pantalla); assertEquals(Minijuego.Tipo.CADENA, j.minijuego.tipo());
    }

    @Test void ganarUnMinijuegoDeLaHistoriaAbreElSiguiente() {
        // Rastrea la fuente: con el humano quieto, el ayudante gana algunas semillas. Se busca una.
        var j = juego(); j.accion("hnueva:1");
        for (Capitulo c : new Capitulo[]{Capitulo.M5, Capitulo.CADENA}) j.campana.victoria(c, 100);
        boolean gano = false;
        for (long s = 1; s < 80 && !gano; s++) {
            j.semilla = s; j.abrirCapitulo(Capitulo.FUENTE); j.jugarCapitulo(Minijuego.Modo.SOLO_PERIODISTA);
            avanzar(j, 95);
            gano = j.minijuego.estado() == Minijuego.Estado.VICTORIA;
        }
        assertTrue(gano, "Alguna semilla debe ganarse");
        assertTrue(j.campana.superado(Capitulo.FUENTE)); assertEquals(Capitulo.BOTS, j.campana.actual());
        j.accion("hsiguiente"); assertEquals(Capitulo.BOTS, j.capitulo); assertEquals(Pantalla.CAPITULO, j.pantalla);
    }

    @Test void laMision6CierraLaHistoriaYMuestraElEpilogo() {
        var j = juego(); j.accion("hnueva:2");
        for (Capitulo c : List.of(Capitulo.M5, Capitulo.CADENA, Capitulo.FUENTE, Capitulo.BOTS, Capitulo.DUELO)) j.campana.victoria(c, 100);
        j.accion("hcontinuar"); assertEquals(Capitulo.M6, j.capitulo);
        j.accion("hsaltar"); j.accion("hjugar");
        assertEquals(Pantalla.MISION6, j.pantalla); assertNotNull(j.seis);
        var m = j.seis.motor();
        assertEquals(MisionSeisMotor.Fase.DISCURSO, m.fase);
        // En la historia ya se eligió: al llegar a la selección de modo entra sola en modo dúo.
        m.fase = MisionSeisMotor.Fase.MODO; j.actualizar(DT);
        assertEquals(MisionSeisMotor.Modo.DUO, m.modo); assertEquals(MisionSeisMotor.Fase.DESPEGUE, m.fase);
        // Una derrota en el combate cuenta como reintento.
        m.fase = MisionSeisMotor.Fase.DERROTA; j.actualizar(DT); j.actualizar(DT);
        assertEquals(1, j.campana.derrotas(Capitulo.M6));
        // Final: la entrevista termina con la ruta de escucha.
        m.escucha = 2; m.fase = MisionSeisMotor.Fase.FINAL;
        j.tecla(KeyEvent.VK_ENTER, '\n'); j.actualizar(DT);
        assertEquals(Pantalla.EPILOGO, j.pantalla); assertNull(j.seis);
        assertTrue(j.campana.completada()); assertEquals("ESCUCHA", j.campana.ruta());
        assertEquals(JuegoCliente.puntosMision6(new MisionSeisMotor.Resultado(true, MisionSeisMotor.Modo.DUO, MisionSeisMotor.Ruta.ESCUCHA, 5, 5)), j.campana.puntos(Capitulo.M6));
        for (int i = 0; i < j.campana.epilogo().size(); i++) j.tecla(32, ' ');
        assertTrue(j.campana.epilogoVisto());
        var r = new RenderJuego(); r.dibujar(j);
        assertTrue(hay(r, "menu"));
        j.accion("menu"); r.dibujar(j);
        assertTrue(r.botones().get(0).texto().contains("completada"));
    }

    @Test void nuevaHistoriaEmpiezaDeCeroAunqueHayaProgreso() {
        var j = juego(); j.accion("hnueva:1"); j.campana.victoria(Capitulo.M5, 100);
        j.accion("hnueva:2");
        assertEquals(Capitulo.M5, j.campana.actual()); assertEquals(0, j.campana.superados()); assertTrue(j.campana.dosJugadores());
    }

    @Test void pantallasDeLaHistoriaSeDibujanDentroYSinBotonesEncimados() {
        var j = juego(); var r = new RenderJuego();
        for (int jugadores = 1; jugadores <= 2; jugadores++) {
            j.accion("hnueva:" + jugadores);
            for (Capitulo c : Capitulo.values()) {
                for (boolean contraste : new boolean[]{false, true}) {
                    j.contraste = contraste;
                    j.abrirCapitulo(c);
                    for (int p = 0; p < c.relato().size(); p++) { j.paginaCapitulo = p; revisar(r, j); }
                    j.pantalla = Pantalla.HISTORIA; revisar(r, j);
                }
                j.campana.victoria(c, 50 * c.numero());
            }
            j.paginaEpilogo = 0; j.pantalla = Pantalla.EPILOGO;
            for (int p = 0; p <= j.campana.epilogo().size(); p++) { j.paginaEpilogo = p; revisar(r, j); }
            j.pantalla = Pantalla.MISION6_PAUSA; revisar(r, j);
            j.pantalla = Pantalla.MENU; revisar(r, j);
        }
    }

    private static void revisar(RenderJuego r, JuegoCliente j) {
        var img = r.dibujar(j);
        assertEquals(1280, img.getWidth()); assertEquals(720, img.getHeight());
        var bs = r.botones();
        for (int i = 0; i < bs.size(); i++) {
            var b = bs.get(i).area();
            assertTrue(b.x >= 0 && b.y >= 0 && b.getMaxX() <= 1280 && b.getMaxY() <= 720, j.pantalla + " " + bs.get(i).accion());
            for (int k = i + 1; k < bs.size(); k++) assertFalse(b.intersects(bs.get(k).area()), j.pantalla + ": " + bs.get(i).accion() + " / " + bs.get(k).accion());
        }
    }
}
