package pe.edu.upeu.alquileres.components;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import pe.edu.upeu.alquileres.enums.EstadoContrato;
import pe.edu.upeu.alquileres.enums.EstadoDepartamento;
import pe.edu.upeu.alquileres.enums.MetodoPago;

import java.util.function.Function;

/**
 * Fábricas de celdas reutilizables para las tablas.
 * Convierten estados y métodos de pago en "chips" de color.
 */
public final class Celdas {

    private Celdas() {
    }

    /** Chip para la columna Estado de la tabla de departamentos. */
    public static <S> Callback<TableColumn<S, EstadoDepartamento>, TableCell<S, EstadoDepartamento>>
            chipEstadoDepartamento() {
        return chip(EstadoDepartamento::getDescripcion, estado -> switch (estado) {
            case DISPONIBLE -> "badge-disponible";
            case OCUPADO -> "badge-ocupado";
        });
    }

    /** Chip para la columna Estado de la tabla de contratos. */
    public static <S> Callback<TableColumn<S, EstadoContrato>, TableCell<S, EstadoContrato>>
            chipEstadoContrato() {
        return chip(EstadoContrato::getDescripcion, estado -> switch (estado) {
            case VIGENTE -> "badge-vigente";
            case FINALIZADO -> "badge-finalizado";
        });
    }

    /** Chip para la columna Método de la tabla de pagos. */
    public static <S> Callback<TableColumn<S, MetodoPago>, TableCell<S, MetodoPago>>
            chipMetodoPago() {
        return chip(MetodoPago::getDescripcion, metodo -> switch (metodo) {
            case EFECTIVO -> "badge-disponible";
            case TRANSFERENCIA -> "badge-vigente";
            case YAPE, PLIN -> "badge-info";
            case TARJETA -> "badge-mantenimiento";
        });
    }

    private static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> chip(
            Function<T, String> texto, Function<T, String> estilo) {
        return columna -> new TableCell<>() {
            @Override
            protected void updateItem(T valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label etiqueta = new Label(texto.apply(valor));
                etiqueta.getStyleClass().add("badge");
                String clase = estilo.apply(valor);
                if (clase != null && !clase.isBlank()) {
                    etiqueta.getStyleClass().add(clase);
                }
                setText(null);
                setGraphic(etiqueta);
            }
        };
    }
}
