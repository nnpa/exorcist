package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.Insets3f;
import com.mygame.managers.*;
import com.mygame.managers.GameManager.GameState;
import com.mygame.monsters.Monster;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;
import com.simsilica.lemur.component.QuadBackgroundComponent;

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
    
    // Параметры камеры (используются в CameraFollowControl)
    private float camDistance = 12f;
    private float camHeight = 8f;

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

        // ===== ФИЗИКА (SEQUENTIAL + ВЫСОКАЯ ТОЧНОСТЬ) =====
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
                if (isPressed && "RightClick".equals(name)) {
                    if (!worldLoaded) return;
                    if (uiManager != null && uiManager.isAnyWindowOpen()) return;
                    if (playerManager == null) return;
                    
                    Vector2f cursor = inputManager.getCursorPosition();
                    Vector3f groundPoint = getGroundPoint(cursor.x, cursor.y);
                    if (groundPoint != null) {
                        playerManager.moveTo(groundPoint);
                    }
                }
            }
        }, "RightClick");

        Monster.setApp(this);
    }

    public void loadGameWorld() {
        if (worldLoaded) return;
        
        System.out.println("[Main] ===== ЗАГРУЗКА МИРА =====");
        
        // Включаем физику и настраиваем точность
        bulletAppState.setEnabled(true);
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        space.setAccuracy(0.001f);
        space.setMaxSubSteps(10);
        bulletAppState.setSpeed(0.8f);
        System.out.println("[Main] Физика включена с высокой точностью");
        
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

            // ===== ДОБАВЛЯЕМ CAMERA FOLLOW CONTROL =====
            Vector3f camOffset = new Vector3f(0, camHeight, camDistance);
            playerNode.addControl(new CameraFollowControl(cam, camOffset));
            System.out.println("[Main] CameraFollowControl добавлен на персонажа");
        }
        
        gameManager.setState(GameState.CITY);
        worldManager.switchToCity();
        
        worldLoaded = true;
        System.out.println("[Main] ===== МИР ЗАГРУЖЕН =====");
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

    // ===== ИЩЕМ ТОРГОВЦА (NPC) =====
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

    // ===== ИЩЕМ МОНСТРОВ С УВЕЛИЧЕННЫМ РАДИУСОМ =====
    Spatial clicked = worldManager.getClosestInteractiveObject(groundPoint, 5.5f); // было 1.2f
    if (clicked != null) {
        String objectName = clicked.getName();
        if ("TestMonster".equals(objectName) || "Monster".equals(objectName)) {
            playerManager.attackTarget(clicked);
            return;
        }
    }

    // ===== ПРОСТОЕ ДВИЖЕНИЕ =====
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
    }

    // ===== simpleUpdate – ТОЛЬКО ЛОГИКА (КАМЕРА НЕ ОБНОВЛЯЕТСЯ) =====
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        
        if (gameManager != null) gameManager.update(tpf);
        if (playerManager != null) playerManager.update(tpf);
        if (worldManager != null) worldManager.update(tpf);
        if (uiManager != null) uiManager.update(tpf);
    }

    // ===== simpleRender – УДАЛЯЕМ, Т.К. КАМЕРА УПРАВЛЯЕТСЯ CONTROL =====
    // (оставляем пустым или удаляем)

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

    // ---------- ГЕТТЕРЫ ----------
    public static Main getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public UIManager getUIManager() { return uiManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public DropManager getDropManager() { return dropManager; }
    public boolean isWorldLoaded() { return worldLoaded; }

private void applySkinStyle() {
    Styles styles = GuiGlobals.getInstance().getStyles();

    // ===== НЕ ЗАДАЁМ ФОН ДЛЯ КОНТЕЙНЕРОВ =====
    // Убираем строки:
    // containerAttrs.set("background", leatherBg);
    // panelAttrs.set("background", leatherBg);

    // ===== НАСТРАИВАЕМ ТЕКСТ =====
    Attributes labelAttrs = styles.getSelector(Label.ELEMENT_ID, null);
    labelAttrs.set("fontSize", 18f);
    labelAttrs.set("color", new ColorRGBA(0.9f, 0.7f, 0.4f, 1f));

    // ===== НАСТРАИВАЕМ ПОЛЯ ВВОДА (TextField) =====
    Attributes textFieldAttrs = styles.getSelector(TextField.ELEMENT_ID, null);
    textFieldAttrs.set("background", new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.1f, 0.03f, 0.9f)));
    textFieldAttrs.set("color", ColorRGBA.White);
    textFieldAttrs.set("fontSize", 18f);
}
}