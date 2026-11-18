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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pantalla principal: pestañas, listados con CRUD y nota de venta. */
public class MainActivity extends Activity {

    private static final String[] MODULOS = {"Departamentos", "Inquilinos", "Contratos", "Pagos"};
    private static final String[] RUTAS = {
            "/rest/v1/departamento?select=iddepartamento,numero,piso,direccion,precio_mensual,estado&order=numero",
            "/rest/v1/inquilino?select=idinquilino,dni,nombre,apellido,telefono,direccion&order=apellido",
            "/rest/v1/contrato?select=idcontrato,fecha_inicio,fecha_fin,monto_mensual,estado,inquilino(dni,nombre,apellido),departamento(numero)&order=idcontrato",
            "/rest/v1/pago?select=idpago,fecha_pago,periodo,monto,metodo,descuento,igv,contrato(idcontrato,inquilino(nombre,apellido),departamento(numero))&order=idpago.desc"
    };
    private static final String[] IDS = {"iddepartamento", "idinquilino", "idcontrato", "idpago"};

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final ArrayList<Item> datos = new ArrayList<>();
    private ItemAdapter adaptador;
    private final Button[] pestañas = new Button[4];
    private EditText txtBusca;
    private TextView lblConteo;
    private Button btnNuevo;
    private int moduloActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtBusca = findViewById(R.id.txtBusca);
        lblConteo = findViewById(R.id.lblConteo);
        btnNuevo = findViewById(R.id.btnNuevo);
        ListView listView = findViewById(R.id.listView);
        Button btnActualizar = findViewById(R.id.btnActualizar);
        Button btnSalir = findViewById(R.id.btnSalir);
        TextView lblUsuario = findViewById(R.id.lblUsuario);

        SharedPreferences sesion = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = sesion.getString("nombre", "Administrador");
        lblUsuario.setText(nombre + " · " + sesion.getString("rol", "ADMIN"));

