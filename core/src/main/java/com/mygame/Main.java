package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
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
import java.awt.*;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.CollisionResult;
import com.jme3.math.Ray;
import com.jme3.renderer.RenderManager;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.Container;

public class Main extends SimpleApplication {
    private String getPath(Spatial s) {
        StringBuilder sb = new StringBuilder();
        while (s != null) {
            sb.insert(0, "/" + s.getName());
            s = s.getParent();
        }
        return sb.toString();
    }

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

    // ===== КАМЕРА =====
    private float camDistance = 12f;
    private float camHeight = 8f;
    private float cameraAngle = 0f;
    private boolean isRotatingCamera = false;
    private float lastMouseX = 0;
    private float mouseSensitivity = 0.005f;

    public static void main(String[] args) {
        // 1. Базовые настройки
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Exorcist");
        settings.setWindowSize(800, 600);
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setResizable(true);
        settings.setFullscreen(false);
        settings.setVSync(false);
        settings.setSamples(4);
        settings.setUseInput(true);
        settings.setRenderer("LWJGL3");

        // 2. Создаём приложение
        Main app = new Main();
        app.setSettings(settings);
        app.setShowSettings(false);

        // 3. Запускаем в отдельном потоке
        Thread gameThread = new Thread(() -> app.start());
        gameThread.start();

        // 4. Ждём и меняем размер через AWT
        try {
            Thread.sleep(500);
            java.awt.EventQueue.invokeAndWait(() -> {
                java.awt.Frame[] frames = java.awt.Frame.getFrames();
                for (java.awt.Frame f : frames) {
                    if (f.getTitle().equals("Exorcist")) {
                        f.setSize(800, 600);
                        f.setLocationRelativeTo(null);
                        f.setResizable(true);
                        System.out.println("[Main] Window resized to 800x600 via AWT");
                        break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setResolution(int width, int height, boolean fullscreen) {
        AppSettings settings = getContext().getSettings();
        settings.setResolution(width, height);
        settings.setWidth(width);
        settings.setHeight(height);
        settings.setFullscreen(fullscreen);
        if (fullscreen) {
            settings.setResizable(false);
        } else {
            settings.setResizable(true);
        }
        setSettings(settings);
        restart();
    }

    @Override
    public void simpleInitApp() {

        setDisplayFps(false);
        setDisplayStatView(false);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        setResolution(width, height, false);
        instance = this;
 SoundManager.initialize(this);
        viewPort.setBackgroundColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));

        // ===== ФИЗИКА =====
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

        // Инициализация менеджеров
        initializeManagers();
if (playerManager != null && playerManager.getPlayerNode() != null) {
            cameraControl = new CameraFollowControl(cam, playerManager.getPlayerNode());
            playerManager.getPlayerNode().addControl(cameraControl);
        }

        if (playerManager != null) {
            playerManager.setPhysicsSpace(bulletAppState.getPhysicsSpace());
        }
        
        if (worldManager != null) {
            worldManager.setBulletAppState(bulletAppState);
        }

        if (uiManager != null && playerManager != null) {
            uiManager.setPlayerManager(playerManager);
        }
        if (uiManager != null && inventoryManager != null) {
            uiManager.setInventoryManager(inventoryManager);
        }
        if (dropManager != null && inventoryManager != null) {
            dropManager.setInventoryManager(inventoryManager);
        }
        if (playerManager != null) {
            playerManager.setWorldManager(worldManager);
            playerManager.setDropManager(dropManager);
        }

        if (uiManager != null) {
            uiManager.forceShowLogin();
        }

        gameManager.setState(GameState.LOGIN);

        // ===== УПРАВЛЕНИЕ =====
        inputManager.addMapping("MouseClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && "MouseClick".equals(name) && playerManager != null) {
                    if (uiManager != null && uiManager.isAnyWindowOpen()) {
                        return;
                    }
                    Vector2f cursor = inputManager.getCursorPosition();
                    handleClick(cursor.x, cursor.y);
                }
            }
        }, "MouseClick");

        inputManager.addMapping("RightClick", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if ("RightClick".equals(name)) {
                    isRotatingCamera = isPressed;
                    if (isPressed) {
                        lastMouseX = inputManager.getCursorPosition().x;
                    }
                }
            }
        }, "RightClick");

        inputManager.addRawInputListener(new RawInputListener() {
            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onMouseMotionEvent(MouseMotionEvent evt) {
                if (!worldLoaded || !isRotatingCamera) return;
                float currentX = inputManager.getCursorPosition().x;
                float deltaX = currentX - lastMouseX;
                cameraAngle += deltaX * mouseSensitivity;
                lastMouseX = currentX;
            }
            @Override public void onMouseButtonEvent(MouseButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}
            @Override public void onTouchEvent(TouchEvent evt) {}
        });

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

        Monster.setApp(this);

        playerManager.setUIManager(uiManager);
        playerManager.setNetworkManager(networkManager);

