package alcaldedigital.cliente.campana;

import alcaldedigital.cliente.campana.Campana.Capitulo;
import alcaldedigital.servidor.minijuegos.Minijuego;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CampanaTest {
    @TempDir Path tmp;
    private Campana nueva() { return new Campana(tmp.resolve("campana.properties")); }

    @Test void elOrdenDeLaHistoriaEsMision5CuatroMinijuegosYMision6() {
        assertArrayEquals(new Capitulo[]{Capitulo.M5, Capitulo.CADENA, Capitulo.FUENTE, Capitulo.BOTS, Capitulo.DUELO, Capitulo.M6}, Capitulo.values());
        assertNull(Capitulo.M5.minijuego);assertNull(Capitulo.M6.minijuego);
        for (Minijuego.Tipo t : Minijuego.Tipo.values()) assertEquals(t, Capitulo.de(t).minijuego, "Cada minijuego es un capítulo");
        for (Capitulo c : Capitulo.values()) {
            assertTrue(c.relato().size() >= 4, c + " cuenta su parte de la historia");
            assertFalse(c.estructura.isBlank());
        }
    }

    @Test void cadaVictoriaAbreElSiguienteYUnaDerrotaNo() {
        var c = nueva();
        assertFalse(c.iniciada()); assertFalse(c.disponible(Capitulo.M5));
        c.nueva(1);
        assertEquals(Capitulo.M5, c.actual()); assertTrue(c.disponible(Capitulo.M5)); assertFalse(c.disponible(Capitulo.CADENA));
        c.derrota(Capitulo.M5);
        assertEquals(Capitulo.M5, c.actual()); assertEquals(1, c.derrotas(Capitulo.M5));
        assertEquals(Capitulo.CADENA, c.victoria(Capitulo.M5, 300));
        assertTrue(c.mision6Desbloqueada(), "Ganar la misión 5 en la historia desbloquea la 6 suelta");
        c.victoria(Capitulo.FUENTE, 999);
        assertFalse(c.superado(Capitulo.FUENTE), "No se puede saltar un capítulo bloqueado");
        for (Capitulo cap : new Capitulo[]{Capitulo.CADENA, Capitulo.FUENTE, Capitulo.BOTS, Capitulo.DUELO}) c.victoria(cap, 100);
        assertEquals(Capitulo.M6, c.actual()); assertFalse(c.completada());
        c.ruta("ESCUCHA");
        assertNull(c.victoria(Capitulo.M6, 400));
        assertTrue(c.completada()); assertEquals(6, c.superados());
        assertEquals(300 + 4 * 100 + 400, c.puntosTotales());
        assertEquals("Verificadores incansables", c.rango());
    }

    @Test void repetirGuardaElMejorPuntajeSinRetroceder() {
        var c = nueva(); c.nueva(2);
        c.victoria(Capitulo.M5, 200); c.victoria(Capitulo.M5, 150);
        assertEquals(200, c.puntos(Capitulo.M5)); assertEquals(Capitulo.CADENA, c.actual());
        c.victoria(Capitulo.M5, 260);
        assertEquals(260, c.puntos(Capitulo.M5)); assertEquals(Capitulo.CADENA, c.actual());
    }

    @Test void elProgresoSeGuardaYSeRecupera() throws Exception {
        var c = nueva(); c.nueva(2); c.victoria(Capitulo.M5, 210); c.derrota(Capitulo.CADENA);
        var otra = nueva();
        assertTrue(otra.iniciada()); assertTrue(otra.dosJugadores()); assertEquals(Capitulo.CADENA, otra.actual());
        assertEquals(210, otra.puntos(Capitulo.M5)); assertEquals(1, otra.derrotas(Capitulo.CADENA));
        Files.writeString(tmp.resolve("campana.properties"), "version=1\nsiguiente=99\n");
        var rota = nueva();
        assertFalse(rota.error.isBlank(), "Un guardado fuera de rango se rechaza");
        assertFalse(rota.iniciada());
    }

    @Test void nuevaHistoriaConservaElDesbloqueoLibreYValidaJugadores() {
        var c = nueva(); c.victoriaLibreM5();
        c.nueva(1);
        assertTrue(c.mision6Desbloqueada()); assertEquals(0, c.superados());
        assertThrows(IllegalArgumentException.class, () -> c.nueva(3));
    }

    @Test void modoRecomendadoUsaLaHabilidadMasUtilDeCadaPersonaje() {
        assertEquals(Minijuego.Modo.SOLO_PERIODISTA, Campana.modoRecomendado(Capitulo.DUELO), "Solo el periodista rompe chismes de más de 70");
        assertEquals(Minijuego.Modo.SOLO_CIUDADANO, Campana.modoRecomendado(Capitulo.CADENA), "El ciudadano blinda rápido");
    }

    @Test void elEpilogoCambiaConLaRutaDeLaEntrevista() {
        var c = nueva(); c.nueva(1);
        c.ruta("ESCUCHA"); String escucha = c.epilogo().get(1).texto();
        c.ruta("CONVICCION"); String conviccion = c.epilogo().get(1).texto();
        assertNotEquals(escucha, conviccion);
        assertTrue(escucha.contains("escuchar"));
    }
}
