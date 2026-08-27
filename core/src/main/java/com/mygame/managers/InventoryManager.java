package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
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
    private List<Spatial> slotSpaces = new ArrayList<>();
    private UIManager uiManager;
    private NetworkManager networkManager;

    private Item[] inventoryItems = new Item[20];
    private Item[] equipment = new Item[7];

    private float currentScreenWidth = 1280;
    private float currentScreenHeight = 720;
    private float scale = 1f;
    private boolean isProcessing = false;

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
    //   ЗАГРУЗКА С СЕРВЕРА
    // ================================================================
    public void loadFromServerData(List<Map<String, Object>> inventoryData) {
        System.out.println("==================================================");
        System.out.println("[InventoryManager] loadFromServerData: received " + (inventoryData != null ? inventoryData.size() : "null") + " items from server");
        System.out.println("==================================================");

        Arrays.fill(inventoryItems, null);
        Arrays.fill(equipment, null);

        if (inventoryData == null || inventoryData.isEmpty()) {
            System.out.println("[InventoryManager] No inventory data to load.");
            updateUI();
            return;
        }

        for (Map<String, Object> data : inventoryData) {
            try {
                System.out.println("--------------------------------------------------");
                System.out.println("[InventoryManager] Processing data. Keys: " + data.keySet());

                // ---- Извлечение слота ----
                int slot = -1;
                Object slotObj = data.get("slot");
                if (slotObj instanceof Number) {
                    slot = ((Number) slotObj).intValue();
                } else if (slotObj instanceof String) {
                    try {
                        slot = Integer.parseInt((String) slotObj);
                    } catch (NumberFormatException e) {
                        System.err.println("[InventoryManager] Invalid slot format: " + slotObj);
                    }
                }

                // ---- Извлечение equipped ----
                boolean equipped = false;
                Object eqObj = data.get("equipped");
                if (eqObj instanceof Boolean) {
                    equipped = (Boolean) eqObj;
                } else if (eqObj instanceof String) {
                    equipped = Boolean.parseBoolean((String) eqObj);
                } else if (eqObj instanceof Number) {
                    equipped = ((Number) eqObj).intValue() != 0;
                }

                // ---- Извлечение equipped_slot ----
                String equippedSlot = null;
                Object eqSlotObj = data.get("equipped_slot");
                if (eqSlotObj == null) {
                    eqSlotObj = data.get("equippedSlot");
                }
                if (eqSlotObj != null) {
                    equippedSlot = eqSlotObj.toString();
                }

                // ---- Извлечение и создание Item ----
                Object itemMapObj = data.get("item");
                Item item = null;
                if (itemMapObj instanceof Map) {
                    Map<String, Object> itemMap = (Map<String, Object>) itemMapObj;
                    System.out.println("[InventoryManager] Item map keys: " + itemMap.keySet());
                    item = Item.fromMap(itemMap);
                    if (item == null) {
                        System.err.println("[InventoryManager] ERROR: Item.fromMap returned null for map: " + itemMap);
                        continue;
                    }
                } else {
                    System.err.println("[InventoryManager] Key 'item' is missing or not a Map!");
                    continue;
                }

                // ---- Размещение предмета ----
                if (equipped) {
                    int equipIndex = getEquipIndexBySlot(equippedSlot);
                    if (equipIndex != -1 && equipIndex < equipment.length) {
                        equipment[equipIndex] = item;
                        System.out.println("[SUCCESS] Equipped into slot " + equippedSlot + " (" + equipIndex + "): " + item.getName());
                    } else {
                        System.err.println("[ERROR] Unknown equipment slot: " + equippedSlot + " (index " + equipIndex + ")");
                    }
                } else {
                    if (slot >= 0 && slot < inventoryItems.length) {
                        inventoryItems[slot] = item;
                        System.out.println("[SUCCESS] Placed into inventory slot " + slot + ": " + item.getName());
                    } else {
                        System.err.println("[ERROR] Invalid or missing slot for item: " + item.getName() + " (slot=" + slot + "). Skipping.");
                    }
                }

            } catch (Exception e) {
                System.err.println("[CRITICAL ERROR] Cannot process inventory item: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("==================================================");
        System.out.println("[TOTAL] Items in inventory: " + countItems() + ", in equipment: " + countEquipment());
        for (int i = 0; i < inventoryItems.length; i++) {
            if (inventoryItems[i] != null) {
                System.out.println("  slot " + i + ": " + inventoryItems[i].getName());
            }
        }
        for (int i = 0; i < equipment.length; i++) {
            if (equipment[i] != null) {
                System.out.println("  equip " + i + ": " + equipment[i].getName());
            }
        }
        System.out.println("==================================================");

        updateUI();
    }

    private int countItems() {
        int c = 0;
        for (Item i : inventoryItems) if (i != null) c++;
        return c;
    }

    private int countEquipment() {
        int c = 0;
        for (Item i : equipment) if (i != null) c++;
        return c;
    }

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
    //   ТЕСТОВЫЕ МЕТОДЫ
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
        slotSpaces.clear();
    }

    private void createUI(float screenWidth, float screenHeight) {
        inventoryNode.detachAllChildren();
        uiElements.clear();

        float eqWidth = 250 * scale;
        float eqHeight = 450 * scale;
        float invWidth = 400 * scale;
        float invHeight = (350 + 100) * scale;
        float spacing = 30 * scale;

        float totalWidth = eqWidth + spacing + invWidth;
        float startX = (screenWidth - totalWidth) / 2;
        float startY = (screenHeight - (eqHeight + invHeight) / 2) / 2 - 100 * scale;

        float slotSize = 60 * scale;
        float shiftDown = slotSize * 1;

        float eqX = startX;
        float eqY = startY + 100 * scale;
        float invX = startX + eqWidth + spacing;
        float invY = startY + 50 * scale;

        // Фоны
        if (uiManager != null) {
            Geometry eqBg = uiManager.createBackgroundGeometry(eqWidth, eqHeight);
            eqBg.setLocalTranslation(eqX, eqY, -1f);
            eqBg.setUserData("pickable", false);
            inventoryNode.attachChild(eqBg);
            uiElements.add(eqBg);

            Geometry invBg = uiManager.createBackgroundGeometry(invWidth, invHeight);
            invBg.setLocalTranslation(invX, invY, -1f);
            invBg.setUserData("pickable", false);
            inventoryNode.attachChild(invBg);
            uiElements.add(invBg);
        }

        // Слоты экипировки
        float offsetY = 70 * scale;
        createSlotGeometry(eqX + 95 * scale, eqY + 320 * scale + offsetY - shiftDown, 0, slotSize);
        createSlotGeometry(eqX + 160 * scale, eqY + 250 * scale + offsetY - shiftDown, 2, slotSize);
        createSlotGeometry(eqX + 30 * scale, eqY + 250 * scale + offsetY - shiftDown, 3, slotSize);
        createSlotGeometry(eqX + 95 * scale, eqY + 180 * scale + offsetY - shiftDown, 1, slotSize);
        createSlotGeometry(eqX + 95 * scale, eqY + 110 * scale + offsetY - shiftDown, 4, slotSize);
        createSlotGeometry(eqX + 95 * scale, eqY + 40 * scale + offsetY - shiftDown, 5, slotSize);
        createSlotGeometry(eqX + 30 * scale, eqY + 110 * scale + offsetY - shiftDown, 6, slotSize);

        Button closeEq = new Button("X");
        closeEq.setFontSize(16 * scale);
        closeEq.setLocalTranslation(eqX + eqWidth - 80 * scale, eqY + eqHeight - 30 * scale - shiftDown, 0);
        closeEq.addClickCommands((source) -> hide());
        inventoryNode.attachChild(closeEq);
        uiElements.add(closeEq);

        // Ячейки инвентаря (ИСПОЛЬЗУЕМ GEOMETRY, чтобы клик был точным)
        float cellSize = 55 * scale;
        float spacingCell = 8 * scale;
        float paddingLeft = 40 * scale;
        float paddingTop = 40 * scale;
        float startXCell = invX + paddingLeft;
        float startYCell = invY + invHeight - paddingTop - 50 * scale;

        for (int i = 0; i < 20; i++) {
            int col = i % 4;
            int row = i / 4;
            float x = startXCell + col * (cellSize + spacingCell);
            float y = startYCell - row * (cellSize + spacingCell);

            Geometry cell = new Geometry("InvCell_" + i, new Quad(cellSize, cellSize));
            cell.setLocalTranslation(x, y, 1f);

            Material cellMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            Item item = inventoryItems[i];
            if (item != null) {
                Texture tex = null;
                try {
                    tex = app.getAssetManager().loadTexture(item.getIconPath());
                } catch (Exception e) {}
                if (tex != null) {
                    cellMat.setTexture("ColorMap", tex);
                } else {
                    cellMat.setColor("Color", item.getFallbackColor());
                }
            } else {
                cellMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
            }
            cell.setMaterial(cellMat);

            final int idx = i;
            MouseEventControl.addListenersToSpatial(cell, new MouseListener() {
                @Override
                public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {
                    if (evt.isPressed() && evt.getButtonIndex() == 0 && isVisible) {
                        float localX = evt.getX() - x;
                        float localY = evt.getY() - y;
                        if (localX >= 0 && localX <= cellSize && localY >= 0 && localY <= cellSize) {
                            handleInventoryClick(idx);
                        }
                    }
                }
                @Override public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
                @Override public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
                @Override public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
            });

            addTooltipListener(cell, idx, true);

            inventoryNode.attachChild(cell);
            uiElements.add(cell);
            slotSpaces.add(cell);
        }

        Button closeInv = new Button("X");
        closeInv.setFontSize(16 * scale);
        closeInv.setLocalTranslation(invX + invWidth - 80 * scale, invY + invHeight - 30 * scale, 0);
        closeInv.addClickCommands((source) -> hide());
        inventoryNode.attachChild(closeInv);
        uiElements.add(closeInv);

        tooltipLabel = new Label("");
        tooltipLabel.setFontSize(14 * scale);
        tooltipLabel.setColor(ColorRGBA.White);
        tooltipLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f)));
        tooltipLabel.setPreferredSize(new Vector3f(280 * scale, 100 * scale, 1f));
        tooltipLabel.setLocalTranslation(10 * scale, 160 * scale, 10f);
        tooltipLabel.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(tooltipLabel);
        uiElements.add(tooltipLabel);
    }

    private void createSlotGeometry(float x, float y, int slotIndex, float slotSize) {
        System.out.println("[DEBUG] Slot " + slotIndex + " at (" + x + ", " + y + ") size=" + slotSize);

        Quad quad = new Quad(slotSize, slotSize);
        Geometry geo = new Geometry("slotGeo_" + slotIndex, quad);
        geo.setLocalTranslation(x, y, 1f);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        if (equipment[slotIndex] != null) {
            Item item = equipment[slotIndex];
            Texture tex = null;
            try {
                tex = app.getAssetManager().loadTexture(item.getIconPath());
            } catch (Exception e) {}
            if (tex != null) {
                mat.setTexture("ColorMap", tex);
            } else {
                mat.setColor("Color", item.getFallbackColor());
            }
        } else {
            Texture emptyTex = null;
            try {
                emptyTex = app.getAssetManager().loadTexture(EMPTY_SLOT_ICONS[slotIndex]);
            } catch (Exception e) {}
            if (emptyTex != null) {
                mat.setTexture("ColorMap", emptyTex);
            } else {
                mat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
            }
        }

        geo.setMaterial(mat);

        final int idx = slotIndex;
        MouseEventControl.addListenersToSpatial(geo, new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {
                if (evt.isPressed() && evt.getButtonIndex() == 0 && isVisible) {
                    float localX = evt.getX() - x;
                    float localY = evt.getY() - y;
                    if (localX >= 0 && localX <= slotSize && localY >= 0 && localY <= slotSize) {
                        System.out.println("[SLOT CLICK] Slot " + idx + " at (" + x + ", " + y + "), size=" + slotSize);
                        handleEquipmentClick(idx);
                    }
                }
            }
            @Override public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
        });

        addTooltipListener(geo, idx, false);

        inventoryNode.attachChild(geo);
        uiElements.add(geo);
        slotSpaces.add(geo);
    }

    private void addTooltipListener(Spatial target, int index, boolean isInventory) {
        MouseEventControl.removeListenersFromSpatial(target);

        MouseListener listener = new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {}

            @Override
            public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {
                if (!isVisible) return;
                String text = "";
                if (isInventory && index < inventoryItems.length && inventoryItems[index] != null) {
                    text = buildTooltip(inventoryItems[index]);
                } else if (!isInventory && equipment[index] != null) {
                    text = buildTooltip(equipment[index]);
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
        MouseEventControl.addListenersToSpatial(target, listener);
    }

private String buildTooltip(Item item) {
    if (item == null) return "";
    StringBuilder sb = new StringBuilder();
    sb.append(item.getName()).append("\n");
    sb.append(getLocalized("stat.level")).append(": ").append(item.getLevel()).append("\n");
    sb.append(getLocalized("stat.rarity")).append(": ").append(getLocalized("rarity." + item.getRarity().name().toLowerCase())).append("\n");
    if (item.getDamage() > 0) sb.append(getLocalized("stat.damage")).append(": +").append(item.getDamage()).append("\n");
    if (item.getDefense() > 0) sb.append(getLocalized("stat.defense")).append(": +").append(item.getDefense()).append("\n");
    if (item.getHealthBonus() > 0) sb.append(getLocalized("stat.health")).append(": +").append(item.getHealthBonus()).append("\n");
    if (item.getManaBonus() > 0) sb.append(getLocalized("stat.mana")).append(": +").append(item.getManaBonus()).append("\n");
    sb.append(getLocalized("stat.type")).append(": ").append(item.getLocalizedType());
    return sb.toString();
}
private String getLocalized(String key) {
    return LocalizationManager.getInstance().get(key);
}
    private void handleInventoryClick(int index) {
        if (!isVisible || isProcessing) return;
        if (index < 0 || index >= inventoryItems.length) return;
        Item item = inventoryItems[index];
        if (item == null) return;

        if (networkManager != null) {
            isProcessing = true;
            System.out.println("[InventoryManager] Sending equip request for inventory slot " + index);
            networkManager.equipItem(index).thenAccept(response -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    if (response != null && uiManager != null) {
                        System.out.println("[InventoryManager] Equip successful. Applying data.");
                        uiManager.applyCharacterData(response);
                        updateUI();
                        SoundManager.playSound(SoundManager.SOUND_CLICK);
                    } else {
                        System.err.println("[InventoryManager] Equip failed: response is null.");
                        requestInventoryRefresh();
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    System.err.println("[InventoryManager] Equip exception: " + ex.getMessage());
                    ex.printStackTrace();
                    requestInventoryRefresh();
                });
                return null;
            });
        } else {
            System.out.println("[InventoryManager] Equip locally (offline) not implemented");
        }
    }

    private void handleEquipmentClick(int slotIndex) {
        System.out.println("[InventoryManager] handleEquipmentClick called for slot " + slotIndex);
        if (!isVisible || isProcessing) return;
        if (equipment[slotIndex] == null) return;
        String slotName = getSlotName(slotIndex);
        if (slotName == null) return;

        if (networkManager != null) {
            isProcessing = true;
            networkManager.unequipItem(slotName).thenAccept(response -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    if (response != null && !response.containsKey("error")) {
                        System.out.println("[InventoryManager] Unequip success, applying data");
                        uiManager.applyCharacterData(response);
                        updateUI();
                        SoundManager.playSound(SoundManager.SOUND_UNEQUIP);
                    } else {
                        // Ответ невалидный или сервер вернул ошибку
                        System.err.println("[InventoryManager] Unequip failed: " + (response != null ? response.get("error") : "Response null"));
                        // Не делаем локальное снятие, просто обновляем данные с сервера
                        requestInventoryRefresh();
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    System.err.println("[InventoryManager] Unequip exception: " + ex.getMessage());
                    ex.printStackTrace();
                    // Не делаем локальное снятие, просто обновляем данные
                    requestInventoryRefresh();
                });
                return null;
            });
        } else {
            // Оффлайн режим
            Item item = equipment[slotIndex];
            if (item != null) {
                equipment[slotIndex] = null;
                for (int i = 0; i < inventoryItems.length; i++) {
                    if (inventoryItems[i] == null) {
                        inventoryItems[i] = item;
                        break;
                    }
                }
                updateUI();
            }
        }
    }

    private String getSlotName(int slotIndex) {
        switch (slotIndex) {
            case 0: return "helmet";
            case 1: return "chest";
            case 2: return "weapon";
            case 3: return "shield";
            case 4: return "legs";
            case 5: return "boots";
            case 6: return "gloves";
            default: return null;
        }
    }

    public void requestInventoryRefresh() {
        if (networkManager != null && !isProcessing) {
            isProcessing = true;
            networkManager.loadCharacterData().thenAccept(data -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    if (data != null && uiManager != null) {
                        uiManager.applyCharacterData(data);
                        updateUI();
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    System.err.println("[InventoryManager] Refresh exception: " + ex.getMessage());
                });
                return null;
            });
        }
    }

    private void setVisible(boolean visible) {
        isVisible = visible;

        // Сбрасываем масштаб и позицию узла при любом открытии, чтобы координаты не сбивались
        inventoryNode.setLocalScale(1f, 1f, 1f);
        inventoryNode.setLocalTranslation(0, 0, 0);

        inventoryNode.setCullHint(visible ? Node.CullHint.Dynamic : Node.CullHint.Always);

        if (visible) {
            if (guiNode.hasChild(inventoryNode)) {
                guiNode.detachChild(inventoryNode);
            }
            guiNode.attachChild(inventoryNode);
            System.out.println("[InventoryManager] inventoryNode attached to guiNode");
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
        SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
    }

    public void hide() {
        SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
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

    public int getItemIndex(Item item) {
        if (item == null) return -1;
        for (int i = 0; i < inventoryItems.length; i++) {
            if (inventoryItems[i] == item) {
                return i;
            }
        }
        return -1;
    }

    public int getSlotIndex(Item item) {
        if (item == null) return -1;
        for (int i = 0; i < inventoryItems.length; i++) {
            if (inventoryItems[i] == item) {
                return i;
            }
        }
        return -1;
    }

    public Item getItemAtSlot(int slot) {
        if (slot < 0 || slot >= inventoryItems.length) return null;
        return inventoryItems[slot];
    }

    public boolean isFull() {
        for (Item item : inventoryItems) {
            if (item == null) return false;
        }
        return true;
    }
}