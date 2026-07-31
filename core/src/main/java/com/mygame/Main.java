package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
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

    public void setBulletAppState(BulletAppState bas) {
        this.bulletAppState = bas;
        System.out.println("[WorldManager] BulletAppState set: " + (bas != null));
    }
    private BulletAppState bulletAppState;

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

        // ===== ФИЗИКА =====
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        setupLighting();
        GuiGlobals.initialize(this);
        applyTextFieldStyle();

        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        initializeManagers();

        // ===== ПЕРЕДАЁМ ФИЗИКУ =====
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

        // ===== ОБРАБОТЧИК КЛИКОВ =====
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

        // ===== ТАЧ ДЛЯ ANDROID =====
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
                    if (uiManager != null && uiManager.isAnyWindowOpen()) return;
                    handleClick(evt.getX(), evt.getY());
                }
            }
        });
        Monster.setApp(this);

    }

    private void handleClick(float screenX, float screenY) {
        DropManager.DropItem drop = dropManager.getDropAt(screenX, screenY);
        if (drop != null) {
            dropManager.pickupDrop(drop);
            return;
        }

        Vector3f groundPoint = getGroundPoint(screenX, screenY);
        if (groundPoint == null) return;

        Spatial npc = null;
        if (worldManager.getCityNode() != null) {
            for (Spatial child : worldManager.getCityNode().getChildren()) {
                if (child.getName() != null && child.getName().equals("NPC_Trader")) {
                    float dist = child.getWorldTranslation().distance(groundPoint);
                    if (dist < 0.8f) {
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

        Spatial clicked = worldManager.getClosestInteractiveObject(groundPoint, 1.2f);
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

    // ===== ВАЖНО: передаём ссылки =====
    worldManager.setPlayerManager(playerManager);
    worldManager.setDropManager(dropManager);
    playerManager.setWorldManager(worldManager);
    playerManager.setDropManager(dropManager);
    dropManager.setInventoryManager(inventoryManager);
    uiManager.setPlayerManager(playerManager);
    uiManager.setInventoryManager(inventoryManager);

    // Передаём BulletAppState в WorldManager для физики города (если есть)
     worldManager.setBulletAppState(bulletAppState); // если у вас есть BulletAppState

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