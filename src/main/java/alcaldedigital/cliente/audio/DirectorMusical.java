package alcaldedigital.cliente.audio;

import alcaldedigital.cliente.JuegoCliente;
import alcaldedigital.cliente.JuegoCliente.Pantalla;
import alcaldedigital.cliente.mision6.MisionSeisMotor;
import alcaldedigital.servidor.minijuegos.Minijuego;
import alcaldedigital.servidor.misiones.Mision5;
import java.util.ArrayList;
import java.util.List;

/**
 * Elige la música y los efectos a partir de lo que muestra el juego. Solo observa:
 * nunca cambia reglas, tiempos ni puntajes. {@link #elegir} y {@link #eventos} son
 * funciones puras, así que se prueban sin tarjeta de sonido.
 */
public final class DirectorMusical {
    /** Pista, intensidad (0–1: más alta suma las capas «ritmo» y «épica») y si hay diálogo. */
    public record Cue(String pista, float intensidad, boolean dialogo) { }

    private static Cue c(String pista, double intensidad, boolean dialogo) {
        return new Cue(pista, (float) Math.max(0, Math.min(1, intensidad)), dialogo);
    }

    public static Cue elegir(JuegoCliente j) {
        return switch (j.pantalla) {
            case HISTORIA, CAPITULO -> c("G02", .2, true);
            case EPILOGO -> c("G03", .45, true);
            case MISION5_MODO, MISION5_INTRO -> c("M101", .2, true);
            case MISION5 -> {
                var m = j.mision5;
                double peligro = m == null ? 0 : Math.max(m.panico(), m.desinformacion()) / 100.0;
                yield c("M102", .25 + .75 * peligro, false);
            }
            case MISION5_FIN -> j.mision5 != null && j.mision5.estado() == Mision5.Estado.VICTORIA ? c("G03", .45, false) : c("G02", .1, false);
            case MINIJUEGOS -> c("G02", .2, false);
            case MINIJUEGO -> {
                var m = j.minijuego;
                if (m == null) yield c("M303", .3, false);
                double avance = m.transcurrido() / Math.max(1, m.duracion());
                // El duelo es el jefe: entra con la capa épica desde el principio.
                yield c("M303", m.tipo() == Minijuego.Tipo.DUELO ? .7 + .3 * avance : .3 + .5 * avance, false);
            }
            case MINIJUEGO_FIN -> j.minijuego != null && j.minijuego.estado() == Minijuego.Estado.VICTORIA ? c("G03", .45, false) : c("G02", .1, false);
            case MISION6, MISION6_PAUSA -> j.seis == null ? c("G02", .2, true) : seis(j.seis.motor());
            default -> c("G01", .3, false);
        };
    }

    /** Misma partitura de la misión 6 original: plaza tranquila, vuelo, combate por fases y sala. */
    static Cue seis(MisionSeisMotor m) {
        return switch (m.fase) {
            case COMBATE -> c("M602", .35 + Math.max(0, m.etapa() - 1) * .3, false);
            case DESPEGUE, AVISO, ENTRENAMIENTO, RETIRADA, LLEGADA -> c("M601", m.fase == MisionSeisMotor.Fase.ENTRENAMIENTO ? .3 : .5, m.fase == MisionSeisMotor.Fase.AVISO || m.fase == MisionSeisMotor.Fase.RETIRADA);
            case DERROTA -> c("G02", .1, false);
            case ENTREVISTA, RESPUESTA, SALA, FINAL, EXPLORAR_SALA -> c("G02", .15, true);
            default -> c("M101", .2, true);
        };
    }

    /** Fotografía mínima del juego para detectar cambios entre dos cuadros. */
    public record Estado(Pantalla pantalla, boolean gano, int desmentidas, int calmados, int puntosMini) {
        public static Estado de(JuegoCliente j) {
            boolean gano = switch (j.pantalla) {
                case MISION5_FIN -> j.mision5 != null && j.mision5.estado() == Mision5.Estado.VICTORIA;
                case MINIJUEGO_FIN -> j.minijuego != null && j.minijuego.estado() == Minijuego.Estado.VICTORIA;
                default -> false;
            };
            boolean m5 = j.pantalla == Pantalla.MISION5 && j.mision5 != null, mini = j.pantalla == Pantalla.MINIJUEGO && j.minijuego != null;
            return new Estado(j.pantalla, gano, m5 ? j.mision5.desmentidas() : 0, m5 ? j.mision5.calmados() : 0, mini ? j.minijuego.puntos() : 0);
        }
    }

    /** Efectos nuevos entre dos estados (la misión 6 tiene su propio observador de sonido). */
    public static List<String> eventos(Estado antes, Estado ahora) {
        var salida = new ArrayList<String>();
        if (antes == null) return salida;
        if (antes.pantalla() != ahora.pantalla()) {
            switch (ahora.pantalla()) {
                case MISION5_FIN, MINIJUEGO_FIN -> salida.add(ahora.gano() ? "victoria" : "derrota");
                case MISION5, MINIJUEGO -> salida.add("cuenta");
                case CAPITULO -> salida.add("fase");
                case EPILOGO -> salida.add("victoria");
                case MISION6_PAUSA -> salida.add("pausa");
                default -> { }
            }
            return salida;
        }
        if (ahora.desmentidas() > antes.desmentidas()) salida.add("verificar");
        if (ahora.calmados() > antes.calmados()) salida.add("recuperacion");
        if (ahora.puntosMini() > antes.puntosMini()) salida.add("recoger");
        return salida;
    }

    private final MotorAudio motor;
    private Estado anterior;
    public DirectorMusical(MotorAudio motor) { this.motor = motor; }

    public void observar(JuegoCliente j) {
        var cue = elegir(j);
        motor.pausa(j.pantalla == Pantalla.MISION6_PAUSA);
        motor.musica(cue.pista(), cue.intensidad(), cue.dialogo());
        var ahora = Estado.de(j);
        for (String id : eventos(anterior, ahora)) {
            if (id.equals("pausa")) motor.interfaz(id); else motor.fx(id);
        }
        anterior = ahora;
    }
}
