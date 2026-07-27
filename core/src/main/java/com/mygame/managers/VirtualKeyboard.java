package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.QuadBackgroundComponent;

import java.util.ArrayList;
import java.util.List;

public class VirtualKeyboard {
    private SimpleApplication app;
    private Node guiNode;
    private Node keyboardNode;
    private Geometry background;
    private TextField activeField;
    private boolean shift = false;
    private boolean isVisible = false;

    private float currentHeight = 0;

    // Слушатели
    public interface OnShowListener {
        void onShow(float keyboardHeight);
    }
    public interface OnHideListener {
        void onHide();
    }
    private OnShowListener showListener;
    private OnHideListener hideListener;

    private static final String[] ROW1 = {"q","w","e","r","t","y","u","i","o","p"};
    private static final String[] ROW2 = {"a","s","d","f","g","h","j","k","l"};
    private static final String[] ROW3 = {"z","x","c","v","b","n","m"};

    private List<Button> allButtons = new ArrayList<>();

    public VirtualKeyboard(SimpleApplication app, Node guiNode) {
        this.app = app;
        this.guiNode = guiNode;
        keyboardNode = new Node("VirtualKeyboard");
        keyboardNode.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(keyboardNode);
    }

    public void setOnShowListener(OnShowListener listener) {
        this.showListener = listener;
    }

    public void setOnHideListener(OnHideListener listener) {
        this.hideListener = listener;
    }

    public void show(TextField target) {
        if (target == null) return;
        this.activeField = target;
        buildKeyboard();
        keyboardNode.setCullHint(Spatial.CullHint.Dynamic);
        isVisible = true;
        if (showListener != null) {
            showListener.onShow(currentHeight);
        }
    }

    public void hide() {
        keyboardNode.setCullHint(Spatial.CullHint.Always);
        isVisible = false;
        activeField = null;
        shift = false;
        if (hideListener != null) {
            hideListener.onHide();
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void rebuild() {
        if (isVisible && activeField != null) {
            keyboardNode.detachAllChildren();
            allButtons.clear();
            buildKeyboard();
            if (showListener != null) {
                showListener.onShow(currentHeight);
            }
        }
    }

    public Node getContainer() {
        return keyboardNode;
    }

    public float getHeight() {
        return currentHeight;
    }

    private void buildKeyboard() {
        keyboardNode.detachAllChildren();
        allButtons.clear();

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float keyWidth = screenWidth / 11f;
        float keyHeight = screenHeight / 9f;
        keyWidth = Math.min(keyWidth, 80f);
        keyHeight = Math.min(keyHeight, 60f);
        float spacing = 4f;

        float totalHeight = keyHeight * 4 + spacing * 3 + 20;
        currentHeight = totalHeight;

        // Фон
        Quad bgQuad = new Quad(screenWidth, totalHeight);
        background = new Geometry("KeyboardBg", bgQuad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.3f, 0.3f, 0.35f, 0.9f));
        background.setMaterial(mat);
        background.setLocalTranslation(0, 0, -0.1f);
        keyboardNode.attachChild(background);

        // Ряд 1
        float startX = (screenWidth - (ROW1.length * (keyWidth + spacing) - spacing)) / 2;
        float yPos = totalHeight - keyHeight - 10;
        for (int i = 0; i < ROW1.length; i++) {
            Button btn = createKey(ROW1[i], keyWidth, keyHeight);
            btn.setLocalTranslation(startX + i * (keyWidth + spacing), yPos, 0.1f);
            keyboardNode.attachChild(btn);
            allButtons.add(btn);
        }

        // Ряд 2
        startX = (screenWidth - (ROW2.length * (keyWidth + spacing) - spacing)) / 2;
        yPos -= keyHeight + spacing;
        for (int i = 0; i < ROW2.length; i++) {
            Button btn = createKey(ROW2[i], keyWidth, keyHeight);
            btn.setLocalTranslation(startX + i * (keyWidth + spacing), yPos, 0.1f);
            keyboardNode.attachChild(btn);
            allButtons.add(btn);
        }

        // Ряд 3
        startX = (screenWidth - (ROW3.length * (keyWidth + spacing) - spacing)) / 2;
        yPos -= keyHeight + spacing;
        for (int i = 0; i < ROW3.length; i++) {
            Button btn = createKey(ROW3[i], keyWidth, keyHeight);
            btn.setLocalTranslation(startX + i * (keyWidth + spacing), yPos, 0.1f);
            keyboardNode.attachChild(btn);
            allButtons.add(btn);
        }

        // Специальный ряд
        yPos -= keyHeight + spacing;
        float specialWidth = keyWidth * 1.4f;
        float spaceWidth = keyWidth * 3.5f;

        Button shiftBtn = createSpecialKey("Shift", specialWidth, keyHeight);
        shiftBtn.setLocalTranslation(10, yPos, 0.1f);
        shiftBtn.addClickCommands((source) -> {
            shift = !shift;
            if (shift) {
                shiftBtn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.6f, 0.6f, 0.2f, 0.9f)));
            } else {
                shiftBtn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.4f, 0.9f)));
            }
        });
        keyboardNode.attachChild(shiftBtn);
        allButtons.add(shiftBtn);

        Button spaceBtn = createSpecialKey(" ", spaceWidth, keyHeight);
        float spaceX = (screenWidth - spaceWidth) / 2;
        spaceBtn.setLocalTranslation(spaceX, yPos, 0.1f);
        spaceBtn.addClickCommands((source) -> {
            if (activeField != null) {
                activeField.setText(activeField.getText() + " ");
            }
        });
        keyboardNode.attachChild(spaceBtn);
        allButtons.add(spaceBtn);

        Button backBtn = createSpecialKey("⌫", specialWidth, keyHeight);
        backBtn.setLocalTranslation(screenWidth - specialWidth - 10, yPos, 0.1f);
        backBtn.addClickCommands((source) -> {
            if (activeField != null) {
                String text = activeField.getText();
                if (text.length() > 0) {
                    activeField.setText(text.substring(0, text.length() - 1));
                }
            }
        });
        keyboardNode.attachChild(backBtn);
        allButtons.add(backBtn);

        Button enterBtn = createSpecialKey("↵", specialWidth, keyHeight);
        enterBtn.setLocalTranslation(screenWidth - specialWidth*2 - 15, yPos, 0.1f);
        enterBtn.addClickCommands((source) -> hide());
        keyboardNode.attachChild(enterBtn);
        allButtons.add(enterBtn);

        keyboardNode.setLocalTranslation(0, 0, 0);
    }

    private Button createKey(String label, float w, float h) {
        Button btn = new Button(label);
        btn.setPreferredSize(new Vector3f(w, h, 0));
        btn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.4f, 0.9f)));
        btn.setColor(ColorRGBA.White);
        btn.setFontSize(Math.min(w, h) * 0.4f);

        btn.addClickCommands((source) -> {
            if (activeField != null && label.length() == 1) {
                String current = activeField.getText();
                char ch = label.charAt(0);
                if (Character.isLetter(ch)) {
                    ch = shift ? Character.toUpperCase(ch) : Character.toLowerCase(ch);
                }
                activeField.setText(current + ch);
            }
        });
        return btn;
    }

    private Button createSpecialKey(String label, float w, float h) {
        Button btn = new Button(label);
        btn.setPreferredSize(new Vector3f(w, h, 0));
        btn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.5f, 0.9f)));
        btn.setColor(ColorRGBA.White);
        btn.setFontSize(Math.min(w, h) * 0.35f);
        return btn;
    }
}