package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер звуков и музыки для игры "Экзорцист".
 * Все методы статические. Загружает звуки при инициализации,
 * воспроизводит одноразовые эффекты и фоновую музыку.
 * <p>
 * Важно:
 * - Для коротких звуков (SFX) используется буферизированная загрузка (по умолчанию).
 * - Для музыки тоже используем буферизацию, чтобы работало зацикливание.
 * - Все звуки должны быть в формате .ogg (рекомендуется).
 */
public class SoundManager {

    private static SimpleApplication app;
    private static Node rootNode;

    // Хранилище загруженных звуков
    private static Map<String, AudioNode> soundEffects = new HashMap<>();

    // Текущая музыка
    private static AudioNode currentMusic = null;
    private static String currentMusicKey = null;

    // Громкость
    private static float musicVolume = 0.2f;
    private static float sfxVolume = 0.3f;
public static AudioNode getSoundNode(String key) {
    return soundEffects.get(key);
}


    // ===================== КОНСТАНТЫ =====================
    // UI
    public static final String SOUND_CLICK = "ui_button_click";
    public static final String SOUND_LEVEL_UP = "ui_level_up";
    public static final String SOUND_PICKUP = "ui_pickup_item";
    public static final String SOUND_UNEQUIP = "ui_item_unequip";
    public static final String SOUND_WINDOW_TALENTS = "ui_window_open_talents_inventory";
    public static final String SOUND_WINDOW_TRADER = "ui_window_open_trader_auction";
    public static final String SOUND_WINDOW_CLOSE = "ui_window_close";
private AudioNode footstepNode;
private float footstepTimer = 0f;
private static final float FOOTSTEP_INTERVAL = 0.45f;
    // Skills
    public static final String SOUND_HEAL = "skill_heal";
    public static final String SOUND_SHIELD_BASH = "skill_shield_bash";
    public static final String SOUND_WHIRLWIND = "skill_whirlwind";
    public static final String SOUND_KICK = "skill_kick";

    // Combat
    public static final String SOUND_ATTACK_PLAYER = "combat_attack_player";
    public static final String SOUND_PLAYER_DIE = "combat_player_die";
    public static final String SOUND_MONSTER_DIE = "combat_monster_die";
    public static final String SOUND_FOOTSTEP = "ambient_footstep_player";

    // Music
    public static final String MUSIC_CITY = "music_city_loop";
    public static final String MUSIC_DUNGEON = "music_dungeon_loop";
    public static final String MUSIC_BOSS = "music_boss_battle";

    // ===================== ИНИЦИАЛИЗАЦИЯ =====================

    /**
     * Вызывается один раз в simpleInitApp().
     */
    public static void initialize(SimpleApplication application) {
        app = application;
        rootNode = app.getRootNode();
        System.out.println("[SoundManager] Initializing...");
        loadAllSounds();
        System.out.println("[SoundManager] Ready. Loaded " + soundEffects.size() + " sounds.");
    }

    /**
     * Загрузка всех звуковых файлов из папки resources/sounds/
     */
    private static void loadAllSounds() {
        // UI
        loadSound(SOUND_CLICK, "sounds/ui/ui_button_click.ogg");
        loadSound(SOUND_LEVEL_UP, "sounds/ui/ui_level_up.ogg");
        loadSound(SOUND_PICKUP, "sounds/ui/ui_pickup_item.ogg");
        loadSound(SOUND_UNEQUIP, "sounds/ui/ui_item_unequip.ogg");
        loadSound(SOUND_WINDOW_TALENTS, "sounds/ui/ui_window_open_talents_inventory.ogg");
        loadSound(SOUND_WINDOW_TRADER, "sounds/ui/ui_window_open_trader_auction.ogg");
        loadSound(SOUND_WINDOW_CLOSE, "sounds/ui/ui_window_close.ogg");

        // Skills
        loadSound(SOUND_HEAL, "sounds/skills/skill_heal.ogg");
        loadSound(SOUND_SHIELD_BASH, "sounds/skills/skill_shield_bash.ogg");
        loadSound(SOUND_WHIRLWIND, "sounds/skills/skill_whirlwind.ogg");
        loadSound(SOUND_KICK, "sounds/skills/skill_kick.ogg");

        // Combat
        loadSound(SOUND_ATTACK_PLAYER, "sounds/combat/combat_attack_player.ogg");
        loadSound(SOUND_PLAYER_DIE, "sounds/combat/combat_player_die.ogg");
        loadSound(SOUND_MONSTER_DIE, "sounds/combat/combat_monster_die.ogg");

        // Ambient
        loadSound(SOUND_FOOTSTEP, "sounds/ambient/ambient_footstep_player.ogg");

        // Music
        loadMusic(MUSIC_CITY, "sounds/music/music_city_loop.ogg");
        loadMusic(MUSIC_DUNGEON, "sounds/music/music_dungeon_loop.ogg");
        loadMusic(MUSIC_BOSS, "sounds/music/music_boss_battle.ogg");
    }

