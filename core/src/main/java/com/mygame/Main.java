package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;
import com.mygame.managers.*;
import com.mygame.managers.GameManager.GameState;
import com.mygame.monsters.Monster;
import com.simsilica.lemur.Label;

import java.util.Map;
import java.awt.*;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.CollisionResult;
import com.jme3.math.Ray;
import com.jme3.renderer.RenderManager;
import com.jme3.texture.Texture2D;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.Container;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.util.List;

public class Main extends SimpleApplication {

    // ===== ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ОТЛАДКИ =====

    private String getPath(Spatial s) {
        StringBuilder sb = new StringBuilder();

        while (s != null) {
            sb.insert(0, "/" + s.getName());
            s = s.getParent();
        }

        return sb.toString();
    }

    private static Main instance;

    private GameManager gameManager;
    private NetworkManager networkManager;
    private UIManager uiManager;
    private WorldManager worldManager;
    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private DropManager dropManager;

    private boolean isInitialized = false;
    private boolean worldLoaded = false;

    private BulletAppState bulletAppState;

    // ===== КАМЕРА =====

    private float camDistance = 12f;
    private float camHeight = 8f;
    private float cameraAngle = 0f;
    private boolean isRotatingCamera = false;
    private float lastMouseX = 0;
    private float mouseSensitivity = 0.005f;
    private CameraFollowControl cameraControl;

    // ===== ВИДЕО-ЗАСТАВКА (JAVA FX) =====

    private boolean introSkipped = false;
    private Stage introStage;
    private MediaPlayer introPlayer;

    // ============================================================
    //                    ИКОНКИ
    // ============================================================

    private static BufferedImage[] applicationIcons;

    /**
     * Флаг.
     *
     * После restart() jMonkeyEngine сначала уничтожает старое
     * GLFW-окно, затем создаёт новое.
     *
     * Иконку необходимо устанавливать уже ПОСЛЕ создания нового окна.
     */
    private volatile boolean applyIconsAfterRestart = false;

    /**
     * Загружает иконки из src/main/resources.
     *
     * Файлы должны находиться:
     *
     * src/main/resources/icon16.png
     * src/main/resources/icon32.png
     * src/main/resources/icon64.png
     * src/main/resources/icon128.png
     * src/main/resources/icon256.png
     */
    private static BufferedImage[] loadApplicationIcons()
            throws IOException {

        List<BufferedImage> icons = new ArrayList<>();

        String[] paths = {
                "/icon16.png",
                "/icon32.png",
                "/icon64.png",
                "/icon128.png",
                "/icon256.png"
        };

        for (String path : paths) {

            try (InputStream input =
                         Main.class.getResourceAsStream(path)) {

                if (input == null) {

                    // 256x256 необязательная.
                    if ("/icon256.png".equals(path)) {
                        System.out.println(
                                "[Main] /icon256.png не найден. Пропускаем."
                        );
                        continue;
                    }

                    throw new IOException(
                            "Не найден ресурс: " + path
                    );
                }

                BufferedImage image =
                        ImageIO.read(input);

                if (image == null) {

                    throw new IOException(
                            "ImageIO не смог прочитать: " + path
                    );
                }

                System.out.println(
                        "[Main] Загружена иконка "
                        + path
                        + " -> "
                        + image.getWidth()
                        + "x"
                        + image.getHeight()
                        + ", type="
                        + image.getType()
                );

                icons.add(image);
            }
        }

        if (icons.isEmpty()) {

            throw new IOException(
                    "Не загружено ни одной иконки."
            );
        }

        return icons.toArray(
                new BufferedImage[0]
        );
    }

    /**
     * Устанавливает иконки в AppSettings.
     *
     * Это необходимо сделать ДО создания GLFW-окна.
     */
    private static void applyIconsToSettings(
            AppSettings settings) {

        if (settings == null) {
            return;
        }

        if (applicationIcons == null ||
            applicationIcons.length == 0) {

            System.err.println(
                    "[Main] НЕТ ИКОНОК В applicationIcons!"
            );

            return;
        }

        settings.setIcons(
                applicationIcons
        );

        System.out.println(
                "[Main] AppSettings.setIcons() выполнен. "
                + "Количество иконок: "
                + applicationIcons.length
        );
    }

