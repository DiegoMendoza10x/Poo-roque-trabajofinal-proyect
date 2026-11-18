package pe.edu.upeu.alquileres.movil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Formulario genérico: alta, edición y borrado de los cuatro módulos. */
public class FormularioActivity extends Activity {

    private static final String[] TABLAS = {"departamento", "inquilino", "contrato", "pago"};
    private static final String[] CLAVES = {"iddepartamento", "idinquilino", "idcontrato", "idpago"};
    private static final String[] NOMBRES = {"Departamento", "Inquilino", "Contrato", "Pago"};

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final List<VistaCampo> campos = new ArrayList<>();
    private final Map<String, String> ocultos = new LinkedHashMap<>();

    private String modulo;
    private String tabla;
    private String clavePrimaria;
    private long id = -1;
    private JSONObject item;
    private LinearLayout contenedor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario);

        int indice = getIntent().getIntExtra("modulo", 0);
        modulo = NOMBRES[indice];
        tabla = TABLAS[indice];
        clavePrimaria = CLAVES[indice];
        id = getIntent().getLongExtra("id", -1);
        String json = getIntent().getStringExtra("item");
        try {
            item = json == null ? new JSONObject() : new JSONObject(json);
        } catch (Exception e) {
            item = new JSONObject();
        }

        ((TextView) findViewById(R.id.tituloForm)).setText((id < 0 ? "Nuevo " : "Editar ") + modulo);
        contenedor = findViewById(R.id.contenedorCampos);

        construirCampos(indice);

        Button btnGuardar = findViewById(R.id.btnGuardar);
        Button btnCancelar = findViewById(R.id.btnCancelar);
        Button btnEliminar = findViewById(R.id.btnEliminar);
        btnEliminar.setVisibility(id < 0 ? View.GONE : View.VISIBLE);

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { guardar(); }
        });
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmarEliminacion(); }
        });

        if (getIntent().getBooleanExtra("eliminar", false)) {
            confirmarEliminacion();
        }
    }

    // ------------------------------------------------------------------
    // Construcción dinámica del formulario
    // ------------------------------------------------------------------

    private void construirCampos(int indice) {
        switch (indice) {
            case 0: // Departamentos
                agregar(Campo.texto("numero", "Número"));
                agregar(Campo.numero("piso", "Piso"));
                agregar(Campo.texto("direccion", "Dirección"));
                agregar(Campo.numero("precio_mensual", "Precio mensual (S/)"));
                agregar(Campo.opciones("estado", "Estado", "Disponible", "Ocupado"));
                break;
            case 1: // Inquilinos
                agregar(Campo.texto("dni", "DNI"));
                agregar(Campo.texto("nombre", "Nombre"));
                agregar(Campo.texto("apellido", "Apellido"));
                agregar(Campo.texto("telefono", "Teléfono"));
                agregar(Campo.texto("direccion", "Dirección"));
                agregar(Campo.lookup("idtipopersoneria", "Personería",
                        "/rest/v1/tipo_personeria?select=idtipopersoneria,nombre&order=idtipopersoneria",
                        "idtipopersoneria", "nombre"));
                agregar(Campo.texto("rep_legal", "Representante legal"));
                agregar(Campo.texto("ruc", "RUC"));
                agregar(Campo.texto("direccion_fiscal", "Dirección fiscal"));
                break;
            case 2: // Contratos
                agregar(Campo.lookup("idinquilino", "Inquilino",
                        "/rest/v1/inquilino?select=idinquilino,nombre,apellido&order=apellido",
                        "idinquilino", "nombre", "apellido"));
                agregar(Campo.lookup("iddepartamento", "Departamento",
                        "/rest/v1/departamento?select=iddepartamento,numero,direccion&order=numero",
                        "iddepartamento", "numero", "direccion"));
                agregar(Campo.fecha("fecha_inicio", "Fecha inicio"));
                agregar(Campo.fecha("fecha_fin", "Fecha fin"));
                agregar(Campo.numero("monto_mensual", "Monto mensual (S/)"));
                agregar(Campo.numero("cantidad_persona", "Cantidad de personas"));
                agregar(Campo.opciones("estado", "Estado", "Vigente", "Finalizado"));
                if (id < 0) {
                    ocultos.put("fecha_contrato",
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
                }
                break;
            default: // Pagos
                agregar(Campo.lookup("idcontrato", "Contrato",
                        "/rest/v1/contrato?select=idcontrato,estado,inquilino(nombre,apellido),"
                                + "departamento(numero)&estado=eq.Vigente&order=idcontrato",
                        "idcontrato", "idcontrato", "inquilino.nombre", "inquilino.apellido",
                        "departamento.numero"));
                agregar(Campo.fecha("fecha_pago", "Fecha de pago"));
                agregar(Campo.fecha("periodo", "Periodo"));
                agregar(Campo.numero("monto", "Monto (S/)"));
                agregar(Campo.opciones("metodo", "Método",
                        "Efectivo", "Transferencia", "Yape", "Plin", "Tarjeta"));
                agregar(Campo.numero("descuento", "Descuento", "0"));
                agregar(Campo.numero("igv", "IGV", "0"));
                break;
        }
    }

    private void agregar(final Campo campo) {
        TextView etiqueta = new TextView(this);
        etiqueta.setText(campo.etiqueta);
        etiqueta.setTextColor(getResources().getColor(R.color.texto));
        etiqueta.setTextSize(12);
        etiqueta.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = contenedor.getChildCount() > 0 ? 12 : 0;
        contenedor.addView(etiqueta, lp);

        VistaCampo vista;
        if ("opciones".equals(campo.tipo) || "lookup".equals(campo.tipo)) {
            Spinner spinner = new Spinner(this);
            spinner.setBackgroundResource(R.drawable.bg_campo);
            spinner.setPadding(8, 8, 8, 8);
            ArrayAdapter<String> adaptador = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, new ArrayList<String>());
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adaptador);
            vista = new VistaCampo(campo, spinner, adaptador);
            contenedor.addView(spinner, lp);

            if ("opciones".equals(campo.tipo)) {
                for (String opcion : campo.opciones) adaptador.add(opcion);
                adaptador.notifyDataSetChanged();
                seleccionarValor(vista, item.optString(campo.clave));
            } else {
                cargarLookup(vista, campo);
            }
        } else {
            EditText edit = new EditText(this);
            edit.setBackgroundResource(R.drawable.bg_campo);
            edit.setPadding(10, 10, 10, 10);
            edit.setInputType("numero".equals(campo.tipo)
                    ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    : ("fecha".equals(campo.tipo)
                        ? InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE
                        : InputType.TYPE_CLASS_TEXT));
            String valor = item.optString(campo.clave, campo.valorInicial);
            if (!valor.isEmpty()) edit.setText(valor);
            vista = new VistaCampo(campo, edit);
            contenedor.addView(edit, lp);
        }
        campos.add(vista);
    }

    private void cargarLookup(final VistaCampo vista, final Campo campo) {
        final ProgressDialog espera = ProgressDialog.show(this, "Cargando", campo.etiqueta + "…", true);
        hilo.execute(new Runnable() {
            @Override public void run() {
                try {
                    final JSONArray filas = new JSONArray(SupabaseClient.get(campo.endpoint));
                    final ArrayList<String> opciones = new ArrayList<>();
                    final ArrayList<JSONObject> objetos = new ArrayList<>();
                    for (int i = 0; i < filas.length(); i++) {
                        JSONObject f = filas.getJSONObject(i);
                        objetos.add(f);
                        StringBuilder texto = new StringBuilder();
                        for (int j = 0; j < campo.display.length; j++) {
                            String parte = mostrar(f, campo.display[j]);
                            if (!parte.isEmpty()) {
                                if (texto.length() > 0) texto.append(" · ");
                                texto.append(parte);
                            }
                        }
                        opciones.add(texto.length() > 0 ? texto.toString() : f.toString());
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            vista.adaptador.clear();
                            vista.adaptador.addAll(opciones);
                            vista.adaptador.notifyDataSetChanged();
                            vista.objetos = objetos;
                            String buscado = item.optString(campo.clave);
                            for (int i = 0; i < objetos.size(); i++) {
                                if (objetos.get(i).optString(campo.valor).equals(buscado)) {
                                    ((Spinner) vista.vista).setSelection(i);
                                    break;
                                }
                            }
                            espera.dismiss();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            espera.dismiss();
                            Toast.makeText(FormularioActivity.this,
                                    "Error cargando " + campo.etiqueta + ": " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private String mostrar(JSONObject objeto, String ruta) {
        if (objeto == null) return "";
        int punto = ruta.indexOf('.');
        if (punto < 0) return objeto.optString(ruta, "");
        JSONObject hijo = objeto.optJSONObject(ruta.substring(0, punto));
        return mostrar(hijo, ruta.substring(punto + 1));
    }

    private void seleccionarValor(VistaCampo vista, String valor) {
        for (int i = 0; i < vista.adaptador.getCount(); i++) {
            if (vista.adaptador.getItem(i).equals(valor)) {
                ((Spinner) vista.vista).setSelection(i);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Guardar / eliminar
    // ------------------------------------------------------------------

    private void guardar() {
        JSONObject cuerpo = new JSONObject();
        try {
            for (VistaCampo vc : campos) {
                Campo campo = vc.campo;
                String valor;
                if (vc.vista instanceof EditText) {
                    valor = ((EditText) vc.vista).getText().toString().trim();
                } else {
                    Spinner spinner = (Spinner) vc.vista;
                    if ("lookup".equals(campo.tipo)) {
                        if (vc.objetos == null || vc.objetos.isEmpty()
                                || spinner.getSelectedItemPosition() < 0) {
                            throw new IllegalArgumentException("Selecciona " + campo.etiqueta);
                        }
                        JSONObject elegido = vc.objetos.get(spinner.getSelectedItemPosition());
                        cuerpo.put(campo.clave, elegido.get(campo.valor));
                        continue;
                    }
                    valor = spinner.getSelectedItem() == null ? "" : spinner.getSelectedItem().toString();
                }
                if ("numero".equals(campo.tipo)) {
                    cuerpo.put(campo.clave, new BigDecimal(valor.isEmpty() ? "0" : valor));
                } else {
                    cuerpo.put(campo.clave, valor);
                }
            }
            for (Map.Entry<String, String> entry : ocultos.entrySet()) {
                cuerpo.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Revisa los campos: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        final JSONObject datos = cuerpo;
        final ProgressDialog espera = ProgressDialog.show(this, "Guardando", modulo, true);
        hilo.execute(new Runnable() {
            @Override public void run() {
                try {
                    if (id < 0) {
                        SupabaseClient.post("/rest/v1/" + tabla, datos.toString());
                    } else {
                        SupabaseClient.patch("/rest/v1/" + tabla + "?" + clavePrimaria + "=eq." + id,
                                datos.toString());
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            espera.dismiss();
                            Toast.makeText(FormularioActivity.this, "Registro guardado", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            espera.dismiss();
                            Toast.makeText(FormularioActivity.this,
                                    "No se pudo guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar " + modulo.toLowerCase())
                .setMessage("¿Seguro que deseas eliminar este registro?")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        eliminar();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminar() {
        final ProgressDialog espera = ProgressDialog.show(this, "Eliminando", modulo, true);
        hilo.execute(new Runnable() {
            @Override public void run() {
                try {
                    SupabaseClient.delete("/rest/v1/" + tabla + "?" + clavePrimaria + "=eq." + id);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            espera.dismiss();
                            Toast.makeText(FormularioActivity.this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            espera.dismiss();
                            Toast.makeText(FormularioActivity.this,
                                    "No se pudo eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdownNow();
    }

    // ------------------------------------------------------------------
    // Clases auxiliares
    // ------------------------------------------------------------------

    private static class Campo {
        String clave;
        String etiqueta;
        String tipo;
        String[] opciones = new String[0];
        String[] display = new String[0];
        String endpoint;
        String valor;
        String valorInicial = "";

        private Campo(String clave, String etiqueta, String tipo) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.tipo = tipo;
        }

        static Campo texto(String clave, String etiqueta) {
            return new Campo(clave, etiqueta, "texto");
        }

        static Campo texto(String clave, String etiqueta, String inicial) {
            Campo c = texto(clave, etiqueta);
            c.valorInicial = inicial;
            return c;
        }

        static Campo numero(String clave, String etiqueta) {
            return new Campo(clave, etiqueta, "numero");
        }

        static Campo numero(String clave, String etiqueta, String inicial) {
            Campo c = numero(clave, etiqueta);
            c.valorInicial = inicial;
            return c;
        }

        static Campo fecha(String clave, String etiqueta) {
            Campo c = new Campo(clave, etiqueta, "fecha");
            c.valorInicial = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            return c;
        }

        static Campo opciones(String clave, String etiqueta, String... opciones) {
            Campo c = new Campo(clave, etiqueta, "opciones");
            c.opciones = opciones;
            return c;
        }

        static Campo lookup(String clave, String etiqueta, String endpoint, String valor, String... display) {
            Campo c = new Campo(clave, etiqueta, "lookup");
            c.endpoint = endpoint;
            c.valor = valor;
            c.display = display;
            return c;
        }
    }

    private static class VistaCampo {
        Campo campo;
        View vista;
        ArrayAdapter<String> adaptador;
        List<JSONObject> objetos;

        VistaCampo(Campo campo, View vista) {
            this.campo = campo;
            this.vista = vista;
        }

        VistaCampo(Campo campo, View vista, ArrayAdapter<String> adaptador) {
            this(campo, vista);
            this.adaptador = adaptador;
        }
    }
}
