package com.mygame.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootTable {

    private List<LootEntry> entries = new ArrayList<>();
    private Random random = new Random();

    public void addEntry(Item item, float chance) {
        entries.add(new LootEntry(item, chance));
    }

    // Основной метод дропа – теперь принимает difficulty
public List<Item> rollForLoot(int difficulty) {
    List<Item> dropped = new ArrayList<>();
    for (LootEntry entry : entries) {
        if (random.nextFloat() < entry.chance) {
            dropped.add(entry.item);
        }
    }
    return dropped;
}

    // Перегрузка без параметров для обратной совместимости (использует difficulty = 1)
    public List<Item> rollForLoot() {
        return rollForLoot(1);
    }

    // Генерирует таблицу для уровня с учётом difficulty
    public static LootTable generateForLevel(int level, int difficulty) {
        LootTable table = new LootTable();
        int count = 1 + new Random().nextInt(3);
        for (int i = 0; i < count; i++) {
            Item item = ItemGenerator.generateItem(level, "Weapon", difficulty);
            table.addEntry(item, 0.3f + new Random().nextFloat() * 0.4f);
        }
        return table;
    }

    // Перегрузка для обратной совместимости
    public static LootTable generateForLevel(int level) {
        return generateForLevel(level, 1);
    }

    private static class LootEntry {
        Item item;
        float chance;
        LootEntry(Item item, float chance) {
            this.item = item;
            this.chance = chance;
        }
    }
}