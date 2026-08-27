package com.mygame;

import com.atr.jme.font.TrueTypeFont;
import com.atr.jme.font.asset.TrueTypeKey;
import com.atr.jme.font.asset.TrueTypeKeyBMP;
import com.atr.jme.font.asset.TrueTypeLoader;
import com.atr.jme.font.util.Style;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.renderer.RenderManager;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;
import com.mygame.managers.*;
import com.mygame.managers.GameManager.GameState;
import com.mygame.monsters.Monster;
import com.simsilica.lemur.Label;

import java.util.Map;

public class Main extends SimpleApplication {

    private static Main instance;
    private GameManager gameManager;
    private NetworkManager networkManager;
    private UIManager uiManager;
    private WorldManager worldManager;
    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private DropManager dropManager;
    private boolean isInitialized = false;
    private boolean worldLoaded = false;
    private BulletAppState bulletAppState;

    // Камера
    private CameraFollowControl cameraControl;
    private boolean isRotating = false;
    private float lastMouseX = 0;

    @Override
    public void simpleInitApp() {
        instance = this;
        setDisplayFps(false);
        setDisplayStatView(false);
        viewPort.setBackgroundColor(ColorRGBA.Black);

        SoundManager.initialize(this);

        bulletAppState = new BulletAppState();
        bulletAppState.setEnabled(false);
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.SEQUENTIAL);
        stateManager.attach(bulletAppState);

        setupLighting();

        GuiGlobals.initialize(this);
        applySkinStyle();
        applyTextFieldStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        initializeManagers();

        // Настройка камеры
        if (playerManager != null && playerManager.getPlayerNode() != null) {
            cameraControl = new CameraFollowControl(cam, playerManager.getPlayerNode());
            playerManager.getPlayerNode().addControl(cameraControl);
            // Устанавливаем начальный угол (по желанию)
            cameraControl.setCameraAngle(0f);
        }

        if (playerManager != null) {
            playerManager.setPhysicsSpace(bulletAppState.getPhysicsSpace());
        }

        if (worldManager != null) {
            worldManager.setBulletAppState(bulletAppState);
        }

        if (uiManager != null && playerManager != null) uiManager.setPlayerManager(playerManager);
        if (uiManager != null && inventoryManager != null) uiManager.setInventoryManager(inventoryManager);
        if (dropManager != null && inventoryManager != null) dropManager.setInventoryManager(inventoryManager);
        if (playerManager != null) {
            playerManager.setWorldManager(worldManager);
            playerManager.setDropManager(dropManager);
        }

        setupInput();

        Monster.setApp(this);
        playerManager.setUIManager(uiManager);
        playerManager.setNetworkManager(networkManager);

        if (uiManager != null) {
            uiManager.forceShowLogin();
        }
    }

    private void setupInput() {
        // Левый клик – взаимодействие
        inputManager.addMapping("MouseClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && "MouseClick".equals(name) && playerManager != null) {
                    if (uiManager != null && uiManager.isAnyWindowOpen()) return;
                    Vector2f cursor = inputManager.getCursorPosition();
                    handleClick(cursor.x, cursor.y);
                }
            }
        }, "MouseClick");

        // Правая кнопка – вращение камеры
        inputManager.addMapping("RotateCamera", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if ("RotateCamera".equals(name)) {
                    isRotating = isPressed;
                    if (isPressed) {
                        lastMouseX = inputManager.getCursorPosition().x;
                    }
                }
            }
        }, "RotateCamera");

        // Слушатель движения мыши
        inputManager.addRawInputListener(new RawInputListener() {
            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onMouseMotionEvent(MouseMotionEvent evt) {
                if (isRotating && cameraControl != null) {
                    float currentX = inputManager.getCursorPosition().x;
                    float deltaX = currentX - lastMouseX;
                    cameraControl.rotate(deltaX * 0.005f);
                    lastMouseX = currentX;
                }
            }
            @Override public void onMouseButtonEvent(MouseButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}
            @Override public void onTouchEvent(TouchEvent evt) {}
        });

        // Клавиша N – возврат в город
        inputManager.addMapping("ReturnToCity", new KeyTrigger(com.jme3.input.KeyInput.KEY_N));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && "ReturnToCity".equals(name) && worldLoaded) {
                    if (gameManager != null && gameManager.getCurrentState() == GameState.DUNGEON) {
                        worldManager.returnToCity();
                        gameManager.setState(GameState.CITY);
                    }
                }
            }
        }, "ReturnToCity");
        
        inputManager.addMapping("PrintCoordinates", new KeyTrigger(com.jme3.input.KeyInput.KEY_Z));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && "PrintCoordinates".equals(name) && playerManager != null) {
                    Vector3f pos = playerManager.getPosition();
                    System.out.println("[Player] Position: x=" + pos.x + ", y=" + pos.y + ", z=" + pos.z);
                }
            }
        }, "PrintCoordinates");
        
        inputManager.addMapping("OpenEditor", new KeyTrigger(KeyInput.KEY_F12));
