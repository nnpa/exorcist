package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.*;
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

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Exorcist");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);
        settings.setSamples(4);
        settings.setUseInput(true);
        Main app = new Main();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);
        instance = this;

        viewPort.setBackgroundColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));

        setupLighting();
        GuiGlobals.initialize(this);
        applyTextFieldStyle();

        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        // === ОТКЛЮЧАЕМ ПЕРЕХВАТ МЫШИ В LEMUR ===

        initializeManagers();

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

        // === ОСНОВНОЙ ОБРАБОТЧИК КЛИКОВ ===
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

        // === ДЛЯ ANDROID ===
        inputManager.addRawInputListener(new RawInputListener() {
            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onMouseMotionEvent(MouseMotionEvent evt) {}
            @Override public void onMouseButtonEvent(MouseButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}

            @Override
            public void onTouchEvent(TouchEvent evt) {
                if (evt.getType() == TouchEvent.Type.DOWN) {
                    if (playerManager == null) return;
                    if (uiManager != null && uiManager.isAnyWindowOpen()) {
                        return;
                    }
                    handleClick(evt.getX(), evt.getY());
                }
            }
        });
    }

    private void handleClick(float screenX, float screenY) {
    System.out.println("[Main] handleClick at (" + screenX + ", " + screenY + ")");

    // 1. Проверка дропа
    DropManager.DropItem drop = dropManager.getDropAt(screenX, screenY);
    if (drop != null) {
        dropManager.pickupDrop(drop);
        return;
    }

    // 2. Вычисляем точку на земле
    Vector3f groundPoint = getGroundPoint(screenX, screenY);
    if (groundPoint == null) return;

    // 3. Проверяем NPC и монстров
    Spatial npc = null;
    if (worldManager.getCityNode() != null) {
        for (Spatial child : worldManager.getCityNode().getChildren()) {
            if (child.getName() != null && child.getName().equals("NPC_Trader")) {
                float dist = child.getWorldTranslation().distance(groundPoint);
                if (dist < 1.8f) {
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

    // 4. Проверяем монстров (включая скелета)
    Spatial clicked = worldManager.getClosestInteractiveObject(groundPoint, 3.5f);
    if (clicked != null) {
        String objectName = clicked.getName();
        if ("TestMonster".equals(objectName) || "Monster".equals(objectName)) {
            playerManager.attackTarget(clicked);
            return;
        }
    }

    // 5. Движение
    playerManager.moveTo(groundPoint);
}

    private Ray createRay(float screenX, float screenY) {
        Vector2f click2d = new Vector2f(screenX, screenY);
        Vector3f origin = cam.getWorldCoordinates(click2d, 0f);
        Vector3f far = cam.getWorldCoordinates(click2d, 1f);
        Vector3f direction = far.subtract(origin).normalizeLocal();
        return new Ray(origin, direction);
    }

    private Vector3f getGroundPoint(float screenX, float screenY) {
        Ray ray = createRay(screenX, screenY);
        if (ray == null) return null;
        Plane groundPlane = new Plane(Vector3f.UNIT_Y, 0);
        Vector3f intersection = new Vector3f();
        boolean hit = ray.intersectsWherePlane(groundPlane, intersection);
        if (hit) {
            intersection.y = 0;
            return intersection;
        }
        return null;
    }

    private void setupLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.4f, 0.4f, 0.4f, 1.0f));
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

    // Устанавливаем связи (ОБЯЗАТЕЛЬНО ДО ПЕРЕКЛЮЧЕНИЯ СОСТОЯНИЙ)
    worldManager.setPlayerManager(playerManager);
    worldManager.setDropManager(dropManager);
    playerManager.setWorldManager(worldManager);
    playerManager.setDropManager(dropManager);
    dropManager.setInventoryManager(inventoryManager);
    uiManager.setPlayerManager(playerManager);
    uiManager.setInventoryManager(inventoryManager);

    gameManager.setNetworkManager(networkManager);
    gameManager.setPlayerManager(playerManager);
    gameManager.setWorldManager(worldManager);
    gameManager.setUIManager(uiManager);

    isInitialized = true;

    // Теперь можно показывать логин
    if (uiManager != null) {
        uiManager.forceShowLogin();
    }
    gameManager.setState(GameState.LOGIN);
}

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        if (worldManager != null) worldManager.update(tpf);
        if (gameManager != null) gameManager.update(tpf);
        if (playerManager != null) playerManager.update(tpf);
        if (worldManager != null) worldManager.update(tpf);
        if (uiManager != null) uiManager.update(tpf);

        if (gameManager != null && gameManager.getCurrentState() == GameState.CITY) {
            Node playerNode = playerManager.getPlayerNode();
            if (playerNode != null) {
                Vector3f playerPos = playerNode.getWorldTranslation();
                float camDistance = 12f;
                float camHeight = 8f;
                Vector3f camPos = playerPos.add(new Vector3f(0, camHeight, camDistance));
                cam.setLocation(camPos);
                cam.lookAt(playerPos, Vector3f.UNIT_Y);
            }
        }
    }

    @Override
    public void destroy() {
        if (gameManager != null) gameManager.cleanup();
        super.destroy();
    }

    @Override
    public void reshape(int w, int h) {
        super.reshape(w, h);
        if (uiManager != null) uiManager.onResize(w, h);
        if (inventoryManager != null) inventoryManager.updateLayout(w, h);
        if (uiManager != null) {
            if (uiManager.getTalentWindow() != null) uiManager.getTalentWindow().updateLayout(w, h);
            if (uiManager.getTraderWindow() != null) uiManager.getTraderWindow().updateLayout(w, h);
        }
    }

    public static Main getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public UIManager getUIManager() { return uiManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public DropManager getDropManager() { return dropManager; }
}