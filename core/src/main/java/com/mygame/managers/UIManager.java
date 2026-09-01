package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.material.Material;
import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.focus.FocusNavigationFunctions;
import com.mygame.Main;
import com.mygame.items.ItemGenerator;
import com.mygame.managers.GameManager.GameState;
import com.simsilica.lemur.component.TextEntryComponent;
import com.simsilica.lemur.event.KeyAction;
import com.simsilica.lemur.event.KeyActionListener;
import com.jme3.input.event.JoyButtonEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UIManager {

    private WorldManager worldManager;

    public void setWorldManager(WorldManager wm) {
        this.worldManager = wm;
    }

    private Node backgroundNode;
    private Geometry backgroundGeom;
    private String backgroundTexturePath = "Interface/login_bg.png";
    private TalentManager talentManager;

    public TalentManager getTalentManager() {
        return talentManager;
    }

    public TalentWindow getTalentWindow() {
        if (talentWindow == null && talentManager != null) {
            createWindows(true);
        }
        return talentWindow;
    }

    public TraderWindow getTraderWindow() {
        if (traderWindow == null && playerManager != null && inventoryManager != null) {
            traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
        }
        return traderWindow;
    }

    public AuctionWindow getAuctionWindow() {
        if (auctionWindow == null && playerManager != null && inventoryManager != null) {
            auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
        }
        return auctionWindow;
    }

    private AuctionWindow auctionWindow;
    private TraderWindow traderWindow;

    public void openAuction() {
        toggleAuction();
    }

    public void openTrader() {
        toggleTrader();
    }

    public void toggleInventory() {
        if (inventoryManager == null) return;

        if (inventoryManager.isVisible()) {
            inventoryManager.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("inventory");
            inventoryManager.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
        }
    }

    public void toggleTalents() {
        if (talentWindow == null) return;

        if (talentWindow.isVisible()) {
            talentWindow.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("talents");
            talentWindow.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
        }
    }

    public void toggleAuction() {
        if (auctionWindow == null) {
            auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
        }

        if (auctionWindow.isVisible()) {
            auctionWindow.hide();
            return;
        }

        closeAllWindowsExcept("auction");
        SoundManager.playSound(SoundManager.SOUND_WINDOW_TRADER);
        auctionWindow.show();
    }

    public void toggleTrader() {
        if (traderWindow == null) {
            traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
        }

        if (traderWindow.isVisible()) {
            traderWindow.hide();
            return;
        }

        closeAllWindowsExcept("trader");
        SoundManager.playSound(SoundManager.SOUND_WINDOW_TRADER);
        traderWindow.show();
    }

    private void closeAllWindowsExcept(String keepOpen) {

        if (!"inventory".equals(keepOpen)
                && inventoryManager != null
                && inventoryManager.isVisible()) {
            inventoryManager.hide();
        }

        if (!"talents".equals(keepOpen)
                && talentWindow != null
                && talentWindow.isVisible()) {
            talentWindow.hide();
        }

        if (!"auction".equals(keepOpen)
                && auctionWindow != null
                && auctionWindow.isVisible()) {
            auctionWindow.hide();
        }

        if (!"trader".equals(keepOpen)
                && traderWindow != null
                && traderWindow.isVisible()) {
            traderWindow.hide();
        }



        if (!"settings".equals(keepOpen)
                && settingsWindow != null
                && settingsWindow.isVisible()) {
            settingsWindow.hide();
        }
        if (!"blacksmith".equals(keepOpen)
             && blacksmithWindow != null
             && blacksmithWindow.isVisible()) {
             blacksmithWindow.hide();
        }
        if (!"characterStats".equals(keepOpen)
                && characterStatsWindow != null
                && characterStatsWindow.isVisible()) {
            characterStatsWindow.hide();
        }
    }

    private SimpleApplication app;
    private Node guiNode;
    private NetworkManager networkManager;

    private Container loginWindow;
    private Container registerWindow;
    private Node hudNode;

    private boolean loginVisible = false;
    private boolean registerVisible = false;
    private boolean hudVisible = false;

    // HUD
    private Geometry hudBackground;
    private List<Button> hudButtons = new ArrayList<>();
    private Button inventoryButton;
    private Button talentButton;
    private Label hpCountLabel;
    private Label mpCountLabel;
    private Button healthPotionBtn;
    private Button manaPotionBtn;
    private Button skill1Btn, skill2Btn, skill3Btn, skill4Btn;
    private Map<Button, Geometry> iconGeoms = new HashMap<>();

    // Статистика игрока
    private Container playerStatsContainer;
    private Label playerNameLabel;
    private ProgressBar hpBar;
    private ProgressBar manaBar;
    private Label hpTextLabel;
    private Label manaTextLabel;

    private float lastHealth = -1;
    private float lastMaxHealth = -1;
    private float lastMana = -1;
    private float lastMaxMana = -1;
    private String lastName = "";

    private float hudHeight = 90;
    private float buttonSize = 70;
    private float buttonSpacing = 8;
    private float bottomOffset = 20;

    // Поля ввода
    private TextField loginField;
    private TextField passwordField;

    private TextField emailField;
    private TextField regLoginField;
    private TextField regPasswordField;

    private TextField[] loginFields;
    private TextField[] registerFields;

    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private TalentWindow talentWindow;

    private float scale = 1f;

    private static final String KEY_SKILL1 = "skill1";
    private static final String KEY_SKILL2 = "skill2";
    private static final String KEY_SKILL3 = "skill3";
    private static final String KEY_SKILL4 = "skill4";

    private static final String KEY_HEALTH_POTION = "healthPotion";
    private static final String KEY_MANA_POTION = "manaPotion";

    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_TALENTS = "talents";

    /*
     * ============================================================
     * Новые поля для RawInputListener (латинский ввод)
     * ============================================================
     */
    private RawInputListener authRawInputListener;
    private boolean authShiftDown = false;
    private boolean authCtrlDown = false;
    private boolean authAltDown = false;

    private SettingsWindow settingsWindow;

    public UIManager(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();

        Main main = (Main) app;

        if (main != null) {
            this.networkManager = main.getNetworkManager();
        }

        updateScale();
    }

    private void updateScale() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float baseWidth = 800f;
        float baseHeight = 600f;

        float scaleX = screenWidth / baseWidth;
        float scaleY = screenHeight / baseHeight;

        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));
    }

    public void attachNode(Node node) {
        if (node != null && !guiNode.hasChild(node)) {
            guiNode.attachChild(node);
            System.out.println("[UIManager] ATTACHED: " + node.getName());
        }
    }

    public void detachNode(Node node) {
        if (node != null && guiNode.hasChild(node)) {
            guiNode.detachChild(node);
            System.out.println("[UIManager] DETACHED: " + node.getName());
        }
    }

    public void onInventoryOpened(Node node) {
        attachNode(node);
    }

    public void onInventoryClosed(Node node) {
        detachNode(node);
    }

    public void onTalentOpened(Node node) {
        attachNode(node);
    }

    public void onTalentClosed(Node node) {
        detachNode(node);
    }

    public void onTraderOpened(Node node) {
        attachNode(node);
    }

    public void onTraderClosed(Node node) {
        detachNode(node);
    }

    public Geometry createBackgroundGeometry(float width, float height) {

        Texture leatherTexture = null;

        try {
            leatherTexture = app.getAssetManager()
                    .loadTexture("Interface/leather_border.png");
        } catch (Exception e) {
            System.err.println(
                    "[UIManager] Текстура не загружена, используем цвет."
            );
        }

        Quad quad = new Quad(width, height);

        Geometry bgGeom = new Geometry("WindowBg", quad);

        Material mat = new Material(
                app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"
        );

        if (leatherTexture != null) {
            mat.setTexture("ColorMap", leatherTexture);
        } else {
            mat.setColor(
                    "Color",
                    new ColorRGBA(0.4f, 0.2f, 0.05f, 1f)
            );
        }

        bgGeom.setMaterial(mat);
        bgGeom.setLocalTranslation(0, 0, 0f);

        return bgGeom;
    }

