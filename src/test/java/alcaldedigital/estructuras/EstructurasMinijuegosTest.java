package alcaldedigital.estructuras;

import alcaldedigital.compartido.estructuras.avl.ArbolAVLPublicaciones;
import alcaldedigital.compartido.estructuras.difusion.ArbolDifusion;
import alcaldedigital.compartido.estructuras.grafos.GrafoRed;
import alcaldedigital.compartido.estructuras.hash.TablaHash;
import alcaldedigital.compartido.modelo.Publicacion;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Estructuras propias que usan los cuatro minijuegos de la misión 5. */
class EstructurasMinijuegosTest {
    // ───────────── Grafo + BFS (Corta la cadena) ─────────────
    private static GrafoRed camino(int n) {
        GrafoRed g = new GrafoRed();
        for (int i = 0; i < n; i++) g.agregarVertice();
        for (int i = 1; i < n; i++) g.agregarArista(i - 1, i);
        return g;
    }

    @Test void grafoNoDirigidoSinLazosNiAristasRepetidas() {
        GrafoRed g = camino(3);
        assertTrue(g.adyacentes(0, 1) && g.adyacentes(1, 0));
        assertFalse(g.agregarArista(1, 0));
        assertFalse(g.agregarArista(2, 2));
        assertEquals(2, g.cantidadAristas());
        assertEquals(2, g.grado(1));
        assertThrows(IndexOutOfBoundsException.class, () -> g.agregarArista(0, 9));
    }

    @Test void bfsConVariosOrigenesDaLaDistanciaAlMasCercano() {
        GrafoRed g = camino(7);                   // 0-1-2-3-4-5-6
        int[] d = g.distanciasBFS(List.of(0, 6), Set.of());
        assertArrayEquals(new int[]{0, 1, 2, 3, 2, 1, 0}, d);
    }

    @Test void losVerticesBloqueadosCortanElRecorrido() {
        GrafoRed g = camino(5);
        int[] d = g.distanciasBFS(List.of(0), Set.of(2));
        assertEquals(1, d[1]);
        assertEquals(GrafoRed.INALCANZABLE, d[2]);
        assertEquals(GrafoRed.INALCANZABLE, d[3], "Detrás de un blindaje el rumor no llega");
        assertEquals(GrafoRed.INALCANZABLE, d[4]);
    }

    @Test void recorridoBFSYComponentes() {
        GrafoRed g = camino(4);
        g.agregarArista(0, 2);
        assertEquals(List.of(0, 1, 2, 3), g.recorridoBFS(0));
        assertTrue(g.esConexo());
        g.agregarVertice();
        assertEquals(2, g.componentes().size());
        assertFalse(g.esConexo());
    }

    // ───────────── Árbol n-ario (Rastrea la fuente) ─────────────
    private static ArbolDifusion ejemplo() {
        //        1
        //      / | \
        //     2  3  4
        //    / \     \
        //   5   6     7
        //             |
        //             8
        ArbolDifusion a = new ArbolDifusion(1);
        a.agregar(1, 2); a.agregar(1, 3); a.agregar(1, 4);
        a.agregar(2, 5); a.agregar(2, 6); a.agregar(4, 7); a.agregar(7, 8);
        return a;
    }

    @Test void subirDePadreEnPadreLlegaALaRaiz() {
        ArbolDifusion a = ejemplo();
        assertEquals(7, a.padre(8));
        assertEquals(-1, a.padre(1));
        assertEquals(List.of(8, 7, 4, 1), a.caminoARaiz(8));
        assertEquals(3, a.profundidad(8));
        assertEquals(0, a.profundidad(1));
    }

