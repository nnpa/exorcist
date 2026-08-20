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

        if (inventoryData != null && !inventoryData.isEmpty()) {
            for (Map<String, Object> data : inventoryData) {
                try {
                    System.out.println("--------------------------------------------------");
                    System.out.println("[InventoryManager] Processing data. Keys: " + data.keySet());

                    Object slotObj = data.get("slot");
                    int slot = -1;
                    if (slotObj instanceof Number) {
                        slot = ((Number) slotObj).intValue();
                    } else if (slotObj instanceof String) {
                        try {
                            slot = Integer.parseInt((String) slotObj);
                        } catch (Exception e) { /* ignore */ }
                    }

                    Object eqObj = data.get("equipped");
                    boolean equipped = false;
                    if (eqObj instanceof Boolean) equipped = (Boolean) eqObj;
                    else if (eqObj instanceof String) equipped = Boolean.parseBoolean((String) eqObj);
                    else if (eqObj instanceof Number) equipped = ((Number) eqObj).intValue() != 0;

                    Object eqSlotObj = data.get("equipped_slot");
                    if (eqSlotObj == null) eqSlotObj = data.get("equippedSlot");
                    String equippedSlot = (eqSlotObj != null) ? String.valueOf(eqSlotObj) : null;

                    Object itemMapObj = data.get("item");
                    Item item = null;
                    if (itemMapObj instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) itemMapObj;
                        item = Item.fromMap(itemMap);
                        if (item == null) {
                            System.err.println("[InventoryManager] ERROR: Item.fromMap returned null!");
                        }
                    } else {
                        System.err.println("[InventoryManager] Key 'item' is missing or not a Map!");
                    }

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

    float slotSize = 60 * scale;                // размер одной кнопки
    float shiftDown = slotSize *1;             // ← сдвиг вниз на 2 высоты кнопки (можно регулировать)

    float eqX = startX;
    float eqY = startY + 100 * scale;           // фон левого окна остаётся на месте
    float invX = startX + eqWidth + spacing;
    float invY = startY + 50 * scale;           // правое окно поднято на 100 пикселей

    // ===== ФОНЫ =====
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

    // ===== СЛОТЫ ЭКИПИРОВКИ (сдвинуты вниз на shiftDown — вычитаем из Y) =====
    float offsetY = 70 * scale;
    createSlotGeometry(eqX + 95 * scale, eqY + 320 * scale + offsetY - shiftDown, 0, slotSize);
    createSlotGeometry(eqX + 160 * scale, eqY + 250 * scale + offsetY - shiftDown, 2, slotSize);
    createSlotGeometry(eqX + 30 * scale, eqY + 250 * scale + offsetY - shiftDown, 3, slotSize);
    createSlotGeometry(eqX + 95 * scale, eqY + 180 * scale + offsetY - shiftDown, 1, slotSize);
    createSlotGeometry(eqX + 95 * scale, eqY + 110 * scale + offsetY - shiftDown, 4, slotSize);
    createSlotGeometry(eqX + 95 * scale, eqY + 40 * scale + offsetY - shiftDown, 5, slotSize);
    createSlotGeometry(eqX + 30 * scale, eqY + 110 * scale + offsetY - shiftDown, 6, slotSize);

    // Кнопка Close для экипировки (тоже сдвигаем вниз)
    Button closeEq = new Button("Close");
    closeEq.setFontSize(16 * scale);
    closeEq.setLocalTranslation(eqX + eqWidth - 80 * scale, eqY + eqHeight - 30 * scale - shiftDown, 0);
    closeEq.addClickCommands((source) -> hide());
    inventoryNode.attachChild(closeEq);
    uiElements.add(closeEq);

    // ===== ЯЧЕЙКИ ИНВЕНТАРЯ (без изменений) =====
    float cellSize = 55 * scale;
    float spacingCell = 8 * scale;
    float paddingLeft = 40 * scale;
    float paddingTop = 40 * scale;
    float startXCell = invX + paddingLeft;
    float startYCell = invY + invHeight - paddingTop;

    for (int i = 0; i < 20; i++) {
        int col = i % 4;
        int row = i / 4;
        float x = startXCell + col * (cellSize + spacingCell);
        float y = startYCell - row * (cellSize + spacingCell);

        Button cell = new Button("");
        cell.setPreferredSize(new Vector3f(cellSize, cellSize, 1f));
        cell.setInsets(new Insets3f(0, 0, 0, 0));
        cell.setLocalTranslation(x, y, 0f);

        Item item = inventoryItems[i];
        if (item != null) {
            Texture tex = null;
            try {
                tex = app.getAssetManager().loadTexture(item.getIconPath());
            } catch (Exception e) {}
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
        final int idx = i;
        cell.addClickCommands((source) -> {
            if (!isVisible) return;
            handleInventoryClick(idx);
        });
        addTooltipListener(cell, idx, true);
        inventoryNode.attachChild(cell);
        uiElements.add(cell);
        slotSpaces.add(cell);
    }

    // Кнопка Close для инвентаря
    Button closeInv = new Button("Close");
    closeInv.setFontSize(16 * scale);
    closeInv.setLocalTranslation(invX + invWidth - 80 * scale, invY + invHeight - 30 * scale, 0);
    closeInv.addClickCommands((source) -> hide());
    inventoryNode.attachChild(closeInv);
    uiElements.add(closeInv);

    // Тултип
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

    // ================================================================
    //   СОЗДАНИЕ СЛОТА ЭКИПИРОВКИ (GEOMETRY) С ОТЛАДКОЙ
    // ================================================================
    private void createSlotGeometry(float x, float y, int slotIndex, float slotSize) {
        System.out.println("[DEBUG] Slot " + slotIndex + " at (" + x + ", " + y + ") size=" + slotSize);

        Quad quad = new Quad(slotSize, slotSize);
        Geometry geo = new Geometry("slotGeo_" + slotIndex, quad);
        geo.setLocalTranslation(x, y, 1f); // Z = 1, чтобы быть над фоном

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");


            // Обычная логика с текстурой или цветом
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

        // ===== ОБРАБОТЧИК КЛИКА С ДЕТАЛЬНЫМ ЛОГИРОВАНИЕМ =====
        final int idx = slotIndex;
        MouseEventControl.addListenersToSpatial(geo, new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {
                if (evt.isPressed() && evt.getButtonIndex() == 0 && isVisible) {
                    Vector2f cursorPos = new Vector2f(evt.getX(), evt.getY());
                    float localX = cursorPos.x - x;
                    float localY = cursorPos.y - y;

                    System.out.println("[SLOT CLICK] Slot " + idx + " at (" + x + ", " + y + "), size=" + slotSize);
                    System.out.println("  cursor screen: (" + cursorPos.x + ", " + cursorPos.y + ")");
                    System.out.println("  local coords: (" + localX + ", " + localY + ")");
                    System.out.println("  visible area: x=" + x + ".." + (x + slotSize) + ", y=" + y + ".." + (y + slotSize));

                    if (localX >= 0 && localX <= slotSize && localY >= 0 && localY <= slotSize) {
                        System.out.println("  ✅ Click INSIDE slot area");
                        handleEquipmentClick(idx);
                    } else {
                        System.out.println("  ❌ Click OUTSIDE slot area");
                    }
                }
            }
            @Override public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
        });

        // Тултип
        addTooltipListener(geo, idx, false);

        inventoryNode.attachChild(geo);
        uiElements.add(geo);
        slotSpaces.add(geo);
    }

    // ================================================================
    //   УНИВЕРСАЛЬНЫЙ МЕТОД ДОБАВЛЕНИЯ ТУЛТИПА
    // ================================================================
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
        return item.getName() + "\n" +
                item.getRarity().getDisplayName() + " (Lv." + item.getLevel() + ")\n" +
                "Damage: " + item.getDamage() + ", Defense: " + item.getDefense() + "\n" +
                item.getDescription();
    }

    // ================================================================
    //   ОБРАБОТЧИКИ КЛИКОВ
    // ================================================================
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
        if (!isVisible || isProcessing) {
            System.out.println("  -> ignored (visible=" + isVisible + ", processing=" + isProcessing + ")");
            return;
        }
        if (equipment[slotIndex] == null) {
            System.out.println("  -> slot is empty");
            return;
        }
        String slotName = getSlotName(slotIndex);
        System.out.println("[InventoryManager] Trying to unequip slot " + slotIndex + " (" + slotName + ")");

        if (networkManager != null) {
            isProcessing = true;
            networkManager.unequipItem(slotIndex).thenAccept(response -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    if (response != null && uiManager != null) {
                        System.out.println("[InventoryManager] Unequip success, applying data");
                        uiManager.applyCharacterData(response);
                        updateUI();
                        SoundManager.playSound(SoundManager.SOUND_UNEQUIP); // <-- добавлено
                    } else {
                        System.err.println("[InventoryManager] Unequip failed: response null, requesting refresh");
                        requestInventoryRefresh();
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    System.err.println("[InventoryManager] Unequip exception: " + ex.getMessage());
                    ex.printStackTrace();
                    requestInventoryRefresh();
                });
                return null;
            });
        } else {
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

    // ================================================================
    //   УПРАВЛЕНИЕ ВИДИМОСТЬЮ (ИСПРАВЛЕНО)
    // ================================================================
    private void setVisible(boolean visible) {
        isVisible = visible;

        // ===== КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: управляем CullHint у корневого узла =====
        inventoryNode.setCullHint(visible ? Node.CullHint.Dynamic : Node.CullHint.Always);

        if (visible) {
            // Переподключаем узел, чтобы он оказался поверх всех окон
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
}