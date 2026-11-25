package monedas.api.aplicacion.servicios;

/**
 * Variante A: contador SIN control de concurrencia.
 *
 * Usa un campo primitivo (long) incrementado directamente con "contador++".
 * Ese operador NO es atomico: por debajo equivale a tres pasos separados
 * (1) leer el valor actual, (2) sumarle 1, (3) escribir el resultado.
 *
 * Si dos hilos ejecutan esos tres pasos entrelazados, ambos pueden leer el
 * MISMO valor antes de que el otro haya escrito su resultado, y uno de los
 * dos incrementos se pierde (fenomeno conocido como "lost update" / carrera
 * de datos). Mientras mas hilos compitan al mismo tiempo, mas incrementos
 * se pierden.
 */
public class ContadorSinControl implements IContadorIntentosLogin {

    private long contador = 0;

    @Override
    public void registrarIntento() {
        contador++;
    }

    @Override
    public long obtenerTotalIntentos() {
        return contador;
    }

    @Override
    public void reiniciar() {
        contador = 0;
    }

    @Override
    public String nombreEstrategia() {
        return "A - Sin control (long sin sincronizar)";
    }
}