    /**
     * Повторно устанавливает иконку непосредственно в уже
     * созданное LWJGL3/GLFW окно.
     *
     * В jMonkeyEngine 3.8.1-stable метод setWindowIcon()
     * находится в LwjglWindow и является protected.
     *
     * Мы НЕ импортируем LwjglWindow напрямую.
     *
     * Вместо этого получаем реальный класс контекста:
     *
     * getContext().getClass()
     *
     * и ищем setWindowIcon() во всей цепочке наследования.
     *
     * Это позволяет работать даже если LwjglWindow находится
     * только в runtime dependency.
     */
    private void applyIconsDirectlyToWindow() {

        if (applicationIcons == null ||
            applicationIcons.length == 0) {

            System.err.println(
                    "[Main] Невозможно установить иконки: "
                    + "applicationIcons пуст."
            );

            return;
        }

        try {

            AppSettings settings =
                    getContext().getSettings();

            // Сначала гарантируем, что текущие settings
            // содержат наши иконки.
            settings.setIcons(
                    applicationIcons
            );

            Object context =
                    getContext();

            if (context == null) {

                System.err.println(
                        "[Main] getContext() == null"
                );

                return;
            }

            Class<?> currentClass =
                    context.getClass();

            Method setWindowIconMethod = null;

            /*
             * Ищем метод во всей цепочке:
             *
             * LwjglDisplay
             *    ↓
             * LwjglWindow
             *    ↓
             * LwjglContext
             */
            while (currentClass != null) {

                try {

                    setWindowIconMethod =
                            currentClass.getDeclaredMethod(
                                    "setWindowIcon",
                                    AppSettings.class
                            );

                    break;

                } catch (NoSuchMethodException e) {

                    currentClass =
                            currentClass.getSuperclass();
                }
            }

            if (setWindowIconMethod == null) {

                System.err.println(
                        "[Main] Метод setWindowIcon(AppSettings) "
                        + "не найден."
                );

                System.err.println(
                        "[Main] Класс context: "
                        + context.getClass().getName()
                );

                return;
            }

            setWindowIconMethod.setAccessible(
                    true
            );

            setWindowIconMethod.invoke(
                    context,
                    settings
            );

            System.out.println(
                    "[Main] ====================================="
            );

            System.out.println(
                    "[Main] ИКОНКИ ПРИНУДИТЕЛЬНО УСТАНОВЛЕНЫ "
                    + "В GLFW ОКНО."
            );

            System.out.println(
                    "[Main] Context: "
                    + context.getClass().getName()
            );

            System.out.println(
                    "[Main] Метод: "
                    + setWindowIconMethod
            );

            System.out.println(
                    "[Main] ====================================="
            );

        } catch (InvocationTargetException e) {

            System.err.println(
                    "[Main] Ошибка внутри setWindowIcon()."
            );

            Throwable cause =
                    e.getCause();

            if (cause != null) {
                cause.printStackTrace();
            } else {
                e.printStackTrace();
            }

        } catch (Exception e) {

            System.err.println(
                    "[Main] Не удалось применить иконки "
                    + "непосредственно к GLFW."
            );

            e.printStackTrace();
        }
    }

    /**
     * Устанавливаем иконки после restart().
     *
     * restart() в jMonkeyEngine только ставит флаг needRestart.
     * Реальный destroy/create нового GLFW окна происходит
     * в следующем цикле LWJGL.
     *
     * Поэтому здесь нельзя вызывать setWindowIcon()
     * сразу после restart().
     *
     * simpleUpdate() вызывается после того, как restartContext()
     * уже был выполнен.
     */
    private void applyIconsAfterRestartIfNeeded() {

        if (!applyIconsAfterRestart) {
            return;
        }

        applyIconsAfterRestart = false;

        System.out.println(
                "[Main] Новый GLFW-контекст создан."
        );

        System.out.println(
                "[Main] Применяем иконки ПОСЛЕ restart..."
        );

        applyIconsDirectlyToWindow();
    }

    // ===== ТОЧКА ВХОДА =====

    public static void main(String[] args) {

        AppSettings settings =
                new AppSettings(true);

        settings.setTitle(
                "Exorcist"
        );

        settings.setWindowSize(
                800,
                600
        );

        settings.setResizable(
                true
        );

        settings.setFullscreen(
                false
        );

        settings.setVSync(
                false
        );

        settings.setSamples(
                4
        );

        settings.setUseInput(
                true
        );

        settings.setRenderer(
                "LWJGL3"
        );

        // ========================================================
        //                    ЗАГРУЗКА ИКОНОК
        // ========================================================

        try {

            applicationIcons =
                    loadApplicationIcons();

            applyIconsToSettings(
                    settings
            );

            System.out.println(
                    "[Main] ====================================="
            );

            System.out.println(
                    "[Main] ИКОНКИ УСПЕШНО ЗАГРУЖЕНЫ"
            );

            for (BufferedImage icon :
                    applicationIcons) {

                System.out.println(
                        "[Main] "
                        + icon.getWidth()
                        + "x"
                        + icon.getHeight()
                );
            }

            System.out.println(
                    "[Main] ====================================="
            );

        } catch (Exception e) {

            System.err.println(
                    "[Main] ОШИБКА ЗАГРУЗКИ ИКОНОК!"
            );

            e.printStackTrace();
        }

        Main app =
                new Main();

        app.setSettings(
                settings
        );

        app.setShowSettings(
                false
        );

        app.start();
    }

