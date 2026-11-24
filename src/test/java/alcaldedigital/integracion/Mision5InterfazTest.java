package alcaldedigital.integracion;

import alcaldedigital.cliente.*;
import alcaldedigital.cliente.interfaz.*;
import alcaldedigital.servidor.misiones.Mision5;
import alcaldedigital.servidor.sesion.SesionLocal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Mision5InterfazTest {
    private static void avanzar(JuegoCliente j, double segundos) { for (double t = 0; t < segundos; t += 1 / 60.0) j.actualizar(1 / 60.0); }

    @Test void flujoMenuModoIntroMisionYMenu() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("mision5");
        assertEquals(JuegoCliente.Pantalla.MISION5_MODO, j.pantalla);
        j.accion("m5modo:DUO");
        assertEquals(JuegoCliente.Pantalla.MISION5_INTRO, j.pantalla);
        assertEquals(Mision5.Modo.DUO, j.mision5.modo());
        for (int i = 0; i < JuegoCliente.PAGINAS_INTRO5; i++) j.accion("m5sig");
        assertEquals(JuegoCliente.Pantalla.MISION5, j.pantalla);
        j.tecla(27, (char) 27);
        assertEquals(JuegoCliente.Pantalla.MENU, j.pantalla);
        assertTrue(j.mision5EnCurso());
        j.accion("mision5");
        assertEquals(JuegoCliente.Pantalla.MISION5, j.pantalla);
    }

    @Test void enterEnLaIntroAvanzaLaHistoriaYNoSaleDelMenu() {
        var j = new JuegoCliente(SesionLocal::new);
        var r = new RenderJuego();
        j.accion("m5modo:SOLO");
        r.dibujar(j);
        r.activarFoco(j);
        assertEquals(JuegoCliente.Pantalla.MISION5_INTRO, j.pantalla);
        assertEquals(1, j.pagina5);
    }

    @Test void elRelojSoloCorreEnLaPantallaDeLaMision() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("m5modo:SOLO"); j.accion("m5empezar");
        avanzar(j, 5);
        double t = j.mision5.transcurrido();
        assertTrue(t > 1);
        j.accion("menu");
        avanzar(j, 5);
        assertEquals(t, j.mision5.transcurrido(), 1e-9);
    }

    @Test void controlesDelCiudadanoYDelPeriodista() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("m5modo:DUO"); j.accion("m5empezar");
        avanzar(j, 3.5);
        float x = j.mision5.jugador().x();
        j.teclas.add(68); avanzar(j, .5); j.teclas.clear();
        assertTrue(j.mision5.jugador().x() > x, "D mueve al ciudadano");
        float x2 = j.mision5.jugador().x();
        j.teclas.add(39); avanzar(j, .5); j.teclas.clear();
        assertEquals(x2, j.mision5.jugador().x(), 1e-4, "En pareja las flechas son del Periodista");
        avanzar(j, 4);
        int n = j.mision5.noticias().size();
        assertTrue(n >= 2);
        j.tecla(40, (char) 0);
        assertEquals(1, j.seleccion5);
        j.tecla(10, '\n');
        assertTrue(j.mision5.verificando());
    }

    @Test void enUnJugadorLasFlechasTambienMueven() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("m5modo:SOLO"); j.accion("m5empezar");
        avanzar(j, 3.5);
        float x = j.mision5.jugador().x();
        j.teclas.add(37); avanzar(j, .4); j.teclas.clear();
        assertTrue(j.mision5.jugador().x() < x);
    }

    @Test void alTerminarPasaALaPantallaFinalYPermiteReintentar() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("m5modo:SOLO"); j.accion("m5empezar");
        avanzar(j, 90);
        assertEquals(JuegoCliente.Pantalla.MISION5_FIN, j.pantalla);
        assertTrue(j.mision5.terminada());
        j.accion("m5reintentar");
        assertEquals(JuegoCliente.Pantalla.MISION5, j.pantalla);
        assertFalse(j.mision5.terminada());
        assertEquals(Mision5.Estado.PREPARACION, j.mision5.estado());
    }

    @Test void todasLasPantallasDeLaMisionSeDibujanDentroDeLaVentana() {
        var j = new JuegoCliente(SesionLocal::new);
        var r = new RenderJuego();
        for (boolean contraste : new boolean[]{false, true}) for (boolean reducido : new boolean[]{false, true}) {
            j.contraste = contraste; j.movimientoReducido = reducido;
            j.accion("mision5"); assertEquals(1280, r.dibujar(j).getWidth());
            j.accion("m5modo:SOLO");
            for (int i = 0; i < JuegoCliente.PAGINAS_INTRO5; i++) { r.dibujar(j); j.accion("m5sig"); }
            for (int paso = 0; paso < 30; paso++) {
                avanzar(j, 3);
                var img = r.dibujar(j);
                assertEquals(720, img.getHeight());
                for (var b : r.botones()) assertTrue(b.area().x >= 0 && b.area().y >= 0 && b.area().getMaxX() <= 1280 && b.area().getMaxY() <= 720);
                if (j.pantalla == JuegoCliente.Pantalla.MISION5_FIN) break;
            }
            assertEquals(JuegoCliente.Pantalla.MISION5_FIN, j.pantalla);
            r.dibujar(j); avanzar(j, 1); r.dibujar(j);
            j.accion("m5otromodo"); r.dibujar(j);
        }
    }

    @Test void clicEnUnaTarjetaVerificaEsaNoticia() {
        var j = new JuegoCliente(SesionLocal::new);
        var r = new RenderJuego();
        j.accion("m5modo:SOLO"); j.accion("m5empezar");
        avanzar(j, 5);
        r.dibujar(j);
        var tarjeta = r.botones().stream().filter(b -> b.accion().equals("m5v:0")).findFirst().orElseThrow();
        r.pulsar(j, tarjeta.area().x + 5, tarjeta.area().y + 5);
        assertTrue(j.mision5.verificando());
    }
}
