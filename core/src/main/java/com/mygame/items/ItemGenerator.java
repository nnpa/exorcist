package com.mygame.items;

import java.util.*;

public class ItemGenerator {

    private static final Random random = new Random();

    private static final String[] PREFIXES = {"Old", "Rusty", "Sharp", "Heavy", "Light", "Sturdy", "Magic", "Enchanted", "Ancient", "Dark"};
private static final String[] WEAPON_SUFFIXES = {"Sword", "Blade", "Sabre", "Rapier", "Claymore", "Katana", "Scimitar", "Falchion", "Longsword", "Shortsword"};

    private static final List<String> VALID_TYPES = Arrays.asList(
        "Weapon", "Helmet", "Chest", "Shield", "Legs", "Boots", "Gloves"
    );

    // ===== СПИСОК ИКОНОК ДЛЯ КАЖДОГО ТИПА (9 иконок с именами 1.png ... 9.png) =====
    private static final Map<String, List<String>> ICONS_BY_TYPE = new HashMap<>();
    static {
        // Для каждого типа генерируем список от 1 до 9
        for (String type : VALID_TYPES) {
            List<String> icons = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                icons.add(i + ".png");
            }
            ICONS_BY_TYPE.put(type, icons);
        }
    }

    /**
     * Генерирует предмет заданного типа. Если тип неизвестен, генерирует случайный одеваемый предмет.
     */
    public static Item generateItem(int playerLevel, String type) {
        // Если тип не в списке допустимых, выбираем случайный
        if (!VALID_TYPES.contains(type)) {
            System.out.println("[ItemGenerator] Unknown type: " + type + ", generating random wearable item");
            type = VALID_TYPES.get(random.nextInt(VALID_TYPES.size()));
        }

        int level = playerLevel;
        int baseDamage = 0, baseDefense = 0;
        switch (type) {
            case "Weapon": baseDamage = 5 + level * 2; break;
            case "Helmet": baseDefense = 3 + level; break;
            case "Chest": baseDefense = 5 + level * 2; break;
            case "Shield": baseDefense = 4 + level; break;
            case "Legs": baseDefense = 2 + level; break;
            case "Boots": baseDefense = 1 + level; break;
            case "Gloves": baseDefense = 1 + level; break;
        }

        float bonusPercent = -10 + random.nextFloat() * 30;
        int finalDamage = (int)(baseDamage * (1 + bonusPercent / 100f));
        int finalDefense = (int)(baseDefense * (1 + bonusPercent / 100f));
        if (finalDamage < 0) finalDamage = 0;
        if (finalDefense < 0) finalDefense = 0;

        ItemRarity rarity;
        if (bonusPercent >= 15) rarity = ItemRarity.EPIC;
        else if (bonusPercent >= 5) rarity = ItemRarity.RARE;
        else if (bonusPercent >= -2) rarity = ItemRarity.UNCOMMON;
        else rarity = ItemRarity.COMMON;

        if (playerLevel >= 50 && random.nextFloat() < 0.05f) rarity = ItemRarity.EPIC;
        if (playerLevel >= 50 && random.nextFloat() < 0.02f) rarity = ItemRarity.LEGENDARY;

        String name = generateName(type);
        String iconPath = selectIcon(type);
        String desc = String.format("Level %d, Damage: %d, Defense: %d", level, finalDamage, finalDefense);
        String id = UUID.randomUUID().toString();

        Item item = new Item(id, name, type, level, rarity, desc, finalDamage, finalDefense, iconPath);
        item.setSocketCount(random.nextInt(3));
        return item;
    }

    private static String generateName(String type) {
        String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
        String suffix;
        switch (type) {
            case "Weapon": suffix = WEAPON_SUFFIXES[random.nextInt(WEAPON_SUFFIXES.length)]; break;
            case "Helmet": suffix = "Helmet"; break;
            case "Chest": suffix = "Chestplate"; break;
            case "Shield": suffix = "Shield"; break;
            case "Legs": suffix = "Leggings"; break;
            case "Boots": suffix = "Boots"; break;
            case "Gloves": suffix = "Gloves"; break;
            default: suffix = "Item"; // никогда не должно сработать
        }
        return prefix + " " + suffix;
    }

    /**
     * Выбирает случайную иконку из папки типа (1.png ... 9.png).
     * Путь: Icons/Items/Weapon/1.png
     */
    private static String selectIcon(String type) {
        List<String> icons = ICONS_BY_TYPE.get(type);
        if (icons == null || icons.isEmpty()) return "default.png";
        String fileName = icons.get(random.nextInt(icons.size()));
        // Путь к папке с иконками (относительно assets)
        return "Icons/Items/" + type + "/" + fileName;
    }

    /**
     * Генерирует список случайных одеваемых предметов (без зелий и мусора).
     */
    public static List<Item> generateDrop(int playerLevel, int count) {
        List<Item> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String type = VALID_TYPES.get(random.nextInt(VALID_TYPES.size()));
            result.add(generateItem(playerLevel, type));
        }
        return result;
    }
}