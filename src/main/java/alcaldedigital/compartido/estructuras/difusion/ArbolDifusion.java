package alcaldedigital.compartido.estructuras.difusion;

import java.util.*;

/**
 * Árbol n-ario de difusión de un rumor: la raíz es quien lo inventó y cada hijo es alguien que
 * lo recibió de su padre y lo reenvió. Lo usa el minijuego «Rastrea la fuente»: a partir de
 * una hoja se sube de padre en padre hasta la raíz.
 *
 * <p>Cada nodo guarda la referencia a su padre y la lista de hijos, así que subir cuesta O(1)
 * por paso y el camino a la raíz cuesta O(profundidad).</p>
 */
public final class ArbolDifusion {
    private static final class Nodo {
        final int id; final Nodo padre; final List<Nodo> hijos = new ArrayList<>();
        Nodo(int id, Nodo padre) { this.id = id; this.padre = padre; }
    }
    /** Vista inmutable para dibujar el árbol. */
    public record Vista(int id, List<Vista> hijos) { }

    private final Nodo raiz;
    private final Map<Integer, Nodo> indice = new HashMap<>();

    public ArbolDifusion(int idRaiz) {
        raiz = new Nodo(idRaiz, null);
        indice.put(idRaiz, raiz);
    }

    public int raiz() { return raiz.id; }
    public boolean contiene(int id) { return indice.containsKey(id); }
    public int tamano() { return indice.size(); }

    /** Agrega {@code hijo} como alguien que recibió el rumor de {@code padre}. */
    public void agregar(int padre, int hijo) {
        Nodo p = indice.get(padre);
        if (p == null) throw new IllegalArgumentException("El padre no está en el árbol: " + padre);
        if (indice.containsKey(hijo)) throw new IllegalArgumentException("Nodo repetido: " + hijo);
        Nodo n = new Nodo(hijo, p);
        p.hijos.add(n);
        indice.put(hijo, n);
    }

    /** Padre del nodo, o -1 si es la raíz. */
    public int padre(int id) {
        Nodo n = nodo(id);
        return n.padre == null ? -1 : n.padre.id;
    }

    public List<Integer> hijos(int id) {
        List<Integer> salida = new ArrayList<>();
        for (Nodo h : nodo(id).hijos) salida.add(h.id);
        return salida;
    }

    public boolean esHoja(int id) { return nodo(id).hijos.isEmpty(); }

    /** Número de reenvíos que separan al nodo de la fuente (la raíz tiene profundidad 0). */
    public int profundidad(int id) {
        int p = 0;
        for (Nodo n = nodo(id); n.padre != null; n = n.padre) p++;
        return p;
    }

    /** Altura en niveles: un árbol con solo la raíz tiene altura 1. */
    public int altura() { return altura(raiz); }
    private int altura(Nodo n) {
        int mayor = 0;
        for (Nodo h : n.hijos) mayor = Math.max(mayor, altura(h));
        return mayor + 1;
    }

    /** Camino desde el nodo hasta la raíz, ambos incluidos. */
    public List<Integer> caminoARaiz(int id) {
        List<Integer> camino = new ArrayList<>();
        for (Nodo n = nodo(id); n != null; n = n.padre) camino.add(n.id);
        return camino;
    }

    public List<Integer> preorden() {
        List<Integer> salida = new ArrayList<>();
        preorden(raiz, salida);
        return salida;
    }
    private void preorden(Nodo n, List<Integer> salida) {
        salida.add(n.id);
        for (Nodo h : n.hijos) preorden(h, salida);
    }

    /** Recorrido por niveles (BFS del árbol). */
    public List<Integer> porNiveles() {
        List<Integer> salida = new ArrayList<>();
        ArrayDeque<Nodo> cola = new ArrayDeque<>(List.of(raiz));
        while (!cola.isEmpty()) {
            Nodo n = cola.poll();
            salida.add(n.id);
            cola.addAll(n.hijos);
        }
        return salida;
    }

    public List<Integer> hojas() {
        List<Integer> salida = new ArrayList<>();
        for (int id : preorden()) if (esHoja(id)) salida.add(id);
        return salida;
    }

    /** La hoja más profunda (primera en preorden si hay empate). */
    public int hojaMasProfunda() {
        int mejor = raiz.id, prof = -1;
        for (int id : hojas()) { int p = profundidad(id); if (p > prof) { prof = p; mejor = id; } }
        return mejor;
    }

    public Vista vista() { return vista(raiz); }
    private Vista vista(Nodo n) {
        List<Vista> hijos = new ArrayList<>();
        for (Nodo h : n.hijos) hijos.add(vista(h));
        return new Vista(n.id, List.copyOf(hijos));
    }

    private Nodo nodo(int id) {
        Nodo n = indice.get(id);
        if (n == null) throw new IllegalArgumentException("Nodo inexistente: " + id);
        return n;
    }
}