inputManager.addListener(new ActionListener() {
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed && worldLoaded && worldManager != null) {
            worldManager.openEditorForCurrentDungeon();
        }
    }
}, "OpenEditor");

 inputManager.addMapping("ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
    inputManager.addMapping("ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

    AnalogListener wheelListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (uiManager != null && uiManager.isAnyWindowOpen()) return;
            if (cameraControl == null) return;

            if ("ZoomIn".equals(name)) {
                cameraControl.zoom(-1.5f);
            } else if ("ZoomOut".equals(name)) {
                cameraControl.zoom(1.5f);
            }
        }
    };
    inputManager.addListener(wheelListener, "ZoomIn", "ZoomOut");

    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        if (gameManager != null) gameManager.update(tpf);
        if (playerManager != null) playerManager.update(tpf);
        if (worldManager != null) worldManager.update(tpf);
        if (uiManager != null) {
            uiManager.update(tpf);
            uiManager.updateMap(tpf); // только если окно открыто
         }
        // Камера обновляется автоматически через controlUpdate
    }
    private TrueTypeFont ttfFont;

    // ============================================================
    //                    МИР, КЛИКИ, ТАЛАНТЫ
    // ============================================================

    public void loadTalentsFromServer() {
        if (networkManager == null || uiManager == null) return;
        networkManager.loadTalents().thenAccept(data -> {
            if (data != null) {
                Map<String, Integer> talents = (Map<String, Integer>) data.get("talents");
                int points = (int) data.get("availablePoints");
                this.enqueue(() -> {
                    TalentManager tm = uiManager.getTalentManager();
                    TalentWindow tw = uiManager.getTalentWindow();
                    if (tm != null) {
                        tm.loadFromServer(talents, points);
                        if (tw != null && tw.isVisible()) tw.updateUI();
                    }
                    return null;
                });
            }
        }).exceptionally(ex -> {
            System.err.println("[Main] Failed to load talents: " + ex.getMessage());
            return null;
        });
    }

    public void loadGameWorld() {
        if (worldLoaded) return;
        System.out.println("[Main] ===== ЗАГРУЗКА МИРА =====");
        bulletAppState.setEnabled(true);
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        space.setAccuracy(0.001f);
        space.setMaxSubSteps(10);
        space.setGravity(new Vector3f(0, -30f, 0));
        bulletAppState.setSpeed(0.8f);
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.SEQUENTIAL);

        worldManager.loadCityWithPhysics();

        if (playerManager != null) {
            Node playerNode = playerManager.getPlayerNode();
            if (playerNode != null && !rootNode.hasChild(playerNode)) {
                rootNode.attachChild(playerNode);
            }
            Vector3f spawnPos = new Vector3f(0f, 0.5f, -8f);
            playerManager.setPosition(spawnPos);
            if (playerManager.getCharacterControl() != null) {
                playerManager.getCharacterControl().warp(spawnPos);
                playerManager.getCharacterControl().setWalkDirection(Vector3f.ZERO);
            }
        }
        gameManager.setState(GameState.CITY);
        worldManager.switchToCity();
        worldLoaded = true;
        System.out.println("[Main] ===== МИР ЗАГРУЖЕН =====");
        loadTalentsFromServer();
        if (worldManager != null && worldManager.getDungeonNode() != null) {
        uiManager.initMap(worldManager.getDungeonNode(), playerManager);
    }
    }
