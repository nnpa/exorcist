package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;

import java.util.*;

public class TalentWindow {

    private SimpleApplication app;
    private TalentManager talentManager;
    private UIManager uiManager;
    private Node windowNode;
    private boolean isVisible = false;

    private Label pointsLabel;
    private Label tooltipLabel;
    private float tooltipTimer = 0f;
    private boolean tooltipVisible = false;
    private List<Button> talentButtons = new ArrayList<>();
    private Talent.Branch currentBranch = Talent.Branch.DEFENSE;
    private Map<String, Long> lastClickTime = new HashMap<>();

    private float currentWidth = 500;
    private float currentHeight = 550;
    private float buttonSize = 60;
    private float buttonSpacingH = 20;
    private float buttonSpacingV = 30;

    private static final Map<String, String> ICON_MAP = new HashMap<>();
    static {
        ICON_MAP.put("def_1", "talent_shield.png");
        ICON_MAP.put("def_2", "talent_iron_skin.png");
        ICON_MAP.put("def_3", "talent_magic_barrier.png");
        ICON_MAP.put("def_4", "talent_block.png");
        ICON_MAP.put("def_5", "talent_resilience.png");
        ICON_MAP.put("def_6", "talent_regen.png");
        ICON_MAP.put("def_7", "talent_fortify.png");
        ICON_MAP.put("def_8", "talent_impervious.png");
        ICON_MAP.put("def_9", "talent_undying.png");
        ICON_MAP.put("def_10", "talent_aura.png");
        ICON_MAP.put("light_1", "talent_heal.png");
        ICON_MAP.put("light_2", "talent_divine_shield.png");
        ICON_MAP.put("light_3", "talent_holy_strike.png");
        ICON_MAP.put("light_4", "talent_cleanse.png");
        ICON_MAP.put("light_5", "talent_holy_power.png");
        ICON_MAP.put("light_6", "talent_angelic.png");
        ICON_MAP.put("light_7", "talent_blessing.png");
        ICON_MAP.put("light_8", "talent_inspire.png");
        ICON_MAP.put("light_9", "talent_light_nova.png");
        ICON_MAP.put("light_10", "talent_resurrect.png");
        ICON_MAP.put("attack_1", "talent_bash.png");
        ICON_MAP.put("attack_2", "talent_sweep.png");
        ICON_MAP.put("attack_3", "talent_kick.png");
        ICON_MAP.put("attack_4", "talent_wrath.png");
        ICON_MAP.put("attack_5", "talent_critical.png");
        ICON_MAP.put("attack_6", "talent_mastery.png");
        ICON_MAP.put("attack_7", "talent_strength.png");
        ICON_MAP.put("attack_8", "talent_precision.png");
        ICON_MAP.put("attack_9", "talent_lethal.png");
        ICON_MAP.put("attack_10", "talent_rage.png");
    }

    public TalentWindow(SimpleApplication app, TalentManager tm, UIManager ui) {
        this.app = app;
        this.talentManager = tm;
        this.uiManager = ui;
        createTooltip();
        createWindow();
    }

    public boolean isVisible() { return isVisible; }
    public Node getNode() { return windowNode; }

