package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.mygame.Main;
import com.mygame.items.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropManager {

    private SimpleApplication app;
    private Node dropNode;
    private List<DropItem> drops = new ArrayList<>();
    private InventoryManager inventoryManager;
    private NetworkManager networkManager; // ← добавили

    // ===== РАЗМЕРЫ =====
    private static final float ICON_SIZE = 0.8f; 
    private static final float BORDER_SIZE = 0.06f;
    private static final float TEXT_OFFSET = 1.4f;
    private static final float PIXEL_THRESHOLD = 80f;

    public DropManager(SimpleApplication app, Node guiNode) {
        this.app = app;
        dropNode = new Node("DropNode");
        app.getRootNode().attachChild(dropNode);
        // Получаем NetworkManager из Main
        Main main = (Main) app;
        if (main != null) {
            this.networkManager = main.getNetworkManager();
        }
    }

    public void setInventoryManager(InventoryManager im) {
        this.inventoryManager = im;
    }

    public void spawnDrops(Vector3f center, List<Item> items) {
        int count = items.size();
        float radius = 1.5f;
        for (int i = 0; i < count; i++) {
            float angle = (float)(i * 2 * Math.PI / count + Math.random() * 0.5);
            float offsetX = (float)(radius * Math.cos(angle) + (Math.random() - 0.5) * 0.5);
            float offsetZ = (float)(radius * Math.sin(angle) + (Math.random() - 0.5) * 0.5);
            Vector3f pos = new Vector3f(center.x + offsetX, 0, center.z + offsetZ);
            spawnDrop(pos, items.get(i));
        }
    }

    public void spawnDrop(Vector3f position, Item item) {
        DropItem drop = new DropItem(position, item);
        dropNode.attachChild(drop.node);
        drops.add(drop);
        System.out.println("[DropManager] Drop spawned: " + item.getName());
    }

    public DropItem getDropAt(float screenX, float screenY) {
        Camera cam = app.getCamera();
        Vector2f clickPos = new Vector2f(screenX, screenY);

        for (DropItem drop : drops) {
            Vector3f worldPos = drop.node.getWorldTranslation();
            Vector3f screenPos = cam.getScreenCoordinates(worldPos);
            if (screenPos == null) continue;

            float distX = screenPos.x - screenX;
            float distY = screenPos.y - screenY;
            float dist = (float) Math.sqrt(distX * distX + distY * distY);

            if (dist < PIXEL_THRESHOLD) {
                return drop;
            }
        }
        return null;
    }

    // ============================================================
    //   ПОДБОР ПРЕДМЕТА (СЕТЕВОЙ ВАРИАНТ)
    // ============================================================
   public void pickupDrop(DropItem drop) {
    if (drop == null) return;

    // Проверка, есть ли место в инвентаре
    if (inventoryManager != null && inventoryManager.isFull()) {
        System.out.println("[DropManager] Inventory is full!");
        if (uiManager != null) {
            uiManager.showToast("Inventory is full!");
        }
        return;
    }

    // Если нет сети – локальное добавление (для тестов)
    if (networkManager == null) {
        if (inventoryManager != null) {
            inventoryManager.addItem(drop.item);
            dropNode.detachChild(drop.node);
            drops.remove(drop);
            System.out.println("[DropManager] (offline) Picked up: " + drop.item.getName());
        }
        return;
    }

    // Формируем данные предмета для отправки на сервер
    Map<String, Object> itemData = new HashMap<>();
    itemData.put("id", drop.item.getId());
    itemData.put("name", drop.item.getName());
    itemData.put("type", drop.item.getType());
    itemData.put("level", drop.item.getLevel());
    itemData.put("rarity", drop.item.getRarity().name());
    itemData.put("description", drop.item.getDescription());
    itemData.put("damage", drop.item.getDamage());
    itemData.put("defense", drop.item.getDefense());
    itemData.put("healthBonus", drop.item.getHealthBonus());
    itemData.put("manaBonus", drop.item.getManaBonus());
    itemData.put("iconPath", drop.item.getIconPath());
    itemData.put("socketCount", drop.item.getSocketCount());
    itemData.put("difficulty", drop.item.getDifficulty());

    // Отправляем запрос
    networkManager.pickupItem(itemData).thenAccept(response -> {
        app.enqueue(() -> {
            System.out.println("[DropManager] Pickup response: " + response);
            if (response != null) {
                // Успех – обновляем персонажа и удаляем дроп
                Main main = (Main) app;
                if (main != null) {
                    UIManager ui = main.getUIManager();
                    if (ui != null) {
                        ui.applyCharacterData(response);
                    }
                }
                // Удаляем дроп только после успешного ответа
                dropNode.detachChild(drop.node);
                drops.remove(drop);
                SoundManager.playSound(SoundManager.SOUND_PICKUP);
                System.out.println("[DropManager] Picked up (server): " + drop.item.getName());
            } else {
                // Сервер вернул null – значит, ошибка (возможно, инвентарь полон)
                System.err.println("[DropManager] Server rejected pickup. Check server logs.");
                // Попробуем показать сообщение
                if (uiManager != null) {
                    uiManager.showToast("Failed to pick up item.");
                }
            }
        });
    }).exceptionally(ex -> {
        app.enqueue(() -> {
            System.err.println("[DropManager] Network error: " + ex.getMessage());
            if (uiManager != null) {
                uiManager.showToast("Network error: " + ex.getMessage());
            }
        });
        return null;
    });
}
private UIManager uiManager;

public void setUIManager(UIManager ui) {
    this.uiManager = ui;
}
    // ============================================================
    //   КЛАСС DROPITEM
    // ============================================================
    public class DropItem {
        public Node node;
        public Item item;
        private Geometry iconGeom;
        private BitmapText labelText;

        public DropItem(Vector3f position, Item item) {
            this.item = item;
            node = new Node("DropNode_" + item.getName());
            node.setLocalTranslation(position.x, position.y + 0.8f, position.z);

            // Иконка
            Quad quad = new Quad(ICON_SIZE, ICON_SIZE);
            iconGeom = new Geometry("DropIcon", quad);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            Texture tex = null;
            try {
                tex = app.getAssetManager().loadTexture(item.getIconPath());
            } catch (Exception e) {}
            if (tex != null) {
                mat.setTexture("ColorMap", tex);
                mat.setColor("Color", ColorRGBA.White);
            } else {
                mat.setColor("Color", item.getColor());
            }
            iconGeom.setMaterial(mat);
            iconGeom.setLocalTranslation(-ICON_SIZE/2, 0, -ICON_SIZE/2);
            node.attachChild(iconGeom);

            // Рамка
            Quad borderQuad = new Quad(ICON_SIZE + BORDER_SIZE, ICON_SIZE + BORDER_SIZE);
            Geometry borderGeom = new Geometry("DropBorder", borderQuad);
            Material borderMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            borderMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.8f));
            borderGeom.setMaterial(borderMat);
            borderGeom.setLocalTranslation(-(ICON_SIZE + BORDER_SIZE)/2, -BORDER_SIZE/2, -(ICON_SIZE + BORDER_SIZE)/2);
            node.attachChild(borderGeom);

            // Текст
            labelText = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
            labelText.setText(item.getName());
            labelText.setSize(0.4f);
            labelText.setColor(item.getColor());
            float textWidth = labelText.getLineWidth();
            labelText.setLocalTranslation(-textWidth/2, ICON_SIZE/2 + TEXT_OFFSET, 0);
            node.attachChild(labelText);

            BillboardControl billboard = new BillboardControl();
            node.addControl(billboard);
        }
    }
}