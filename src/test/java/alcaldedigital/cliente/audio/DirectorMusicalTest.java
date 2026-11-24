package alcaldedigital.cliente.audio;

import alcaldedigital.cliente.JuegoCliente;
import alcaldedigital.cliente.JuegoCliente.Pantalla;
import alcaldedigital.cliente.campana.Campana;
import alcaldedigital.cliente.mision6.MisionSeisMotor;
import alcaldedigital.servidor.minijuegos.Minijuego;
import alcaldedigital.servidor.sesion.SesionLocal;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DirectorMusicalTest {
    @TempDir Path tmp;
    private JuegoCliente juego() { return new JuegoCliente(SesionLocal::new, new Campana(tmp.resolve("c.properties"))); }

    @Test void cadaPantallaTieneSuMusicaYTodasLasPistasExisten() throws Exception {
        var j = juego();
        var catalogo = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/cliente/audio/catalogo.properties")) { catalogo.load(in); }
        for (Pantalla p : Pantalla.values()) {
            j.pantalla = p;
            var cue = DirectorMusical.elegir(j);
            assertNotNull(getClass().getResource("/cliente/audio/musica/" + cue.pista() + ".wav"), p + " usa " + cue.pista());
            assertNotNull(catalogo.getProperty(cue.pista() + ".titulo"), "La pista está en el catálogo");
            for (String capa : catalogo.getProperty(cue.pista() + ".capas", "").split(","))
                if (!capa.isBlank()) assertNotNull(getClass().getResource("/cliente/audio/musica/" + cue.pista() + "-" + capa + ".wav"));
        }
        j.pantalla = Pantalla.MENU; assertEquals("G01", DirectorMusical.elegir(j).pista());
    }

    @Test void laMusicaSubeConElPeligroYConLasFasesDelDron() {
        var j = juego();
        j.iniciarMinijuego(Minijuego.Tipo.CADENA, Minijuego.Modo.DUO);
        float inicio = DirectorMusical.elegir(j).intensidad();
        for (int i = 0; i < 60 * 30; i++) j.minijuego.actualizar(1 / 60.0);
        assertTrue(DirectorMusical.elegir(j).intensidad() > inicio, "El minijuego se acelera con el reloj");
        var m = new MisionSeisMotor(); m.comenzar(MisionSeisMotor.Modo.SOLO); m.fase = MisionSeisMotor.Fase.COMBATE;
        float f1 = DirectorMusical.seis(m).intensidad(); m.energia = 50;
        assertEquals("M602", DirectorMusical.seis(m).pista());
        assertTrue(DirectorMusical.seis(m).intensidad() > f1, "Fase 3 del dron = música épica");
    }

    @Test void losEfectosSoloSuenanCuandoAlgoCambia() {
        var antes = new DirectorMusical.Estado(Pantalla.MISION5, false, 2, 1, 0);
        assertTrue(DirectorMusical.eventos(antes, antes).isEmpty());
        assertEquals(java.util.List.of("verificar"), DirectorMusical.eventos(antes, new DirectorMusical.Estado(Pantalla.MISION5, false, 3, 1, 0)));
        assertEquals(java.util.List.of("victoria"), DirectorMusical.eventos(antes, new DirectorMusical.Estado(Pantalla.MISION5_FIN, true, 3, 1, 0)));
        assertEquals(java.util.List.of("derrota"), DirectorMusical.eventos(antes, new DirectorMusical.Estado(Pantalla.MISION5_FIN, false, 3, 1, 0)));
        assertTrue(DirectorMusical.eventos(null, antes).isEmpty(), "Al arrancar no suena nada repetido");
    }

    @Test void losEfectosUsadosExisten() {
        for (String id : new String[]{"victoria", "derrota", "cuenta", "fase", "pausa", "verificar", "recuperacion", "recoger", "click", "confirmar"})
            assertNotNull(getClass().getResource("/cliente/audio/fx/" + id + ".wav"), id);
    }
}
