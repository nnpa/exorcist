package com.mygame.monsters;

import com.mygame.items.Item;
import com.mygame.items.ItemRarity;
import com.mygame.items.LootTable;
import com.mygame.items.ItemGenerator;

public class SkeletonWarrior extends Monster {

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
        
        // Генерируем только стандартные типы: Weapon, Helmet, Chest, Shield, Legs, Boots, Gloves
        // Шансы можно настроить
        loot.addEntry(ItemGenerator.generateItem(1, "Weapon"), 0.25f);
        loot.addEntry(ItemGenerator.generateItem(1, "Helmet"), 0.2f);
        loot.addEntry(ItemGenerator.generateItem(1, "Chest"), 0.2f);
        loot.addEntry(ItemGenerator.generateItem(1, "Shield"), 0.15f);
        loot.addEntry(ItemGenerator.generateItem(1, "Legs"), 0.2f);
        loot.addEntry(ItemGenerator.generateItem(1, "Boots"), 0.15f);
        loot.addEntry(ItemGenerator.generateItem(1, "Gloves"), 0.15f);

        setLootTable(loot);
    }
}