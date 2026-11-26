package pe.edu.upeu.alquileres.impresion;

import pe.edu.upeu.alquileres.model.Contrato;
import pe.edu.upeu.alquileres.model.Inquilino;
import pe.edu.upeu.alquileres.model.Pago;
import pe.edu.upeu.alquileres.util.Formato;
import pe.edu.upeu.alquileres.util.Sesion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el texto de la nota de venta para impresora térmica.
 *
 * Con fuente normal (12 puntos por carácter):
 *   - Papel de 58 mm: 384 puntos / 12 = 32 caracteres por línea.
 *   - Papel de 80 mm: 576 puntos / 12 = 48 caracteres por línea.
 */
public final class NotaVenta {

    public static final int ANCHO_58 = 32;
    public static final int ANCHO_80 = 48;

    /** Se mantiene por compatibilidad con código que usaba solo 58 mm. */
    public static final int ANCHO = ANCHO_58;

    private NotaVenta() {
    }

    /** Devuelve cuántos caracteres caben según el ancho de papel en milímetros. */
    public static int columnas(int milimetros) {
        return milimetros >= 80 ? ANCHO_80 : ANCHO_58;
    }

    /** Ticket para papel de 58 mm (32 columnas). */
    public static List<String> lineas(Pago pago) {
        return lineas(pago, 58);
    }

    /** Ticket para el ancho de papel indicado en milímetros (58 u 80). */
    public static List<String> lineas(Pago pago, int anchoMm) {
        int ancho = columnas(anchoMm);
        BigDecimal monto = valor(pago.getMonto());
        BigDecimal descuento = valor(pago.getDescuento());
        BigDecimal igv = valor(pago.getIgv());
        BigDecimal total = monto.subtract(descuento).add(igv);

        List<String> lineas = new ArrayList<>();
        lineas.add(centrar(DatosEmpresa.NOMBRE, ancho));
        lineas.add(centrar("RUC " + DatosEmpresa.RUC, ancho));
        lineas.add(centrar(DatosEmpresa.DIRECCION, ancho));
        lineas.add(centrar(DatosEmpresa.TELEFONO, ancho));
        lineas.add(separador('-', ancho));

        lineas.add(centrar("NOTA DE VENTA", ancho));
        lineas.add(centrar(numero(pago), ancho));
        lineas.add("Emitido: " + Formato.fechaHora(LocalDateTime.now()));
        lineas.add(separador('-', ancho));

        lineas.add("Cliente: " + recortar(nombreCliente(pago), ancho - 9));
        lineas.add("DNI: " + dniCliente(pago));
        lineas.add("Depto.: " + numeroDepartamento(pago));
        lineas.add("Contrato: #" + numeroContrato(pago));
        lineas.add("Periodo: " + pago.getPeriodoTexto());
        lineas.add("Fecha pago: " + pago.getFechaTexto());
        lineas.add("Atendido: " + atendidoPor(ancho));
        lineas.add(separador('-', ancho));

        lineas.add(fila("Alquiler", Formato.dinero(monto), ancho));
        lineas.add(fila("Descuento", "-" + Formato.dinero(descuento), ancho));
        lineas.add(fila("IGV", Formato.dinero(igv), ancho));
        lineas.add(separador('-', ancho));
        lineas.add(fila("TOTAL", Formato.dinero(total), ancho));
        lineas.add(separador('-', ancho));

        lineas.add("Metodo: " + pago.getMetodoTexto());
        lineas.add(centrar("Gracias por su preferencia", ancho));
        lineas.add(centrar("Documento no fiscal", ancho));
        lineas.add(separador('-', ancho));
        return lineas;
    }

    /** Ticket corto para probar la impresora desde el panel de configuración. */
    public static List<String> lineasPrueba(int anchoMm) {
        int ancho = columnas(anchoMm);
        List<String> lineas = new ArrayList<>();
        lineas.add(centrar(DatosEmpresa.NOMBRE, ancho));
        lineas.add(centrar("RUC " + DatosEmpresa.RUC, ancho));
        lineas.add(separador('-', ancho));
        lineas.add(centrar("PRUEBA DE IMPRESION", ancho));
        lineas.add("Fecha: " + Formato.fecha(java.time.LocalDate.now()));
        lineas.add("Papel: " + anchoMm + " mm · " + ancho + " columnas");
        lineas.add(separador('-', ancho));
        lineas.add("Si puedes leer esto, la");
        lineas.add("impresora esta configurada");
        lineas.add("correctamente.");
        lineas.add(separador('-', ancho));
        lineas.add(centrar("SYSALQUILERES", ancho));
        return lineas;
    }

    /** Calcula lo que realmente paga el inquilino: monto - descuento + IGV. */
    public static BigDecimal total(Pago pago) {
        return valor(pago.getMonto())
                .subtract(valor(pago.getDescuento()))
                .add(valor(pago.getIgv()));
    }

    /** Número de la nota, por ejemplo NV-000123. */
    public static String numero(Pago pago) {
        long id = pago.getIdPago() == null ? 0 : pago.getIdPago();
        return String.format("NV-%06d", id);
    }

    // ------------------------------------------------------------------
    // Ayudas para que todas las líneas midan exactamente el ancho elegido
    // ------------------------------------------------------------------

    private static String centrar(String texto, int ancho) {
        String limpio = recortar(texto, ancho);
        int espacios = ancho - limpio.length();
        int izquierda = espacios / 2;
        return " ".repeat(izquierda) + limpio + " ".repeat(espacios - izquierda);
    }

    private static String fila(String izquierda, String derecha, int ancho) {
        String der = recortar(derecha, ancho - 1);
        String izq = recortar(izquierda, ancho - der.length() - 1);
        int espacios = ancho - izq.length() - der.length();
        return izq + " ".repeat(Math.max(1, espacios)) + der;
    }

    private static String separador(char caracter, int ancho) {
        return String.valueOf(caracter).repeat(ancho);
    }

    private static String recortar(String texto, int maximo) {
        String limpio = texto == null ? "" : texto.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, Math.max(0, maximo));
    }

    private static BigDecimal valor(BigDecimal numero) {
        return numero == null ? BigDecimal.ZERO : numero;
    }

    private static String nombreCliente(Pago pago) {
        Contrato contrato = pago.getContrato();
        Inquilino inquilino = contrato == null ? null : contrato.getInquilino();
        return inquilino == null ? "Cliente varios" : inquilino.getNombreCompleto().trim();
    }

    private static String dniCliente(Pago pago) {
        Contrato contrato = pago.getContrato();
        Inquilino inquilino = contrato == null ? null : contrato.getInquilino();
        return inquilino == null || inquilino.getDni() == null ? "-" : inquilino.getDni();
    }

    private static String numeroDepartamento(Pago pago) {
        return pago.getDepartamentoNumero() == null || pago.getDepartamentoNumero().isBlank()
                ? "-" : pago.getDepartamentoNumero();
    }

    private static String numeroContrato(Pago pago) {
        return pago.getContrato() == null || pago.getContrato().getIdContrato() == null
                ? "-" : String.valueOf(pago.getContrato().getIdContrato());
    }

    private static String atendidoPor(int ancho) {
        String nombre = Sesion.nombre();
        return nombre == null || nombre.isBlank() ? "Caja" : recortar(nombre, ancho - 10);
    }
}
