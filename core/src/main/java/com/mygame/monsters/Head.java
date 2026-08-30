package com.mygame.monsters;

import com.mygame.items.Item;
import com.mygame.items.ItemRarity;
import com.mygame.items.LootTable;
import com.mygame.items.ItemGenerator;

public class Head extends MeleMonster {

    public Head() {
        setId("Head");
        setName("Head");
        setLevel(1);
        setMaxHealth(30);
        setHealth(30);
        setDamage(5);
        setAttackRange(1.5f);
        setMoveSpeed(2.0f);
        setAggroRange(8.0f);

        // Таблица дропа – только одеваемые предметы
        LootTable loot = new LootTable();
        
        // Передаём difficulty = 1 для базовых монстров (в реальности сложность будет передаваться из DungeonLoader)
        int diff = 1;
        loot.addEntry(ItemGenerator.generateItem(1, "Weapon", diff), 0.25f);
        loot.addEntry(ItemGenerator.generateItem(1, "Helmet", diff), 0.2f);
        loot.addEntry(ItemGenerator.generateItem(1, "Chest", diff), 0.2f);
        loot.addEntry(ItemGenerator.generateItem(1, "Shield", diff), 0.15f);
        loot.addEntry(ItemGenerator.generateItem(1, "Legs", diff), 0.2f);
        loot.addEntry(ItemGenerator.generateItem(1, "Boots", diff), 0.15f);
        loot.addEntry(ItemGenerator.generateItem(1, "Gloves", diff), 0.15f);

        setLootTable(loot);
    }
}