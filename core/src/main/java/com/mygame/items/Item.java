package com.mygame.items;

import com.jme3.math.ColorRGBA;
import java.util.ArrayList;
import java.util.List;

public class Item {
    private String id;
    private String name;
    private String type;
    private int level;
    private ItemRarity rarity;
    private String description;
    private int damage;
    private int defense;
    private String iconPath;
    private int socketCount;
    private List<String> runes;
    private ColorRGBA fallbackColor;

    public Item() {
        this.runes = new ArrayList<>();
        this.socketCount = 0;
        this.fallbackColor = generateRandomColor();
    }

    public Item(String id, String name, String type, int level, ItemRarity rarity, String description,
                int damage, int defense, String iconPath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.level = level;
        this.rarity = rarity;
        this.description = description;
        this.damage = damage;
        this.defense = defense;
        this.iconPath = iconPath;
        this.socketCount = 0;
        this.runes = new ArrayList<>();
        this.fallbackColor = generateRandomColor();
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public ItemRarity getRarity() { return rarity; }
    public void setRarity(ItemRarity rarity) { this.rarity = rarity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = defense; }

    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }

    public int getSocketCount() { return socketCount; }
    public void setSocketCount(int socketCount) { this.socketCount = socketCount; }

    public List<String> getRunes() { return runes; }
    public void setRunes(List<String> runes) { this.runes = runes; }

    public ColorRGBA getFallbackColor() {
        if (fallbackColor == null) {
            fallbackColor = generateRandomColor();
        }
        return fallbackColor;
    }
    public void setFallbackColor(ColorRGBA color) { this.fallbackColor = color; }

    public ColorRGBA getColor() {
        return rarity != null ? rarity.getColor() : ColorRGBA.White;
    }

    private ColorRGBA generateRandomColor() {
        float r = 0.3f + (float) Math.random() * 0.7f;
        float g = 0.3f + (float) Math.random() * 0.7f;
        float b = 0.3f + (float) Math.random() * 0.7f;
        return new ColorRGBA(r, g, b, 1f);
    }
}