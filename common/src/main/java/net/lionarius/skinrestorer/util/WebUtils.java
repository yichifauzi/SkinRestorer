package net.lionarius.skinrestorer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class WebUtils {
    
    private WebUtils() {}
    
    public static final String USER_AGENT = "SkinRestorer";
    
    public static String postRequest(URL url, String contentType, String body)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("User-Agent", WebUtils.USER_AGENT);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8), 0, body.length());
        }
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return StringUtils.readString(br);
        }
    }
    
    public static String getRequest(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return StringUtils.readString(br);
        }
    }
}
