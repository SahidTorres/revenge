package alcaldedigital.compartido.estructuras.hash;

import java.util.*;

/**
 * Tabla hash propia con encadenamiento separado y claves de texto. La usa el minijuego
 * «Caza de bots» como base de huellas de las redes de bots del Palomo: consultar si una huella
 * está registrada cuesta O(1) en promedio.
 *
 * <p>Función hash polinómica: h(c₀…cₙ) = (…((c₀·31 + c₁)·31 + c₂)…) mod capacidad, calculada
 * de forma incremental para no desbordar. La capacidad es un número primo.</p>
 */
public final class TablaHash<V> {
    public static final int BASE = 31;

    /** Par clave-valor guardado en una cubeta. */
    public record Entrada<V>(String clave, V valor) { }
    /** Resultado detallado de la última búsqueda, para explicarla en pantalla. */
    public record Busqueda(String clave, int cubeta, int comparaciones, boolean encontrada) { }

    private final List<List<Entrada<V>>> cubetas = new ArrayList<>();
    private int cantidad;
    private Busqueda ultima;

    public TablaHash(int capacidad) {
        if (capacidad < 1) throw new IllegalArgumentException("Capacidad inválida");
        for (int i = 0; i < capacidad; i++) cubetas.add(new LinkedList<>());
    }

    public int capacidad() { return cubetas.size(); }
    public int cantidad() { return cantidad; }
    public double factorCarga() { return (double) cantidad / cubetas.size(); }

    /** Índice de cubeta de una clave. */
    public int indice(String clave) {
        Objects.requireNonNull(clave);
        int h = 0;
        for (int i = 0; i < clave.length(); i++) h = (h * BASE + clave.charAt(i)) % cubetas.size();
        return h;
    }

    /** Inserta o reemplaza. Devuelve el valor anterior o null. */
    public V poner(String clave, V valor) {
        List<Entrada<V>> cubeta = cubetas.get(indice(clave));
        for (ListIterator<Entrada<V>> it = cubeta.listIterator(); it.hasNext(); ) {
            Entrada<V> e = it.next();
            if (e.clave().equals(clave)) { it.set(new Entrada<>(clave, valor)); return e.valor(); }
        }
        cubeta.add(new Entrada<>(clave, valor));
        cantidad++;
        return null;
    }

    /** Busca la clave y registra cuántas comparaciones hicieron falta. */
    public V obtener(String clave) {
        int i = indice(clave), comparaciones = 0;
        for (Entrada<V> e : cubetas.get(i)) {
            comparaciones++;
            if (e.clave().equals(clave)) { ultima = new Busqueda(clave, i, comparaciones, true); return e.valor(); }
        }
        ultima = new Busqueda(clave, i, comparaciones, false);
        return null;
    }

    public boolean contiene(String clave) { return obtener(clave) != null; }

    public V eliminar(String clave) {
        for (Iterator<Entrada<V>> it = cubetas.get(indice(clave)).iterator(); it.hasNext(); ) {
            Entrada<V> e = it.next();
            if (e.clave().equals(clave)) { it.remove(); cantidad--; return e.valor(); }
        }
        return null;
    }

    public List<Entrada<V>> cubeta(int i) { return List.copyOf(cubetas.get(i)); }

    public List<String> claves() {
        List<String> salida = new ArrayList<>();
        for (List<Entrada<V>> c : cubetas) for (Entrada<V> e : c) salida.add(e.clave());
        return salida;
    }

    /** Última búsqueda realizada con {@link #obtener} o {@link #contiene}, o null. */
    public Busqueda ultimaBusqueda() { return ultima; }
}
