package pe.edu.upeu.alquileres.movil;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Vista del cliente: muestra SOLAMENTE la información del inquilino en sesión.
 * Consulta la API REST de Supabase filtrando por el DNI (usuario de acceso).
 */
public class ClienteActivity extends Activity {

    private TextView lblTitular, lblDni, lblDepto, lblPiso, lblDireccion;
    private TextView lblContrato, lblInicio, lblFin, lblMonto, lblEstado, lblMensaje;
    private LinearLayout listaPagos;
    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private String dni = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente);

        SharedPreferences sesion = getSharedPreferences("sesion", MODE_PRIVATE);
        dni = sesion.getString("usuario", "");
        SupabaseClient.cabeceraDni = dni;

        lblTitular = findViewById(R.id.lblTitular);
        lblDni = findViewById(R.id.lblDni);
        lblDepto = findViewById(R.id.lblDepto);
        lblPiso = findViewById(R.id.lblPiso);
        lblDireccion = findViewById(R.id.lblDireccion);
        lblContrato = findViewById(R.id.lblContrato);
        lblInicio = findViewById(R.id.lblInicio);
        lblFin = findViewById(R.id.lblFin);
        lblMonto = findViewById(R.id.lblMonto);
        lblEstado = findViewById(R.id.lblEstado);
        lblMensaje = findViewById(R.id.lblMensaje);
        listaPagos = findViewById(R.id.listaPagos);

        findViewById(R.id.btnActualizar).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { cargar(); }
        });

        ((Button) findViewById(R.id.btnSalir)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                getSharedPreferences("sesion", MODE_PRIVATE).edit().clear().apply();
                SupabaseClient.cabeceraDni = "";
                startActivity(new Intent(ClienteActivity.this, LoginActivity.class));
                finish();
            }
        });

        cargar();
    }

    private void cargar() {
        lblMensaje.setText("Cargando tu información…");
        hilo.execute(new Runnable() {
            @Override public void run() {
                try {
                    String d = URLEncoder.encode(dni, "UTF-8");

                    JSONArray inquilinos = new JSONArray(SupabaseClient.get(
                            "/rest/v1/inquilino?dni=eq." + d
                                    + "&select=dni,nombre,apellido,telefono,direccion&limit=1"));

                    JSONArray contratos = new JSONArray(SupabaseClient.get(
                            "/rest/v1/contrato?inquilino.dni=eq." + d
                                    + "&select=idcontrato,fecha_inicio,fecha_fin,monto_mensual,estado,"
                                    + "inquilino(dni),departamento(numero,piso,direccion)"
                                    + "&order=fecha_inicio.desc"));

                    StringBuilder ids = new StringBuilder();
                    for (int i = 0; i < contratos.length(); i++) {
                        if (ids.length() > 0) ids.append(',');
                        ids.append(contratos.optJSONObject(i).optString("idcontrato", "0"));
                    }

                    JSONArray pagos = new JSONArray("[]");
                    if (ids.length() > 0) {
                        pagos = new JSONArray(SupabaseClient.get(
                                "/rest/v1/pago?idcontrato=in.(" + ids + ")"
                                        + "&select=idpago,fecha_pago,periodo,monto,metodo,descuento,igv"
                                        + "&order=fecha_pago.desc"));
                    }

                    final JSONObject inq = inquilinos.length() > 0 ? inquilinos.getJSONObject(0) : null;
                    final JSONObject con = contratos.length() > 0 ? contratos.getJSONObject(0) : null;
                    final JSONArray pag = pagos;

                    runOnUiThread(new Runnable() {
                        @Override public void run() { pintar(inq, con, pag); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            lblMensaje.setText("No se pudo cargar: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void pintar(JSONObject inq, JSONObject con, JSONArray pagos) {
        if (inq != null) {
            String nombre = (inq.optString("nombre", "") + " " + inq.optString("apellido", "")).trim();
            lblTitular.setText(nombre.isEmpty() ? "—" : nombre);
            lblDni.setText("DNI: " + inq.optString("dni", dni));
            lblDireccion.setText("Dirección: " + guion(inq.optString("direccion", "")));
        } else {
            lblTitular.setText("—");
            lblDni.setText("DNI: " + dni);
            lblDireccion.setText("Dirección: —");
        }

        JSONObject dep = con == null ? null : con.optJSONObject("departamento");
        lblDepto.setText("N° " + (dep == null ? guion("") : guion(dep.optString("numero", ""))));
        lblPiso.setText("Piso: " + (dep == null ? "—" : guion(dep.optString("piso", ""))));

        lblContrato.setText("Contrato: " + (con == null ? "—" : "#" + con.optString("idcontrato", "—")));
        lblInicio.setText("Inicio: " + (con == null ? "—" : fecha(con.optString("fecha_inicio", ""))));
        lblFin.setText("Vencimiento: " + (con == null ? "—" : fecha(con.optString("fecha_fin", ""))));
        lblMonto.setText("Monto mensual: " + (con == null ? "—" : dinero(con.optString("monto_mensual", ""))));
        lblEstado.setText("Estado: " + (con == null ? "—" : guion(con.optString("estado", ""))));

        listaPagos.removeAllViews();
        if (pagos.length() == 0) {
            lblMensaje.setText(con == null
                    ? "No se encontró un contrato asociado a tu DNI."
                    : "Aún no hay pagos registrados. Próximo vencimiento: "
                            + fecha(con.optString("fecha_fin", "")));
        } else {
            for (int i = 0; i < pagos.length(); i++) {
                JSONObject p = pagos.optJSONObject(i);
                TextView fila = new TextView(this);
                String texto = "NV-" + numeroNota(p.optString("idpago", "0"))
                        + "  ·  " + fecha(p.optString("fecha_pago", ""))
                        + "  ·  " + dinero(total(p))
                        + "  ·  " + guion(p.optString("metodo", ""));
                fila.setText(texto);
                fila.setTextSize(13);
                fila.setTextColor(getResources().getColor(R.color.texto));
                fila.setPadding(0, 10, 0, 10);
                listaPagos.addView(fila);
            }
            lblMensaje.setText("Último pago: " + fecha(pagos.optJSONObject(0).optString("fecha_pago", ""))
                    + " · Vencimiento del contrato: "
                    + (con == null ? "—" : fecha(con.optString("fecha_fin", ""))));
        }
    }

    // ---------- Ayudas ----------
    private static String fecha(String iso) {
        if (iso == null || iso.length() < 10) return "—";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    private static String dinero(String valor) {
        try {
            return String.format(Locale.US, "S/ %.2f", new BigDecimal(valor));
        } catch (Exception e) {
            return "S/ 0.00";
        }
    }

    private static String total(JSONObject pago) {
        try {
            BigDecimal monto = new BigDecimal(vacioCero(pago.optString("monto", "")));
            BigDecimal desc = new BigDecimal(vacioCero(pago.optString("descuento", "")));
            BigDecimal igv = new BigDecimal(vacioCero(pago.optString("igv", "")));
            return monto.subtract(desc).add(igv).toPlainString();
        } catch (Exception e) {
            return "0";
        }
    }

    private static String vacioCero(String texto) {
        return texto == null || texto.trim().isEmpty() ? "0" : texto.trim();
    }

    private static String guion(String texto) {
        return texto == null || texto.trim().isEmpty() ? "—" : texto.trim();
    }

    private static String numeroNota(String id) {
        try {
            return String.format(Locale.US, "%06d", Integer.parseInt(id));
        } catch (Exception e) {
            return "000000";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdownNow();
    }
}
