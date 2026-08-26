package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture2D;
import com.simsilica.lemur.Button;

/**
 * Окно мини-карты.
 */
public class MapWindow {

    private final SimpleApplication app;
    private final MapRenderer mapRenderer;
    private final PlayerManager playerManager;

    private Node windowNode;

    private Geometry mapTextureGeometry;
    private Geometry playerMarker;

    private boolean isVisible = false;

    /**
     * Размер карты на экране.
     */
    private final float mapSize = 400f;

    /**
     * Размер области мира, отображаемой камерой.
     *
     * Должен совпадать с MapRenderer.viewSize.
     */
    private final float mapViewSize = 40f;

    public MapWindow(
            SimpleApplication app,
            MapRenderer renderer,
            PlayerManager pm
    ) {

        this.app = app;
        this.mapRenderer = renderer;
        this.playerManager = pm;

        createWindow();
    }

    /**
     * Создание GUI окна карты.
     */
    private void createWindow() {

        windowNode = new Node(
                "MapWindow"
        );

        /*
         * ---------------------------------------------------------
         * ПОЛОЖЕНИЕ ОКНА
         * ---------------------------------------------------------
         */

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

float x = screenWidth - mapSize - 20f;
float y = screenHeight - mapSize - 20f;

        /*
         * ---------------------------------------------------------
         * MAP TEXTURE
         * ---------------------------------------------------------
         */

        Quad quad = new Quad(
                mapSize,
                mapSize
        );

        mapTextureGeometry =
                new Geometry(
                        "MapTexture",
                        quad
                );

        Material mapMaterial =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        /*
         * Пока текстура не появилась,
         * показываем тёмный фон.
         */
        mapMaterial.setColor(
                "Color",
                new ColorRGBA(
                        0.05f,
                        0.05f,
                        0.08f,
                        1f
                )
        );

        mapTextureGeometry.setMaterial(
                mapMaterial
        );

        mapTextureGeometry.setLocalTranslation(
                0f,
                0f,
                0f
        );

        windowNode.attachChild(
                mapTextureGeometry
        );

        /*
         * ---------------------------------------------------------
         * PLAYER MARKER
         * ---------------------------------------------------------
         */

        Sphere sphere =
                new Sphere(
                        8,
                        8,
                        6f
                );

        playerMarker =
                new Geometry(
                        "PlayerMarker",
                        sphere
                );

        Material markerMaterial =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        markerMaterial.setColor(
                "Color",
                ColorRGBA.Red
        );

        playerMarker.setMaterial(
                markerMaterial
        );

        /*
         * Игрок изначально в центре.
         */
        playerMarker.setLocalTranslation(
                mapSize / 2f,
                mapSize / 2f,
                0.1f
        );

        windowNode.attachChild(
                playerMarker
        );

        /*
         * ---------------------------------------------------------
         * BORDER
         * ---------------------------------------------------------
         */

        Geometry border =
                new Geometry(
                        "MapBorder",
                        new Quad(
                                mapSize + 4f,
                                mapSize + 4f
                        )
                );

        Material borderMaterial =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        borderMaterial.setColor(
                "Color",
                new ColorRGBA(
                        0.5f,
                        0.5f,
                        0.5f,
                        0.8f
                )
        );

        border.setMaterial(
                borderMaterial
        );

        /*
         * Border должен быть ПОД картой.
         */
        border.setLocalTranslation(
                -2f,
                -2f,
                -0.1f
        );

        windowNode.attachChild(
                border
        );

        /*
         * ---------------------------------------------------------
         * CLOSE BUTTON
         * ---------------------------------------------------------
         */

        

        /*
         * ---------------------------------------------------------
         * POSITION
         * ---------------------------------------------------------
         */

        windowNode.setLocalTranslation(
                x,
                y,
                0f
        );

        /*
         * Сначала скрываем окно.
         */
        windowNode.setCullHint(
                Node.CullHint.Always
        );

        /*
         * Добавляем в GUI.
         */
        app.getGuiNode().attachChild(
                windowNode
        );

        System.out.println(
                "[MapWindow] Created"
        );
    }

    /**
     * Обновление карты.
     */
    public void update() {

        if (!isVisible) {
            return;
        }

        if (mapRenderer == null) {
            return;
        }

        /*
         * ---------------------------------------------------------
         * ТЕКСТУРА
         * ---------------------------------------------------------
         */

        Texture2D texture =
                mapRenderer.getTexture();

        if (texture != null
                && mapTextureGeometry != null) {

            Material material =
                    mapTextureGeometry.getMaterial();

            if (material != null) {

                material.setTexture(
                        "ColorMap",
                        texture
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * PLAYER MARKER
         * ---------------------------------------------------------
         */

        if (playerManager == null
                || playerMarker == null) {

            return;
        }

        Vector3f playerPos =
                playerManager.getPosition();

        if (playerPos == null) {
            return;
        }

        /*
         * Камера карты центрирована на игроке.
         *
         * Поэтому игрок всегда находится
         * примерно в центре карты.
         */
        float pixelX =
                mapSize / 2f;

        float pixelY =
                mapSize / 2f;

        playerMarker.setLocalTranslation(
                pixelX,
                pixelY,
                0.1f
        );
    }

    /**
     * Показать карту.
     */
    public void show() {

        if (isVisible) {
            return;
        }

        System.out.println(
                "[MapWindow] SHOW"
        );

        isVisible = true;

        /*
         * Показываем GUI.
         */
        windowNode.setCullHint(
                Node.CullHint.Dynamic
        );

        /*
         * Включаем viewport.
         *
         * Это выполняется в основном jME-потоке,
         * поскольку show() вызывается из игрового update/input.
         */
        mapRenderer.setEnabled(
                true
        );

        /*
         * Сразу обновляем положение камеры.
         */
        if (playerManager != null) {

            Vector3f playerPos =
                    playerManager.getPosition();

            if (playerPos != null) {

                mapRenderer.update(
                        playerPos
                );
            }
        }

        update();
    }

    /**
     * Скрыть карту.
     */
    public void hide() {

        if (!isVisible) {
            return;
        }

        System.out.println(
                "[MapWindow] HIDE"
        );

        isVisible = false;

        /*
         * Сначала выключаем viewport.
         */
        mapRenderer.setEnabled(
                false
        );

        /*
         * Затем скрываем GUI.
         */
        windowNode.setCullHint(
                Node.CullHint.Always
        );
    }

    /**
     * Переключение карты.
     */
    public void toggle() {

        if (isVisible) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible() {

        return isVisible;
    }

    /**
     * Получить GUI Node карты.
     */
    public Node getWindowNode() {

        return windowNode;
    }

    /**
     * Очистка.
     */
    public void cleanup() {

        System.out.println(
                "[MapWindow] Cleanup"
        );

        if (mapRenderer != null) {

            mapRenderer.setEnabled(
                    false
            );
        }

        if (windowNode != null) {

            app.getGuiNode().detachChild(
                    windowNode
            );

            windowNode = null;
        }

        mapTextureGeometry = null;
        playerMarker = null;
        isVisible = false;
    }
}