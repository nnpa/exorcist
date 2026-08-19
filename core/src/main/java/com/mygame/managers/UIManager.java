package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.*;
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
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;
import com.mygame.Main;
import com.mygame.items.ItemGenerator;
import com.mygame.managers.GameManager.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UIManager {
    // Поля
    private Node backgroundNode;
    private Geometry backgroundGeom;
    private String backgroundTexturePath = "Interface/login_bg.png";
    private TalentManager talentManager;

    public TalentManager getTalentManager() { return talentManager; }

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

    // Методы открытия окон с проверкой
    public void openAuction() {
        toggleAuction();
    }

    public void openTrader() {
        toggleTrader();
    }

   public void toggleInventory() {
    if (inventoryManager == null) return;
    // Если инвентарь открыт → закрываем
    if (inventoryManager.isVisible()) {
        inventoryManager.hide();
        return;
    }
    // Иначе закрываем все остальные окна и открываем инвентарь
    closeAllWindowsExcept("inventory");
    inventoryManager.show();
}

public void toggleTalents() {
    if (talentWindow == null) return;
    if (talentWindow.isVisible()) {
        talentWindow.hide();
        return;
    }
    closeAllWindowsExcept("talents");
    talentWindow.show();
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
    traderWindow.show();
}

private void closeAllWindowsExcept(String keepOpen) {
    // Закрываем инвентарь
    if (!"inventory".equals(keepOpen) && inventoryManager != null && inventoryManager.isVisible()) {
        inventoryManager.hide();
    }
    // Закрываем таланты
    if (!"talents".equals(keepOpen) && talentWindow != null && talentWindow.isVisible()) {
        talentWindow.hide();
    }
    // Закрываем аукцион
    if (!"auction".equals(keepOpen) && auctionWindow != null && auctionWindow.isVisible()) {
        auctionWindow.hide();
    }
    // Закрываем торговца
    if (!"trader".equals(keepOpen) && traderWindow != null && traderWindow.isVisible()) {
        traderWindow.hide();
    }
}

    private boolean isAuctionOrTraderOpen() {
        return (auctionWindow != null && auctionWindow.isVisible()) ||
               (traderWindow != null && traderWindow.isVisible());
    }

    private boolean isInventoryOrTalentsOpen() {
        return (inventoryManager != null && inventoryManager.isVisible()) ||
               (talentWindow != null && talentWindow.isVisible());
    }

    private void closeAuctionAndTrader() {
        if (auctionWindow != null && auctionWindow.isVisible()) auctionWindow.hide();
        if (traderWindow != null && traderWindow.isVisible()) traderWindow.hide();
    }

    private void closeInventoryAndTalents() {
        if (inventoryManager != null && inventoryManager.isVisible()) inventoryManager.hide();
        if (talentWindow != null && talentWindow.isVisible()) talentWindow.hide();
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

    // HUD элементы
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

    // Элементы статистики
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
    private TextField loginField, passwordField;
    private TextField emailField, regLoginField, regPasswordField;
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

    // RawInputListener для перехвата Tab и Enter
    private RawInputListener authRawListener;

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

    // ===== УПРАВЛЕНИЕ НОДАМИ =====
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

    public void onInventoryOpened(Node node) { attachNode(node); }
    public void onInventoryClosed(Node node) { detachNode(node); }
    public void onTalentOpened(Node node) { attachNode(node); }
    public void onTalentClosed(Node node) { detachNode(node); }
    public void onTraderOpened(Node node) { attachNode(node); }
    public void onTraderClosed(Node node) { detachNode(node); }

    // ===== ФОНОВАЯ ГЕОМЕТРИЯ =====
    public Geometry createBackgroundGeometry(float width, float height) {
        Texture leatherTexture = null;
        try {
            leatherTexture = app.getAssetManager().loadTexture("Interface/leather_border.png");
        } catch (Exception e) {
            System.err.println("[UIManager] Текстура не загружена, используем цвет.");
        }

        Quad quad = new Quad(width, height);
        Geometry bgGeom = new Geometry("WindowBg", quad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        if (leatherTexture != null) {
            mat.setTexture("ColorMap", leatherTexture);
        } else {
            mat.setColor("Color", new ColorRGBA(0.4f, 0.2f, 0.05f, 1f));
        }
        bgGeom.setMaterial(mat);
        bgGeom.setLocalTranslation(0, 0, 0f);
        return bgGeom;
    }

    // ===== ИНИЦИАЛИЗАЦИЯ =====
    public void initialize() {
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
        hudNode.attachChild(talentButton);
        hudNode.attachChild(playerStatsContainer);

        // Изначально все окна скрыты
        loginWindow.setCullHint(Node.CullHint.Always);
        registerWindow.setCullHint(Node.CullHint.Always);
        hideHUD();
        GuiGlobals.getInstance().getFocusNavigationState().setEnabled(false);

        // Регистрируем обработчики
        setupKeyboardShortcuts();
        setupAuthRawInputListener();

        createBackground();
        hideBackground();

        if (backgroundNode != null && !guiNode.hasChild(backgroundNode)) {
            guiNode.attachChild(backgroundNode);
        }
    }

    // ===== ФОНОВОЕ ИЗОБРАЖЕНИЕ =====
    private void createBackground() {
        if (backgroundNode != null) return;
        backgroundNode = new Node("LoginBackground");
        try {
            Texture tex = app.getAssetManager().loadTexture(backgroundTexturePath);
            float w = app.getCamera().getWidth();
            float h = app.getCamera().getHeight();
            Quad quad = new Quad(w, h);
            backgroundGeom = new Geometry("LoginBg", quad);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", tex);
            backgroundGeom.setMaterial(mat);
            backgroundGeom.setLocalTranslation(0, 0, -10);
            backgroundNode.attachChild(backgroundGeom);
            backgroundNode.setCullHint(Node.CullHint.Always);
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to load background, using fallback");
            createFallbackBackground();
        }
    }

    private void createFallbackBackground() {
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        Quad quad = new Quad(w, h);
        backgroundGeom = new Geometry("LoginBg", quad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.25f, 1f));
        backgroundGeom.setMaterial(mat);
        backgroundGeom.setLocalTranslation(0, 0, -10);
        backgroundNode.attachChild(backgroundGeom);
        backgroundNode.setCullHint(Node.CullHint.Always);
    }

    private void updateBackgroundScale() {
        if (backgroundGeom == null) return;
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        Quad q = (Quad) backgroundGeom.getMesh();
        q.updateGeometry(w, h);
        backgroundGeom.setLocalTranslation(0, 0, -10);
    }

    private void showBackground() {
        if (backgroundNode != null) {
            backgroundNode.setCullHint(Node.CullHint.Never);
            updateBackgroundScale();
        }
    }

    private void hideBackground() {
        if (backgroundNode != null) {
            backgroundNode.setCullHint(Node.CullHint.Always);
        }
    }

    public void forceShowLogin() {
        showLoginScreen();
    }

    // ===== HUD КЛАВИШИ =====
    private void setupKeyboardShortcuts() {
        app.getInputManager().addMapping(KEY_SKILL1, new KeyTrigger(KeyInput.KEY_1));
        app.getInputManager().addMapping(KEY_SKILL2, new KeyTrigger(KeyInput.KEY_2));
        app.getInputManager().addMapping(KEY_SKILL3, new KeyTrigger(KeyInput.KEY_3));
        app.getInputManager().addMapping(KEY_SKILL4, new KeyTrigger(KeyInput.KEY_4));
        app.getInputManager().addMapping(KEY_HEALTH_POTION, new KeyTrigger(KeyInput.KEY_Q));
        app.getInputManager().addMapping(KEY_MANA_POTION, new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping(KEY_INVENTORY, new KeyTrigger(KeyInput.KEY_I));
        app.getInputManager().addMapping(KEY_TALENTS, new KeyTrigger(KeyInput.KEY_T));

        ActionListener listener = new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return;
                if (!hudVisible) return;

                switch (name) {
                    case KEY_SKILL1:
                        if (playerManager != null) playerManager.castSkill("Heal");
                        flashButton(skill1Btn);
                        break;
                    case KEY_SKILL2:
                        if (playerManager != null) playerManager.castSkill("ShieldBash");
                        flashButton(skill2Btn);
                        break;
                    case KEY_SKILL3:
                        if (playerManager != null) playerManager.castSkill("Whirlwind");
                        flashButton(skill3Btn);
                        break;
                    case KEY_SKILL4:
                        if (playerManager != null) playerManager.castSkill("Kick");
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
                        toggleInventory(); // <- изменено
                        flashButton(inventoryButton);
                        break;
                    case KEY_TALENTS:
                        toggleTalents(); // <- изменено
                        flashButton(talentButton);
                        break;
                }
            }
        };
        app.getInputManager().addListener(listener, KEY_SKILL1, KEY_SKILL2, KEY_SKILL3, KEY_SKILL4,
                KEY_HEALTH_POTION, KEY_MANA_POTION, KEY_INVENTORY, KEY_TALENTS);
    }

    private void flashButton(Button btn) {
        if (btn == null) return;
        Geometry iconGeom = iconGeoms.get(btn);
        if (iconGeom == null) return;
        Material mat = iconGeom.getMaterial();
        if (mat == null) return;
        MatParam colorParam = mat.getParam("Color");
        ColorRGBA originalColor = (colorParam != null) ? (ColorRGBA) colorParam.getValue() : ColorRGBA.White;
        final ColorRGBA finalOriginalColor = originalColor.clone();
        mat.setColor("Color", new ColorRGBA(1f, 1f, 0f, 1f));
        new Thread(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            app.enqueue(() -> mat.setColor("Color", finalOriginalColor));
        }).start();
    }

    private void setupAuthRawInputListener() {
        if (authRawListener != null) {
            app.getInputManager().removeRawInputListener(authRawListener);
        }

        authRawListener = new RawInputListener() {
            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onMouseMotionEvent(MouseMotionEvent evt) {}
            @Override public void onMouseButtonEvent(MouseButtonEvent evt) {}

            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                if (evt.isPressed()) {
                    int key = evt.getKeyCode();
                    if (key == KeyInput.KEY_TAB || key == KeyInput.KEY_RETURN) {
                        processAuthKey(key);
                        evt.setConsumed();
                    }
                }
            }

            private void processAuthKey(int key) {
                if (key == KeyInput.KEY_TAB) {
                    if (loginVisible && loginFields != null && loginFields.length > 0) {
                        moveFocus(loginFields);
                    } else if (registerVisible && registerFields != null && registerFields.length > 0) {
                        moveFocus(registerFields);
                    }
                } else if (key == KeyInput.KEY_RETURN) {
                    if (loginVisible) {
                        String login = loginField.getText();
                        String pass = passwordField.getText();
                        if (!login.isEmpty() && !pass.isEmpty()) {
                            handleLogin(login, pass);
                        }
                    } else if (registerVisible) {
                        String email = emailField.getText();
                        String login = regLoginField.getText();
                        String pass = regPasswordField.getText();
                        if (!email.isEmpty() && !login.isEmpty() && !pass.isEmpty()) {
                            handleRegister(email, login, pass);
                        }
                    }
                }
            }

            @Override public void onTouchEvent(TouchEvent evt) {}
        };
        app.getInputManager().addRawInputListener(authRawListener);
        System.out.println("[UI] Auth RawInputListener for Tab/Enter setup complete.");
    }

    // ===== ПЕРЕКЛЮЧЕНИЕ ФОКУСА =====
    private void moveFocus(TextField[] fields) {
        if (fields == null || fields.length == 0) return;
        Spatial currentFocus = GuiGlobals.getInstance().getFocusManagerState().getFocus();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == currentFocus) {
                int nextIndex = (i + 1) % fields.length;
                GuiGlobals.getInstance().requestFocus(fields[nextIndex]);
                return;
            }
        }
        GuiGlobals.getInstance().requestFocus(fields[0]);
    }

    // ===== HUD =====
    private Button createIconOnlyButton(String iconPath, float size) {
        Button btn = new Button("");
        btn.setPreferredSize(new Vector3f(size, size, 0));
        btn.setBackground(null);
        btn.setColor(ColorRGBA.White);
        try {
            Texture tex = app.getAssetManager().loadTexture(iconPath);
            if (tex != null) {
                Geometry iconGeom = new Geometry("Icon", new Quad(size, size));
                Material iconMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                iconMat.setTexture("ColorMap", tex);
                iconMat.setColor("Color", ColorRGBA.White);
                iconGeom.setMaterial(iconMat);
                iconGeom.setLocalTranslation(0, 0, 0.1f);
                btn.attachChild(iconGeom);
                iconGeoms.put(btn, iconGeom);
            }
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to load icon: " + iconPath);
            btn.setText("?");
        }
        btn.setCullHint(Node.CullHint.Always);
        return btn;
    }

    private void createHUD() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float hudHeightScaled = hudHeight * scale;
        float buttonSizeScaled = buttonSize * scale;

        Quad quad = new Quad(screenWidth, hudHeightScaled);
        hudBackground = new Geometry("HUDBackground", quad);
        hudBackground.setName("HUDBackground");
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.5f));
        hudBackground.setMaterial(mat);

        healthPotionBtn = createIconOnlyButton("Interface/Icons/hp.png", buttonSizeScaled);
        healthPotionBtn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.useHealthPotion();
            updatePotionCounts();
            updatePlayerStats();
            flashButton(healthPotionBtn);
        });
        hudButtons.add(healthPotionBtn);
        hpCountLabel = new Label("0");
        hpCountLabel.setFontSize(14 * scale);
        hpCountLabel.setColor(ColorRGBA.White);
        hpCountLabel.setLocalTranslation(buttonSizeScaled - 25 * scale, 10 * scale, 0.1f);
        healthPotionBtn.attachChild(hpCountLabel);

        manaPotionBtn = createIconOnlyButton("Interface/Icons/mp.png", buttonSizeScaled);
        manaPotionBtn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.useManaPotion();
            updatePotionCounts();
            updatePlayerStats();
            flashButton(manaPotionBtn);
        });
        hudButtons.add(manaPotionBtn);
        mpCountLabel = new Label("0");
        mpCountLabel.setFontSize(14 * scale);
        mpCountLabel.setColor(ColorRGBA.White);
        mpCountLabel.setLocalTranslation(buttonSizeScaled - 25 * scale, 10 * scale, 0.1f);
        manaPotionBtn.attachChild(mpCountLabel);

        // Скиллы
        skill1Btn = createIconOnlyButton("Interface/Icons/light.png", buttonSizeScaled);
        skill1Btn.addClickCommands((source) -> { if (playerManager != null) playerManager.castSkill("Heal"); });
        hudButtons.add(skill1Btn);

        skill2Btn = createIconOnlyButton("Interface/Icons/shield.png", buttonSizeScaled);
        skill2Btn.addClickCommands((source) -> { if (playerManager != null) playerManager.castSkill("ShieldBash"); });
        hudButtons.add(skill2Btn);

        skill3Btn = createIconOnlyButton("Interface/Icons/whirlwind.png", buttonSizeScaled);
        skill3Btn.addClickCommands((source) -> { if (playerManager != null) playerManager.castSkill("Whirlwind"); });
        hudButtons.add(skill3Btn);

        skill4Btn = createIconOnlyButton("Interface/Icons/kick.png", buttonSizeScaled);
        skill4Btn.addClickCommands((source) -> { if (playerManager != null) playerManager.castSkill("Kick"); });
        hudButtons.add(skill4Btn);

        // Инвентарь (изменён обработчик)
        inventoryButton = createIconOnlyButton("Interface/Icons/backpack.png", buttonSizeScaled);
        inventoryButton.addClickCommands((source) -> {
            toggleInventory();
        });
        hudButtons.add(inventoryButton);

        // Таланты (изменён обработчик)
        talentButton = new Button("T");
        talentButton.setPreferredSize(new Vector3f(buttonSizeScaled, buttonSizeScaled, 0));
        talentButton.setBackground(null);
        talentButton.setColor(ColorRGBA.White);
        talentButton.setFontSize(24 * scale);
        talentButton.addClickCommands((source) -> {
            toggleTalents();
        });
    }

    private void createPlayerStatsUI() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        playerStatsContainer = new Container();
        playerStatsContainer.setName("PlayerStatsContainer");
        playerStatsContainer.setLayout(new SpringGridLayout(Axis.Y, Axis.X));
        playerStatsContainer.setPreferredSize(new Vector3f(200 * scale, 80 * scale, 0));
        playerStatsContainer.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0f, 0.5f)));
        playerStatsContainer.setLocalTranslation(10 * scale, screenHeight - 110 * scale, 0);
        playerStatsContainer.setCullHint(Node.CullHint.Always);

        playerNameLabel = new Label("Player");
        playerNameLabel.setFontSize(18 * scale);
        playerNameLabel.setColor(ColorRGBA.White);
        playerStatsContainer.addChild(playerNameLabel);

        hpBar = new ProgressBar();
        hpBar.setPreferredSize(new Vector3f(180 * scale, 18 * scale, 0));
        hpBar.setProgressPercent(1.0f);
        hpBar.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0f, 0f, 0.8f)));
        Panel hpIndicator = hpBar.getValueIndicator();
        if (hpIndicator != null) hpIndicator.setBackground(new QuadBackgroundComponent(ColorRGBA.Green));
        playerStatsContainer.addChild(hpBar);

        hpTextLabel = new Label("100/100");
        hpTextLabel.setFontSize(12 * scale);
        hpTextLabel.setColor(ColorRGBA.White);
        playerStatsContainer.addChild(hpTextLabel);

        manaBar = new ProgressBar();
        manaBar.setPreferredSize(new Vector3f(180 * scale, 18 * scale, 0));
        manaBar.setProgressPercent(1.0f);
        manaBar.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0.2f, 0.8f)));
        Panel manaIndicator = manaBar.getValueIndicator();
        if (manaIndicator != null) manaIndicator.setBackground(new QuadBackgroundComponent(ColorRGBA.Blue));
        playerStatsContainer.addChild(manaBar);

        manaTextLabel = new Label("50/50");
        manaTextLabel.setFontSize(12 * scale);
        manaTextLabel.setColor(ColorRGBA.White);
        playerStatsContainer.addChild(manaTextLabel);

        lastHealth = -1;
        lastMaxHealth = -1;
        lastMana = -1;
        lastMaxMana = -1;
        lastName = "";
    }

    public void updatePlayerStats() {
        if (playerManager == null || playerStatsContainer == null) return;
        float hp = playerManager.getHealth();
        float maxHp = playerManager.getMaxHealth();
        float mana = playerManager.getMana();
        float maxMana = playerManager.getMaxMana();
        String name = playerManager.getPlayerName();

        if (hp != lastHealth || maxHp != lastMaxHealth) {
            hpBar.setProgressPercent(hp / maxHp);
            hpTextLabel.setText((int) hp + "/" + (int) maxHp);
            lastHealth = hp;
            lastMaxHealth = maxHp;
        }
        if (mana != lastMana || maxMana != lastMaxMana) {
            manaBar.setProgressPercent(mana / maxMana);
            manaTextLabel.setText((int) mana + "/" + (int) maxMana);
            lastMana = mana;
            lastMaxMana = maxMana;
        }
        if (!name.equals(lastName)) {
            playerNameLabel.setText(name);
            lastName = name;
        }
    }

    public void updatePotionCounts() {
        if (playerManager == null) return;
        if (hpCountLabel != null) hpCountLabel.setText(String.valueOf(playerManager.getHealthPotions()));
        if (mpCountLabel != null) mpCountLabel.setText(String.valueOf(playerManager.getManaPotions()));
    }

    public void updateHUDPosition(boolean force) {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        if (hudBackground != null) {
            hudBackground.setLocalTranslation(0, bottomOffset * scale, 0);
            Quad quad = (Quad) ((Geometry) hudBackground).getMesh();
            quad.updateGeometry(screenWidth, hudHeight * scale);
        }

        float yPos = bottomOffset * scale + 10 * scale;
        float xPos = 20 * scale;
        float buttonSizeScaled = buttonSize * scale;
        float spacingScaled = buttonSpacing * scale;

        for (int i = 0; i < 2 && i < hudButtons.size(); i++) {
            Button btn = hudButtons.get(i);
            btn.setLocalTranslation(xPos, yPos, 0);
            xPos += buttonSizeScaled + spacingScaled;
        }
        xPos += buttonSizeScaled * 0.5f;
        for (int i = 2; i < 6 && i < hudButtons.size(); i++) {
            Button btn = hudButtons.get(i);
            btn.setLocalTranslation(xPos, yPos, 0);
            xPos += buttonSizeScaled + spacingScaled;
        }
        xPos += buttonSizeScaled * 0.5f;
        if (hudButtons.size() > 6) {
            Button btn = hudButtons.get(6);
            btn.setLocalTranslation(xPos, yPos, 0);
        }

        if (talentButton != null) {
            talentButton.setLocalTranslation(screenWidth - buttonSizeScaled - 10 * scale, screenHeight - buttonSizeScaled - 10 * scale, 0);
        }

        if (playerStatsContainer != null) {
            playerStatsContainer.setLocalTranslation(10 * scale, screenHeight - 110 * scale, 0);
        }
    }

    public void showHUD() {
        if (hudNode == null) return;
        attachNode(hudNode);
        hudVisible = true;
        for (Button btn : hudButtons) btn.setCullHint(Node.CullHint.Dynamic);
        if (hudBackground != null) hudBackground.setCullHint(Node.CullHint.Dynamic);
        if (talentButton != null) talentButton.setCullHint(Node.CullHint.Dynamic);
        if (playerStatsContainer != null) playerStatsContainer.setCullHint(Node.CullHint.Dynamic);
        updateHUDPosition(true);
        updatePotionCounts();
        updatePlayerStats();
    }

    public void hideHUD() {
        detachNode(hudNode);
        hudVisible = false;
        for (Button btn : hudButtons) btn.setCullHint(Node.CullHint.Always);
        if (hudBackground != null) hudBackground.setCullHint(Node.CullHint.Always);
        if (talentButton != null) talentButton.setCullHint(Node.CullHint.Always);
        if (playerStatsContainer != null) playerStatsContainer.setCullHint(Node.CullHint.Always);
    }

    // ===== ОКНО ЛОГИНА =====
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

    Label title = new Label("Login");
    title.setFontSize(30 * scale);
    title.setColor(ColorRGBA.White);
    title.setLocalTranslation(20 * scale, winH - 35 * scale, 0.1f);
    loginWindow.attachChild(title);

    float labelY1 = winH - 85 * scale;
    Label loginLabel = new Label("Login:");
    loginLabel.setFontSize(18 * scale);
    loginLabel.setColor(ColorRGBA.White);
    loginLabel.setLocalTranslation(20 * scale, labelY1, 0.1f);
    loginWindow.attachChild(loginLabel);

    loginField = new TextField("");
    // Уменьшаем высоту до 26 * scale
    loginField.setPreferredSize(new Vector3f(220 * scale, 26 * scale, 0));
    loginField.setColor(ColorRGBA.Black);
    loginField.setFontSize(18 * scale);
    loginField.setLocalTranslation(130 * scale, labelY1 - 8 * scale, 0.1f);
    loginField.setSize(loginField.getPreferredSize());
    loginWindow.attachChild(loginField);

    float labelY2 = winH - 140 * scale;
    Label passLabel = new Label("Password:");
    passLabel.setFontSize(18 * scale);
    passLabel.setColor(ColorRGBA.White);
    passLabel.setLocalTranslation(20 * scale, labelY2, 0.1f);
    loginWindow.attachChild(passLabel);

    passwordField = new TextField("");
    // Уменьшаем высоту до 26 * scale
    passwordField.setPreferredSize(new Vector3f(220 * scale, 26 * scale, 0));
    passwordField.setColor(ColorRGBA.Black);
    passwordField.setFontSize(18 * scale);
    passwordField.setLocalTranslation(130 * scale, labelY2 - 8 * scale, 0.1f);
    passwordField.setSize(passwordField.getPreferredSize());
    loginWindow.attachChild(passwordField);

    Button loginButton = new Button("Login");
    loginButton.setPreferredSize(new Vector3f(110 * scale, 30 * scale, 0));
    loginButton.setColor(ColorRGBA.White);
    loginButton.setFontSize(18 * scale);
    loginButton.setLocalTranslation(85 * scale, 45 * scale, 0.1f);
    loginButton.addClickCommands((source) -> {
        if (loginVisible) {
            String login = loginField.getText();
            String pass = passwordField.getText();
            if (!login.isEmpty() && !pass.isEmpty()) handleLogin(login, pass);
        }
    });
    loginWindow.attachChild(loginButton);

    Button registerButton = new Button("Register");
    registerButton.setPreferredSize(new Vector3f(110 * scale, 30 * scale, 0));
    registerButton.setColor(ColorRGBA.White);
    registerButton.setFontSize(18 * scale);
    registerButton.setLocalTranslation(220 * scale, 45 * scale, 0.1f);
    registerButton.addClickCommands((source) -> {
        if (loginVisible) showRegisterScreen();
    });
    loginWindow.attachChild(registerButton);

    loginFields = new TextField[]{loginField, passwordField};

    guiNode.attachChild(loginWindow);
    loginWindow.setCullHint(Node.CullHint.Always);
}

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

    Label title = new Label("Registration");
    title.setFontSize(30 * scale);
    title.setColor(ColorRGBA.White);
    title.setLocalTranslation(20 * scale, winH - 35 * scale, 0.1f);
    registerWindow.attachChild(title);

    float labelY1 = winH - 85 * scale;
    Label emailLabel = new Label("Email:");
    emailLabel.setFontSize(18 * scale);
    emailLabel.setColor(ColorRGBA.White);
    emailLabel.setLocalTranslation(20 * scale, labelY1, 0.1f);
    registerWindow.attachChild(emailLabel);

    emailField = new TextField("");
    // Уменьшаем высоту до 26 * scale
    emailField.setPreferredSize(new Vector3f(240 * scale, 26 * scale, 0));
    emailField.setColor(ColorRGBA.Black);
    emailField.setFontSize(18 * scale);
    emailField.setLocalTranslation(150 * scale, labelY1 - 8 * scale, 0.1f);
    emailField.setSize(emailField.getPreferredSize());
    registerWindow.attachChild(emailField);

    float labelY2 = winH - 140 * scale;
    Label regLoginLabel = new Label("Login:");
    regLoginLabel.setFontSize(18 * scale);
    regLoginLabel.setColor(ColorRGBA.White);
    regLoginLabel.setLocalTranslation(20 * scale, labelY2, 0.1f);
    registerWindow.attachChild(regLoginLabel);

    regLoginField = new TextField("");
    // Уменьшаем высоту до 26 * scale
    regLoginField.setPreferredSize(new Vector3f(240 * scale, 26 * scale, 0));
    regLoginField.setColor(ColorRGBA.Black);
    regLoginField.setFontSize(18 * scale);
    regLoginField.setLocalTranslation(150 * scale, labelY2 - 8 * scale, 0.1f);
    regLoginField.setSize(regLoginField.getPreferredSize());
    registerWindow.attachChild(regLoginField);

    float labelY3 = winH - 195 * scale;
    Label regPassLabel = new Label("Password:");
    regPassLabel.setFontSize(18 * scale);
    regPassLabel.setColor(ColorRGBA.White);
    regPassLabel.setLocalTranslation(20 * scale, labelY3, 0.1f);
    registerWindow.attachChild(regPassLabel);

    regPasswordField = new TextField("");
    // Уменьшаем высоту до 26 * scale
    regPasswordField.setPreferredSize(new Vector3f(240 * scale, 26 * scale, 0));
    regPasswordField.setColor(ColorRGBA.Black);
    regPasswordField.setFontSize(18 * scale);
    regPasswordField.setLocalTranslation(150 * scale, labelY3 - 8 * scale, 0.1f);
    regPasswordField.setSize(regPasswordField.getPreferredSize());
    registerWindow.attachChild(regPasswordField);

    Button registerButton = new Button("Register");
    registerButton.setPreferredSize(new Vector3f(130 * scale, 30 * scale, 0));
    registerButton.setColor(ColorRGBA.White);
    registerButton.setFontSize(18 * scale);
    registerButton.setLocalTranslation(100 * scale, 70 * scale, 0.1f);
    registerButton.addClickCommands((source) -> {
        if (registerVisible) {
            String email = emailField.getText();
            String login = regLoginField.getText();
            String pass = regPasswordField.getText();
            if (!email.isEmpty() && !login.isEmpty() && !pass.isEmpty()) handleRegister(email, login, pass);
        }
    });
    registerWindow.attachChild(registerButton);

    Button backButton = new Button("Back");
    backButton.setPreferredSize(new Vector3f(110 * scale, 30 * scale, 0));
    backButton.setColor(ColorRGBA.White);
    backButton.setFontSize(18 * scale);
    backButton.setLocalTranslation(260 * scale, 70 * scale, 0.1f);
    backButton.addClickCommands((source) -> showLoginScreen());
    registerWindow.attachChild(backButton);

    registerFields = new TextField[]{emailField, regLoginField, regPasswordField};

    guiNode.attachChild(registerWindow);
    registerWindow.setCullHint(Node.CullHint.Always);
}

    // ===== ПОКАЗ И СКРЫТИЕ ОКОН =====
    public void showLoginScreen() {
        if (loginWindow == null) return;
        hideRegisterScreen();
        updateLoginPosition();

        loginWindow.setCullHint(Node.CullHint.Dynamic);
        loginVisible = true;
        registerVisible = false;
        showBackground();

        app.enqueue(() -> {
            if (loginFields != null && loginFields.length > 0) {
                GuiGlobals.getInstance().requestFocus(loginFields[0]);
                System.out.println("[UI] Focus set to login field");
            }
        });
    }

    public void hideLoginScreen() {
        if (loginVisible) {
            loginWindow.setCullHint(Node.CullHint.Always);
            loginVisible = false;
            GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
        }
    }

    public void showRegisterScreen() {
        if (registerWindow == null) return;
        hideLoginScreen();
        updateRegisterPosition();

        registerWindow.setCullHint(Node.CullHint.Dynamic);
        registerVisible = true;
        loginVisible = false;

        app.enqueue(() -> {
            if (registerFields != null && registerFields.length > 0) {
                GuiGlobals.getInstance().requestFocus(registerFields[0]);
                System.out.println("[UI] Focus set to register field");
            }
        });
    }

    public void hideRegisterScreen() {
        if (registerVisible) {
            registerWindow.setCullHint(Node.CullHint.Always);
            registerVisible = false;
            GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
        }
    }

    // ===== ДИАЛОГ ТЕЛЕПОРТА =====
    private Container teleporterDialog;
    private boolean teleporterDialogVisible = false;

    public void showTeleporterDialog() {
        if (teleporterDialog == null) createTeleporterDialog();
        if (teleporterDialogVisible) return;
        teleporterDialogVisible = true;
        attachNode(teleporterDialog);
    }

    public void hideTeleporterDialog() {
        if (teleporterDialog != null && teleporterDialogVisible) {
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

        Label question = new Label("Teleport to dungeon?");
        question.setFontSize(22 * scale);
        question.setColor(ColorRGBA.White);
        question.setLocalTranslation(winW/2 - 120*scale, winH - 40*scale, 0.1f);
        teleporterDialog.attachChild(question);

        Button yesButton = new Button("Yes");
        yesButton.setPreferredSize(new Vector3f(80*scale, 30*scale, 0));
        yesButton.setFontSize(18*scale);
        yesButton.setColor(ColorRGBA.White);
        yesButton.setLocalTranslation(60*scale, 30*scale, 0.1f);
        yesButton.addClickCommands((source) -> {
            hideTeleporterDialog();
            Main main = (Main) app;
            if (main != null) {
                WorldManager wm = main.getWorldManager();
                if (wm != null) wm.teleportToDungeon();
            }
        });
        teleporterDialog.attachChild(yesButton);

        Button noButton = new Button("No");
        noButton.setPreferredSize(new Vector3f(80*scale, 30*scale, 0));
        noButton.setFontSize(18*scale);
        noButton.setColor(ColorRGBA.White);
        noButton.setLocalTranslation(180*scale, 30*scale, 0.1f);
        noButton.addClickCommands((source) -> hideTeleporterDialog());
        teleporterDialog.attachChild(noButton);
    }

    // ===== ОБНОВЛЕНИЕ ПОЗИЦИИ ОКОН =====
    private void updateLoginPosition() {
        if (loginWindow == null) return;
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float winW = 450 * scale;
        float winH = 300 * scale;
        float x = (screenWidth - winW) / 2;
        float y = (screenHeight - winH) / 2;
        if (y < 0) y = 0;
        loginWindow.setLocalTranslation(x, y, 0);
    }

    private void updateRegisterPosition() {
        if (registerWindow == null) return;
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float winW = 480 * scale;
        float winH = 420 * scale;
        float x = (screenWidth - winW) / 2;
        float y = (screenHeight - winH) / 2;
        if (y < 0) y = 0;
        registerWindow.setLocalTranslation(x, y, 0);
    }

    // ===== ОСТАЛЬНЫЕ МЕТОДЫ =====
    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
        updatePlayerStats();
    }

    public void setInventoryManager(InventoryManager im) {
        this.inventoryManager = im;
        if (im != null) im.setUIManager(this);
    }

    public void setTalentManager(TalentManager tm) {
        this.talentManager = tm;
        if (tm != null) createWindows(true);
    }

    private void createWindows(boolean force) {
        if (talentManager == null) {
            System.err.println("[UIManager] Cannot create windows: talentManager is null");
            return;
        }
        if (force || talentWindow == null) talentWindow = new TalentWindow(app, talentManager, this);
        if (force || traderWindow == null) traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
        if (force || auctionWindow == null) auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
    }

    public void onStateChanged(GameState newState) {
        if (newState == GameState.LOGIN) {
            showLoginScreen();
            hideHUD();
        } else if (newState == GameState.CITY || newState == GameState.DUNGEON) {
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
        return loginVisible || registerVisible || teleporterDialogVisible ||
                (inventoryManager != null && inventoryManager.isVisible()) ||
                (talentWindow != null && talentWindow.isVisible()) ||
                (traderWindow != null && traderWindow.isVisible()) ||
                (auctionWindow != null && auctionWindow.isVisible());
    }

    public void update(float tpf) {
        if (hudVisible) {
            updateHUDPosition(false);
            updatePlayerStats();
        }
        if (talentWindow != null) talentWindow.update(tpf);
    }

    public void onResize(int width, int height) {
        updateScale();
        if (hudVisible) {
            updateHUDPosition(true);
            if (playerStatsContainer != null) {
                float screenHeight = app.getCamera().getHeight();
                playerStatsContainer.setLocalTranslation(10 * scale, screenHeight - 110 * scale, 0);
            }
        }
        if (loginVisible) showLoginScreen();
        if (registerVisible) showRegisterScreen();
        if (inventoryManager != null) inventoryManager.updateLayout(width, height);
        if (talentWindow != null) talentWindow.updateLayout(width, height);
        if (traderWindow != null) traderWindow.updateLayout(width, height);
        if (auctionWindow != null) auctionWindow.updateLayout(width, height);
        if (backgroundNode != null && backgroundNode.getCullHint() == Node.CullHint.Never) {
            updateBackgroundScale();
        }
    }

    public void cleanup() {
        if (authRawListener != null) {
            app.getInputManager().removeRawInputListener(authRawListener);
        }
        detachNode(hudNode);
        if (inventoryManager != null) inventoryManager.cleanup();
        if (talentWindow != null) talentWindow.hide();
        if (traderWindow != null) traderWindow.hide();
        if (auctionWindow != null) auctionWindow.hide();
    }

    public Node getGuiNode() { return guiNode; }

    // ===== NETWORK HANDLERS =====
    private void handleLogin(String login, String password) {
        System.out.println("[UI] Попытка входа: " + login);
        if (networkManager == null) {
            System.err.println("[UI] NetworkManager не инициализирован, используем локальный вход.");
            loadTestCharacter();
            Main main = (Main) app;
            if (main != null) main.loadGameWorld();
            return;
        }

        networkManager.login(login, password).thenAccept(success -> {
            app.enqueue(() -> {
                if (success) {
                    System.out.println("[UI] Вход выполнен успешно.");
                    loadCharacterFromServer();
                } else {
                    showLoginError("Invalid login or password.");
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> showLoginError("Network error: " + ex.getMessage()));
            return null;
        });
    }

    private void handleRegister(String email, String login, String password) {
        System.out.println("[UI] Регистрация: " + login);
        if (networkManager == null) {
            System.err.println("[UI] NetworkManager не инициализирован.");
            loadTestCharacter();
            Main main = (Main) app;
            if (main != null) main.loadGameWorld();
            return;
        }

        networkManager.register(email, login, password).thenAccept(success -> {
            app.enqueue(() -> {
                if (success) {
                    System.out.println("[UI] Регистрация успешна. Переход к логину.");
                    showLoginScreen();
                    showToast("Registration successful! Please login.");
                } else {
                    showLoginError("Registration failed. Please try again.");
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> showLoginError("Network error: " + ex.getMessage()));
            return null;
        });
    }

    private void loadCharacterFromServer() {
        if (networkManager == null) return;
        networkManager.loadCharacterData().thenAccept(data -> {
            app.enqueue(() -> {
                if (data != null) {
                    System.out.println("[UI] Данные персонажа загружены с сервера.");
                    applyCharacterData(data);
                    Main main = (Main) app;
                    if (main != null) {
                        main.loadGameWorld();
                        main.getGameManager().setState(GameState.CITY);
                    }
                } else {
                    showLoginError("Failed to load character data.");
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> showLoginError("Network error: " + ex.getMessage()));
            return null;
        });
    }

    public void applyCharacterData(Map<String, Object> data) {
        if (playerManager == null) return;
        System.out.println("[UI] applyCharacterData: data keys = " + data.keySet());

        if (data.containsKey("name")) {
            playerManager.setPlayerName((String) data.get("name"));
        }
        if (data.containsKey("gold")) {
            playerManager.setGold(((Number) data.get("gold")).intValue());
        }
        if (data.containsKey("healthPotions")) {
            playerManager.setHealthPotions(((Number) data.get("healthPotions")).intValue());
        }
        if (data.containsKey("manaPotions")) {
            playerManager.setManaPotions(((Number) data.get("manaPotions")).intValue());
        }
        if (data.containsKey("health")) {
            playerManager.setHealth(((Number) data.get("health")).intValue());
        }
        if (data.containsKey("maxHealth")) {
            playerManager.setMaxHealth(((Number) data.get("maxHealth")).intValue());
        }
        if (data.containsKey("mana")) {
            playerManager.setMana(((Number) data.get("mana")).intValue());
        }
        if (data.containsKey("maxMana")) {
            playerManager.setMaxMana(((Number) data.get("maxMana")).intValue());
        }
        if (data.containsKey("level")) {
            playerManager.setLevel(((Number) data.get("level")).intValue());
        }
        if (data.containsKey("experience")) {
            playerManager.setExperience(((Number) data.get("experience")).intValue());
        }

        if (data.containsKey("inventory")) {
            List<Map<String, Object>> invList = (List<Map<String, Object>>) data.get("inventory");
            if (inventoryManager != null) inventoryManager.loadFromServerData(invList);
        }

        updatePlayerStats();
        updatePotionCounts();
    }
    


    private void showLoginError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setFontSize(16 * scale);
        errorLabel.setColor(ColorRGBA.Red);
        errorLabel.setLocalTranslation(20 * scale, 250 * scale, 0.1f);
        loginWindow.attachChild(errorLabel);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            app.enqueue(() -> {
                if (loginWindow.hasChild(errorLabel)) loginWindow.detachChild(errorLabel);
            });
        }).start();
    }

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.setFontSize(18 * scale);
        toast.setColor(ColorRGBA.Green);
        toast.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.9f)));
        toast.setPreferredSize(new Vector3f(400 * scale, 40 * scale, 0));
        toast.setLocalTranslation((app.getCamera().getWidth() - 400 * scale) / 2, app.getCamera().getHeight() - 100 * scale, 0.1f);
        guiNode.attachChild(toast);
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            app.enqueue(() -> guiNode.detachChild(toast));
        }).start();
    }

    private void loadTestCharacter() {
        Main main = (Main) app;
        if (main == null) return;
        PlayerManager pm = main.getPlayerManager();
        if (pm == null) return;
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
        InventoryManager im = main.getInventoryManager();
        if (im != null) {
            im.addItem(ItemGenerator.generateItem(1, "Weapon", 1));
            im.addItem(ItemGenerator.generateItem(1, "Helmet", 1));
            im.addItem(ItemGenerator.generateItem(1, "Chest", 1));
            im.addItem(ItemGenerator.generateItem(1, "Legs", 1));
            im.addItem(ItemGenerator.generateItem(1, "Boots", 1));
        }
        GameManager gm = main.getGameManager();
        if (gm != null) gm.setState(GameState.CITY);
        updatePlayerStats();
        updatePotionCounts();
    }
}