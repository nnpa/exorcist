package com.mygame.items;

import com.jme3.math.ColorRGBA;

public enum ItemRarity {
    COMMON("Common", ColorRGBA.White),
    UNCOMMON("Uncommon", ColorRGBA.Green),
    RARE("Rare", ColorRGBA.Blue),
    EPIC("Epic", ColorRGBA.Magenta),    // сетовые (фиолетовые)
    LEGENDARY("Legendary", ColorRGBA.Orange);

    private final String displayName;
    private final ColorRGBA color;

    ItemRarity(String displayName, ColorRGBA color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public ColorRGBA getColor() { return color; }
}