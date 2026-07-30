package com.mygame.dungeons;

import com.google.gson.Gson;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.scene.Node;
import com.mygame.managers.DropManager;
import com.mygame.managers.PlayerManager;
import com.mygame.monsters.Monster;
import com.mygame.monsters.MonsterFactory;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DungeonLoader {

    private SimpleApplication app;
    private Gson gson = new Gson();
    private DropManager dropManager;
    private PlayerManager playerManager;

    public DungeonLoader(SimpleApplication app) {
        this.app = app;
    }

    public void setDropManager(DropManager dm) { this.dropManager = dm; }
    public void setPlayerManager(PlayerManager pm) { this.playerManager = pm; }

    public Dungeon loadDungeon(String path) {
        try {
            AssetKey<Object> key = new AssetKey<>(path);
            AssetInfo info = app.getAssetManager().locateAsset(key);
            if (info == null) {
                System.err.println("[DungeonLoader] Asset not found: " + path);
                return null;
            }
            InputStreamReader reader = new InputStreamReader(info.openStream());
            return gson.fromJson(reader, Dungeon.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Monster> spawnDungeon(Dungeon dungeon, Node worldNode) {
        List<Monster> spawned = new ArrayList<>();
        if (dungeon == null || worldNode == null) return spawned;

        for (Dungeon.MonsterSpawn spawn : dungeon.getSpawns()) {
            try {
                Monster monster = MonsterFactory.createMonster(spawn.monsterClass);
                if (monster == null) continue;

                monster.setSpawnPosition(spawn.getPosition());
                monster.setDropManager(dropManager);
                if (playerManager != null) {
                    monster.setPlayerManager(playerManager);
                }

                // Загружаем модель
                try {
                    Node model = (Node) app.getAssetManager().loadModel("Models/Monsters/" + monster.getId() + ".gltf");
                    if (model != null) {
                        monster.setModelNode(model);
                        worldNode.attachChild(model);
                    } else {
                        System.err.println("[DungeonLoader] Model not found for " + monster.getId());
                        continue;
                    }
                } catch (Exception e) {
                    System.err.println("[DungeonLoader] Error loading model for " + monster.getId());
                    e.printStackTrace();
                    continue;
                }

                spawned.add(monster);
            } catch (Exception e) {
                System.err.println("[DungeonLoader] Error spawning monster: " + spawn.monsterClass);
                e.printStackTrace();
            }
        }
        return spawned;
    }
}