        pestañas[0] = findViewById(R.id.tab0);
        pestañas[1] = findViewById(R.id.tab1);
        pestañas[2] = findViewById(R.id.tab2);
        pestañas[3] = findViewById(R.id.tab3);
        for (int i = 0; i < 4; i++) {
            final int indice = i;
            pestañas[i].setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    moduloActual = indice;
                    marcarPestaña();
                    cargar();
                }
            });
        }

        adaptador = new ItemAdapter(this);
        listView.setAdapter(adaptador);

        txtBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                adaptador.filtrar(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override public void onItemClick(android.widget.AdapterView<?> padre, View vista,
                                              int posicion, long id) {
                final Item elegido = adaptador.getItem(posicion);
                if (elegido == null) return;
                mostrarOpciones(elegido);
            }
        });

        btnActualizar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { cargar(); }
        });
        btnNuevo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { abrirFormulario(-1, null, false); }
        });
        btnSalir.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                getSharedPreferences("sesion", MODE_PRIVATE).edit().clear().apply();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        marcarPestaña();
        cargar();
    }

    private void marcarPestaña() {
        for (int i = 0; i < 4; i++) {
            boolean activo = i == moduloActual;
            pestañas[i].setBackgroundResource(activo
                    ? R.drawable.bg_chip_activo : R.drawable.bg_chip);
            pestañas[i].setTextColor(getResources().getColor(
                    activo ? R.color.blanco : R.color.texto));
        }
        btnNuevo.setText("+ Nuevo " + MODULOS[moduloActual].replace("os", "o")
                .replace("as", "a").toLowerCase());
    }

    private void abrirFormulario(long id, JSONObject item, boolean eliminar) {
        Intent intent = new Intent(this, FormularioActivity.class);
        intent.putExtra("modulo", moduloActual);
        intent.putExtra("id", id);
        if (item != null) intent.putExtra("item", item.toString());
        intent.putExtra("eliminar", eliminar);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) cargar();
    }

    private void mostrarOpciones(final Item elegido) {
        final JSONArray opciones = new JSONArray();
        opciones.put("Editar");
        opciones.put("Eliminar");
        if (moduloActual == 3) opciones.put("Ver nota de venta");

        String[] items = new String[opciones.length()];
        for (int i = 0; i < opciones.length(); i++) items[i] = opciones.optString(i);

        new AlertDialog.Builder(this)
                .setTitle(elegido.titulo)
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        String accion = opciones.optString(which);
                        try {
                            long id = elegido.json.getLong(IDS[moduloActual]);
                            if ("Editar".equals(accion)) {
                                abrirFormulario(id, elegido.json, false);
                            } else if ("Eliminar".equals(accion)) {
                                abrirFormulario(id, elegido.json, true);
                            } else {
                                mostrarNota(notaVenta(elegido.json,
                                        elegido.json.optJSONObject("contrato")));
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    private void cargar() {
        lblConteo.setText("Cargando…");
        final int indice = moduloActual;
        hilo.execute(new Runnable() {
            @Override public void run() {
                try {
                    final JSONArray filas = new JSONArray(SupabaseClient.get(RUTAS[indice]));
                    final ArrayList<Item> lista = new ArrayList<>();
                    for (int i = 0; i < filas.length(); i++) {
                        JSONObject f = filas.getJSONObject(i);
                        lista.add(convertir(indice, f));
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            datos.clear();
                            datos.addAll(lista);
                            adaptador.setDatos(lista);
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

    private Item convertir(int modulo, JSONObject f) {
        switch (modulo) {
            case 0:
                return new Item("N° " + f.optString("numero") + " · " + f.optString("direccion"),
                        "Piso " + f.optString("piso") + " · " + dinero(f.optString("precio_mensual")),
                        f.optString("estado"), f);
            case 1:
                return new Item(f.optString("nombre") + " " + f.optString("apellido"),
                        "DNI " + f.optString("dni") + " · " + f.optString("telefono"),
                        f.optString("direccion"), f);
            case 2:
                JSONObject inq = f.optJSONObject("inquilino");
                JSONObject dep = f.optJSONObject("departamento");
                return new Item("#" + f.optString("idcontrato") + " · "
                                + (inq == null ? "-" : inq.optString("nombre") + " " + inq.optString("apellido")),
                        "Dpto. " + (dep == null ? "-" : dep.optString("numero"))
                                + " · " + fecha(f.optString("fecha_inicio")) + " al " + fecha(f.optString("fecha_fin")),
                        f.optString("estado"), f);
            default:
                JSONObject c = f.optJSONObject("contrato");
                JSONObject i = c == null ? null : c.optJSONObject("inquilino");
                JSONObject d = c == null ? null : c.optJSONObject("departamento");
                return new Item("NV-" + numeroNota(f.optString("idpago")) + " · " + dinero(f.optString("monto")),
                        (i == null ? "-" : i.optString("nombre") + " " + i.optString("apellido"))
                                + " · Dpto. " + (d == null ? "-" : d.optString("numero"))
                                + " · " + fecha(f.optString("fecha_pago")),
                        f.optString("metodo"), f);
        }
    }

    // ------------------------------------------------------------------
    // Nota de venta
    // ------------------------------------------------------------------

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

    private String notaVenta(JSONObject pago, JSONObject contrato) {
        JSONObject inq = contrato == null ? null : contrato.optJSONObject("inquilino");
        JSONObject dep = contrato == null ? null : contrato.optJSONObject("departamento");
        String cliente = inq == null ? "Cliente varios"
                : inq.optString("nombre") + " " + inq.optString("apellido");
        String dni = inq == null ? "-" : inq.optString("dni");
        String depto = dep == null ? "-" : dep.optString("numero");
        double total = new BigDecimal(vacioCero(pago.optString("monto"))).doubleValue()
                - new BigDecimal(vacioCero(pago.optString("descuento"))).doubleValue()
                + new BigDecimal(vacioCero(pago.optString("igv"))).doubleValue();
        String atendido = getSharedPreferences("sesion", MODE_PRIVATE).getString("nombre", "Caja");

        StringBuilder sb = new StringBuilder();
        sb.append(centrar("INMOBILIARIA J && D && J")).append('\n');
        sb.append(centrar("RUC 00000000")).append('\n');
        sb.append(centrar("Salida Arequipa KM 6")).append('\n');
        sb.append(centrar("Tel. 900 876 123")).append('\n');
        sb.append(separador()).append('\n');
        sb.append(centrar("NOTA DE VENTA")).append('\n');
        sb.append(centrar("NV-" + numeroNota(pago.optString("idpago", "0")))).append('\n');
        sb.append(centrar("Emitido: " + new SimpleDateFormat("dd/MM/yyyy", Locale.US)
                .format(new Date()))).append('\n');
        sb.append(separador()).append('\n');
        sb.append("Cliente: " + recortar(cliente, 23)).append('\n');
        sb.append("DNI: " + dni).append('\n');
        sb.append("Depto.: " + depto).append('\n');
        sb.append("Contrato: #" + (contrato == null ? "-" : contrato.optString("idcontrato"))).append('\n');
        sb.append("Periodo: " + fecha(pago.optString("periodo"))).append('\n');
        sb.append("Fecha pago: " + fecha(pago.optString("fecha_pago"))).append('\n');
        sb.append("Atendido: " + recortar(atendido, 22)).append('\n');
        sb.append(separador()).append('\n');
        sb.append(fila("Alquiler", dinero(pago.optString("monto")))).append('\n');
        sb.append(fila("Descuento", "-" + dinero(pago.optString("descuento")))).append('\n');
        sb.append(fila("IGV", dinero(pago.optString("igv")))).append('\n');
        sb.append(separador('-')).append('\n');
        sb.append(fila("TOTAL", String.format(Locale.US, "S/ %.2f", total))).append('\n');
        sb.append(separador()).append('\n');
        sb.append("Metodo: " + pago.optString("metodo")).append('\n');
        sb.append(centrar("Gracias por su preferencia")).append('\n');
        sb.append(centrar("Documento no fiscal")).append('\n');
        sb.append(separador());
        return sb.toString();
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
        try {
            return String.format(Locale.US, "%06d", Integer.parseInt(id));
        } catch (Exception e) {
            return "000000";
        }
    }

    private static final int ANCHO = 32;

    private static String centrar(String texto) {
        String limpio = recortar(texto, ANCHO);
        int espacios = ANCHO - limpio.length();
        int izq = espacios / 2;
        return relleno(' ', izq) + limpio + relleno(' ', espacios - izq);
    }

    private static String fila(String izquierda, String derecha) {
        String der = recortar(derecha, ANCHO - 1);
        String izq = recortar(izquierda, ANCHO - der.length() - 1);
        return izq + relleno(' ', Math.max(1, ANCHO - izq.length() - der.length())) + der;
    }

    private static String separador() {
        return separador('-');
    }

    private static String separador(char c) {
        return relleno(c, ANCHO);
    }

    private static String relleno(char c, int cantidad) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cantidad; i++) sb.append(c);
        return sb.toString();
    }

    private static String recortar(String texto, int maximo) {
        String limpio = texto == null ? "" : texto.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    /** Adaptador propio: mantiene una lista maestra y una lista visible filtrada. */
    private static class ItemAdapter extends BaseAdapter {
        private final Context contexto;
        private final ArrayList<Item> todos = new ArrayList<>();
        private final ArrayList<Item> visibles = new ArrayList<>();

        ItemAdapter(Context contexto) {
            this.contexto = contexto;
        }

        void setDatos(List<Item> nuevos) {
            todos.clear();
            todos.addAll(nuevos);
            filtrar(null);
        }

        void filtrar(String texto) {
            visibles.clear();
            String busqueda = normalizar(texto);
            for (Item item : todos) {
                if (busqueda.isEmpty() || normalizar(item.toString()).contains(busqueda)) {
                    visibles.add(item);
                }
            }
            notifyDataSetChanged();
        }

        private static String normalizar(String texto) {
            if (texto == null || texto.isEmpty()) return "";
            return Normalizer.normalize(texto, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
        }

        @Override public int getCount() {
            return visibles.size();
        }

        @Override public Item getItem(int posicion) {
            return visibles.get(posicion);
        }

        @Override public long getItemId(int posicion) {
            return posicion;
        }

        @Override
        public View getView(int posicion, View convertido, ViewGroup padre) {
            View vista = convertido;
            if (vista == null) {
                vista = LayoutInflater.from(contexto).inflate(R.layout.item_row, padre, false);
            }
            Item item = getItem(posicion);
            ((TextView) vista.findViewById(R.id.itemTitulo)).setText(item.titulo);
            ((TextView) vista.findViewById(R.id.itemSub)).setText(item.sub);
            TextView badge = vista.findViewById(R.id.itemBadge);
            badge.setText(item.badge);
            return vista;
        }
    }

    private static class Item {
        String titulo;
        String sub;
        String badge;
        JSONObject json;

        Item(String titulo, String sub, String badge, JSONObject json) {
            this.titulo = titulo;
            this.sub = sub;
            this.badge = badge;
            this.json = json;
        }

        @Override
        public String toString() {
            return titulo + " " + sub + " " + badge;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdownNow();
    }
}
