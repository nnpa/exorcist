package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NetworkManager {
    
    private SimpleApplication app;
    private String serverUrl = "http://eczo/";
    private String authToken = null;
    private boolean isConnected = false;
    private static final String TOKEN_FILE_NAME = ".exorcist_token.txt";

    public NetworkManager(SimpleApplication app) {
        this.app = app;
    }

    public void initialize() {
        System.out.println("[NetworkManager] Инициализация...");
        loadAuthToken();
    }

    // ---- Auth ----
    public CompletableFuture<Boolean> register(String email, String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("login", login);
                json.put("password", password);
                String response = sendPostRequest("/auth/register", json.toString(), null);
                System.out.println("[NetworkManager] Register response: " + response);
                JSONObject result = new JSONObject(response);
                return result.optBoolean("success", false);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> login(String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("login", login);
                json.put("password", password);
                String response = sendPostRequest("/auth/login", json.toString(), null);
                System.out.println("[NetworkManager] Login response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    authToken = result.optString("token");
                    saveAuthToken(authToken);
                    isConnected = true;
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> checkToken() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return false;
                String response = sendPostRequest("/auth/check", "{}", authToken);
                JSONObject result = new JSONObject(response);
                return result.optBoolean("success", false);
            } catch (Exception e) {
                return false;
            }
        });
    }

    // ---- Character ----
public CompletableFuture<Map<String, Object>> loadCharacterData() {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            String response = sendGetRequest("/character", authToken);
            System.out.println("[NetworkManager] Character data response: " + response);
            JSONObject result = new JSONObject(response);
            return parseCharacterResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

    public CompletableFuture<Boolean> saveCharacter(Map<String, Object> characterData) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return false;
            JSONObject json = new JSONObject();
            if (characterData.containsKey("health")) json.put("health", characterData.get("health"));
            if (characterData.containsKey("mana")) json.put("mana", characterData.get("mana"));
            if (characterData.containsKey("maxHealth")) json.put("max_health", characterData.get("maxHealth"));
            if (characterData.containsKey("maxMana")) json.put("max_mana", characterData.get("maxMana"));
            if (characterData.containsKey("gold")) json.put("gold", characterData.get("gold"));
            if (characterData.containsKey("level")) json.put("level", characterData.get("level"));
            if (characterData.containsKey("experience")) json.put("experience", characterData.get("experience"));
            if (characterData.containsKey("currentDungeon")) json.put("current_dungeon", characterData.get("currentDungeon"));
            if (characterData.containsKey("difficulty")) json.put("difficulty", characterData.get("difficulty"));
            if (characterData.containsKey("lastX")) json.put("last_dungeon_position_x", characterData.get("lastX"));
            if (characterData.containsKey("lastY")) json.put("last_dungeon_position_y", characterData.get("lastY"));
            if (characterData.containsKey("lastZ")) json.put("last_dungeon_position_z", characterData.get("lastZ"));
            
            // ===== ИСПРАВЛЕНИЕ: ключи с подчеркиванием =====
            if (characterData.containsKey("healthPotions")) {
                json.put("health_potions", characterData.get("healthPotions"));
            }
            if (characterData.containsKey("manaPotions")) {
                json.put("mana_potions", characterData.get("manaPotions"));
            }

            String response = sendPostRequest("/character/save", json.toString(), authToken);
            JSONObject result = new JSONObject(response);
            return result.optBoolean("success", false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    });
}

    // ---- Inventory ----
