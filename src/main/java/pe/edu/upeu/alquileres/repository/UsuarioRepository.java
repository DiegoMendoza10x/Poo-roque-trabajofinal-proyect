package pe.edu.upeu.alquileres.repository;

import pe.edu.upeu.alquileres.model.Usuario;
import pe.edu.upeu.alquileres.repository.helper.SqlHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository extends SqlHelper<Usuario> {

    /** Busca un usuario activo con las credenciales indicadas. */
    public Optional<Usuario> login(String usuario, String clave) {
        return executeQueryOne(
                "SELECT * FROM usuario WHERE usuario = ? AND clave = ? AND estado = 'Activo'",
                usuario, clave);
    }

    /** Busca cualquier usuario por su nombre de acceso (para no duplicar clientes). */
    public Optional<Usuario> findByUsuario(String usuario) {
        return executeQueryOne("SELECT * FROM usuario WHERE usuario = ?", usuario);
    }

    /** Crea un usuario cliente y devuelve el registro con su id asignado. */
    public Usuario guardarCliente(String usuario, String clave, String nombre) {
        String sql = "INSERT INTO usuario (usuario, clave, nombre, rol, estado) VALUES (?, ?, ?, 'CLIENTE', 'Activo')";
        try (Connection conn = openConnection()) {
            long id = executeInsertGetKey(conn, sql, usuario, clave, nombre);
            return Usuario.builder()
                    .idUsuario(id)
                    .usuario(usuario)
                    .clave(clave)
                    .nombre(nombre)
                    .rol("CLIENTE")
                    .estado("Activo")
                    .build();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo crear el usuario cliente: " + e.getMessage(), e);
        }
    }

    /** Lista solo los usuarios tipo cliente (acceso a la APK). */
    public List<Usuario> findClientes() {
        return executeQuery("SELECT * FROM usuario WHERE rol = 'CLIENTE' ORDER BY usuario");
    }

    /** Cambia la clave de un usuario por su id. */
    public void cambiarClave(Long id, String clave) {
        int filas = executeUpdateStandalone("UPDATE usuario SET clave = ? WHERE idusuario = ?", clave, id);
        if (filas == 0) throw new RuntimeException("No se encontró el usuario indicado.");
    }

    /** Activa o desactiva un usuario. */
    public void cambiarEstado(Long id, String estado) {
        int filas = executeUpdateStandalone("UPDATE usuario SET estado = ? WHERE idusuario = ?", estado, id);
        if (filas == 0) throw new RuntimeException("No se encontró el usuario indicado.");
    }

    /** Cambia el estado de un usuario por su usuario de acceso (DNI). */
    public void cambiarEstadoPorUsuario(String usuario, String estado) {
        executeUpdateStandalone("UPDATE usuario SET estado = ? WHERE usuario = ?", estado, usuario);
    }

    /** Elimina un usuario cliente. */
    public void eliminar(Long id) {
        int filas = executeUpdateStandalone("DELETE FROM usuario WHERE idusuario = ?", id);
        if (filas == 0) throw new RuntimeException("No se encontró el usuario indicado.");
    }

    /** Actualiza la clave y los datos de un cliente existente. */
    public void actualizarCliente(String usuario, String clave, String nombre) {
        String sql = "UPDATE usuario SET clave = ?, nombre = ?, rol = 'CLIENTE', estado = 'Activo' WHERE usuario = ?";
        try (Connection conn = openConnection()) {
            executeUpdate(conn, sql, clave, nombre, usuario);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el usuario cliente: " + e.getMessage(), e);
        }
    }

    @Override
    protected Usuario mapRow(ResultSet rs) throws SQLException {
        return Usuario.builder()
                .idUsuario(rs.getLong("idusuario"))
                .usuario(rs.getString("usuario"))
                .clave(rs.getString("clave"))
                .nombre(rs.getString("nombre"))
                .rol(rs.getString("rol"))
                .estado(rs.getString("estado"))
                .build();
    }
}