    // ============================================================
    //                 ИЗМЕНЕНИЕ РАЗРЕШЕНИЯ
    // ============================================================

    public void setResolution(
            int width,
            int height,
            boolean fullscreen) {

        AppSettings settings =
                getContext().getSettings();

        settings.setResolution(
                width,
                height
        );

        settings.setWidth(
                width
        );

        settings.setHeight(
                height
        );

        settings.setFullscreen(
                fullscreen
        );

        if (fullscreen) {

            settings.setResizable(
                    false
            );

        } else {

            settings.setResizable(
                    true
            );
        }

        // ========================================================
        // ИКОНКИ
        //
        // Перед restart обязательно помещаем иконки
        // в AppSettings.
        // ========================================================

        applyIconsToSettings(
                settings
        );

        setSettings(
                settings
        );

        // Говорим simpleUpdate(), что после создания
        // нового GLFW окна нужно применить иконку напрямую.
        applyIconsAfterRestart = true;

        System.out.println(
                "[Main] ====================================="
        );

        System.out.println(
                "[Main] Перезапускаем окно:"
        );

        System.out.println(
                "[Main] "
                + width
                + "x"
                + height
                + " fullscreen="
                + fullscreen
        );

        System.out.println(
                "[Main] Иконки будут применены "
                + "после создания нового GLFW окна."
        );

        System.out.println(
                "[Main] ====================================="
        );

        restart();
    }

