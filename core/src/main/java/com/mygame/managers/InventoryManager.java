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

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {

    private SimpleApplication app;
    private Node guiNode;
    private Node inventoryNode; // корневой узел для всего UI инвентаря
    private List<Spatial> uiElements = new ArrayList<>();
    private boolean isVisible = false;
    private Label tooltipLabel;
    private List<Button> slotButtons = new ArrayList<>();
    private UIManager uiManager;

    private List<Item> inventoryItems = new ArrayList<>();
    private Item[] equipment = new Item[7];

    private static final String[] EMPTY_SLOT_ICONS = {
        "Interface/Icons/empty_helmet.png",
        "Interface/Icons/empty_armor.png",
        "Interface/Icons/empty_weapon.png",
        "Interface/Icons/empty_shield.png",
        "Interface/Icons/empty_legs.png",
        "Interface/Icons/empty_boots.png",
        "Interface/Icons/empty_gloves.png"
    };

    private float currentScreenWidth = 1280;
    private float currentScreenHeight = 720;
    private float scale = 1f;

    public InventoryManager(SimpleApplication app, Node guiNode) {
        this.app = app;
        this.guiNode = guiNode;
        inventoryNode = new Node("InventoryNode");
        inventoryNode.setName("InventoryNode");
        generateTestItems();
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        setVisible(false);
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
        // Добавляем разнообразные предметы для теста
        inventoryItems.add(new Item("Iron Helmet", "Helmet", 1, ColorRGBA.White, "+5 defense"));
        inventoryItems.add(new Item("Leather Armor", "Armor", 1, ColorRGBA.White, "+10 defense"));
        inventoryItems.add(new Item("Steel Sword", "Weapon", 1, ColorRGBA.White, "+8 attack"));
        inventoryItems.add(new Item("Wooden Shield", "Shield", 1, ColorRGBA.White, "+6 defense"));
        inventoryItems.add(new Item("Leather Leggings", "Leggings", 1, ColorRGBA.White, "+4 defense"));
        inventoryItems.add(new Item("Leather Boots", "Boots", 1, ColorRGBA.White, "+3 defense"));
        inventoryItems.add(new Item("Leather Gloves", "Gloves", 1, ColorRGBA.White, "+2 defense"));
        inventoryItems.add(new Item("Health Potion", "Consumable", 1, ColorRGBA.Green, "Restores 50 HP"));
        inventoryItems.add(new Item("Mana Potion", "Consumable", 1, ColorRGBA.Blue, "Restores 30 MP"));
        inventoryItems.add(new Item("Ring of Power", "Accessory", 1, ColorRGBA.Orange, "+10 attack"));
    }

    private void ensureCreated() {
        if (!uiElements.isEmpty()) return;
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
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
        // Убеждаемся, что узел пуст перед добавлением
        inventoryNode.detachAllChildren();
        uiElements.clear();

        float baseWidth = 800f;
        float baseHeight = 600f;
        float scaleX = screenWidth / baseWidth;
        float scaleY = screenHeight / baseHeight;
        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));

        float eqWidth = 250 * scale;
        float eqHeight = 450 * scale;
        float invWidth = 400 * scale;
        float invHeight = 350 * scale;
        float spacing = 30 * scale;

        float totalWidth = eqWidth + spacing + invWidth;
        float startX = (screenWidth - totalWidth) / 2;
        float startY = (screenHeight - (eqHeight + invHeight) / 2) / 2 + 80 * scale;

        // Экипировка
        float eqX = startX;
        float eqY = startY;

        Geometry eqBg = createBackground(eqX, eqY, eqWidth, eqHeight, new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f));
        eqBg.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(eqBg);
        uiElements.add(eqBg);

        Label eqTitle = new Label("Equipment");
        eqTitle.setFontSize(20 * scale);
        eqTitle.setColor(ColorRGBA.White);
        eqTitle.setLocalTranslation(eqX + eqWidth/2 - 50*scale, eqY + eqHeight - 20*scale, 0);
        eqTitle.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(eqTitle);
        uiElements.add(eqTitle);

        float slotSize = 60 * scale;
        float offsetY = 70 * scale;
        createSlot(eqX + 95*scale, eqY + 320*scale + offsetY, "Helmet", 0, slotSize);
        createSlot(eqX + 160*scale, eqY + 250*scale + offsetY, "Weapon", 2, slotSize);
        createSlot(eqX + 30*scale, eqY + 250*scale + offsetY, "Shield", 3, slotSize);
        createSlot(eqX + 95*scale, eqY + 180*scale + offsetY, "Armor", 1, slotSize);
        createSlot(eqX + 95*scale, eqY + 110*scale + offsetY, "Leggings", 4, slotSize);
        createSlot(eqX + 95*scale, eqY + 40*scale + offsetY, "Boots", 5, slotSize);
        createSlot(eqX + 30*scale, eqY + 110*scale + offsetY, "Gloves", 6, slotSize);

        Button closeEq = new Button("Close");
        closeEq.setFontSize(16 * scale);
        closeEq.setLocalTranslation(eqX + 80*scale, eqY + 30*scale, 0);
        closeEq.setCullHint(Node.CullHint.Always);
        closeEq.addClickCommands((source) -> hideWindows());
        inventoryNode.attachChild(closeEq);
        uiElements.add(closeEq);

        // Инвентарь
        float invX = startX + eqWidth + spacing;
        float invY = startY;

        Geometry invBg = createBackground(invX, invY, invWidth, invHeight, new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f));
        invBg.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(invBg);
        uiElements.add(invBg);

        Label invTitle = new Label("Inventory");
        invTitle.setFontSize(20 * scale);
        invTitle.setColor(ColorRGBA.White);
        invTitle.setLocalTranslation(invX + invWidth/2 - 50*scale, invY + invHeight - 20*scale, 0);
        invTitle.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(invTitle);
        uiElements.add(invTitle);

        // Ячейки инвентаря (4x5)
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
                cell.setText(item.type.substring(0, 1));
                QuadBackgroundComponent itemBg = new QuadBackgroundComponent(item.color);
                cell.setBackground(itemBg);
            } else {
                QuadBackgroundComponent cellBg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
                cell.setBackground(cellBg);
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
        closeInv.addClickCommands((source) -> hideWindows());
        inventoryNode.attachChild(closeInv);
        uiElements.add(closeInv);

        // Tooltip
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

    private void createSlot(float x, float y, String slotName, int slotIndex, float slotSize) {
        Label slotLabel = new Label(slotName);
        slotLabel.setFontSize(12 * scale);
        slotLabel.setColor(ColorRGBA.White);
        slotLabel.setLocalTranslation(x + 10*scale, y + 25*scale, 0);
        slotLabel.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(slotLabel);
        uiElements.add(slotLabel);

        Button slot = new Button("");
        slot.setPreferredSize(new Vector3f(slotSize, slotSize, 0));
        slot.setColor(ColorRGBA.White);
        QuadBackgroundComponent slotBg;

        if (equipment[slotIndex] != null) {
            Item item = equipment[slotIndex];
            slot.setText(item.type.substring(0, 1));
            slotBg = new QuadBackgroundComponent(item.color);
        } else {
            String iconPath = EMPTY_SLOT_ICONS[slotIndex];
            Texture emptyTex = null;
            try {
                emptyTex = app.getAssetManager().loadTexture(iconPath);
            } catch (Exception e) {}
            if (emptyTex != null) {
                slotBg = new QuadBackgroundComponent(emptyTex);
                slot.setText("");
            } else {
                slot.setText("+");
                slot.setFontSize(18 * scale);
                slot.setColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
                slotBg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
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
                    text = item.name + "\n" + item.description;
                } else if (!isInventory && equipment[index] != null) {
                    Item item = equipment[index];
                    text = item.name + "\n" + item.description;
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

    // ===== УПРАВЛЕНИЕ ВИДИМОСТЬЮ =====
    private void setVisible(boolean visible) {
        isVisible = visible;
        // Управляем через добавление/удаление из guiNode
        if (visible) {
            if (!guiNode.hasChild(inventoryNode)) {
                guiNode.attachChild(inventoryNode);
                System.out.println("[InventoryManager] Node attached to guiNode");
            }
        } else {
            if (guiNode.hasChild(inventoryNode)) {
                guiNode.detachChild(inventoryNode);
                System.out.println("[InventoryManager] Node detached from guiNode");
            }
        }
        // Скрываем/показываем внутренние элементы через cull (но они уже внутри inventoryNode)
        for (Spatial s : uiElements) {
            s.setCullHint(visible ? Node.CullHint.Dynamic : Node.CullHint.Always);
        }
        if (!visible && tooltipLabel != null) {
            tooltipLabel.setCullHint(Node.CullHint.Always);
        }
    }

    private void clearUI() {
        // При перестроении очищаем inventoryNode
        inventoryNode.detachAllChildren();
        uiElements.clear();
        slotButtons.clear();
    }

    public void show() {
        ensureCreated();
        isVisible = true;
        if (uiManager != null) {
            uiManager.onInventoryOpened(inventoryNode);
        } else {
            setVisible(true);
        }
        // При открытии пересоздаём UI (чтобы обновить содержимое)
        updateUI();
    }

    public void hide() {
        isVisible = false;
        if (uiManager != null) {
            uiManager.onInventoryClosed(inventoryNode);
        } else {
            setVisible(false);
        }
        if (tooltipLabel != null) {
            tooltipLabel.setCullHint(Node.CullHint.Always);
        }
    }

    public void toggleVisibility() {
        ensureCreated();
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }

    public void hideWindows() {
        hide();
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
        switch (item.type) {
            case "Helmet": return 0;
            case "Armor": return 1;
            case "Weapon": return 2;
            case "Shield": return 3;
            case "Leggings": return 4;
            case "Boots": return 5;
            case "Gloves": return 6;
            default: return -1;
        }
    }

    public void addItem(Item item) {
        inventoryItems.add(item);
        updateUI();
        System.out.println("[InventoryManager] Added: " + item.name);
    }

    public List<Item> getItems() {
        return inventoryItems;
    }

    public void removeItem(Item item) {
        inventoryItems.remove(item);
        updateUI();
        System.out.println("[InventoryManager] Removed: " + item.name);
    }

    public void updateUI() {
        clearUI();
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        if (isVisible) {
            setVisible(true);
        }
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

    public static class Item {
        public String name;
        public String type;
        public int level;
        public ColorRGBA color;
        public String description;

        public Item(String name, String type, int level, ColorRGBA color) {
            this.name = name;
            this.type = type;
            this.level = level;
            this.color = color;
            this.description = "Level " + level;
        }

        public Item(String name, String type, int level, ColorRGBA color, String description) {
            this.name = name;
            this.type = type;
            this.level = level;
            this.color = color;
            this.description = description;
        }
    }
}