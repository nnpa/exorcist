package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;
import com.mygame.items.Item;
import com.mygame.items.ItemGenerator;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {

    private SimpleApplication app;
    private Node guiNode;
    private Node inventoryNode;
    private List<Spatial> uiElements = new ArrayList<>();
    private boolean isVisible = false;
    private Label tooltipLabel;
    private List<Button> slotButtons = new ArrayList<>();
    private UIManager uiManager;

    private List<Item> inventoryItems = new ArrayList<>();
    private Item[] equipment = new Item[7]; // 0-Helmet, 1-Chest, 2-Weapon, 3-Shield, 4-Legs, 5-Boots, 6-Gloves

    private float currentScreenWidth = 1280;
    private float currentScreenHeight = 720;
    private float scale = 1f;

    // Пустые иконки для слотов (если есть)
    private static final String[] EMPTY_SLOT_ICONS = {
        "Interface/Icons/empty_helmet.png",
        "Interface/Icons/empty_armor.png",
        "Interface/Icons/empty_weapon.png",
        "Interface/Icons/empty_shield.png",
        "Interface/Icons/empty_legs.png",
        "Interface/Icons/empty_boots.png",
        "Interface/Icons/empty_gloves.png"
    };

    public InventoryManager(SimpleApplication app, Node guiNode) {
        this.app = app;
        this.guiNode = guiNode;
        inventoryNode = new Node("InventoryNode");
        inventoryNode.setName("InventoryNode");
        generateTestItems();
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        isVisible = false;
    }

    public void setUIManager(UIManager ui) {
        this.uiManager = ui;
    }

    public Node getNode() {
        return inventoryNode;
    }

    public boolean isVisible() {
        return isVisible;
    }

    private void updateScreenSize() {
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        if (w > 0 && h > 0) {
            currentScreenWidth = w;
            currentScreenHeight = h;
        }
        float baseWidth = 800f;
        float baseHeight = 600f;
        float scaleX = currentScreenWidth / baseWidth;
        float scaleY = currentScreenHeight / baseHeight;
        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));
    }

    private void generateTestItems() {
        // Генерируем несколько случайных предметов для демонстрации
        for (int i = 0; i < 8; i++) {
            Item item = ItemGenerator.generateItem(1, "Weapon");
            inventoryItems.add(item);
        }
        for (int i = 0; i < 5; i++) {
            Item item = ItemGenerator.generateItem(1, "Helmet");
            inventoryItems.add(item);
        }
        inventoryItems.add(ItemGenerator.generateItem(1, "Chest"));
        inventoryItems.add(ItemGenerator.generateItem(1, "Shield"));
        inventoryItems.add(ItemGenerator.generateItem(1, "Legs"));
        inventoryItems.add(ItemGenerator.generateItem(1, "Boots"));
        inventoryItems.add(ItemGenerator.generateItem(1, "Gloves"));
    }

    private Geometry createBackground(float x, float y, float w, float h, ColorRGBA color) {
        Quad quad = new Quad(w, h);
        Geometry bg = new Geometry("Bg", quad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        bg.setMaterial(mat);
        bg.setLocalTranslation(x, y, 0);
        return bg;
    }

    private void createUI(float screenWidth, float screenHeight) {
        inventoryNode.detachAllChildren();
        uiElements.clear();

        float eqWidth = 250 * scale;
        float eqHeight = 450 * scale;
        float invWidth = 400 * scale;
        float invHeight = 350 * scale;
        float spacing = 30 * scale;

        float totalWidth = eqWidth + spacing + invWidth;
        float startX = (screenWidth - totalWidth) / 2;
        float startY = (screenHeight - (eqHeight + invHeight) / 2) / 2 + 80 * scale;

        // ===== ЭКИПИРОВКА =====
        float eqX = startX;
        float eqY = startY;

        Geometry eqBg = createBackground(eqX, eqY, eqWidth, eqHeight, new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f));
        eqBg.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(eqBg);
        uiElements.add(eqBg);

        float slotSize = 60 * scale;
        float offsetY = 70 * scale;

        createSlot(eqX + 95*scale, eqY + 320*scale + offsetY, 0, slotSize);
        createSlot(eqX + 160*scale, eqY + 250*scale + offsetY, 2, slotSize);
        createSlot(eqX + 30*scale, eqY + 250*scale + offsetY, 3, slotSize);
        createSlot(eqX + 95*scale, eqY + 180*scale + offsetY, 1, slotSize);
        createSlot(eqX + 95*scale, eqY + 110*scale + offsetY, 4, slotSize);
        createSlot(eqX + 95*scale, eqY + 40*scale + offsetY, 5, slotSize);
        createSlot(eqX + 30*scale, eqY + 110*scale + offsetY, 6, slotSize);

        Button closeEq = new Button("Close");
        closeEq.setFontSize(16 * scale);
        closeEq.setLocalTranslation(eqX + 80*scale, eqY + 30*scale, 0);
        closeEq.setCullHint(Node.CullHint.Always);
        closeEq.addClickCommands((source) -> hide());
        inventoryNode.attachChild(closeEq);
        uiElements.add(closeEq);

        // ===== ИНВЕНТАРЬ =====
        float invX = startX + eqWidth + spacing;
        float invY = startY;

        Geometry invBg = createBackground(invX, invY, invWidth, invHeight, new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f));
        invBg.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(invBg);
        uiElements.add(invBg);

        float cellSize = 55 * scale;
        float spacingCell = 8 * scale;
        float startXCell = invX + 20 * scale;
        float startYCell = invY + invHeight - 20 * scale;

        for (int i = 0; i < 20; i++) {
            int col = i % 4;
            int row = i / 4;
            float x = startXCell + col * (cellSize + spacingCell);
            float y = startYCell - row * (cellSize + spacingCell);
            if (y < 0) y = 0;
            Button cell = new Button("");
            cell.setPreferredSize(new Vector3f(cellSize, cellSize, 0));
            cell.setColor(ColorRGBA.White);
            if (i < inventoryItems.size()) {
                Item item = inventoryItems.get(i);
                Texture tex = null;
                try {
                    tex = app.getAssetManager().loadTexture(item.getIconPath());
                } catch (Exception e) {}
                if (tex != null) {
                    cell.setBackground(new QuadBackgroundComponent(tex));
                    cell.setText("");
                } else {
                    // Заглушка: цвет + первая буква
                    cell.setBackground(new QuadBackgroundComponent(item.getFallbackColor()));
                    cell.setText(item.getName().substring(0, 1));
                    cell.setFontSize(20 * scale);
                    cell.setColor(ColorRGBA.Black);
                }
            } else {
                // Пустая ячейка
                cell.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f)));
                cell.setText("");
            }
            cell.setLocalTranslation(x, y, 0);
            cell.setCullHint(Node.CullHint.Always);
            final int idx = i;
            cell.addClickCommands((source) -> {
                if (!isVisible) return;
                handleInventoryClick(idx);
            });
            addTooltipListener(cell, idx, true);
            inventoryNode.attachChild(cell);
            uiElements.add(cell);
            slotButtons.add(cell);
        }

        Button closeInv = new Button("Close");
        closeInv.setFontSize(16 * scale);
        closeInv.setLocalTranslation(invX + invWidth/2 - 40*scale, invY + 20*scale, 0);
        closeInv.setCullHint(Node.CullHint.Always);
        closeInv.addClickCommands((source) -> hide());
        inventoryNode.attachChild(closeInv);
        uiElements.add(closeInv);

        tooltipLabel = new Label("");
        tooltipLabel.setFontSize(14 * scale);
        tooltipLabel.setColor(ColorRGBA.White);
        tooltipLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f)));
        tooltipLabel.setPreferredSize(new Vector3f(250 * scale, 60 * scale, 0));
        tooltipLabel.setLocalTranslation(10 * scale, screenHeight - 80 * scale, 0);
        tooltipLabel.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(tooltipLabel);
        uiElements.add(tooltipLabel);
    }

    private void createSlot(float x, float y, int slotIndex, float slotSize) {
        Button slot = new Button("");
        slot.setPreferredSize(new Vector3f(slotSize, slotSize, 0));
        slot.setColor(ColorRGBA.White);
        QuadBackgroundComponent slotBg;

        if (equipment[slotIndex] != null) {
            Item item = equipment[slotIndex];
            Texture tex = null;
            try {
                tex = app.getAssetManager().loadTexture(item.getIconPath());
            } catch (Exception e) {}
            if (tex != null) {
                slotBg = new QuadBackgroundComponent(tex);
                slot.setText("");
            } else {
                slotBg = new QuadBackgroundComponent(item.getFallbackColor());
                slot.setText(item.getName().substring(0, 1));
                slot.setFontSize(18 * scale);
                slot.setColor(ColorRGBA.Black);
            }
        } else {
            // Пустой слот – пытаемся загрузить иконку, если есть, иначе плюс
            String iconPath = EMPTY_SLOT_ICONS[slotIndex];
            Texture emptyTex = null;
            try {
                emptyTex = app.getAssetManager().loadTexture(iconPath);
            } catch (Exception e) {}
            if (emptyTex != null) {
                slotBg = new QuadBackgroundComponent(emptyTex);
                slot.setText("");
            } else {
                // Цветная заглушка с плюсом
                slotBg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
                slot.setText("+");
                slot.setFontSize(18 * scale);
                slot.setColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
            }
        }
        slot.setBackground(slotBg);
        slot.setLocalTranslation(x, y, 0);
        slot.setCullHint(Node.CullHint.Always);
        final int idx = slotIndex;
        slot.addClickCommands((source) -> {
            if (!isVisible) return;
            handleEquipmentClick(idx);
        });
        addTooltipListener(slot, idx, false);
        inventoryNode.attachChild(slot);
        uiElements.add(slot);
        slotButtons.add(slot);
    }

    private void addTooltipListener(Button btn, int index, boolean isInventory) {
        MouseEventControl.removeListenersFromSpatial(btn);
        MouseListener listener = new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {}

            @Override
            public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {
                if (!isVisible) return;
                String text = "";
                if (isInventory && index < inventoryItems.size()) {
                    Item item = inventoryItems.get(index);
                    text = buildTooltip(item);
                } else if (!isInventory && equipment[index] != null) {
                    Item item = equipment[index];
                    text = buildTooltip(item);
                }
                if (!text.isEmpty() && tooltipLabel != null) {
                    tooltipLabel.setText(text);
                    tooltipLabel.setCullHint(Node.CullHint.Dynamic);
                }
            }

            @Override
            public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {
                if (tooltipLabel != null) {
                    tooltipLabel.setCullHint(Node.CullHint.Always);
                }
            }

            @Override
            public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
        };
        MouseEventControl.addListenersToSpatial(btn, listener);
    }

    private String buildTooltip(Item item) {
        return item.getName() + "\n" +
                item.getRarity().getDisplayName() + " (Lv." + item.getLevel() + ")\n" +
                "Урон: " + item.getDamage() + ", Защита: " + item.getDefense() + "\n" +
                item.getDescription();
    }

    // ===== УПРАВЛЕНИЕ ВИДИМОСТЬЮ =====
    private void setVisible(boolean visible) {
        isVisible = visible;
        if (visible) {
            if (!guiNode.hasChild(inventoryNode)) {
                guiNode.attachChild(inventoryNode);
            }
            if (uiManager != null) uiManager.onInventoryOpened(inventoryNode);
        } else {
            if (guiNode.hasChild(inventoryNode)) {
                guiNode.detachChild(inventoryNode);
            }
            if (uiManager != null) uiManager.onInventoryClosed(inventoryNode);
        }
        for (Spatial s : uiElements) {
            s.setCullHint(visible ? Node.CullHint.Dynamic : Node.CullHint.Always);
        }
        if (!visible && tooltipLabel != null) {
            tooltipLabel.setCullHint(Node.CullHint.Always);
        }
    }

    public void show() {
        updateUI();
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    public void toggleVisibility() {
        if (isVisible) hide(); else show();
    }

    // ===== ОБРАБОТЧИКИ КЛИКОВ =====
    private void handleInventoryClick(int index) {
        if (!isVisible) return;
        if (index < inventoryItems.size()) {
            Item item = inventoryItems.get(index);
            equipItem(item);
        }
    }

    private void handleEquipmentClick(int slotIndex) {
        if (!isVisible) return;
        if (equipment[slotIndex] != null) {
            inventoryItems.add(equipment[slotIndex]);
            equipment[slotIndex] = null;
            updateUI();
        }
    }

    private void equipItem(Item item) {
        int slot = getSlotForItem(item);
        if (slot == -1) return;
        if (equipment[slot] != null) {
            inventoryItems.add(equipment[slot]);
        }
        equipment[slot] = item;
        inventoryItems.remove(item);
        updateUI();
    }

    private int getSlotForItem(Item item) {
        String type = item.getType();
        switch (type) {
            case "Helmet": return 0;
            case "Chest": return 1;
            case "Weapon": return 2;
            case "Shield": return 3;
            case "Legs": return 4;
            case "Boots": return 5;
            case "Gloves": return 6;
            default:
                System.out.println("[InventoryManager] Unknown item type: " + type);
                return -1;
        }
    }

    public void addItem(Item item) {
        inventoryItems.add(item);
        updateUI();
        System.out.println("[InventoryManager] Added: " + item.getName());
    }

    public List<Item> getItems() {
        return inventoryItems;
    }

    public void removeItem(Item item) {
        inventoryItems.remove(item);
        updateUI();
        System.out.println("[InventoryManager] Removed: " + item.getName());
    }

    public void updateUI() {
        clearUI();
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        if (isVisible) {
            setVisible(true);
        }
    }

    private void clearUI() {
        inventoryNode.detachAllChildren();
        uiElements.clear();
        slotButtons.clear();
    }

    public void updateLayout(int screenWidth, int screenHeight) {
        currentScreenWidth = screenWidth;
        currentScreenHeight = screenHeight;
        if (isVisible) {
            clearUI();
            createUI(screenWidth, screenHeight);
            setVisible(true);
        }
    }

    public void cleanup() {
        if (guiNode.hasChild(inventoryNode)) {
            guiNode.detachChild(inventoryNode);
        }
    }
}