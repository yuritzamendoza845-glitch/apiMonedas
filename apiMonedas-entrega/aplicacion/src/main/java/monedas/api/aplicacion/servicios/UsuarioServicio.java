package monedas.api.aplicacion.servicios;

import monedas.api.dominio.modelo.Usuario;

/**
 * Punto de instrumentacion elegido para este laboratorio: UsuarioServicio.login(...).
 *
 * Cada intento de autenticacion (exitoso o fallido) pasa por aqui y se
 * contabiliza a traves de la estrategia de contador inyectada (Variante A,
 * B o C segun IContadorIntentosLogin). La capa de aplicacion no conoce
 * detalles de HTTP/Tomcat: eso es responsabilidad del modulo presentacion.
 */
public class UsuarioServicio {

    private static final Usuario CREDENCIAL_VALIDA = new Usuario("admin", "admin123");

    private final IContadorIntentosLogin contadorIntentos;

    public UsuarioServicio(IContadorIntentosLogin contadorIntentos) {
        this.contadorIntentos = contadorIntentos;
    }

    public boolean login(String usuario, String clave) {
        contadorIntentos.registrarIntento();
        return CREDENCIAL_VALIDA.getNombreUsuario().equals(usuario)
                && CREDENCIAL_VALIDA.getClave().equals(clave);
    }

    public IContadorIntentosLogin getContadorIntentos() {
        return contadorIntentos;
    }
}
