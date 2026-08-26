package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;

/**
 * Рендерер карты.
 *
 * ВАЖНО:
 *
 * Карта НЕ использует DungeonNode как отдельный root.
 *
 * Дополнительный viewport подключается к основному rootNode:
 *
 *      app.getRootNode()
 *             |
 *             +-- WorldNode
 *             |
 *             +-- DungeonNode
 *             |
 *             +-- другие игровые объекты
 *
 * Это позволяет jME обновлять scene graph
 * обычным способом.
 */
public class MapRenderer {

    private final SimpleApplication app;

    private Camera mapCam;
    private FrameBuffer frameBuffer;
    private Texture2D renderTexture;
    private ViewPort mapViewPort;

    private boolean initialized = false;

    /**
     * Размер области мира, видимой на карте.
     */
    private float viewSize = 120f;

    private static final int TEXTURE_WIDTH = 1024;
    private static final int TEXTURE_HEIGHT = 1024;

    public MapRenderer(SimpleApplication app) {
        this.app = app;
    }

    /**
     * Инициализация карты.
     *
     * ВАЖНО:
     * DungeonNode сюда больше НЕ передаётся.
     *
     * Карта использует основной rootNode.
     */
    public void initialize() {

        if (initialized) {
            return;
        }

        System.out.println("[MapRenderer] Initializing...");

        /*
         * =========================================================
         * TEXTURE
         * =========================================================
         */

        renderTexture = new Texture2D(
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                Format.RGBA8
        );

        renderTexture.setMinFilter(
                Texture2D.MinFilter.BilinearNoMipMaps
        );

        renderTexture.setMagFilter(
                Texture2D.MagFilter.Bilinear
        );

        /*
         * =========================================================
         * FRAME BUFFER
         * =========================================================
         */

        frameBuffer = new FrameBuffer(
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                1
        );

        frameBuffer.addColorTarget(
                FrameBufferTarget.newTarget(renderTexture)
        );

        frameBuffer.setDepthBuffer(
                Format.Depth
        );

        /*
         * =========================================================
         * CAMERA
         * =========================================================
         */

        mapCam = new Camera(
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );

        mapCam.setParallelProjection(true);

        updateCameraFrustum();

        /*
         * Начальная позиция камеры.
         */
        mapCam.setLocation(
                new Vector3f(
                        0f,
                        50f,
                        0f
                )
        );

        mapCam.lookAt(
                new Vector3f(
                        0f,
                        0f,
                        0f
                ),
                Vector3f.UNIT_Z
        );

        /*
         * =========================================================
         * VIEWPORT
         * =========================================================
         */

        mapViewPort = app.getRenderManager().createPreView(
                "MapViewPort",
                mapCam
        );

        mapViewPort.setClearFlags(
                true,
                true,
                true
        );

        mapViewPort.setBackgroundColor(
                new ColorRGBA(
                        0.05f,
                        0.05f,
                        0.08f,
                        1f
                )
        );

        mapViewPort.setOutputFrameBuffer(
                frameBuffer
        );

        /*
         * =========================================================
         * КРИТИЧЕСКИ ВАЖНО
         * =========================================================
         *
         * НЕ:
         *
         *     attachScene(DungeonNode)
         *
         * НЕ:
         *
         *     attachScene(WorldNode)
         *
         * Используем основной rootNode.
         *
         * DungeonNode остаётся обычным дочерним узлом
         * основного scene graph.
         */
        mapViewPort.attachScene(
                app.getRootNode()
        );

        System.out.println(
                "[MapRenderer] Attached main rootNode"
        );

        /*
         * Карта изначально выключена.
         */
        mapViewPort.setEnabled(false);

        initialized = true;

        System.out.println(
                "[MapRenderer] Initialized successfully"
        );
    }

    /**
     * Обновляет положение камеры карты.
     *
     * ВАЖНО:
     *
     * Здесь НЕЛЬЗЯ вызывать:
     *
     * updateLogicalState()
     * updateGeometricState()
     *
     * Также здесь не изменяется DungeonNode.
     */
    public void update(Vector3f playerPos) {

        if (!initialized) {
            return;
        }

        if (mapCam == null) {
            return;
        }

        if (playerPos == null) {
            return;
        }

        float x = playerPos.x;
        float z = playerPos.z;

        /*
         * Камера находится над игроком.
         */
        mapCam.setLocation(
                new Vector3f(
                        x,
                        50f,
                        z
                )
        );

        /*
         * Смотрим строго вниз.
         */
        mapCam.lookAt(
                new Vector3f(
                        x,
                        0f,
                        z
                ),
                Vector3f.UNIT_Z
        );
    }

    /**
     * Изменяет размер области мира,
     * отображаемой на карте.
     */
    public void setViewSize(float size) {

        if (size <= 0f) {
            return;
        }

        viewSize = size;

        if (mapCam != null) {
            updateCameraFrustum();
        }
    }

    public float getViewSize() {
        return viewSize;
    }

    /**
     * Обновление orthographic frustum.
     */
    private void updateCameraFrustum() {

        if (mapCam == null) {
            return;
        }

        float half = viewSize / 2f;

        mapCam.setFrustum(
                0.1f,
                1000f,
                half,
                -half,
                -half,
                half
        );
    }

    /**
     * Включение / выключение карты.
     */
    public void setEnabled(boolean enabled) {

        if (mapViewPort == null) {
            return;
        }

        mapViewPort.setEnabled(enabled);
    }

    public boolean isEnabled() {

        return mapViewPort != null
                && mapViewPort.isEnabled();
    }

    /**
     * Получить текстуру карты.
     */
    public Texture2D getTexture() {
        return renderTexture;
    }

    /**
     * Получить камеру карты.
     */
    public Camera getCamera() {
        return mapCam;
    }

    /**
     * Получить viewport карты.
     */
    public ViewPort getViewPort() {
        return mapViewPort;
    }

    /**
     * Освобождение ресурсов.
     */
    public void cleanup() {

        System.out.println(
                "[MapRenderer] Cleanup..."
        );

        if (mapViewPort != null) {

            mapViewPort.setEnabled(false);

            app.getRenderManager().removePreView(
                    "MapViewPort"
            );

            mapViewPort = null;
        }

        if (frameBuffer != null) {

            frameBuffer.dispose();

            frameBuffer = null;
        }

        renderTexture = null;
        mapCam = null;

        initialized = false;

        System.out.println(
                "[MapRenderer] Cleanup complete"
        );
    }
}