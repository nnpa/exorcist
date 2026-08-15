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
import com.mygame.Main;
import com.mygame.items.Item;
import com.mygame.items.ItemGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InventoryManager {

    private SimpleApplication app;
    private Node guiNode;
    private Node inventoryNode;
    private List<Spatial> uiElements = new ArrayList<>();
    private boolean isVisible = false;
    private Label tooltipLabel;
    private List<Button> slotButtons = new ArrayList<>();
    private UIManager uiManager;
    private NetworkManager networkManager;

    private Item[] inventoryItems = new Item[20]; 
    private Item[] equipment = new Item[7];

    private float currentScreenWidth = 1280;
    private float currentScreenHeight = 720;
    private float scale = 1f;

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
        Main main = (Main) app;
        if (main != null) {
            this.networkManager = main.getNetworkManager();
        }
        inventoryNode = new Node("InventoryNode");
        inventoryNode.setName("InventoryNode");
        
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        isVisible = false;
    }

    public void setUIManager(UIManager ui) {
        this.uiManager = ui;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public Node getNode() {
        return inventoryNode;
    }

    // ================================================================
    //   ЗАГРУЗКА С СЕРВЕРА (ИСПРАВЛЕН КЛЮЧ equipped_slot)
    // ================================================================
    public void loadFromServerData(List<Map<String, Object>> inventoryData) {
        System.out.println("==================================================");
        System.out.println("[InventoryManager] loadFromServerData: received " + (inventoryData != null ? inventoryData.size() : "null") + " items from server");
        System.out.println("==================================================");

        Arrays.fill(inventoryItems, null);
        Arrays.fill(equipment, null);

        if (inventoryData != null && !inventoryData.isEmpty()) {
            for (Map<String, Object> data : inventoryData) {
                try {
                    System.out.println("--------------------------------------------------");
                    System.out.println("[InventoryManager] Processing data. Keys: " + data.keySet());

                    // 1. Получаем слот
                    Object slotObj = data.get("slot");
                    int slot = -1;
                    if (slotObj instanceof Number) {
                        slot = ((Number) slotObj).intValue();
                    } else if (slotObj instanceof String) {
                        try {
                            slot = Integer.parseInt((String) slotObj);
                        } catch (Exception e) { /* ignore */ }
                    }

                    // 2. Получаем статус экипировки
                    Object eqObj = data.get("equipped");
                    boolean equipped = false;
                    if (eqObj instanceof Boolean) equipped = (Boolean) eqObj;
                    else if (eqObj instanceof String) equipped = Boolean.parseBoolean((String) eqObj);
                    else if (eqObj instanceof Number) equipped = ((Number) eqObj).intValue() != 0;

                    // 3. ИСПРАВЛЕНИЕ: Ищем equipped_slot (как в JSON) ИЛИ equippedSlot
                    Object eqSlotObj = data.get("equipped_slot");
                    if (eqSlotObj == null) eqSlotObj = data.get("equippedSlot");
                    String equippedSlot = (eqSlotObj != null) ? String.valueOf(eqSlotObj) : null;

                    // 4. Парсим предмет
                    Object itemMapObj = data.get("item");
                    Item item = null;
                    if (itemMapObj instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) itemMapObj;
                        System.out.println("[InventoryManager] Transferring itemMap to Item.fromMap: " + itemMap.keySet());
                        item = Item.fromMap(itemMap);
                        
                        if (item == null) {
                            System.err.println("[InventoryManager] ERROR: Item.fromMap returned null! Item will be skipped.");
                        }
                    } else {
                        System.err.println("[InventoryManager] Key 'item' is missing or not a Map! Received: " + itemMapObj);
                    }

                    // 5. Раскладываем предмет
                    if (item != null) {
                        if (equipped) {
                            int index = getEquipIndexBySlot(equippedSlot);
                            if (index != -1) {
                                equipment[index] = item;
                                System.out.println("[SUCCESS] Equipped into slot " + equippedSlot + ": " + item.getName());
                            } else {
                                System.err.println("[ERROR] Unknown equipment slot: " + equippedSlot);
                            }
                        } else {
                            if (slot >= 0 && slot < inventoryItems.length) {
                                inventoryItems[slot] = item;
                                System.out.println("[SUCCESS] Placed into inventory slot " + slot + ": " + item.getName());
                            } else {
                                System.err.println("[ERROR] Inventory slot out of bounds: " + slot);
                            }
                        }
                    } else {
                        System.err.println("[WARNING] Item skipped (item == null).");
                    }

                } catch (Exception e) {
                    System.err.println("[CRITICAL ERROR] Cannot process inventory item: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("[WARNING] Server sent an empty inventory list!");
        }

        System.out.println("==================================================");
        System.out.println("[TOTAL] Items in inventory: " + countItems() + ", in equipment: " + countEquipment());
        System.out.println("==================================================");
        updateUI();
    }
    
    private int countItems() { int c = 0; for (Item i : inventoryItems) if (i != null) c++; return c; }
    private int countEquipment() { int c = 0; for (Item i : equipment) if (i != null) c++; return c; }

    private int getEquipIndexBySlot(String slot) {
        if (slot == null) return -1;
        switch (slot.toLowerCase().trim()) {
            case "helmet": return 0;
            case "chest": return 1;
            case "weapon": return 2;
            case "shield": return 3;
            case "legs": return 4;
            case "boots": return 5;
            case "gloves": return 6;
            default: return -1;
        }
    }

    // ================================================================
    //   ТЕСТОВЫЕ МЕТОДЫ И UI
    // ================================================================
    public void loadTestItems() {
        Arrays.fill(inventoryItems, null);
        Arrays.fill(equipment, null);
        inventoryItems[0] = ItemGenerator.generateItem(1, "Weapon", 1);
        inventoryItems[1] = ItemGenerator.generateItem(1, "Helmet", 1);
        inventoryItems[2] = ItemGenerator.generateItem(1, "Chest", 1);
        inventoryItems[3] = ItemGenerator.generateItem(1, "Shield", 1);
        inventoryItems[4] = ItemGenerator.generateItem(1, "Legs", 1);
        inventoryItems[5] = ItemGenerator.generateItem(1, "Boots", 1);
        inventoryItems[6] = ItemGenerator.generateItem(1, "Gloves", 1);
        updateUI();
    }

    public void addItem(Item item) {
        if (item == null) return;
        for (int i = 0; i < inventoryItems.length; i++) {
            if (inventoryItems[i] == null) {
                inventoryItems[i] = item;
                updateUI();
                return;
            }
        }
        System.err.println("[InventoryManager] Inventory is full!");
    }

    public void removeItem(Item item) {
        if (item == null) return;
        for (int i = 0; i < inventoryItems.length; i++) {
            if (inventoryItems[i] == item) {
                inventoryItems[i] = null;
                updateUI();
                return;
            }
        }
    }

    public List<Item> getItems() {
        List<Item> result = new ArrayList<>();
        for (Item it : inventoryItems) {
            if (it != null) result.add(it);
        }
        return result;
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

    public void updateLayout(int screenWidth, int screenHeight) {
        currentScreenWidth = screenWidth;
        currentScreenHeight = screenHeight;
        if (isVisible) {
            clearUI();
            createUI(screenWidth, screenHeight);
            setVisible(true);
        }
    }

    private void clearUI() {
        inventoryNode.detachAllChildren();
        uiElements.clear();
        slotButtons.clear();
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

        float eqX = startX;
        float eqY = startY;
        float invX = startX + eqWidth + spacing;
        float invY = startY;

        if (uiManager != null) {
            Geometry eqBg = uiManager.createBackgroundGeometry(eqWidth, eqHeight);
            eqBg.setLocalTranslation(eqX, eqY, -0.1f);
            inventoryNode.attachChild(eqBg);
            uiElements.add(eqBg);

            Geometry invBg = uiManager.createBackgroundGeometry(invWidth, invHeight);
            invBg.setLocalTranslation(invX, invY, -0.1f);
            inventoryNode.attachChild(invBg);
            uiElements.add(invBg);
        }

        float slotSize = 60 * scale;
        float offsetY = 70 * scale;

        createSlot(eqX + 95 * scale, eqY + 320 * scale + offsetY, 0, slotSize);
        createSlot(eqX + 160 * scale, eqY + 250 * scale + offsetY, 2, slotSize);
        createSlot(eqX + 30 * scale, eqY + 250 * scale + offsetY, 3, slotSize);
        createSlot(eqX + 95 * scale, eqY + 180 * scale + offsetY, 1, slotSize);
        createSlot(eqX + 95 * scale, eqY + 110 * scale + offsetY, 4, slotSize);
        createSlot(eqX + 95 * scale, eqY + 40 * scale + offsetY, 5, slotSize);
        createSlot(eqX + 30 * scale, eqY + 110 * scale + offsetY, 6, slotSize);

        Button closeEq = new Button("Close");
        closeEq.setFontSize(16 * scale);
        closeEq.setLocalTranslation(eqX + 80 * scale, eqY + 30 * scale, 0);
        closeEq.addClickCommands((source) -> hide());
        inventoryNode.attachChild(closeEq);
        uiElements.add(closeEq);

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

            Item item = inventoryItems[i];

            if (item != null) {
                Texture tex = null;
                try {
                    tex = app.getAssetManager().loadTexture(item.getIconPath());
                } catch (Exception e) {
                }
                if (tex != null) {
                    cell.setBackground(new QuadBackgroundComponent(tex));
                    cell.setText("");
                } else {
                    cell.setBackground(new QuadBackgroundComponent(item.getFallbackColor()));
                    cell.setText(item.getName().substring(0, 1));
                    cell.setFontSize(20 * scale);
                    cell.setColor(ColorRGBA.Black);
                }
            } else {
                cell.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f)));
                cell.setText("");
            }
            cell.setLocalTranslation(x, y, 0);
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
        closeInv.setLocalTranslation(invX + invWidth / 2 - 40 * scale, invY + 20 * scale, 0);
        closeInv.addClickCommands((source) -> hide());
        inventoryNode.attachChild(closeInv);
        uiElements.add(closeInv);

        tooltipLabel = new Label("");
        tooltipLabel.setFontSize(14 * scale);
        tooltipLabel.setColor(ColorRGBA.White);
        tooltipLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f)));
        tooltipLabel.setPreferredSize(new Vector3f(280 * scale, 100 * scale, 0));
        tooltipLabel.setLocalTranslation(10 * scale, 160 * scale, 10f);
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
            } catch (Exception e) {
            }
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
            String iconPath = EMPTY_SLOT_ICONS[slotIndex];
            Texture emptyTex = null;
            try {
                emptyTex = app.getAssetManager().loadTexture(iconPath);
            } catch (Exception e) {
            }
            if (emptyTex != null) {
                slotBg = new QuadBackgroundComponent(emptyTex);
                slot.setText("");
            } else {
                slotBg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
                slot.setText("+");
                slot.setFontSize(18 * scale);
                slot.setColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
            }
        }
        slot.setBackground(slotBg);
        slot.setLocalTranslation(x, y, 0);
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
                if (isInventory && index < inventoryItems.length && inventoryItems[index] != null) {
                    Item item = inventoryItems[index];
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
        if (item == null) return "";
        return item.getName() + "\n" +
                item.getRarity().getDisplayName() + " (Lv." + item.getLevel() + ")\n" +
                "Damage: " + item.getDamage() + ", Defense: " + item.getDefense() + "\n" +
                item.getDescription();
    }

   private void handleInventoryClick(int index) {
        if (!isVisible) return;
        if (index < 0 || index >= inventoryItems.length) return;
        Item item = inventoryItems[index];
        if (item == null) return;
        
        if (networkManager != null) {
            System.out.println("[InventoryManager] Sending equip request for inventory slot " + index);
            networkManager.equipItem(index).thenAccept(response -> {
                app.enqueue(() -> {
                    if (response != null && uiManager != null) {
                        System.out.println("[InventoryManager] Equip successful. Updating UI.");
                        uiManager.applyCharacterData(response);
                    } else {
                        System.err.println("[InventoryManager] Equip failed: response is null (server rejected the action).");
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> {
                    System.err.println("[InventoryManager] Equip exception: " + ex.getMessage());
                    ex.printStackTrace();
                });
                return null;
            });
        }
    }

private void handleEquipmentClick(int slotIndex) {
    if (!isVisible || equipment[slotIndex] == null) return;
    if (networkManager != null) {
        networkManager.unequipItem(slotIndex).thenAccept(response -> {
            app.enqueue(() -> {
                if (response != null && uiManager != null) {
                    uiManager.applyCharacterData(response);
                } else {
                    requestInventoryRefresh(); // обновить UI при ошибке
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> requestInventoryRefresh());
            return null;
        });
    }
}
public void requestInventoryRefresh() {
    if (networkManager != null) {
        networkManager.loadCharacterData().thenAccept(data -> {
            app.enqueue(() -> {
                if (data != null && uiManager != null) {
                    uiManager.applyCharacterData(data);
                }
            });
        });
    }
}
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
        if (isVisible) hide();
        else show();
    }

    public void updateUI() {
        clearUI();
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        if (isVisible) {
            setVisible(true);
        }
    }

    public void cleanup() {
        if (guiNode.hasChild(inventoryNode)) {
            guiNode.detachChild(inventoryNode);
        }
    }
}