package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Slider;

public class SettingsWindow {

    private final SimpleApplication app;
    private final UIManager uiManager;

    private Node windowNode;

    private boolean isVisible = false;

    private float scale = 1f;
    private float winW;
    private float winH;

    /**
     * Шрифт, используемый только этим окном.
     * Глобальный стиль Lemur не изменяем.
     */
    private BitmapFont currentFont;

    /**
     * Слайдер громкости.
     */
    private Slider soundSlider;

    /**
     * Текстовое отображение громкости.
     */
    private Label soundValueLabel;

    public SettingsWindow(
            SimpleApplication app,
            UIManager uiManager
    ) {
        this.app = app;
        this.uiManager = uiManager;

        loadCurrentFont();
        createWindow();
    }

    // =============================================================
    // ЗАГРУЗКА ШРИФТА
    // =============================================================

    private void loadCurrentFont() {

        String language =
                SettingsManager.getInstance()
                        .getLanguage();

        if ("ru".equalsIgnoreCase(language)) {

            try {

                currentFont =
                        app.getAssetManager()
                                .loadFont(
                                        "Interface/Fonts/ru.fnt"
                                );

                System.out.println(
                        "[SettingsWindow] Russian font loaded"
                );

            } catch (Exception e) {

                System.err.println(
                        "[SettingsWindow] Failed to load Russian font: "
                                + e.getMessage()
                );

                currentFont = null;
            }

        } else {

            try {

                currentFont =
                        app.getAssetManager()
                                .loadFont(
                                        "Interface/Fonts/en.fnt"
                                );

                System.out.println(
                        "[SettingsWindow] English font loaded"
                );

            } catch (Exception e) {

                System.err.println(
                        "[SettingsWindow] Failed to load English font: "
                                + e.getMessage()
                );

                currentFont = null;
            }
        }
    }

    // =============================================================
    // SCALE
    // =============================================================

    private void updateScale() {

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        float baseWidth = 800f;
        float baseHeight = 600f;

        float scaleX =
                screenWidth / baseWidth;

        float scaleY =
                screenHeight / baseHeight;

        scale =
                Math.min(
                        scaleX,
                        scaleY
                );

        scale =
                Math.max(
                        0.5f,
                        Math.min(
                                scale,
                                1.5f
                        )
                );
    }

    // =============================================================
    // СОЗДАНИЕ ОКНА
    // =============================================================