public void applyResolution(int width, int height) {
    AppSettings settings = new AppSettings(true);
    settings.setWidth(width);
    settings.setHeight(height);
    setSettings(settings);
    restart(); // перезапускает дисплей
}
    private void handleClick(float screenX, float screenY) {
        if (!worldLoaded || playerManager == null) return;
        DropManager.DropItem drop = dropManager.getDropAt(screenX, screenY);
        if (drop != null) { dropManager.pickupDrop(drop); return; }

        Vector3f groundPoint = getGroundPoint(screenX, screenY);
        if (groundPoint == null) return;

        Spatial teleporter = null;
        if (worldManager.getCityNode() != null) {
            for (Spatial child : worldManager.getCityNode().getChildren()) {
                if (child.getName() != null && child.getName().equals("Teleporter")) {
                    if (child.getWorldTranslation().distance(groundPoint) < 1.5f) { teleporter = child; break; }
                }
            }
        }
        if (teleporter != null) { if (uiManager != null) uiManager.showTeleporterDialog(); return; }

        Spatial npc = null;
        if (worldManager.getNpcNode() != null) {
            for (Spatial child : worldManager.getNpcNode().getChildren()) {
                if (child.getName() != null && child.getName().equals("NPC_Trader")) {
                    if (child.getWorldTranslation().distance(groundPoint) < 1.5f) { npc = child; break; }
                }
            }
        }
        if (npc != null) { if (uiManager != null) uiManager.openTrader(); return; }

        Spatial auctioneer = null;
        if (worldManager.getNpcNode() != null) {
            for (Spatial child : worldManager.getNpcNode().getChildren()) {
                if (child.getName() != null && child.getName().equals("NPC_Auctioneer")) {
                    if (child.getWorldTranslation().distance(groundPoint) < 1.5f) { auctioneer = child; break; }
                }
            }
        }
        if (auctioneer != null) { if (uiManager != null) uiManager.openAuction(); return; }

        Spatial clicked = worldManager.getClosestInteractiveObject(groundPoint, 5.5f);
        if (clicked != null) {
            String objectName = clicked.getName();
            if ("TestMonster".equals(objectName) || "Monster".equals(objectName)) {
                playerManager.attackTarget(clicked);
                return;
            }
        }
        playerManager.moveTo(groundPoint);
    }

    private Vector3f getGroundPoint(float screenX, float screenY) {
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 0f);
        Vector3f direction = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 1f).subtract(origin).normalizeLocal();
        if (direction.y == 0) return null;
        float t = -origin.y / direction.y;
        if (t < 0) return null;
        Vector3f hitPoint = origin.add(direction.mult(t));
        hitPoint.y = 0;
        return hitPoint;
    }

    // ============================================================
    //                    СВЕТ И СТИЛИ
    // ============================================================
private DirectionalLightShadowRenderer shadowRenderer;
private void setupLighting() {

    // ============================================================
    // ОСНОВНОЙ СВЕТ — СОЛНЦЕ
    // ============================================================

    DirectionalLight sun = new DirectionalLight();

    sun.setDirection(
            new Vector3f(-1f, -2f, -1f).normalizeLocal()
    );

    sun.setColor(
            ColorRGBA.White.mult(1.2f)
    );

    rootNode.addLight(sun);


    // ============================================================
    // SHADOW RENDERER
    // ============================================================

    shadowRenderer =
            new DirectionalLightShadowRenderer(
                    assetManager,
                    2048,
                    3
            );

    shadowRenderer.setLight(sun);

    shadowRenderer.setEdgeFilteringMode(
            EdgeFilteringMode.PCFPOISSON
    );

    shadowRenderer.setShadowZExtend(100f);

    viewPort.addProcessor(shadowRenderer);


    // ============================================================
    // ДОПОЛНИТЕЛЬНЫЙ СВЕТ
    // ============================================================

    DirectionalLight fillLight = new DirectionalLight();

    fillLight.setDirection(
            new Vector3f(1f, -1f, 1f).normalizeLocal()
    );

    fillLight.setColor(
            new ColorRGBA(
                    0.6f,
                    0.6f,
                    0.7f,
                    1f
            ).mult(0.8f)
    );

    rootNode.addLight(fillLight);


    // ============================================================
    // AMBIENT LIGHT
    // ============================================================

    AmbientLight ambient = new AmbientLight();

    ambient.setColor(
            new ColorRGBA(
                    0.6f,
                    0.6f,
                    0.6f,
                    1f
            )
    );

    rootNode.addLight(ambient);
}
    private void applyTextFieldStyle() {
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes attrs = styles.getSelector(TextField.ELEMENT_ID, null);
        attrs.set("background", new QuadBackgroundComponent(new ColorRGBA(0.25f, 0.25f, 0.35f, 0.9f)));
        attrs.set("color", ColorRGBA.Black);
        attrs.set("fontSize", 18f);
        attrs.set("insets", new Insets3f(2, 6, 2, 6));
    }

    private void applySkinStyle() {
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes labelAttrs = styles.getSelector(Label.ELEMENT_ID, null);
        labelAttrs.set("fontSize", 18f);
        labelAttrs.set("color", new ColorRGBA(0.9f, 0.7f, 0.4f, 1f));

        Attributes textFieldAttrs = styles.getSelector(TextField.ELEMENT_ID, null);
        textFieldAttrs.set("background", new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.1f, 0.03f, 0.9f)));
        textFieldAttrs.set("color", ColorRGBA.White);
        textFieldAttrs.set("fontSize", 18f);
    }

    // ============================================================
    //                    МЕНЕДЖЕРЫ
    // ============================================================
