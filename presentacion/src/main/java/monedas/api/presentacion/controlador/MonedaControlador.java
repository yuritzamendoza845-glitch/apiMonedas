package monedas.api.presentacion.controlador;

import monedas.api.aplicacion.servicios.UsuarioServicio;

/**
 * Representa el punto de entrada HTTP (normalmente resuelto por Tomcat) del
 * sistema apiMonedas. Solo delega en la capa de aplicacion; no contiene
 * logica de negocio ni de contabilizacion de intentos.
 */
public class MonedaControlador {

    private final UsuarioServicio usuarioServicio;

    public MonedaControlador(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    public String listar() {
        return "[]";
    }

    public boolean login(String usuario, String clave) {
        return usuarioServicio.login(usuario, clave);
    }
}
