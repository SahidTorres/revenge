package alcaldedigital.integracion;

import alcaldedigital.cliente.*;
import alcaldedigital.cliente.interfaz.*;
import alcaldedigital.servidor.minijuegos.Minijuego;
import alcaldedigital.servidor.sesion.SesionLocal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinijuegosInterfazTest {
    private static void avanzar(JuegoCliente j, double segundos) { for (double t = 0; t < segundos; t += 1 / 60.0) j.actualizar(1 / 60.0); }

    @Test void elMenuPrincipalAbreElSelectorDeMinijuegos() {
        var j = new JuegoCliente(SesionLocal::new);
        var r = new RenderJuego();
        r.dibujar(j);
        assertTrue(r.botones().stream().anyMatch(b -> b.accion().equals("minijuegos")));
        j.accion("minijuegos");
        assertEquals(JuegoCliente.Pantalla.MINIJUEGOS, j.pantalla);
        r.dibujar(j);
        long opciones = r.botones().stream().filter(b -> b.accion().startsWith("mj:")).count();
        assertEquals(12, opciones, "4 minijuegos × 3 modos");
    }

    @Test void flujoSelectorPartidaPausaYContinuar() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("minijuegos");
        j.accion("mj:DUELO:DUO");
        assertEquals(JuegoCliente.Pantalla.MINIJUEGO, j.pantalla);
        assertEquals(Minijuego.Tipo.DUELO, j.minijuego.tipo());
        avanzar(j, 4);
        double t = j.minijuego.transcurrido();
        assertTrue(t > .5);
        j.tecla(27, (char) 27);
        assertEquals(JuegoCliente.Pantalla.MINIJUEGOS, j.pantalla, "Esc pausa y vuelve al selector");
        avanzar(j, 3);
        assertEquals(t, j.minijuego.transcurrido(), 1e-9, "En pausa el reloj no corre");
        assertTrue(j.minijuegoEnCurso());
        j.accion("mjcontinuar");
        assertEquals(JuegoCliente.Pantalla.MINIJUEGO, j.pantalla);
        j.tecla(27, (char) 27); j.tecla(27, (char) 27);
        assertEquals(JuegoCliente.Pantalla.MENU, j.pantalla);
    }

    @Test void tecladoCompartidoEnPareja() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("mj:BOTS:DUO");
        avanzar(j, 3.2);
        var m = j.minijuego;
        float cx = m.avatarVista(Minijuego.Jugador.CIUDADANO).x(), px = m.avatarVista(Minijuego.Jugador.PERIODISTA).x();
        j.teclas.add(65); j.teclas.add(39);               // A (J1 a la izquierda) y → (J2 a la derecha)
        avanzar(j, .5); j.teclas.clear();
        assertTrue(m.avatarVista(Minijuego.Jugador.CIUDADANO).x() < cx);
        assertTrue(m.avatarVista(Minijuego.Jugador.PERIODISTA).x() > px);
        j.tecla(10, '\n');                                 // Enter: escaneo del Periodista
        avanzar(j, 1 / 60.0);
        assertTrue(m.avatarVista(Minijuego.Jugador.PERIODISTA).ocupado(), "Enter es la acción de J2");
        assertFalse(m.avatarVista(Minijuego.Jugador.CIUDADANO).ocupado());
    }

    @Test void enUnJugadorLasFlechasTambienMuevenATuPersonaje() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("mj:CADENA:SOLO_PERIODISTA");
        avanzar(j, 3.2);
        var m = j.minijuego;
        float x = m.avatarVista(Minijuego.Jugador.PERIODISTA).x();
        j.teclas.add(68); avanzar(j, .4); j.teclas.clear();
        assertTrue(m.avatarVista(Minijuego.Jugador.PERIODISTA).x() > x, "WASD también mueve al Periodista en un jugador");
    }

    @Test void unaPulsacionCortaNoSePierde() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("mj:BOTS:SOLO_PERIODISTA");
        avanzar(j, 3.2);
        j.tecla(69, 'e');          // se pulsa y se suelta antes del siguiente cuadro
        avanzar(j, 1 / 60.0);
        assertTrue(j.minijuego.avatarVista(Minijuego.Jugador.PERIODISTA).ocupado());
    }

    @Test void alTerminarSeVeElResultadoYSePuedeReintentar() {
        var j = new JuegoCliente(SesionLocal::new);
        j.accion("mj:CADENA:DUO");
        avanzar(j, 80);
        assertEquals(JuegoCliente.Pantalla.MINIJUEGO_FIN, j.pantalla);
        long semilla = j.semilla;
        j.accion("mjreintentar");
        assertEquals(JuegoCliente.Pantalla.MINIJUEGO, j.pantalla);
        assertEquals(semilla + 1, j.semilla);
        assertEquals(Minijuego.Estado.PREPARACION, j.minijuego.estado());
        assertEquals(Minijuego.Modo.DUO, j.minijuego.modo());
    }

    @Test void todasLasPantallasSeDibujanDentroDeLaVentana() {
        var r = new RenderJuego();
        for (Minijuego.Tipo tipo : Minijuego.Tipo.values()) for (Minijuego.Modo modo : Minijuego.Modo.values())
            for (boolean contraste : new boolean[]{false, true}) {
                var j = new JuegoCliente(SesionLocal::new);
                j.contraste = contraste; j.movimientoReducido = modo == Minijuego.Modo.SOLO_PERIODISTA;
                j.accion("minijuegos"); r.dibujar(j);
                j.accion("mj:" + tipo.name() + ":" + modo.name());
                for (int paso = 0; paso < 80 && j.pantalla == JuegoCliente.Pantalla.MINIJUEGO; paso++) {
                    avanzar(j, 2.5);
                    var img = r.dibujar(j);
                    assertEquals(1280, img.getWidth()); assertEquals(720, img.getHeight());
                    for (var b : r.botones()) assertTrue(b.area().x >= 0 && b.area().y >= 0 && b.area().getMaxX() <= 1280 && b.area().getMaxY() <= 720);
                }
                assertEquals(JuegoCliente.Pantalla.MINIJUEGO_FIN, j.pantalla, tipo + " " + modo + " termina");
                r.dibujar(j); avanzar(j, 1); r.dibujar(j);
                j.accion("minijuegos"); r.dibujar(j);
                assertTrue(r.botones().stream().noneMatch(b -> b.accion().equals("mjcontinuar")), "Una partida terminada no se continúa");
            }
    }
}