private void applyStoredLanguage() {
    String lang = SettingsManager.getInstance().getLanguage();
    LocalizationManager.getInstance().init(app.getAssetManager());
    LocalizationManager.getInstance().loadLanguage(lang);
    // Всегда загружаем ru.fnt, так как он содержит и русские, и английские символы
    try {
        BitmapFont customFont = app.getAssetManager().loadFont("Interface/Fonts/ru.fnt");
        GuiGlobals.getInstance().getStyles().setDefault(customFont);
    } catch (Exception e) {
        System.err.println("Failed to load ru.fnt, using default Lemur font.");
    }
}
    private void setupSettingsKey() {

        app.getInputManager().addMapping(
                "TOGGLE_SETTINGS",
                new KeyTrigger(KeyInput.KEY_GRAVE)
        );

        ActionListener listener = (name, isPressed, tpf) -> {

            if (!isPressed) return;

            if (name.equals("TOGGLE_SETTINGS")) {
                toggleSettings();
            }
        };

        app.getInputManager()
                .addListener(listener, "TOGGLE_SETTINGS");
    }

    public void toggleSettings() {

    if (settingsWindow == null) return;

    /*
     * На экранах логина и регистрации
     * настройки открывать нельзя.
     */
    if (loginVisible || registerVisible) {

        if (settingsWindow.isVisible()) {
            settingsWindow.hide();
        }

        return;
    }

    if (settingsWindow.isVisible()) {

        settingsWindow.hide();

        SoundManager.playSound(
                SoundManager.SOUND_WINDOW_CLOSE
        );

    } else {

        closeAllWindowsExcept("settings");

        settingsWindow.show();

        SoundManager.playSound(
                SoundManager.SOUND_WINDOW_TALENTS
        );
    }
}

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    public void initialize() {

        applyStoredLanguage();

        settingsWindow = new SettingsWindow(app, this);

        setupSettingsKey();

        /*
         * ВАЖНО: Устанавливаем наш обработчик после создания GUI.
         */
        setupLatinAuthInput();

        GuiGlobals.getInstance()
                .getFocusNavigationState()
                .setEnabled(false);

        GuiGlobals.getInstance()
                .getInputMapper()
                .deactivateGroup(
                        FocusNavigationFunctions.UI_NAV
                );

        createLoginScreen();
        createRegisterScreen();
        createHUD();
        createPlayerStatsUI();

        hudNode = new Node("HUDNode");
        hudNode.setName("HUDNode");

        hudNode.attachChild(hudBackground);

        for (Button btn : hudButtons) {
            hudNode.attachChild(btn);
        }

        hudNode.attachChild(playerStatsContainer);

        loginWindow.setCullHint(Node.CullHint.Always);
        registerWindow.setCullHint(Node.CullHint.Always);

        hideHUD();

        setupKeyboardShortcuts();
        setupAuthKeys();

        createBackground();
        hideBackground();

        if (backgroundNode != null
                && !guiNode.hasChild(backgroundNode)) {

            guiNode.attachChild(backgroundNode);
        }
        createLoginBottomButtons();
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ АВТОРИЗАЦИИ
    // ============================================================

    private boolean isAuthFieldFocused() {

        Spatial currentFocus =
                GuiGlobals.getInstance()
                        .getFocusManagerState()
                        .getFocus();

        return currentFocus == loginField
                || currentFocus == passwordField
                || currentFocus == emailField
                || currentFocus == regLoginField
                || currentFocus == regPasswordField;
    }

    private TextField getFocusedAuthField() {

        Spatial currentFocus =
                GuiGlobals.getInstance()
                        .getFocusManagerState()
                        .getFocus();

        if (currentFocus == loginField) {
            return loginField;
        }

        if (currentFocus == passwordField) {
            return passwordField;
        }

        if (currentFocus == emailField) {
            return emailField;
        }

        if (currentFocus == regLoginField) {
            return regLoginField;
        }

        if (currentFocus == regPasswordField) {
            return regPasswordField;
        }

        return null;
    }

    // Преобразование физической клавиши в латинский символ (без использования getKeyChar())
    private char keyCodeToLatinCharacter(
            int keyCode,
            boolean shift) {

        // Буквы
        switch (keyCode) {

            case KeyInput.KEY_Q:
                return shift ? 'Q' : 'q';
            case KeyInput.KEY_W:
                return shift ? 'W' : 'w';
            case KeyInput.KEY_E:
                return shift ? 'E' : 'e';
            case KeyInput.KEY_R:
                return shift ? 'R' : 'r';
            case KeyInput.KEY_T:
                return shift ? 'T' : 't';
            case KeyInput.KEY_Y:
                return shift ? 'Y' : 'y';
            case KeyInput.KEY_U:
                return shift ? 'U' : 'u';
            case KeyInput.KEY_I:
                return shift ? 'I' : 'i';
            case KeyInput.KEY_O:
                return shift ? 'O' : 'o';
            case KeyInput.KEY_P:
                return shift ? 'P' : 'p';
            case KeyInput.KEY_A:
                return shift ? 'A' : 'a';
            case KeyInput.KEY_S:
                return shift ? 'S' : 's';
            case KeyInput.KEY_D:
                return shift ? 'D' : 'd';
            case KeyInput.KEY_F:
                return shift ? 'F' : 'f';
            case KeyInput.KEY_G:
                return shift ? 'G' : 'g';
            case KeyInput.KEY_H:
                return shift ? 'H' : 'h';
            case KeyInput.KEY_J:
                return shift ? 'J' : 'j';
            case KeyInput.KEY_K:
                return shift ? 'K' : 'k';
            case KeyInput.KEY_L:
                return shift ? 'L' : 'l';
            case KeyInput.KEY_Z:
                return shift ? 'Z' : 'z';
            case KeyInput.KEY_X:
                return shift ? 'X' : 'x';
            case KeyInput.KEY_C:
                return shift ? 'C' : 'c';
            case KeyInput.KEY_V:
                return shift ? 'V' : 'v';
            case KeyInput.KEY_B:
                return shift ? 'B' : 'b';
            case KeyInput.KEY_N:
                return shift ? 'N' : 'n';
            case KeyInput.KEY_M:
                return shift ? 'M' : 'm';

            // Цифры
            case KeyInput.KEY_0:
                return shift ? ')' : '0';
            case KeyInput.KEY_1:
                return shift ? '!' : '1';
            case KeyInput.KEY_2:
                return shift ? '@' : '2';
            case KeyInput.KEY_3:
                return shift ? '#' : '3';
            case KeyInput.KEY_4:
                return shift ? '$' : '4';
            case KeyInput.KEY_5:
                return shift ? '%' : '5';
            case KeyInput.KEY_6:
                return shift ? '^' : '6';
            case KeyInput.KEY_7:
                return shift ? '&' : '7';
            case KeyInput.KEY_8:
                return shift ? '*' : '8';
            case KeyInput.KEY_9:
                return shift ? '(' : '9';

            // Символы
            case KeyInput.KEY_MINUS:
                return shift ? '_' : '-';
            case KeyInput.KEY_EQUALS:
                return shift ? '+' : '=';
            case KeyInput.KEY_LBRACKET:
                return shift ? '{' : '[';
            case KeyInput.KEY_RBRACKET:
                return shift ? '}' : ']';
            case KeyInput.KEY_SEMICOLON:
                return shift ? ':' : ';';
            case KeyInput.KEY_APOSTROPHE:
                return shift ? '"' : '\'';
            case KeyInput.KEY_COMMA:
                return shift ? '<' : ',';
            case KeyInput.KEY_PERIOD:
                return shift ? '>' : '.';
            case KeyInput.KEY_SLASH:
                return shift ? '?' : '/';

            default:
                return 0;
        }
    }

    private boolean isAllowedAuthCharacter(char c) {

        if ((c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')) {
            return true;
        }

        if (c >= '0' && c <= '9') {
            return true;
        }

        return c == '@'
                || c == '.'
                || c == '_'
                || c == '-'
                || c == '+';
    }

    private void insertAuthCharacter(char character) {

        TextField field = getFocusedAuthField();

        if (field == null) {
            return;
        }

        if (!isAllowedAuthCharacter(character)) {
            return;
        }

        String current = field.getText();

        if (current == null) {
            current = "";
        }

        field.setText(current + character);
    }

    // ============================================================
    // ИСПРАВЛЕННЫЙ RawInputListener
    // ============================================================

    private void setupLatinAuthInput() {
    if (authRawInputListener != null) {
        return;
    }

    authRawInputListener = new RawInputListener() {
        // НЕ объявляем локальные переменные — используем поля класса

        @Override
        public void beginInput() {}

        @Override
        public void endInput() {}

        @Override
        public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent evt) {}

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {}

        @Override
        public void onMouseMotionEvent(com.jme3.input.event.MouseMotionEvent evt) {}

        @Override
        public void onMouseButtonEvent(com.jme3.input.event.MouseButtonEvent evt) {}

        @Override
        public void onTouchEvent(com.jme3.input.event.TouchEvent evt) {}

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            int keyCode = evt.getKeyCode();

            // Обновляем флаги модификаторов
            if (keyCode == KeyInput.KEY_LSHIFT || keyCode == KeyInput.KEY_RSHIFT) {
                authShiftDown = evt.isPressed();
                return;
            }
            if (keyCode == KeyInput.KEY_LCONTROL || keyCode == KeyInput.KEY_RCONTROL) {
                authCtrlDown = evt.isPressed();
                return;
            }


            // Если фокус не в поле авторизации — пропускаем
            if (!isAuthFieldFocused()) {
                return;
            }

            // Работаем только при нажатии (не отпускании)
            if (!evt.isPressed()) {
                return;
            }

            TextField field = getFocusedAuthField();
            if (field == null) return;

            // ============================================================
            // ОБРАБОТКА СПЕЦИАЛЬНЫХ КЛАВИШ (всегда пропускаем)
            // ============================================================
            if (keyCode == KeyInput.KEY_BACK
                    || keyCode == KeyInput.KEY_DELETE
                    || keyCode == KeyInput.KEY_LEFT
                    || keyCode == KeyInput.KEY_RIGHT
                    || keyCode == KeyInput.KEY_UP
                    || keyCode == KeyInput.KEY_DOWN
                    || keyCode == KeyInput.KEY_HOME
                    || keyCode == KeyInput.KEY_END
                    || keyCode == KeyInput.KEY_RETURN
                    || keyCode == KeyInput.KEY_TAB
                    || keyCode == KeyInput.KEY_ESCAPE) {
                return; // ничего не блокируем
            }

            // Если зажат Ctrl или Alt — пропускаем (управляющие комбинации)
            if (authCtrlDown || authAltDown) {
                return;
            }

            // ============================================================
            // ГЛАВНОЕ: проверяем символ, который выдаёт система (раскладка)
            // ============================================================
            char keyChar = evt.getKeyChar();

            // Если символ — это русская буква (кириллица), значит русская раскладка
            // Проверяем диапазон Unicode: А-я (1040-1103) плюс Ёё (1025, 1105)
            boolean isCyrillic = (keyChar >= 0x0410 && keyChar <= 0x044F)
                    || keyChar == 0x0401 || keyChar == 0x0451;

            if (isCyrillic) {
                // Русская раскладка — подменяем на латиницу
                char latinChar = keyCodeToLatinCharacter(keyCode, authShiftDown);
                if (latinChar == 0) {
                    return; // непечатная клавиша
                }
                if (!isAllowedAuthCharacter(latinChar)) {
                    evt.setConsumed();
                    return;
                }
                // Блокируем событие, чтобы Lemur не получил русскую букву
                evt.setConsumed();
                // Вставляем латинский символ
                insertAuthCharacter(latinChar);
            } else {
                // Английская раскладка или небуквенный символ — ничего не делаем,
                // событие дойдёт до Lemur и он добавит символ как обычно.
                // Но если это цифра или символ, которые мы разрешаем, то они уже латиница,
                // и мы не блокируем, чтобы они прошли в поле нормально.
            }
        }
    };

    app.getInputManager().addRawInputListener(authRawInputListener);
}

    // ============================================================
    // TAB
    // ============================================================

    private void setupAuthKeys() {

        System.out.println("[UI] Setting up auth keys...");

        app.getInputManager().addMapping(
                "AUTH_TAB",
                new KeyTrigger(KeyInput.KEY_TAB)
        );

        ActionListener authListener =
                new ActionListener() {

                    @Override
                    public void onAction(
                            String name,
                            boolean isPressed,
                            float tpf) {

                        if (!isPressed) {
                            return;
                        }

                        if (name.equals("AUTH_TAB")) {

                            if (loginVisible
                                    && loginFields != null
                                    && loginFields.length > 0) {

                                moveFocus(loginFields);

                            } else if (registerVisible
                                    && registerFields != null
                                    && registerFields.length > 0) {

                                moveFocus(registerFields);
                            }
                        }
                    }
                };

        app.getInputManager()
                .addListener(
                        authListener,
                        "AUTH_TAB"
                );
    }

    // ============================================================
    // ФОКУС
    // ============================================================

    private void moveFocus(TextField[] fields) {

        if (fields == null || fields.length == 0) {
            return;
        }

        Spatial currentFocus =
                GuiGlobals.getInstance()
                        .getFocusManagerState()
                        .getFocus();

        int currentIndex = -1;

        for (int i = 0; i < fields.length; i++) {

            if (fields[i] == currentFocus) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex =
                (currentIndex + 1) % fields.length;

        final TextField nextField =
                fields[nextIndex];

        app.enqueue(() -> {

            GuiGlobals.getInstance()
                    .requestFocus(nextField);

            if (GuiGlobals.getInstance()
                    .getFocusManagerState()
                    .getFocus() != nextField) {

                GuiGlobals.getInstance()
                        .getFocusManagerState()
                        .setFocus(nextField);
            }
        });
    }

    // ============================================================
    // ФОН
    // ============================================================

    private void createBackground() {

        if (backgroundNode != null) {
            return;
        }

        backgroundNode =
                new Node("LoginBackground");

        try {

            Texture tex =
                    app.getAssetManager()
                            .loadTexture(backgroundTexturePath);

            float w =
                    app.getCamera().getWidth();

            float h =
                    app.getCamera().getHeight();

            Quad quad =
                    new Quad(w, h);

            backgroundGeom =
                    new Geometry(
                            "LoginBg",
                            quad
                    );

            Material mat =
                    new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );

            mat.setTexture(
                    "ColorMap",
                    tex
            );

            backgroundGeom.setMaterial(mat);

            backgroundGeom.setLocalTranslation(
                    0,
                    0,
                    -10
            );

            backgroundNode.attachChild(
                    backgroundGeom
            );

            backgroundNode.setCullHint(
                    Node.CullHint.Always
            );

        } catch (Exception e) {

            System.err.println(
                    "[UIManager] Failed to load background, using fallback"
            );

            createFallbackBackground();
        }
    }

    private void createFallbackBackground() {

        float w =
                app.getCamera().getWidth();

        float h =
                app.getCamera().getHeight();

        Quad quad =
                new Quad(w, h);

        backgroundGeom =
                new Geometry(
                        "LoginBg",
                        quad
                );

        Material mat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        mat.setColor(
                "Color",
                new ColorRGBA(
                        0.15f,
                        0.15f,
                        0.25f,
                        1f
                )
        );

        backgroundGeom.setMaterial(mat);

        backgroundGeom.setLocalTranslation(
                0,
                0,
                -10
        );

        backgroundNode.attachChild(
                backgroundGeom
        );

        backgroundNode.setCullHint(
                Node.CullHint.Always
        );
    }

    private void updateBackgroundScale() {

        if (backgroundGeom == null) {
            return;
        }

        float w =
                app.getCamera().getWidth();

        float h =
                app.getCamera().getHeight();

        Quad q =
                (Quad) backgroundGeom.getMesh();

        q.updateGeometry(w, h);

        backgroundGeom.setLocalTranslation(
                0,
                0,
                -10
        );
    }

    private void showBackground() {

        if (backgroundNode != null) {

            backgroundNode.setCullHint(
                    Node.CullHint.Never
            );

            updateBackgroundScale();
        }
    }

    private void hideBackground() {

        if (backgroundNode != null) {

            backgroundNode.setCullHint(
                    Node.CullHint.Always
            );
        }
    }

    public void forceShowLogin() {
        showLoginScreen();
    }

    // ============================================================
    // ИСПРАВЛЕННЫЙ setupKeyboardShortcuts()
    // ============================================================

    private void setupKeyboardShortcuts() {
        // Добавляем маппинги всегда
        app.getInputManager().addMapping(
            KEY_CHARACTER_STATS,
            new KeyTrigger(KeyInput.KEY_C)
        );
        
        app.getInputManager().addMapping(
                KEY_SKILL1,
                new KeyTrigger(KeyInput.KEY_1)
        );

        app.getInputManager().addMapping(
                KEY_SKILL2,
                new KeyTrigger(KeyInput.KEY_2)
        );

        app.getInputManager().addMapping(
                KEY_SKILL3,
                new KeyTrigger(KeyInput.KEY_3)
        );

        app.getInputManager().addMapping(
                KEY_SKILL4,
                new KeyTrigger(KeyInput.KEY_4)
        );

        app.getInputManager().addMapping(
                KEY_HEALTH_POTION,
                new KeyTrigger(KeyInput.KEY_Q)
        );

        app.getInputManager().addMapping(
                KEY_MANA_POTION,
                new KeyTrigger(KeyInput.KEY_W)
        );

        app.getInputManager().addMapping(
                KEY_INVENTORY,
                new KeyTrigger(KeyInput.KEY_I)
        );

        app.getInputManager().addMapping(
                KEY_TALENTS,
                new KeyTrigger(KeyInput.KEY_T)
        );

        ActionListener listener =
                new ActionListener() {

                    @Override
                    public void onAction(
                            String name,
                            boolean isPressed,
                            float tpf) {

                        if (!isPressed) {
                            return;
                        }

                        // =============================================
                        // ВАЖНО: НЕ ОБРАБАТЫВАЕМ игровые клавиши,
                        // если видимы окна входа или регистрации
                        // =============================================
                        if (loginVisible || registerVisible) {
                            return;
                        }

                        if (!hudVisible) {
                            return;
                        }

                        switch (name) {
                            case KEY_CHARACTER_STATS:
                                toggleCharacterStats();
                                break;
                            case KEY_SKILL1:

                                if (playerManager != null) {
                                    playerManager.castSkill("Heal");
                                }

                                flashButton(skill1Btn);
                                break;

                            case KEY_SKILL2:

                                if (playerManager != null) {
                                    playerManager.castSkill("ShieldBash");
                                }

                                flashButton(skill2Btn);
                                break;

                            case KEY_SKILL3:

                                if (playerManager != null) {
                                    playerManager.castSkill("Whirlwind");
                                }

                                flashButton(skill3Btn);
                                break;

                            case KEY_SKILL4:

                                if (playerManager != null) {
                                    playerManager.castSkill("Kick");
                                }

                                flashButton(skill4Btn);
                                break;

                            case KEY_HEALTH_POTION:

                                if (playerManager != null) {

                                    playerManager.useHealthPotion();

                                    updatePotionCounts();
                                    updatePlayerStats();

                                    flashButton(healthPotionBtn);
                                }

                                break;

                            case KEY_MANA_POTION:

                                if (playerManager != null) {

                                    playerManager.useManaPotion();

                                    updatePotionCounts();
                                    updatePlayerStats();

                                    flashButton(manaPotionBtn);
                                }

                                break;

                            case KEY_INVENTORY:

                                toggleInventory();
                                //flashButton(inventoryButton);
                                break;

                            case KEY_TALENTS:

                                toggleTalents();
                                flashButton(talentButton);
                                break;
                        }
                    }
                };

        app.getInputManager().addListener(
                listener,
                KEY_SKILL1,
                KEY_SKILL2,
                KEY_SKILL3,
                KEY_SKILL4,
                KEY_HEALTH_POTION,
                KEY_MANA_POTION,
                KEY_INVENTORY,
                KEY_TALENTS,
                KEY_CHARACTER_STATS
        );
    }

    private void flashButton(Button btn) {

        if (btn == null) {
            return;
        }

        Geometry iconGeom =
                iconGeoms.get(btn);

        if (iconGeom == null) {
            return;
        }

        Material mat =
                iconGeom.getMaterial();

        if (mat == null) {
            return;
        }

        MatParam colorParam =
                mat.getParam("Color");

        ColorRGBA originalColor =
                colorParam != null
                        ? (ColorRGBA) colorParam.getValue()
                        : ColorRGBA.White;

        final ColorRGBA finalOriginalColor =
                originalColor.clone();

        mat.setColor(
                "Color",
                new ColorRGBA(
                        1f,
                        1f,
                        0f,
                        1f
                )
        );

        new Thread(() -> {

            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            app.enqueue(() ->
                    mat.setColor(
                            "Color",
                            finalOriginalColor
                    )
            );

        }).start();
    }

    // ============================================================
    // HUD
    // ============================================================

    private Button createIconOnlyButton(
            String iconPath,
            float size) {

        Button btn =
                new Button("");

        btn.setPreferredSize(
                new Vector3f(
                        size,
                        size,
                        0
                )
        );

        btn.setBackground(null);
        btn.setColor(ColorRGBA.White);

        try {

            Texture tex =
                    app.getAssetManager()
                            .loadTexture(iconPath);

            if (tex != null) {

                Geometry iconGeom =
                        new Geometry(
                                "Icon",
                                new Quad(size, size)
                        );

                Material iconMat =
                        new Material(
                                app.getAssetManager(),
                                "Common/MatDefs/Misc/Unshaded.j3md"
                        );

                iconMat.setTexture(
                        "ColorMap",
                        tex
                );

                iconMat.setColor(
                        "Color",
                        ColorRGBA.White
                );

                iconGeom.setMaterial(iconMat);

                iconGeom.setLocalTranslation(
                        0,
                        0,
                        0.1f
                );

                btn.attachChild(iconGeom);

                iconGeoms.put(
                        btn,
                        iconGeom
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "[UIManager] Failed to load icon: "
                            + iconPath
            );

            btn.setText("?");
        }

        btn.setCullHint(
                Node.CullHint.Always
        );

        return btn;
    }

    private void createHUD() {

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        float hudHeightScaled =
                hudHeight * scale;

        float buttonSizeScaled =
                buttonSize * scale;

        Quad quad =
                new Quad(
                        screenWidth,
                        hudHeightScaled
                );

        hudBackground =
                new Geometry(
                        "HUDBackground",
                        quad
                );

        hudBackground.setName(
                "HUDBackground"
        );

        Material mat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        mat.setColor(
                "Color",
                new ColorRGBA(
                        0f,
                        0f,
                        0f,
                        0.5f
                )
        );

        hudBackground.setMaterial(mat);

        healthPotionBtn =
                createIconOnlyButton(
                        "Interface/Icons/hp.png",
                        buttonSizeScaled
                );

        healthPotionBtn.addClickCommands(
                (source) -> {

                    if (playerManager != null) {
                        playerManager.useHealthPotion();
                    }

                    updatePotionCounts();
                    updatePlayerStats();

                    flashButton(healthPotionBtn);
                }
        );

        hudButtons.add(
                healthPotionBtn
        );

        hpCountLabel =
                new Label("0");

        hpCountLabel.setFontSize(
                14 * scale
        );

        hpCountLabel.setColor(
                ColorRGBA.White
        );

        hpCountLabel.setLocalTranslation(
                buttonSizeScaled - 25 * scale,
                10 * scale,
                0.1f
        );

        healthPotionBtn.attachChild(
                hpCountLabel
        );

        manaPotionBtn =
                createIconOnlyButton(
                        "Interface/Icons/mp.png",
                        buttonSizeScaled
                );

        manaPotionBtn.addClickCommands(
                (source) -> {

                    if (playerManager != null) {
                        playerManager.useManaPotion();
                    }

                    updatePotionCounts();
                    updatePlayerStats();

                    flashButton(manaPotionBtn);
                }
        );

        hudButtons.add(
                manaPotionBtn
        );

        mpCountLabel =
                new Label("0");

        mpCountLabel.setFontSize(
                14 * scale
        );

        mpCountLabel.setColor(
                ColorRGBA.White
        );

        mpCountLabel.setLocalTranslation(
                buttonSizeScaled - 25 * scale,
                10 * scale,
                0.1f
        );

        manaPotionBtn.attachChild(
                mpCountLabel
        );

        skill1Btn =
                createIconOnlyButton(
                        "Interface/Icons/light.png",
                        buttonSizeScaled
                );

        skill1Btn.addClickCommands(
                (source) -> {

                    if (playerManager != null) {
                        playerManager.castSkill("Heal");
                    }
                }
        );

        hudButtons.add(skill1Btn);

        skill2Btn =
                createIconOnlyButton(
                        "Interface/Icons/shield.png",
                        buttonSizeScaled
                );

        skill2Btn.addClickCommands(
                (source) -> {

                    if (playerManager != null) {
                        playerManager.castSkill("ShieldBash");
                    }
                }
        );

        hudButtons.add(skill2Btn);

        skill3Btn =
                createIconOnlyButton(
                        "Interface/Icons/whirlwind.png",
                        buttonSizeScaled
                );

        skill3Btn.addClickCommands(
                (source) -> {

                    if (playerManager != null) {
                        playerManager.castSkill("Whirlwind");
                    }
                }
        );

        hudButtons.add(skill3Btn);

        skill4Btn =
                createIconOnlyButton(
                        "Interface/Icons/kick.png",
                        buttonSizeScaled
                );

        skill4Btn.addClickCommands(
                (source) -> {

                    if (playerManager != null) {
                        playerManager.castSkill("Kick");
                    }
                }
        );

        hudButtons.add(skill4Btn);


        talentButton =
                new Button("T");

        talentButton.setPreferredSize(
                new Vector3f(
                        buttonSizeScaled,
                        buttonSizeScaled,
                        0
                )
        );

        talentButton.setBackground(null);
        talentButton.setColor(ColorRGBA.White);

        talentButton.setFontSize(
                24 * scale
        );

        talentButton.addClickCommands(
                (source) -> {

                    SoundManager.playSound(
                            SoundManager.SOUND_CLICK
                    );

                    toggleTalents();
                }
        );
    }

    // ============================================================
    // PLAYER STATS
    // ============================================================

    private void createPlayerStatsUI() {

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        playerStatsContainer =
                new Container();

        playerStatsContainer.setName(
                "PlayerStatsContainer"
        );

        playerStatsContainer.setLayout(
                new SpringGridLayout(
                        Axis.Y,
                        Axis.X
                )
        );

        playerStatsContainer.setPreferredSize(
                new Vector3f(
                        200 * scale,
                        80 * scale,
                        0
                )
        );

        playerStatsContainer.setBackground(
                new QuadBackgroundComponent(
                        new ColorRGBA(
                                0f,
                                0f,
                                0f,
                                0.5f
                        )
                )
        );

        playerStatsContainer.setLocalTranslation(
                10 * scale,
                screenHeight - 110 * scale,
                0
        );

        playerStatsContainer.setCullHint(
                Node.CullHint.Always
        );

        playerNameLabel =
                new Label("Player");

        playerNameLabel.setFontSize(
                18 * scale
        );

        playerNameLabel.setColor(
                ColorRGBA.White
        );

        playerStatsContainer.addChild(
                playerNameLabel
        );

        hpBar =
                new ProgressBar();

        hpBar.setPreferredSize(
                new Vector3f(
                        180 * scale,
                        18 * scale,
                        0
                )
        );

        hpBar.setProgressPercent(1.0f);

        hpBar.setBackground(
                new QuadBackgroundComponent(
                        new ColorRGBA(
                                0.2f,
                                0f,
                                0f,
                                0.8f
                        )
                )
        );

        Panel hpIndicator =
                hpBar.getValueIndicator();

        if (hpIndicator != null) {

            hpIndicator.setBackground(
                    new QuadBackgroundComponent(
                            ColorRGBA.Green
                    )
            );
        }

        playerStatsContainer.addChild(hpBar);

        hpTextLabel =
                new Label("100/100");

        hpTextLabel.setFontSize(
                12 * scale
        );

        hpTextLabel.setColor(
                ColorRGBA.White
        );

        playerStatsContainer.addChild(
                hpTextLabel
        );

        manaBar =
                new ProgressBar();

        manaBar.setPreferredSize(
                new Vector3f(
                        180 * scale,
                        18 * scale,
                        0
                )
        );

        manaBar.setProgressPercent(1.0f);

        manaBar.setBackground(
                new QuadBackgroundComponent(
                        new ColorRGBA(
                                0f,
                                0f,
                                0.2f,
                                0.8f
                        )
                )
        );

        Panel manaIndicator =
                manaBar.getValueIndicator();

        if (manaIndicator != null) {

            manaIndicator.setBackground(
                    new QuadBackgroundComponent(
                            ColorRGBA.Blue
                    )
            );
        }

        playerStatsContainer.addChild(
                manaBar
        );

        manaTextLabel =
                new Label("50/50");

        manaTextLabel.setFontSize(
                12 * scale
        );

        manaTextLabel.setColor(
                ColorRGBA.White
        );

        playerStatsContainer.addChild(
                manaTextLabel
        );

        lastHealth = -1;
        lastMaxHealth = -1;
        lastMana = -1;
        lastMaxMana = -1;
        lastName = "";
    }

    public void updatePlayerStats() {

        if (playerManager == null
                || playerStatsContainer == null) {
            return;
        }

        float hp =
                playerManager.getHealth();

        float maxHp =
                playerManager.getMaxHealth();

        float mana =
                playerManager.getMana();

        float maxMana =
                playerManager.getMaxMana();

        String name =
                playerManager.getPlayerName();

        if (hp != lastHealth
                || maxHp != lastMaxHealth) {

            if (maxHp > 0) {

                hpBar.setProgressPercent(
                        hp / maxHp
                );
            }

            hpTextLabel.setText(
                    (int) hp
                            + " / "
                            + (int) maxHp
            );

            lastHealth = hp;
            lastMaxHealth = maxHp;
        }

        if (mana != lastMana
                || maxMana != lastMaxMana) {

            if (maxMana > 0) {

                manaBar.setProgressPercent(
                        mana / maxMana
                );
            }

            manaTextLabel.setText(
                    (int) mana
                            + " / "
                            + (int) maxMana
            );

            lastMana = mana;
            lastMaxMana = maxMana;
        }

        if (!name.equals(lastName)) {

            playerNameLabel.setText(name);

            lastName = name;
        }
    }

    public void updatePotionCounts() {

        if (playerManager == null) {
            return;
        }

        if (hpCountLabel != null) {

            hpCountLabel.setText(
                    String.valueOf(
                            playerManager.getHealthPotions()
                    )
            );
        }

        if (mpCountLabel != null) {

            mpCountLabel.setText(
                    String.valueOf(
                            playerManager.getManaPotions()
                    )
            );
        }
    }

    public void updateHUDPosition(boolean force) {

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        if (hudBackground != null) {

            hudBackground.setLocalTranslation(
                    0,
                    bottomOffset * scale,
                    0
            );

            Quad quad =
                    (Quad) ((Geometry) hudBackground)
                            .getMesh();

            quad.updateGeometry(
                    screenWidth,
                    hudHeight * scale
            );
        }

        float yPos =
                bottomOffset * scale
                        + 10 * scale;

        float xPos =
                20 * scale;

        float buttonSizeScaled =
                buttonSize * scale;

        float spacingScaled =
                buttonSpacing * scale;

        for (int i = 0;
             i < 2 && i < hudButtons.size();
             i++) {

            Button btn =
                    hudButtons.get(i);

            btn.setLocalTranslation(
                    xPos,
                    yPos,
                    0
            );

            xPos +=
                    buttonSizeScaled
                            + spacingScaled;
        }

        xPos +=
                buttonSizeScaled * 0.5f;

        for (int i = 2;
             i < 6 && i < hudButtons.size();
             i++) {

            Button btn =
                    hudButtons.get(i);

            btn.setLocalTranslation(
                    xPos,
                    yPos,
                    0
            );

            xPos +=
                    buttonSizeScaled
                            + spacingScaled;
        }

        xPos +=
                buttonSizeScaled * 0.5f;

        if (hudButtons.size() > 6) {

            Button btn =
                    hudButtons.get(6);

            btn.setLocalTranslation(
                    xPos,
                    yPos,
                    0
            );
        }

        if (talentButton != null) {

            talentButton.setLocalTranslation(
                    screenWidth
                            - buttonSizeScaled
                            - 10 * scale,
                    screenHeight
                            - buttonSizeScaled
                            - 10 * scale,
                    0
            );
        }

        if (playerStatsContainer != null) {

            playerStatsContainer.setLocalTranslation(
                    10 * scale,
                    screenHeight
                            - 110 * scale,
                    0
            );
        }
    }

    public void showHUD() {

        if (hudNode == null) {
            return;
        }

        attachNode(hudNode);

        hudVisible = true;

        for (Button btn : hudButtons) {
            btn.setCullHint(
                    Node.CullHint.Dynamic
            );
        }

        if (hudBackground != null) {
            hudBackground.setCullHint(
                    Node.CullHint.Dynamic
            );
        }

        if (talentButton != null) {
            talentButton.setCullHint(
                    Node.CullHint.Dynamic
            );
        }

        if (playerStatsContainer != null) {
            playerStatsContainer.setCullHint(
                    Node.CullHint.Dynamic
            );
        }

        updateHUDPosition(true);
        updatePotionCounts();
        updatePlayerStats();
        updatePlayerName();
    }

    public void hideHUD() {

        detachNode(hudNode);

        hudVisible = false;

        for (Button btn : hudButtons) {
            btn.setCullHint(
                    Node.CullHint.Always
            );
        }

        if (hudBackground != null) {
            hudBackground.setCullHint(
                    Node.CullHint.Always
            );
        }

        if (talentButton != null) {
            talentButton.setCullHint(
                    Node.CullHint.Always
            );
        }

        if (playerStatsContainer != null) {
            playerStatsContainer.setCullHint(
                    Node.CullHint.Always
            );
        }
    }

    // ============================================================
    // LOGIN
    // ============================================================

private void createLoginScreen() {
    updateScale();

    float screenWidth = app.getCamera().getWidth();
    float screenHeight = app.getCamera().getHeight();

    float winW = 450 * scale;
    float winH = 300 * scale;

    loginWindow = new Container();
    loginWindow.setPreferredSize(new Vector3f(winW, winH, 0));
    loginWindow.setLayout(null);
    loginWindow.setName("LoginWindow");

    float x = (screenWidth - winW) / 2;
    float y = (screenHeight - winH) / 2;
    if (y < 0) y = 0;
    loginWindow.setLocalTranslation(x, y, 0);

    Geometry bgGeom = createBackgroundGeometry(winW, winH);
    loginWindow.attachChild(bgGeom);

    Label title = new Label(getLocalized("login.title"));
    title.setFontSize(30 * scale);
    title.setColor(ColorRGBA.White);
    title.setLocalTranslation(20 * scale, winH - 35 * scale, 0.1f);
    loginWindow.attachChild(title);

    float labelY1 = winH - 85 * scale;
    Label loginLabel = new Label(getLocalized("login.label.login"));
    loginLabel.setFontSize(18 * scale);
    loginLabel.setColor(ColorRGBA.White);
    loginLabel.setLocalTranslation(20 * scale, labelY1, 0.1f);
    loginWindow.attachChild(loginLabel);

    loginField = new TextField("");
    loginField.setPreferredSize(new Vector3f(220 * scale, 26 * scale, 0));
    loginField.setColor(ColorRGBA.Black);
    loginField.setFontSize(18 * scale);
    loginField.setLocalTranslation(130 * scale, labelY1 - 8 * scale, 0.1f);
    loginField.setSize(loginField.getPreferredSize());
    loginWindow.attachChild(loginField);

    float labelY2 = winH - 140 * scale;
    Label passLabel = new Label(getLocalized("login.label.password"));
    passLabel.setFontSize(18 * scale);
    passLabel.setColor(ColorRGBA.White);
    passLabel.setLocalTranslation(20 * scale, labelY2, 0.1f);
    loginWindow.attachChild(passLabel);

    passwordField = new TextField("");
    passwordField.setPreferredSize(new Vector3f(220 * scale, 26 * scale, 0));
    passwordField.setColor(ColorRGBA.Black);
    passwordField.setFontSize(18 * scale);
    passwordField.setLocalTranslation(130 * scale, labelY2 - 8 * scale, 0.1f);
    passwordField.setSize(passwordField.getPreferredSize());
    loginWindow.attachChild(passwordField);

    Button loginButton = new Button(getLocalized("login.button.login"));
    loginButton.setPreferredSize(new Vector3f(110 * scale, 30 * scale, 0));
    loginButton.setColor(ColorRGBA.White);
    loginButton.setFontSize(18 * scale);
    loginButton.setLocalTranslation(85 * scale, 45 * scale, 0.1f);
    loginButton.addClickCommands((source) -> {
        SoundManager.playSound(SoundManager.SOUND_CLICK);
        if (loginVisible) {
            String login = loginField.getText();
            String pass = passwordField.getText();
            if (!login.isEmpty() && !pass.isEmpty()) {
                handleLogin(login, pass);
            } else {
                showLoginError(getLocalized("login.error.fillfields"));
            }
        }
    });
    loginWindow.attachChild(loginButton);

    Button registerButton = new Button(getLocalized("login.button.register"));
    registerButton.setPreferredSize(new Vector3f(110 * scale, 30 * scale, 0));
    registerButton.setColor(ColorRGBA.White);
    registerButton.setFontSize(18 * scale);
    registerButton.setLocalTranslation(220 * scale, 45 * scale, 0.1f);
    registerButton.addClickCommands((source) -> {
        SoundManager.playSound(SoundManager.SOUND_CLICK);
        if (loginVisible) {
            showRegisterScreen();
        }
    });
    loginWindow.attachChild(registerButton);

    loginFields = new TextField[]{loginField, passwordField};

    // ENTER LOGIN
    loginField.getActionMap().put(new KeyAction(KeyInput.KEY_RETURN), new KeyActionListener() {
        @Override
        public void keyAction(TextEntryComponent source, KeyAction action) {
            if (loginVisible) {
                String login = loginField.getText();
                String pass = passwordField.getText();
                if (!login.isEmpty() && !pass.isEmpty()) {
                    handleLogin(login, pass);
                } else {
                    showLoginError(getLocalized("login.error.fillfields"));
                }
            }
        }
    });

    // ENTER PASSWORD
    passwordField.getActionMap().put(new KeyAction(KeyInput.KEY_RETURN), new KeyActionListener() {
        @Override
        public void keyAction(TextEntryComponent source, KeyAction action) {
            if (loginVisible) {
                String login = loginField.getText();
                String pass = passwordField.getText();
                if (!login.isEmpty() && !pass.isEmpty()) {
                    handleLogin(login, pass);
                } else {
                    showLoginError(getLocalized("login.error.fillfields"));
                }
            }
        }
    });

    guiNode.attachChild(loginWindow);
    loginWindow.setCullHint(Node.CullHint.Always);
}
    // ============================================================
    // REGISTER
    // ============================================================

private void createRegisterScreen() {
    updateScale();

    float screenWidth = app.getCamera().getWidth();
    float screenHeight = app.getCamera().getHeight();

    float winW = 480 * scale;
    float winH = 420 * scale;

    registerWindow = new Container();
    registerWindow.setPreferredSize(new Vector3f(winW, winH, 0));
    registerWindow.setLayout(null);
    registerWindow.setName("RegisterWindow");

    float x = (screenWidth - winW) / 2;
    float y = (screenHeight - winH) / 2;
    if (y < 0) y = 0;
    registerWindow.setLocalTranslation(x, y, 0);

    Geometry bgGeom = createBackgroundGeometry(winW, winH);
    registerWindow.attachChild(bgGeom);

    Label title = new Label(getLocalized("register.title"));
    title.setFontSize(30 * scale);
    title.setColor(ColorRGBA.White);
    title.setLocalTranslation(20 * scale, winH - 35 * scale, 0.1f);
    registerWindow.attachChild(title);

    float labelY1 = winH - 85 * scale;
    Label emailLabel = new Label(getLocalized("register.label.email"));
    emailLabel.setFontSize(18 * scale);
    emailLabel.setColor(ColorRGBA.White);
    emailLabel.setLocalTranslation(20 * scale, labelY1, 0.1f);
    registerWindow.attachChild(emailLabel);

    emailField = new TextField("");
    emailField.setPreferredSize(new Vector3f(240 * scale, 26 * scale, 0));
    emailField.setColor(ColorRGBA.Black);
    emailField.setFontSize(18 * scale);
    emailField.setLocalTranslation(150 * scale, labelY1 - 8 * scale, 0.1f);
    emailField.setSize(emailField.getPreferredSize());
    registerWindow.attachChild(emailField);

    float labelY2 = winH - 140 * scale;
    Label regLoginLabel = new Label(getLocalized("register.label.login"));
    regLoginLabel.setFontSize(18 * scale);
    regLoginLabel.setColor(ColorRGBA.White);
    regLoginLabel.setLocalTranslation(20 * scale, labelY2, 0.1f);
    registerWindow.attachChild(regLoginLabel);

    regLoginField = new TextField("");
    regLoginField.setPreferredSize(new Vector3f(240 * scale, 26 * scale, 0));
    regLoginField.setColor(ColorRGBA.Black);
    regLoginField.setFontSize(18 * scale);
    regLoginField.setLocalTranslation(150 * scale, labelY2 - 8 * scale, 0.1f);
    regLoginField.setSize(regLoginField.getPreferredSize());
    registerWindow.attachChild(regLoginField);

    float labelY3 = winH - 195 * scale;
    Label regPassLabel = new Label(getLocalized("register.label.password"));
    regPassLabel.setFontSize(18 * scale);
    regPassLabel.setColor(ColorRGBA.White);
    regPassLabel.setLocalTranslation(20 * scale, labelY3, 0.1f);
    registerWindow.attachChild(regPassLabel);

    regPasswordField = new TextField("");
    regPasswordField.setPreferredSize(new Vector3f(240 * scale, 26 * scale, 0));
    regPasswordField.setColor(ColorRGBA.Black);
    regPasswordField.setFontSize(18 * scale);
    regPasswordField.setLocalTranslation(150 * scale, labelY3 - 8 * scale, 0.1f);
    regPasswordField.setSize(regPasswordField.getPreferredSize());
    registerWindow.attachChild(regPasswordField);

    Button registerButton = new Button(getLocalized("register.button.register"));
    registerButton.setPreferredSize(new Vector3f(130 * scale, 30 * scale, 0));
    registerButton.setColor(ColorRGBA.White);
    registerButton.setFontSize(18 * scale);
    registerButton.setLocalTranslation(100 * scale, 70 * scale, 0.1f);
    registerButton.addClickCommands((source) -> {
        SoundManager.playSound(SoundManager.SOUND_CLICK);
        if (registerVisible) {
            String email = emailField.getText();
            String login = regLoginField.getText();
            String pass = regPasswordField.getText();
            if (!email.isEmpty() && !login.isEmpty() && !pass.isEmpty()) {
                handleRegister(email, login, pass);
            } else {
                showLoginError(getLocalized("register.error.fillfields"));
            }
        }
    });
    registerWindow.attachChild(registerButton);

    Button backButton = new Button(getLocalized("register.button.back"));
    backButton.setPreferredSize(new Vector3f(110 * scale, 30 * scale, 0));
    backButton.setColor(ColorRGBA.White);
    backButton.setFontSize(18 * scale);
    backButton.setLocalTranslation(260 * scale, 70 * scale, 0.1f);
    backButton.addClickCommands((source) -> showLoginScreen());
    registerWindow.attachChild(backButton);

    registerFields = new TextField[]{emailField, regLoginField, regPasswordField};

    // ENTER EMAIL
    emailField.getActionMap().put(new KeyAction(KeyInput.KEY_RETURN), new KeyActionListener() {
        @Override
        public void keyAction(TextEntryComponent source, KeyAction action) {
            if (registerVisible) {
                String email = emailField.getText();
                String login = regLoginField.getText();
                String pass = regPasswordField.getText();
                if (!email.isEmpty() && !login.isEmpty() && !pass.isEmpty()) {
                    handleRegister(email, login, pass);
                } else {
                    showLoginError(getLocalized("register.error.fillfields"));
                }
            }
        }
    });

    // ENTER LOGIN
    regLoginField.getActionMap().put(new KeyAction(KeyInput.KEY_RETURN), new KeyActionListener() {
        @Override
        public void keyAction(TextEntryComponent source, KeyAction action) {
            if (registerVisible) {
                String email = emailField.getText();
                String login = regLoginField.getText();
                String pass = regPasswordField.getText();
                if (!email.isEmpty() && !login.isEmpty() && !pass.isEmpty()) {
                    handleRegister(email, login, pass);
                } else {
                    showLoginError(getLocalized("register.error.fillfields"));
                }
            }
        }
    });

    // ENTER PASSWORD
    regPasswordField.getActionMap().put(new KeyAction(KeyInput.KEY_RETURN), new KeyActionListener() {
        @Override
        public void keyAction(TextEntryComponent source, KeyAction action) {
            if (registerVisible) {
                String email = emailField.getText();
                String login = regLoginField.getText();
                String pass = regPasswordField.getText();
                if (!email.isEmpty() && !login.isEmpty() && !pass.isEmpty()) {
                    handleRegister(email, login, pass);
                } else {
                    showLoginError(getLocalized("register.error.fillfields"));
                }
            }
        }
    });

    guiNode.attachChild(registerWindow);
    registerWindow.setCullHint(Node.CullHint.Always);
}
    // ============================================================
    // SHOW / HIDE LOGIN
    // ============================================================

    public void showLoginScreen() {

        if (loginWindow == null) {
            return;
        }

        hideRegisterScreen();

        updateLoginPosition();

        loginWindow.setCullHint(
                Node.CullHint.Dynamic
        );

        loginVisible = true;
        registerVisible = false;

        showBackground();

        attachNode(loginWindow);

        app.enqueue(() -> {

            if (loginFields != null
                    && loginFields.length > 0) {

                GuiGlobals.getInstance()
                        .requestFocus(
                                loginFields[0]
                        );

                System.out.println(
                        "[UI] Focus set to login field"
                );
            }
        });
            showLoginBottomButtons();

    }

    public void hideLoginScreen() {

        if (loginVisible) {

            loginWindow.setCullHint(
                    Node.CullHint.Always
            );

            loginVisible = false;

            GuiGlobals.getInstance()
                    .getFocusManagerState()
                    .setFocus(null);

            detachNode(loginWindow);
        }
        hideLoginBottomButtons();
    }

    public void showRegisterScreen() {

        if (registerWindow == null) {
            return;
        }

        hideLoginScreen();

        updateRegisterPosition();

        registerWindow.setCullHint(
                Node.CullHint.Dynamic
        );

        registerVisible = true;
        loginVisible = false;

        attachNode(registerWindow);

        app.enqueue(() -> {

            if (registerFields != null
                    && registerFields.length > 0) {

                GuiGlobals.getInstance()
                        .requestFocus(
                                registerFields[0]
                        );

                System.out.println(
                        "[UI] Focus set to register field"
                );
            }
        });
    }

    public void hideRegisterScreen() {

        if (registerVisible) {

            registerWindow.setCullHint(
                    Node.CullHint.Always
            );

            registerVisible = false;

            GuiGlobals.getInstance()
                    .getFocusManagerState()
                    .setFocus(null);

            detachNode(registerWindow);
        }
    }

    // ============================================================
    // TELEPORTER
    // ============================================================

    private Container teleporterDialog;
    private boolean teleporterDialogVisible = false;

 public void showTeleporterDialog() {
        if (teleporterDialog == null) {
            createTeleporterDialog();
        }

        if (teleporterDialogVisible) {
            return;
        }

        // Принудительно делаем узел видимым перед прикреплением
        teleporterDialog.setCullHint(Node.CullHint.Never);

        teleporterDialogVisible = true;
        attachNode(teleporterDialog);
    }

    public void hideTeleporterDialog() {
        if (teleporterDialog != null && teleporterDialogVisible) {
            // Скрываем узел перед откреплением
            teleporterDialog.setCullHint(Node.CullHint.Always);

            detachNode(teleporterDialog);
            teleporterDialogVisible = false;
        }
    }

private void createTeleporterDialog() {
        updateScale();

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float winW = 350 * scale;
        float winH = 160 * scale;

        teleporterDialog = new Container();
        teleporterDialog.setPreferredSize(new Vector3f(winW, winH, 0));
        teleporterDialog.setLayout(null);
        teleporterDialog.setName("TeleporterDialog");

        float x = (screenWidth - winW) / 2;
        float y = (screenHeight - winH) / 2;
        if (y < 0) y = 0;
        teleporterDialog.setLocalTranslation(x, y, 0);

        Geometry bgGeom = createBackgroundGeometry(winW, winH);
        teleporterDialog.attachChild(bgGeom);

        Label question = new Label(getLocalized("teleporter.question"));
        question.setFontSize(22 * scale);
        question.setColor(ColorRGBA.White);
        question.setLocalTranslation(winW / 2 - 120 * scale, winH - 40 * scale, 0.1f);
        teleporterDialog.attachChild(question);

        Button yesButton = new Button(getLocalized("teleporter.yes"));
        yesButton.setPreferredSize(new Vector3f(80 * scale, 30 * scale, 0));
        yesButton.setFontSize(18 * scale);
        yesButton.setColor(ColorRGBA.White);
        yesButton.setLocalTranslation(60 * scale, 30 * scale, 0.1f);
        yesButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            hideTeleporterDialog();
            Main main = (Main) app;
            if (main != null) {
                WorldManager wm = main.getWorldManager();
                if (wm != null) wm.teleportToDungeon();
            }
        });
        teleporterDialog.attachChild(yesButton);

        Button noButton = new Button(getLocalized("teleporter.no"));
        noButton.setPreferredSize(new Vector3f(80 * scale, 30 * scale, 0));
        noButton.setFontSize(18 * scale);
        noButton.setColor(ColorRGBA.White);
        noButton.setLocalTranslation(180 * scale, 30 * scale, 0.1f);
        noButton.addClickCommands((source) -> hideTeleporterDialog());
        teleporterDialog.attachChild(noButton);

        // ВАЖНО: Сразу после создания делаем узел видимым,
        // чтобы он не остался скрытым после первого открепления.
        teleporterDialog.setCullHint(Node.CullHint.Never);
        guiNode.attachChild(teleporterDialog);
    }    // ============================================================
    // ПОЗИЦИИ
    // ============================================================

    private void updateLoginPosition() {

        if (loginWindow == null) {
            return;
        }

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        float winW =
                450 * scale;

        float winH =
                300 * scale;

        float x =
                (screenWidth - winW) / 2;

        float y =
                (screenHeight - winH) / 2;

        if (y < 0) {
            y = 0;
        }

        loginWindow.setLocalTranslation(
                x,
                y,
                0
        );
    }

    private void updateRegisterPosition() {

        if (registerWindow == null) {
            return;
        }

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        float winW =
                480 * scale;

        float winH =
                420 * scale;

        float x =
                (screenWidth - winW) / 2;

        float y =
                (screenHeight - winH) / 2;

        if (y < 0) {
            y = 0;
        }

        registerWindow.setLocalTranslation(
                x,
                y,
                0
        );
    }

    // ============================================================
    // MANAGERS
    // ============================================================

    public void setPlayerManager(PlayerManager pm) {

        this.playerManager = pm;

        updatePlayerStats();
        updatePlayerName();
    }

    public void setInventoryManager(
            InventoryManager im) {

        this.inventoryManager = im;

        if (im != null) {
            im.setUIManager(this);
        }
    }

    public void setTalentManager(
            TalentManager tm) {

        this.talentManager = tm;

        if (tm != null) {
            createWindows(true);
        }
    }

    private void createWindows(boolean force) {

        if (talentManager == null) {

            System.err.println(
                    "[UIManager] Cannot create windows: talentManager is null"
            );

            return;
        }

        if (force || talentWindow == null) {

            talentWindow =
                    new TalentWindow(
                            app,
                            talentManager,
                            this
                    );
        }

        if (force || traderWindow == null) {

            traderWindow =
                    new TraderWindow(
                            app,
                            playerManager,
                            inventoryManager,
                            this
                    );
        }

        if (force || auctionWindow == null) {

            auctionWindow =
                    new AuctionWindow(
                            app,
                            this,
                            inventoryManager,
                            playerManager
                    );
        }
    }

    // ============================================================
    // GAME STATE
    // ============================================================

    public void onStateChanged(
            GameState newState) {

        if (newState == GameState.LOGIN) {

            showLoginScreen();
            hideHUD();

        } else if (newState == GameState.CITY
                || newState == GameState.DUNGEON) {

            hideLoginScreen();
            hideRegisterScreen();

            showHUD();

            updatePlayerStats();
            updatePotionCounts();

            hideBackground();

        } else {

            hideHUD();
        }
    }

    public boolean isAnyWindowOpen() {

        return loginVisible
                || registerVisible
                || teleporterDialogVisible
                || (inventoryManager != null
                    && inventoryManager.isVisible())
                || (talentWindow != null
                    && talentWindow.isVisible())
                || (traderWindow != null
                    && traderWindow.isVisible())
                || (auctionWindow != null
                    && auctionWindow.isVisible()
                || (blacksmithWindow != null && blacksmithWindow.isVisible()));
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public void update(float tpf) {

        if (hudVisible) {

            updateHUDPosition(false);
            updatePlayerStats();
        }

        if (talentWindow != null) {
            talentWindow.update(tpf);
        }

        if (settingsWindow != null
                && settingsWindow.isVisible()) {

            settingsWindow.update();
        }
    }

    // ============================================================
    // RESIZE
    // ============================================================

    public void onResize(
            int width,
            int height) {

        updateScale();

        if (hudVisible) {

            updateHUDPosition(true);

            if (playerStatsContainer != null) {

                float screenHeight =
                        app.getCamera().getHeight();

                playerStatsContainer.setLocalTranslation(
                        10 * scale,
                        screenHeight - 110 * scale,
                        0
                );
            }
        }

        if (loginVisible) {
            showLoginScreen();
        }

        if (registerVisible) {
            showRegisterScreen();
        }

        if (inventoryManager != null) {
            inventoryManager.updateLayout(
                    width,
                    height
            );
        }

        if (talentWindow != null) {
            talentWindow.updateLayout(
                    width,
                    height
            );
        }

        if (traderWindow != null) {
            traderWindow.updateLayout(
                    width,
                    height
            );
        }

        if (auctionWindow != null) {
            auctionWindow.updateLayout(
                    width,
                    height
            );
        }

        if (backgroundNode != null
                && backgroundNode.getCullHint()
                == Node.CullHint.Never) {

            updateBackgroundScale();
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    public void cleanup() {

        detachNode(hudNode);

        if (inventoryManager != null) {
            inventoryManager.cleanup();
        }

        if (talentWindow != null) {
            talentWindow.hide();
        }

        if (traderWindow != null) {
            traderWindow.hide();
        }

        if (auctionWindow != null) {
            auctionWindow.hide();
        }

        if (mapRenderer != null) {
            mapRenderer.cleanup();
        }

        if (mapWindow != null) {
            mapWindow.cleanup();
        }

        /*
         * Удаляем наш RawInputListener.
         */
        if (authRawInputListener != null) {

            app.getInputManager()
                    .removeRawInputListener(
                            authRawInputListener
                    );

            authRawInputListener = null;
        }
        if (blacksmithWindow != null) {
    blacksmithWindow.cleanup();
}
    }

    public Node getGuiNode() {
        return guiNode;
    }

    // ============================================================
    // LOGIN / NETWORK
    // ============================================================

    private void handleLogin(
            String login,
            String password) {

        System.out.println(
                "[UI] Попытка входа: "
                        + login
        );

        if (networkManager == null) {

            System.err.println(
                    "[UI] NetworkManager не инициализирован, используем локальный вход."
            );

            loadTestCharacter();

            Main main =
                    (Main) app;

            if (main != null) {
                main.loadGameWorld();
            }

            return;
        }

        networkManager
                .login(login, password)
                .thenAccept(success -> {

                    app.enqueue(() -> {

                        if (success) {

                            System.out.println(
                                    "[UI] Вход выполнен успешно."
                            );

                            loadCharacterFromServer();

                        } else {

                            showLoginError(
                                    "Invalid login or password."
                            );
                        }
                    });

                })
                .exceptionally(ex -> {

                    app.enqueue(() ->
                            showLoginError(
                                    "Network error: "
                                            + ex.getMessage()
                            )
                    );

                    return null;
                });
    }

    private void handleRegister(
            String email,
            String login,
            String password) {

        System.out.println(
                "[UI] Регистрация: "
                        + login
        );

        if (networkManager == null) {

            System.err.println(
                    "[UI] NetworkManager не инициализирован."
            );

            loadTestCharacter();

            Main main =
                    (Main) app;

            if (main != null) {
                main.loadGameWorld();
            }

            return;
        }

        networkManager
                .register(
                        email,
                        login,
                        password
                )
                .thenAccept(success -> {

                    app.enqueue(() -> {

                        if (success) {

                            System.out.println(
                                    "[UI] Регистрация успешна. Переход к логину."
                            );

                            showLoginScreen();

                            showToast(
                                    "Registration successful! Please login."
                            );

                        } else {

                            showLoginError(
                                    "Registration failed. Please try again."
                            );
                        }
                    });

                })
                .exceptionally(ex -> {

                    app.enqueue(() ->
                            showLoginError(
                                    "Network error: "
                                            + ex.getMessage()
                            )
                    );

                    return null;
                });
    }

    private void loadCharacterFromServer() {

        if (networkManager == null) {
            return;
        }

        networkManager
                .loadCharacterData()
                .thenAccept(data -> {

                    app.enqueue(() -> {

                        if (data != null) {

                            System.out.println(
                                    "[UI] Данные персонажа загружены с сервера."
                            );

                            applyCharacterData(data);

                            Main main =
                                    (Main) app;

                            if (main != null) {

                                main.loadGameWorld();

                                main.getGameManager()
                                        .setState(
                                                GameState.CITY
                                        );
                            }

                        } else {

                            showLoginError(
                                    "Failed to load character data."
                            );
                        }
                    });

                })
                .exceptionally(ex -> {

                    app.enqueue(() ->
                            showLoginError(
                                    "Network error: "
                                            + ex.getMessage()
                            )
                    );

                    return null;
                });
    }

    // ============================================================
    // CHARACTER DATA
    // ============================================================

    @SuppressWarnings("unchecked")
    public void applyCharacterData(
            Map<String, Object> data) {

        if (playerManager == null) {
            return;
        }

        System.out.println(
                "[UI] applyCharacterData: data keys = "
                        + data.keySet()
        );

        if (data.containsKey("name")) {

            playerManager.setPlayerName(
                    (String) data.get("name")
            );
        }

        if (data.containsKey("gold")) {

            playerManager.setGold(
                    ((Number) data.get("gold"))
                            .intValue()
            );
        }

        if (data.containsKey("healthPotions")) {

            playerManager.setHealthPotions(
                    ((Number) data.get("healthPotions"))
                            .intValue()
            );
        }

        if (data.containsKey("manaPotions")) {

            playerManager.setManaPotions(
                    ((Number) data.get("manaPotions"))
                            .intValue()
            );
        }

        if (data.containsKey("health")) {

            playerManager.setHealth(
                    ((Number) data.get("health"))
                            .intValue()
            );
        }

        if (data.containsKey("maxHealth")) {

            playerManager.setMaxHealth(
                    ((Number) data.get("maxHealth"))
                            .intValue()
            );
        }

        if (data.containsKey("mana")) {

            playerManager.setMana(
                    ((Number) data.get("mana"))
                            .intValue()
            );
        }

        if (data.containsKey("maxMana")) {

            playerManager.setMaxMana(
                    ((Number) data.get("maxMana"))
                            .intValue()
            );
        }

        if (data.containsKey("level")) {

            playerManager.setLevel(
                    ((Number) data.get("level"))
                            .intValue()
            );
        }

        // ДАНЖ И СЛОЖНОСТЬ

        if (data.containsKey("currentDungeon")) {

            playerManager.setCurrentDungeon(
                    (String) data.get("currentDungeon")
            );
        }

        if (data.containsKey("difficulty")) {

            playerManager.setCurrentDifficulty(
                    ((Number) data.get("difficulty"))
                            .intValue()
            );
        }

        updatePlayerName();

        if (data.containsKey("experience")) {

            playerManager.setExperience(
                    ((Number) data.get("experience"))
                            .intValue()
            );
        }

        if (data.containsKey("inventory")) {

            List<Map<String, Object>> invList =
                    (List<Map<String, Object>>)
                            data.get("inventory");

            if (inventoryManager != null) {

                inventoryManager.loadFromServerData(
                        invList
                );
            }
        }

        updatePlayerStats();
        updatePotionCounts();
    }

    // ============================================================
    // LOGIN ERROR
    // ============================================================

    private void showLoginError(
            String message) {

        Label errorLabel =
                new Label(message);

        errorLabel.setFontSize(
                16 * scale
        );

        errorLabel.setColor(
                ColorRGBA.Red
        );

        errorLabel.setLocalTranslation(
                20 * scale,
                250 * scale,
                0.1f
        );

        loginWindow.attachChild(
                errorLabel
        );

        new Thread(() -> {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            app.enqueue(() -> {

                if (loginWindow.hasChild(
                        errorLabel
                )) {

                    loginWindow.detachChild(
                            errorLabel
                    );
                }
            });

        }).start();
    }

    // ============================================================
    // TOAST
    // ============================================================

    public void showToast(
            String message) {

        Label toast =
                new Label(message);

        toast.setFontSize(
                18 * scale
        );

        toast.setColor(
                ColorRGBA.Green
        );

        toast.setBackground(
                new QuadBackgroundComponent(
                        new ColorRGBA(
                                0.1f,
                                0.1f,
                                0.1f,
                                0.9f
                        )
                )
        );

        toast.setPreferredSize(
                new Vector3f(
                        400 * scale,
                        40 * scale,
                        0
                )
        );

        toast.setLocalTranslation(
                (app.getCamera().getWidth()
                        - 400 * scale) / 2,
                app.getCamera().getHeight()
                        - 100 * scale,
                0.1f
        );

        guiNode.attachChild(toast);

        new Thread(() -> {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            app.enqueue(() ->
                    guiNode.detachChild(toast)
            );

        }).start();
    }

    // ============================================================
    // TEST CHARACTER
    // ============================================================

    private void loadTestCharacter() {

        Main main =
                (Main) app;

        if (main == null) {
            return;
        }

        PlayerManager pm =
                main.getPlayerManager();

        if (pm == null) {
            return;
        }

        pm.setPlayerName("Test Player");
        pm.setLevel(1);

        pm.setMaxHealth(100);
        pm.setHealth(100);

        pm.setMaxMana(50);
        pm.setMana(50);

        pm.setExperience(0);
        pm.setGold(100);

        pm.setHealthPotions(3);
        pm.setManaPotions(3);

        InventoryManager im =
                (InventoryManager)
                        main.getInventoryManager();

        if (im != null) {

            im.addItem(
                    ItemGenerator.generateItem(
                            1,
                            "Weapon",
                            1
                    )
            );

            im.addItem(
                    ItemGenerator.generateItem(
                            1,
                            "Helmet",
                            1
                    )
            );

            im.addItem(
                    ItemGenerator.generateItem(
                            1,
                            "Chest",
                            1
                    )
            );

            im.addItem(
                    ItemGenerator.generateItem(
                            1,
                            "Legs",
                            1
                    )
            );

            im.addItem(
                    ItemGenerator.generateItem(
                            1,
                            "Boots",
                            1
                    )
            );
        }

        GameManager gm =
                main.getGameManager();

        if (gm != null) {

            gm.setState(
                    GameState.CITY
            );
        }

        updatePlayerStats();
        updatePotionCounts();
    }

    public void updatePlayerName() {

        if (playerManager == null
                || playerNameLabel == null) {
            return;
        }

        String name =
                playerManager.getPlayerName();

        int level =
                playerManager.getLevel();

        playerNameLabel.setText(
                name
                        + " ["
                        + level
                        + "]"
        );
    }

    // ============================================================
    // LOADING SCREEN
    // ============================================================

    private Node loadingScreenNode;
    private Geometry loadingImage;

    private List<String> loadingImages =
            Arrays.asList(
                    "Interface/hud/1.png",
                    "Interface/hud/2.png",
                    "Interface/hud/3.png",
                    "Interface/hud/4.png",
                    "Interface/hud/5.png",
                    "Interface/hud/6.png"
            );

    private Random random =
            new Random();

    public void showLoadingScreen() {

        if (loadingScreenNode == null) {

            loadingScreenNode =
                    new Node("LoadingScreen");

            float w =
                    app.getCamera().getWidth();

            float h =
                    app.getCamera().getHeight();

            if (w <= 0) {
                w = 1280;
            }

            if (h <= 0) {
                h = 720;
            }

            Quad quad =
                    new Quad(w, h);

            loadingImage =
                    new Geometry(
                            "LoadingImage",
                            quad
                    );

            loadingImage.setLocalTranslation(
                    0,
                    0,
                    0
            );

            loadingScreenNode.attachChild(
                    loadingImage
            );

            loadingScreenNode.setLocalTranslation(
                    0,
                    0,
                    100
            );

        } else {

            updateLoadingScreenSize();
        }

        String path =
                loadingImages.get(
                        random.nextInt(
                                loadingImages.size()
                        )
                );

        try {

            Texture tex =
                    app.getAssetManager()
                            .loadTexture(path);

            Material mat =
                    new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );

            mat.setTexture(
                    "ColorMap",
                    tex
            );

            loadingImage.setMaterial(mat);

        } catch (Exception e) {

            Material mat =
                    new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );

            mat.setColor(
                    "Color",
                    new ColorRGBA(
                            0.2f,
                            0.2f,
                            0.3f,
                            1f
                    )
            );

            loadingImage.setMaterial(mat);
        }

        if (!guiNode.hasChild(
                loadingScreenNode
        )) {

            guiNode.attachChild(
                    loadingScreenNode
            );
        }
    }

    public void hideLoadingScreen() {

        if (loadingScreenNode != null
                && guiNode.hasChild(
                        loadingScreenNode
                )) {

            guiNode.detachChild(
                    loadingScreenNode
            );
        }
    }

    private void updateLoadingScreenSize() {

        if (loadingImage == null) {
            return;
        }

        float w =
                app.getCamera().getWidth();

        float h =
                app.getCamera().getHeight();

        if (w <= 0) {
            w = 1280;
        }

        if (h <= 0) {
            h = 720;
        }

        Quad q =
                (Quad) loadingImage.getMesh();

        q.updateGeometry(
                w,
                h
        );

        loadingImage.setLocalTranslation(
                0,
                0,
                0
        );
    }

    // ============================================================
    // MAP
    // ============================================================

    private MapRenderer mapRenderer;
    private MapWindow mapWindow;

    public void initMap(
            Node sceneNode,
            PlayerManager pm) {

        if (mapRenderer == null) {

            mapRenderer =
                    new MapRenderer(app);

            mapRenderer.initialize();
        }

        if (mapWindow == null) {

            mapWindow =
                    new MapWindow(
                            app,
                            mapRenderer,
                            pm
                    );

            mapWindow.show();
        }
    }

    public void toggleMap() {

        if (mapWindow == null) {
            return;
        }

        if (mapWindow.isVisible()) {

            mapWindow.hide();

            SoundManager.playSound(
                    SoundManager.SOUND_WINDOW_CLOSE
            );

        } else {

            closeAllWindowsExcept("map");

            mapWindow.show();

            SoundManager.playSound(
                    SoundManager.SOUND_WINDOW_TALENTS
            );
        }
    }

    public void updateMap(float tpf) {

        if (mapRenderer == null
                || playerManager == null
                || mapWindow == null) {

            return;
        }

        if (!mapWindow.isVisible()) {
            return;
        }

        Vector3f pos =
                playerManager.getPosition();

        if (pos != null) {
            mapRenderer.update(playerManager);
        }

        mapWindow.update();
    }
    private String getLocalized(String key) {
    return LocalizationManager.getInstance().get(key);
}
    
    
     private boolean loginButtonsVisible = false;
    private Button creditsButton;
    private Button bestiaryButton;
    private Container creditsWindow;
    private BestiaryWindow bestiaryWindow;

    // Добавьте этот метод в createLoginScreen() или initialize()
    private void createLoginBottomButtons() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        // Создаем кнопку Credits
        creditsButton = new Button(getLocalized("ui.credits"));
        creditsButton.setFontSize(18 * scale);
        creditsButton.setPreferredSize(new Vector3f(100 * scale, 30 * scale, 0));
        creditsButton.setLocalTranslation(screenWidth - 240 * scale, 20 * scale, 0.1f);
        creditsButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            showCreditsWindow();
        });
        creditsButton.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(creditsButton);

        // Создаем кнопку Bestiary
        bestiaryButton = new Button(getLocalized("ui.bestiary"));
        bestiaryButton.setFontSize(18 * scale);
        bestiaryButton.setPreferredSize(new Vector3f(100 * scale, 30 * scale, 0));
        bestiaryButton.setLocalTranslation(screenWidth - 140 * scale, 20 * scale, 0.1f);
        bestiaryButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            toggleBestiaryWindow();
        });
        bestiaryButton.setCullHint(Node.CullHint.Always);
        //guiNode.attachChild(bestiaryButton);
    }

