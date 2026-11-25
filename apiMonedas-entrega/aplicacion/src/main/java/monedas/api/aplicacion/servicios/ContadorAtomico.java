package monedas.api.aplicacion.servicios;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Variante C: operacion atomica "lock-free" usando AtomicLong (el mismo
 * enfoque usado en la Cubeta de Tokens).
 *
 * AtomicLong.incrementAndGet() no usa un cerrojo: se apoya en una
 * instruccion de hardware Compare-And-Swap (CAS). El hilo lee el valor
 * actual, calcula el nuevo, e intenta escribirlo SOLO SI nadie lo cambio
 * mientras tanto; si otro hilo le gano la carrera, reintenta
 * automaticamente con el valor mas reciente. Ningun hilo queda bloqueado
 * esperando a otro (no hay "fila"), por lo que normalmente escala mejor
 * que synchronized bajo alta contencion, manteniendo el resultado correcto.
 */
public class ContadorAtomico implements IContadorIntentosLogin {

    private final AtomicLong contador = new AtomicLong(0);

    @Override
    public void registrarIntento() {
        contador.incrementAndGet();
    }

    @Override
    public long obtenerTotalIntentos() {
        return contador.get();
    }

    @Override
    public void reiniciar() {
        contador.set(0);
    }

    @Override
    public String nombreEstrategia() {
        return "C - AtomicLong (lock-free / CAS)";
    }
}
