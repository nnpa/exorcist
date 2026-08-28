package com.mygame.dungeons;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.mygame.managers.DropManager;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.WorldManager;
import com.mygame.monsters.Monster;
import com.mygame.monsters.SkeletonWarrior;
import com.mygame.monsters.BossMonster;
import com.mygame.monsters.Demon;
import com.mygame.monsters.Head;
import com.mygame.monsters.Scorpion;
import com.mygame.monsters.Sgolem;
import com.mygame.monsters.Snake;
import com.mygame.monsters.SpiderBoss;
import com.mygame.monsters.WormBoss;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DungeonLoader {

    private static final Logger LOG = Logger.getLogger(DungeonLoader.class.getName());

    private SimpleApplication app;
    private PlayerManager playerManager;
    private DropManager dropManager;
    private WorldManager worldManager;
    private BulletAppState bulletAppState;

    public DungeonLoader(SimpleApplication app) {
        this.app = app;
    }

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
    }

    public void setDropManager(DropManager dm) {
        this.dropManager = dm;
    }

    public void setWorldManager(WorldManager wm) {
        this.worldManager = wm;
    }

    public void setBulletAppState(BulletAppState bas) {
        this.bulletAppState = bas;
    }

    public Dungeon loadDungeon(String filePath) {
        
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
            if (is == null) {
                LOG.warning("File not found: " + filePath);
                return null;
            }
            InputStreamReader reader = new InputStreamReader(is);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<MonsterSpawnData>>(){}.getType();
            List<MonsterSpawnData> spawnDataList = gson.fromJson(reader, listType);
            reader.close();

            String id = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('.'));
            Dungeon dungeon = new Dungeon(id);
            for (MonsterSpawnData data : spawnDataList) {
                dungeon.addSpawn(new Dungeon.MonsterSpawn(
                    data.className,
                    data.x, data.y, data.z,
                    data.level,
                    data.health,
                    data.damage,
                    data.nextDungeon,
                    data.isBoss,
                    data.isFinalBoss,
                    data.increaseDifficulty
                ));
            }
            return dungeon;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error loading dungeon: " + e.getMessage(), e);
            return null;
        }
    }

    public List<Monster> spawnDungeon(Dungeon dungeon, Node parentNode, int difficulty) {
    // Ключевое исправление: сбрасываем трансляцию родителя,
    // чтобы координаты монстров были мировыми, а не относительными.
    parentNode.setLocalTranslation(0, 0, 0);

    List<Monster> spawned = new ArrayList<>();

    createDungeonScene(parentNode, dungeon.getId());

    for (Dungeon.MonsterSpawn spawn : dungeon.getSpawns()) {
        try {
            Monster monster = createMonster(spawn, difficulty);
            if (monster == null) continue;

            Vector3f pos = new Vector3f(spawn.x, spawn.y, spawn.z);
            monster.setSpawnPosition(pos);

            monster.setPlayerManager(playerManager);
            monster.setDropManager(dropManager);
            monster.setWorldManager(worldManager);

            // Загружаем модель
            Spatial model = loadMonsterModel(monster);
            Node modelNode = new Node("MonsterNode_" + monster.getClass().getSimpleName());
            if (model != null) {
                modelNode.attachChild(model);
            } else {
                // Заглушка
                Geometry box = new Geometry("Monster", new Box(0.5f, 0.5f, 0.5f));
                Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", ColorRGBA.Red);
                box.setMaterial(mat);
                modelNode.attachChild(box);
            }

            // Добавляем модель в сцену
            parentNode.attachChild(modelNode);

            // Сохраняем ссылку на модель в монстре и устанавливаем позицию
            monster.setModelNode(modelNode);
            monster.setPosition(pos);

            spawned.add(monster);

            // Отладочный вывод
            System.out.println("[DungeonLoader] " + monster.getName() + " spawned at " + pos +
                    ", world pos: " + modelNode.getWorldTranslation());

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error creating monster: " + e.getMessage(), e);
        }
    }

    return spawned;
}

    private Monster createMonster(Dungeon.MonsterSpawn spawn, int difficulty) {
        try {
            Class<?> clazz = Class.forName(spawn.className);
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            Monster monster = (Monster) ctor.newInstance();

            monster.setLevel(spawn.level);
            float health = spawn.health * difficulty;
            float damage = spawn.damage * difficulty;
            monster.setMaxHealth(health);
            monster.setHealth(health);
            monster.setDamage(damage);

            monster.setBoss(spawn.isBoss);
            monster.setFinalBoss(spawn.isFinalBoss);
            monster.setNextDungeonId(spawn.nextDungeon);
            monster.setIncreaseDifficultyOnDeath(spawn.increaseDifficulty);

            return monster;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Reflection error: " + e.getMessage(), e);
            return null;
        }
    }

    // ===== ЗАГРУЗКА МОДЕЛИ МОНСТРА ПО ЕГО КЛАССУ =====
    private Spatial loadMonsterModel(Monster monster) {
        String modelPath = null;

        // Определяем путь к модели в зависимости от класса монстра
        if (monster instanceof SkeletonWarrior) {
            modelPath = "Models/Monsters/skeleton_warrior.gltf";
        } else if (monster instanceof BossMonster) {
            modelPath = "Models/Monsters/boss_monster.gltf"; // если есть такая модель
        } else if (monster instanceof Demon) {
            modelPath = "Models/Monsters/Demon.gltf"; // если есть такая модель
        }
         else if (monster instanceof Head) {
            modelPath = "Models/Monsters/Head.gltf"; // если есть такая модель
        }
         else if (monster instanceof SpiderBoss) {
            modelPath = "Models/Monsters/SpiderBoss.gltf"; // если есть такая модель
        }
         else if (monster instanceof Scorpion) {
            modelPath = "Models/Monsters/Scorpion.gltf"; // если есть такая модель
        }else if (monster instanceof Snake) {
            modelPath = "Models/Monsters/snake.gltf"; // если есть такая модель
        }
        else if (monster instanceof Sgolem) {
            modelPath = "Models/Monsters/sgolem.gltf"; // если есть такая модель
        }else if (monster instanceof WormBoss) {
            modelPath = "Models/Monsters/wormboss.gltf"; // если есть такая модель
        }else {
            // По умолчанию – скелет
            modelPath = "Models/Monsters/skeleton_warrior.gltf";
        }

        try {
            Spatial model = app.getAssetManager().loadModel(modelPath);
            enableShadows(model);
            if (model != null) {
                // Применяем трансформации, если нужно
                model.scale(2.5f);
                model.move(0, 0.5f, 0);
                return model;
            } else {
                LOG.warning("Model not found for: " + monster.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOG.warning("Error loading model " + modelPath + ": " + e.getMessage());
        }
        return null;
    }
private void enableShadows(Spatial spatial) {

    if (spatial instanceof Geometry) {

        Geometry geometry = (Geometry) spatial;

        geometry.setShadowMode(
                RenderQueue.ShadowMode.CastAndReceive
        );

        System.out.println(
                "[SHADOW] " + geometry.getName()
                + " -> CastAndReceive"
        );

        return;
    }

    if (spatial instanceof Node) {

        Node node = (Node) spatial;

        for (Spatial child : node.getChildren()) {
            enableShadows(child);
        }
    }
}
    private void createDungeonScene(Node parentNode, String dungeonId) {
        // ... (без изменений, как было)
        try {
            Spatial sceneModel = app.getAssetManager().loadModel("Models/Dungeons/" + dungeonId + ".gltf");
           sceneModel.setShadowMode(
             RenderQueue.ShadowMode.Receive
           );
            if (sceneModel != null) {
                if (bulletAppState != null) {
                    CollisionShape shape = CollisionShapeFactory.createMeshShape(sceneModel);
                    RigidBodyControl physics = new RigidBodyControl(shape, 0);
                    sceneModel.addControl(physics);
                    bulletAppState.getPhysicsSpace().add(physics);
                }
                parentNode.attachChild(sceneModel);
                LOG.info("Dungeon scene model loaded for " + dungeonId);
                return;
            }
        } catch (Exception e) {
            LOG.warning("No scene model found for " + dungeonId + ", creating default floor.");
        }

        Box floorBox = new Box(20f, 0.1f, 20f);
        Geometry floor = new Geometry("DungeonFloor", floorBox);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.2f, 0.15f, 0.1f, 1f));
        floor.setMaterial(mat);
        floor.move(0, -0.05f, 0);

        if (bulletAppState != null) {
            RigidBodyControl floorPhysics = new RigidBodyControl(0f);
            floor.addControl(floorPhysics);
            bulletAppState.getPhysicsSpace().add(floorPhysics);
        }

        parentNode.attachChild(floor);
    }

    // Класс для десериализации JSON
    private static class MonsterSpawnData {
        String className;
        float x, y, z;
        int level;
        float health, damage;
        String nextDungeon;
        boolean isBoss;
        boolean isFinalBoss;
        boolean increaseDifficulty;
    }
    
    
}