private MapRenderer mapRenderer;
private MapWindow mapWindow;
    private void initializeManagers() {

    if (isInitialized) {
        return;
    }

    System.out.println("[Main] ===== INITIALIZING MANAGERS =====");

    // ============================================================
    // NETWORK
    // ============================================================

    networkManager = new NetworkManager(this);
    networkManager.initialize();


    // ============================================================
    // GAME MANAGER
    // ============================================================

    gameManager = new GameManager(this);
    gameManager.initialize();


    // ============================================================
    // PLAYER MANAGER
    // ============================================================

    playerManager = new PlayerManager(this);
    playerManager.initialize();


    // ============================================================
    // WORLD MANAGER
    // ============================================================

    worldManager = new WorldManager(this);
    worldManager.initialize();


    // ============================================================
    // UI MANAGER
    // ============================================================

    uiManager = new UIManager(this);
    uiManager.initialize();


    // ============================================================
    // INVENTORY / DROP
    // ============================================================

    inventoryManager =
            new InventoryManager(this, guiNode);

    dropManager =
            new DropManager(this, guiNode);


    // ============================================================
    // UI -> MANAGERS
    // ============================================================

    uiManager.setPlayerManager(
            playerManager
    );

    uiManager.setInventoryManager(
            inventoryManager
    );


    // ============================================================
    // TALENTS
    // ============================================================

    TalentManager talentManager =
            new TalentManager(
                    playerManager,
                    networkManager
            );

    uiManager.setTalentManager(
            talentManager
    );

    playerManager.setTalentManager(
            talentManager
    );


    // ============================================================
    // WORLD MANAGER CONNECTIONS
    // ============================================================

    worldManager.setNetworkManager(
            networkManager
    );

    worldManager.setPlayerManager(
            playerManager
    );

    worldManager.setDropManager(
            dropManager
    );

    worldManager.setUIManager(
            uiManager
    );


    // ============================================================
    // PLAYER MANAGER CONNECTIONS
    // ============================================================

    playerManager.setWorldManager(
            worldManager
    );

    playerManager.setDropManager(
            dropManager
    );


    // ============================================================
    // DROP MANAGER
    // ============================================================

    dropManager.setInventoryManager(
            inventoryManager
    );


    // ============================================================
    // GAME MANAGER CONNECTIONS
    // ============================================================

    gameManager.setNetworkManager(
            networkManager
    );

    gameManager.setPlayerManager(
            playerManager
    );

    gameManager.setWorldManager(
            worldManager
    );

    gameManager.setUIManager(
            uiManager
    );


    // ============================================================
    // UI -> WORLD
    // ============================================================

    uiManager.setWorldManager(
            worldManager
    );


    // ============================================================
    // MINI MAP
    // ============================================================
    //
    // ВАЖНО:
    //
    // MapRenderer использует основной rootNode.
    // Он НЕ получает dungeonNode.
    //
    // Поэтому создание карты должно происходить
    // после создания WorldManager.
    //
    // ============================================================

    System.out.println(
            "[Main] Creating MapRenderer..."
    );

    mapRenderer =
            new MapRenderer(this);

    mapRenderer.initialize();

    System.out.println(
            "[Main] MapRenderer initialized."
    );


    System.out.println(
            "[Main] Creating MapWindow..."
    );

    mapWindow =
            new MapWindow(
                    this,
                    mapRenderer,
                    playerManager
            );

    System.out.println(
            "[Main] MapWindow created."
    );


    // ============================================================
    // INITIALIZED
    // ============================================================

    isInitialized = true;

    System.out.println(
            "[Main] ===== ALL MANAGERS INITIALIZED ====="
    );
}

    // ============================================================
    //                    ГЕТТЕРЫ
    // ============================================================

    public static Main getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public UIManager getUIManager() { return uiManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public DropManager getDropManager() { return dropManager; }
    public boolean isWorldLoaded() { return worldLoaded; }

    @Override
    public void destroy() {
        SoundManager.cleanup();
        super.destroy();
    }
}