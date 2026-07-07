package com.example.myapo.network;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CloudflareApi {

    private static final String BASE = "https://api.cloudflare.com/client/v4";
    private static final int TIMEOUT_MS = 20_000;

    public static class Response {
        public final int code;
        public final String body;
        public Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
        public boolean isSuccess() { return code >= 200 && code < 300; }
    }

    public static Response get(String path, String token) throws Exception {
        HttpURLConnection conn = open(BASE + path, token);
        conn.setRequestMethod("GET");
        return read(conn);
    }

    public static Response putJson(String path, String token, String json) throws Exception {
        HttpURLConnection conn = open(BASE + path, token);
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        writeBody(conn, json.getBytes(StandardCharsets.UTF_8));
        return read(conn);
    }

    public static Response putScript(String path, String token, String js) throws Exception {
        HttpURLConnection conn = open(BASE + path, token);
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/javascript");
        writeBody(conn, js.getBytes(StandardCharsets.UTF_8));
        return read(conn);
    }

    public static Response delete(String path, String token) throws Exception {
        HttpURLConnection conn = open(BASE + path, token);
        conn.setRequestMethod("DELETE");
        return read(conn);
    }

    public static Response postJson(String path, String token, String json) throws Exception {
        HttpURLConnection conn = open(BASE + path, token);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        writeBody(conn, json.getBytes(StandardCharsets.UTF_8));
        return read(conn);
    }

    private static HttpURLConnection open(String urlStr, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, byte[] data) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(data);
        }
    }

    private static Response read(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        } finally {
            conn.disconnect();
        }
        return new Response(code, sb.toString());
    }
}