public void showLoginBottomButtons() {

    if (creditsButton != null && !guiNode.hasChild(creditsButton)) {
        guiNode.attachChild(creditsButton);
    }

    if (bestiaryButton != null && !guiNode.hasChild(bestiaryButton)) {
        guiNode.attachChild(bestiaryButton);
    }

    if (creditsButton != null) creditsButton.setCullHint(Node.CullHint.Never);
    if (bestiaryButton != null) bestiaryButton.setCullHint(Node.CullHint.Never);

    loginButtonsVisible = true;
}

public void hideLoginBottomButtons() {

    if (creditsButton != null && guiNode.hasChild(creditsButton)) {
        guiNode.detachChild(creditsButton);
    }

    if (bestiaryButton != null && guiNode.hasChild(bestiaryButton)) {
        guiNode.detachChild(bestiaryButton);
    }

    loginButtonsVisible = false;
}

    // ============================================================
    // CREDITS WINDOW
    // ============================================================
    public void showCreditsWindow() {

    // ============================================================
    // ЕСЛИ ОКНО УЖЕ СОЗДАНО
    // ============================================================

    if (creditsWindow != null) {

        creditsWindow.setCullHint(
                Node.CullHint.Never
        );

        // Поднимаем поверх всех окон
        Vector3f pos =
                creditsWindow.getLocalTranslation();

        creditsWindow.setLocalTranslation(
                pos.x,
                pos.y,
                5000f
        );

        return;
    }


    // ============================================================
    // РАЗМЕР ОКНА
    // ============================================================

    float winW =
            430f * scale;

    float winH =
            560f * scale;


    // ============================================================
    // ОТСТУПЫ
    // ============================================================

    float sideMargin =
            35f * scale;

    float topMargin =
            30f * scale;

    float bottomMargin =
            25f * scale;


    // ============================================================
    // СОЗДАЁМ ОКНО
    // ============================================================

    creditsWindow =
            new Container();


    creditsWindow.setPreferredSize(
            new Vector3f(
                    winW,
                    winH,
                    0f
            )
    );


    creditsWindow.setLayout(
            null
    );


    // ============================================================
    // ЦЕНТРИРУЕМ ОКНО
    // ============================================================

    float windowX =
            (app.getCamera().getWidth() - winW) / 2f;

    float windowY =
            (app.getCamera().getHeight() - winH) / 2f;


    // ============================================================
    // Z-СЛОЙ
    // ============================================================
    //
    // Большое значение, чтобы Credits был поверх
    // логина, полей ввода и остальных окон.
    //

    final float CREDITS_Z =
            5000f;


    creditsWindow.setLocalTranslation(
            windowX,
            windowY,
            CREDITS_Z
    );


    // ============================================================
    // КОЖАНЫЙ ФОН
    // ============================================================

    Geometry leatherBg =
            createBackgroundGeometry(
                    winW,
                    winH
            );


    leatherBg.setLocalTranslation(
            0f,
            0f,
            0f
    );


    creditsWindow.attachChild(
            leatherBg
    );


    // ============================================================
    // ИМЯ
    // ============================================================

    Label nameLabel =
            new Label(
                    "Ivan Aleksandrov"
            );


    nameLabel.setFontSize(
            22f * scale
    );


    nameLabel.setColor(
            ColorRGBA.White
    );


    nameLabel.setLocalTranslation(
            sideMargin,
            winH - topMargin,
            30f
    );


    creditsWindow.attachChild(
            nameLabel
    );


    // ============================================================
    // ИЗОБРАЖЕНИЯ
    // ============================================================

    try {

        // ========================================================
        // ПОРТРЕТ DEVELOPER.PNG
        // ========================================================
        //
        // Прямоугольный формат.
        //

        Texture devTex =
                app.getAssetManager().loadTexture(
                        "Interface/developer.png"
                );


        float portraitW =
                winW - sideMargin * 2f;


        float portraitH =
                250f * scale;


        Geometry devImg =
                new Geometry(
                        "DevImage",
                        new Quad(
                                portraitW,
                                portraitH
                        )
                );


        Material devMat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );


        devMat.setTexture(
                "ColorMap",
                devTex
        );


        devImg.setMaterial(
                devMat
        );


        // Портрет под именем
        devImg.setLocalTranslation(
                sideMargin,
                winH
                        - topMargin
                        - 40f * scale
                        - portraitH,
                10f
        );


        creditsWindow.attachChild(
                devImg
        );


        // ========================================================
        // INTRO_BG.PNG
        // ========================================================
        //
        // Квадратная иконка.
        //

        Texture introTex =
                app.getAssetManager().loadTexture(
                        "Interface/intro_bg.png"
                );


        float iconSize =
                150f * scale;


        Geometry introImg =
                new Geometry(
                        "IntroImage",
                        new Quad(
                                iconSize,
                                iconSize
                        )
                );


        Material introMat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );


        introMat.setTexture(
                "ColorMap",
                introTex
        );


        introImg.setMaterial(
                introMat
        );


        // Центрируем квадрат
        float iconX =
                (winW - iconSize) / 2f;


        float iconY =
                bottomMargin;


        introImg.setLocalTranslation(
                iconX,
                iconY,
                10f
        );


        creditsWindow.attachChild(
                introImg
        );


    } catch (Exception e) {

        System.err.println(
                "[CreditsWindow] Failed to load images:"
        );

        e.printStackTrace();
    }


    // ============================================================
    // КРЕСТИК
    // ============================================================

    Button closeBtn =
            new Button(
                    "X"
            );


    closeBtn.setPreferredSize(
            new Vector3f(
                    40f * scale,
                    40f * scale,
                    0f
            )
    );


    closeBtn.setLocalTranslation(
            winW - 50f * scale,
            winH - 50f * scale,
            50f
    );


    closeBtn.addClickCommands(
            source -> {

                creditsWindow.setCullHint(
                        Node.CullHint.Always
                );

            }
    );


    creditsWindow.attachChild(
            closeBtn
    );


    // ============================================================
    // ДОБАВЛЯЕМ ОКНО В GUI
    // ============================================================

    guiNode.attachChild(
            creditsWindow
    );


    // ============================================================
    // ФИНАЛЬНО УСТАНАВЛИВАЕМ Z
    // ============================================================

    creditsWindow.setLocalTranslation(
            windowX,
            windowY,
            CREDITS_Z
    );
}

    // ============================================================
    // BESTIARY WINDOW
    // ============================================================