    @Override
    public void simpleInitApp() {

        setDisplayFps(
                false
        );

        setDisplayStatView(
                false
        );

        Dimension screenSize =
                Toolkit
                        .getDefaultToolkit()
                        .getScreenSize();

        int width =
                screenSize.width;

        int height =
                screenSize.height;

        setResolution(
                width,
                height,
                false
        );

        instance = this;

        SoundManager.initialize(
                this
        );

        viewPort.setBackgroundColor(
                new ColorRGBA(
                        0.2f,
                        0.2f,
                        0.2f,
                        1f
                )
        );

        // ===== ФИЗИКА =====

        bulletAppState =
                new BulletAppState();

        bulletAppState.setEnabled(
                false
        );

        bulletAppState.setThreadingType(
                BulletAppState.ThreadingType.SEQUENTIAL
        );

        stateManager.attach(
                bulletAppState
        );

        setupLighting();

        GuiGlobals.initialize(
                this
        );

        applySkinStyle();
        applyTextFieldStyle();

        GuiGlobals.getInstance()
                .getStyles()
                .setDefaultStyle(
                        "glass"
                );

        flyCam.setEnabled(
                false
        );

        inputManager.setCursorVisible(
                true
        );

        // Инициализация менеджеров
        initializeManagers();

        if (playerManager != null &&
            playerManager.getPlayerNode() != null) {

            cameraControl =
                    new CameraFollowControl(
                            cam,
                            playerManager.getPlayerNode()
                    );

            playerManager
                    .getPlayerNode()
                    .addControl(
                            cameraControl
                    );
        }

        if (playerManager != null) {

            playerManager.setPhysicsSpace(
                    bulletAppState
                            .getPhysicsSpace()
            );
        }

        if (worldManager != null) {

            worldManager.setBulletAppState(
                    bulletAppState
            );
        }

        if (uiManager != null &&
            playerManager != null) {

            uiManager.setPlayerManager(
                    playerManager
            );
        }

        if (uiManager != null &&
            inventoryManager != null) {

            uiManager.setInventoryManager(
                    inventoryManager
            );
        }

        if (dropManager != null &&
            inventoryManager != null) {

            dropManager.setInventoryManager(
                    inventoryManager
            );
        }

        if (playerManager != null) {

            playerManager.setWorldManager(
                    worldManager
            );

            playerManager.setDropManager(
                    dropManager
            );
        }

        // ===== УПРАВЛЕНИЕ =====

        inputManager.addMapping(
                "MouseClick",
                new MouseButtonTrigger(
                        MouseInput.BUTTON_LEFT
                )
        );

        inputManager.addListener(
                new ActionListener() {

                    @Override
                    public void onAction(
                            String name,
                            boolean isPressed,
                            float tpf) {

                        if (isPressed &&
                            "MouseClick".equals(name) &&
                            playerManager != null) {

                            if (uiManager != null &&
                                uiManager.isAnyWindowOpen()) {

                                return;
                            }

                            Vector2f cursor =
                                    inputManager
                                            .getCursorPosition();

                            handleClick(
                                    cursor.x,
                                    cursor.y
                            );
                        }
                    }
                },
                "MouseClick"
        );

        inputManager.addMapping(
                "RightClick",
                new MouseButtonTrigger(
                        MouseInput.BUTTON_RIGHT
                )
        );

        inputManager.addListener(
                new ActionListener() {

                    @Override
                    public void onAction(
                            String name,
                            boolean isPressed,
                            float tpf) {

                        if ("RightClick".equals(name)) {

                            isRotatingCamera =
                                    isPressed;

                            if (cameraControl != null) {

                                cameraControl.setEnabled(
                                        !isPressed
                                );
                            }

                            if (isPressed) {

                                lastMouseX =
                                        inputManager
                                                .getCursorPosition()
                                                .x;
                            }
                        }
                    }
                },
                "RightClick"
        );

        inputManager.addRawInputListener(
                new RawInputListener() {

                    @Override
                    public void beginInput() {
                    }

                    @Override
                    public void endInput() {
                    }

                    @Override
                    public void onJoyAxisEvent(
                            JoyAxisEvent evt) {
                    }

                    @Override
                    public void onJoyButtonEvent(
                            JoyButtonEvent evt) {
                    }

                    @Override
                    public void onMouseMotionEvent(
                            MouseMotionEvent evt) {

                        if (!worldLoaded ||
                            !isRotatingCamera) {

                            return;
                        }

                        float currentX =
                                inputManager
                                        .getCursorPosition()
                                        .x;

                        float deltaX =
                                currentX - lastMouseX;

                        cameraAngle +=
                                deltaX
                                * mouseSensitivity;

                        lastMouseX =
                                currentX;
                    }

                    @Override
                    public void onMouseButtonEvent(
                            MouseButtonEvent evt) {
                    }

                    @Override
                    public void onKeyEvent(
                            KeyInputEvent evt) {
                    }

                    @Override
                    public void onTouchEvent(
                            TouchEvent evt) {
                    }
                }
        );

        inputManager.addMapping(
                "ReturnToCity",
                new KeyTrigger(
                        com.jme3.input.KeyInput.KEY_N
                )
        );

        inputManager.addListener(
                new ActionListener() {

                    @Override
                    public void onAction(
                            String name,
                            boolean isPressed,
                            float tpf) {

                        if (isPressed &&
                            "ReturnToCity".equals(name) &&
                            worldLoaded) {

                            if (gameManager != null &&
                                gameManager.getCurrentState()
                                        == GameState.DUNGEON) {

                                worldManager.returnToCity();

                                gameManager.setState(
                                        GameState.CITY
                                );
                            }
                        }
                    }
                },
                "ReturnToCity"
        );

        Monster.setApp(
                this
        );

        playerManager.setUIManager(
                uiManager
        );

        playerManager.setNetworkManager(
                networkManager
        );

        // ===== ПОКАЗЫВАЕМ ВИДЕО-ЗАСТАВКУ =====

        showIntroVideo();

        // ===== ГЛОБАЛЬНЫЙ ОБРАБОТЧИК КЛИКОВ =====

        inputManager.addRawInputListener(
                new RawInputListener() {

                    @Override
                    public void beginInput() {
                    }

                    @Override
                    public void endInput() {
                    }

                    @Override
                    public void onJoyAxisEvent(
                            JoyAxisEvent evt) {
                    }

                    @Override
                    public void onJoyButtonEvent(
                            JoyButtonEvent evt) {
                    }

                    @Override
                    public void onMouseMotionEvent(
                            MouseMotionEvent evt) {
                    }

                    @Override
                    public void onMouseButtonEvent(
                            MouseButtonEvent evt) {

                        if (evt.getButtonIndex() == 0 &&
                            evt.isPressed()) {

                            Vector2f click2d =
                                    new Vector2f(
                                            evt.getX(),
                                            evt.getY()
                                    );

                            Vector3f click3d =
                                    cam.getWorldCoordinates(
                                            new Vector2f(
                                                    click2d.x,
                                                    click2d.y
                                            ),
                                            0f
                                    );

                            Vector3f dir =
                                    cam.getWorldCoordinates(
                                            new Vector2f(
                                                    click2d.x,
                                                    click2d.y
                                            ),
                                            1f
                                    )
                                    .subtract(click3d)
                                    .normalizeLocal();

                            Ray ray =
                                    new Ray(
                                            click3d,
                                            dir
                                    );

                            CollisionResults results =
                                    new CollisionResults();

                            guiNode.collideWith(
                                    ray,
                                    results
                            );

                            System.out.println(
                                    "=== GLOBAL CLICK DEBUG ==="
                            );

                            System.out.println(
                                    "Click coordinates: (" +
                                    evt.getX() +
                                    ", " +
                                    evt.getY() +
                                    ")"
                            );

                            if (results.size() > 0) {

                                for (CollisionResult res :
                                        results) {

                                    Spatial target =
                                            res.getGeometry();

                                    System.out.println(
                                            "  Hit spatial: "
                                            + target
                                    );

                                    System.out.println(
                                            "    Name: "
                                            + target.getName()
                                    );

                                    System.out.println(
                                            "    Class: "
                                            + target
                                                    .getClass()
                                                    .getSimpleName()
                                    );

                                    Spatial parent =
                                            target;

                                    while (parent != null) {

                                        if (parent
                                                instanceof Button) {

                                            System.out.println(
                                                    "    -> This is a Button! Text: "
                                                    + ((Button) parent)
                                                    .getText()
                                            );

                                            break;

                                        } else if (
                                                parent
                                                        instanceof Panel) {

                                            System.out.println(
                                                    "    -> This is a Panel"
                                            );

                                            break;

                                        } else if (
                                                parent
                                                        instanceof Container) {

                                            System.out.println(
                                                    "    -> This is a Container"
                                            );

                                            break;

                                        } else if (
                                                parent
                                                        instanceof Label) {

                                            System.out.println(
                                                    "    -> This is a Label"
                                            );

                                            break;
                                        }

                                        parent =
                                                parent.getParent();
                                    }

                                    System.out.println(
                                            "    Full path: "
                                            + getPath(target)
                                    );
                                }

                            } else {

                                System.out.println(
                                        "  No GUI elements under cursor."
                                );

                                CollisionResults worldResults =
                                        new CollisionResults();

                                rootNode.collideWith(
                                        ray,
                                        worldResults
                                );

                                if (worldResults.size() > 0) {

                                    System.out.println(
                                            "  But hit a 3D object: "
                                            + worldResults
                                                    .getClosestCollision()
                                                    .getGeometry()
                                    );
                                }
                            }

                            System.out.println(
                                    "============================="
                            );
                        }
                    }

                    @Override
                    public void onKeyEvent(
                            KeyInputEvent evt) {
                    }

                    @Override
                    public void onTouchEvent(
                            TouchEvent evt) {
                    }
                }
        );
    }

