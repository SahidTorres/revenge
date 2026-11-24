package alcaldedigital.cliente.campana;

import alcaldedigital.compartido.guardado.ArchivosPartida;
import alcaldedigital.servidor.minijuegos.Minijuego;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Modo historia «La noche del Palomo»: une la misión 5, los cuatro minijuegos y la misión 6
 * en una sola aventura. Solo guarda el progreso y los textos; cada capítulo conserva su
 * propia lógica. No depende de Processing, así que se prueba sin ventana.
 */
public final class Campana {

    /** Una línea de guion: quién habla y qué dice. */
    public record Linea(String quien, String texto) { }

    public enum Capitulo {
        M5("La calle del pánico", "Misión 5", "Centro de Periodismo · calle principal", null,
            "AVL de prioridad (radar de noticias)",
            new Linea[] {
                new Linea("NARRADOR", "Ciudad Nova, a una semana de elegir alcalde. Civitas, la red social donde todos se informan, amaneció llena de noticias falsas y la gente empieza a tener miedo."),
                new Linea("NARRADOR", "Detrás de todo está el Palomo: una paloma con lentes oscuros, un celular robado y un plan para que nadie confíe en nada antes de las elecciones."),
                new Linea("EL PALOMO", "«Si la gente tiene miedo, no piensa. Si no piensa, comparte. Y si comparte… yo gano.»"),
                new Linea("TU MISIÓN", "Eres la Periodista que desmiente noticias falsas y el Ciudadano que calma a los vecinos. Aguanten 70 segundos sin que el pánico ni la desinformación lleguen a 100.")
            }),
        CADENA("Corta la cadena", "Minijuego 1", "La red de Civitas", Minijuego.Tipo.CADENA,
            "Grafo con listas de adyacencia + BFS",
            new Linea[] {
                new Linea("NARRADOR", "El Palomo huyó, pero en la carrera dejó caer una memoria USB. En el Centro de Periodismo la conectan: adentro está la «Operación Pánico Total», su plan completo en cuatro pasos."),
                new Linea("PERIODISTA", "Paso uno: un rumor que se reenvía solo por los contactos de Civitas. Si llega a la mitad de la red, ya nadie va a creer en nada."),
                new Linea("CIUDADANO", "Entonces hay que cortarle el camino antes de que se riegue. ¡Vamos a blindar cuentas!"),
                new Linea("CÓMO SE JUEGA", "Blinda las cuentas sanas para que el rumor no pase por ellas y desmiente las contagiadas. Cada ola el rumor avanza un contacto: es un nivel del recorrido BFS.")
            }),
        FUENTE("Rastrea la fuente", "Minijuego 2", "La plaza de los reenvíos", Minijuego.Tipo.FUENTE,
            "Árbol n-ario con referencia al padre",
            new Linea[] {
                new Linea("NARRADOR", "La red resistió. Pero un rumor no aparece de la nada: alguien lo escribió primero. Paso dos del plan: esconder quién lo inventó."),
                new Linea("PERIODISTA", "Cada persona sabe quién le mandó el mensaje. Si subimos por esa cadena, de hijo a padre, llegamos a quien lo creó."),
                new Linea("EL PALOMO", "«¿Seguir una cadena de chismes en una plaza llena de gente? Suerte con eso.»"),
                new Linea("CÓMO SE JUEGA", "Pregunta «¿quién te lo mandó?» y busca a esa persona. Sube hasta la raíz del árbol: es una cuenta falsa del Palomo. Son tres rondas.")
            }),
        BOTS("Caza de bots", "Minijuego 3", "El parque de las cuentas falsas", Minijuego.Tipo.BOTS,
            "Tabla hash con encadenamiento",
            new Linea[] {
                new Linea("NARRADOR", "Las tres fuentes eran cuentas falsas y todas tenían una huella en común: un ícono y un color que se repiten. Paso tres del plan: un ejército de bots."),
                new Linea("CIUDADANO", "¡Están por todo el parque publicando cadenas! «¡¡COMPARTE YA!!»"),
                new Linea("PERIODISTA", "Anoté sus huellas en una tabla. Con ella reconocemos a cada bot al instante, sin revisar a todo el mundo."),
                new Linea("CÓMO SE JUEGA", "Reporta solo a las cuentas cuya huella está en la tabla hash. Cuidado con las personas reales que se parecen a un bot.")
            }),
        DUELO("Duelo contra el Palomo", "Minijuego 4 · Jefe", "Frente al Centro de Periodismo", Minijuego.Tipo.DUELO,
            "Árbol AVL + búsqueda del piso",
            new Linea[] {
                new Linea("NARRADOR", "Sin bots, el Palomo se quedó solo. Furioso, aterriza frente al Centro de Periodismo protegido por un escudo de chismes. Paso cuatro: hacerlo él mismo."),
                new Linea("EL PALOMO", "«¡Basta! Si no gano con rumores, ganaré con ruido. ¡A ver quién rompe mi escudo!»"),
                new Linea("PERIODISTA", "Cada chisme tiene una fuerza. Un dato verificado rompe el chisme más fuerte que alcance… y si el escudo cae, el Palomo queda expuesto."),
                new Linea("CÓMO SE JUEGA", "Mantén la acción para cargar un dato y suéltala para disparar. Esquiva los chismes que caen. Deja al Palomo sin vida.")
            }),
        M6("Cielos de campaña", "Misión 6", "La plaza, el cielo y la casa de campaña", null,
            "Máquina de estados + autoguardado",
            new Linea[] {
                new Linea("NARRADOR", "Derrotado, el Palomo escapa volando. Minutos después, todas las pantallas de Civitas muestran una transmisión pirata."),
                new Linea("EL PALOMO", "«¿Creen que ganaron? Hoy Juan, el candidato, vuela en helicóptero a su casa de campaña. Mi Dron Viral lo estará esperando en el cielo.»"),
                new Linea("PERIODISTA", "La influencer que lo va a entrevistar está en la plaza con él. Le mando todo lo que descubrimos: el USB, los bots y la amenaza."),
                new Linea("INFLUENCER", "Recibido. Voy con Juan. Si ese dron aparece, no vamos a dejar que el miedo decida por Ciudad Nova."),
                new Linea("CÓMO SE JUEGA", "Habla con Juan y acompáñalo al helicóptero. Practica, derriba al Dron Viral en tres fases (los misiles llegan en la segunda) y termina con la entrevista.")
            });