    // ===================== ЗАГРУЗКА ЗВУКОВ =====================

    /**
     * Загрузка короткого звукового эффекта.
     * Используем конструктор AudioNode(AssetManager, String) — буферизация по умолчанию.
     */
    private static void loadSound(String key, String path) {
        try {
            AudioNode node = new AudioNode(app.getAssetManager(), path);
            node.setPositional(false);
            node.setLooping(false);
            node.setVolume(sfxVolume);
            // Важно: узел должен быть прикреплён к сцене, чтобы звук воспроизводился
            rootNode.attachChild(node);
            soundEffects.put(key, node);
            System.out.println("[SoundManager] Loaded sound: " + key);
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to load sound: " + key + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загрузка музыки.
     * Используем тот же конструктор — буферизация, чтобы работало зацикливание.
     */
    private static void loadMusic(String key, String path) {
        try {
            AudioNode node = new AudioNode(app.getAssetManager(), path);
            node.setPositional(false);
            node.setLooping(true);   // Зацикливание работает только для буферизированных звуков
            node.setVolume(musicVolume);
            rootNode.attachChild(node);
            soundEffects.put(key, node);
            System.out.println("[SoundManager] Loaded music: " + key);
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to load music: " + key + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== ВОСПРОИЗВЕДЕНИЕ =====================

    /**
     * Проигрывает звуковой эффект один раз.
     * Использует playInstance() — позволяет накладывать несколько копий.
     */
    public static void playSound(String key) {
        AudioNode node = soundEffects.get(key);
        if (node == null) {
            System.err.println("[SoundManager] Sound not found: " + key);
            return;
        }
        node.playInstance();
    }

    /**
     * Проигрывает звук с задержкой (для синхронизации с анимацией).
     */
    public static void playSoundDelayed(String key, float delaySec) {
        if (delaySec <= 0) {
            playSound(key);
            return;
        }
        new Thread(() -> {
            try {
                Thread.sleep((long)(delaySec * 1000));
                playSound(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // ===================== УПРАВЛЕНИЕ МУЗЫКОЙ =====================

    /**
     * Включает фоновую музыку.
     * Предыдущая останавливается.
     */
    public static void playMusic(String key) {
        // Если уже играет эта же музыка — ничего не делаем
        if (currentMusic != null && currentMusicKey != null && currentMusicKey.equals(key)) {
            return;
        }
        stopMusic();

        AudioNode node = soundEffects.get(key);
        if (node == null) {
            System.err.println("[SoundManager] Music not found: " + key);
            return;
        }
        currentMusic = node;
        currentMusic.setVolume(musicVolume);
        currentMusic.play();   // управляемый источник (не playInstance)
        currentMusicKey = key;
        System.out.println("[SoundManager] Playing music: " + key);
    }

    /**
     * Останавливает текущую музыку.
     */
    public static void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicKey = null;
        }
    }

    // ===================== УПРАВЛЕНИЕ ГРОМКОСТЬЮ =====================

    public static void setMusicVolume(float volume) {
        musicVolume = Math.max(0, Math.min(1, volume));
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
        }
    }

    public static void setSfxVolume(float volume) {
        sfxVolume = Math.max(0, Math.min(1, volume));
        for (Map.Entry<String, AudioNode> entry : soundEffects.entrySet()) {
            entry.getValue().setVolume(sfxVolume);
        }
    }

    public static float getMusicVolume() { return musicVolume; }
    public static float getSfxVolume() { return sfxVolume; }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    public static boolean isSoundLoaded(String key) {
        return soundEffects.containsKey(key);
    }

    /**
     * Освобождение ресурсов при завершении игры.
     */
    public static void cleanup() {
        stopMusic();
        for (AudioNode node : soundEffects.values()) {
            node.stop();
        }
        soundEffects.clear();
        System.out.println("[SoundManager] Cleanup complete.");
    }
}