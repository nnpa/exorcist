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
import com.mygame.managers.GameManager.GameState;
import com.mygame.monsters.Monster;
import com.mygame.monsters.SkeletonWarrior;
import com.mygame.dungeons.Dungeon;
import com.mygame.dungeons.DungeonLoader;
import com.mygame.dungeons.DungeonManager;

import java.util.ArrayList;
import java.util.List;

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
private void createTeleporter() {
    System.out.println("[WorldManager] Loading Teleporter model from: Models/City/NPC/Teleport/Teleport.gltf");
    try {
        Spatial teleporterModel = app.getAssetManager().loadModel("Models/City/NPC/Teleport/teleport.gltf");
        if (teleporterModel != null) {
            teleporterModel.setName("Teleporter");
            teleporterModel.scale(2.5f); // возможно, подобрать масштаб
            Vector3f pos = new Vector3f(-3.711333f, -1.9884592f, -11.55361f);
            pos.y += 0.7f; // поднять на 2 единицы
            teleporterModel.setLocalTranslation(pos);
            teleporterModel.rotate(0, -FastMath.HALF_PI / 2.0f, 0); // 30 градусов = PI/6 ≈ 0.5236, но -0.5236 для по часовой

            cityNode.attachChild(teleporterModel);
            this.teleporter = teleporterModel;
            System.out.println("[WorldManager] Teleporter model loaded and placed at (0, 0, -8)");
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
    mat.setColor("Color", new ColorRGBA(1f, 0.4f, 0.8f, 1f));
    teleporterGeom.setMaterial(mat);
    teleporterGeom.setName("Teleporter");
    teleporterGeom.setLocalTranslation(0f, 0.8f, -8f);
    cityNode.attachChild(teleporterGeom);
    this.teleporter = teleporterGeom;
    System.out.println("[WorldManager] Teleporter placeholder created at (0, 0.8, -8)");
}

// Добавить метод для телепортации игрока в данж
public void teleportToDungeon() {
    if (playerManager == null) return;
    // Сохраняем текущую позицию в городе (опционально)
    // Переключаемся на данж
    loadDungeon("dungeon_1"); // предполагаем, что есть такой метод
    // Перемещаем игрока в точку спавна данжа
    playerManager.setPosition(new Vector3f(0f, 2.5f, 0f));
    if (playerManager.getCharacterControl() != null) {
        playerManager.getCharacterControl().warp(new Vector3f(0f, 2.5f, 0f));
    }
    System.out.println("[WorldManager] Игрок телепортирован в данж");
}
    // ============================================================
    //   loadCityWithPhysics()
    // ============================================================
    public void loadCityWithPhysics() {
        System.out.println("[WorldManager] ===== LOADING CITY WITH PHYSICS =====");
        
        // Очищаем
        cityNode.detachAllChildren();
        npcNode.detachAllChildren();
        monsters.clear();
        activeMonsters.clear();
        testMonsterSpawned = false;
        
        createCityScene();
        loadNPCs();
        createInteractiveObjects();
        spawnTestMonster(); // Оригинальный метод с SkeletonWarrior
        createTeleporter();

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
    //   NPC (КУЗНЕЦ И ТОРГОВЕЦ)
    // ============================================================
    private void loadNPCs() {
        System.out.println("[WorldManager] Loading NPCs...");
        
        // ===== КУЗНЕЦ (Black) =====
        loadBlacksmith();
        
        // ===== ТОРГОВЕЦ (зеленый куб) =====
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
            System.err.println("[WorldManager] Blacksmith model is NULL! Skipping placeholder.");
            // Убрали createBlacksmithPlaceholder()
        }
    } catch (Exception e) {
        System.err.println("[WorldManager] Error loading Blacksmith: " + e.getMessage());
        // Убрали createBlacksmithPlaceholder()
    }
}

    private void createBlacksmithPlaceholder() {
        System.out.println("[WorldManager] Creating Blacksmith placeholder...");
        
        Node npcNodeLocal = new Node("BlacksmithPlaceholder");
        
        Geometry body = new Geometry("Body", new Box(0.3f, 0.5f, 0.2f));
        Material bodyMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bodyMat.setColor("Color", new ColorRGBA(0.2f, 0.3f, 0.8f, 1f));
        body.setMaterial(bodyMat);
        body.move(0, 0.5f, 0);
        npcNodeLocal.attachChild(body);
        
        Geometry head = new Geometry("Head", new com.jme3.scene.shape.Sphere(8, 8, 0.15f));
        Material headMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        headMat.setColor("Color", new ColorRGBA(0.9f, 0.7f, 0.5f, 1f));
        head.setMaterial(headMat);
        head.move(0, 1.1f, 0);
        npcNodeLocal.attachChild(head);
        
        npcNodeLocal.setLocalTranslation(6f, 0f, 3f);
        npcNodeLocal.setName("NPC_Blacksmith");
        npcNode.attachChild(npcNodeLocal);
        
        System.out.println("[WorldManager] Blacksmith placeholder at (6, 0, 3)");
    }

// ============================================================
//   ТОРГОВЕЦ (trade.gltf) — с ручным позиционированием
// ============================================================
private void createTraderNPC() {
    System.out.println("[WorldManager] Loading Trader NPC from: Models/City/NPC/Trade/trade.gltf");
    try {
        Spatial trader = app.getAssetManager().loadModel("Models/City/NPC/Trade/trade.gltf");
        if (trader != null) {
            trader.scale(0.65f);
            trader.rotate(0, FastMath.PI, 0);
            trader.setLocalTranslation(9.285181f, -0.5f, 10.98106f); // подберите Y под вашу модель
            trader.setName("NPC_Trader");
            npcNode.attachChild(trader);
            System.out.println("[WorldManager] Trader NPC placed at (9.285, -0.5, 10.981)");
        } else {
            System.err.println("[WorldManager] Trader model is NULL! Skipping placeholder.");
            // Убрали createTraderPlaceholder()
        }
    } catch (Exception e) {
        System.err.println("[WorldManager] Error loading Trader: " + e.getMessage());
        // Убрали createTraderPlaceholder()
    }
}

private void createTraderPlaceholder() {
    Box npcBox = new Box(0.5f, 0.8f, 0.5f); // чуть выше
    Geometry npc = new Geometry("NPC_Trader_Placeholder", npcBox);
    npc.setName("NPC_Trader");
    Material npcMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    npcMat.setColor("Color", ColorRGBA.Green);
    npc.setMaterial(npcMat);
    npc.setLocalTranslation(9.285181f, 0.8f, 10.98106f); // половинка высоты
    npcNode.attachChild(npc);
    System.out.println("[WorldManager] Trader placeholder at (9.285, 0.8, 10.981)");
}
    // ============================================================
    //   ИНТЕРАКТИВНЫЕ ОБЪЕКТЫ
    // ============================================================
    private void createInteractiveObjects() {
        System.out.println("[WorldManager] Creating interactive objects...");
        
        
    }
private Spatial teleporter;

    // ============================================================
    //   ТЕСТОВЫЙ МОНСТР (ОРИГИНАЛЬНЫЙ SKELETON WARRIOR)
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

    // Создаём скелета
    Monster skeleton = new SkeletonWarrior();
    Vector3f spawnPos = new Vector3f(6f, -1.65f, -6f); // XZ остаются, Y пока 0
    skeleton.setSpawnPosition(spawnPos);
    skeleton.setPlayerManager(playerManager);
    skeleton.setDropManager(dropManager);

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
        
        // ===== 1. СНАЧАЛА МАСШТАБ И ПОВОРОТ =====
        model.scale(2.0f);
        // Поворот не трогаем, если модель уже повёрнута правильно
        
        // ===== 2. ОБНОВЛЯЕМ BOUND, ЧТОБЫ ВЫЧИСЛИТЬ РАЗМЕР =====
        model.updateModelBound();
        com.jme3.bounding.BoundingBox bb = (com.jme3.bounding.BoundingBox) model.getWorldBound();
        float bottomY = bb.getCenter().y - bb.getYExtent();
        System.out.println("[WorldManager] Skeleton bottomY = " + bottomY + ", centerY = " + bb.getCenter().y);
        
        // ===== 3. СМЕЩАЕМ МОДЕЛЬ ТАК, ЧТОБЫ bottomY = 0 =====
        float offsetY = -bottomY; // поднимаем так, чтобы низ был на 0
        model.move(0, offsetY, 0); // теперь ноги будут на Y=0
        
        // ===== 4. УСТАНАВЛИВАЕМ КОНЕЧНУЮ ПОЗИЦИЮ =====
        // Если хотим, чтобы скелет стоял на земле, Y уже не нужен (он уже в offsetY)
        // Но координата XZ остаётся из spawnPos
        model.setLocalTranslation(spawnPos.x, 0, spawnPos.z); // Y=0, т.к. мы сместили модель
        
        skeleton.setModelNode((Node) model);
        targetNode.attachChild(model);
        System.out.println("[WorldManager] Model attached to cityNode at Y=" + 0);
    } else {
        System.err.println("[WorldManager] Model is NULL! Creating placeholder...");
        Geometry box = new Geometry("Stub", new Box(0.5f, 1f, 0.3f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        box.setMaterial(mat);
        box.setLocalTranslation(spawnPos.x, 0.5f, spawnPos.z);
        box.setName("Monster");
        Node stubNode = new Node("StubNode");
        stubNode.attachChild(box);
        targetNode.attachChild(stubNode);
        skeleton.setModelNode(stubNode);
        System.out.println("[WorldManager] Red cube placeholder added");
    }

    // ===== 5. ДОБАВЛЯЕМ В СПИСКИ ДЛЯ ОТСЛЕЖИВАНИЯ =====
    activeMonsters.add(skeleton);
    testMonsterSpawned = true;
    
    // Добавляем в monsters для кликабельности
    if (skeleton.getModelNode() != null) {
        // Ищем геометрию для MonsterData
        Spatial modelNode = skeleton.getModelNode();
        if (modelNode instanceof Node) {
            for (Spatial child : ((Node) modelNode).getChildren()) {
                if (child instanceof Geometry) {
                    MonsterData md = new MonsterData((Geometry) child, 30, 10);
                    monsters.add(md);
                    System.out.println("[WorldManager] MonsterData added for click detection");
                    break;
                }
            }
        }
    }
    
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
                if (name.equals("Portal") || name.equals("Teleport") || name.equals("Monster")) {
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

    public void switchToCity() {
        cityNode.setCullHint(Node.CullHint.Dynamic);
        dungeonNode.setCullHint(Node.CullHint.Always);
        npcNode.setCullHint(Node.CullHint.Dynamic);
        currentState = GameState.CITY;
        // Не спавним здесь, чтобы не дублировать
        System.out.println("[WorldManager] Switched to City");
    }

    public void switchToDungeon() {
        cityNode.setCullHint(Node.CullHint.Always);
        dungeonNode.setCullHint(Node.CullHint.Always);
        npcNode.setCullHint(Node.CullHint.Always);
        dungeonNode.setCullHint(Node.CullHint.Dynamic);
        currentState = GameState.DUNGEON;
        System.out.println("[WorldManager] Switched to Dungeon");
    }

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
                cityNode.setCullHint(Node.CullHint.Always);
                dungeonNode.setCullHint(Node.CullHint.Always);
                npcNode.setCullHint(Node.CullHint.Always);
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

    public void loadDungeon(String dungeonId) {
        System.out.println("[WorldManager] Loading dungeon: " + dungeonId);
    }

    public void cleanup() {
        app.getRootNode().detachChild(worldNode);
        System.out.println("[WorldManager] Cleanup");
    }

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