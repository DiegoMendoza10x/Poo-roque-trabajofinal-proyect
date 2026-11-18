package pe.edu.upeu.alquileres.movil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente mínimo de la API REST de Supabase (PostgREST).
 * Usa HttpURLConnection para no depender de librerías externas.
 */
public final class SupabaseClient {

    public static final String URL_BASE = "https://zarbbelqcsivzhjrpguu.supabase.co";
    public static final String API_KEY = "sb_publishable_wkoLJTTv6EeGN0d4QvLKHQ_7-OBCFPN";

    private SupabaseClient() {
    }

    public static String get(String ruta) throws IOException {
        return peticion("GET", ruta, null);
    }

    public static String post(String ruta, String json) throws IOException {
        return peticion("POST", ruta, json);
    }

    private static String peticion(String metodo, String ruta, String cuerpo) throws IOException {
        HttpURLConnection conexion = (HttpURLConnection) new URL(URL_BASE + ruta).openConnection();
        conexion.setRequestMethod(metodo);
        conexion.setConnectTimeout(15000);
        conexion.setReadTimeout(15000);
        conexion.setRequestProperty("apikey", API_KEY);
        conexion.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conexion.setRequestProperty("Accept", "application/json");
        conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if ("POST".equals(metodo)) {
            conexion.setDoOutput(true);
            conexion.setRequestProperty("Prefer", "return=representation");
        }

        if (cuerpo != null) {
            try (OutputStream salida = conexion.getOutputStream()) {
                salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
            }
        }

        int codigo = conexion.getResponseCode();
        InputStream flujo = codigo >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
        String respuesta = flujo == null ? "" : new String(leer(flujo), StandardCharsets.UTF_8);
        conexion.disconnect();

        if (codigo >= 400) {
            throw new IOException("HTTP " + codigo + ": " + respuesta);
        }
        return respuesta;
    }

    private static byte[] leer(InputStream flujo) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] datos = new byte[4096];
        int n;
        while ((n = flujo.read(datos)) != -1) {
            buffer.write(datos, 0, n);
        }
        return buffer.toByteArray();
    }
}