    @Test void alturaTamanoHojasYRecorridos() {
        ArbolDifusion a = ejemplo();
        assertEquals(4, a.altura());
        assertEquals(8, a.tamano());
        assertEquals(List.of(5, 6, 3, 8), a.hojas());
        assertEquals(8, a.hojaMasProfunda());
        assertEquals(List.of(1, 2, 5, 6, 3, 4, 7, 8), a.preorden());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), a.porNiveles());
        assertEquals(List.of(2, 3, 4), a.hijos(1));
    }

    @Test void elArbolRechazaNodosRepetidosOPadresAusentes() {
        ArbolDifusion a = ejemplo();
        assertThrows(IllegalArgumentException.class, () -> a.agregar(1, 5));
        assertThrows(IllegalArgumentException.class, () -> a.agregar(99, 100));
        assertThrows(IllegalArgumentException.class, () -> a.padre(42));
    }

    // ───────────── AVL: búsqueda del piso (Duelo contra el Palomo) ─────────────
    private static ArbolAVLPublicaciones escudo(int... fuerzas) {
        ArbolAVLPublicaciones avl = new ArbolAVLPublicaciones();
        long id = 1;
        for (int f : fuerzas) avl.insertar(new Publicacion(id++, f, "Palomo", "chisme", "Escudo"));
        return avl;
    }

    @Test void pisoDevuelveElMayorQueNoSuperaLaFuerza() {
        var avl = escudo(20, 38, 55, 68, 84);
        assertEquals(55, avl.piso(60).riesgo());
        assertEquals(55, avl.piso(55).riesgo(), "Igual cuenta como ≤");
        assertEquals(84, avl.piso(99).riesgo());
        assertEquals(20, avl.piso(20).riesgo());
        assertNull(avl.piso(19), "Más débil que todos: rebota");
        assertEquals(20, avl.minimo().riesgo());
        assertEquals(84, avl.maximo().riesgo());
    }

    @Test void pisoCoincideConUnaBusquedaLinealTrasInsercionesYEliminaciones() {
        Random azar = new Random(5);
        var avl = new ArbolAVLPublicaciones();
        List<Integer> fuerzas = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            int f = 10 + azar.nextInt(90);
            avl.insertar(new Publicacion(i, f, "Palomo", "c", "Escudo")); fuerzas.add(f);
        }
        for (int consulta = 0; consulta <= 100; consulta++) {
            final int c = consulta;
            Integer esperado = fuerzas.stream().filter(f -> f <= c).max(Integer::compare).orElse(null);
            var p = avl.piso(c);
            assertEquals(esperado, p == null ? null : p.riesgo());
            if (p != null && consulta % 3 == 0) { avl.eliminar(p.id()); fuerzas.remove(Integer.valueOf(p.riesgo())); }
        }
        assertTrue(avl.altura() <= 1.45 * (Math.log(avl.cantidad() + 2) / Math.log(2)), "Sigue balanceado");
    }

    // ───────────── Tabla hash (Caza de bots) ─────────────
    @Test void tablaHashPonerObtenerEliminar() {
        TablaHash<String> t = new TablaHash<>(11);
        assertNull(t.poner("megáfono·rojo", "Red Cuuu"));
        assertEquals("Red Cuuu", t.obtener("megáfono·rojo"));
        assertEquals("Red Cuuu", t.poner("megáfono·rojo", "Red Plumas"), "Reemplazar devuelve el anterior");
        assertEquals(1, t.cantidad());
        assertTrue(t.contiene("megáfono·rojo"));
        assertFalse(t.contiene("wifi·azul"));
        assertEquals("Red Plumas", t.eliminar("megáfono·rojo"));
        assertEquals(0, t.cantidad());
    }

    @Test void funcionHashPolinomicaYEncadenamientoEnColisiones() {
        TablaHash<Integer> t = new TablaHash<>(11);
        // h("ab") = ((97)·31 + 98) mod 11
        assertEquals((97 * 31 + 98) % 11, t.indice("ab"));
        // Busca dos claves que caigan en la misma cubeta.
        String a = "k0", b = null;
        for (int i = 1; b == null; i++) if (t.indice("k" + i) == t.indice(a)) b = "k" + i;
        t.poner(a, 1); t.poner(b, 2);
        assertEquals(2, t.cubeta(t.indice(a)).size(), "La colisión se encadena en la misma cubeta");
        assertEquals(Integer.valueOf(2), t.obtener(b));
        var busqueda = t.ultimaBusqueda();
        assertEquals(t.indice(b), busqueda.cubeta());
        assertEquals(2, busqueda.comparaciones());
        assertTrue(busqueda.encontrada());
        assertNull(t.obtener("no-está"));
        assertFalse(t.ultimaBusqueda().encontrada());
    }

    private static void assertArrayEquals(int[] esperado, int[] real) { assertEquals(Arrays.toString(esperado), Arrays.toString(real)); }
}
