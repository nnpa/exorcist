package com.mygame.dungeons;

import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Представляет данж со списком спавнов монстров.
 */
public class Dungeon {
    private String id;
    private String name;
    private int level;
    private List<MonsterSpawn> spawns = new ArrayList<>();

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public List<MonsterSpawn> getSpawns() { return spawns; }
    public void setSpawns(List<MonsterSpawn> spawns) { this.spawns = spawns; }

    /**
     * Внутренний класс для описания спавна монстра.
     */
    public static class MonsterSpawn {
        public String monsterClass; // полное имя класса, например "com.mygame.monsters.SkeletonWarrior"
        public float x;
        public float y;
        public float z;

        public Vector3f getPosition() {
            return new Vector3f(x, y, z);
        }
    }
}