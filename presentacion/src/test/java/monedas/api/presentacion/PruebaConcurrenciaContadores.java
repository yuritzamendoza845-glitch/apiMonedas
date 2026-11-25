package monedas.api.presentacion;

import monedas.api.aplicacion.servicios.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Evaluacion 5 - Laboratorio de deteccion y mitigacion de condiciones de
 * carrera en contadores compartidos.
 *
 * Dispara una rafaga de N hilos concurrentes. Cada hilo ejecuta
 * UsuarioServicio.login(...) INTENTOS_POR_HILO veces contra cada variante del
 * contador de intentos (A: sin control, B: synchronized, C: AtomicLong), y mide:
 * valor final vs total esperado, incrementos perdidos y tiempo total.
 *
 * Se repite el login varias veces por hilo para alargar la ventana de
 * contencion: con un solo incremento por hilo la carrera casi nunca ocurre.
 */
class PruebaConcurrenciaContadores {

    /** Cantidades de hilos concurrentes a probar, segun pide el enunciado. */
    private static final int[] TAMANOS_RAFAGA = {500, 1000, 2000};

    /** Intentos de login que hace CADA hilo (alarga la ventana de contencion). */
    private static final int INTENTOS_POR_HILO = 5_000;

    private static final List<ResultadoPrueba> RESULTADOS = new ArrayList<>();

    private record ResultadoPrueba(String variante, int hilos, long esperado, long obtenido,
                                    long perdidos, long duracionMs) {
    }

    @Test
    void variante_A_sinControl_pierdeIncrementosBajoConcurrencia() throws InterruptedException {
        for (int n : TAMANOS_RAFAGA) {
            ResultadoPrueba r = ejecutarRafaga(new ContadorSinControl(), n);
            RESULTADOS.add(r);
            System.out.printf("[A - Sin control] hilos=%d -> esperado=%d, obtenido=%d, perdidos=%d, tiempo=%d ms%n",
                    n, r.esperado(), r.obtenido(), r.perdidos(), r.duracionMs());
        }
        // No se afirma igualdad aqui a proposito: esta variante pierde
        // incrementos bajo alta concurrencia; eso es lo que el laboratorio
        // pide evidenciar.
    }

    @Test
    void variante_B_synchronized_esCorrectaBajoConcurrencia() throws InterruptedException {
        for (int n : TAMANOS_RAFAGA) {
            ResultadoPrueba r = ejecutarRafaga(new ContadorSincronizado(), n);
            RESULTADOS.add(r);
            System.out.printf("[B - synchronized] hilos=%d -> esperado=%d, obtenido=%d, perdidos=%d, tiempo=%d ms%n",
                    n, r.esperado(), r.obtenido(), r.perdidos(), r.duracionMs());
            assertEquals(r.esperado(), r.obtenido(),
                    "synchronized debe preservar TODOS los incrementos, sin perdidas");
        }
    }

    @Test
    void variante_C_atomica_esCorrectaBajoConcurrencia() throws InterruptedException {
        for (int n : TAMANOS_RAFAGA) {
            ResultadoPrueba r = ejecutarRafaga(new ContadorAtomico(), n);
            RESULTADOS.add(r);
            System.out.printf("[C - AtomicLong]   hilos=%d -> esperado=%d, obtenido=%d, perdidos=%d, tiempo=%d ms%n",
                    n, r.esperado(), r.obtenido(), r.perdidos(), r.duracionMs());
            assertEquals(r.esperado(), r.obtenido(),
                    "AtomicLong debe preservar TODOS los incrementos, sin perdidas");
        }
    }

    /**
     * Dispara `numHilos` tareas concurrentes, cada una con INTENTOS_POR_HILO
     * llamadas a login(...), liberandolas todas a la vez (gate pattern) para
     * maximizar la concurrencia real al momento de medir.
     */
    private ResultadoPrueba ejecutarRafaga(IContadorIntentosLogin contador, int numHilos) throws InterruptedException {
        contador.reiniciar();
        UsuarioServicio usuarioServicio = new UsuarioServicio(contador);

        // Un hilo por tarea: con un pool mas chico que numHilos habria
        // interbloqueo por inanicion en "arranque.await()".
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);

        CountDownLatch listos = new CountDownLatch(numHilos);
        CountDownLatch arranque = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(numHilos);

        for (int i = 0; i < numHilos; i++) {
            executor.submit(() -> {
                listos.countDown();
                try {
                    arranque.await();
                    for (int j = 0; j < INTENTOS_POR_HILO; j++) {
                        usuarioServicio.login("usuario_carga", "clave_incorrecta");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fin.countDown();
                }
            });
        }

        listos.await();                        // todos los hilos listos en la puerta
        long inicio = System.nanoTime();
        arranque.countDown();                  // se liberan TODOS a la vez
        boolean termino = fin.await(120, TimeUnit.SECONDS);
        long duracionMs = (System.nanoTime() - inicio) / 1_000_000;

        executor.shutdown();
        assertTrue(termino, "Las tareas no terminaron a tiempo");

        long esperado = (long) numHilos * INTENTOS_POR_HILO;
        long obtenido = contador.obtenerTotalIntentos();
        return new ResultadoPrueba(contador.nombreEstrategia(), numHilos, esperado, obtenido,
                esperado - obtenido, duracionMs);
    }

    /**
     * Se ejecuta una sola vez al terminar las 3 variantes e imprime/guarda la
     * tabla comparativa completa (3 variantes x 3 tamanos).
     */
    @AfterAll
    static void imprimirTablaYGuardarCsv() {
        String linea = "=".repeat(98);
        StringBuilder tabla = new StringBuilder();
        tabla.append("\n").append(linea).append("\n");
        tabla.append(String.format("(cada hilo ejecuta %d intentos de login)%n", INTENTOS_POR_HILO));
        tabla.append(String.format("%-40s %7s %12s %12s %10s %11s%n",
                "Variante", "Hilos", "Esperado", "Obtenido", "Perdidos", "Tiempo(ms)"));
        tabla.append("-".repeat(98)).append("\n");
        for (ResultadoPrueba r : RESULTADOS) {
            tabla.append(String.format("%-40s %7d %12d %12d %10d %11d%n",
                    r.variante(), r.hilos(), r.esperado(), r.obtenido(), r.perdidos(), r.duracionMs()));
        }
        tabla.append(linea).append("\n");
        System.out.println(tabla);

        try (PrintWriter csv = new PrintWriter(new FileWriter("resultados-concurrencia.csv"))) {
            csv.println("variante,hilos,esperado,obtenido,perdidos,tiempo_ms");
            for (ResultadoPrueba r : RESULTADOS) {
                csv.printf("%s,%d,%d,%d,%d,%d%n", r.variante(), r.hilos(), r.esperado(),
                        r.obtenido(), r.perdidos(), r.duracionMs());
            }
        } catch (IOException e) {
            System.err.println("No se pudo escribir resultados-concurrencia.csv: " + e.getMessage());
        }
    }
}