    private void createWindow() {

        updateScale();

        windowNode =
                new Node(
                        "SettingsWindowNode"
                );

        /*
         * Разрешение полностью удалено.
         *
         * Поэтому окно стало меньше.
         */
        winW = 420f * scale;
        winH = 360f * scale;

        // =========================================================
        // ФОН
        // =========================================================

        Geometry bg =
                uiManager.createBackgroundGeometry(
                        winW,
                        winH
                );

        bg.setLocalTranslation(
                0,
                0,
                -0.1f
        );

        windowNode.attachChild(bg);

        // =========================================================
        // ЗАГОЛОВОК
        // =========================================================

        Label titleLabel =
                createLabel(
                        getLocalized(
                                "settings.title"
                        ),
                        24f
                );

        titleLabel.setLocalTranslation(
                winW / 2f - 80f * scale,
                winH - 30f * scale,
                0.1f
        );

        windowNode.attachChild(
                titleLabel
        );

        // =========================================================
        // КНОПКА ЗАКРЫТИЯ
        // =========================================================

        Button closeButton =
                createButton(
                        "X",
                        14f
                );

        closeButton.setPreferredSize(
                new Vector3f(
                        25f * scale,
                        25f * scale,
                        0
                )
        );

        closeButton.setLocalTranslation(
                winW - 35f * scale,
                winH - 30f * scale,
                0.1f
        );

        closeButton.addClickCommands(
                source -> hide()
        );

        windowNode.attachChild(
                closeButton
        );

        // =========================================================
        // НАЧАЛЬНАЯ ПОЗИЦИЯ
        // =========================================================

        float yPos =
                winH - 75f * scale;

        float stepY =
                48f * scale;

        // =========================================================
        // ЗВУК
        // =========================================================

        Label soundLabel =
                createLabel(
                        getLocalized(
                                "settings.sound"
                        ),
                        16f
                );

        soundLabel.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        windowNode.attachChild(
                soundLabel
        );

        yPos -= 30f * scale;

        // =========================================================
        // СЛАЙДЕР ГРОМКОСТИ
        // =========================================================

        soundSlider =
                new Slider(
                        Axis.X
                );

        soundSlider.setPreferredSize(
                new Vector3f(
                        200f * scale,
                        20f * scale,
                        0
                )
        );

        float initialVolume =
                SettingsManager.getInstance()
                        .getSoundVolume();

        /*
         * В Lemur значение модели задаётся напрямую.
         *
         * 0   = 0%
         * 50  = 50%
         * 100 = 100%
         */
        soundSlider.getModel()
                .setValue(
                        initialVolume * 100f
                );

        soundSlider.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        windowNode.attachChild(
                soundSlider
        );

        /*
         * Отключаем встроенные кнопки +/-.
         */
        soundSlider
                .getIncrementButton()
                .setEnabled(false);

        soundSlider
                .getDecrementButton()
                .setEnabled(false);

        // =========================================================
        // ПРОЦЕНТ ГРОМКОСТИ
        // =========================================================

        soundValueLabel =
                createLabel(
                        Math.round(
                                initialVolume * 100f
                        ) + "%",
                        14f
                );

        soundValueLabel.setLocalTranslation(
                230f * scale,
                yPos + 2f * scale,
                0.1f
        );

        windowNode.attachChild(
                soundValueLabel
        );

        /*
         * Важно:
         *
         * Здесь намеренно НЕ используется:
         *
         * addChangeListener()
         * getFloat()
         *
         * потому что в используемой тобой версии
         * Lemur RangedValueModel этих методов нет.
         */

        yPos -= stepY;

        // =========================================================
        // ЯЗЫК
        // =========================================================

        Label langLabel =
                createLabel(
                        getLocalized(
                                "settings.language"
                        ),
                        16f
                );

        langLabel.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        windowNode.attachChild(
                langLabel
        );

        yPos -= 32f * scale;

        // =========================================================
        // КНОПКА ЯЗЫКА
        // =========================================================

        Button langButton =
                createButton(
                        getLocalized(
                                "settings.language.switch"
                        ),
                        18f
                );

        langButton.setPreferredSize(
                new Vector3f(
                        180f * scale,
                        30f * scale,
                        0
                )
        );

        langButton.setColor(
                ColorRGBA.White
        );

        langButton.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        langButton.addClickCommands(
                source -> changeLanguage()
        );

        windowNode.attachChild(
                langButton
        );

        yPos -= stepY;

        // =========================================================
        // СБРОС НАСТРОЕК
        // =========================================================

        

        yPos -= stepY;

        // =========================================================
        // КНОПКА ПЕРЕЗАПУСКА
        // =========================================================

        

        // =========================================================
        // ПОЗИЦИЯ ОКНА
        // =========================================================

        positionWindow();
    }

    // =============================================================
    // СОЗДАНИЕ LABEL
    // =============================================================

    private Label createLabel(
            String text,
            float fontSize
    ) {

        Label label =
                new Label(text);

        if (currentFont != null) {

            label.setFont(
                    currentFont
            );
        }

        label.setFontSize(
                fontSize * scale
        );

        label.setColor(
                ColorRGBA.White
        );

        return label;
    }

    // =============================================================
    // СОЗДАНИЕ BUTTON
    // =============================================================

    private Button createButton(
            String text,
            float fontSize
    ) {

        Button button =
                new Button(text);

        if (currentFont != null) {

            button.setFont(
                    currentFont
            );
        }

        button.setFontSize(
                fontSize * scale
        );

        return button;
    }

