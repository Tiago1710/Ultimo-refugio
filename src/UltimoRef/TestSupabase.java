/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UltimoRef;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestSupabase {

    public static void main(String[] args) throws Exception {

        URL url = new URL(SupabaseConfig.URL + "/rest/v1/ranking?select=*&limit=1");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("apikey", SupabaseConfig.API_KEY);

        int codigo = con.getResponseCode();

        InputStream is = (codigo >= 400) ? con.getErrorStream() : con.getInputStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea);
            }
        }

        System.out.println("Código: " + codigo);
        System.out.println("Respuesta: " + sb);
    }
    
}