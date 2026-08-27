package com.mygame.items;

import com.mygame.managers.LocalizationManager;

import java.util.*;

public class ItemGenerator {
    private static final Random random = new Random();

    private static final String[] PREFIX_KEYS = {
        "item.prefix.0", "item.prefix.1", "item.prefix.2",
        "item.prefix.3", "item.prefix.4", "item.prefix.5",
        "item.prefix.6", "item.prefix.7", "item.prefix.8", "item.prefix.9"
    };
    private static final String[] WEAPON_SUFFIX_KEYS = {
        "item.suffix.weapon.0", "item.suffix.weapon.1", "item.suffix.weapon.2",
        "item.suffix.weapon.3", "item.suffix.weapon.4", "item.suffix.weapon.5",
        "item.suffix.weapon.6", "item.suffix.weapon.7", "item.suffix.weapon.8", "item.suffix.weapon.9"
    };
    private static final Map<String, String[]> SUFFIX_KEYS_BY_TYPE = new HashMap<>();
    static {
        SUFFIX_KEYS_BY_TYPE.put("Weapon", WEAPON_SUFFIX_KEYS);
        SUFFIX_KEYS_BY_TYPE.put("Helmet", new String[]{"item.suffix.helmet.0", "item.suffix.helmet.1", "item.suffix.helmet.2"});
        SUFFIX_KEYS_BY_TYPE.put("Chest", new String[]{"item.suffix.chest.0", "item.suffix.chest.1", "item.suffix.chest.2"});
        SUFFIX_KEYS_BY_TYPE.put("Shield", new String[]{"item.suffix.shield.0", "item.suffix.shield.1", "item.suffix.shield.2"});
        SUFFIX_KEYS_BY_TYPE.put("Legs", new String[]{"item.suffix.legs.0", "item.suffix.legs.1", "item.suffix.legs.2"});
        SUFFIX_KEYS_BY_TYPE.put("Boots", new String[]{"item.suffix.boots.0", "item.suffix.boots.1", "item.suffix.boots.2"});
        SUFFIX_KEYS_BY_TYPE.put("Gloves", new String[]{"item.suffix.gloves.0", "item.suffix.gloves.1", "item.suffix.gloves.2"});
    }

    private static final List<String> VALID_TYPES = Arrays.asList(
        "Weapon", "Helmet", "Chest", "Shield", "Legs", "Boots", "Gloves"
    );

    private static final Map<String, List<String>> ICONS_BY_TYPE = new HashMap<>();
    static {
        for (String type : VALID_TYPES) {
            List<String> icons = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                icons.add(i + ".png");
            }
            ICONS_BY_TYPE.put(type, icons);
        }
    }

    private static String getLocalizedOrFallback(String key, String fallback) {
        String value = LocalizationManager.getInstance().get(key);
        if (value == null || value.startsWith("???")) {
            return fallback;
        }
        return value;
    }

    public static Item generateItem(int playerLevel, String type, int difficulty) {
        if (!VALID_TYPES.contains(type)) {
            type = VALID_TYPES.get(random.nextInt(VALID_TYPES.size()));
        }

        int level = playerLevel;
        int baseDamage = 0, baseDefense = 0;
        int baseHealthBonus = 0, baseManaBonus = 0;

        switch (type) {
            case "Weapon":
                baseDamage = 5 + level * 2;
                break;
            case "Helmet":
                baseDefense = 3 + level;
                baseHealthBonus = 2 + level;
                break;
            case "Chest":
                baseDefense = 5 + level * 2;
                baseHealthBonus = 4 + level * 2;
                baseManaBonus = 2 + level;
                break;
            case "Shield":
                baseDefense = 4 + level;
                baseHealthBonus = 3 + level;
                break;
            case "Legs":
                baseDefense = 2 + level;
                baseHealthBonus = 1 + level;
                break;
            case "Boots":
                baseDefense = 1 + level;
                baseHealthBonus = 1 + level;
                break;
            case "Gloves":
                baseDefense = 1 + level;
                baseManaBonus = 1 + level;
                break;
        }

        float bonusPercent = -10 + random.nextFloat() * 30;
        int finalDamage = (int)(baseDamage * (1 + bonusPercent / 100f));
        int finalDefense = (int)(baseDefense * (1 + bonusPercent / 100f));
        finalDamage = Math.max(0, finalDamage);
        finalDefense = Math.max(0, finalDefense);

        ItemRarity rarity;
        if (bonusPercent >= 15) rarity = ItemRarity.EPIC;
        else if (bonusPercent >= 5) rarity = ItemRarity.RARE;
        else if (bonusPercent >= -2) rarity = ItemRarity.UNCOMMON;
        else rarity = ItemRarity.COMMON;

        if (playerLevel >= 50 && random.nextFloat() < 0.05f) rarity = ItemRarity.EPIC;
        if (playerLevel >= 50 && random.nextFloat() < 0.02f) rarity = ItemRarity.LEGENDARY;

        int effectiveDifficulty = Math.max(1, difficulty);
        finalDamage *= effectiveDifficulty;
        finalDefense *= effectiveDifficulty;
        baseHealthBonus *= effectiveDifficulty;
        baseManaBonus *= effectiveDifficulty;

        String name = generateLocalizedName(type);
        String iconPath = selectIcon(type);
        String desc = "";
        String id = UUID.randomUUID().toString();

        Item item = new Item(id, name, type, level, rarity, desc, finalDamage, finalDefense, iconPath);
        item.setSocketCount(random.nextInt(3));
        item.setHealthBonus(baseHealthBonus);
        item.setManaBonus(baseManaBonus);
        item.setDifficulty(difficulty);

        return item;
    }

    private static String generateLocalizedName(String type) {
        // Выбираем случайный префикс
        String prefixKey = PREFIX_KEYS[random.nextInt(PREFIX_KEYS.length)];
        // Если ключ отсутствует в словаре, используем "Old" как fallback
        String prefix = getLocalizedOrFallback(prefixKey, "Old");

        // Суффикс зависит от типа
        String[] suffixKeys = SUFFIX_KEYS_BY_TYPE.get(type);
        if (suffixKeys == null) {
            suffixKeys = new String[]{"item.suffix.default.0", "item.suffix.default.1", "item.suffix.default.2"};
        }
        String suffixKey = suffixKeys[random.nextInt(suffixKeys.length)];
        String suffix = getLocalizedOrFallback(suffixKey, "Item");

        return prefix + " " + suffix;
    }

    private static String selectIcon(String type) {
        List<String> icons = ICONS_BY_TYPE.get(type);
        if (icons == null || icons.isEmpty()) return "default.png";
        String fileName = icons.get(random.nextInt(icons.size()));
        return "Icons/Items/" + type + "/" + fileName;
    }

    public static List<Item> generateDrop(int playerLevel, int count, int difficulty) {
        List<Item> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String type = VALID_TYPES.get(random.nextInt(VALID_TYPES.size()));
            result.add(generateItem(playerLevel, type, difficulty));
        }
        return result;
    }
}