        public final String titulo, etiqueta, lugar, estructura;
        public final Minijuego.Tipo minijuego;
        private final Linea[] relato;
        Capitulo(String titulo, String etiqueta, String lugar, Minijuego.Tipo minijuego, String estructura, Linea[] relato) {
            this.titulo = titulo; this.etiqueta = etiqueta; this.lugar = lugar; this.minijuego = minijuego;
            this.estructura = estructura; this.relato = relato;
        }
        public int numero() { return ordinal() + 1; }
        public List<Linea> relato() { return List.of(relato); }
        public static Capitulo de(Minijuego.Tipo tipo) {
            for (Capitulo c : values()) if (c.minijuego == tipo) return c;
            throw new IllegalArgumentException(String.valueOf(tipo));
        }
    }

    /** Rol recomendado para jugar solo cada minijuego (el otro lo maneja la computadora). */
    public static Minijuego.Modo modoRecomendado(Capitulo c) {
        return c == Capitulo.FUENTE || c == Capitulo.DUELO ? Minijuego.Modo.SOLO_PERIODISTA : Minijuego.Modo.SOLO_CIUDADANO;
    }

    public static final int TOTAL = Capitulo.values().length;
    private final Path archivo;
    private int jugadores = 1, siguiente;
    private final int[] puntos = new int[TOTAL], derrotas = new int[TOTAL];
    private final boolean[] superado = new boolean[TOTAL];
    private boolean iniciada, libreM5, epilogoVisto;
    private String ruta = "";
    public String error = "";

    public Campana() { this(ArchivosPartida.directorio().resolve("campana.properties")); }
    public Campana(Path archivo) { this.archivo = archivo; cargar(); }

    // ───────────────────────── Consultas ─────────────────────────
    public boolean iniciada() { return iniciada; }
    public int jugadores() { return jugadores; }
    public boolean dosJugadores() { return jugadores == 2; }
    /** Próximo capítulo por jugar, o {@code null} si la historia ya terminó. */
    public Capitulo actual() { return siguiente >= TOTAL ? null : Capitulo.values()[siguiente]; }
    public boolean completada() { return iniciada && siguiente >= TOTAL; }
    public boolean superado(Capitulo c) { return superado[c.ordinal()]; }
    public boolean disponible(Capitulo c) { return iniciada && c.ordinal() <= siguiente; }
    public int puntos(Capitulo c) { return puntos[c.ordinal()]; }
    public int derrotas(Capitulo c) { return derrotas[c.ordinal()]; }
    public int puntosTotales() { return Arrays.stream(puntos).sum(); }
    public int derrotasTotales() { return Arrays.stream(derrotas).sum(); }
    public int superados() { int n = 0; for (boolean s : superado) if (s) n++; return n; }
    /** Ruta elegida en la entrevista final (ESCUCHA o CONVICCION); vacía si aún no se juega. */
    public String ruta() { return ruta; }
    public boolean epilogoVisto() { return epilogoVisto; }
    /** La misión 6 suelta del menú exige haber ganado la misión 5 (en historia o por separado). */
    public boolean mision6Desbloqueada() { return libreM5 || superado[Capitulo.M5.ordinal()]; }

    /** Título que se gana al terminar, según cuántas veces hubo que reintentar. */
    public String rango() {
        int d = derrotasTotales();
        return d == 0 ? "Guardianes de Ciudad Nova" : d <= 3 ? "Verificadores incansables" : "Aprendices que nunca se rinden";
    }