    // ============================================================
    //   ВИДЕО-ЗАСТАВКА (JAVA FX)
    // ============================================================

    private void showIntroVideo() {

        Platform.startup(() -> {
        });

        Platform.runLater(() -> {

            try {

                introStage =
                        new Stage();

                introStage.initStyle(
                        StageStyle.UNDECORATED
                );

                introStage.setFullScreen(
                        true
                );

                introStage.setAlwaysOnTop(
                        true
                );

                String path =
                        getClass()
                                .getResource(
                                        "/video/video2.mp4"
                                )
                                .toExternalForm();

                Media media =
                        new Media(path);

                introPlayer =
                        new MediaPlayer(
                                media
                        );

                introPlayer.setAutoPlay(
                        true
                );

                MediaView view =
                        new MediaView(
                                introPlayer
                        );

                view.setPreserveRatio(
                        true
                );

                view.setFitWidth(
                        Toolkit
                                .getDefaultToolkit()
                                .getScreenSize()
                                .getWidth()
                );

                view.setFitHeight(
                        Toolkit
                                .getDefaultToolkit()
                                .getScreenSize()
                                .getHeight()
                );

                StackPane root =
                        new StackPane(view);

                root.setStyle(
                        "-fx-background-color: black;"
                );

                Scene scene =
                        new Scene(root);

                scene.setOnKeyPressed(
                        e -> skipIntro()
                );

                scene.setOnMouseClicked(
                        e -> skipIntro()
                );

                introPlayer.setOnEndOfMedia(
                        this::skipIntro
                );

                introStage.setScene(
                        scene
                );

                introStage.show();

            } catch (Exception e) {

                e.printStackTrace();

                finishIntro();
            }
        });
    }

    private void skipIntro() {

        if (introSkipped) {
            return;
        }

        introSkipped = true;

        Platform.runLater(() -> {

            if (introPlayer != null) {

                introPlayer.stop();
                introPlayer.dispose();

                introPlayer = null;
            }

            if (introStage != null) {

                introStage.hide();
                introStage.close();

                introStage = null;
            }

            finishIntro();
        });
    }

    private void finishIntro() {

        uiManager.forceShowLogin();
    }

    // ============================================================
    //   ОБНОВЛЕНИЕ КАМЕРЫ
    // ============================================================

    private void updateCamera() {

        if (!worldLoaded ||
            gameManager == null) {

            return;
        }

        GameState state =
                gameManager.getCurrentState();

        if (state != GameState.CITY &&
            state != GameState.DUNGEON) {

            return;
        }

        if (playerManager == null) {
            return;
        }

        Node playerNode =
                playerManager.getPlayerNode();

        if (playerNode == null) {
            return;
        }

        Vector3f playerPos =
                playerNode.getWorldTranslation();

        if (playerPos == null) {
            return;
        }

        if (cameraControl != null &&
            cameraControl.isEnabled()) {

            return;
        }

        float distance = 18f;
        float height = 22f;

        float x =
                FastMath.sin(cameraAngle)
                * distance;

        float z =
                FastMath.cos(cameraAngle)
                * distance;

        Vector3f camPos =
                playerPos.add(
                        new Vector3f(
                                x,
                                height,
                                z
                        )
                );

        Vector3f currentCamPos =
                cam.getLocation();

        camPos.interpolateLocal(
                currentCamPos,
                0.15f
        );

        cam.setLocation(
                camPos
        );

        Vector3f lookTarget =
                playerPos.add(
                        new Vector3f(
                                0,
                                -0.5f,
                                0
                        )
                );

        cam.lookAt(
                lookTarget,
                Vector3f.UNIT_Y
        );
    }

