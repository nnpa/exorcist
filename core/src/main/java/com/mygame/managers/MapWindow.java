package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;

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

    private final float mapSize = 400f;

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

    private void createWindow() {

        windowNode = new Node(
                "MapWindow"
        );

        /*
         * =========================================================
         * ПОЗИЦИЯ
         * =========================================================
         */

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        float x =
                screenWidth
                - mapSize
                - 20f;

        float y =
                screenHeight
                - mapSize
                - 20f;

        /*
         * =========================================================
         * MAP
         * =========================================================
         */

        Quad quad =
                new Quad(
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
         * Это резервный фон,
         * пока framebuffer ещё ничего не нарисовал.
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
         * =========================================================
         * BORDER
         * =========================================================
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

        border.setLocalTranslation(
                -2f,
                -2f,
                -0.01f
        );

        windowNode.attachChild(
                border
        );

        /*
         * =========================================================
         * PLAYER MARKER
         * =========================================================
         */

        Quad markerQuad =
                new Quad(
                        10f,
                        10f
                );

        playerMarker =
                new Geometry(
                        "PlayerMarker",
                        markerQuad
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
         * Центр карты.
         */
        playerMarker.setLocalTranslation(
                mapSize / 2f - 5f,
                mapSize / 2f - 5f,
                0.1f
        );

        windowNode.attachChild(
                playerMarker
        );

        /*
         * =========================================================
         * POSITION
         * =========================================================
         */

        windowNode.setLocalTranslation(
                x,
                y,
                0f
        );

        /*
         * =========================================================
         * HIDE
         * =========================================================
         */

        windowNode.setCullHint(
                Node.CullHint.Always
        );

        app.getGuiNode().attachChild(
                windowNode
        );

        System.out.println(
                "[MapWindow] Created"
        );
    }

    /**
     * Обновляет отображение текстуры карты.
     *
     * Вызывается из игрового jME потока.
     */
    public void update() {

        if (!isVisible) {
            return;
        }

        if (mapRenderer == null) {
            return;
        }

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
         * Игрок всегда в центре,
         * потому что камера карты следует за ним.
         */
        if (playerMarker != null) {

            playerMarker.setLocalTranslation(
                    mapSize / 2f - 5f,
                    mapSize / 2f - 5f,
                    0.1f
            );
        }
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

        windowNode.setCullHint(
                Node.CullHint.Dynamic
        );

        /*
         * Сначала ставим камеру карты
         * в позицию игрока.
         */
        if (playerManager != null) {

            Vector3f pos =
                    playerManager.getPosition();

            if (pos != null) {

                mapRenderer.update(pos);
            }
        }

        /*
         * Только после этого включаем viewport.
         */
        mapRenderer.setEnabled(true);

        /*
         * Привязываем framebuffer texture
         * к GUI.
         */
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

        mapRenderer.setEnabled(false);

        windowNode.setCullHint(
                Node.CullHint.Always
        );
    }

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

    public Node getWindowNode() {
        return windowNode;
    }

    /**
     * Метод, который можно вызывать после загрузки данжа.
     */
    public void refresh() {

        if (!isVisible) {
            return;
        }

        if (playerManager == null) {
            return;
        }

        Vector3f pos =
                playerManager.getPosition();

        if (pos != null) {

            mapRenderer.update(pos);
        }

        update();
    }

    public void cleanup() {

        System.out.println(
                "[MapWindow] Cleanup"
        );

        if (mapRenderer != null) {
            mapRenderer.setEnabled(false);
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