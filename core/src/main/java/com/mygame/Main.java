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
import com.jme3.collision.CollisionResults;
import com.jme3.collision.CollisionResult;
import com.jme3.math.Ray;
import com.jme3.renderer.RenderManager;

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
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.Container;

import java.util.Map;
import java.awt.*;

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

    // ===== КАМЕРА =====
    private float cameraAngle = 0f;
    private boolean isRotatingCamera = false;
    private float lastMouseX = 0;
    private float mouseSensitivity = 0.005f;
    private CameraFollowControl cameraControl;

    @Override
    public void simpleInitApp() {
        instance = this;

        setDisplayFps(false);
        setDisplayStatView(false);
        viewPort.setBackgroundColor(ColorRGBA.Black);

        // ЗВУК
        SoundManager.initialize(this);

        // ФИЗИКА
        bulletAppState = new BulletAppState();
        bulletAppState.setEnabled(false);
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.SEQUENTIAL);
        stateManager.attach(bulletAppState);

        // ОСВЕЩЕНИЕ
        setupLighting();

        // LEMUR
        GuiGlobals.initialize(this);
        applySkinStyle();
        applyTextFieldStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        // КАМЕРА
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        // ИНИЦИАЛИЗАЦИЯ МЕНЕДЖЕРОВ
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

        if (uiManager != null && playerManager != null) uiManager.setPlayerManager(playerManager);
        if (uiManager != null && inventoryManager != null) uiManager.setInventoryManager(inventoryManager);
        if (dropManager != null && inventoryManager != null) dropManager.setInventoryManager(inventoryManager);
        if (playerManager != null) {
            playerManager.setWorldManager(worldManager);
            playerManager.setDropManager(dropManager);
        }

        // УПРАВЛЕНИЕ
        setupInput();

        Monster.setApp(this);
        playerManager.setUIManager(uiManager);
        playerManager.setNetworkManager(networkManager);

        // СРАЗУ ПОКАЗЫВАЕМ ЛОГИН (вместо видео)
        if (uiManager != null) {
            uiManager.forceShowLogin();
        }
    }

    private void setupInput() {
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

        inputManager.addMapping("RightClick", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if ("RightClick".equals(name)) {
                    isRotatingCamera = isPressed;
                    if (cameraControl != null) cameraControl.setEnabled(!isPressed);
                    if (isPressed) lastMouseX = inputManager.getCursorPosition().x;
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
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        if (gameManager != null) gameManager.update(tpf);
        if (playerManager != null) playerManager.update(tpf);
        if (worldManager != null) worldManager.update(tpf);
        if (uiManager != null) uiManager.update(tpf);
        updateCamera();
    }

    private void updateCamera() {
        if (!worldLoaded || gameManager == null) return;
        GameState state = gameManager.getCurrentState();
        if (state != GameState.CITY && state != GameState.DUNGEON) return;
        if (playerManager == null) return;

        Node playerNode = playerManager.getPlayerNode();
        if (playerNode == null) return;
        Vector3f playerPos = playerNode.getWorldTranslation();
        if (playerPos == null) return;

        if (cameraControl != null && cameraControl.isEnabled()) return;

        float distance = 18f;
        float height = 22f;
        float x = FastMath.sin(cameraAngle) * distance;
        float z = FastMath.cos(cameraAngle) * distance;
        Vector3f camPos = playerPos.add(new Vector3f(x, height, z));
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

    // ============================================================
    //                    МЕНЕДЖЕРЫ
    // ============================================================

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