        // ===== ГЛОБАЛЬНЫЙ ОБРАБОТЧИК КЛИКОВ (отладка) =====
        inputManager.addRawInputListener(new RawInputListener() {
            @Override
            public void beginInput() {}
            @Override
            public void endInput() {}
            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override
            public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {}
            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
                if (evt.getButtonIndex() == 0 && evt.isPressed()) {
                    Vector2f click2d = new Vector2f(evt.getX(), evt.getY());
                    Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f);
                    Vector3f dir = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtract(click3d).normalizeLocal();
                    Ray ray = new Ray(click3d, dir);

                    CollisionResults results = new CollisionResults();
                    guiNode.collideWith(ray, results);

                    System.out.println("=== GLOBAL CLICK DEBUG ===");
                    System.out.println("Click coordinates: (" + evt.getX() + ", " + evt.getY() + ")");
                    if (results.size() > 0) {
                        for (CollisionResult res : results) {
                            Spatial target = res.getGeometry();
                            System.out.println("  Hit spatial: " + target);
                            System.out.println("    Name: " + target.getName());
                            System.out.println("    Class: " + target.getClass().getSimpleName());
                            Spatial parent = target;
                            while (parent != null) {
                                if (parent instanceof Button) {
                                    System.out.println("    -> This is a Button! Text: " + ((Button) parent).getText());
                                    break;
                                } else if (parent instanceof Panel) {
                                    System.out.println("    -> This is a Panel");
                                    break;
                                } else if (parent instanceof Container) {
                                    System.out.println("    -> This is a Container");
                                    break;
                                } else if (parent instanceof Label) {
                                    System.out.println("    -> This is a Label");
                                    break;
                                }
                                parent = parent.getParent();
                            }
                            System.out.println("    Full path: " + getPath(target));
                        }
                    } else {
                        System.out.println("  No GUI elements under cursor.");
                        CollisionResults worldResults = new CollisionResults();
                        rootNode.collideWith(ray, worldResults);
                        if (worldResults.size() > 0) {
                            System.out.println("  But hit a 3D object: " + worldResults.getClosestCollision().getGeometry());
                        }
                    }
                    System.out.println("=============================");
                }
            }
            @Override
            public void onKeyEvent(KeyInputEvent evt) {}
            @Override
            public void onTouchEvent(TouchEvent evt) {}
        });
    }

    // ===== ОБНОВЛЕНИЕ КАМЕРЫ =====
    private void updateCamera() {
        if (!worldLoaded || gameManager == null) return;
        GameState state = gameManager.getCurrentState();
        if (state != GameState.CITY && state != GameState.DUNGEON) return;
        if (playerManager == null) return;

        Node playerNode = playerManager.getPlayerNode();
        if (playerNode == null) return;

        Vector3f playerPos = playerNode.getWorldTranslation();
        if (playerPos == null) return;

        float distance = 18f;
        float height = 22f;
        float x = FastMath.sin(cameraAngle) * distance;
        float z = FastMath.cos(cameraAngle) * distance;
        Vector3f camPos = playerPos.add(new Vector3f(x, height, z));

        // Плавное следование камеры
        Vector3f currentCamPos = cam.getLocation();
        camPos.interpolateLocal(currentCamPos, 0.15f);

        cam.setLocation(camPos);
        Vector3f lookTarget = playerPos.add(new Vector3f(0, -0.5f, 0));
        cam.lookAt(lookTarget, Vector3f.UNIT_Y);
    }

