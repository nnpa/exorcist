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

    public TraderWindow(SimpleApplication app, PlayerManager pm, InventoryManager im, UIManager ui) {
        this.app = app;
        this.playerManager = pm;
        this.inventoryManager = im;
        this.uiManager = ui;
        this.networkManager = Main.getInstance().getNetworkManager();
        createWindow();
        positionWindow();
    }

    public Node getNode() {
        return windowNode;
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

        windowWidth = 420 * scale;
        windowHeight = 380 * scale;

        windowNode = new Node("TraderWindowNode");
        windowNode.setName("TraderWindowNode");

        // Фон
        if (uiManager != null) {
            Geometry bgGeom = uiManager.createBackgroundGeometry(windowWidth, windowHeight);
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

        // Заголовок
        Label title = new Label("Trader");
        title.setFontSize(20 * scale);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(windowWidth / 2 - 40 * scale, windowHeight - 30 * scale, 0.1f);
        windowNode.attachChild(title);

        // Золото
        goldLabel = new Label("Gold: 0");
        goldLabel.setFontSize(14 * scale);
        goldLabel.setColor(ColorRGBA.Yellow);
        goldLabel.setLocalTranslation(15 * scale, windowHeight - 60 * scale, 0.1f);
        windowNode.attachChild(goldLabel);

        // Вкладки
        Button buyTab = new Button("Buy");
        buyTab.setPreferredSize(new Vector3f(70 * scale, 22 * scale, 0));
        buyTab.setFontSize(12 * scale);
        buyTab.setLocalTranslation(15 * scale, windowHeight - 95 * scale, 0.1f);
        buyTab.addClickCommands((source) -> showBuyTab());
        windowNode.attachChild(buyTab);

        Button sellTab = new Button("Sell");
        sellTab.setPreferredSize(new Vector3f(70 * scale, 22 * scale, 0));
        sellTab.setFontSize(12 * scale);
        sellTab.setLocalTranslation(95 * scale, windowHeight - 95 * scale, 0.1f);
        sellTab.addClickCommands((source) -> showSellTab());
        windowNode.attachChild(sellTab);

        // Контейнер контента
        contentNode = new Node("ContentNode");
        contentNode.setLocalTranslation(15 * scale, 20 * scale, 0.1f);
        windowNode.attachChild(contentNode);

        // Кнопка закрытия
        Button closeButton = new Button("X");
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
        for (Spatial s : children) {
            contentNode.detachChild(s);
        }
    }

    // ========== СОХРАНЕНИЕ НА СЕРВЕР ==========
    private void saveToServer() {
        if (networkManager == null || playerManager == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("gold", playerManager.getGold());
        data.put("healthPotions", playerManager.getHealthPotions());
        data.put("manaPotions", playerManager.getManaPotions());
        networkManager.saveCharacter(data).thenAccept(success -> {
            app.enqueue(() -> {
                if (success) {
                    System.out.println("[TraderWindow] Data saved to server.");
                } else {
                    System.err.println("[TraderWindow] Failed to save data!");
                }
            });
        });
    }

    // ========== ПОКУПКА ==========
    private void showBuyTab() {
        clearContent();
        Label header = new Label("Buy items:");
        header.setFontSize(14 * scale);
        header.setColor(ColorRGBA.White);
        header.setLocalTranslation(0, 200 * scale, 0.1f);
        contentNode.attachChild(header);

        // Health Potion
        Label hpLabel = new Label("Health Potion (50 HP) - 10g");
        hpLabel.setFontSize(12 * scale);
        hpLabel.setColor(ColorRGBA.White);
        hpLabel.setLocalTranslation(0, 170 * scale, 0.1f);
        contentNode.attachChild(hpLabel);

        Button hpBuy = new Button("Buy");
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

        // Mana Potion
        Label mpLabel = new Label("Mana Potion (30 MP) - 10g");
        mpLabel.setFontSize(12 * scale);
        mpLabel.setColor(ColorRGBA.White);
        mpLabel.setLocalTranslation(0, 140 * scale, 0.1f);
        contentNode.attachChild(mpLabel);

        Button mpBuy = new Button("Buy");
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

    // ========== ПРОДАЖА ==========
    private void showSellTab() {
        clearContent();
        Label header = new Label("Sell items (click to sell):");
        header.setFontSize(14 * scale);
        header.setColor(ColorRGBA.White);
        header.setLocalTranslation(0, 200 * scale, 0.1f);
        contentNode.attachChild(header);

        List<Item> items = inventoryManager.getItems();
        if (items.isEmpty()) {
            Label empty = new Label("Inventory is empty.");
            empty.setFontSize(12 * scale);
            empty.setColor(ColorRGBA.Gray);
            empty.setLocalTranslation(0, 170 * scale, 0.1f);
            contentNode.attachChild(empty);
        } else {
            float yPos = 170 * scale;
            for (Item item : items) {
                Label nameLabel = new Label(item.getName() + " (" + item.getType() + ")");
                nameLabel.setFontSize(11 * scale);
                nameLabel.setColor(ColorRGBA.White);
                nameLabel.setLocalTranslation(0, yPos, 0.1f);
                contentNode.attachChild(nameLabel);

                int price = Math.max(1, item.getLevel() * 5);
                Button sellBtn = new Button("Sell " + price + "g");
                sellBtn.setPreferredSize(new Vector3f(60 * scale, 20 * scale, 0));
                sellBtn.setFontSize(10 * scale);
                sellBtn.setLocalTranslation(250 * scale, yPos, 0.1f);
                sellBtn.addClickCommands((source) -> {
                    // 1. Получаем индекс предмета
                    int slotIndex = inventoryManager.getItemIndex(item);
                    if (slotIndex == -1) {
                        System.err.println("[TraderWindow] Item not found in inventory!");
                        return;
                    }

                    // 2. Если есть сеть – сначала удаляем на сервере
                    if (networkManager != null) {
                        networkManager.dropItem(slotIndex).thenAccept(response -> {
                            app.enqueue(() -> {
                                if (response != null) {
                                    // Сервер успешно удалил предмет и вернул обновлённые данные
                                    // Применяем их (обновит инвентарь и золото)
                                    if (uiManager != null) {
                                        uiManager.applyCharacterData(response);
                                    }
                                    // Обновляем отображение
                                    updateGold();
                                    if (uiManager != null) uiManager.updatePotionCounts();
                                    showSellTab();
                                } else {
                                    // Сервер отказал – возможно, предмет уже удалён или ошибка
                                    System.err.println("[TraderWindow] Server rejected drop. Refreshing inventory...");
                                    // Принудительно запрашиваем свежие данные
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
                        // Оффлайн режим: просто удаляем локально
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
            goldLabel.setText("Gold: " + playerManager.getGold());
        }
    }

    public void show() {
        isVisible = true;
        if (uiManager != null) {
            uiManager.onTraderOpened(windowNode);
        } else {
            if (!app.getGuiNode().hasChild(windowNode)) {
                app.getGuiNode().attachChild(windowNode);
            }
        }
        positionWindow();
        updateGold();
        showBuyTab();
    }

    public void hide() {
        isVisible = false;
        if (uiManager != null) {
            uiManager.onTraderClosed(windowNode);
        } else {
            if (app.getGuiNode().hasChild(windowNode)) {
                app.getGuiNode().detachChild(windowNode);
            }
        }
    }

    public void toggle() {
        if (isVisible) hide(); else show();
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void updateLayout(int screenWidth, int screenHeight) {
        if (isVisible) {
            if (uiManager != null) {
                uiManager.onTraderClosed(windowNode);
            }
            windowNode.detachAllChildren();
            createWindow();
            if (uiManager != null) {
                uiManager.onTraderOpened(windowNode);
            }
        }
    }
}