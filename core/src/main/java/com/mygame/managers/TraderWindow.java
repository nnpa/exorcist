package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.mygame.Main;
import com.mygame.items.Item;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraderWindow {

    private SimpleApplication app;
    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private UIManager uiManager;
    private NetworkManager networkManager;
    private Node windowNode;
    private boolean isVisible = false;

    private Label goldLabel;
    private Node contentNode;
    private float scale = 1f;
    private float windowWidth = 420;
    private float windowHeight = 380;
    private float leftShift = 0f;

    public TraderWindow(SimpleApplication app, PlayerManager pm, InventoryManager im, UIManager ui) {
        this.app = app;
        this.playerManager = pm;
        this.inventoryManager = im;
        this.uiManager = ui;
        this.networkManager = Main.getInstance().getNetworkManager();
        createWindow();
        positionWindow();
    }

    public Node getNode() { return windowNode; }

    private String getLocalized(String key) {
        return LocalizationManager.getInstance().get(key);
    }

    private void updateScale() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float baseWidth = 800f;
        float baseHeight = 600f;
        float scaleX = screenWidth / baseWidth;
        float scaleY = screenHeight / baseHeight;
        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));
    }

    private void createWindow() {
        updateScale();
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            screenWidth = 1280;
            screenHeight = 720;
        }

        leftShift = 30 * scale;
        windowWidth = (420 + 30) * scale;
        windowHeight = 380 * scale;

        windowNode = new Node("TraderWindowNode");
        windowNode.setName("TraderWindowNode");

        if (uiManager != null) {
            Geometry bgGeom = uiManager.createBackgroundGeometry(windowWidth, windowHeight);
            bgGeom.setLocalTranslation(0, 0, -0.1f);
            windowNode.attachChild(bgGeom);
        } else {
            com.jme3.scene.shape.Quad bgQuad = new com.jme3.scene.shape.Quad(windowWidth, windowHeight);
            Geometry bgGeom = new Geometry("TraderBg", bgQuad);
            com.jme3.material.Material bgMat = new com.jme3.material.Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            bgMat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.15f, 0.97f));
            bgGeom.setMaterial(bgMat);
            bgGeom.setLocalTranslation(0, 0, -0.1f);
            windowNode.attachChild(bgGeom);
        }

        Label title = new Label(getLocalized("trader.title"));
        title.setFontSize(20 * scale);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(windowWidth / 2 - 40 * scale + leftShift, windowHeight - 30 * scale, 0.1f);
        windowNode.attachChild(title);

        goldLabel = new Label(getLocalized("gold.label") + "0");
        goldLabel.setFontSize(14 * scale);
        goldLabel.setColor(ColorRGBA.Yellow);
        goldLabel.setLocalTranslation(15 * scale + leftShift, windowHeight - 60 * scale, 0.1f);
        windowNode.attachChild(goldLabel);

        Button buyTab = new Button(getLocalized("trader.tab.buy"));
        buyTab.setPreferredSize(new Vector3f(70 * scale, 22 * scale, 0));
        buyTab.setFontSize(12 * scale);
        buyTab.setLocalTranslation(15 * scale + leftShift, windowHeight - 95 * scale, 0.1f);
        buyTab.addClickCommands((source) -> showBuyTab());
        windowNode.attachChild(buyTab);

        Button sellTab = new Button(getLocalized("trader.tab.sell"));
        sellTab.setPreferredSize(new Vector3f(70 * scale, 22 * scale, 0));
        sellTab.setFontSize(12 * scale);
        sellTab.setLocalTranslation(95 * scale + leftShift, windowHeight - 95 * scale, 0.1f);
        sellTab.addClickCommands((source) -> showSellTab());
        windowNode.attachChild(sellTab);

        contentNode = new Node("ContentNode");
        contentNode.setLocalTranslation(15 * scale + leftShift, 20 * scale, 0.1f);
        windowNode.attachChild(contentNode);

        Button closeButton = new Button(getLocalized("trader.close"));
        closeButton.setPreferredSize(new Vector3f(25 * scale, 25 * scale, 0));
        closeButton.setFontSize(14 * scale);
        closeButton.setLocalTranslation(windowWidth - 35 * scale, windowHeight - 30 * scale, 0.1f);
        closeButton.addClickCommands((source) -> hide());
        windowNode.attachChild(closeButton);

        positionWindow();
        showBuyTab();
        updateGold();
    }

    private void positionWindow() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float x = (screenWidth - windowWidth) / 2;
        float y = (screenHeight - windowHeight) / 2;
        if (y < 0) y = 0;
        windowNode.setLocalTranslation(x, y, 0);
    }

    private void clearContent() {
        List<Spatial> children = new ArrayList<>(contentNode.getChildren());
        for (Spatial s : children) contentNode.detachChild(s);
    }

    private void saveToServer() {
        if (networkManager == null || playerManager == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("gold", playerManager.getGold());
        data.put("healthPotions", playerManager.getHealthPotions());
        data.put("manaPotions", playerManager.getManaPotions());
        networkManager.saveCharacter(data).thenAccept(success -> {
            app.enqueue(() -> {
                if (!success) System.err.println("[TraderWindow] Failed to save data!");
            });
        });
    }

    private void showBuyTab() {
        clearContent();
        Label header = new Label(getLocalized("trader.buy.header"));
        header.setFontSize(14 * scale);
        header.setColor(ColorRGBA.White);
        header.setLocalTranslation(0, 200 * scale, 0.1f);
        contentNode.attachChild(header);

        Label hpLabel = new Label(getLocalized("trader.buy.hp"));
        hpLabel.setFontSize(12 * scale);
        hpLabel.setColor(ColorRGBA.White);
        hpLabel.setLocalTranslation(0, 170 * scale, 0.1f);
        contentNode.attachChild(hpLabel);

        Button hpBuy = new Button(getLocalized("trader.buy.button"));
        hpBuy.setPreferredSize(new Vector3f(50 * scale, 20 * scale, 0));
        hpBuy.setFontSize(11 * scale);
        hpBuy.setLocalTranslation(240 * scale, 170 * scale, 0.1f);
        hpBuy.addClickCommands((source) -> {
            if (playerManager.getGold() >= 10) {
                playerManager.setGold(playerManager.getGold() - 10);
                playerManager.addHealthPotions(1);
                updateGold();
                if (uiManager != null) uiManager.updatePotionCounts();
                showBuyTab();
                saveToServer();
            }
        });
        contentNode.attachChild(hpBuy);

        Label mpLabel = new Label(getLocalized("trader.buy.mp"));
        mpLabel.setFontSize(12 * scale);
        mpLabel.setColor(ColorRGBA.White);
        mpLabel.setLocalTranslation(0, 140 * scale, 0.1f);
        contentNode.attachChild(mpLabel);

        Button mpBuy = new Button(getLocalized("trader.buy.button"));
        mpBuy.setPreferredSize(new Vector3f(50 * scale, 20 * scale, 0));
        mpBuy.setFontSize(11 * scale);
        mpBuy.setLocalTranslation(240 * scale, 140 * scale, 0.1f);
        mpBuy.addClickCommands((source) -> {
            if (playerManager.getGold() >= 10) {
                playerManager.setGold(playerManager.getGold() - 10);
                playerManager.addManaPotions(1);
                updateGold();
                if (uiManager != null) uiManager.updatePotionCounts();
                showBuyTab();
                saveToServer();
            }
        });
        contentNode.attachChild(mpBuy);
    }

    private void showSellTab() {
        clearContent();
        Label header = new Label(getLocalized("trader.sell.header"));
        header.setFontSize(14 * scale);
        header.setColor(ColorRGBA.White);
        header.setLocalTranslation(0, 200 * scale, 0.1f);
        contentNode.attachChild(header);

        List<Item> items = inventoryManager.getItems();
        if (items.isEmpty()) {
            Label empty = new Label(getLocalized("trader.sell.empty"));
            empty.setFontSize(12 * scale);
            empty.setColor(ColorRGBA.Gray);
            empty.setLocalTranslation(0, 170 * scale, 0.1f);
            contentNode.attachChild(empty);
        } else {
            float yPos = 170 * scale;
            for (Item item : items) {
                Label nameLabel = new Label(item.getName() + " (" + item.getLocalizedType() + ")");
                nameLabel.setFontSize(11 * scale);
                nameLabel.setColor(ColorRGBA.White);
                nameLabel.setLocalTranslation(0, yPos, 0.1f);
                contentNode.attachChild(nameLabel);

                int price = Math.max(1, item.getLevel() * 5);
                Button sellBtn = new Button(getLocalized("trader.sell.button") + price + "g");
                sellBtn.setPreferredSize(new Vector3f(60 * scale, 20 * scale, 0));
                sellBtn.setFontSize(10 * scale);
                sellBtn.setLocalTranslation(250 * scale, yPos, 0.1f);
                sellBtn.addClickCommands((source) -> {
                    int slotIndex = inventoryManager.getItemIndex(item);
                    if (slotIndex == -1) {
                        System.err.println("[TraderWindow] Item not found in inventory!");
                        return;
                    }
                    if (networkManager != null) {
                        networkManager.dropItem(slotIndex).thenAccept(response -> {
                            app.enqueue(() -> {
                                if (response != null && uiManager != null) {
                                    uiManager.applyCharacterData(response);
                                    updateGold();
                                    if (uiManager != null) uiManager.updatePotionCounts();
                                    showSellTab();
                                } else {
                                    System.err.println("[TraderWindow] Server rejected drop. Refreshing...");
                                    if (networkManager != null) {
                                        networkManager.loadCharacterData().thenAccept(data -> {
                                            app.enqueue(() -> {
                                                if (data != null && uiManager != null) {
                                                    uiManager.applyCharacterData(data);
                                                    showSellTab();
                                                }
                                            });
                                        });
                                    }
                                }
                            });
                        }).exceptionally(ex -> {
                            app.enqueue(() -> System.err.println("[TraderWindow] Network error: " + ex.getMessage()));
                            return null;
                        });
                    } else {
                        playerManager.setGold(playerManager.getGold() + price);
                        inventoryManager.removeItem(item);
                        updateGold();
                        if (uiManager != null) uiManager.updatePotionCounts();
                        showSellTab();
                    }
                });
                contentNode.attachChild(sellBtn);
                yPos -= 30 * scale;
                if (yPos < 20 * scale) break;
            }
        }
    }

    private void updateGold() {
        if (goldLabel != null) {
            goldLabel.setText(getLocalized("gold.label") + playerManager.getGold());
        }
    }

    public void show() {
        isVisible = true;
        if (uiManager != null) uiManager.onTraderOpened(windowNode);
        else if (!app.getGuiNode().hasChild(windowNode)) app.getGuiNode().attachChild(windowNode);
        positionWindow();
        updateGold();
        showBuyTab();
    }

    public void hide() {
        isVisible = false;
        if (uiManager != null) uiManager.onTraderClosed(windowNode);
        else if (app.getGuiNode().hasChild(windowNode)) app.getGuiNode().detachChild(windowNode);
    }

    public void toggle() { if (isVisible) hide(); else show(); }
    public boolean isVisible() { return isVisible; }

    public void updateLayout(int screenWidth, int screenHeight) {
        if (isVisible) {
            if (uiManager != null) uiManager.onTraderClosed(windowNode);
            windowNode.detachAllChildren();
            createWindow();
            if (uiManager != null) uiManager.onTraderOpened(windowNode);
        }
    }
}