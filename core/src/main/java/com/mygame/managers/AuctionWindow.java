package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;
import com.mygame.Main;
import com.mygame.items.Item;
import com.mygame.items.ItemRarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AuctionWindow {

    private SimpleApplication app;
    private UIManager uiManager;
    private NetworkManager networkManager;
    private InventoryManager inventoryManager;
    private PlayerManager playerManager;

    private Node windowNode;
    private boolean isVisible = false;

    private List<Spatial> dynamicParts = new ArrayList<>();

    private Container listAndPaginationContainer;
    private Container paginationContainer;
    private Container tooltipContainer;
    private Label tooltipLabel;
    private Label goldLabel;
    private Label statusLabel;
    private TextField priceInput;

    private int selectedSlot = -1;

    private List<AuctionLot> currentLots = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;

    private String filterType = "";
    private String filterRarity = "";
    private int filterMinLevel = 1;
    private int filterMaxLevel = 100;

    private float scale = 1f;
    private float winW, winH;
    private float leftShift = 0f; // ← новое поле

    private final float ROW_HEIGHT = 26f;
    private final float GAP = 10f;
    private final float PAGINATION_HEIGHT = 30f;

    public AuctionWindow(SimpleApplication app, UIManager ui, InventoryManager im, PlayerManager pm) {
        this.app = app;
        this.uiManager = ui;
        this.inventoryManager = im;
        this.playerManager = pm;
        this.networkManager = Main.getInstance().getNetworkManager();
        createWindow();
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
        windowNode = new Node("AuctionWindowNode");

        // Увеличиваем ширину на 10 пикселей и задаём смещение вправо
        winW = (700 + 25) * scale;  // +10
        winH = 500 * scale;
        leftShift = 19 * scale;     // смещение вправо

        Geometry bg = uiManager.createBackgroundGeometry(winW, winH);
        bg.setLocalTranslation(0, 0, -0.1f);
        windowNode.attachChild(bg);

        Label title = new Label("Auction House");
        title.setFontSize(20 * scale);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(winW / 2 - 80 * scale + leftShift, winH - 30 * scale, 0.1f);
        windowNode.attachChild(title);

        Button closeBtn = new Button("X");
        closeBtn.setPreferredSize(new Vector3f(25 * scale, 25 * scale, 0));
        closeBtn.setLocalTranslation(winW - 35 * scale + leftShift, winH - 30 * scale, 0.1f);
        closeBtn.addClickCommands(s -> hide());
        windowNode.attachChild(closeBtn);

        Button buyTab = new Button("Browse");
        buyTab.setPreferredSize(new Vector3f(70 * scale, 22 * scale, 0));
        buyTab.setLocalTranslation(15 * scale + leftShift, winH - 95 * scale, 0.1f);
        buyTab.addClickCommands(s -> showBrowseTab());
        windowNode.attachChild(buyTab);

        Button sellTab = new Button("Sell");
        sellTab.setPreferredSize(new Vector3f(70 * scale, 22 * scale, 0));
        sellTab.setLocalTranslation(95 * scale + leftShift, winH - 95 * scale, 0.1f);
        sellTab.addClickCommands(s -> showSellTab());
        windowNode.attachChild(sellTab);

        goldLabel = new Label("Gold: " + playerManager.getGold());
        goldLabel.setFontSize(14 * scale);
        goldLabel.setColor(ColorRGBA.Yellow);
        goldLabel.setLocalTranslation(15 * scale + leftShift, winH - 65 * scale, 0.1f);
        windowNode.attachChild(goldLabel);

        MouseEventControl.removeListenersFromSpatial(windowNode);
        MouseEventControl.addListenersToSpatial(windowNode, new MouseListener() {
            @Override public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) { evt.setConsumed(); }
            @Override public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
        });

        positionWindow();
        showBrowseTab();
    }

    private void positionWindow() {
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        float offsetY = 40 * scale + 90 * scale;
        windowNode.setLocalTranslation((w - winW) / 2, (h - winH) / 2 + offsetY, 0);
    }

    private void clearDynamicParts() {
        for (Spatial s : dynamicParts) {
            if (windowNode.hasChild(s)) windowNode.detachChild(s);
        }
        dynamicParts.clear();
    }

    private void showBrowseTab() {
        clearDynamicParts();
        selectedSlot = -1;
        currentLots.clear();

        float y = winH - 110 * scale;

        goldLabel.setText("Gold: " + playerManager.getGold());
        goldLabel.setPreferredSize(new Vector3f(winW - 30 * scale, 30 * scale, 0));
        goldLabel.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        windowNode.attachChild(goldLabel);
        dynamicParts.add(goldLabel);
        y -= 35 * scale;

        // Фильтр типа
        Label typeLabel = new Label("Type:");
        typeLabel.setFontSize(12 * scale);
        typeLabel.setColor(ColorRGBA.White);
        typeLabel.setPreferredSize(new Vector3f(winW - 30 * scale, 20 * scale, 0));
        typeLabel.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        windowNode.attachChild(typeLabel);
        dynamicParts.add(typeLabel);
        y -= 25 * scale;

        String[] typeOptions = {"All", "Weapon", "Helmet", "Chest", "Shield", "Legs", "Boots", "Gloves"};
        Container typeContainer = new Container();
        typeContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        typeContainer.setPreferredSize(new Vector3f(winW - 30 * scale, 25 * scale, 0));
        typeContainer.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        for (String opt : typeOptions) {
            Button btn = new Button(opt);
            btn.setFontSize(11 * scale);
            btn.setPreferredSize(new Vector3f(70 * scale, 20 * scale, 0));
            if (filterType.isEmpty() && opt.equals("All")) btn.setColor(ColorRGBA.Yellow);
            else if (opt.equals(filterType)) btn.setColor(ColorRGBA.Yellow);
            else btn.setColor(ColorRGBA.White);
            btn.addClickCommands((source) -> {
                filterType = opt.equals("All") ? "" : opt;
                loadLots(1);
            });
            typeContainer.addChild(btn);
        }
        windowNode.attachChild(typeContainer);
        dynamicParts.add(typeContainer);
        y -= 35 * scale;

        // Фильтр редкости
        Label rarityLabel = new Label("Rarity:");
        rarityLabel.setFontSize(12 * scale);
        rarityLabel.setColor(ColorRGBA.White);
        rarityLabel.setPreferredSize(new Vector3f(winW - 30 * scale, 20 * scale, 0));
        rarityLabel.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        windowNode.attachChild(rarityLabel);
        dynamicParts.add(rarityLabel);
        y -= 25 * scale;

        String[] rarityOptions = {"All", "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"};
        Container rarityContainer = new Container();
        rarityContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        rarityContainer.setPreferredSize(new Vector3f(winW - 30 * scale, 25 * scale, 0));
        rarityContainer.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        for (String opt : rarityOptions) {
            Button btn = new Button(opt);
            btn.setFontSize(11 * scale);
            btn.setPreferredSize(new Vector3f(80 * scale, 20 * scale, 0));
            if (filterRarity.isEmpty() && opt.equals("All")) btn.setColor(ColorRGBA.Yellow);
            else if (opt.equals(filterRarity)) btn.setColor(ColorRGBA.Yellow);
            else btn.setColor(ColorRGBA.White);
            btn.addClickCommands((source) -> {
                filterRarity = opt.equals("All") ? "" : opt;
                loadLots(1);
            });
            rarityContainer.addChild(btn);
        }
        windowNode.attachChild(rarityContainer);
        dynamicParts.add(rarityContainer);
        y -= 35 * scale;

        // Фильтр уровня
        Container lvlContainer = new Container();
        lvlContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        lvlContainer.setPreferredSize(new Vector3f(winW - 30 * scale, 25 * scale, 0));
        lvlContainer.setLocalTranslation(15 * scale + leftShift, y, 0.1f);

        Label lvlLabel = new Label("Min Lvl:");
        lvlLabel.setFontSize(12 * scale);
        lvlLabel.setColor(ColorRGBA.White);
        lvlContainer.addChild(lvlLabel);

        TextField levelField = new TextField("1");
        levelField.setFontSize(12 * scale);
        levelField.setPreferredSize(new Vector3f(40 * scale, 20 * scale, 0));
        lvlContainer.addChild(levelField);

        Button lvlApplyBtn = new Button("Go");
        lvlApplyBtn.setFontSize(12 * scale);
        lvlApplyBtn.setPreferredSize(new Vector3f(30 * scale, 20 * scale, 0));
        lvlApplyBtn.addClickCommands((source) -> {
            try {
                filterMinLevel = Math.max(1, Integer.parseInt(levelField.getText()));
                loadLots(1);
            } catch (Exception ignored) {}
        });
        lvlContainer.addChild(lvlApplyBtn);

        windowNode.attachChild(lvlContainer);
        dynamicParts.add(lvlContainer);
        y -= 40 * scale;

        float listStartY = y;

        // Контейнер списка + пагинации
        listAndPaginationContainer = new Container();
        listAndPaginationContainer.setLayout(new SpringGridLayout(Axis.Y, Axis.X));
        float containerWidth = winW - 40 * scale;
        listAndPaginationContainer.setPreferredSize(new Vector3f(containerWidth, 100 * scale, 0));
        listAndPaginationContainer.setLocalTranslation(15 * scale + leftShift, listStartY, 0.1f);
        windowNode.attachChild(listAndPaginationContainer);
        dynamicParts.add(listAndPaginationContainer);

        paginationContainer = new Container();
        paginationContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        paginationContainer.setPreferredSize(new Vector3f(containerWidth, PAGINATION_HEIGHT * scale, 0));

        // Тултип
        tooltipContainer = new Container();
        float tooltipWidth = 200 * scale;
        float tooltipHeight = 150 * scale;
        tooltipContainer.setPreferredSize(new Vector3f(tooltipWidth, tooltipHeight, 0));
        tooltipContainer.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.05f, 0.05f, 0.1f, 0.95f)));
        // Позиционируем: отступ от правого края 10 пикселей, по вертикали центрируем
        float tooltipX = winW + 30 * scale + leftShift; // сдвиг вправо
        float tooltipY = winH / 2 - tooltipHeight / 2;
        tooltipContainer.setLocalTranslation(tooltipX, tooltipY, 0.1f);
        tooltipContainer.setCullHint(Node.CullHint.Always);
        windowNode.attachChild(tooltipContainer);
        dynamicParts.add(tooltipContainer);

        tooltipLabel = new Label("");
        tooltipLabel.setFontSize(12 * scale);
        tooltipLabel.setColor(ColorRGBA.White);
        tooltipLabel.setPreferredSize(new Vector3f(tooltipWidth - 10 * scale, tooltipHeight - 10 * scale, 0));
        tooltipLabel.setInsets(new Insets3f(5 * scale, 5 * scale, 5 * scale, 5 * scale));
        tooltipContainer.addChild(tooltipLabel);

        loadLots(1);
    }

    private void loadLots(int page) {
        this.currentPage = page;
        if (networkManager == null) {
            updateStatus("No network connection");
            return;
        }

        networkManager.getAuctionList(page, filterType, filterRarity, filterMinLevel, filterMaxLevel)
            .thenAccept(response -> {
                app.enqueue(() -> {
                    if (response == null) {
                        updateStatus("Failed to load auctions.");
                        return null;
                    }
                    this.currentLots = response.getLots();
                    this.totalPages = response.getTotalPages();
                    updateLotList();
                    return null;
                });
            });
    }

    private void updateLotList() {
        if (listAndPaginationContainer == null) return;
        listAndPaginationContainer.detachAllChildren();

        float rowWidth = listAndPaginationContainer.getPreferredSize().x - 10 * scale;
        if (rowWidth <= 0) rowWidth = 600 * scale;
        float rowHeightScaled = ROW_HEIGHT * scale;

        int displayCount = Math.min(currentLots.size(), 5);

        if (displayCount == 0) {
            Label empty = new Label("No auction lots found.");
            empty.setFontSize(12 * scale);
            empty.setColor(ColorRGBA.Gray);
            listAndPaginationContainer.addChild(empty);
        } else {
            for (int i = 0; i < displayCount; i++) {
                AuctionLot lot = currentLots.get(i);
                Container row = new Container();
                row.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
                row.setPreferredSize(new Vector3f(rowWidth, rowHeightScaled, 0));
                row.setInsets(new Insets3f(2 * scale, 2 * scale, 2 * scale, 2 * scale));

                String itemName = lot.getItems().isEmpty() ? "Empty" : lot.getItems().get(0).getName();
                Label infoLabel = new Label("[" + lot.getItems().size() + "] " + itemName + " | " + lot.getPrice() + "g");
                infoLabel.setFontSize(12 * scale);
                infoLabel.setColor(ColorRGBA.White);
                infoLabel.setPreferredSize(new Vector3f(380 * scale, rowHeightScaled - 4 * scale, 0));
                row.addChild(infoLabel);

                if (lot.getSellerName().equals(playerManager.getPlayerName())) {
                    Label ownerLabel = new Label(" (Yours)");
                    ownerLabel.setFontSize(12 * scale);
                    ownerLabel.setColor(ColorRGBA.Gray);
                    ownerLabel.setPreferredSize(new Vector3f(80 * scale, rowHeightScaled - 4 * scale, 0));
                    row.addChild(ownerLabel);
                } else {
                    Button buyBtn = new Button("Buy");
                    buyBtn.setFontSize(11 * scale);
                    buyBtn.setPreferredSize(new Vector3f(60 * scale, rowHeightScaled - 4 * scale, 0));
                    buyBtn.setColor(ColorRGBA.Green);
                    final int lotId = lot.getId();
                    buyBtn.addClickCommands(s -> handleBuyLot(lotId));
                    row.addChild(buyBtn);
                }

                // MouseListener для тултипа
                MouseEventControl.removeListenersFromSpatial(row);
                MouseEventControl.addListenersToSpatial(row, new MouseListener() {
                    @Override
                    public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {
                        if (!lot.getItems().isEmpty()) {
                            Item item = lot.getItems().get(0);
                            String tooltipText = buildTooltipText(item);
                            tooltipLabel.setText(tooltipText);
                            ColorRGBA rarityColor = item.getColor();
                            if (rarityColor != null) {
                                tooltipLabel.setColor(rarityColor);
                            } else {
                                tooltipLabel.setColor(ColorRGBA.White);
                            }
                            tooltipContainer.setCullHint(Node.CullHint.Never);
                        }
                    }

                    @Override
                    public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {
                        tooltipContainer.setCullHint(Node.CullHint.Always);
                    }

                    @Override
                    public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {
                        // не потребляем
                    }

                    @Override
                    public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
                });

                listAndPaginationContainer.addChild(row);
            }
        }

        updatePagination();
        listAndPaginationContainer.addChild(paginationContainer);

        int childCount = listAndPaginationContainer.getChildren().size();
        float totalHeight = childCount * (rowHeightScaled + 2 * scale) + PAGINATION_HEIGHT * scale;
        totalHeight = Math.max(totalHeight, 80 * scale);
        listAndPaginationContainer.setPreferredSize(new Vector3f(
            listAndPaginationContainer.getPreferredSize().x,
            totalHeight,
            0
        ));
    }

    private String buildTooltipText(Item item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.getName()).append("\n");
        sb.append("Level: ").append(item.getLevel()).append("\n");
        sb.append("Rarity: ").append(item.getRarity()).append("\n");
        if (item.getDamage() > 0) {
            sb.append("Damage: +").append(item.getDamage()).append("\n");
        }
        if (item.getDefense() > 0) {
            sb.append("Defense: +").append(item.getDefense()).append("\n");
        }
        if (item.getHealthBonus() > 0) {
            sb.append("Health: +").append(item.getHealthBonus()).append("\n");
        }
        if (item.getManaBonus() > 0) {
            sb.append("Mana: +").append(item.getManaBonus()).append("\n");
        }
        if (item.getSocketCount() > 0) {
            sb.append("Sockets: ").append(item.getSocketCount()).append("\n");
        }
        if (!item.getRunes().isEmpty()) {
            sb.append("Runes: ").append(item.getRunes().size()).append("\n");
        }
        sb.append("Type: ").append(item.getType());
        return sb.toString();
    }

    private void updatePagination() {
        paginationContainer.detachAllChildren();
        paginationContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));

        Button prevBtn = new Button("<<");
        prevBtn.setFontSize(11 * scale);
        prevBtn.setPreferredSize(new Vector3f(40 * scale, 22 * scale, 0));
        prevBtn.addClickCommands(s -> { if (currentPage > 1) loadLots(currentPage - 1); });
        paginationContainer.addChild(prevBtn);

        Label pageNum = new Label("Page " + currentPage + " / " + totalPages);
        pageNum.setFontSize(12 * scale);
        pageNum.setPreferredSize(new Vector3f(150 * scale, 22 * scale, 0));
        pageNum.setColor(ColorRGBA.White);
        paginationContainer.addChild(pageNum);

        Button nextBtn = new Button(">>");
        nextBtn.setFontSize(11 * scale);
        nextBtn.setPreferredSize(new Vector3f(40 * scale, 22 * scale, 0));
        nextBtn.addClickCommands(s -> { if (currentPage < totalPages) loadLots(currentPage + 1); });
        paginationContainer.addChild(nextBtn);
    }

    // ---------- Вкладка продажи ----------
    private void showSellTab() {
        clearDynamicParts();

        float startY = winH - 110 * scale;
        float cellSize = 40 * scale;
        float padding = 4f;
        float borderSize = 2f * scale;

        Label header = new Label("Select ONE item to sell, then click \"List for Auction\":");
        header.setFontSize(14 * scale);
        header.setColor(ColorRGBA.White);
        header.setPreferredSize(new Vector3f(winW - 30 * scale, 30 * scale, 0));
        header.setLocalTranslation(15 * scale + leftShift, startY, 0.1f);
        windowNode.attachChild(header);
        dynamicParts.add(header);

        Container gridContainer = new Container();
        SpringGridLayout gridLayout = new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None);
        gridContainer.setLayout(gridLayout);
        gridContainer.setPreferredSize(new Vector3f(550 * scale, 240 * scale, 0));
        gridContainer.setLocalTranslation(15 * scale + leftShift, startY - 40 * scale, 0.1f);
        gridContainer.setBackground(null);
        windowNode.attachChild(gridContainer);
        dynamicParts.add(gridContainer);

        List<Item> items = inventoryManager.getItems();
        int itemIndex = 0;

        for (int row = 0; row < 5; row++) {
            Container rowContainer = new Container();
            SpringGridLayout rowLayout = new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None);
            rowContainer.setLayout(rowLayout);
            rowContainer.setPreferredSize(new Vector3f(550 * scale, cellSize, 0));
            rowContainer.setInsets(new Insets3f(padding, padding, padding, padding));
            rowContainer.setBackground(null);
            gridContainer.addChild(rowContainer);

            for (int col = 0; col < 4; col++) {
                if (itemIndex >= 20) break;

                Item item = (itemIndex < items.size()) ? items.get(itemIndex) : null;
                int realSlot = (item != null) ? inventoryManager.getSlotIndex(item) : -1;

                Container outerContainer = new Container();
                outerContainer.setPreferredSize(new Vector3f(cellSize, cellSize, 0));
                outerContainer.setBackground(null);
                SpringGridLayout outerLayout = new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None);
                outerContainer.setLayout(outerLayout);

                Container innerContainer = new Container();
                innerContainer.setPreferredSize(new Vector3f(cellSize - borderSize * 2, cellSize - borderSize * 2, 0));
                innerContainer.setBackground(null);
                SpringGridLayout innerLayout = new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None);
                innerContainer.setLayout(innerLayout);

                Label slotLabel = new Label(item != null ? item.getName().substring(0, 1) : " ");
                slotLabel.setPreferredSize(new Vector3f(cellSize - borderSize * 2, cellSize - borderSize * 2, 0));

                if (item != null) {
                    Texture tex = null;
                    try {
                        tex = app.getAssetManager().loadTexture(item.getIconPath());
                    } catch (Exception ignored) {}

                    if (tex != null) {
                        slotLabel.setBackground(new QuadBackgroundComponent(tex));
                        slotLabel.setText("");
                    } else {
                        slotLabel.setBackground(new QuadBackgroundComponent(item.getFallbackColor()));
                        slotLabel.setText(item.getName().substring(0, 1));
                        slotLabel.setFontSize(14 * scale);
                        slotLabel.setColor(ColorRGBA.Black);
                    }

                    if (realSlot == selectedSlot) {
                        outerContainer.setBackground(new QuadBackgroundComponent(ColorRGBA.Yellow));
                    } else {
                        outerContainer.setBackground(null);
                    }

                    final int slot = realSlot;
                    MouseEventControl.removeListenersFromSpatial(outerContainer);
                    MouseEventControl.addListenersToSpatial(outerContainer, new MouseListener() {
                        @Override
                        public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {
                            if (evt.isPressed() && evt.getButtonIndex() == 0) {
                                evt.setConsumed();
                                if (selectedSlot == slot) {
                                    selectedSlot = -1;
                                } else {
                                    selectedSlot = slot;
                                }
                                showSellTab();
                            }
                        }
                        @Override public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
                        @Override public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
                        @Override public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
                    });
                } else {
                    slotLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f)));
                    slotLabel.setText("");
                    outerContainer.setBackground(null);
                }

                innerContainer.addChild(slotLabel);
                outerContainer.addChild(innerContainer);
                rowContainer.addChild(outerContainer);
                itemIndex++;
            }
        }

        float gridBottomY = startY - 40 * scale - 240 * scale;
        float priceY = gridBottomY - 35 * scale;

        Container priceContainer = new Container();
        SpringGridLayout priceLayout = new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None);
        priceContainer.setLayout(priceLayout);
        priceContainer.setPreferredSize(new Vector3f(550 * scale, 30 * scale, 0));
        priceContainer.setLocalTranslation(15 * scale + leftShift, priceY, 0.1f);
        priceContainer.setBackground(null);
        windowNode.attachChild(priceContainer);
        dynamicParts.add(priceContainer);

        Label priceLabel = new Label("Price (Gold):");
        priceLabel.setFontSize(14 * scale);
        priceContainer.addChild(priceLabel);

        priceInput = new TextField("100");
        priceInput.setPreferredSize(new Vector3f(80 * scale, 20 * scale, 0));
        priceInput.setFontSize(14 * scale);
        priceContainer.addChild(priceInput);

        Button sellNowBtn = new Button("List for Auction");
        sellNowBtn.setFontSize(14 * scale);
        sellNowBtn.setPreferredSize(new Vector3f(150 * scale, 25 * scale, 0));
        sellNowBtn.setColor(ColorRGBA.Green);
        sellNowBtn.addClickCommands(s -> {
            if (selectedSlot == -1) {
                updateStatus("Please select an item first.");
                return;
            }
            Item selectedItem = inventoryManager.getItemAtSlot(selectedSlot);
            if (selectedItem == null) {
                updateStatus("Selected slot is empty.");
                return;
            }
            int price = 100;
            try { price = Integer.parseInt(priceInput.getText()); } catch (Exception ignored) {}
            handleCreateLot(Arrays.asList(selectedSlot), price);
        });
        priceContainer.addChild(sellNowBtn);
    }

    // ============================================================
    //   ОБРАБОТЧИКИ ПОКУПКИ / ПРОДАЖИ
    // ============================================================
    private void handleBuyLot(int lotId) {
        System.out.println("[AuctionWindow] Attempting to buy lot " + lotId);
        networkManager.buyAuctionLot(lotId).thenAccept(response -> {
            if (response != null) {
                app.enqueue(() -> {
                    System.out.println("[AuctionWindow] Buy successful! Removing lot " + lotId);
                    boolean removed = currentLots.removeIf(lot -> lot.getId() == lotId);
                    System.out.println("[AuctionWindow] Removed: " + removed + ", remaining: " + currentLots.size());
                    updateLotList();
                    updateStatus("Lot purchased successfully!");
                    uiManager.applyCharacterData(response);
                });
            } else {
                app.enqueue(() -> updateStatus("Failed to buy lot: server returned null."));
            }
        }).exceptionally(ex -> {
            app.enqueue(() -> {
                System.err.println("[AuctionWindow] Exception: " + ex.getMessage());
                updateStatus("Network error: " + ex.getMessage());
            });
            return null;
        });
    }

    private void handleCreateLot(List<Integer> slotIndices, int price) {
        System.out.println("[AuctionWindow] handleCreateLot: slots=" + slotIndices + ", price=" + price);
        networkManager.createAuctionLot(slotIndices, price).thenAccept(response -> {
            app.enqueue(() -> {
                if (response == null) {
                    updateStatus("Failed to create lot: server returned null.");
                    return;
                }
                if (response.containsKey("error")) {
                    String errorMsg = (String) response.get("error");
                    updateStatus("Error: " + errorMsg);
                    return;
                }
                System.out.println("[AuctionWindow] Received characterData, applying...");
                uiManager.applyCharacterData(response);
                updateStatus("Lot successfully listed for " + price + "g!");
                selectedSlot = -1;
                showSellTab();
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> {
                System.err.println("[AuctionWindow] Exception: " + ex.getMessage());
                updateStatus("Network error: " + ex.getMessage());
            });
            return null;
        });
    }

    private void updateStatus(String msg) {
        if (statusLabel == null) {
            statusLabel = new Label(msg);
            statusLabel.setFontSize(14 * scale);
            statusLabel.setColor(ColorRGBA.Red);
            statusLabel.setLocalTranslation(15 * scale + leftShift, 15 * scale, 0.1f);
            windowNode.attachChild(statusLabel);
            dynamicParts.add(statusLabel);
        } else {
            statusLabel.setText(msg);
        }
    }

    // ============================================================
    //   УПРАВЛЕНИЕ ВИДИМОСТЬЮ
    // ============================================================
    public void show() {
        if (isVisible) return;
        isVisible = true;
        updateGold();
        if (windowNode.getParent() == null) {
            uiManager.getGuiNode().attachChild(windowNode);
        }
        uiManager.onTraderOpened(windowNode);
    }

    public void hide() {
        if (!isVisible) return;
        isVisible = false;
        selectedSlot = -1;
        if (windowNode.getParent() != null) {
            uiManager.getGuiNode().detachChild(windowNode);
        }
        if (statusLabel != null) {
            statusLabel.setText("");
            statusLabel.setCullHint(Node.CullHint.Always);
        }
        if (tooltipContainer != null) {
            tooltipContainer.setCullHint(Node.CullHint.Always);
        }
        uiManager.onTraderClosed(windowNode);
    }

    public void toggle() {
        if (isVisible) hide(); else show();
    }

    public void updateGold() {
        if (goldLabel != null) {
            goldLabel.setText("Gold: " + playerManager.getGold());
        }
    }

    public void updateLayout(int screenWidth, int screenHeight) {
        if (isVisible) {
            windowNode.detachAllChildren();
            createWindow();
            uiManager.getGuiNode().attachChild(windowNode);
        }
    }

    public boolean isVisible() {
        return isVisible;
    }
}