package pe.edu.upeu.alquileres.service;

import pe.edu.upeu.alquileres.model.Contrato;
import pe.edu.upeu.alquileres.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface IUsuarioService {

    /** Devuelve el usuario si las credenciales son correctas. */
    Optional<Usuario> login(String usuario, String clave);

    /**
     * Crea (o renueva) el acceso del cliente asociado a un contrato.
     * El usuario es el DNI del inquilino y la clave se genera automáticamente.
     */
    Usuario crearCliente(Contrato contrato);

    /** Clientes dados de alta para la APK. */
    List<Usuario> listarClientes();

    /** Cambia la clave de un cliente (recuperación desde el escritorio). */
    void cambiarClave(Long idUsuario, String nuevaClave);

    /** Activa o desactiva el acceso de un cliente. */
    void cambiarEstado(Long idUsuario, String estado);

    /** Alta manual de un acceso (para completar el CRUD del panel). */
    Usuario crearClienteManual(String dni, String nombre, String clave);

    /** Elimina un acceso de cliente. */
    void eliminar(Long idUsuario);

    /** Activa/desactiva el acceso según el DNI del inquilino. */
    void activarPorDni(String dni);

    void desactivarPorDni(String dni);
}
