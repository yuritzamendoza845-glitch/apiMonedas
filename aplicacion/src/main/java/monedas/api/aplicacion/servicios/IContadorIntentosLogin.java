package monedas.api.aplicacion.servicios;

public interface IContadorIntentosLogin {
    void registrarIntento();
    long obtenerTotalIntentos();
    void reiniciar();
    String nombreEstrategia();
}
