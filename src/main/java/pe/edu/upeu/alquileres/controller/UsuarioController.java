package pe.edu.upeu.alquileres.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import pe.edu.upeu.alquileres.components.Dialogos;
import pe.edu.upeu.alquileres.exception.BusinessException;
import pe.edu.upeu.alquileres.model.Usuario;
import pe.edu.upeu.alquileres.service.IUsuarioService;
import pe.edu.upeu.alquileres.service.impl.UsuarioServiceImp;
import pe.edu.upeu.alquileres.util.Formato;

/** Panel de administración de accesos de clientes (recuperación de clave). */
public class UsuarioController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, String> colClave;
    @FXML private TextField txtDni;
    @FXML private TextField txtNombre;
    @FXML private TextField txtClave;
    @FXML private CheckBox chkMostrar;
    @FXML private Label lblMensaje;
    @FXML private Label lblConteo;
    @FXML private Button btnNuevo;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;
    @FXML private Button btnActivar;
    @FXML private Button btnDesactivar;

    private final IUsuarioService service;
    private final ObservableList<Usuario> datos = FXCollections.observableArrayList();
    private FilteredList<Usuario> filtrados;
    private final boolean[] mostrarClaves = {false};

    public UsuarioController(IUsuarioService service) {
        this.service = service;
    }

    @FXML
    public void initialize() {
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colClave.setCellValueFactory(new PropertyValueFactory<>("clave"));
        colClave.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null
                        : (mostrarClaves[0] ? valor : "••••••••"));
            }
        });

        filtrados = new FilteredList<>(datos, u -> true);
        tblUsuarios.setItems(filtrados);
        tblUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        txtBuscar.textProperty().addListener((o, a, b) -> aplicarFiltro());
        chkMostrar.selectedProperty().addListener((o, a, b) -> {
            mostrarClaves[0] = b;
            tblUsuarios.refresh();
        });
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> cargarFormulario(b));

        cargar();
        mensaje("Selecciona un cliente para ver o cambiar su clave.", false);
    }

    private void cargar() {
        datos.setAll(service.listarClientes());
        aplicarFiltro();
        tblUsuarios.getSelectionModel().clearSelection();
    }

    private void aplicarFiltro() {
        if (filtrados == null) return;
        String texto = Formato.normalizar(txtBuscar.getText());
        filtrados.setPredicate(u -> texto.isEmpty()
                || Formato.normalizar(u.getUsuario()).contains(texto)
                || Formato.normalizar(u.getNombre()).contains(texto)
                || Formato.normalizar(u.getRol()).contains(texto));
        lblConteo.setText(filtrados.size() + " clientes");
    }

    private void cargarFormulario(Usuario usuario) {
        txtDni.setText(usuario == null ? "" : usuario.getUsuario());
        txtNombre.setText(usuario == null ? "" : usuario.getNombre());
        txtClave.setText(usuario == null ? "" : usuario.getClave());
        boolean hay = usuario != null;
        btnGuardar.setDisable(!hay);
        btnEliminar.setDisable(!hay);
        btnActivar.setDisable(!hay);
        btnDesactivar.setDisable(!hay);
        if (hay) {
            mensaje("Cliente: " + usuario.getNombre() + " · DNI " + usuario.getUsuario()
                    + " · estado " + usuario.getEstado(), false);
        }
    }

    @FXML
    private void onNuevo() {
        String clave = txtClave.getText();
        if (clave == null || clave.isBlank()) {
            clave = UsuarioServiceImp.generarClave();
            txtClave.setText(clave);
        }
        try {
            Usuario creado = service.crearClienteManual(txtDni.getText(), txtNombre.getText(), clave);
            cargar();
            mensaje("Acceso creado para " + creado.getNombre() + ". Usuario: " + creado.getUsuario()
                    + " · Clave: " + creado.getClave(), false);
        } catch (BusinessException e) {
            mensaje(e.getMessage(), true);
        } catch (Exception e) {
            mensaje("No se pudo crear el acceso: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onEliminar() {
        Usuario seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mensaje("Selecciona un cliente de la tabla.", true);
            return;
        }
        boolean confirmado = Dialogos.confirmar("Eliminar acceso",
                "¿Eliminar el acceso de " + seleccionado.getNombre()
                        + " (" + seleccionado.getUsuario() + ")?");
        if (!confirmado) return;
        try {
            service.eliminar(seleccionado.getIdUsuario());
            cargar();
            mensaje("Acceso eliminado correctamente.", false);
        } catch (Exception e) {
            mensaje("No se pudo eliminar el acceso: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onGenerar() {
        txtClave.setText(UsuarioServiceImp.generarClave());
        mensaje("Clave nueva generada. Presiona Guardar clave para aplicarla.", false);
    }

    @FXML
    private void onGuardar() {
        Usuario seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        try {
            service.cambiarClave(seleccionado == null ? null : seleccionado.getIdUsuario(),
                    txtClave.getText());
            cargar();
            txtClave.setText(txtClave.getText().trim());
            mensaje("Clave actualizada. Entrega la nueva clave al cliente.", false);
        } catch (BusinessException e) {
            mensaje(e.getMessage(), true);
        } catch (Exception e) {
            mensaje("No se pudo actualizar la clave: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onActivar() {
        cambiarEstado("Activo");
    }

    @FXML
    private void onDesactivar() {
        cambiarEstado("Inactivo");
    }

    private void cambiarEstado(String estado) {
        Usuario seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        try {
            service.cambiarEstado(seleccionado == null ? null : seleccionado.getIdUsuario(), estado);
            cargar();
            mensaje("Acceso " + estado.toLowerCase() + " correctamente.", false);
        } catch (BusinessException e) {
            mensaje(e.getMessage(), true);
        } catch (Exception e) {
            mensaje("No se pudo cambiar el estado: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onActualizar() {
        cargar();
        mensaje("Lista de clientes actualizada.", false);
    }

    private void mensaje(String texto, boolean error) {
        lblMensaje.setText(texto == null ? "" : texto);
        lblMensaje.getStyleClass().removeAll("error", "exito");
        lblMensaje.getStyleClass().add(error ? "error" : "exito");
    }
}
