package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.mygame.Main;
import com.mygame.managers.GameManager.GameState;
import com.mygame.monsters.Monster;
import com.mygame.monsters.SkeletonWarrior;
import com.mygame.dungeons.Dungeon;
import com.mygame.dungeons.DungeonLoader;
import com.mygame.dungeons.DungeonManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldManager {

    private SimpleApplication app;
    private Node worldNode;
    private Node cityNode;
    private Node dungeonNode;
    private Node interactableNode;
    private Node npcNode;

    private List<MonsterData> monsters = new ArrayList<>();
    private List<Monster> activeMonsters = new ArrayList<>();
    private boolean testMonsterSpawned = false;

    private GameState currentState = GameState.LOADING;
    private PlayerManager playerManager;
    private DropManager dropManager;

    private DungeonLoader dungeonLoader;
    private DungeonManager dungeonManager;

    private BulletAppState bulletAppState;

    // ============================================================
    //   ИСПРАВЛЕНИЕ 1: Поле NetworkManager добавлено и инициализируется
    // ============================================================
    private NetworkManager networkManager;

    public WorldManager(SimpleApplication app) {
        this.app = app;
        this.worldNode = new Node("WorldNode");
        this.cityNode = new Node("CityNode");
        this.dungeonNode = new Node("DungeonNode");
        this.npcNode = new Node("NPCNode");
    }

    public void setBulletAppState(BulletAppState bas) {
        this.bulletAppState = bas;
        System.out.println("[WorldManager] BulletAppState set: " + (bas != null));
    }

    // ============================================================
    //   ИСПРАВЛЕНИЕ 2: Добавлен сеттер для NetworkManager
    // ============================================================
    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public void initialize() {
        System.out.println("[WorldManager] Initializing...");
        app.getRootNode().attachChild(worldNode);
        addLighting();

        worldNode.attachChild(cityNode);
        worldNode.attachChild(dungeonNode);
        worldNode.attachChild(npcNode);

        cityNode.setCullHint(Node.CullHint.Always);
        dungeonNode.setCullHint(Node.CullHint.Always);
        npcNode.setCullHint(Node.CullHint.Always);

        dungeonLoader = new DungeonLoader(app);
        dungeonManager = new DungeonManager();

        // Передаём ссылку на этот WorldManager в DungeonLoader
        dungeonLoader.setWorldManager(this);

        System.out.println("[WorldManager] Initialization complete");
    }

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
        System.out.println("[WorldManager] PlayerManager set");
    }

    public void setDropManager(DropManager dm) {
        this.dropManager = dm;
        if (dungeonLoader != null) {
            dungeonLoader.setDropManager(dm);
        }
        System.out.println("[WorldManager] DropManager set");
    }

    public void setInteractableNode(Node node) {
        this.interactableNode = node;
    }

    private void addLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        worldNode.addLight(sun);
    }

    // ============================================================
    //   ЗАГРУЗКА ГОРОДА
    // ============================================================
    public void loadCityWithPhysics() {
        System.out.println("[WorldManager] ===== LOADING CITY WITH PHYSICS =====");

        // Очищаем город и удаляем его физику
        if (cityNode != null) {
            removePhysicsFromNode(cityNode);
            cityNode.detachAllChildren();
        }
        npcNode.detachAllChildren();
        monsters.clear();
        activeMonsters.clear();
        testMonsterSpawned = false;

        createCityScene();
        loadNPCs();
        createInteractiveObjects();
        spawnTestMonster();

        cityNode.setCullHint(Node.CullHint.Dynamic);
        npcNode.setCullHint(Node.CullHint.Dynamic);

        System.out.println("[WorldManager] City loaded!");
    }

    private void createCityScene() {
        System.out.println("[WorldManager] Creating city scene...");

        Spatial cityModel = null;
        try {
            System.out.println("[WorldManager] Loading city model from: Models/City/city.gltf");
            cityModel = app.getAssetManager().loadModel("Models/City/city.gltf");
        } catch (Exception e) {
            System.err.println("[WorldManager] Exception loading city model: " + e.getMessage());
        }

        if (cityModel != null) {
            System.out.println("[WorldManager] City model loaded.");
            cityModel.rotate(0, FastMath.HALF_PI, 0);
            cityModel.scale(1.0f);
            cityModel.move(0, -2f, 0);

            if (bulletAppState != null) {
                try {
                    System.out.println("[WorldManager] Adding physics to city...");
                    CollisionShape shape = CollisionShapeFactory.createMeshShape(cityModel);
                    RigidBodyControl physics = new RigidBodyControl(shape, 0);
                    cityModel.addControl(physics);
                    bulletAppState.getPhysicsSpace().add(physics);
                    System.out.println("[WorldManager] Physics added to city.");
                } catch (Exception e) {
                    System.err.println("[WorldManager] Physics error: " + e.getMessage());
                }
            } else {
                System.err.println("[WorldManager] BulletAppState is NULL! City without physics!");
            }

            cityNode.attachChild(cityModel);
        } else {
            createFallbackCity();
        }
    }

    private void createFallbackCity() {
        System.out.println("[WorldManager] Creating fallback city...");

        Box floorBox = new Box(30, 0.1f, 30);
        Geometry floor = new Geometry("CityFloor", floorBox);
        floor.setName("Ground");
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.3f, 0.35f, 0.4f, 1.0f));
        floor.setMaterial(mat);
        floor.move(0, -0.05f, 0);
        cityNode.attachChild(floor);

        if (bulletAppState != null) {
            RigidBodyControl floorPhysics = new RigidBodyControl(0);
            floor.addControl(floorPhysics);
            bulletAppState.getPhysicsSpace().add(floorPhysics);
            System.out.println("[WorldManager] Fallback floor physics added.");
        }
    }

    // ============================================================
    //   NPC
    // ============================================================
    private void loadNPCs() {
        System.out.println("[WorldManager] Loading NPCs...");

        loadBlacksmith();
        createTraderNPC();

        System.out.println("[WorldManager] NPCs loaded");
    }

    private void loadBlacksmith() {
        System.out.println("[WorldManager] Loading Blacksmith from: Models/City/NPC/Black/black.gltf");
        try {
            Spatial blacksmith = app.getAssetManager().loadModel("Models/City/NPC/Black/black.gltf");
            if (blacksmith != null) {
                System.out.println("[WorldManager] Blacksmith model loaded successfully!");
                blacksmith.scale(0.7f);
                blacksmith.rotate(0, FastMath.PI, 0);
                blacksmith.setLocalTranslation(11.341951f, -1.9861704f, -6.05198f);
                blacksmith.setName("NPC_Blacksmith");
                npcNode.attachChild(blacksmith);
                System.out.println("[WorldManager] Blacksmith placed at (11.342, -1.986, -6.052)");
            } else {
                System.err.println("[WorldManager] Blacksmith model is NULL!");
            }
        } catch (Exception e) {
            System.err.println("[WorldManager] Error loading Blacksmith: " + e.getMessage());
        }
    }

    private void createTraderNPC() {
        System.out.println("[WorldManager] Loading Trader NPC from: Models/City/NPC/Trade/trade.gltf");
        try {
            Spatial trader = app.getAssetManager().loadModel("Models/City/NPC/Trade/trade.gltf");
            if (trader != null) {
                trader.scale(0.65f);
                trader.rotate(0, FastMath.PI, 0);
                trader.setLocalTranslation(9.285181f, -0.5f, 10.98106f);
                trader.setName("NPC_Trader");
                npcNode.attachChild(trader);
                System.out.println("[WorldManager] Trader NPC placed at (9.285, -0.5, 10.981)");
            } else {
                System.err.println("[WorldManager] Trader model is NULL!");
            }
        } catch (Exception e) {
            System.err.println("[WorldManager] Error loading Trader: " + e.getMessage());
        }
    }

    // ============================================================
    //   ИНТЕРАКТИВНЫЕ ОБЪЕКТЫ
    // ============================================================
    private void createInteractiveObjects() {
        System.out.println("[WorldManager] Creating interactive objects...");

        // Портал
        Sphere portalSphere = new Sphere(16, 16, 1.5f);
        Geometry portal = new Geometry("Portal", portalSphere);
        portal.setName("Portal");
        Material portalMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        portalMat.setColor("Color", new ColorRGBA(0.2f, 0.6f, 1.0f, 0.7f));
        portal.setMaterial(portalMat);
        portal.move(5, 1.5f, 5);
        cityNode.attachChild(portal);
        System.out.println("[WorldManager] Portal at (5, 1.5, 5)");

        // Телепортер (розовый куб)
        createTeleporter();
    }

    private Spatial teleporter;

    private void createTeleporter() {
        System.out.println("[WorldManager] Loading Teleporter model from: Models/City/NPC/Teleport/teleport.gltf");
        try {
            Spatial teleporterModel = app.getAssetManager().loadModel("Models/City/NPC/Teleport/teleport.gltf");
            if (teleporterModel != null) {
                teleporterModel.setName("Teleporter");
                teleporterModel.scale(2.5f);
                Vector3f pos = new Vector3f(-3.711333f, -1.9884592f, -11.55361f);
                pos.y += 0.7f;
                teleporterModel.setLocalTranslation(pos);
                teleporterModel.rotate(0, -FastMath.HALF_PI / 2.0f, 0);
                cityNode.attachChild(teleporterModel);
                this.teleporter = teleporterModel;
                System.out.println("[WorldManager] Teleporter model loaded and placed.");
            } else {
                System.err.println("[WorldManager] Teleporter model is NULL, creating placeholder.");
                createTeleporterPlaceholder();
            }
        } catch (Exception e) {
            System.err.println("[WorldManager] Error loading Teleporter: " + e.getMessage());
            createTeleporterPlaceholder();
        }
    }

    private void createTeleporterPlaceholder() {
        Box box = new Box(0.8f, 0.8f, 0.8f);
        Geometry teleporterGeom = new Geometry("Teleporter", box);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1f, 0.4f, 0.8f, 1f)); // розовый
        teleporterGeom.setMaterial(mat);
        teleporterGeom.setName("Teleporter");
        teleporterGeom.setLocalTranslation(0f, 0.8f, -8f);
        cityNode.attachChild(teleporterGeom);
        this.teleporter = teleporterGeom;
        System.out.println("[WorldManager] Teleporter placeholder created at (0, 0.8, -8)");
    }

    // ============================================================
    //   МОНСТРЫ
    // ============================================================
    public void spawnTestMonster() {
        System.out.println("[WorldManager] spawnTestMonster() CALLED!");
        if (testMonsterSpawned) {
            System.out.println("[WorldManager] Already spawned, skipping");
            return;
        }
        if (playerManager == null) {
            System.err.println("[WorldManager] PlayerManager is NULL!");
            return;
        }
        if (dropManager == null) {
            System.err.println("[WorldManager] DropManager is NULL!");
            return;
        }

        Monster skeleton = new SkeletonWarrior();
        Vector3f spawnPos = new Vector3f(6f, -1.65f, -6f);
        skeleton.setSpawnPosition(spawnPos);
        skeleton.setPlayerManager(playerManager);
        skeleton.setDropManager(dropManager);
        skeleton.setWorldManager(this); // передаём WorldManager для смены данжа

        Spatial model = null;
        try {
            System.out.println("[WorldManager] Loading model: Models/Monsters/skeleton_warrior.gltf");
            model = app.getAssetManager().loadModel("Models/Monsters/skeleton_warrior.gltf");
        } catch (Exception e) {
            System.err.println("[WorldManager] Exception loading model: " + e.getMessage());
            e.printStackTrace();
        }

        Node targetNode = cityNode;

        if (model != null) {
            model.setName("Monster");
            System.out.println("[WorldManager] Model loaded successfully!");
            model.scale(2.0f);
            model.move(0, 0.5f, 0);
            skeleton.setModelNode((Node) model);
            targetNode.attachChild(model);
            System.out.println("[WorldManager] Model attached to cityNode");
        } else {
            System.err.println("[WorldManager] Model is NULL! Creating placeholder...");
            Geometry box = new Geometry("Stub", new Box(0.5f, 1f, 0.3f));
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Red);
            box.setMaterial(mat);
            box.setLocalTranslation(spawnPos);
            box.setName("Monster");
            Node stubNode = new Node("StubNode");
            stubNode.attachChild(box);
            targetNode.attachChild(stubNode);
            skeleton.setModelNode(stubNode);
            System.out.println("[WorldManager] Red cube placeholder added");
        }

        activeMonsters.add(skeleton);
        testMonsterSpawned = true;
        System.out.println("[WorldManager] Test Skeleton spawned at " + spawnPos);
    }

    public Monster getMonsterByModel(Spatial model) {
        for (Monster m : activeMonsters) {
            if (m.getModelNode() == model) return m;
        }
        return null;
    }

    public MonsterData getMonsterByGeometry(Geometry geom) {
        for (MonsterData md : monsters) {
            if (md.geom == geom) return md;
        }
        return null;
    }

    public Spatial getClosestInteractiveObject(Vector3f point, float radius) {
        Spatial closest = null;
        float closestDist = radius;

        for (Monster m : activeMonsters) {
            if (!m.isAlive()) continue;
            Spatial model = m.getModelNode();
            if (model == null) continue;
            float dist = model.getWorldTranslation().distance(point);
            if (dist < closestDist) {
                closestDist = dist;
                closest = model;
            }
        }

        if (cityNode != null) {
            for (Spatial child : cityNode.getChildren()) {
                String name = child.getName();
                if (name == null) continue;
                if (name.equals("Portal") || name.equals("Teleporter")) {
                    float dist = child.getWorldTranslation().distance(point);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = child;
                    }
                }
            }
        }

        if (npcNode != null) {
            for (Spatial child : npcNode.getChildren()) {
                String name = child.getName();
                if (name == null) continue;
                if (name.equals("NPC_Trader") || name.equals("NPC_Blacksmith")) {
                    float dist = child.getWorldTranslation().distance(point);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = child;
                    }
                }
            }
        }

        return closest;
    }

    // ============================================================
    //   ПЕРЕКЛЮЧЕНИЕ СОСТОЯНИЙ
    // ============================================================
    public void switchToCity() {
        if (dungeonNode != null) {
            removePhysicsFromNode(dungeonNode);
            dungeonNode.setCullHint(Node.CullHint.Always);
        }
        if (cityNode != null) {
            cityNode.setCullHint(Node.CullHint.Dynamic);
        }
        if (npcNode != null) {
            npcNode.setCullHint(Node.CullHint.Dynamic);
        }
        currentState = GameState.CITY;
        System.out.println("[WorldManager] Switched to City");
    }

    public void switchToDungeon() {
        if (cityNode != null) {
            removePhysicsFromNode(cityNode);
            cityNode.setCullHint(Node.CullHint.Always);
        }
        if (dungeonNode != null) {
            dungeonNode.setCullHint(Node.CullHint.Dynamic);
        }
        if (npcNode != null) {
            npcNode.setCullHint(Node.CullHint.Always);
        }
        currentState = GameState.DUNGEON;
        System.out.println("[WorldManager] Switched to Dungeon");
    }

    // ============================================================
    //   ЗАГРУЗКА ДАНЖА
    // ============================================================
    public void loadDungeon(String dungeonId, int difficulty) {
        if (dungeonNode != null) {
            removePhysicsFromNode(dungeonNode);
            worldNode.detachChild(dungeonNode);
            dungeonNode = null;
        }

        dungeonNode = new Node("DungeonNode_" + dungeonId);
        worldNode.attachChild(dungeonNode);

        dungeonManager.clearMonsters();
        activeMonsters.clear();

        dungeonLoader.setBulletAppState(bulletAppState);

        Dungeon dungeon = dungeonLoader.loadDungeon("dungeons/" + dungeonId + ".json");
        if (dungeon != null) {
            dungeonLoader.setPlayerManager(playerManager);
            dungeonLoader.setDropManager(dropManager);
            List<Monster> newMonsters = dungeonLoader.spawnDungeon(dungeon, dungeonNode, difficulty);
            activeMonsters.addAll(newMonsters);
            for (Monster m : newMonsters) {
                m.setWorldManager(this);
                dungeonManager.addMonster(m);
            }
            dungeonManager.setCurrentDungeon(dungeon);
        }

        switchToDungeon();

        if (Main.getInstance() != null) {
            Main.getInstance().getGameManager().setState(GameState.DUNGEON);
        }
    }

    public void returnToCity() {
        if (playerManager == null) return;
        
        Vector3f currentPos = playerManager.getPosition();
        playerManager.setLastDungeonPosition(currentPos);

        if (dungeonNode != null) {
            removePhysicsFromNode(dungeonNode);
            worldNode.detachChild(dungeonNode);
            dungeonNode = null;
        }
        dungeonManager.clearMonsters();
        activeMonsters.clear();

        loadCityWithPhysics();

        Vector3f citySpawn = new Vector3f(0f, 0.5f, -8f);
        playerManager.setPosition(citySpawn);
        if (playerManager.getCharacterControl() != null) {
            playerManager.getCharacterControl().warp(citySpawn);
        }

        switchToCity();

        if (Main.getInstance() != null) {
            Main.getInstance().getGameManager().setState(GameState.CITY);
        }
        System.out.println("[WorldManager] Returned to city");
    }

    // ============================================================
    //   УДАЛЕНИЕ ФИЗИКИ
    // ============================================================
    private void removePhysicsFromNode(Node node) {
        if (node == null || bulletAppState == null) return;
        for (Spatial child : node.getChildren()) {
            removePhysicsFromSpatial(child);
        }
        removePhysicsFromSpatial(node);
    }

    private void removePhysicsFromSpatial(Spatial spatial) {
        if (spatial == null) return;
        RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
        if (rbc != null) {
            bulletAppState.getPhysicsSpace().remove(rbc);
            spatial.removeControl(rbc);
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                removePhysicsFromSpatial(child);
            }
        }
    }

    // ============================================================
    //   СМЕНА ДАНЖА ПОСЛЕ БОССА (С ИСПРАВЛЕННЫМ ВЫЗОВОМ)
    // ============================================================
    public void changeDungeon(String newDungeonId, boolean increaseDifficulty) {
        if (playerManager != null) {
            if (increaseDifficulty) {
                int newDiff = playerManager.getCurrentDifficulty() + 1;
                playerManager.setCurrentDifficulty(newDiff);
            }
            playerManager.setCurrentDungeon(newDungeonId);
            
            saveDungeonAndDifficultyToServer();
        }
        
        // ИСПРАВЛЕНИЕ: Передаем актуальную сложность в метод загрузки
        int currentDifficulty = playerManager != null ? playerManager.getCurrentDifficulty() : 1;
        loadDungeon(newDungeonId, currentDifficulty);
    }

    private void saveDungeonAndDifficultyToServer() {
        if (networkManager == null || playerManager == null) {
            System.err.println("[WorldManager] Cannot save: networkManager or playerManager is null!");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("currentDungeon", playerManager.getCurrentDungeon());
        data.put("difficulty", playerManager.getCurrentDifficulty());
        data.put("lastX", playerManager.getPosition().x);
        data.put("lastY", playerManager.getPosition().y);
        data.put("lastZ", playerManager.getPosition().z);

        networkManager.saveCharacter(data).thenAccept(success -> {
            app.enqueue(() -> {
                if (success) {
                    System.out.println("[WorldManager] Dungeon state saved to server.");
                } else {
                    System.err.println("[WorldManager] Failed to save dungeon state!");
                }
            });
        });
    }

    // ============================================================
    //   ТЕЛЕПОРТ В ТЕКУЩИЙ ДАНЖ
    // ============================================================
    public void teleportToDungeon() {
        if (playerManager == null) return;
        String dungeonId = playerManager.getCurrentDungeon();
        int difficulty = playerManager.getCurrentDifficulty();
        if (dungeonId == null || dungeonId.isEmpty()) {
            dungeonId = "dungeon_1";
            playerManager.setCurrentDungeon(dungeonId);
        }

        loadDungeon(dungeonId, difficulty);

        Vector3f spawnPos = playerManager.getLastDungeonPosition();
        if (spawnPos == null || spawnPos.length() < 0.01f) {
            spawnPos = new Vector3f(0f, 2.5f, 0f);
        }
        playerManager.setPosition(spawnPos);
        if (playerManager.getCharacterControl() != null) {
            playerManager.getCharacterControl().warp(spawnPos);
        }
        switchToDungeon();
    }

    // ============================================================
    //   ОСТАЛЬНЫЕ МЕТОДЫ
    // ============================================================
    public void onStateChanged(GameState newState) {
        System.out.println("[WorldManager] State change: " + newState);
        switch (newState) {
            case CITY:
                switchToCity();
                break;
            case DUNGEON:
                switchToDungeon();
                break;
            default:
                if (cityNode != null) cityNode.setCullHint(Node.CullHint.Always);
                if (dungeonNode != null) dungeonNode.setCullHint(Node.CullHint.Always);
                if (npcNode != null) npcNode.setCullHint(Node.CullHint.Always);
                break;
        }
    }

    public void update(float tpf) {
        for (Monster m : activeMonsters) {
            m.update(tpf);
        }
        if (dungeonManager != null) {
            dungeonManager.update();
        }
    }

    public void cleanup() {
        app.getRootNode().detachChild(worldNode);
        System.out.println("[WorldManager] Cleanup");
    }

    // ============================================================
    //   ГЕТТЕРЫ
    // ============================================================
    public Node getWorldNode() { return worldNode; }
    public Node getCityNode() { return cityNode; }
    public Node getDungeonNode() { return dungeonNode; }
    public Node getInteractableNode() { return interactableNode; }
    public Node getNpcNode() { return npcNode; }
    public List<Monster> getActiveMonsters() { return activeMonsters; }
    public DungeonManager getDungeonManager() { return dungeonManager; }

    public static class MonsterData {
        public Geometry geom;
        public int hp;
        public int maxHp;
        public int damage;
        public boolean isDead = false;

        public MonsterData(Geometry geom, int hp, int damage) {
            this.geom = geom;
            this.hp = hp;
            this.maxHp = hp;
            this.damage = damage;
        }
    }
}