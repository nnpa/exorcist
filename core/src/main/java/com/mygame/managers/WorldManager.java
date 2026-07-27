package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.mygame.managers.GameManager.GameState;

import java.util.ArrayList;
import java.util.List;

public class WorldManager {

    private SimpleApplication app;
    private Node worldNode;
    private Node cityNode;
    private Node dungeonNode;
    private Node interactableNode; // <-- новый узел для всех интерактивных объектов

    private List<MonsterData> monsters = new ArrayList<>();

    private GameState currentState = GameState.LOADING;
    private PlayerManager playerManager;

    public WorldManager(SimpleApplication app) {
        this.app = app;
        this.worldNode = new Node("WorldNode");
    }

    public void initialize() {
        System.out.println("[WorldManager] Initializing...");
        app.getRootNode().attachChild(worldNode);
        addLighting();
        createCityScene();
        createDungeonScene();
    }

    // ===== НОВЫЙ МЕТОД: установка узла для интерактивных объектов =====
    public void setInteractableNode(Node node) {
        this.interactableNode = node;
        // Если город уже создан, переносим все объекты из cityNode в interactableNode
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

        // Пол
        Box floorBox = new Box(30, 0.1f, 30);
        Geometry floor = new Geometry("CityFloor", floorBox);
        floor.setName("Ground"); // <-- добавляем имя для идентификации
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.3f, 0.35f, 0.4f, 1.0f));
        floor.setMaterial(mat);
        floor.move(0, -0.05f, 0);

        // Портал
        Sphere portalSphere = new Sphere(16, 16, 1.5f);
        Geometry portal = new Geometry("Portal", portalSphere);
        Material portalMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        portalMat.setColor("Color", new ColorRGBA(0.2f, 0.6f, 1.0f, 0.7f));
        portal.setMaterial(portalMat);
        portal.move(5, 1.5f, 5);

        // NPC Торговец
        Box npcBox = new Box(0.5f, 1f, 0.5f);
        Geometry npc = new Geometry("NPC_Trader", npcBox);
        npc.setName("NPC_Trader");
        Material npcMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        npcMat.setColor("Color", ColorRGBA.Green);
        npc.setMaterial(npcMat);
        npc.move(-5, 0.5f, 5);
        System.out.println("[WorldManager] NPC_Trader created at position: " + npc.getWorldTranslation());

        // Телепорт
        Box teleportBox = new Box(1f, 0.5f, 0.5f);
        Geometry teleport = new Geometry("Teleport", teleportBox);
        teleport.setName("Teleport");
        Material teleportMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        teleportMat.setColor("Color", new ColorRGBA(0.8f, 0.2f, 0.8f, 1.0f));
        teleport.setMaterial(teleportMat);
        teleport.move(0, 0.25f, 8);

        // Тестовый монстр
        Box monsterBox = new Box(0.8f, 1.2f, 0.8f);
        Geometry monster = new Geometry("TestMonster", monsterBox);
        monster.setName("TestMonster");
        Material monsterMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        monsterMat.setColor("Color", ColorRGBA.Red);
        monster.setMaterial(monsterMat);
        monster.move(5, 0.6f, -5);
        MonsterData md = new MonsterData(monster, 30, 10);
        monsters.add(md);

        // ===== ВСЕ ОБЪЕКТЫ ДОБАВЛЯЕМ В interactableNode ИЛИ cityNode =====
        if (interactableNode != null) {
            interactableNode.attachChild(floor);
            interactableNode.attachChild(portal);
            interactableNode.attachChild(npc);
            interactableNode.attachChild(teleport);
            interactableNode.attachChild(monster);
        } else {
            // fallback — добавляем в cityNode
            cityNode.attachChild(floor);
            cityNode.attachChild(portal);
            cityNode.attachChild(npc);
            cityNode.attachChild(teleport);
            cityNode.attachChild(monster);
        }

        worldNode.attachChild(cityNode);
        System.out.println("[WorldManager] City created");
    }

    private void createDungeonScene() {
        dungeonNode = new Node("DungeonNode");
        dungeonNode.setCullHint(Node.CullHint.Always);
        Box floorBox = new Box(20, 0.1f, 20);
        Geometry floor = new Geometry("DungeonFloor", floorBox);
        floor.setName("Ground"); // тоже имя, чтобы идентифицировать
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.2f, 0.15f, 0.1f, 1.0f));
        floor.setMaterial(mat);
        floor.move(0, -0.05f, 0);
        dungeonNode.attachChild(floor);
        worldNode.attachChild(dungeonNode);
        System.out.println("[WorldManager] Dungeon created");
    }

    public MonsterData getMonsterByGeometry(Geometry geom) {
        for (MonsterData md : monsters) {
            if (md.geom == geom) {
                return md;
            }
        }
        return null;
    }

    // ===== МЕТОД ДЛЯ ПОИСКА БЛИЖАЙШЕГО ОБЪЕКТА (уже не используется в новом подходе, но оставим) =====
    public Spatial getClosestInteractiveObject(Vector3f point, float radius) {
        System.out.println("[WorldManager] Searching interactive objects near " + point + " with radius " + radius);
        Spatial closest = null;
        float closestDist = radius;
        // Проверяем только если interactableNode не null и видим
        if (interactableNode != null && interactableNode.getCullHint() != Node.CullHint.Always) {
            for (Spatial child : interactableNode.getChildren()) {
                String name = child.getName();
                if (name == null) continue;
                // Ищем только монстров и NPC (исключаем пол, портал и т.д.)
                if (name.equals("NPC_Trader") || name.equals("TestMonster")) {
                    float dist = child.getWorldTranslation().distance(point);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = child;
                    }
                }
            }
        }
        if (closest != null) {
            System.out.println("[WorldManager] Found: " + closest.getName() + " at distance: " + closestDist);
        } else {
            System.out.println("[WorldManager] Nothing found within radius " + radius);
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
        System.out.println("[WorldManager] Switched to City");
    }

    public void switchToDungeon() {
        cityNode.setCullHint(Node.CullHint.Always);
        dungeonNode.setCullHint(Node.CullHint.Dynamic);
        if (interactableNode != null) {
            interactableNode.setCullHint(Node.CullHint.Dynamic);
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
        // логика ИИ монстров (можно добавить позже)
    }

    public void cleanup() {
        app.getRootNode().detachChild(worldNode);
        System.out.println("[WorldManager] Cleanup");
    }

    public Node getWorldNode() { return worldNode; }
    public Node getCityNode() { return cityNode; }
    public Node getDungeonNode() { return dungeonNode; }
    public Node getInteractableNode() { return interactableNode; }

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