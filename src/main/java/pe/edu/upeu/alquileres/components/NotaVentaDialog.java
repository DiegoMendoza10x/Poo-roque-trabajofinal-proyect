package pe.edu.upeu.alquileres.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import pe.edu.upeu.alquileres.impresion.ImpresoraSerial;
import pe.edu.upeu.alquileres.impresion.ImpresoraWindows;
import pe.edu.upeu.alquileres.impresion.NotaVenta;
import pe.edu.upeu.alquileres.model.Pago;

import javax.print.PrintService;
import java.util.List;

/**
 * Ventana de vista previa e impresión de la nota de venta.
 *
 * Permite dos formas de conexión:
 *  1. Impresora instalada en Windows (USB o Bluetooth emparejado).
 *  2. Puerto COM (impresoras Bluetooth que crean un puerto serial).
 */
public final class NotaVentaDialog {

    private NotaVentaDialog() {
    }

    public static void mostrar(Window duenio, Pago pago) {
        if (pago == null) return;

        List<String> lineas = NotaVenta.lineas(pago);

        TextArea vistaPrevia = new TextArea(String.join("\n", lineas));
        vistaPrevia.setEditable(false);
        vistaPrevia.setWrapText(false);
        vistaPrevia.setPrefColumnCount(34);
        vistaPrevia.setPrefRowCount(24);
        vistaPrevia.getStyleClass().add("ticket-previa");
        VBox.setVgrow(vistaPrevia, Priority.ALWAYS);

        // ----- Forma de conexión -----
        ToggleGroup grupo = new ToggleGroup();
        RadioButton rbWindows = new RadioButton("Windows (USB / Bluetooth)");
        RadioButton rbSerial = new RadioButton("Puerto COM (Bluetooth)");
        rbWindows.setToggleGroup(grupo);
        rbSerial.setToggleGroup(grupo);
        rbWindows.setSelected(true);

        // ----- Impresoras de Windows -----
        ComboBox<PrintService> cbxImpresoras = new ComboBox<>();
        cbxImpresoras.getItems().setAll(ImpresoraWindows.listar());
        cbxImpresoras.setMaxWidth(Double.MAX_VALUE);
        cbxImpresoras.setPromptText("No se encontraron impresoras instaladas");
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
        PrintService predeterminada = ImpresoraWindows.predeterminada();
        if (predeterminada != null) {
            cbxImpresoras.getItems().stream()
                    .filter(p -> p.getName().equals(predeterminada.getName()))
                    .findFirst()
                    .ifPresent(p -> cbxImpresoras.getSelectionModel().select(p));
        }
        if (cbxImpresoras.getValue() == null && !cbxImpresoras.getItems().isEmpty()) {
            cbxImpresoras.getSelectionModel().selectFirst();
        }

        // ----- Puertos COM -----
        ComboBox<String> cbxPuertos = new ComboBox<>();
        cbxPuertos.getItems().setAll(ImpresoraSerial.listarPuertos());
        cbxPuertos.setMaxWidth(Double.MAX_VALUE);
        cbxPuertos.setPromptText("No se encontraron puertos COM");
        if (!cbxPuertos.getItems().isEmpty()) {
            cbxPuertos.getSelectionModel().selectFirst();
        }

        ComboBox<Integer> cbxBaudios = new ComboBox<>();
        cbxBaudios.getItems().setAll(9600, 19200, 38400, 57600, 115200);
        cbxBaudios.getSelectionModel().select(Integer.valueOf(9600));

        Runnable actualizarControles = () -> {
            boolean windows = rbWindows.isSelected();
            cbxImpresoras.setDisable(!windows);
            cbxPuertos.setDisable(windows);
            cbxBaudios.setDisable(windows);
        };
        grupo.selectedToggleProperty().addListener((o, anterior, actual) -> actualizarControles.run());
        actualizarControles.run();

        // ----- Botones y mensajes -----
        Label estado = new Label("Selecciona la conexión y presiona Imprimir.");
        estado.setWrapText(true);
        estado.getStyleClass().add("texto-ayuda");

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        if (duenio != null) {
            ventana.initOwner(duenio);
        }
        ventana.setTitle("Nota de venta · 58 mm");

        Button btnImprimir = new Button("Imprimir nota");
        btnImprimir.getStyleClass().add("boton-primario");
        btnImprimir.setDefaultButton(true);

        Button btnCopiar = new Button("Copiar texto");
        btnCopiar.getStyleClass().add("boton-secundario");

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("boton-secundario");

        btnCerrar.setOnAction(e -> ventana.close());

        btnCopiar.setOnAction(e -> {
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(String.join("\n", lineas));
            Clipboard.getSystemClipboard().setContent(contenido);
            mostrarEstado(estado, "Nota copiada al portapapeles.", false);
        });

        btnImprimir.setOnAction(e -> {
            try {
                if (rbWindows.isSelected()) {
                    PrintService impresora = cbxImpresoras.getValue();
                    if (impresora == null) {
                        throw new IllegalStateException("Selecciona una impresora de Windows.");
                    }
                    ImpresoraWindows.imprimir(impresora, lineas);
                } else {
                    String puerto = cbxPuertos.getValue();
                    if (puerto == null) {
                        throw new IllegalStateException("Selecciona un puerto COM.");
                    }
                    int baudios = cbxBaudios.getValue() == null ? 9600 : cbxBaudios.getValue();
                    ImpresoraSerial.imprimir(puerto, baudios, lineas);
                }
                mostrarEstado(estado, "Ticket enviado a la impresora.", false);
            } catch (Exception ex) {
                mostrarEstado(estado, "No se pudo imprimir: " + ex.getMessage(), true);
            }
        });

        // ----- Distribución de la ventana -----
        Label tituloPrevia = new Label("Vista previa · 58 mm");
        tituloPrevia.getStyleClass().add("card-titulo");

        Label ayudaPrevia = new Label("32 caracteres por línea · documento no fiscal");
        ayudaPrevia.getStyleClass().add("texto-ayuda");

        Label tituloConexion = new Label("Forma de conexión");
        tituloConexion.getStyleClass().add("card-titulo");

        VBox campoImpresora = new VBox(4, etiqueta("Impresora de Windows"), cbxImpresoras);
        VBox campoPuerto = new VBox(4, etiqueta("Puerto COM"), cbxPuertos);
        VBox campoBaudios = new VBox(4, etiqueta("Velocidad"), cbxBaudios);

        HBox filaConexion = new HBox(12, campoPuerto, campoBaudios);
        HBox.setHgrow(campoPuerto, Priority.ALWAYS);
        campoBaudios.setPrefWidth(130);

        HBox botones = new HBox(10, btnCopiar, btnCerrar, btnImprimir);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox tarjetaPrevia = new VBox(8, tituloPrevia, ayudaPrevia, vistaPrevia);
        tarjetaPrevia.getStyleClass().add("ticket-marco");
        VBox.setVgrow(vistaPrevia, Priority.ALWAYS);

        VBox tarjetaConexion = new VBox(10,
                tituloConexion,
                new HBox(18, rbWindows, rbSerial),
                campoImpresora,
                filaConexion);
        tarjetaConexion.getStyleClass().add("ticket-marco");

        VBox raiz = new VBox(12, tarjetaPrevia, tarjetaConexion, estado, botones);
        raiz.setPadding(new Insets(16));
        raiz.getStyleClass().add("modulo");

        Scene escena = new Scene(raiz, 520, 720);
        var css = NotaVentaDialog.class.getResource("/css/app.css");
        if (css != null) {
            escena.getStylesheets().add(css.toExternalForm());
        }

        ventana.setScene(escena);
        ventana.setMinWidth(520);
        ventana.setMinHeight(660);
        ventana.showAndWait();
    }

    private static Label etiqueta(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("campo-etiqueta");
        return etiqueta;
    }

    private static void mostrarEstado(Label etiqueta, String texto, boolean error) {
        etiqueta.setText(texto);
        etiqueta.getStyleClass().removeAll("error", "exito", "texto-ayuda");
        etiqueta.getStyleClass().add(error ? "error" : "exito");
    }
}