@Override
public void simpleRender(RenderManager rm) {
    super.simpleRender(rm);
    updateCamera();
}

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
                        if (tw != null && tw.isVisible()) {
                            tw.updateUI();
                        }
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
        System.out.println("[Main] Физика включена и настроена");

        worldManager.loadCityWithPhysics();

        if (playerManager != null) {
            Node playerNode = playerManager.getPlayerNode();
            if (playerNode != null && !rootNode.hasChild(playerNode)) {
                rootNode.attachChild(playerNode);
                System.out.println("[Main] Персонаж добавлен в rootNode");
            }

            Vector3f spawnPos = new Vector3f(0f, 0.5f, -8f);
            playerManager.setPosition(spawnPos);
            if (playerManager.getCharacterControl() != null) {
                playerManager.getCharacterControl().warp(spawnPos);
                playerManager.getCharacterControl().setWalkDirection(Vector3f.ZERO);
            }
            System.out.println("[Main] Персонаж на позиции: " + spawnPos);
        }

        gameManager.setState(GameState.CITY);
        worldManager.switchToCity();

        worldLoaded = true;
        System.out.println("[Main] ===== МИР ЗАГРУЖЕН =====");

        loadTalentsFromServer();
    }

    private void handleClick(float screenX, float screenY) {
        if (!worldLoaded || playerManager == null) return;

        DropManager.DropItem drop = dropManager.getDropAt(screenX, screenY);
        if (drop != null) {
            dropManager.pickupDrop(drop);
            return;
        }

        Vector3f groundPoint = getGroundPoint(screenX, screenY);
        if (groundPoint == null) return;

        // Телепортер
        Spatial teleporter = null;
        if (worldManager.getCityNode() != null) {
            for (Spatial child : worldManager.getCityNode().getChildren()) {
                if (child.getName() != null && child.getName().equals("Teleporter")) {
                    float dist = child.getWorldTranslation().distance(groundPoint);
                    if (dist < 1.5f) {
                        teleporter = child;
                        break;
                    }
                }
            }
        }
        if (teleporter != null) {
            if (uiManager != null) {
                uiManager.showTeleporterDialog();
            }
            return;
        }

        // Торговец
        Spatial npc = null;
        if (worldManager.getNpcNode() != null) {
            for (Spatial child : worldManager.getNpcNode().getChildren()) {
                if (child.getName() != null && child.getName().equals("NPC_Trader")) {
                    float dist = child.getWorldTranslation().distance(groundPoint);
                    if (dist < 1.5f) {
                        npc = child;
                        break;
                    }
                }
            }
        }
        if (npc != null) {
            if (uiManager != null) uiManager.openTrader();
            return;
        }

        // Аукционер
        Spatial auctioneer = null;
        if (worldManager.getNpcNode() != null) {
            for (Spatial child : worldManager.getNpcNode().getChildren()) {
                if (child.getName() != null && child.getName().equals("NPC_Auctioneer")) {
                    float dist = child.getWorldTranslation().distance(groundPoint);
                    if (dist < 1.5f) {
                        auctioneer = child;
                        break;
                    }
                }
            }
        }
        if (auctioneer != null) {
            if (uiManager != null) {
                uiManager.openAuction();
            }
            return;
        }

        // Монстры
        Spatial clicked = worldManager.getClosestInteractiveObject(groundPoint, 5.5f);
        if (clicked != null) {
            String objectName = clicked.getName();
            if ("TestMonster".equals(objectName) || "Monster".equals(objectName)) {
                playerManager.attackTarget(clicked);
                return;
            }
        }

        // Движение
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

    // ---------- ОСВЕЩЕНИЕ, GUI, МЕНЕДЖЕРЫ ----------
    private void setupLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1, -2, -1).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.2f));
        rootNode.addLight(sun);

        DirectionalLight fillLight = new DirectionalLight();
        fillLight.setDirection(new Vector3f(1, -1, 1).normalizeLocal());
        fillLight.setColor(new ColorRGBA(0.6f, 0.6f, 0.7f, 1f).mult(0.8f));
        rootNode.addLight(fillLight);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f));
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

    private void initializeManagers() {
        if (isInitialized) return;
        networkManager = new NetworkManager(this);
        networkManager.initialize();

        gameManager = new GameManager(this);
        gameManager.initialize();
        playerManager = new PlayerManager(this);
        playerManager.initialize();
        worldManager = new WorldManager(this);
        worldManager.initialize();
        uiManager = new UIManager(this);
        uiManager.initialize();
        inventoryManager = new InventoryManager(this, guiNode);
        dropManager = new DropManager(this, guiNode);

        uiManager.setPlayerManager(playerManager);
        uiManager.setInventoryManager(inventoryManager);

        TalentManager talentManager = new TalentManager(playerManager, networkManager);
        uiManager.setTalentManager(talentManager);
        playerManager.setTalentManager(talentManager);

        worldManager.setNetworkManager(networkManager);
        worldManager.setPlayerManager(playerManager);
        worldManager.setDropManager(dropManager);
        playerManager.setWorldManager(worldManager);
        playerManager.setDropManager(dropManager);
        dropManager.setInventoryManager(inventoryManager);

        gameManager.setNetworkManager(networkManager);
        gameManager.setPlayerManager(playerManager);
        gameManager.setWorldManager(worldManager);
        gameManager.setUIManager(uiManager);

        isInitialized = true;
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        if (gameManager != null) gameManager.update(tpf);
        if (playerManager != null) playerManager.update(tpf);
        if (worldManager != null) worldManager.update(tpf);
        if (uiManager != null) uiManager.update(tpf);
    }

    @Override
    public void destroy() {
        SoundManager.cleanup();
        super.destroy();
    }

    @Override
    public void reshape(int w, int h) {
        super.reshape(w, h);
        if (uiManager != null) uiManager.onResize(w, h);
        if (inventoryManager != null) inventoryManager.updateLayout(w, h);
        if (uiManager != null) {
            TalentWindow tw = uiManager.getTalentWindow();
            if (tw != null) tw.updateLayout(w, h);
            TraderWindow trw = uiManager.getTraderWindow();
            if (trw != null) trw.updateLayout(w, h);
            AuctionWindow aw = uiManager.getAuctionWindow();
            if (aw != null) aw.updateLayout(w, h);
        }
    }
    private CameraFollowControl cameraControl; 

    public static Main getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public UIManager getUIManager() { return uiManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public DropManager getDropManager() { return dropManager; }
    public boolean isWorldLoaded() { return worldLoaded; }
}