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

    public List<Item> rollForLoot() {
        List<Item> dropped = new ArrayList<>();
        for (LootEntry entry : entries) {
            if (random.nextFloat() < entry.chance) {
                dropped.add(entry.item);
            }
        }
        return dropped;
    }

    public static LootTable generateForLevel(int level) {
        LootTable table = new LootTable();
        // Генерируем 1-3 предмета с разными шансами
        int count = 1 + new Random().nextInt(3);
        for (int i = 0; i < count; i++) {
            Item item = ItemGenerator.generateItem(level, "Weapon");
            table.addEntry(item, 0.3f + new Random().nextFloat() * 0.4f);
        }
        return table;
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