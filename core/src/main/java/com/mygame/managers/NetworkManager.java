package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

/**
 * Менеджер сетевого взаимодействия
 * Отвечает за HTTP-запросы к серверу (Yii2 API)
 */
public class NetworkManager {
    
    private SimpleApplication app;
    private String serverUrl = "http://localhost:8080/api";
    private String authToken = null;
    private boolean isConnected = false;
    
    public NetworkManager(SimpleApplication app) {
        this.app = app;
    }
    
    public void initialize() {
        System.out.println("[NetworkManager] Инициализация...");
        loadAuthToken();
    }
    
    public CompletableFuture<Boolean> register(String email, String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[NetworkManager] Регистрация: " + login + ", " + email);
                
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("login", login);
                json.put("password", password);
                
                String response = sendPostRequest("/auth/register", json.toString());
                JSONObject result = new JSONObject(response);
                
                return result.optBoolean("success", false);
            } catch (Exception e) {
                System.out.println("[NetworkManager] Ошибка регистрации: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> login(String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[NetworkManager] Вход: " + login);
                
                JSONObject json = new JSONObject();
                json.put("login", login);
                json.put("password", password);
                
                String response = sendPostRequest("/auth/login", json.toString());
                JSONObject result = new JSONObject(response);
                
                if (result.optBoolean("success", false)) {
                    authToken = result.optString("token");
                    saveAuthToken(authToken);
                    isConnected = true;
                    return true;
                }
                return false;
            } catch (Exception e) {
                System.out.println("[NetworkManager] Ошибка входа: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> checkToken(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[NetworkManager] Проверка токена");
                
                JSONObject json = new JSONObject();
                json.put("token", token);
                
                String response = sendPostRequest("/auth/check", json.toString());
                JSONObject result = new JSONObject(response);
                
                return result.optBoolean("valid", false);
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    public CompletableFuture<Map<String, Object>> loadCharacterData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[NetworkManager] Загрузка данных персонажа...");
                
                if (authToken == null) {
                    return null;
                }
                
                JSONObject json = new JSONObject();
                json.put("token", authToken);
                
                String response = sendPostRequest("/character/data", json.toString());
                JSONObject result = new JSONObject(response);
                
                if (result.optBoolean("success", false)) {
                    Map<String, Object> data = new HashMap<>();
                    JSONObject character = result.getJSONObject("character");
                    
                    data.put("name", character.optString("name", "Экзорцист"));
                    data.put("level", character.optInt("level", 1));
                    data.put("health", character.optInt("health", 100));
                    data.put("maxHealth", character.optInt("maxHealth", 100));
                    data.put("mana", character.optInt("mana", 50));
                    data.put("maxMana", character.optInt("maxMana", 50));
                    data.put("experience", character.optInt("experience", 0));
                    data.put("gold", character.optInt("gold", 100));
                    
                    return data;
                }
                return null;
            } catch (Exception e) {
                System.out.println("[NetworkManager] Ошибка загрузки персонажа: " + e.getMessage());
                return null;
            }
        });
    }
    
    private String sendPostRequest(String endpoint, String jsonBody) throws Exception {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        BufferedReader br = new BufferedReader(
            new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? 
                conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8
            )
        );
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        
        return response.toString();
    }
    
    private void saveAuthToken(String token) {
        System.out.println("[NetworkManager] Токен сохранен");
    }
    
    private void loadAuthToken() {
        authToken = null;
        System.out.println("[NetworkManager] Токен загружен");
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void cleanup() {
        isConnected = false;
        System.out.println("[NetworkManager] Очистка выполнена");
    }
}