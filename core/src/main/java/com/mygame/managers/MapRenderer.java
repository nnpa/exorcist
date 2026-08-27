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
 * Рендерер мини-карты.
 *
 * Карта использует ОСНОВНОЙ rootNode.
 * Никаких отдельных DungeonNode для viewport здесь нет.
 */
public class MapRenderer {

    private final SimpleApplication app;

    private Camera mapCam;
    private FrameBuffer frameBuffer;
    private Texture2D renderTexture;
    private ViewPort mapViewPort;

    private boolean initialized = false;

    /**
     * Размер области мира, отображаемой картой.
     */
    private float viewSize = 120f;

    private static final int TEXTURE_WIDTH = 1024;
    private static final int TEXTURE_HEIGHT = 1024;

    public MapRenderer(SimpleApplication app) {
        this.app = app;
    }

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
         * Начальная позиция.
         */
        mapCam.setLocation(
                new Vector3f(
                        0f,
                        100f,
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

        mapViewPort =
                app.getRenderManager().createPreView(
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
         * КРИТИЧЕСКИ ВАЖНО:
         *
         * viewport смотрит на тот же rootNode,
         * который используется основной камерой.
         */
        mapViewPort.attachScene(
                app.getRootNode()
        );

        mapViewPort.setEnabled(false);

        initialized = true;

        System.out.println(
                "[MapRenderer] Initialized successfully"
        );
    }

    /**
     * Обновление позиции камеры карты.
     *
     * НИКАКИХ updateGeometricState здесь нет.
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

        mapCam.setLocation(
                new Vector3f(
                        x,
                        100f,
                        z
                )
        );

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
     * Правильный orthographic frustum.
     *
     * left  = -half
     * right = +half
     * top   = +half
     * bottom= -half
     */
    private void updateCameraFrustum() {

        if (mapCam == null) {
            return;
        }

        float half = viewSize / 2f;

        mapCam.setFrustum(
                0.1f,
                1000f,
                -half,
                half,
                half,
                -half
        );
    }

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

    public void setEnabled(boolean enabled) {

        if (mapViewPort == null) {
            return;
        }

        mapViewPort.setEnabled(enabled);

        System.out.println(
                "[MapRenderer] ViewPort enabled = " + enabled
        );
    }

    public boolean isEnabled() {

        return mapViewPort != null
                && mapViewPort.isEnabled();
    }

    public Texture2D getTexture() {
        return renderTexture;
    }

    public Camera getCamera() {
        return mapCam;
    }

    public ViewPort getViewPort() {
        return mapViewPort;
    }

    /**
     * Важно:
     * вызывать только из jME потока.
     */
    public void refresh(Vector3f playerPos) {

        if (!initialized) {
            return;
        }

        if (playerPos != null) {
            update(playerPos);
        }

        if (mapViewPort != null) {
            mapViewPort.setEnabled(
                    mapViewPort.isEnabled()
            );
        }
    }

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