    @Override
    public void simpleRender(
            RenderManager rm) {

        super.simpleRender(
                rm
        );

        updateCamera();
    }

    // ============================================================
    //   ЗАГРУЗКА ТАЛАНТОВ И ИГРОВОГО МИРА
    // ============================================================

    public void loadTalentsFromServer() {

        if (networkManager == null ||
            uiManager == null) {

            return;
        }

        networkManager
                .loadTalents()
                .thenAccept(data -> {

                    if (data != null) {

                        Map<String, Integer> talents =
                                (Map<String, Integer>)
                                        data.get("talents");

                        int points =
                                (int)
                                        data.get(
                                                "availablePoints"
                                        );

                        this.enqueue(() -> {

                            TalentManager tm =
                                    uiManager
                                            .getTalentManager();

                            TalentWindow tw =
                                    uiManager
                                            .getTalentWindow();

                            if (tm != null) {

                                tm.loadFromServer(
                                        talents,
                                        points
                                );

                                if (tw != null &&
                                    tw.isVisible()) {

                                    tw.updateUI();
                                }
                            }

                            return null;
                        });
                    }

                })
                .exceptionally(ex -> {

                    System.err.println(
                            "[Main] Failed to load talents: "
                            + ex.getMessage()
                    );

                    return null;
                });
    }

    public void loadGameWorld() {

        if (worldLoaded) {
            return;
        }

        System.out.println(
                "[Main] ===== ЗАГРУЗКА МИРА ====="
        );

        bulletAppState.setEnabled(
                true
        );

        PhysicsSpace space =
                bulletAppState
                        .getPhysicsSpace();

        space.setAccuracy(
                0.001f
        );

        space.setMaxSubSteps(
                10
        );

        space.setGravity(
                new Vector3f(
                        0,
                        -30f,
                        0
                )
        );

        bulletAppState.setSpeed(
                0.8f
        );

        bulletAppState.setThreadingType(
                BulletAppState.ThreadingType.SEQUENTIAL
        );

        System.out.println(
                "[Main] Физика включена и настроена"
        );

        worldManager.loadCityWithPhysics();

        if (playerManager != null) {

            Node playerNode =
                    playerManager
                            .getPlayerNode();

            if (playerNode != null &&
                !rootNode.hasChild(
                        playerNode
                )) {

                rootNode.attachChild(
                        playerNode
                );

                System.out.println(
                        "[Main] Персонаж добавлен в rootNode"
                );
            }

            Vector3f spawnPos =
                    new Vector3f(
                            0f,
                            0.5f,
                            -8f
                    );

            playerManager.setPosition(
                    spawnPos
            );

            if (playerManager
                    .getCharacterControl() != null) {

                playerManager
                        .getCharacterControl()
                        .warp(spawnPos);

                playerManager
                        .getCharacterControl()
                        .setWalkDirection(
                                Vector3f.ZERO
                        );
            }

            System.out.println(
                    "[Main] Персонаж на позиции: "
                    + spawnPos
            );
        }

        gameManager.setState(
                GameState.CITY
        );

        worldManager.switchToCity();

        worldLoaded = true;

        System.out.println(
                "[Main] ===== МИР ЗАГРУЖЕН ====="
        );

        loadTalentsFromServer();
    }

    // ============================================================
    //   ОБРАБОТЧИК КЛИКОВ
    // ============================================================

