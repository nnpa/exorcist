package com.mygame.monsters;

import com.mygame.items.Item;
import com.mygame.items.ItemRarity;
import com.mygame.items.LootTable;
import com.mygame.items.ItemGenerator;

public class SkeletonWarrior extends MeleMonster {

    public SkeletonWarrior() {
        setId("skeleton_warrior");
        setName("Skeleton Warrior");
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

loot.addEntry(ItemGenerator.generateRandomGem(diff), 1.05f);
loot.addEntry(ItemGenerator.generateRandomGem(diff), 1.05f);

loot.addEntry(ItemGenerator.generateRandomGem(diff), 1.05f);

        setLootTable(loot);
    }
}