package pe.edu.upeu.alquileres.service.impl;

import pe.edu.upeu.alquileres.exception.BusinessException;
import pe.edu.upeu.alquileres.model.Contrato;
import pe.edu.upeu.alquileres.model.Inquilino;
import pe.edu.upeu.alquileres.model.Usuario;
import pe.edu.upeu.alquileres.repository.UsuarioRepository;
import pe.edu.upeu.alquileres.service.IUsuarioService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UsuarioServiceImp implements IUsuarioService {

    private final UsuarioRepository repository;

    public UsuarioServiceImp(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Usuario> login(String usuario, String clave) {
        if (usuario == null || usuario.isBlank()) {
            throw new BusinessException("Ingresa el nombre de usuario.");
        }
        if (clave == null || clave.isBlank()) {
            throw new BusinessException("Ingresa la clave.");
        }
        return repository.login(usuario.trim(), clave.trim());
    }

    @Override
    public Usuario crearCliente(Contrato contrato) {
        Inquilino inquilino = contrato == null ? null : contrato.getInquilino();
        if (inquilino == null || inquilino.getDni() == null || inquilino.getDni().isBlank()) {
            throw new BusinessException("El contrato debe tener un inquilino con DNI para crear su acceso.");
        }

        String usuario = inquilino.getDni().trim();
        String nombre = inquilino.getNombreCompleto().trim();
        String clave = generarClave();

        Optional<Usuario> existente = repository.findByUsuario(usuario);
        if (existente.isPresent() && "ADMIN".equalsIgnoreCase(existente.get().getRol())) {
            throw new BusinessException("El DNI ya pertenece a un usuario administrador.");
        }

        Usuario cliente;
        if (existente.isPresent()) {
            repository.actualizarCliente(usuario, clave, nombre);
            cliente = existente.get();
            cliente.setClave(clave);
            cliente.setNombre(nombre);
            cliente.setRol("CLIENTE");
            cliente.setEstado("Activo");
        } else {
            cliente = repository.guardarCliente(usuario, clave, nombre);
        }
        return cliente;
    }

    /** Genera una clave aleatoria de 8 caracteres para el cliente. */
    public static String generarClave() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    @Override
    public List<Usuario> listarClientes() {
        return repository.findClientes();
    }

    @Override
    public void cambiarClave(Long idUsuario, String nuevaClave) {
        if (idUsuario == null) {
            throw new BusinessException("Selecciona un cliente de la tabla.");
        }
        if (nuevaClave == null || nuevaClave.trim().length() < 4) {
            throw new BusinessException("La clave debe tener al menos 4 caracteres.");
        }
        repository.cambiarClave(idUsuario, nuevaClave.trim());
    }

    @Override
    public void cambiarEstado(Long idUsuario, String estado) {
        if (idUsuario == null) {
            throw new BusinessException("Selecciona un cliente de la tabla.");
        }
        if (!"Activo".equalsIgnoreCase(estado) && !"Inactivo".equalsIgnoreCase(estado)) {
            throw new BusinessException("El estado debe ser Activo o Inactivo.");
        }
        repository.cambiarEstado(idUsuario, estado);
    }

    @Override
    public Usuario crearClienteManual(String dni, String nombre, String clave) {
        String usuario = validarDni(dni);
        if (repository.findByUsuario(usuario).isPresent()) {
            throw new BusinessException("Ya existe un usuario con ese DNI.");
        }
        if (clave == null || clave.trim().length() < 4) {
            throw new BusinessException("La clave debe tener al menos 4 caracteres.");
        }
        String nombreLimpio = nombre == null || nombre.isBlank() ? "Cliente " + usuario : nombre.trim();
        return repository.guardarCliente(usuario, clave.trim(), nombreLimpio);
    }

    @Override
    public void eliminar(Long idUsuario) {
        if (idUsuario == null) {
            throw new BusinessException("Selecciona un cliente de la tabla.");
        }
        repository.eliminar(idUsuario);
    }

    @Override
    public void activarPorDni(String dni) {
        if (dni != null && !dni.isBlank()) {
            repository.cambiarEstadoPorUsuario(dni.trim(), "Activo");
        }
    }

    @Override
    public void desactivarPorDni(String dni) {
        if (dni != null && !dni.isBlank()) {
            repository.cambiarEstadoPorUsuario(dni.trim(), "Inactivo");
        }
    }

    private String validarDni(String dni) {
        if (dni == null || !dni.trim().matches("\\d{8}")) {
            throw new BusinessException("El DNI debe tener exactamente 8 dígitos.");
        }
        return dni.trim();
    }
}