    private void handleClick(
            float screenX,
            float screenY) {

        if (!worldLoaded ||
            playerManager == null) {

            return;
        }

        DropManager.DropItem drop =
                dropManager.getDropAt(
                        screenX,
                        screenY
                );

        if (drop != null) {

            dropManager.pickupDrop(
                    drop
            );

            return;
        }

        Vector3f groundPoint =
                getGroundPoint(
                        screenX,
                        screenY
                );

        if (groundPoint == null) {
            return;
        }

        // Телепортер

        Spatial teleporter = null;

        if (worldManager
                .getCityNode() != null) {

            for (Spatial child :
                    worldManager
                            .getCityNode()
                            .getChildren()) {

                if (child.getName() != null &&
                    child.getName()
                            .equals(
                                    "Teleporter"
                            )) {

                    float dist =
                            child.getWorldTranslation()
                                    .distance(
                                            groundPoint
                                    );

                    if (dist < 1.5f) {

                        teleporter =
                                child;

                        break;
                    }
                }
            }
        }

        if (teleporter != null) {

            if (uiManager != null) {

                uiManager
                        .showTeleporterDialog();
            }

            return;
        }

        // Торговец

        Spatial npc = null;

        if (worldManager
                .getNpcNode() != null) {

            for (Spatial child :
                    worldManager
                            .getNpcNode()
                            .getChildren()) {

                if (child.getName() != null &&
                    child.getName()
                            .equals(
                                    "NPC_Trader"
                            )) {

                    float dist =
                            child.getWorldTranslation()
                                    .distance(
                                            groundPoint
                                    );

                    if (dist < 1.5f) {

                        npc =
                                child;

                        break;
                    }
                }
            }
        }

        if (npc != null) {

            if (uiManager != null) {

                uiManager.openTrader();
            }

            return;
        }

        // Аукционер

        Spatial auctioneer = null;

        if (worldManager
                .getNpcNode() != null) {

            for (Spatial child :
                    worldManager
                            .getNpcNode()
                            .getChildren()) {

                if (child.getName() != null &&
                    child.getName()
                            .equals(
                                    "NPC_Auctioneer"
                            )) {

                    float dist =
                            child.getWorldTranslation()
                                    .distance(
                                            groundPoint
                                    );

                    if (dist < 1.5f) {

                        auctioneer =
                                child;

                        break;
                    }
                }
            }
        }

        if (auctioneer != null) {

            if (uiManager != null) {

                uiManager.openAuction();
            }

            return;
        }

        // Монстры

        Spatial clicked =
                worldManager
                        .getClosestInteractiveObject(
                                groundPoint,
                                5.5f
                        );

        if (clicked != null) {

            String objectName =
                    clicked.getName();

            if ("TestMonster"
                    .equals(objectName) ||
                "Monster"
                        .equals(objectName)) {

                playerManager
                        .attackTarget(
                                clicked
                        );

                return;
            }
        }

        // Движение

        playerManager.moveTo(
                groundPoint
        );
    }

    private Vector3f getGroundPoint(
            float screenX,
            float screenY) {

        Vector3f origin =
                cam.getWorldCoordinates(
                        new Vector2f(
                                screenX,
                                screenY
                        ),
                        0f
                );

        Vector3f direction =
                cam.getWorldCoordinates(
                        new Vector2f(
                                screenX,
                                screenY
                        ),
                        1f
                )
                .subtract(origin)
                .normalizeLocal();

        if (direction.y == 0) {
            return null;
        }

        float t =
                -origin.y
                / direction.y;

        if (t < 0) {
            return null;
        }

        Vector3f hitPoint =
                origin.add(
                        direction.mult(t)
                );

        hitPoint.y = 0;

        return hitPoint;
    }

    // ============================================================
    //   ОСВЕЩЕНИЕ, GUI, МЕНЕДЖЕРЫ
    // ============================================================

    private void setupLighting() {

        DirectionalLight sun =
                new DirectionalLight();

        sun.setDirection(
                new Vector3f(
                        -1,
                        -2,
                        -1
                ).normalizeLocal()
        );

        sun.setColor(
                ColorRGBA.White.mult(
                        1.2f
                )
        );

        rootNode.addLight(
                sun
        );

        DirectionalLight fillLight =
                new DirectionalLight();

        fillLight.setDirection(
                new Vector3f(
                        1,
                        -1,
                        1
                ).normalizeLocal()
        );

        fillLight.setColor(
                new ColorRGBA(
                        0.6f,
                        0.6f,
                        0.7f,
                        1f
                ).mult(
                        0.8f
                )
        );

        rootNode.addLight(
                fillLight
        );

        AmbientLight ambient =
                new AmbientLight();

        ambient.setColor(
                new ColorRGBA(
                        0.6f,
                        0.6f,
                        0.6f,
                        1.0f
                )
        );

        rootNode.addLight(
                ambient
        );
    }

    private void applyTextFieldStyle() {

        Styles styles =
                GuiGlobals
                        .getInstance()
                        .getStyles();

        Attributes attrs =
                styles.getSelector(
                        TextField.ELEMENT_ID,
                        null
                );

        attrs.set(
                "background",
                new QuadBackgroundComponent(
                        new ColorRGBA(
                                0.25f,
                                0.25f,
                                0.35f,
                                0.9f
                        )
                ));

        attrs.set(
                "color",
                ColorRGBA.Black
        );

        attrs.set(
                "fontSize",
                18f
        );

        attrs.set(
                "insets",
                new Insets3f(
                        2,
                        6,
                        2,
                        6
                )
        );
    }

