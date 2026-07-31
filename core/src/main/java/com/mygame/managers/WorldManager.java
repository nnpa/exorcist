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
import static com.mygame.managers.GameManager.GameState.CITY;
import static com.mygame.managers.GameManager.GameState.DUNGEON;

import java.util.ArrayList;
import java.util.List;

public class WorldManager {

    private SimpleApplication app;
    private Node worldNode;
    private Node cityNode;
    private Node dungeonNode;
    private Node interactableNode;

    private List<MonsterData> monsters = new ArrayList<>();
    private List<Monster> activeMonsters = new ArrayList<>();
    private boolean testMonsterSpawned = false;

    private GameState currentState = GameState.LOADING;
    private PlayerManager playerManager;
    private DropManager dropManager;

    private DungeonLoader dungeonLoader;
    private DungeonManager dungeonManager;

    // Добавляем поле для BulletAppState
    private BulletAppState bulletAppState;

    public WorldManager(SimpleApplication app) {
        this.app = app;
        this.worldNode = new Node("WorldNode");
    }

    // Метод для установки BulletAppState
    public void setBulletAppState(BulletAppState bas) {
        this.bulletAppState = bas;
    }

    public void initialize() {
        System.out.println("[WorldManager] Initializing...");
        app.getRootNode().attachChild(worldNode);
        addLighting();
        createCityScene();
        createDungeonScene();

        dungeonLoader = new DungeonLoader(app);
        dungeonManager = new DungeonManager();
    }

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
        System.out.println("[WorldManager] PlayerManager set");
        // Не вызываем здесь spawnTestMonster, только в switchToCity
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
        if (cityNode != null && interactableNode != null) {
            List<Spatial> children = new ArrayList<>(cityNode.getChildren());
            for (Spatial child : children) {
                cityNode.detachChild(child);
                interactableNode.attachChild(child);
            }
        }
    }

    private void addLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        worldNode.addLight(sun);
    }

    private void createCityScene() {
    cityNode = new Node("CityNode");
    cityNode.setCullHint(Node.CullHint.Always);

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

        // ===== ФИЗИКА ГОРОДА =====
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
        // fallback
        createFallbackCity();
    }

    createInteractiveObjects();
    worldNode.attachChild(cityNode);
    System.out.println("[WorldManager] City scene ready.");
}
private void createFallbackCity() {
    Box floorBox = new Box(30, 0.1f, 30);
    Geometry floor = new Geometry("CityFloor", floorBox);
    floor.setName("Ground");
    Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", new ColorRGBA(0.3f, 0.35f, 0.4f, 1.0f));
    floor.setMaterial(mat);
    floor.move(0, -0.05f, 0);
    cityNode.attachChild(floor);
    
    // fallback физика
    if (bulletAppState != null) {
        RigidBodyControl floorPhysics = new RigidBodyControl(0);
        floor.addControl(floorPhysics);
        bulletAppState.getPhysicsSpace().add(floorPhysics);
        System.out.println("[WorldManager] Fallback floor physics added.");
    }
}
    private void createInteractiveObjects() {
        // Портал
        Sphere portalSphere = new Sphere(16, 16, 1.5f);
        Geometry portal = new Geometry("Portal", portalSphere);
        portal.setName("Portal");
        Material portalMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        portalMat.setColor("Color", new ColorRGBA(0.2f, 0.6f, 1.0f, 0.7f));
        portal.setMaterial(portalMat);
        portal.move(5, 1.5f, 5);
        cityNode.attachChild(portal);

        // NPC Торговец
        Box npcBox = new Box(0.5f, 1f, 0.5f);
        Geometry npc = new Geometry("NPC_Trader", npcBox);
        npc.setName("NPC_Trader");
        Material npcMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        npcMat.setColor("Color", ColorRGBA.Green);
        npc.setMaterial(npcMat);
        npc.move(-5, 0.5f, 5);
        cityNode.attachChild(npc);

        // Телепорт
        Box teleportBox = new Box(1f, 0.5f, 0.5f);
        Geometry teleport = new Geometry("Teleport", teleportBox);
        teleport.setName("Teleport");
        Material teleportMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        teleportMat.setColor("Color", new ColorRGBA(0.8f, 0.2f, 0.8f, 1.0f));
        teleport.setMaterial(teleportMat);
        teleport.move(0, 0.25f, 8);
        cityNode.attachChild(teleport);

        // Тестовый монстр (красный куб)
        Box monsterBox = new Box(0.8f, 1.2f, 0.8f);
        Geometry monster = new Geometry("TestMonster", monsterBox);
        monster.setName("TestMonster");
        Material monsterMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        monsterMat.setColor("Color", ColorRGBA.Red);
        monster.setMaterial(monsterMat);
        monster.move(5, 0.6f, -5);
        MonsterData md = new MonsterData(monster, 30, 10);
        monsters.add(md);
        cityNode.attachChild(monster);
    }

    private void createDungeonScene() {
        dungeonNode = new Node("DungeonNode");
        dungeonNode.setCullHint(Node.CullHint.Always);
        Box floorBox = new Box(20, 0.1f, 20);
        Geometry floor = new Geometry("DungeonFloor", floorBox);
        floor.setName("Ground");
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.2f, 0.15f, 0.1f, 1.0f));
        floor.setMaterial(mat);
        floor.move(0, -0.05f, 0);
        dungeonNode.attachChild(floor);
        worldNode.attachChild(dungeonNode);
        System.out.println("[WorldManager] Dungeon created");
    }

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
        Vector3f spawnPos = new Vector3f(0f, 0f, 5f);
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

        Node targetNode = (interactableNode != null) ? interactableNode : cityNode;

        if (model != null) {
            model.setName("Monster");
            System.out.println("[WorldManager] Model loaded successfully!");
            model.scale(2.0f);
            model.move(0, 0.5f, 0);
            skeleton.setModelNode((Node) model);
            targetNode.attachChild(model);
            System.out.println("[WorldManager] Model attached to " + (interactableNode != null ? "interactableNode" : "cityNode"));
        } else {
            System.err.println("[WorldManager] Model is NULL! Creating placeholder...");
            Geometry box = new Geometry("Stub", new Box(0.5f, 1f, 0.3f));
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Red);
            box.setMaterial(mat);
            box.setLocalTranslation(spawnPos);
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

        if (interactableNode != null && interactableNode.getCullHint() != Node.CullHint.Always) {
            for (Spatial child : interactableNode.getChildren()) {
                String name = child.getName();
                if (name == null) continue;
                if (name.equals("NPC_Trader") || name.equals("TestMonster") || name.equals("Monster")) {
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
        if (interactableNode != null) {
            interactableNode.setCullHint(Node.CullHint.Dynamic);
        }
        currentState = GameState.CITY;
        spawnTestMonster();
        System.out.println("[WorldManager] Switched to City");
    }

    public void switchToDungeon() {
        cityNode.setCullHint(Node.CullHint.Always);
        dungeonNode.setCullHint(Node.CullHint.Dynamic);
        if (interactableNode != null) {
            interactableNode.setCullHint(Node.CullHint.Always);
        }
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
                if (interactableNode != null) {
                    interactableNode.setCullHint(Node.CullHint.Always);
                }
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
        if (dungeonNode != null) {
            worldNode.detachChild(dungeonNode);
            dungeonNode = null;
        }
        dungeonNode = new Node("DungeonNode_" + dungeonId);
        worldNode.attachChild(dungeonNode);

        dungeonManager.clearMonsters();
        activeMonsters.clear();

        Dungeon dungeon = dungeonLoader.loadDungeon("dungeons/" + dungeonId + ".json");
        if (dungeon != null) {
            dungeonLoader.setPlayerManager(playerManager);
            dungeonLoader.setDropManager(dropManager);
            List<Monster> newMonsters = dungeonLoader.spawnDungeon(dungeon, dungeonNode);
            activeMonsters.addAll(newMonsters);
            for (Monster m : newMonsters) {
                dungeonManager.addMonster(m);
            }
            dungeonManager.setCurrentDungeon(dungeon);
        }
        switchToDungeon();
    }

    public void cleanup() {
        app.getRootNode().detachChild(worldNode);
        System.out.println("[WorldManager] Cleanup");
    }

    // Геттеры
    public Node getWorldNode() { return worldNode; }
    public Node getCityNode() { return cityNode; }
    public Node getDungeonNode() { return dungeonNode; }
    public Node getInteractableNode() { return interactableNode; }
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