public CompletableFuture<Map<String, Object>> pickupItem(Map<String, Object> itemData) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            JSONObject json = new JSONObject();
            json.put("itemData", new JSONObject(itemData));
            String response = sendPostRequest("/inventory/pickup", json.toString(), authToken);
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                return parseCharacterResponse(result);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

    public CompletableFuture<Map<String, Object>> equipItem(int slotIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("slot", slotIndex);
                String response = sendPostRequest("/inventory/equip", json.toString(), authToken);
                System.out.println("[NetworkManager] Equip response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

private String getSlotName(int slotIndex) {
        switch (slotIndex) {
            case 0: return "helmet";
            case 1: return "chest";
            case 2: return "weapon";
            case 3: return "shield";
            case 4: return "legs";
            case 5: return "boots";
            case 6: return "gloves";
            default: return null;
        }
    }

    // ================================================================
    //   ИСПРАВЛЕННЫЙ МЕТОД UNEQUIP
    // ================================================================
 public CompletableFuture<Map<String, Object>> unequipItem(int slotIndex) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            String slotName = getSlotName(slotIndex);
            if (slotName == null) {
                System.err.println("[NetworkManager] Invalid slot index for unequip: " + slotIndex);
                return null;
            }

            JSONObject json = new JSONObject();
            // ВАЖНО: сервер ожидает "equipped_slot", а не "slot"
            json.put("equipped_slot", slotName); 

            String response = sendPostRequest("/inventory/unequip", json.toString(), authToken);
            System.out.println("[DEBUG] UNEQUIP RESPONSE: " + response);

            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                return parseCharacterResponse(result);
            } else {
                System.err.println("[NetworkManager] Server rejected unequip. Response: " + response);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

    public CompletableFuture<Map<String, Object>> dropItem(int slotIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("slot", slotIndex);
                String response = sendPostRequest("/inventory/drop", json.toString(), authToken);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

     // ================================================================
    //   МЕТОДЫ АУКЦИОНА
    // ================================================================

    // Получить список лотов с фильтрами и пагинацией
public CompletableFuture<AuctionLotResponse> getAuctionList(int page, String type, String rarity, int minLevel, int maxLevel) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            StringBuilder query = new StringBuilder("/auction/list?page=" + page);
            if (type != null && !type.isEmpty()) query.append("&type=").append(type);
            if (rarity != null && !rarity.isEmpty()) query.append("&rarity=").append(rarity);
            if (minLevel > 0) query.append("&minLevel=").append(minLevel);
            if (maxLevel < 100) query.append("&maxLevel=").append(maxLevel);

            String response = sendGetRequest(query.toString(), authToken);
            System.out.println("[NetworkManager] getAuctionList response: " + response); // 👈
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                return new AuctionLotResponse(result);
            } else {
                System.err.println("[NetworkManager] Server error: " + result.optString("message"));
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

public CompletableFuture<Map<String, Object>> createAuctionLot(List<Integer> slotIndices, int price) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Not authenticated");
                return error;
            }
            JSONObject json = new JSONObject();
            json.put("slotIndices", slotIndices);
            json.put("price", price);
            System.out.println("[NetworkManager] Sending createAuctionLot: " + json);
            String response = sendPostRequest("/auction/create", json.toString(), authToken);
            System.out.println("[NetworkManager] createAuctionLot response: " + response);
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                // успех, возвращаем данные персонажа
                return parseCharacterResponse(result);
            } else {
                // ошибка, возвращаем карту с сообщением
                Map<String, Object> error = new HashMap<>();
                error.put("error", result.optString("message", "Unknown error"));
                return error;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    });
}
 public CompletableFuture<Map<String, Object>> buyAuctionLot(int lotId) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) {
                System.err.println("[NetworkManager] buyAuctionLot: authToken is null");
                return null;
            }
            JSONObject json = new JSONObject();
            json.put("lotId", lotId);
            System.out.println("[NetworkManager] Sending buy request: " + json);
            String response = sendPostRequest("/auction/buy", json.toString(), authToken);
            System.out.println("[NetworkManager] buyAuctionLot response: " + response);
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                return parseCharacterResponse(result);
            } else {
                System.err.println("[NetworkManager] Server error: " + result.optString("message"));
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

    // Получить мои лоты (для вкладки продаж)
    public CompletableFuture<List<AuctionLot>> getMyLots() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendGetRequest("/auction/my", authToken);
                JSONObject result = new JSONObject(response);
                JSONArray items = result.optJSONArray("items");
                List<AuctionLot> myLots = new ArrayList<>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        myLots.add(AuctionLot.fromMap(items.getJSONObject(i)));
                    }
                }
                return myLots;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    // ---- Helpers ----
    private String sendPostRequest(String endpoint, String jsonBody, String token) throws Exception {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8
        ));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    private String sendGetRequest(String endpoint, String token) throws Exception {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        int responseCode = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8
        ));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

