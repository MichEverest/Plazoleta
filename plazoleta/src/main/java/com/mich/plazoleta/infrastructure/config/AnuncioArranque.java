package com.mich.plazoleta.infrastructure.config;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuando los 4 microservicios responden, imprime en la terminal un agujero negro hecho de caracteres con un panel al
 * estilo fastfetch (estado y URL de cada servicio). Así se sabe de un vistazo que el proyecto terminó de arrancar.
 * Se desactiva con plazoleta.anuncio.enabled=false; los colores se quitan con plazoleta.anuncio.color=false o NO_COLOR.
 */
@Component
@ConditionalOnProperty(name = "plazoleta.anuncio.enabled", havingValue = "true", matchIfMissing = true)
public class AnuncioArranque {

    private static final Duration ESPERA_MAXIMA = Duration.ofMinutes(3);
    private static final Duration INTERVALO = Duration.ofSeconds(1);
    private static final Duration TIMEOUT_PETICION = Duration.ofSeconds(2);
    private static final int ANCHO_LOGO = 50;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String RESET = "\u001B[0m";
    private static final String NEGRITA = "\u001B[1m";
    private static final String MORADO = "\u001B[38;5;141m";
    private static final String GRIS = "\u001B[38;5;245m";
    private static final String VERDE = "\u001B[38;5;114m";
    private static final String ROJO = "\u001B[38;5;203m";
    private static final String CIAN = "\u001B[38;5;117m";
    private static final Map<Character, String> COLOR_LOGO = Map.of(
            '█', "\u001B[38;5;229m",
            '▓', "\u001B[38;5;220m",
            '▒', "\u001B[38;5;208m",
            '░', "\u001B[38;5;166m",
            '·', "\u001B[38;5;245m",
            '*', "\u001B[38;5;255m",
            '+', "\u001B[38;5;250m",
            '.', "\u001B[38;5;240m");

    private final Environment environment;
    private final boolean color;
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT_PETICION)
            .build();

    public AnuncioArranque(Environment environment) {
        this.environment = environment;
        this.color = environment.getProperty("plazoleta.anuncio.color", Boolean.class, true)
                && System.getenv("NO_COLOR") == null;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        Thread hilo = new Thread(this::esperarYMostrar, "anuncio-arranque");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void esperarYMostrar() {
        Map<String, String> servicios = servicios();
        long limite = System.nanoTime() + ESPERA_MAXIMA.toNanos();
        Map<String, Boolean> estado = consultar(servicios);
        while (estado.containsValue(false) && System.nanoTime() < limite) {
            dormir();
            estado = consultar(servicios);
        }
        System.out.print(componer(servicios, estado));
        System.out.flush();
    }

    private Map<String, String> servicios() {
        String puerto = environment.getProperty("server.port", "8082");
        Map<String, String> servicios = new LinkedHashMap<>();
        servicios.put("Usuarios", environment.getProperty("services.usuarios.url", "http://localhost:8081"));
        servicios.put("Plazoleta", "http://localhost:" + puerto);
        servicios.put("Trazabilidad", environment.getProperty("services.trazabilidad.url", "http://localhost:8083"));
        servicios.put("Mensajería", environment.getProperty("services.mensajeria.url", "http://localhost:8084"));
        return servicios;
    }

    private Map<String, Boolean> consultar(Map<String, String> servicios) {
        Map<String, Boolean> estado = new LinkedHashMap<>();
        servicios.forEach((nombre, url) -> estado.put(nombre, responde(url)));
        return estado;
    }

    private boolean responde(String url) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(url + "/")).timeout(TIMEOUT_PETICION).GET().build();
            return http.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode() < 500;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void dormir() {
        try {
            Thread.sleep(INTERVALO.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String componer(Map<String, String> servicios, Map<String, Boolean> estado) {
        List<String> logo = leerLogo();
        List<String> panel = panel(servicios, estado);
        int desplazamiento = Math.max(0, (logo.size() - panel.size()) / 2);
        StringBuilder salida = new StringBuilder(System.lineSeparator());
        for (int i = 0; i < Math.max(logo.size(), panel.size() + desplazamiento); i++) {
            String linea = i < logo.size() ? logo.get(i) : "";
            salida.append(pintarLogo(linea)).append(" ".repeat(Math.max(0, ANCHO_LOGO - linea.length())));
            int indicePanel = i - desplazamiento;
            if (indicePanel >= 0 && indicePanel < panel.size()) {
                salida.append(panel.get(indicePanel));
            }
            salida.append(System.lineSeparator());
        }
        return salida.append(System.lineSeparator()).toString();
    }

    private List<String> panel(Map<String, String> servicios, Map<String, Boolean> estado) {
        boolean todoListo = !estado.containsValue(false);
        long segundos = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        List<String> lineas = new ArrayList<>();
        lineas.add(c(NEGRITA + MORADO) + "PLAZOLETA DE COMIDAS" + c(RESET) + c(GRIS) + "  ·  " + c(RESET)
                + (todoListo ? c(VERDE) + "todo listo" : c(ROJO) + "faltan servicios") + c(RESET));
        lineas.add(c(GRIS) + "─".repeat(48) + c(RESET));
        servicios.forEach((nombre, url) -> {
            boolean ok = estado.get(nombre);
            lineas.add(c(MORADO) + String.format("%-15s", nombre) + c(RESET)
                    + (ok ? c(VERDE) + "● arriba  " : c(ROJO) + "● caído   ") + c(RESET) + c(CIAN) + url + c(RESET));
        });
        lineas.add(c(GRIS) + "─".repeat(48) + c(RESET));
        lineas.add(dato("Perfil", String.join(", ", environment.getActiveProfiles().length > 0
                ? environment.getActiveProfiles() : environment.getDefaultProfiles())));
        lineas.add(dato("Java", System.getProperty("java.version")));
        lineas.add(dato("Spring Boot", SpringBootVersion.getVersion()));
        lineas.add(dato("Arrancó", LocalTime.now().format(HORA) + "  (listo en " + segundos + " s)"));
        lineas.add(dato("Base de datos", servicios.get("Plazoleta") + "/h2-console"));
        lineas.add(dato("Entrar como", "mich · cartethyia · jean · mari"));
        lineas.add(dato("Clave", "Clave123*"));
        return lineas;
    }

    private String dato(String etiqueta, String valor) {
        return c(MORADO) + String.format("%-15s", etiqueta) + c(RESET) + valor;
    }

    private String pintarLogo(String linea) {
        if (!color) {
            return linea;
        }
        StringBuilder pintada = new StringBuilder();
        for (char caracter : linea.toCharArray()) {
            String tono = COLOR_LOGO.get(caracter);
            pintada.append(tono == null ? String.valueOf(caracter) : tono + caracter + RESET);
        }
        return pintada.toString();
    }

    private String c(String codigo) {
        return color ? codigo : "";
    }

    private static List<String> leerLogo() {
        try {
            String texto = new ClassPathResource("agujero-negro.txt").getContentAsString(StandardCharsets.UTF_8);
            return texto.lines().toList();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
