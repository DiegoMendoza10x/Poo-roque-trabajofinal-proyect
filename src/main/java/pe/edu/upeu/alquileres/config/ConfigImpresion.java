package pe.edu.upeu.alquileres.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuración persistente de la impresora térmica.
 *
 * Se guarda en el perfil del usuario (~/.SysAlquileres/impresion.properties)
 * para que la elección de papel, conexión, impresora y puerto se recuerde
 * entre ejecuciones del sistema.
 */
public final class ConfigImpresion {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigImpresion.class);
    private static final Path ARCHIVO = Paths.get(
            System.getProperty("user.home"), ".SysAlquileres", "impresion.properties");

    private static final ConfigImpresion INSTANCIA = new ConfigImpresion();

    private int anchoPapel = 58;
    private boolean usarWindows = true;
    private String nombreImpresora = "";
    private String puertoCOM = "";
    private int baudios = 9600;

    private ConfigImpresion() {
        cargar();
    }

    public static ConfigImpresion getInstance() {
        return INSTANCIA;
    }

    /** Carga la configuración guardada; si no existe, deja los valores por defecto. */
    public void cargar() {
        if (!Files.exists(ARCHIVO)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(ARCHIVO)) {
            props.load(in);
            anchoPapel = Integer.parseInt(props.getProperty("papel", "58"));
            usarWindows = Boolean.parseBoolean(props.getProperty("usar.windows", "true"));
            nombreImpresora = props.getProperty("impresora", "");
            puertoCOM = props.getProperty("puerto.com", "");
            baudios = Integer.parseInt(props.getProperty("baudios", "9600"));
            LOG.info("Configuración de impresión cargada desde {}", ARCHIVO);
        } catch (IOException | NumberFormatException e) {
            LOG.warn("No se pudo leer la configuración de impresión: {}", e.getMessage());
        }
    }

    /** Guarda la configuración actual en disco. */
    public void guardar() {
        Properties props = new Properties();
        props.setProperty("papel", String.valueOf(anchoPapel));
        props.setProperty("usar.windows", String.valueOf(usarWindows));
        props.setProperty("impresora", nombreImpresora == null ? "" : nombreImpresora);
        props.setProperty("puerto.com", puertoCOM == null ? "" : puertoCOM);
        props.setProperty("baudios", String.valueOf(baudios));
        try {
            Files.createDirectories(ARCHIVO.getParent());
            try (OutputStream out = Files.newOutputStream(ARCHIVO)) {
                props.store(out, "SysAlquileres · Configuración de impresión térmica");
            }
            LOG.info("Configuración de impresión guardada en {}", ARCHIVO);
        } catch (IOException e) {
            LOG.warn("No se pudo guardar la configuración de impresión: {}", e.getMessage());
        }
    }

    /** Guarda todos los valores de una vez (lo usa el diálogo de impresión). */
    public void guardar(int anchoPapel, boolean usarWindows, String nombreImpresora,
                        String puertoCOM, int baudios) {
        this.anchoPapel = anchoPapel == 80 ? 80 : 58;
        this.usarWindows = usarWindows;
        this.nombreImpresora = nombreImpresora == null ? "" : nombreImpresora;
        this.puertoCOM = puertoCOM == null ? "" : puertoCOM;
        this.baudios = baudios > 0 ? baudios : 9600;
        guardar();
    }

    public int getAnchoPapel() {
        return anchoPapel;
    }

    public void setAnchoPapel(int anchoPapel) {
        this.anchoPapel = anchoPapel == 80 ? 80 : 58;
    }

    public boolean isUsarWindows() {
        return usarWindows;
    }

    public void setUsarWindows(boolean usarWindows) {
        this.usarWindows = usarWindows;
    }

    public String getNombreImpresora() {
        return nombreImpresora;
    }

    public void setNombreImpresora(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora == null ? "" : nombreImpresora;
    }

    public String getPuertoCOM() {
        return puertoCOM;
    }

    public void setPuertoCOM(String puertoCOM) {
        this.puertoCOM = puertoCOM == null ? "" : puertoCOM;
    }

    public int getBaudios() {
        return baudios;
    }

    public void setBaudios(int baudios) {
        this.baudios = baudios > 0 ? baudios : 9600;
    }

    /** Ruta del archivo, para mostrarla en el panel de configuración. */
    public static String rutaArchivo() {
        return ARCHIVO.toString();
    }
}
