package pe.edu.upeu.alquileres.movil;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pantalla de acceso: valida credenciales contra la tabla usuario de Supabase. */
public class LoginActivity extends Activity {

    private EditText txtUsuario;
    private EditText txtClave;
    private TextView mensaje;
    private final ExecutorService hilo = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtUsuario = findViewById(R.id.txtUsuario);
        txtClave = findViewById(R.id.txtClave);
        mensaje = findViewById(R.id.mensaje);
        Button btnIngresar = findViewById(R.id.btnIngresar);

        SharedPreferences sesion = getSharedPreferences("sesion", MODE_PRIVATE);
        if (sesion.getBoolean("activa", false)) {
            abrirPrincipal();
            finish();
        }

        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ingresar();
            }
        });
    }

    private void ingresar() {
        final String usuario = txtUsuario.getText().toString().trim();
        final String clave = txtClave.getText().toString().trim();
        if (usuario.isEmpty() || clave.isEmpty()) {
            mensaje.setText("Ingresa usuario y clave.");
            return;
        }

        mensaje.setText("Validando…");
        hilo.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String u = URLEncoder.encode(usuario, "UTF-8");
                    String c = URLEncoder.encode(clave, "UTF-8");
                    String ruta = "/rest/v1/usuario?usuario=eq." + u + "&clave=eq." + c
                            + "&select=usuario,nombre,rol&limit=1";
                    JSONArray filas = new JSONArray(SupabaseClient.get(ruta));

                    if (filas.length() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                mensaje.setText("Usuario o clave incorrectos.");
                            }
                        });
                        return;
                    }

                    final JSONObject usuarioEncontrado = filas.getJSONObject(0);
                    getSharedPreferences("sesion", MODE_PRIVATE).edit()
                            .putBoolean("activa", true)
                            .putString("nombre", usuarioEncontrado.optString("nombre", "Usuario"))
                            .putString("rol", usuarioEncontrado.optString("rol", "ADMIN"))
                            .putString("usuario", usuario)
                            .apply();

                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            abrirPrincipal();
                            finish();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            mensaje.setText("No se pudo conectar: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void abrirPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdownNow();
    }
}
