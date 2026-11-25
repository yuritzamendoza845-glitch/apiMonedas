package monedas.api.aplicacion.servicios;

/**
 * Variante B: exclusion mutua mediante "synchronized".
 *
 * Cada metodo toma el monitor (lock intrinseco) del objeto antes de leer o
 * modificar "contador". Mientras un hilo tiene el monitor, cualquier otro
 * hilo que quiera entrar a un metodo synchronized de ESTA instancia queda
 * bloqueado (suspendido) hasta que el primero lo libera.
 *
 * Esto garantiza el resultado correcto (no se pierde ningun incremento),
 * pero tiene un costo: bajo alta concurrencia los hilos hacen fila para
 * tomar el cerrojo, lo que aumenta la latencia total (tiempo de pared) de
 * la rafaga, aunque cada incremento individual siga siendo muy rapido.
 */
public class ContadorSincronizado implements IContadorIntentosLogin {

    private long contador = 0;

    @Override
    public synchronized void registrarIntento() {
        contador++;
    }

    @Override
    public synchronized long obtenerTotalIntentos() {
        return contador;
    }

    @Override
    public synchronized void reiniciar() {
        contador = 0;
    }

    @Override
    public String nombreEstrategia() {
        return "B - synchronized (exclusion mutua)";
    }
}
