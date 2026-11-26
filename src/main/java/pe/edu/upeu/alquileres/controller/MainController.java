package pe.edu.upeu.alquileres.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import pe.edu.upeu.alquileres.App;
import pe.edu.upeu.alquileres.components.Dialogos;
import pe.edu.upeu.alquileres.config.AppContext;
import pe.edu.upeu.alquileres.util.Sesion;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private StackPane contenido;
    @FXML private Label lblTitulo;
    @FXML private Label lblSub;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label lblAvatar;
    @FXML private Button btnDepartamentos;
    @FXML private Button btnInquilinos;
    @FXML private Button btnContratos;
    @FXML private Button btnPagos;
    @FXML private Button btnConfiguracion;
    @FXML private Button btnUsuarios;

    private static final String CLASE_ACTIVO = "nav-button-activo";

    private record Vista(String archivo, String titulo, String subtitulo, Button boton) {
    }

    private final Map<String, Vista> vistas = new HashMap<>();
    private final Map<String, Parent> cache = new HashMap<>();
    private String vistaActual;

    @FXML
    public void initialize() {
        vistas.put("departamentos", new Vista("departamentos.fxml", "Departamentos",
                "Administra los departamentos disponibles y sus precios", btnDepartamentos));
        vistas.put("inquilinos", new Vista("inquilinos.fxml", "Inquilinos",
                "Registra personas naturales y empresas jurídicas", btnInquilinos));
        vistas.put("contratos", new Vista("contratos.fxml", "Contratos",
                "Controla la vigencia de los alquileres", btnContratos));
        vistas.put("pagos", new Vista("pagos.fxml", "Pagos y notas de venta",
                "Registra pagos y emite la nota de venta en 58 mm", btnPagos));
        vistas.put("configuracion", new Vista("configuracion-impresion.fxml", "Configuración de impresión",
                "Papel, conexión y prueba de la impresora térmica", btnConfiguracion));
        vistas.put("usuarios", new Vista("usuarios.fxml", "Accesos de clientes",
                "Consulta y recupera la clave de acceso a la APK", btnUsuarios));

        mostrarUsuario();
        abrir("departamentos");
    }

    private void mostrarUsuario() {
        String nombre = Sesion.nombre();
        lblUsuario.setText(nombre == null || nombre.isBlank() ? "Administrador" : nombre);
        lblAvatar.setText(iniciales(lblUsuario.getText()));
        if (Sesion.actual() != null && Sesion.actual().getRol() != null) {
            lblRol.setText(Sesion.actual().getRol());
        }
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "A";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, 1).toUpperCase();
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    @FXML
    private void irDepartamentos() {
        abrir("departamentos");
    }

    @FXML
    private void irInquilinos() {
        abrir("inquilinos");
    }

    @FXML
    private void irContratos() {
        abrir("contratos");
    }

    @FXML
    private void irPagos() {
        abrir("pagos");
    }

    @FXML
    private void irConfiguracion() {
        abrir("configuracion");
    }

    @FXML
    private void irUsuarios() {
        abrir("usuarios");
    }

    private void abrir(String clave) {
        Vista vista = vistas.get(clave);
        if (vista == null || clave.equals(vistaActual)) return;

        Parent root = cache.get(clave);
        if (root == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + vista.archivo()));
                loader.setControllerFactory(AppContext.getInstance()::getBean);
                root = loader.load();
                cache.put(clave, root);
            } catch (Exception e) {
                e.printStackTrace();
                Dialogos.informar("No se pudo abrir el módulo",
                        "Ocurrió un problema al cargar la pantalla: " + e.getMessage());
                return;
            }
        }

        vistaActual = clave;
        contenido.getChildren().setAll(root);
        lblTitulo.setText(vista.titulo());
        lblSub.setText(vista.subtitulo());
        marcarBotonActivo(vista.boton());
    }

    private void marcarBotonActivo(Button activo) {
        for (Vista vista : vistas.values()) {
            vista.boton().getStyleClass().remove(CLASE_ACTIVO);
        }
        if (activo != null && !activo.getStyleClass().contains(CLASE_ACTIVO)) {
            activo.getStyleClass().add(CLASE_ACTIVO);
        }
    }

    @FXML
    private void onCerrarSesion() {
        boolean confirmado = Dialogos.confirmar("Cerrar sesión",
                "¿Deseas cerrar la sesión y volver al login?");
        if (!confirmado) return;
        Sesion.cerrar();
        App.mostrarLogin();
    }

    @FXML
    private void onSalir() {
        Platform.exit();
    }

    @FXML
    private void onAcercaDe() {
        Dialogos.informar("Acerca de SysAlquileres",
                "Sistema de gestión de alquileres de departamentos.\n"
                        + "Módulos: Departamentos, Inquilinos, Contratos y Pagos.\n"
                        + "Incluye impresión de notas de venta en 58 mm.\n"
                        + "POO 2026 · UPeU Juliaca");
    }
}
