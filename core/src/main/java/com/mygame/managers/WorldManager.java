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
        
        cityNode.setCullHint(Node.CullHint.Always);
        dungeonNode.setCullHint(Node.CullHint.Always);

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

    // ============================================================
    //   loadCityWithPhysics()
    // ============================================================
    public void loadCityWithPhysics() {
        System.out.println("[WorldManager] Loading city with physics...");
        
        cityNode.detachAllChildren();
        cityNode.setCullHint(Node.CullHint.Dynamic);
        
        createCityScene();
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

        createInteractiveObjects();
        System.out.println("[WorldManager] City scene ready.");
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
        System.out.println("[WorldManager] Portal created");

        // NPC Торговец
        Box npcBox = new Box(0.5f, 1f, 0.5f);
        Geometry npc = new Geometry("NPC_Trader", npcBox);
        npc.setName("NPC_Trader");
        Material npcMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        npcMat.setColor("Color", ColorRGBA.Green);
        npc.setMaterial(npcMat);
        npc.move(-5, 0.5f, 5);
        cityNode.attachChild(npc);
        System.out.println("[WorldManager] NPC Trader created");

        // Телепорт
        Box teleportBox = new Box(1f, 0.5f, 0.5f);
        Geometry teleport = new Geometry("Teleport", teleportBox);
        teleport.setName("Teleport");
        Material teleportMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        teleportMat.setColor("Color", new ColorRGBA(0.8f, 0.2f, 0.8f, 1.0f));
        teleport.setMaterial(teleportMat);
        teleport.move(0, 0.25f, 8);
        cityNode.attachChild(teleport);
        System.out.println("[WorldManager] Teleport created");

        // ===== ТЕСТОВЫЙ МОНСТР (СКЕЛЕТ) =====
        createTestMonster();
    }

    private void createTestMonster() {
        System.out.println("[WorldManager] Creating test monster...");
        
        if (playerManager == null) {
            System.err.println("[WorldManager] PlayerManager is NULL! Cannot create monster.");
            return;
        }
        if (dropManager == null) {
            System.err.println("[WorldManager] DropManager is NULL! Cannot create monster.");
            return;
        }
        
        try {
            // Создаем скелета
            Monster skeleton = new SkeletonWarrior();
            Vector3f spawnPos = new Vector3f(5f, 0f, -5f);
            skeleton.setSpawnPosition(spawnPos);
            skeleton.setPlayerManager(playerManager);
            skeleton.setDropManager(dropManager);
            
            // Загружаем модель
            Spatial model = null;
            try {
                model = app.getAssetManager().loadModel("Models/Monsters/skeleton_warrior.gltf");
            } catch (Exception e) {
                System.err.println("[WorldManager] Cannot load skeleton model: " + e.getMessage());
            }
            
            Node targetNode = cityNode;
            
            if (model != null) {
                model.setName("Monster");
                model.scale(2.0f);
                model.move(0, 0.5f, 0);
                skeleton.setModelNode((Node) model);
                targetNode.attachChild(model);
                System.out.println("[WorldManager] Skeleton model loaded and attached");
            } else {
                // Создаем заглушку - красный куб
                System.out.println("[WorldManager] Creating placeholder for skeleton");
                Geometry box = new Geometry("MonsterPlaceholder", new Box(0.5f, 1f, 0.3f));
                Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", ColorRGBA.Red);
                box.setMaterial(mat);
                box.setLocalTranslation(spawnPos);
                box.setName("TestMonster");
                
                Node stubNode = new Node("StubNode");
                stubNode.attachChild(box);
                skeleton.setModelNode(stubNode);
                targetNode.attachChild(stubNode);
                System.out.println("[WorldManager] Skeleton placeholder created");
            }
            
            // ===== ВАЖНО: ДОБАВЛЯЕМ В СПИСОК АКТИВНЫХ МОНСТРОВ =====
            activeMonsters.add(skeleton);
            testMonsterSpawned = true;
            
            // Также добавляем в список для отслеживания геометрии
            if (skeleton.getModelNode() != null) {
                // Ищем геометрию для MonsterData
                Spatial modelNode = skeleton.getModelNode();
                if (modelNode instanceof Node) {
                    for (Spatial child : ((Node) modelNode).getChildren()) {
                        if (child instanceof Geometry) {
                            MonsterData md = new MonsterData((Geometry) child, 30, 10);
                            monsters.add(md);
                            break;
                        }
                    }
                }
            }
            
            System.out.println("[WorldManager] Test Monster spawned at " + spawnPos);
            
        } catch (Exception e) {
            System.err.println("[WorldManager] Error creating test monster: " + e.getMessage());
            e.printStackTrace();
        }
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

        for (MonsterData md : monsters) {
            if (md.isDead) continue;
            if (md.geom == null) continue;
            float dist = md.geom.getWorldTranslation().distance(point);
            if (dist < closestDist) {
                closestDist = dist;
                closest = md.geom;
            }
        }

        if (cityNode != null) {
            for (Spatial child : cityNode.getChildren()) {
                String name = child.getName();
                if (name == null) continue;
                if (name.equals("NPC_Trader") || name.equals("TestMonster") || name.equals("Monster") || name.equals("Portal") || name.equals("Teleport")) {
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
        currentState = GameState.CITY;
        System.out.println("[WorldManager] Switched to City - Active monsters: " + activeMonsters.size());
    }

    public void switchToDungeon() {
        cityNode.setCullHint(Node.CullHint.Always);
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
        // Реализация загрузки данжа
    }

    public void cleanup() {
        app.getRootNode().detachChild(worldNode);
        System.out.println("[WorldManager] Cleanup");
    }

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