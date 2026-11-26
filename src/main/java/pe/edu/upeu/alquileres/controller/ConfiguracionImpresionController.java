package pe.edu.upeu.alquileres.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import pe.edu.upeu.alquileres.config.ConfigImpresion;
import pe.edu.upeu.alquileres.config.DatabaseConfig;
import pe.edu.upeu.alquileres.impresion.ImpresoraSerial;
import pe.edu.upeu.alquileres.impresion.ImpresoraWindows;
import pe.edu.upeu.alquileres.impresion.NotaVenta;

import javax.print.PrintService;
import java.util.List;

/**
 * Panel para configurar la impresora térmica: papel, conexión, impresora,
 * puerto y velocidad. Guarda la configuración en disco y permite probar.
 */
public class ConfiguracionImpresionController {

    @FXML private ComboBox<String> cbxModo;
    @FXML private ComboBox<Integer> cbxPapel;
    @FXML private ComboBox<PrintService> cbxImpresoras;
    @FXML private ComboBox<String> cbxPuertos;
    @FXML private ComboBox<Integer> cbxBaudios;
    @FXML private Label lblEstado;
    @FXML private Label lblBase;
    @FXML private Label lblRuta;

    private final ConfigImpresion config = ConfigImpresion.getInstance();

    @FXML
    public void initialize() {
        cbxModo.getItems().setAll("Cable / Windows (controlador)", "Bluetooth / puerto COM");
        cbxPapel.getItems().setAll(58, 80);
        cbxBaudios.getItems().setAll(9600, 19200, 38400, 57600, 115200);

        cbxImpresoras.setConverter(new StringConverter<>() {
            @Override
            public String toString(PrintService servicio) {
                return servicio == null ? "" : servicio.getName();
            }

            @Override
            public PrintService fromString(String texto) {
                return null;
            }
        });

        actualizarListas();

        cbxModo.getSelectionModel().select(config.isUsarWindows() ? 0 : 1);
        cbxPapel.getSelectionModel().select(Integer.valueOf(config.getAnchoPapel()));
        cbxBaudios.getSelectionModel().select(Integer.valueOf(config.getBaudios()));
        seleccionarImpresora(config.getNombreImpresora());
        seleccionarPuerto(config.getPuertoCOM());

        cbxModo.valueProperty().addListener((o, anterior, actual) -> actualizarControles());
        actualizarControles();

        lblBase.setText("Base de datos activa: " + DatabaseConfig.motorActivo()
                + "\n" + DatabaseConfig.urlActiva());
        lblRuta.setText("Archivo de configuración: " + ConfigImpresion.rutaArchivo());
        mensaje("Configuración cargada. Cambia los valores y presiona Guardar.", false);
    }

    @FXML
    private void onGuardar() {
        config.guardar(anchoPapel(), esWindows(), nombreImpresora(), puerto(), baudios());
        mensaje("Configuración guardada correctamente.", false);
    }

    @FXML
    private void onProbar() {
        try {
            List<String> lineas = NotaVenta.lineasPrueba(anchoPapel());
            if (esWindows()) {
                PrintService impresora = cbxImpresoras.getValue();
                if (impresora == null) {
                    throw new IllegalStateException("Selecciona una impresora de Windows.");
                }
                ImpresoraWindows.imprimir(impresora, lineas, anchoPapel());
            } else {
                String puerto = cbxPuertos.getValue();
                if (puerto == null) {
                    throw new IllegalStateException("Selecciona un puerto COM.");
                }
                ImpresoraSerial.imprimir(puerto, baudios(), lineas);
            }
            mensaje("Impresión de prueba enviada correctamente.", false);
        } catch (Exception e) {
            mensaje("No se pudo imprimir la prueba: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onActualizar() {
        actualizarListas();
        seleccionarImpresora(config.getNombreImpresora());
        seleccionarPuerto(config.getPuertoCOM());
        mensaje("Listas de impresoras y puertos actualizadas.", false);
    }

    private void actualizarListas() {
        cbxImpresoras.getItems().setAll(ImpresoraWindows.listar());
        cbxPuertos.getItems().setAll(ImpresoraSerial.listarPuertos());
    }

    private void seleccionarImpresora(String nombre) {
        PrintService selected = null;
        if (nombre != null && !nombre.isBlank()) {
            selected = cbxImpresoras.getItems().stream()
                    .filter(p -> p.getName().equals(nombre))
                    .findFirst().orElse(null);
        }
        if (selected == null) {
            PrintService predeterminada = ImpresoraWindows.predeterminada();
            if (predeterminada != null) {
                selected = cbxImpresoras.getItems().stream()
                        .filter(p -> p.getName().equals(predeterminada.getName()))
                        .findFirst().orElse(null);
            }
        }
        if (selected == null && !cbxImpresoras.getItems().isEmpty()) {
            selected = cbxImpresoras.getItems().get(0);
        }
        if (selected != null) cbxImpresoras.getSelectionModel().select(selected);
    }

    private void seleccionarPuerto(String nombre) {
        if (nombre != null && !nombre.isBlank() && cbxPuertos.getItems().contains(nombre)) {
            cbxPuertos.getSelectionModel().select(nombre);
        } else if (!cbxPuertos.getItems().isEmpty()) {
            cbxPuertos.getSelectionModel().selectFirst();
        }
    }

    private void actualizarControles() {
        boolean windows = esWindows();
        cbxImpresoras.setDisable(!windows);
        cbxPuertos.setDisable(windows);
        cbxBaudios.setDisable(windows);
    }

    private boolean esWindows() {
        String modo = cbxModo.getValue();
        return modo == null || modo.startsWith("Cable");
    }

    private int anchoPapel() {
        Integer papel = cbxPapel.getValue();
        return papel == null ? 58 : papel;
    }

    private String nombreImpresora() {
        PrintService impresora = cbxImpresoras.getValue();
        return impresora == null ? "" : impresora.getName();
    }

    private String puerto() {
        String puerto = cbxPuertos.getValue();
        return puerto == null ? "" : puerto;
    }

    private int baudios() {
        Integer baudios = cbxBaudios.getValue();
        return baudios == null ? 9600 : baudios;
    }

    private void mensaje(String texto, boolean error) {
        lblEstado.setText(texto == null ? "" : texto);
        lblEstado.getStyleClass().removeAll("error", "exito", "texto-ayuda");
        lblEstado.getStyleClass().add(error ? "error" : "exito");
    }
}
