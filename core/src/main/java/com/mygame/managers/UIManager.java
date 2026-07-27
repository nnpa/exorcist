package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.material.Material;
import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;
import com.mygame.Main;
import com.mygame.managers.GameManager.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIManager {

    private SimpleApplication app;
    private Node guiNode;

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
    private Button skill1Btn, skill2Btn, skill3Btn, skill4Btn; // только 4 скилла
    private Map<Button, Geometry> iconGeoms = new HashMap<>();

    private float hudHeight = 90;
    private float buttonSize = 70;
    private float buttonSpacing = 8;
    private float bottomOffset = 20;

    private TextField loginField;
    private TextField passwordField;
    private TextField emailField;
    private TextField regLoginField;
    private TextField regPasswordField;

    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private TalentWindow talentWindow;
    private TraderWindow traderWindow;

    private VirtualKeyboard keyboard;

    private float scale = 1f;

    private static final String KEY_SKILL1 = "skill1";
    private static final String KEY_SKILL2 = "skill2";
    private static final String KEY_SKILL3 = "skill3";
    private static final String KEY_SKILL4 = "skill4";
    private static final String KEY_HEALTH_POTION = "healthPotion";
    private static final String KEY_MANA_POTION = "manaPotion";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_TALENTS = "talents";

    public UIManager(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();
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

    // ===== ИНИЦИАЛИЗАЦИЯ =====
    public void initialize() {
        keyboard = new VirtualKeyboard(app, guiNode);
        keyboard.hide();

        createLoginScreen();
        createRegisterScreen();
        createHUD();

        hudNode = new Node("HUDNode");
        hudNode.setName("HUDNode");
        hudNode.attachChild(hudBackground);
        for (Button btn : hudButtons) {
            hudNode.attachChild(btn);
        }
        hudNode.attachChild(talentButton);

        hideAllWindows();
        hideHUD();

        setupKeyboardShortcuts();

        Main main = (Main) app;
        if (main != null) {
            PlayerManager pm = main.getPlayerManager();
            if (pm != null) {
                talentWindow = new TalentWindow(app, pm.getTalentManager(), this);
            }
        }
    }

    public void forceShowLogin() {
        showLoginScreen();
    }

    private void setupKeyboardShortcuts() {
        app.getInputManager().addMapping(KEY_SKILL1, new KeyTrigger(com.jme3.input.KeyInput.KEY_1));
        app.getInputManager().addMapping(KEY_SKILL2, new KeyTrigger(com.jme3.input.KeyInput.KEY_2));
        app.getInputManager().addMapping(KEY_SKILL3, new KeyTrigger(com.jme3.input.KeyInput.KEY_3));
        app.getInputManager().addMapping(KEY_SKILL4, new KeyTrigger(com.jme3.input.KeyInput.KEY_4));
        app.getInputManager().addMapping(KEY_HEALTH_POTION, new KeyTrigger(com.jme3.input.KeyInput.KEY_Q));
        app.getInputManager().addMapping(KEY_MANA_POTION, new KeyTrigger(com.jme3.input.KeyInput.KEY_W));
        app.getInputManager().addMapping(KEY_INVENTORY, new KeyTrigger(com.jme3.input.KeyInput.KEY_I));
        app.getInputManager().addMapping(KEY_TALENTS, new KeyTrigger(com.jme3.input.KeyInput.KEY_T));

        ActionListener listener = new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return;
                if (!hudVisible) return;
                if (isAnyWindowOpen()) return;

                switch (name) {
                    case KEY_SKILL1:
                        if (playerManager != null) {
                            playerManager.castSkill("Heal");
                            flashButton(skill1Btn);
                        }
                        break;
                    case KEY_SKILL2:
                        if (playerManager != null) {
                            playerManager.castSkill("ShieldBash");
                            flashButton(skill2Btn);
                        }
                        break;
                    case KEY_SKILL3:
                        if (playerManager != null) {
                            playerManager.castSkill("Whirlwind");
                            flashButton(skill3Btn);
                        }
                        break;
                    case KEY_SKILL4:
                        if (playerManager != null) {
                            playerManager.castSkill("Kick");
                            flashButton(skill4Btn);
                        }
                        break;
                    case KEY_HEALTH_POTION:
                        if (playerManager != null) {
                            playerManager.useHealthPotion();
                            updatePotionCounts();
                            flashButton(healthPotionBtn);
                        }
                        break;
                    case KEY_MANA_POTION:
                        if (playerManager != null) {
                            playerManager.useManaPotion();
                            updatePotionCounts();
                            flashButton(manaPotionBtn);
                        }
                        break;
                    case KEY_INVENTORY:
                        if (inventoryManager != null) {
                            inventoryManager.toggleVisibility();
                            flashButton(inventoryButton);
                        }
                        break;
                    case KEY_TALENTS:
                        if (talentWindow != null) {
                            talentWindow.toggle();
                            flashButton(talentButton);
                        }
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

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
        if (pm != null && inventoryManager != null) {
            traderWindow = new TraderWindow(app, pm, inventoryManager, this);
        }
    }

    public void setInventoryManager(InventoryManager im) {
        this.inventoryManager = im;
        if (im != null) {
            im.setUIManager(this);
        }
        if (im != null && playerManager != null) {
            traderWindow = new TraderWindow(app, playerManager, im, this);
        }
    }

    private Button createIconOnlyButton(String iconPath, float size) {
        Button btn = new Button("");
        btn.setPreferredSize(new Vector3f(size, size, 0));
        btn.setBackground(null);
        btn.setColor(ColorRGBA.White);
        try {
            com.jme3.texture.Texture tex = app.getAssetManager().loadTexture(iconPath);
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
        mat.setColor("Color", new ColorRGBA(0.0f, 0.0f, 0.0f, 0.5f));
        hudBackground.setMaterial(mat);

        // Зелья (2 кнопки)
        healthPotionBtn = createIconOnlyButton("Interface/Icons/hp.png", buttonSizeScaled);
        healthPotionBtn.addClickCommands((source) -> {
            if (playerManager != null) {
                playerManager.useHealthPotion();
                updatePotionCounts();
                flashButton(healthPotionBtn);
            }
        });
        hudButtons.add(healthPotionBtn);
        hpCountLabel = new Label("0");
        hpCountLabel.setFontSize(14 * scale);
        hpCountLabel.setColor(ColorRGBA.White);
        hpCountLabel.setLocalTranslation(buttonSizeScaled - 25 * scale, 10 * scale, 0.1f);
        healthPotionBtn.attachChild(hpCountLabel);

        manaPotionBtn = createIconOnlyButton("Interface/Icons/mp.png", buttonSizeScaled);
        manaPotionBtn.addClickCommands((source) -> {
            if (playerManager != null) {
                playerManager.useManaPotion();
                updatePotionCounts();
                flashButton(manaPotionBtn);
            }
        });
        hudButtons.add(manaPotionBtn);
        mpCountLabel = new Label("0");
        mpCountLabel.setFontSize(14 * scale);
        mpCountLabel.setColor(ColorRGBA.White);
        mpCountLabel.setLocalTranslation(buttonSizeScaled - 25 * scale, 10 * scale, 0.1f);
        manaPotionBtn.attachChild(mpCountLabel);

        // Скиллы (4 кнопки)
        skill1Btn = createIconOnlyButton("Interface/Icons/light.png", buttonSizeScaled);
        skill1Btn.addClickCommands((source) -> {
            if (playerManager != null) {
                playerManager.castSkill("Heal");
                flashButton(skill1Btn);
            }
        });
        hudButtons.add(skill1Btn);

        skill2Btn = createIconOnlyButton("Interface/Icons/shield.png", buttonSizeScaled);
        skill2Btn.addClickCommands((source) -> {
            if (playerManager != null) {
                playerManager.castSkill("ShieldBash");
                flashButton(skill2Btn);
            }
        });
        hudButtons.add(skill2Btn);

        skill3Btn = createIconOnlyButton("Interface/Icons/whirlwind.png", buttonSizeScaled);
        skill3Btn.addClickCommands((source) -> {
            if (playerManager != null) {
                playerManager.castSkill("Whirlwind");
                flashButton(skill3Btn);
            }
        });
        hudButtons.add(skill3Btn);

        skill4Btn = createIconOnlyButton("Interface/Icons/kick.png", buttonSizeScaled);
        skill4Btn.addClickCommands((source) -> {
            if (playerManager != null) {
                playerManager.castSkill("Kick");
                flashButton(skill4Btn);
            }
        });
        hudButtons.add(skill4Btn);

        // Инвентарь
        inventoryButton = createIconOnlyButton("Interface/Icons/backpack.png", buttonSizeScaled);
        inventoryButton.addClickCommands((source) -> {
            if (inventoryManager != null) {
                inventoryManager.toggleVisibility();
                flashButton(inventoryButton);
            }
        });
        hudButtons.add(inventoryButton);

        // Таланты (отдельная кнопка)
        talentButton = new Button("T");
        talentButton.setPreferredSize(new Vector3f(buttonSizeScaled, buttonSizeScaled, 0));
        talentButton.setBackground(null);
        talentButton.setColor(ColorRGBA.White);
        talentButton.setFontSize(24 * scale);
        talentButton.addClickCommands((source) -> {
            if (talentWindow != null) {
                talentWindow.toggle();
                flashButton(talentButton);
            }
        });
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

        // Зелья (2 кнопки)
        for (int i = 0; i < 2 && i < hudButtons.size(); i++) {
            Button btn = hudButtons.get(i);
            btn.setLocalTranslation(xPos, yPos, 0);
            xPos += buttonSizeScaled + spacingScaled;
        }
        xPos += buttonSizeScaled * 0.5f;

        // Скиллы (4 кнопки, индексы 2-5)
        for (int i = 2; i < 6 && i < hudButtons.size(); i++) {
            Button btn = hudButtons.get(i);
            btn.setLocalTranslation(xPos, yPos, 0);
            xPos += buttonSizeScaled + spacingScaled;
        }
        xPos += buttonSizeScaled * 0.5f;

        // Инвентарь (индекс 6)
        if (hudButtons.size() > 6) {
            Button btn = hudButtons.get(6);
            btn.setLocalTranslation(xPos, yPos, 0);
        }

        // Таланты (правый верхний угол)
        if (talentButton != null) {
            talentButton.setLocalTranslation(screenWidth - buttonSizeScaled - 10 * scale, screenHeight - buttonSizeScaled - 10 * scale, 0);
        }
    }

    public void showHUD() {
        if (hudNode == null) return;
        attachNode(hudNode);
        hudVisible = true;
        for (Button btn : hudButtons) {
            btn.setCullHint(Node.CullHint.Dynamic);
        }
        if (hudBackground != null) hudBackground.setCullHint(Node.CullHint.Dynamic);
        if (talentButton != null) talentButton.setCullHint(Node.CullHint.Dynamic);
        updateHUDPosition(true);
        updatePotionCounts();
    }

    public void hideHUD() {
        detachNode(hudNode);
        hudVisible = false;
        for (Button btn : hudButtons) {
            btn.setCullHint(Node.CullHint.Always);
        }
        if (hudBackground != null) hudBackground.setCullHint(Node.CullHint.Always);
        if (talentButton != null) talentButton.setCullHint(Node.CullHint.Always);
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

        Quad bgQuad = new Quad(winW, winH);
        Geometry bgGeom = new Geometry("LoginBg", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f));
        bgGeom.setMaterial(bgMat);
        bgGeom.setLocalTranslation(0, 0, -0.1f);
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
        loginField.setPreferredSize(new Vector3f(220 * scale, 38 * scale, 0));
        loginField.setColor(ColorRGBA.Black);
        loginField.setFontSize(18 * scale);
        loginField.setLocalTranslation(130 * scale, labelY1 - 8 * scale, 0.1f);
        loginField.setSize(loginField.getPreferredSize());
        MouseEventControl.addListenersToSpatial(loginField, new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent event, Spatial spatial, Spatial target) {
                if (event.isPressed() && loginVisible) {
                    if (keyboard != null) keyboard.show(loginField);
                }
            }
            @Override public void mouseEntered(MouseMotionEvent event, Spatial spatial, Spatial target) {}
            @Override public void mouseExited(MouseMotionEvent event, Spatial spatial, Spatial target) {}
            @Override public void mouseMoved(MouseMotionEvent event, Spatial spatial, Spatial target) {}
        });
        loginWindow.attachChild(loginField);

        float labelY2 = winH - 140 * scale;
        Label passLabel = new Label("Password:");
        passLabel.setFontSize(18 * scale);
        passLabel.setColor(ColorRGBA.White);
        passLabel.setLocalTranslation(20 * scale, labelY2, 0.1f);
        loginWindow.attachChild(passLabel);

        passwordField = new TextField("");
        passwordField.setPreferredSize(new Vector3f(220 * scale, 38 * scale, 0));
        passwordField.setColor(ColorRGBA.Black);
        passwordField.setFontSize(18 * scale);
        passwordField.setLocalTranslation(130 * scale, labelY2 - 8 * scale, 0.1f);
        passwordField.setSize(passwordField.getPreferredSize());
        MouseEventControl.addListenersToSpatial(passwordField, new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent event, Spatial spatial, Spatial target) {
                if (event.isPressed() && loginVisible) {
                    if (keyboard != null) keyboard.show(passwordField);
                }
            }
            @Override public void mouseEntered(MouseMotionEvent event, Spatial spatial, Spatial target) {}
            @Override public void mouseExited(MouseMotionEvent event, Spatial spatial, Spatial target) {}
            @Override public void mouseMoved(MouseMotionEvent event, Spatial spatial, Spatial target) {}
        });
        loginWindow.attachChild(passwordField);

        Button loginButton = new Button("Login");
        loginButton.setPreferredSize(new Vector3f(110 * scale, 35 * scale, 0));
        loginButton.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.5f, 0.8f, 0.9f)));
        loginButton.setColor(ColorRGBA.White);
        loginButton.setFontSize(18 * scale);
        loginButton.setLocalTranslation(85 * scale, 45 * scale, 0.1f);
        loginButton.addClickCommands((source) -> {
            if (loginVisible) {
                String login = loginField.getText();
                String pass = passwordField.getText();
                if (!login.isEmpty() && !pass.isEmpty()) {
                    handleLogin(login, pass);
                }
            }
        });
        loginWindow.attachChild(loginButton);

        Button registerButton = new Button("Register");
        registerButton.setPreferredSize(new Vector3f(110 * scale, 35 * scale, 0));
        registerButton.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.5f, 0.2f, 0.9f)));
        registerButton.setColor(ColorRGBA.White);
        registerButton.setFontSize(18 * scale);
        registerButton.setLocalTranslation(220 * scale, 45 * scale, 0.1f);
        registerButton.addClickCommands((source) -> {
            if (loginVisible) showRegisterScreen();
        });
        loginWindow.attachChild(registerButton);
    }

    private void createRegisterScreen() {
        // полный код регистрации (аналогичен предыдущей версии)
        updateScale();
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float winW = 480 * scale;
        float winH = 380 * scale;

        registerWindow = new Container();
        registerWindow.setPreferredSize(new Vector3f(winW, winH, 0));
        registerWindow.setLayout(null);
        registerWindow.setName("RegisterWindow");

        float x = (screenWidth - winW) / 2;
        float y = (screenHeight - winH) / 2;
        if (y < 0) y = 0;
        registerWindow.setLocalTranslation(x, y, 0);

        Quad bgQuad = new Quad(winW, winH);
        Geometry bgGeom = new Geometry("RegisterBg", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f));
        bgGeom.setMaterial(bgMat);
        bgGeom.setLocalTranslation(0, 0, -0.1f);
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
        emailField.setPreferredSize(new Vector3f(240 * scale, 38 * scale, 0));
        emailField.setColor(ColorRGBA.Black);
        emailField.setFontSize(18 * scale);
        emailField.setLocalTranslation(150 * scale, labelY1 - 8 * scale, 0.1f);
        registerWindow.attachChild(emailField);

        float labelY2 = winH - 140 * scale;
        Label regLoginLabel = new Label("Login:");
        regLoginLabel.setFontSize(18 * scale);
        regLoginLabel.setColor(ColorRGBA.White);
        regLoginLabel.setLocalTranslation(20 * scale, labelY2, 0.1f);
        registerWindow.attachChild(regLoginLabel);

        regLoginField = new TextField("");
        regLoginField.setPreferredSize(new Vector3f(240 * scale, 38 * scale, 0));
        regLoginField.setColor(ColorRGBA.Black);
        regLoginField.setFontSize(18 * scale);
        regLoginField.setLocalTranslation(150 * scale, labelY2 - 8 * scale, 0.1f);
        registerWindow.attachChild(regLoginField);

        float labelY3 = winH - 195 * scale;
        Label regPassLabel = new Label("Password:");
        regPassLabel.setFontSize(18 * scale);
        regPassLabel.setColor(ColorRGBA.White);
        regPassLabel.setLocalTranslation(20 * scale, labelY3, 0.1f);
        registerWindow.attachChild(regPassLabel);

        regPasswordField = new TextField("");
        regPasswordField.setPreferredSize(new Vector3f(240 * scale, 38 * scale, 0));
        regPasswordField.setColor(ColorRGBA.Black);
        regPasswordField.setFontSize(18 * scale);
        regPasswordField.setLocalTranslation(150 * scale, labelY3 - 8 * scale, 0.1f);
        registerWindow.attachChild(regPasswordField);

        Button registerButton = new Button("Register");
        registerButton.setPreferredSize(new Vector3f(130 * scale, 35 * scale, 0));
        registerButton.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.5f, 0.2f, 0.9f)));
        registerButton.setColor(ColorRGBA.White);
        registerButton.setFontSize(18 * scale);
        registerButton.setLocalTranslation(110 * scale, 85 * scale, 0.1f);
        registerButton.addClickCommands((source) -> {
            if (registerVisible) {
                String email = emailField.getText();
                String login = regLoginField.getText();
                String pass = regPasswordField.getText();
                if (!email.isEmpty() && !login.isEmpty() && !pass.isEmpty()) {
                    handleRegister(email, login, pass);
                }
            }
        });
        registerWindow.attachChild(registerButton);

        Button backButton = new Button("Back");
        backButton.setPreferredSize(new Vector3f(110 * scale, 35 * scale, 0));
        backButton.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.9f)));
        backButton.setColor(ColorRGBA.White);
        backButton.setFontSize(18 * scale);
        backButton.setLocalTranslation(260 * scale, 85 * scale, 0.1f);
        backButton.addClickCommands((source) -> {
            if (keyboard != null) keyboard.hide();
            showLoginScreen();
        });
        registerWindow.attachChild(backButton);
    }

    public void showLoginScreen() {
        if (loginWindow == null) return;
        hideRegisterScreen();
        detachNode(loginWindow);
        attachNode(loginWindow);
        loginVisible = true;
        registerVisible = false;
        if (keyboard != null) keyboard.hide();
    }

    public void hideLoginScreen() {
        detachNode(loginWindow);
        loginVisible = false;
        if (keyboard != null) keyboard.hide();
    }

    public void showRegisterScreen() {
        if (registerWindow == null) return;
        hideLoginScreen();
        detachNode(registerWindow);
        attachNode(registerWindow);
        registerVisible = true;
        loginVisible = false;
        if (keyboard != null) keyboard.hide();
    }

    public void hideRegisterScreen() {
        detachNode(registerWindow);
        registerVisible = false;
        if (keyboard != null) keyboard.hide();
    }

    public void hideAllWindows() {
        hideLoginScreen();
        hideRegisterScreen();
        if (inventoryManager != null && inventoryManager.isVisible()) inventoryManager.hide();
        if (talentWindow != null && talentWindow.isVisible()) talentWindow.hide();
        if (traderWindow != null && traderWindow.isVisible()) traderWindow.hide();
        if (keyboard != null) keyboard.hide();
    }

    public boolean isAnyWindowOpen() {
        if (keyboard != null && keyboard.isVisible()) return true;
        if (loginVisible || registerVisible) return true;
        if (inventoryManager != null && inventoryManager.isVisible()) return true;
        if (talentWindow != null && talentWindow.isVisible()) return true;
        if (traderWindow != null && traderWindow.isVisible()) return true;
        return false;
    }

    private void handleLogin(String login, String password) {
        System.out.println("[UI] Login: " + login);
        loadTestCharacter();
    }

    private void handleRegister(String email, String login, String password) {
        System.out.println("[UI] Register: " + login);
        loadTestCharacter();
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
            im.addItem(new InventoryManager.Item("Old Sword", "Weapon", 1, ColorRGBA.White, "Simple sword"));
            im.addItem(new InventoryManager.Item("Leather Helmet", "Helmet", 1, ColorRGBA.White, "Light helmet"));
        }

        GameManager gm = main.getGameManager();
        if (gm != null) {
            gm.setState(GameState.CITY);
        }
    }

    public void onStateChanged(GameState newState) {
        if (newState == GameState.LOGIN) {
            showLoginScreen();
            hideHUD();
        } else if (newState == GameState.CITY || newState == GameState.DUNGEON) {
            hideLoginScreen();
            hideRegisterScreen();
            if (keyboard != null) keyboard.hide();
            showHUD();
        } else {
            hideHUD();
        }
    }

    public void openTrader() {
        if (traderWindow != null) traderWindow.show();
    }

    public void closeTrader() {
        if (traderWindow != null) traderWindow.hide();
    }

    public TalentWindow getTalentWindow() { return talentWindow; }
    public TraderWindow getTraderWindow() { return traderWindow; }

    public void update(float tpf) {
        if (hudVisible) updateHUDPosition(false);
        if (talentWindow != null) talentWindow.update(tpf);
    }

    public void onResize(int width, int height) {
        updateScale();
        if (hudVisible) updateHUDPosition(true);
        if (loginVisible) showLoginScreen();
        if (registerVisible) showRegisterScreen();
        if (keyboard != null) keyboard.rebuild();
        if (inventoryManager != null) inventoryManager.updateLayout(width, height);
        if (talentWindow != null) talentWindow.updateLayout(width, height);
        if (traderWindow != null) traderWindow.updateLayout(width, height);
    }

    public void cleanup() {
        if (keyboard != null) {
            keyboard.hide();
            detachNode(keyboard.getContainer());
        }
        detachNode(hudNode);
        detachNode(loginWindow);
        detachNode(registerWindow);
        if (inventoryManager != null) inventoryManager.cleanup();
        if (talentWindow != null) talentWindow.hide();
        if (traderWindow != null) traderWindow.hide();
    }
}