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
import com.jme3.scene.shape.Quad;
import com.mygame.managers.InventoryManager.Item;

import java.util.ArrayList;
import java.util.List;

public class DropManager {

    private SimpleApplication app;
    private Node dropNode;
    private List<DropItem> drops = new ArrayList<>();
    private InventoryManager inventoryManager;

    private static final float ICON_SIZE = 0.5f;
    private static final float BORDER_SIZE = 0.04f;
    private static final float TEXT_OFFSET = 0.7f;
    private static final float PIXEL_THRESHOLD = 45f; // порог в пикселях для клика

    public DropManager(SimpleApplication app, Node guiNode) {
        this.app = app;
        dropNode = new Node("DropNode");
        app.getRootNode().attachChild(dropNode);
    }

    public void setInventoryManager(InventoryManager im) {
        this.inventoryManager = im;
    }

    public void spawnDrops(Vector3f center, List<Item> items) {
        int count = items.size();
        float radius = 1.2f;
        for (int i = 0; i < count; i++) {
            float angle = (float)(i * 2 * Math.PI / count);
            float offsetX = (float)(radius * Math.cos(angle) + (Math.random() - 0.5) * 0.3);
            float offsetZ = (float)(radius * Math.sin(angle) + (Math.random() - 0.5) * 0.3);
            Vector3f pos = new Vector3f(center.x + offsetX, 0, center.z + offsetZ);
            spawnDrop(pos, items.get(i));
        }
    }

    public void spawnDrop(Vector3f position, Item item) {
        DropItem drop = new DropItem(position, item);
        dropNode.attachChild(drop.node);
        drops.add(drop);
        System.out.println("[DropManager] Drop spawned: " + item.name);
    }

    /**
     * Проверяет попадание клика по экранным координатам.
     * @param screenX экранная координата X (от левого нижнего угла)
     * @param screenY экранная координата Y (от левого нижнего угла)
     * @return DropItem, если клик попал в иконку, иначе null
     */
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

    public void pickupDrop(DropItem drop) {
        if (inventoryManager != null) {
            inventoryManager.addItem(drop.item);
            dropNode.detachChild(drop.node);
            drops.remove(drop);
            System.out.println("[DropManager] Picked up: " + drop.item.name);
        }
    }

    public class DropItem {
        public Node node;
        public Item item;
        private Geometry iconGeom;
        private BitmapText labelText;

        public DropItem(Vector3f position, Item item) {
            this.item = item;
            node = new Node("DropNode_" + item.name);
            node.setLocalTranslation(position.x, 0, position.z);

            // Иконка
            Quad quad = new Quad(ICON_SIZE, ICON_SIZE);
            iconGeom = new Geometry("DropIcon", quad);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", item.color);
            iconGeom.setMaterial(mat);
            iconGeom.setLocalTranslation(-ICON_SIZE/2, 0, -ICON_SIZE/2);
            node.attachChild(iconGeom);

            // Рамка
            Quad borderQuad = new Quad(ICON_SIZE + BORDER_SIZE, ICON_SIZE + BORDER_SIZE);
            Geometry borderGeom = new Geometry("DropBorder", borderQuad);
            Material borderMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            borderMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.7f));
            borderGeom.setMaterial(borderMat);
            borderGeom.setLocalTranslation(-(ICON_SIZE + BORDER_SIZE)/2, -BORDER_SIZE/2, -(ICON_SIZE + BORDER_SIZE)/2);
            node.attachChild(borderGeom);

            // Текст над иконкой – используем корректный путь к шрифту
            labelText = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
            labelText.setText(item.name);
            labelText.setSize(0.2f);
            labelText.setColor(ColorRGBA.White);
            float textWidth = labelText.getLineWidth();
            labelText.setLocalTranslation(-textWidth/2, ICON_SIZE/2 + TEXT_OFFSET, 0);
            node.attachChild(labelText);
        }

        public Vector3f getPosition() {
            return node.getWorldTranslation();
        }
    }
}