    private void createTooltip() {
        tooltipLabel = new Label("");
        tooltipLabel.setFontSize(14);
        tooltipLabel.setColor(ColorRGBA.White);
        tooltipLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f)));
        tooltipLabel.setPreferredSize(new Vector3f(300, 80, 0));
        tooltipLabel.setLocalTranslation(10, app.getCamera().getHeight() - 90, 0);
        tooltipLabel.setCullHint(Node.CullHint.Always);
        app.getGuiNode().attachChild(tooltipLabel);
    }

    private void createWindow() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            screenWidth = 1280;
            screenHeight = 720;
        }

        float scaleX = screenWidth / 1280f;
        float scaleY = screenHeight / 720f;
        float scale = Math.min(scaleX, scaleY);

        currentWidth = 500 * scale;
        currentHeight = 550 * scale;
        buttonSize = 60 * scale;
        buttonSpacingH = 20 * scale;
        buttonSpacingV = 30 * scale;

        float x = (screenWidth - currentWidth) / 2;
        float y = (screenHeight - currentHeight) / 2 + 48 * scale;

        windowNode = new Node("TalentWindowNode");
        windowNode.setName("TalentWindowNode");
        windowNode.setLocalTranslation(x, y, 0);

        // ===== ФОН через UIManager =====
        if (uiManager != null) {
            Geometry bgGeom = uiManager.createBackgroundGeometry(currentWidth, currentHeight);
            windowNode.attachChild(bgGeom);
        } else {
            // Запасной вариант (если uiManager не задан)
            com.jme3.scene.shape.Quad quad = new com.jme3.scene.shape.Quad(currentWidth, currentHeight);
            Geometry background = new Geometry("Bg", quad);
            com.jme3.material.Material mat = new com.jme3.material.Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.15f, 0.97f));
            background.setMaterial(mat);
            background.setLocalTranslation(0, 0, -0.1f);
            windowNode.attachChild(background);
        }

        // Заголовок
        Label title = new Label("Talents");
        title.setFontSize(24 * scale);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(currentWidth / 2 - 45 * scale, currentHeight - 25 * scale, 0.1f);
        windowNode.attachChild(title);

        // Вкладки
        float tabY = currentHeight - 60 * scale;
        Button defTab = new Button("Defense");
        defTab.setPreferredSize(new Vector3f(90 * scale, 25 * scale, 0));
        defTab.setFontSize(12 * scale);
        defTab.setLocalTranslation(40 * scale, tabY, 0.1f);
        defTab.addClickCommands((source) -> switchBranch(Talent.Branch.DEFENSE));
        windowNode.attachChild(defTab);

        Button lightTab = new Button("Light");
        lightTab.setPreferredSize(new Vector3f(90 * scale, 25 * scale, 0));
        lightTab.setFontSize(12 * scale);
        lightTab.setLocalTranslation(150 * scale, tabY, 0.1f);
        lightTab.addClickCommands((source) -> switchBranch(Talent.Branch.LIGHT));
        windowNode.attachChild(lightTab);

        Button attackTab = new Button("Attack");
        attackTab.setPreferredSize(new Vector3f(90 * scale, 25 * scale, 0));
        attackTab.setFontSize(12 * scale);
        attackTab.setLocalTranslation(260 * scale, tabY, 0.1f);
        attackTab.addClickCommands((source) -> switchBranch(Talent.Branch.ATTACK));
        windowNode.attachChild(attackTab);

        // Очки
        pointsLabel = new Label("Points: 0");
        pointsLabel.setFontSize(16 * scale);
        pointsLabel.setColor(ColorRGBA.White);
        pointsLabel.setLocalTranslation(15 * scale, 25 * scale, 0.1f);
        windowNode.attachChild(pointsLabel);

        // Кнопка сброса
        Button resetButton = new Button("Reset");
        resetButton.setPreferredSize(new Vector3f(70 * scale, 25 * scale, 0));
        resetButton.setFontSize(12 * scale);
        resetButton.setLocalTranslation(currentWidth - 90 * scale, 25 * scale, 0.1f);
        resetButton.addClickCommands((source) -> {
            if (isVisible) {
                talentManager.resetTalents();
                updateUI();
            }
        });
        windowNode.attachChild(resetButton);

        // Кнопка закрытия
        Button closeButton = new Button("X");
        closeButton.setPreferredSize(new Vector3f(25 * scale, 25 * scale, 0));
        closeButton.setFontSize(14 * scale);
        closeButton.setLocalTranslation(currentWidth - 35 * scale, currentHeight - 30 * scale, 0.1f);
        closeButton.addClickCommands((source) -> hide());
        windowNode.attachChild(closeButton);

        switchBranch(Talent.Branch.DEFENSE);
    }

    // ... остальные методы без изменений ...
    private void switchBranch(Talent.Branch branch) {
        this.currentBranch = branch;
        updateUI();
    }

    private void updateUI() {
        for (Button btn : talentButtons) {
            windowNode.detachChild(btn);
        }
        talentButtons.clear();

        TalentTree tree = talentManager.getTrees().get(currentBranch);
        if (tree == null) return;

        List<Talent> talents = new ArrayList<>(tree.getTalents());
        talents.sort((a, b) -> {
            if (a.getRow() != b.getRow()) return Integer.compare(a.getRow(), b.getRow());
            return Integer.compare(a.getColumn(), b.getColumn());
        });

        float totalWidth = 2 * buttonSize + buttonSpacingH;
        float startX = (currentWidth - totalWidth) / 2;
        float startY = currentHeight - 120 * (currentHeight / 550f);

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 2; c++) {
                final int row = r;
                final int col = c;
                Talent found = null;
                for (Talent t : talents) {
                    if (t.getRow() == row && t.getColumn() == col) {
                        found = t;
                        break;
                    }
                }

                float x = startX + c * (buttonSize + buttonSpacingH);
                float y = startY - r * (buttonSize + buttonSpacingV);

                if (found != null) {
                    final Talent talent = found;
                    int level = talentManager.getLearned().getOrDefault(talent.getId(), 0);
                    boolean isAvailable = talentManager.getAvailablePoints() >= talent.getCost() &&
                            tree.isAvailable(talent, talentManager.getLearned());

                    Button btn = new Button("");
                    btn.setPreferredSize(new Vector3f(buttonSize, buttonSize, 0));
                    btn.setLocalTranslation(x, y, 0.1f);

                    if (level >= talent.getMaxLevel()) {
                        btn.setColor(new ColorRGBA(0.8f, 0.7f, 0.1f, 0.9f));
                    } else if (isAvailable) {
                        btn.setColor(new ColorRGBA(0.1f, 0.8f, 0.1f, 0.9f));
                    } else {
                        btn.setColor(new ColorRGBA(0.3f, 0.3f, 0.3f, 0.9f));
                    }

                    String iconName = ICON_MAP.get(talent.getId());
                    if (iconName != null) {
                        try {
                            com.jme3.texture.Texture tex = app.getAssetManager().loadTexture("Interface/Talents/" + iconName);
                            if (tex != null) {
                                QuadBackgroundComponent bg = new QuadBackgroundComponent(tex);
                                btn.setBackground(bg);
                            }
                        } catch (Exception e) {
                            btn.setText(talent.getName());
                            btn.setFontSize(8);
                        }
                    } else {
                        btn.setText(talent.getName());
                        btn.setFontSize(8);
                    }

                    if (level > 0) {
                        Label levelLabel = new Label(level + "/" + talent.getMaxLevel());
                        levelLabel.setFontSize(11);
                        levelLabel.setColor(ColorRGBA.Black);
                        levelLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(1f, 1f, 1f, 0.7f)));
                        levelLabel.setPreferredSize(new Vector3f(40, 14, 0));
                        float labelX = (buttonSize - 40) / 2;
                        float labelY = 1;
                        levelLabel.setLocalTranslation(labelX, labelY, 0.1f);
                        btn.attachChild(levelLabel);
                    }

                    btn.addClickCommands((source) -> {
                        if (!isVisible) return;
                        String id = talent.getId();
                        long now = System.currentTimeMillis();
                        Long last = lastClickTime.get(id);
                        if (last != null && (now - last) < 500) {
                            boolean success = talentManager.levelUpTalent(id);
                            if (success) {
                                updateUI();
                                showTooltip("✓ " + talent.getName() + " upgraded!");
                            } else {
                                showTooltip("✗ Cannot upgrade " + talent.getName());
                            }
                            lastClickTime.remove(id);
                        } else {
                            showTooltip(talent.getDescription());
                            lastClickTime.put(id, now);
                        }
                    });

                    windowNode.attachChild(btn);
                    talentButtons.add(btn);
                }
            }
        }

        if (pointsLabel != null) {
            pointsLabel.setText("Points: " + talentManager.getAvailablePoints());
        }
    }

    private void showTooltip(String text) {
        if (tooltipLabel == null || !isVisible) return;
        tooltipLabel.setText(text);
        tooltipLabel.setCullHint(Node.CullHint.Dynamic);
        tooltipVisible = true;
        tooltipTimer = 3.0f;
    }

    private void hideTooltip() {
        if (tooltipLabel == null) return;
        tooltipLabel.setCullHint(Node.CullHint.Always);
        tooltipVisible = false;
        tooltipTimer = 0f;
    }

    public void update(float tpf) {
        if (tooltipVisible && isVisible) {
            tooltipTimer -= tpf;
            if (tooltipTimer <= 0) {
                hideTooltip();
            }
        }
    }

    public void updateLayout(int screenWidth, int screenHeight) {
        if (isVisible) {
            if (uiManager != null) uiManager.onTalentClosed(windowNode);
            windowNode.detachAllChildren();
            createWindow();
            if (uiManager != null) uiManager.onTalentOpened(windowNode);
        }
    }

    public void show() {
        isVisible = true;
        if (uiManager != null) {
            uiManager.onTalentOpened(windowNode);
        } else {
            if (!app.getGuiNode().hasChild(windowNode)) {
                app.getGuiNode().attachChild(windowNode);
            }
        }
        updateUI();
    }

    public void hide() {
        isVisible = false;
        if (uiManager != null) {
            uiManager.onTalentClosed(windowNode);
        } else {
            if (app.getGuiNode().hasChild(windowNode)) {
                app.getGuiNode().detachChild(windowNode);
            }
        }
        hideTooltip();
    }

    public void toggle() {
        if (isVisible) hide(); else show();
    }
}