private Map<String, Object> parseCharacterResponse(JSONObject response) {
    JSONObject character = response.optJSONObject("character");
    if (character == null) character = response;
    if (character == null) return null;

    Map<String, Object> data = new HashMap<>();

    // ===== Базовые параметры =====
    data.put("id", character.optString("id"));
    data.put("name", character.optString("name"));
    data.put("level", character.optInt("level", 1));
    data.put("experience", character.optInt("experience", 0));
    data.put("health", character.optInt("health", 100));
    data.put("maxHealth", character.optInt("maxHealth", 100));
    data.put("mana", character.optInt("mana", 50));
    data.put("maxMana", character.optInt("maxMana", 50));
    data.put("gold", character.optInt("gold", 100));         // ← золото
data.put("killCounter", character.optInt("killCounter", 0));

    // ===== Зелья =====
    data.put("healthPotions", character.optInt("healthPotions", 0));
    data.put("manaPotions", character.optInt("manaPotions", 0));

    // ===== Данж и сложность =====
    data.put("currentDungeon", character.optString("currentDungeon"));
    data.put("difficulty", character.optInt("difficulty", 1));

    // ===== Координаты =====
    JSONObject pos = character.optJSONObject("lastDungeonPosition");
    if (pos != null) {
        data.put("lastX", pos.optDouble("x", 0));
        data.put("lastY", pos.optDouble("y", 0));
        data.put("lastZ", pos.optDouble("z", 0));
    }

    // ===== Инвентарь =====
    JSONArray inv = character.optJSONArray("inventory");
    System.out.println("[NetworkManager] Inventory array from server: " + inv);
    if (inv != null) {
        List<Map<String, Object>> invList = new ArrayList<>();
        for (int i = 0; i < inv.length(); i++) {
            JSONObject invItem = inv.getJSONObject(i);
            Map<String, Object> map = new HashMap<>();
            map.put("slot", invItem.optInt("slot"));
            map.put("equipped", invItem.optBoolean("equipped"));
            
            String eqSlot = invItem.optString("equipped_slot");
            if (eqSlot.isEmpty()) {
                eqSlot = invItem.optString("equippedSlot");
            }
            map.put("equippedSlot", eqSlot);

            JSONObject itemObj = invItem.optJSONObject("item");
            if (itemObj != null) {
                map.put("item", itemObj.toMap());
            }
            invList.add(map);
        }
        data.put("inventory", invList);
    }
    return data;
}
    private void saveAuthToken(String token) {
        try {
            Path path = getTokenPath();
            Files.write(path, token.getBytes(StandardCharsets.UTF_8));
            System.out.println("[NetworkManager] Token saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAuthToken() {
        try {
            Path path = getTokenPath();
            if (Files.exists(path)) {
                authToken = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                isConnected = true;
                System.out.println("[NetworkManager] Token loaded.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path getTokenPath() {
        return Paths.get(System.getProperty("user.home"), TOKEN_FILE_NAME);
    }

    public String getAuthToken() { return authToken; }
    public boolean isConnected() { return isConnected; }
    public void cleanup() { isConnected = false; }
    
  public CompletableFuture<Map<String, Object>> loadTalents() {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            String response = sendGetRequest("/talents", authToken);
            System.out.println("[NetworkManager] loadTalents raw response: " + response);
            if (response == null || response.isEmpty() || !response.trim().startsWith("{")) {
                System.err.println("[NetworkManager] Invalid JSON response from /talents: " + response);
                return null;
            }
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                Map<String, Object> data = new HashMap<>();
                JSONObject talents = result.optJSONObject("talents");
                Map<String, Integer> talentMap = new HashMap<>();
                if (talents != null) {
                    for (String key : talents.keySet()) {
                        talentMap.put(key, talents.optInt(key, 0));
                    }
                }
                data.put("talents", talentMap);
                data.put("availablePoints", result.optInt("availablePoints", 0));
                return data;
            } else {
                System.err.println("[NetworkManager] loadTalents failed: " + result.optString("message"));
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

public CompletableFuture<Map<String, Object>> learnTalent(String talentId) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            JSONObject json = new JSONObject();
            json.put("talentId", talentId);
            String response = sendPostRequest("/talents/learn", json.toString(), authToken);
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                JSONObject talents = result.optJSONObject("talents");
                Map<String, Integer> talentMap = new HashMap<>();
                if (talents != null) {
                    for (String key : talents.keySet()) {
                        talentMap.put(key, talents.optInt(key, 0));
                    }
                }
                Map<String, Object> data = new HashMap<>();
                data.put("talents", talentMap);
                data.put("availablePoints", result.optInt("availablePoints", 0));
                return data;
            } else {
                String error = result.optString("message", "Unknown error");
                Map<String, Object> data = new HashMap<>();
                data.put("error", error);
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

public CompletableFuture<Map<String, Object>> resetTalents() {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            String response = sendPostRequest("/talents/reset", "{}", authToken);
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                JSONObject talents = result.optJSONObject("talents");
                Map<String, Integer> talentMap = new HashMap<>();
                if (talents != null) {
                    for (String key : talents.keySet()) {
                        talentMap.put(key, talents.optInt(key, 0));
                    }
                }
                Map<String, Object> data = new HashMap<>();
                data.put("talents", talentMap);
                data.put("availablePoints", result.optInt("availablePoints", 0));
                return data;
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("error", result.optString("message", "Unknown error"));
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}

public CompletableFuture<Map<String, Object>> levelUp() {
    
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) return null;
            String response = sendPostRequest("/character/levelup", "{}", authToken);
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                return parseCharacterResponse(result);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });
}
}