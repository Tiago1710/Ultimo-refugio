package UltimoRef;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Login/registro con Supabase Auth + guardado de partida. Sin librerías externas. */
public class SupabaseClient {

    // ---- Sesión actual (la usa todo el juego) ----
    public static String username;
    public static String userId;
    private static String token;
    private static String refreshToken;

    private static final String DOMINIO = "@elultimorefugio.com";

    private static class Resp {
        int codigo;
        String cuerpo;
    }

    // =====================================================================
    // HTTP
    // =====================================================================

    private static Resp request(String metodo, String ruta, String body,
                                String bearer, String prefer) throws IOException {
        HttpURLConnection con = (HttpURLConnection)
                new URL(SupabaseConfig.URL + ruta).openConnection();
        con.setRequestMethod(metodo);
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);
        con.setRequestProperty("apikey", SupabaseConfig.API_KEY);
        con.setRequestProperty("Content-Type", "application/json");
        if (bearer != null) con.setRequestProperty("Authorization", "Bearer " + bearer);
        if (prefer != null) con.setRequestProperty("Prefer", prefer);

        if (body != null) {
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        Resp r = new Resp();
        r.codigo = con.getResponseCode();
        InputStream is = (r.codigo >= 400) ? con.getErrorStream() : con.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
            }
        }
        r.cuerpo = sb.toString();
        return r;
    }

    /** Request autenticado; si el token venció (401) lo renueva y reintenta una vez. */
    private static Resp authed(String metodo, String ruta, String body, String prefer)
            throws IOException {
        Resp r = request(metodo, ruta, body, token, prefer);
        if (r.codigo == 401 && refreshToken != null) {
            Resp rr = request("POST", "/auth/v1/token?grant_type=refresh_token",
                    "{\"refresh_token\":\"" + refreshToken + "\"}", null, null);
            if (rr.codigo == 200) {
                guardarSesion(rr.cuerpo);
                r = request(metodo, ruta, body, token, prefer);
            }
        }
        return r;
    }

    // =====================================================================
    // AUTH
    // =====================================================================

    public static void registrar(String user, String pass) throws Exception {
        String u = validarUsuario(user);
        validarPass(pass);
        Resp r = request("POST", "/auth/v1/signup",
                "{\"email\":\"" + u + DOMINIO + "\",\"password\":\"" + esc(pass) + "\"}",
                null, null);
        if (r.codigo >= 400) throw new Exception(traducirError(r.cuerpo));
        if (extraer(r.cuerpo, "\"access_token\"\\s*:\\s*\"([^\"]+)\"") == null) {
            throw new Exception("Desactivá 'Confirm email' en Supabase (Authentication → Providers → Email).");
        }
        guardarSesion(r.cuerpo);
        username = u;
    }

    public static void login(String user, String pass) throws Exception {
        String u = validarUsuario(user);
        Resp r = request("POST", "/auth/v1/token?grant_type=password",
                "{\"email\":\"" + u + DOMINIO + "\",\"password\":\"" + esc(pass) + "\"}",
                null, null);
        if (r.codigo >= 400) throw new Exception(traducirError(r.cuerpo));
        guardarSesion(r.cuerpo);
        username = u;
    }

    public static void cerrarSesion() {
        username = null; userId = null; token = null; refreshToken = null;
    }

    private static void guardarSesion(String json) {
        token = extraer(json, "\"access_token\"\\s*:\\s*\"([^\"]+)\"");
        refreshToken = extraer(json, "\"refresh_token\"\\s*:\\s*\"([^\"]+)\"");
        String id = extraer(json, "\"user\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"");
        if (id != null) userId = id;
    }

    private static String validarUsuario(String user) throws Exception {
        String u = user == null ? "" : user.trim().toLowerCase();
        if (!u.matches("[a-z0-9_]{3,20}")) {
            throw new Exception("El usuario debe tener 3 a 20 caracteres (letras, números o _).");
        }
        return u;
    }

    private static void validarPass(String pass) throws Exception {
        if (pass == null || pass.length() < 6) {
            throw new Exception("La contraseña debe tener al menos 6 caracteres.");
        }
    }

    private static String traducirError(String json) {
        String m = extraer(json, "\"(?:msg|error_description|message)\"\\s*:\\s*\"([^\"]+)\"");
        if (m == null) return "Error de conexión con el servidor.";
        String l = m.toLowerCase();
        if (l.contains("invalid login")) return "Usuario o contraseña incorrectos.";
        if (l.contains("already registered")) return "Ese usuario ya existe.";
        if (l.contains("password")) return "Contraseña inválida: " + m;
        return m;
    }

    // =====================================================================
    // PARTIDA
    // =====================================================================

    public static void guardarPartida(Juego j) throws IOException {
        StringBuilder d = new StringBuilder();
        for (String linea : j.getDiario()) d.append(linea).append("\n");

        // Se guarda la puntuación base (sin el bonus de misiones) para no duplicar al cargar
        String json = "{"
                + "\"user_id\":\"" + userId + "\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"rol\":\"" + esc(j.getRol()) + "\","
                + "\"salud\":" + j.getSalud() + ","
                + "\"provisiones\":" + j.getProvisiones() + ","
                + "\"medicinas\":" + j.getMedicinas() + ","
                + "\"municion\":" + j.getMunicion() + ","
                + "\"defensa\":" + j.getDefensa() + ","
                + "\"dias\":" + j.getDias() + ","
                + "\"puntuacion\":" + j.getPuntuacion() + ","
                + "\"vivo\":" + j.isVivo() + ","
                + "\"diario\":\"" + esc(d.toString()) + "\","
                + "\"updated_at\":\"" + java.time.Instant.now() + "\""
                + "}";

        Resp r = authed("POST", "/rest/v1/partidas", json, "resolution=merge-duplicates");
        if (r.codigo >= 400) throw new IOException("No se pudo guardar: " + r.cuerpo);
    }

    /** Devuelve la partida guardada o null si no hay (o si el personaje murió). */
    public static Juego cargarPartida() throws IOException {
        Resp r = authed("GET", "/rest/v1/partidas?user_id=eq." + userId + "&select=*", null, null);
        if (r.codigo >= 400) throw new IOException("No se pudo cargar: " + r.cuerpo);
        String b = r.cuerpo;
        if (b == null || b.length() < 3 || !b.contains("\"rol\"")) return null;
        if (b.contains("\"vivo\":false")) return null;

        String rol = extraer(b, "\"rol\"\\s*:\\s*\"([^\"]*)\"");
        String diarioRaw = extraer(b, "\"diario\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

        List<String> diario = new ArrayList<>();
        if (diarioRaw != null) {
            String txt = diarioRaw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            for (String l : txt.split("\n")) if (!l.isEmpty()) diario.add(l);
        }

        return new Juego(rol,
                num(b, "salud"), num(b, "provisiones"), num(b, "medicinas"),
                num(b, "municion"), num(b, "defensa"), num(b, "dias"),
                num(b, "puntuacion"), diario);
    }

    public static boolean haySesion() {
        return token != null && userId != null;
    }

    public static void borrarPartida() throws IOException {
        authed("DELETE", "/rest/v1/partidas?user_id=eq." + userId, null, null);
    }

    // =====================================================================
    // UTILIDADES
    // =====================================================================

    private static int num(String json, String campo) {
        String v = extraer(json, "\"" + campo + "\"\\s*:\\s*(-?\\d+)");
        return v == null ? 0 : Integer.parseInt(v);
    }

    private static String extraer(String texto, String regex) {
        Matcher m = Pattern.compile(regex).matcher(texto);
        return m.find() ? m.group(1) : null;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