    private void applySkinStyle() {

        Styles styles =
                GuiGlobals
                        .getInstance()
                        .getStyles();

        Attributes labelAttrs =
                styles.getSelector(
                        Label.ELEMENT_ID,
                        null
                );

        labelAttrs.set(
                "fontSize",
                18f
        );

        labelAttrs.set(
                "color",
                new ColorRGBA(
                        0.9f,
                        0.7f,
                        0.4f,
                        1f
                )
        );

        Attributes textFieldAttrs =
                styles.getSelector(
                        TextField.ELEMENT_ID,
                        null
                );

        textFieldAttrs.set(
                "background",
                new QuadBackgroundComponent(
                        new ColorRGBA(
                                0.2f,
                                0.1f,
                                0.03f,
                                0.9f
                        )
                )
        );

        textFieldAttrs.set(
                "color",
                ColorRGBA.White
        );

        textFieldAttrs.set(
                "fontSize",
                18f
        );
    }

    private void initializeManagers() {

        if (isInitialized) {
            return;
        }

        networkManager =
                new NetworkManager(this);

        networkManager.initialize();

        gameManager =
                new GameManager(this);

        gameManager.initialize();

        playerManager =
                new PlayerManager(this);

        playerManager.initialize();

        worldManager =
                new WorldManager(this);

        worldManager.initialize();

        uiManager =
                new UIManager(this);

        uiManager.initialize();

        inventoryManager =
                new InventoryManager(
                        this,
                        guiNode
                );

        dropManager =
                new DropManager(
                        this,
                        guiNode
                );

        uiManager.setPlayerManager(
                playerManager
        );

        uiManager.setInventoryManager(
                inventoryManager
        );

        TalentManager talentManager =
                new TalentManager(
                        playerManager,
                        networkManager
                );

        uiManager.setTalentManager(
                talentManager
        );

        playerManager.setTalentManager(
                talentManager
        );

        worldManager.setNetworkManager(
                networkManager
        );

        worldManager.setPlayerManager(
                playerManager
        );

        worldManager.setDropManager(
                dropManager
        );

        playerManager.setWorldManager(
                worldManager
        );

        playerManager.setDropManager(
                dropManager
        );

        dropManager.setInventoryManager(
                inventoryManager
        );

        gameManager.setNetworkManager(
                networkManager
        );

        gameManager.setPlayerManager(
                playerManager
        );

        gameManager.setWorldManager(
                worldManager
        );

        gameManager.setUIManager(
                uiManager
        );

        isInitialized = true;
    }

    // ============================================================
    //   ЦИКЛ ОБНОВЛЕНИЯ И ОЧИСТКА РЕСУРСОВ
    // ============================================================

    @Override
    public void simpleUpdate(
            float tpf) {

        /*
         * ВАЖНО:
         *
         * Если restart() был вызван, LWJGL3 сначала выполняет
         * restartContext():
         *
         * destroyContext()
         *       ↓
         * createContext()
         *       ↓
         * создаётся новое GLFW окно
         *
         * И только потом вызывается listener.update(),
         * где выполняется simpleUpdate().
         *
         * Поэтому здесь иконка уже применяется к НОВОМУ окну.
         */
        applyIconsAfterRestartIfNeeded();

        super.simpleUpdate(
                tpf
        );

        if (gameManager != null) {
            gameManager.update(tpf);
        }

        if (playerManager != null) {
            playerManager.update(tpf);
        }

        if (worldManager != null) {
            worldManager.update(tpf);
        }

        if (uiManager != null) {
            uiManager.update(tpf);
        }
    }

    /**
     * Правильная очистка ресурсов JavaFX MediaPlayer.
     */
    @Override
    public void destroy() {

        if (introPlayer != null) {

            Platform.runLater(() -> {

                try {

                    introPlayer.stop();
                    introPlayer.dispose();

                    introPlayer = null;

                    System.gc();

                } catch (Exception e) {

                    e.printStackTrace();
                }
            });
        }

        if (introStage != null) {

            Platform.runLater(() -> {

                try {

                    introStage.hide();
                    introStage.close();

                    introStage = null;

                } catch (Exception e) {

                    e.printStackTrace();
                }
            });
        }

        SoundManager.cleanup();

        super.destroy();
    }

    @Override
    public void reshape(
            int w,
            int h) {

        super.reshape(
                w,
                h
        );

        if (uiManager != null) {

            uiManager.onResize(
                    w,
                    h
            );
        }

        if (inventoryManager != null) {

            inventoryManager.updateLayout(
                    w,
                    h
            );
        }

        if (uiManager != null) {

            TalentWindow tw =
                    uiManager
                            .getTalentWindow();

            if (tw != null) {

                tw.updateLayout(
                        w,
                        h
                );
            }

            TraderWindow trw =
                    uiManager
                            .getTraderWindow();

            if (trw != null) {

                trw.updateLayout(
                        w,
                        h
                );
            }

            AuctionWindow aw =
                    uiManager
                            .getAuctionWindow();

            if (aw != null) {

                aw.updateLayout(
                        w,
                        h
                );
            }
        }
    }

    // ===== ГЕТТЕРЫ =====

    public static Main getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public UIManager getUIManager() {
        return uiManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public DropManager getDropManager() {
        return dropManager;
    }

    public boolean isWorldLoaded() {
        return worldLoaded;
    }
}