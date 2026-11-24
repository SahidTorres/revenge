package alcaldedigital.compartido.estructuras.grafos;

import java.util.*;

/**
 * Grafo no dirigido con listas de adyacencia: la red social de Civitas en el minijuego
 * «Corta la cadena». Cada vértice es una cuenta y cada arista, un contacto por el que puede
 * viajar un rumor. Propio del proyecto: no usa colecciones de grafos de terceros.
 */
public final class GrafoRed {
    /** Valor de distancia para los vértices a los que el recorrido no llega. */
    public static final int INALCANZABLE = -1;

    private final List<List<Integer>> adyacencia = new ArrayList<>();
    private int aristas;

    public int agregarVertice() {
        adyacencia.add(new ArrayList<>());
        return adyacencia.size() - 1;
    }

    public int cantidadVertices() { return adyacencia.size(); }
    public int cantidadAristas() { return aristas; }

    /** Agrega la arista {a, b}. Ignora lazos y aristas repetidas; devuelve si la agregó. */
    public boolean agregarArista(int a, int b) {
        validar(a); validar(b);
        if (a == b || adyacencia.get(a).contains(b)) return false;
        adyacencia.get(a).add(b);
        adyacencia.get(b).add(a);
        aristas++;
        return true;
    }

    public boolean adyacentes(int a, int b) { validar(a); validar(b); return adyacencia.get(a).contains(b); }

    public List<Integer> vecinos(int v) { validar(v); return List.copyOf(adyacencia.get(v)); }

    public int grado(int v) { validar(v); return adyacencia.get(v).size(); }

    /** Todas las aristas una sola vez, como pares {menor, mayor}. */
    public List<int[]> aristas() {
        List<int[]> salida = new ArrayList<>();
        for (int a = 0; a < adyacencia.size(); a++)
            for (int b : adyacencia.get(a)) if (a < b) salida.add(new int[]{a, b});
        return salida;
    }

    /**
     * BFS con varios orígenes a la vez. Los vértices bloqueados no se visitan ni dejan pasar
     * el recorrido (un orígen bloqueado se ignora). Devuelve la distancia en aristas desde el
     * origen más cercano, o {@link #INALCANZABLE}.
     */
    public int[] distanciasBFS(Collection<Integer> origenes, Set<Integer> bloqueados) {
        int[] distancia = new int[adyacencia.size()];
        Arrays.fill(distancia, INALCANZABLE);
        ArrayDeque<Integer> cola = new ArrayDeque<>();
        for (int o : origenes) {
            validar(o);
            if (bloqueados.contains(o) || distancia[o] == 0) continue;
            distancia[o] = 0;
            cola.add(o);
        }
        while (!cola.isEmpty()) {
            int actual = cola.poll();
            for (int w : adyacencia.get(actual)) {
                if (distancia[w] != INALCANZABLE || bloqueados.contains(w)) continue;
                distancia[w] = distancia[actual] + 1;
                cola.add(w);
            }
        }
        return distancia;
    }

    /** Orden de visita del BFS desde un único origen (sin bloqueos). */
    public List<Integer> recorridoBFS(int origen) {
        validar(origen);
        boolean[] visto = new boolean[adyacencia.size()];
        List<Integer> orden = new ArrayList<>();
        ArrayDeque<Integer> cola = new ArrayDeque<>(List.of(origen));
        visto[origen] = true;
        while (!cola.isEmpty()) {
            int actual = cola.poll();
            orden.add(actual);
            for (int w : adyacencia.get(actual)) if (!visto[w]) { visto[w] = true; cola.add(w); }
        }
        return orden;
    }

    /** Componentes conexas, cada una con sus vértices en orden de BFS. */
    public List<List<Integer>> componentes() {
        boolean[] visto = new boolean[adyacencia.size()];
        List<List<Integer>> salida = new ArrayList<>();
        for (int v = 0; v < adyacencia.size(); v++) {
            if (visto[v]) continue;
            List<Integer> componente = recorridoBFS(v);
            for (int w : componente) visto[w] = true;
            salida.add(componente);
        }
        return salida;
    }

    public boolean esConexo() { return adyacencia.isEmpty() || componentes().size() == 1; }

    private void validar(int v) {
        if (v < 0 || v >= adyacencia.size()) throw new IndexOutOfBoundsException("Vértice inexistente: " + v);
    }
}