    // ───────────────────────── Cambios ─────────────────────────
    public void nueva(int jugadores) {
        if (jugadores != 1 && jugadores != 2) throw new IllegalArgumentException("1 o 2 jugadores");
        boolean m5 = mision6Desbloqueada();
        this.jugadores = jugadores; siguiente = 0; iniciada = true; epilogoVisto = false; ruta = "";
        Arrays.fill(puntos, 0); Arrays.fill(derrotas, 0); Arrays.fill(superado, false);
        libreM5 = m5;
        guardar();
    }
    /** Victoria en un capítulo: guarda el mejor puntaje y abre el siguiente. Devuelve el siguiente. */
    public Capitulo victoria(Capitulo c, int puntaje) {
        int i = c.ordinal();
        if (!disponible(c)) return actual();
        superado[i] = true; puntos[i] = Math.max(puntos[i], Math.max(0, puntaje));
        if (i == siguiente) siguiente++;
        guardar();
        return actual();
    }
    public void derrota(Capitulo c) { if (disponible(c)) { derrotas[c.ordinal()]++; guardar(); } }
    public void ruta(String valor) { ruta = valor == null ? "" : valor; guardar(); }
    public void epilogoVisto(boolean v) { epilogoVisto = v; guardar(); }
    /** Ganar la misión 5 desde el menú también desbloquea la misión 6 suelta. */
    public void victoriaLibreM5() { if (!libreM5) { libreM5 = true; guardar(); } }

    // ───────────────────────── Guardado ─────────────────────────
    private void cargar() {
        try {
            Properties p = ArchivosPartida.leer(archivo);
            if (p == null) return;
            if (!"1".equals(p.getProperty("version"))) throw new IOException("Versión de historia desconocida");
            int j = Integer.parseInt(p.getProperty("jugadores", "1")), s = Integer.parseInt(p.getProperty("siguiente", "0"));
            if (j < 1 || j > 2 || s < 0 || s > TOTAL) throw new IOException("Historia fuera de rango");
            int[] pts = new int[TOTAL], der = new int[TOTAL]; boolean[] sup = new boolean[TOTAL];
            for (int i = 0; i < TOTAL; i++) {
                pts[i] = Math.max(0, Integer.parseInt(p.getProperty("puntos." + i, "0")));
                der[i] = Math.max(0, Integer.parseInt(p.getProperty("derrotas." + i, "0")));
                sup[i] = Boolean.parseBoolean(p.getProperty("superado." + i, "false"));
            }
            jugadores = j; siguiente = s; iniciada = Boolean.parseBoolean(p.getProperty("iniciada", "false"));
            System.arraycopy(pts, 0, puntos, 0, TOTAL); System.arraycopy(der, 0, derrotas, 0, TOTAL); System.arraycopy(sup, 0, superado, 0, TOTAL);
            libreM5 = Boolean.parseBoolean(p.getProperty("libre.m5", "false"));
            epilogoVisto = Boolean.parseBoolean(p.getProperty("epilogo", "false"));
            ruta = p.getProperty("ruta", "");
        } catch (IOException | RuntimeException e) {
            error = "No se pudo leer la historia guardada; empieza una nueva.";
        }
    }
    private void guardar() {
        Properties p = new Properties();
        p.setProperty("version", "1"); p.setProperty("iniciada", "" + iniciada);
        p.setProperty("jugadores", "" + jugadores); p.setProperty("siguiente", "" + siguiente);
        for (int i = 0; i < TOTAL; i++) {
            p.setProperty("puntos." + i, "" + puntos[i]); p.setProperty("derrotas." + i, "" + derrotas[i]);
            p.setProperty("superado." + i, "" + superado[i]);
        }
        p.setProperty("libre.m5", "" + libreM5); p.setProperty("epilogo", "" + epilogoVisto); p.setProperty("ruta", ruta);
        try { ArchivosPartida.guardar(archivo, p); error = ""; }
        catch (IOException e) { error = "No se pudo guardar la historia: " + e.getMessage(); }
    }

    // ───────────────────────── Epílogo ─────────────────────────
    public List<Linea> epilogo() {
        boolean escucha = "ESCUCHA".equals(ruta);
        return List.of(
            new Linea("NARRADOR", "El Dron Viral se autodestruyó en el aire y la amenaza del Palomo se quedó sin señal. La entrevista de Juan se transmitió completa, sin cortes y sin filtros."),
            new Linea("JUAN", escucha
                ? "«Gobernar es escuchar, ajustar y construir con la gente. Esta noche aprendí que una ciudad informada es más fuerte que cualquier rumor.»"
                : "«Gobernar exige sostener ideas firmes y explicarlas con valentía. Esta noche aprendí que la verdad también necesita quien la defienda.»"),
            new Linea("PERIODISTA", "En Civitas ya circula otra cadena: «Antes de compartir, verifica». Esa sí la dejamos correr."),
            new Linea("CIUDADANO", "¿Y el Palomo?"),
            new Linea("NARRADOR", "Nadie lo ha vuelto a ver. Dicen que sigue suelto, buscando a alguien que comparta sin pensar… Pero esta vez Ciudad Nova está lista."));
    }
}
