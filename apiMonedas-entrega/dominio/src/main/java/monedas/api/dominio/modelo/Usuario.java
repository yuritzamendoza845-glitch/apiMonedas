package monedas.api.dominio.modelo;

/**
 * Entidad de dominio minima. No conoce nada de HTTP, Maven modulos de
 * presentacion ni de la forma en que se contabilizan los intentos de login:
 * esa responsabilidad vive en la capa de aplicacion (ver IContadorIntentosLogin).
 */
public class Usuario {

    private final String nombreUsuario;
    private final String clave;

    public Usuario(String nombreUsuario, String clave) {
        this.nombreUsuario = nombreUsuario;
        this.clave = clave;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getClave() {
        return clave;
    }
}
