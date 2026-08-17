package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.ActionListener;
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
import com.simsilica.lemur.style.Styles;
import com.mygame.Main;
import com.mygame.items.ItemGenerator;
import com.mygame.managers.GameManager.GameState;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.ProgressBar;

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

    public TalentManager getTalentManager() {
    return talentManager; // предполагается, что поле talentManager существует
}

public TalentWindow getTalentWindow() {
        if (talentWindow == null && talentManager != null) {
            createWindows(true);
        }
        return talentWindow;
    }

  public TraderWindow getTraderWindow() {
        if (traderWindow == null && playerManager != null && inventoryManager != null) {
            if (talentManager != null) {
                // Создать окна, но без TalentManager не нужно? traderWindow не зависит от talentManager, так что можно создать
                if (talentWindow == null) {
                    // но для traderWindow не нужен talentManager
                }
                traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
            }
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
  public void openAuction() {
        if (auctionWindow == null) {
            auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
        }
        auctionWindow.show();
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

    // HUD элементы (нижняя панель)
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

    // Элементы статистики игрока (левый верхний угол)
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

    private TextField loginField;
    private TextField passwordField;
    private TextField emailField;
    private TextField regLoginField;
    private TextField regPasswordField;

    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private TalentWindow talentWindow;
    private TraderWindow traderWindow;

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

    // ===== ФОНОВАЯ ГЕОМЕТРИЯ (ДЛЯ ВСЕХ ОКОН) =====
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
        bgGeom.setLocalTranslation(0, 0, -0.1f);
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

    hideAllWindows();
    hideHUD();

    setupKeyboardShortcuts();
 createBackground(); // создаст, но пока скрыт
    hideBackground();   // убедимся, что скрыт
    // Удаляем создание talentWindow здесь!
    // talentWindow = new TalentWindow(app, pm.getTalentManager(), this); // УДАЛИТЬ
    // talentManager = new TalentManager(playerManager, networkManager); // УДАЛИТЬ
}
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
        guiNode.attachChild(backgroundNode);
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
    guiNode.attachChild(backgroundNode);
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

    // ===== КЛАВИАТУРНЫЕ ЯРЛЫКИ =====
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

        updatePlayerStats();
    }

    public void setInventoryManager(InventoryManager im) {
        this.inventoryManager = im;
        if (im != null) {
            im.setUIManager(this);
        }

    }

    // ===== HUD =====
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
public void setTalentManager(TalentManager tm) {
    this.talentManager = tm;
    if (tm != null) {
        // Пересоздаём окна, даже если были созданы ранее
        createWindows(true); // можно передать флаг force
    }
}

 private void createWindows(boolean force) {
    if (talentManager == null) {
        System.err.println("[UIManager] Cannot create windows: talentManager is null");
        return;
    }
    // Пересоздаём всегда, если force == true
    if (force || talentWindow == null) {
        talentWindow = new TalentWindow(app, talentManager, this);
    }
    if (force || traderWindow == null) {
        traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
    }
    if (force || auctionWindow == null) {
        auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
    }
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
                updatePlayerStats();
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
                updatePlayerStats();
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

    // ===== СОЗДАНИЕ КОНТЕЙНЕРА С ИМЕНЕМ И БАРАМИ =====
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

        // Имя игрока
        playerNameLabel = new Label("Player");
        playerNameLabel.setFontSize(18 * scale);
        playerNameLabel.setColor(ColorRGBA.White);
        playerNameLabel.setLocalTranslation(5 * scale, 0, 0);
        playerStatsContainer.addChild(playerNameLabel);

        // HP бар (зелёный)
        hpBar = new ProgressBar();
        hpBar.setPreferredSize(new Vector3f(180 * scale, 18 * scale, 0));
        hpBar.setProgressPercent(1.0f);
        hpBar.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.0f, 0.0f, 0.8f)));
        Panel hpIndicator = hpBar.getValueIndicator();
        if (hpIndicator != null) {
            hpIndicator.setBackground(new QuadBackgroundComponent(ColorRGBA.Green));
        }
        playerStatsContainer.addChild(hpBar);

        hpTextLabel = new Label("100/100");
        hpTextLabel.setFontSize(12 * scale);
        hpTextLabel.setColor(ColorRGBA.White);
        hpTextLabel.setLocalTranslation(5 * scale, 0, 0);
        playerStatsContainer.addChild(hpTextLabel);

        // ===== MANA БАР (СИНИЙ) =====
        manaBar = new ProgressBar();
        manaBar.setPreferredSize(new Vector3f(180 * scale, 18 * scale, 0));
        manaBar.setProgressPercent(1.0f);
        manaBar.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.0f, 0.0f, 0.2f, 0.8f)));
        Panel manaIndicator = manaBar.getValueIndicator();
        if (manaIndicator != null) {
            manaIndicator.setBackground(new QuadBackgroundComponent(ColorRGBA.Blue));
        }
        playerStatsContainer.addChild(manaBar);

        manaTextLabel = new Label("50/50");
        manaTextLabel.setFontSize(12 * scale);
        manaTextLabel.setColor(ColorRGBA.White);
        manaTextLabel.setLocalTranslation(5 * scale, 0, 0);
        playerStatsContainer.addChild(manaTextLabel);

        lastHealth = -1;
        lastMaxHealth = -1;
        lastMana = -1;
        lastMaxMana = -1;
        lastName = "";
    }

    // ===== ОБНОВЛЕНИЕ СТАТИСТИКИ ИГРОКА =====
    public void updatePlayerStats() {
        if (playerManager == null) return;
        if (playerStatsContainer == null) return;

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

        // Позиция контейнера со статистикой
        if (playerStatsContainer != null) {
            playerStatsContainer.setLocalTranslation(10 * scale, screenHeight - 110 * scale, 0);
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
        if (playerStatsContainer != null) playerStatsContainer.setCullHint(Node.CullHint.Dynamic);
        updateHUDPosition(true);
        updatePotionCounts();
        updatePlayerStats();
    }

    public void hideHUD() {
        detachNode(hudNode);
        hudVisible = false;
        for (Button btn : hudButtons) {
            btn.setCullHint(Node.CullHint.Always);
        }
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

        // === ФОН ===
        Geometry bgGeom = createBackgroundGeometry(winW, winH);
        loginWindow.attachChild(bgGeom);

        // === ЭЛЕМЕНТЫ ===
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
        loginWindow.attachChild(passwordField);

        Button loginButton = new Button("Login");
        loginButton.setPreferredSize(new Vector3f(110 * scale, 35 * scale, 0));
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
        registerButton.setColor(ColorRGBA.White);
        registerButton.setFontSize(18 * scale);
        registerButton.setLocalTranslation(220 * scale, 45 * scale, 0.1f);
        registerButton.addClickCommands((source) -> {
            if (loginVisible) showRegisterScreen();
        });
        loginWindow.attachChild(registerButton);
    }

    // ===== ОКНО РЕГИСТРАЦИИ =====
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

        // === ФОН ===
        Geometry bgGeom = createBackgroundGeometry(winW, winH);
        registerWindow.attachChild(bgGeom);

        // === ЗАГОЛОВОК ===
        Label title = new Label("Registration");
        title.setFontSize(30 * scale);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(20 * scale, winH - 35 * scale, 0.1f);
        registerWindow.attachChild(title);

        // === ПОЛЕ EMAIL ===
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
        emailField.setSize(emailField.getPreferredSize());
        registerWindow.attachChild(emailField);

        // === ПОЛЕ LOGIN ===
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
        regLoginField.setSize(regLoginField.getPreferredSize());
        registerWindow.attachChild(regLoginField);

        // === ПОЛЕ PASSWORD ===
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
        regPasswordField.setSize(regPasswordField.getPreferredSize());
        registerWindow.attachChild(regPasswordField);

        // === КНОПКИ ===
        Button registerButton = new Button("Register");
        registerButton.setPreferredSize(new Vector3f(130 * scale, 35 * scale, 0));
        registerButton.setColor(ColorRGBA.White);
        registerButton.setFontSize(18 * scale);
        registerButton.setLocalTranslation(100 * scale, 70 * scale, 0.1f);
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
        backButton.setColor(ColorRGBA.White);
        backButton.setFontSize(18 * scale);
        backButton.setLocalTranslation(260 * scale, 70 * scale, 0.1f);
        backButton.addClickCommands((source) -> {
            showLoginScreen();
        });
        registerWindow.attachChild(backButton);
    }

    // ===== ДИАЛОГ ТЕЛЕПОРТА =====
    private Container teleporterDialog;
    private boolean teleporterDialogVisible = false;

    public void showTeleporterDialog() {
        if (teleporterDialog == null) {
            createTeleporterDialog();
        }
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

        // === ФОН ===
        Geometry bgGeom = createBackgroundGeometry(winW, winH);
        teleporterDialog.attachChild(bgGeom);

        // === Текст ===
        Label question = new Label("Teleport to dungeon?");
        question.setFontSize(22 * scale);
        question.setColor(ColorRGBA.White);
        question.setLocalTranslation(winW / 2 - 120 * scale, winH - 40 * scale, 0.1f);
        teleporterDialog.attachChild(question);

        // Кнопка Да
        Button yesButton = new Button("Yes");
        yesButton.setPreferredSize(new Vector3f(80 * scale, 30 * scale, 0));
        yesButton.setFontSize(18 * scale);
        yesButton.setColor(ColorRGBA.White);
        yesButton.setLocalTranslation(60 * scale, 30 * scale, 0.1f);
        yesButton.addClickCommands((source) -> {
            hideTeleporterDialog();
            Main main = (Main) app;
            if (main != null) {
                WorldManager wm = main.getWorldManager();
                if (wm != null) {
                    wm.teleportToDungeon();
                }
            }
        });
        teleporterDialog.attachChild(yesButton);

        // Кнопка Нет
        Button noButton = new Button("No");
        noButton.setPreferredSize(new Vector3f(80 * scale, 30 * scale, 0));
        noButton.setFontSize(18 * scale);
        noButton.setColor(ColorRGBA.White);
        noButton.setLocalTranslation(180 * scale, 30 * scale, 0.1f);
        noButton.addClickCommands((source) -> {
            hideTeleporterDialog();
        });
        teleporterDialog.attachChild(noButton);
    }

    // ===== УПРАВЛЕНИЕ ОКНАМИ =====
    public void showLoginScreen() {
        if (loginWindow == null) return;
        hideRegisterScreen();
            updateLoginPosition(); // <-- добавляем

        detachNode(loginWindow);
        attachNode(loginWindow);
        loginVisible = true;
        registerVisible = false;
            showBackground();

    }

    public void hideLoginScreen() {
        detachNode(loginWindow);
        loginVisible = false;
    }

    public void showRegisterScreen() {
        if (registerWindow == null) return;
        hideLoginScreen();
        updateRegisterPosition(); // <-- добавляем

        detachNode(registerWindow);
        attachNode(registerWindow);
        registerVisible = true;
        loginVisible = false;
    }

    public void hideRegisterScreen() {
        detachNode(registerWindow);
        registerVisible = false;
    }

    public void hideAllWindows() {
        hideLoginScreen();
        hideRegisterScreen();
        hideTeleporterDialog();
        if (inventoryManager != null && inventoryManager.isVisible()) inventoryManager.hide();
        if (talentWindow != null && talentWindow.isVisible()) talentWindow.hide();
        if (traderWindow != null && traderWindow.isVisible()) traderWindow.hide();
    }

    public boolean isAnyWindowOpen() {
        if (loginVisible || registerVisible || teleporterDialogVisible) return true;
        if (inventoryManager != null && inventoryManager.isVisible()) return true;
        if (talentWindow != null && talentWindow.isVisible()) return true;
        if (traderWindow != null && traderWindow.isVisible()) return true;
        return false;
    }

    // ================================================================
    //   СЕТЕВЫЕ ОБРАБОТЧИКИ
    // ================================================================

    private void handleLogin(String login, String password) {
        System.out.println("[UI] Попытка входа: " + login);

        if (networkManager == null) {
            System.err.println("[UI] NetworkManager не инициализирован, используем локальный вход.");
            loadTestCharacter();
            Main main = (Main) app;
            if (main != null) {
                main.loadGameWorld();
            }
            return;
        }

        networkManager.login(login, password).thenAccept(success -> {
            app.enqueue(() -> {
                if (success) {
                    System.out.println("[UI] Вход выполнен успешно.");
                    loadCharacterFromServer();
                } else {
                    System.out.println("[UI] Ошибка входа: неверный логин или пароль.");
                    showLoginError("Invalid login or password.");
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> {
                System.err.println("[UI] Ошибка сети: " + ex.getMessage());
                showLoginError("Network error: " + ex.getMessage());
            });
            return null;
        });
    }

    private void handleRegister(String email, String login, String password) {
        System.out.println("[UI] Регистрация: " + login);

        if (networkManager == null) {
            System.err.println("[UI] NetworkManager не инициализирован.");
            loadTestCharacter();
            Main main = (Main) app;
            if (main != null) {
                main.loadGameWorld();
            }
            return;
        }

        networkManager.register(email, login, password).thenAccept(success -> {
            app.enqueue(() -> {
                if (success) {
                    System.out.println("[UI] Регистрация успешна. Переход к логину.");
                    showLoginScreen();
                    showToast("Registration successful! Please login.");
                } else {
                    System.out.println("[UI] Ошибка регистрации.");
                    showLoginError("Registration failed. Please try again.");
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> {
                System.err.println("[UI] Ошибка сети: " + ex.getMessage());
                showLoginError("Network error: " + ex.getMessage());
            });
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
                    System.err.println("[UI] Не удалось загрузить персонажа.");
                    showLoginError("Failed to load character data.");
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> {
                System.err.println("[UI] Ошибка загрузки персонажа: " + ex.getMessage());
                showLoginError("Network error: " + ex.getMessage());
            });
            return null;
        });
    }

public void applyCharacterData(Map<String, Object> data) {
    if (playerManager == null) return;

    System.out.println("[UI] applyCharacterData: data keys = " + data.keySet());
    
    if (data.containsKey("name")) {
        String name = (String) data.get("name");
        playerManager.setPlayerName(name);
        System.out.println("[UI] Name loaded: " + name);
    }
    
    // ===== ЗОЛОТО (ДОБАВЛЕНО) =====
    if (data.containsKey("gold")) {
        int gold = ((Number) data.get("gold")).intValue();
        playerManager.setGold(gold);
        System.out.println("[UI] Gold loaded: " + gold);
    }

    // ===== ЗЕЛЬЯ =====
    if (data.containsKey("healthPotions")) {
        playerManager.setHealthPotions(((Number) data.get("healthPotions")).intValue());
    }
    if (data.containsKey("manaPotions")) {
        playerManager.setManaPotions(((Number) data.get("manaPotions")).intValue());
    }

    // ===== ИНВЕНТАРЬ =====
    if (data.containsKey("inventory")) {
        List<Map<String, Object>> invList = (List<Map<String, Object>>) data.get("inventory");
        System.out.println("[UI] Inventory list size = " + (invList != null ? invList.size() : "null"));
        if (inventoryManager != null) {
            inventoryManager.loadFromServerData(invList);
        }
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
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            app.enqueue(() -> {
                if (loginWindow.hasChild(errorLabel)) {
                    loginWindow.detachChild(errorLabel);
                }
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
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            app.enqueue(() -> guiNode.detachChild(toast));
        }).start();
    }

    // ===== ЛОКАЛЬНАЯ ЗАГРУЗКА (для офлайн-тестов) =====
   private void loadTestCharacter() {
    Main main = (Main) app;
    if (main == null) return;
    
    PlayerManager pm = main.getPlayerManager();
    if (pm == null) return;

    // Устанавливаем параметры персонажа
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

    // Добавляем стартовые предметы
    InventoryManager im = main.getInventoryManager();
    if (im != null) {
        im.addItem(ItemGenerator.generateItem(1, "Weapon", 1));
        im.addItem(ItemGenerator.generateItem(1, "Helmet", 1));
        im.addItem(ItemGenerator.generateItem(1, "Chest", 1));
        im.addItem(ItemGenerator.generateItem(1, "Legs", 1));
        im.addItem(ItemGenerator.generateItem(1, "Boots", 1));
    }

    GameManager gm = main.getGameManager();
    if (gm != null) {
        gm.setState(GameState.CITY);
    }

    updatePlayerStats();
    updatePotionCounts();
}

    // ===== ОСТАЛЬНЫЕ МЕТОДЫ =====
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
                    hideBackground(); // скрыть фон

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



    public void update(float tpf) {
        if (hudVisible) {
            updateHUDPosition(false);
            updatePlayerStats();
        }
        if (talentWindow != null) {
            talentWindow.update(tpf);
        }
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
        if (backgroundNode != null && backgroundNode.getCullHint() == Node.CullHint.Never) {
            updateBackgroundScale();
        }
    }

    public void cleanup() {
        detachNode(hudNode);
        detachNode(loginWindow);
        detachNode(registerWindow);
        if (inventoryManager != null) inventoryManager.cleanup();
        if (talentWindow != null) talentWindow.hide();
        if (traderWindow != null) traderWindow.hide();
    }

    public Node getGuiNode() { return guiNode; }
    
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

// Обновление позиции окна регистрации
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
}