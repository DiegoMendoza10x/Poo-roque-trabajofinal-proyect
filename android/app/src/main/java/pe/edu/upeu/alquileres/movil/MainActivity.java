package pe.edu.upeu.alquileres.movil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pantalla principal: listados de los cuatro módulos y registro de pagos. */
public class MainActivity extends Activity {

    private static final String[] MODULOS = {"Departamentos", "Inquilinos", "Contratos", "Pagos"};
    private static final String[] RUTAS = {
            "/rest/v1/departamento?select=numero,piso,direccion,precio_mensual,estado&order=numero",
            "/rest/v1/inquilino?select=dni,nombre,apellido,telefono&order=apellido",
            "/rest/v1/contrato?select=idcontrato,fecha_inicio,fecha_fin,monto_mensual,estado,"
                    + "inquilino(dni,nombre,apellido),departamento(numero)&order=idcontrato",
            "/rest/v1/pago?select=idpago,fecha_pago,periodo,monto,metodo,descuento,igv,"
                    + "contrato(idcontrato,inquilino(nombre,apellido),departamento(numero))&order=idpago.desc"
    };

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final ArrayList<String> datos = new ArrayList<>();
    private ArrayAdapter<String> adaptador;
    private Spinner spinnerModulo;
    private EditText txtBusca;
    private TextView lblConteo;
    private int moduloActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerModulo = findViewById(R.id.spinnerModulo);
        txtBusca = findViewById(R.id.txtBusca);
        lblConteo = findViewById(R.id.lblConteo);
        ListView listView = findViewById(R.id.listView);
        Button btnActualizar = findViewById(R.id.btnActualizar);
        Button btnNuevoPago = findViewById(R.id.btnNuevoPago);
        Button btnSalir = findViewById(R.id.btnSalir);