public void toggleBestiaryWindow() {

    if (bestiaryWindow == null) {

        bestiaryWindow =
                new BestiaryWindow(
                        app,
                        this
                );
    }


    if (!bestiaryVisible) {

        bestiaryVisible = true;

        bestiaryWindow.showView();

    } else {

        bestiaryVisible = false;

        bestiaryWindow.hideView();
    }
}
private boolean bestiaryVisible = false;

private int bestiaryCurrentIndex = 0;
private BlacksmithWindow blacksmithWindow;

public BlacksmithWindow getBlacksmithWindow() {
    if (blacksmithWindow == null) {
        blacksmithWindow = new BlacksmithWindow(app, this);
    }
    return blacksmithWindow;
}
public void toggleBlacksmith() {

    BlacksmithWindow w = getBlacksmithWindow();

    if (w.isVisible()) {
        w.hide();
        SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
    } else {
        closeAllWindowsExcept("blacksmith");
        w.show();
        SoundManager.playSound(SoundManager.SOUND_WINDOW_TRADER);
    }
}
private CharacterStatsWindow characterStatsWindow;
private static final String KEY_CHARACTER_STATS = "characterStats";
public void toggleCharacterStats() {

    if (characterStatsWindow == null) {

        if (playerManager == null) {
            return;
        }

        characterStatsWindow = new CharacterStatsWindow(app, this, playerManager);
    }

    if (characterStatsWindow.isVisible()) {
        characterStatsWindow.hide();
        SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
    } else {
        closeAllWindowsExcept("characterStats");
        characterStatsWindow.show();
        SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
    }
}
}