    // =============================================================
    // СМЕНА ЯЗЫКА
    // =============================================================

    private void changeLanguage() {

        String currentLang =
                SettingsManager.getInstance()
                        .getLanguage();

        String newLang =
                "en".equalsIgnoreCase(
                        currentLang
                )
                        ? "ru"
                        : "en";

        // Сохраняем язык
        SettingsManager.getInstance()
                .setLanguage(
                        newLang
                );

        // Загружаем локализацию
        LocalizationManager.getInstance()
                .loadLanguage(
                        newLang
                );

        // Загружаем новый шрифт
        loadCurrentFont();

        // Пересоздаём окно
        refreshUI();
    }

    // =============================================================
    // СБРОС НАСТРОЕК
    // =============================================================

    private void resetSettings() {

        System.out.println(
                "[Settings] Reset settings"
        );

        // =========================================================
        // ГРОМКОСТЬ = 50%
        // =========================================================

        SettingsManager.getInstance()
                .setSoundVolume(
                        0.5f
                );

        // =========================================================
        // ЯЗЫК = ENGLISH
        // =========================================================

        SettingsManager.getInstance()
                .setLanguage(
                        "en"
                );

        LocalizationManager.getInstance()
                .loadLanguage(
                        "en"
                );

        loadCurrentFont();

        /*
         * ========================================================
         * РАЗРЕШЕНИЕ НЕ ТРОГАЕМ
         * ========================================================
         *
         * Здесь специально отсутствуют:
         *
         * setScreenWidth()
         * setScreenHeight()
         * app.setSettings()
         * app.restart()
         */

        refreshUI();

        System.out.println(
                "[Settings] Reset complete: "
                        + "volume=50%, language=en"
        );
    }

    // =============================================================
    // ПОЗИЦИЯ ОКНА
    // =============================================================

    private void positionWindow() {

        float w =
                app.getCamera().getWidth();

        float h =
                app.getCamera().getHeight();

        float offsetY =
                40f * scale
                        + 90f * scale;

        windowNode.setLocalTranslation(
                (w - winW) / 2f,
                (h - winH) / 2f + offsetY,
                0
        );
    }

    // =============================================================
    // UPDATE
    // =============================================================

    public void update() {

        /*
         * Здесь пока ничего не делаем.
         *
         * Обновление настроек осуществляется
         * через обработчики кнопок.
         */
    }

    // =============================================================
    // ОБНОВЛЕНИЕ UI
    // =============================================================

    private void refreshUI() {

        boolean wasVisible =
                isVisible;

        // Удаляем старое окно
        if (windowNode != null) {

            if (windowNode.getParent() != null) {

                windowNode.getParent()
                        .detachChild(
                                windowNode
                        );
            }
        }

        isVisible = false;

        // Обновляем локализацию
        LocalizationManager.getInstance()
                .loadLanguage(
                        SettingsManager.getInstance()
                                .getLanguage()
                );

        // Обновляем шрифт
        loadCurrentFont();

        // Создаём окно заново
        createWindow();

        // Возвращаем видимость
        if (wasVisible) {
            show();
        }
    }

    // =============================================================
    // ЛОКАЛИЗАЦИЯ
    // =============================================================

    private String getLocalized(
            String key
    ) {

        return LocalizationManager
                .getInstance()
                .get(key);
    }

    // =============================================================
    // SHOW
    // =============================================================

    public void show() {

        if (isVisible) {
            return;
        }

        isVisible = true;

        windowNode.setCullHint(
                Node.CullHint.Dynamic
        );

        uiManager.attachNode(
                windowNode
        );
    }

    // =============================================================
    // HIDE
    // =============================================================

    public void hide() {

        if (!isVisible) {
            return;
        }

        isVisible = false;

        windowNode.setCullHint(
                Node.CullHint.Always
        );

        uiManager.detachNode(
                windowNode
        );
    }

    // =============================================================
    // VISIBLE
    // =============================================================

    public boolean isVisible() {
        return isVisible;
    }
}