        ArrayAdapter<String> adaptadorModulos = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MODULOS);
        adaptadorModulos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModulo.setAdapter(adaptadorModulos);

        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, datos);
        listView.setAdapter(adaptador);

        spinnerModulo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                moduloActual = posicion;
                cargar();
            }

            @Override public void onNothingSelected(AdapterView<?> padre) { }
        });

        txtBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                adaptador.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        btnActualizar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { cargar(); }
        });
        btnNuevoPago.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mostrarDialogoPago(); }
        });
        btnSalir.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                getSharedPreferences("sesion", MODE_PRIVATE).edit().clear().apply();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    // ------------------------------------------------------------------
    // Listados
    // ------------------------------------------------------------------

    private void cargar() {
        lblConteo.setText("Cargando…");
        final int indice = moduloActual;
        hilo.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray filas = new JSONArray(SupabaseClient.get(RUTAS[indice]));
                    final ArrayList<String> lineas = new ArrayList<>();
                    for (int i = 0; i < filas.length(); i++) {
                        JSONObject fila = filas.getJSONObject(i);
                        lineas.add(formatear(indice, fila));
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            datos.clear();
                            datos.addAll(lineas);
                            adaptador.notifyDataSetChanged();
                            adaptador.getFilter().filter(txtBusca.getText());
                            lblConteo.setText(datos.size() + " registros · " + MODULOS[indice]);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            lblConteo.setText("Error: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private String formatear(int modulo, JSONObject f) {
        switch (modulo) {
            case 0:
                return "N° " + f.optString("numero") + " · Piso " + f.optString("piso")
                        + " · " + f.optString("direccion")
                        + " · " + dinero(f.optString("precio_mensual"))
                        + " · " + f.optString("estado");
            case 1:
                return f.optString("dni") + " · " + f.optString("nombre") + " "
                        + f.optString("apellido") + " · " + f.optString("telefono");
            case 2:
                JSONObject inq = f.optJSONObject("inquilino");
                JSONObject dep = f.optJSONObject("departamento");
                return "#" + f.optString("idcontrato") + " · "
                        + (inq == null ? "-" : inq.optString("nombre") + " " + inq.optString("apellido"))
                        + " · Dpto. " + (dep == null ? "-" : dep.optString("numero"))
                        + " · " + fecha(f.optString("fecha_inicio")) + " al " + fecha(f.optString("fecha_fin"))
                        + " · " + dinero(f.optString("monto_mensual"))
                        + " · " + f.optString("estado");
            default:
                JSONObject c = f.optJSONObject("contrato");
                JSONObject i = c == null ? null : c.optJSONObject("inquilino");
                JSONObject d = c == null ? null : c.optJSONObject("departamento");
                return "NV-" + numeroNota(f.optString("idpago")) + " · " + fecha(f.optString("fecha_pago"))
                        + " · " + (i == null ? "-" : i.optString("nombre") + " " + i.optString("apellido"))
                        + " · Dpto. " + (d == null ? "-" : d.optString("numero"))
                        + " · " + dinero(f.optString("monto"))
                        + " · " + f.optString("metodo");
        }
    }

    // ------------------------------------------------------------------
    // Registro de pagos y nota de venta
    // ------------------------------------------------------------------

    private void mostrarDialogoPago() {
        final View vista = LayoutInflater.from(this).inflate(R.layout.dialog_pago, null);
        final Spinner cbxContrato = vista.findViewById(R.id.dlgContrato);
        final EditText txtFecha = vista.findViewById(R.id.dlgFecha);
        final EditText txtPeriodo = vista.findViewById(R.id.dlgPeriodo);
        final EditText txtMonto = vista.findViewById(R.id.dlgMonto);
        final EditText txtMetodo = vista.findViewById(R.id.dlgMetodo);
        final EditText txtDescuento = vista.findViewById(R.id.dlgDescuento);
        final EditText txtIgv = vista.findViewById(R.id.dlgIgv);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        txtFecha.setText(fmt.format(new Date()));
        txtPeriodo.setText(fmt.format(new Date()).substring(0, 8) + "01");

        final ArrayList<JSONObject> contratos = new ArrayList<>();
        final ArrayAdapter<String> adaptadorContratos = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<String>());
        adaptadorContratos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cbxContrato.setAdapter(adaptadorContratos);

        hilo.execute(new Runnable() {
            @Override public void run() {
                try {
                    final JSONArray filas = new JSONArray(SupabaseClient.get(
                            "/rest/v1/contrato?select=idcontrato,estado,"
                                    + "inquilino(nombre,apellido),departamento(numero)"
                                    + "&estado=eq.Vigente&order=idcontrato"));
                    final ArrayList<String> opciones = new ArrayList<>();
                    for (int i = 0; i < filas.length(); i++) {
                        JSONObject f = filas.getJSONObject(i);
                        contratos.add(f);
                        JSONObject inq = f.optJSONObject("inquilino");
                        JSONObject dep = f.optJSONObject("departamento");
                        opciones.add("#" + f.optString("idcontrato") + " · "
                                + (inq == null ? "-" : inq.optString("nombre") + " " + inq.optString("apellido"))
                                + " · Dpto. " + (dep == null ? "-" : dep.optString("numero")));
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            adaptadorContratos.clear();
                            adaptadorContratos.addAll(opciones);
                            adaptadorContratos.notifyDataSetChanged();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            lblConteo.setText("No se pudieron cargar los contratos: " + e.getMessage());
                        }
                    });
                }
            }
        });

        final AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Registrar pago")
                .setView(vista)
                .setPositiveButton("Guardar y emitir nota", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                ((android.widget.Button) dialogo.getButton(AlertDialog.BUTTON_POSITIVE))
                        .setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                guardarPago(contratos, cbxContrato, txtFecha, txtPeriodo,
                                        txtMonto, txtMetodo, txtDescuento, txtIgv);
                            }
                        });
            }
        });
        dialogo.show();
    }

    /** Se llama desde el botón positive del diálogo (implementado en onStart para simplificar). */
    private void guardarPago(final ArrayList<JSONObject> contratos, Spinner cbxContrato,
                             EditText txtFecha, EditText txtPeriodo, EditText txtMonto,
                             EditText txtMetodo, EditText txtDescuento, EditText txtIgv) {
        if (contratos.isEmpty() || cbxContrato.getSelectedItemPosition() < 0) {
            lblConteo.setText("No hay contratos vigentes disponibles.");
            return;
        }
        final JSONObject contrato = contratos.get(cbxContrato.getSelectedItemPosition());
        final JSONObject pago = new JSONObject();
        try {
            pago.put("fecha_pago", txtFecha.getText().toString().trim());
            pago.put("periodo", txtPeriodo.getText().toString().trim());
            pago.put("monto", new BigDecimal(txtMonto.getText().toString().trim()));
            pago.put("metodo", txtMetodo.getText().toString().trim());
            pago.put("descuento", new BigDecimal(vacioCero(txtDescuento.getText().toString())));
            pago.put("igv", new BigDecimal(vacioCero(txtIgv.getText().toString())));
            pago.put("idcontrato", contrato.getLong("idcontrato"));
        } catch (Exception e) {
            lblConteo.setText("Revisa los datos del pago: " + e.getMessage());
            return;
        }

        hilo.execute(new Runnable() {
            @Override public void run() {
                try {
                    final String respuesta = SupabaseClient.post("/rest/v1/pago", pago.toString());
                    final JSONArray guardado = new JSONArray(respuesta);
                    final JSONObject fila = guardado.length() > 0 ? guardado.getJSONObject(0) : pago;
                    final String nota = notaVenta(fila, contrato);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            mostrarNota(nota);
                            cargar();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            lblConteo.setText("No se pudo guardar: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void mostrarNota(final String nota) {
        final TextView vistaNota = new TextView(this);
        vistaNota.setText(nota);
        vistaNota.setTextSize(11);
        vistaNota.setTypeface(android.graphics.Typeface.MONOSPACE);
        vistaNota.setPadding(20, 12, 20, 12);

        new AlertDialog.Builder(this)
                .setTitle("Nota de venta")
                .setView(vistaNota)
                .setPositiveButton("Compartir / imprimir", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, nota);
                        startActivity(Intent.createChooser(intent, "Enviar nota de venta"));
                    }
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private static String vacioCero(String texto) {
        return texto == null || texto.trim().isEmpty() ? "0" : texto.trim();
    }

    private static String dinero(String valor) {
        try {
            return String.format(Locale.US, "S/ %.2f", new BigDecimal(valor));
        } catch (Exception e) {
            return "S/ 0.00";
        }
    }

    private static String fecha(String iso) {
        if (iso == null || iso.length() < 10) return "-";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    private static String numeroNota(String id) {
        return String.format(Locale.US, "%06d", Integer.parseInt(id));
    }

    private String notaVenta(JSONObject pago, JSONObject contrato) {
        JSONObject inq = contrato.optJSONObject("inquilino");
        JSONObject dep = contrato.optJSONObject("departamento");
        String cliente = inq == null ? "Cliente varios"
                : inq.optString("nombre") + " " + inq.optString("apellido");
        String dni = inq == null ? "-" : inq.optString("dni");
        String depto = dep == null ? "-" : dep.optString("numero");
        String monto = dinero(pago.optString("monto"));
        String descuento = dinero(pago.optString("descuento"));
        String igv = dinero(pago.optString("igv"));
        double total = new BigDecimal(vacioCero(pago.optString("monto"))).doubleValue()
                - new BigDecimal(vacioCero(pago.optString("descuento"))).doubleValue()
                + new BigDecimal(vacioCero(pago.optString("igv"))).doubleValue();
        SharedPreferences sesion = getSharedPreferences("sesion", MODE_PRIVATE);

        StringBuilder sb = new StringBuilder();
        sb.append(centrar("INMOBILIARIA J && D && J")).append('\n');
        sb.append(centrar("RUC 00000000")).append('\n');
        sb.append(centrar("Salida Arequipa KM 6")).append('\n');
        sb.append(centrar("Tel. 900 876 123")).append('\n');
        sb.append(separador()).append('\n');
        sb.append(centrar("NOTA DE VENTA")).append('\n');
        sb.append(centrar("NV-" + numeroNota(pago.optString("idpago", "0")))).append('\n');
        sb.append(centrar("Emitido: " + new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date()))).append('\n');
        sb.append(separador()).append('\n');
        sb.append("Cliente: " + recortar(cliente, 23)).append('\n');
        sb.append("DNI: " + dni).append('\n');
        sb.append("Depto.: " + depto).append('\n');
        sb.append("Contrato: #" + contrato.optString("idcontrato")).append('\n');
        sb.append("Periodo: " + fecha(pago.optString("periodo"))).append('\n');
        sb.append("Fecha pago: " + fecha(pago.optString("fecha_pago"))).append('\n');
        sb.append("Atendido: " + recortar(sesion.getString("nombre", "Caja"), 22)).append('\n');
        sb.append(separador()).append('\n');
        sb.append(fila("Alquiler", monto)).append('\n');
        sb.append(fila("Descuento", "-" + descuento)).append('\n');
        sb.append(fila("IGV", igv)).append('\n');
        sb.append(separador('-')).append('\n');
        sb.append(fila("TOTAL", String.format(Locale.US, "S/ %.2f", total))).append('\n');
        sb.append(separador()).append('\n');
        sb.append("Metodo: " + pago.optString("metodo")).append('\n');
        sb.append(centrar("Gracias por su preferencia")).append('\n');
        sb.append(centrar("Documento no fiscal")).append('\n');
        sb.append(separador());
        return sb.toString();
    }

    private static final int ANCHO = 32;

    private static String centrar(String texto) {
        String limpio = recortar(texto, ANCHO);
        int espacios = ANCHO - limpio.length();
        int izquierda = espacios / 2;
        return relleno(' ', izquierda) + limpio + relleno(' ', espacios - izquierda);
    }

    private static String fila(String izquierda, String derecha) {
        String der = recortar(derecha, ANCHO - 1);
        String izq = recortar(izquierda, ANCHO - der.length() - 1);
        int espacios = ANCHO - izq.length() - der.length();
        return izq + relleno(' ', Math.max(1, espacios)) + der;
    }

    private static String separador() {
        return separador('-');
    }

    private static String separador(char caracter) {
        return relleno(caracter, ANCHO);
    }

    private static String relleno(char caracter, int cantidad) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cantidad; i++) sb.append(caracter);
        return sb.toString();
    }

    private static String recortar(String texto, int maximo) {
        String limpio = texto == null ? "" : texto.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdownNow();
    }
}
