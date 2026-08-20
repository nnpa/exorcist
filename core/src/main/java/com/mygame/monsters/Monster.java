package com.mygame.monsters;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;

import com.mygame.items.Item;
import com.mygame.items.LootTable;
import com.mygame.managers.DropManager;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.SoundManager;
import com.mygame.managers.WorldManager;

import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.ProgressBar;
import com.simsilica.lemur.RangedValueModel;
import com.simsilica.lemur.component.QuadBackgroundComponent;

import java.util.List;

public class Monster {

    // ============================================================
    // БОСС MUSIC
    // ============================================================

    private boolean bossMusicStarted = false;

    // ============================================================
    // БАЗОВЫЕ ПОЛЯ
    // ============================================================

    private String id;
    private String name;
    private int level;

    private float health;
    private float maxHealth;

    private float damage;
    private float attackRange;
    private float moveSpeed;
    private float aggroRange;

    // ============================================================
    // МОДЕЛЬ И АНИМАЦИЯ
    // ============================================================

    private Node modelNode;
    private Node healthBarNode;

    private AnimComposer animComposer;

    private Vector3f spawnPosition;
    private Vector3f currentPosition;

    // ============================================================
    // HP BAR
    // ============================================================

    private ProgressBar hpBar;

    /*
     * Модель HP.
     *
     * Диапазон:
     *
     * 0                  maxHealth
     * |----------------------|
     *
     * Значение:
     *
     * health
     */
    private RangedValueModel hpModel;

    // ============================================================
    // СИСТЕМЫ
    // ============================================================

    private LootTable lootTable;
    private MonsterAI ai;

    private DropManager dropManager;
    private PlayerManager playerManager;
    private WorldManager worldManager;

    // ============================================================
    // СОСТОЯНИЕ
    // ============================================================

    private boolean isAlive = true;

    private float deathTimer = -1f;

    private static final float DEATH_DELAY_FALLBACK = 3.0f;

    // ============================================================
    // БОСС
    // ============================================================

    private boolean isBoss = false;
    private boolean isFinalBoss = false;

    private String nextDungeonId = null;

    private boolean increaseDifficultyOnDeath = false;

    // ============================================================
    // APPLICATION
    // ============================================================

    private static SimpleApplication app;

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public Monster() {

        this.ai = new MonsterAI(this);

        System.out.println("[Monster] AI created");
    }

    // ============================================================
    // APPLICATION
    // ============================================================

    public static void setApp(SimpleApplication application) {

        app = application;

        System.out.println("[Monster] Application assigned");
    }

    // ============================================================
    // ID
    // ============================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // ============================================================
    // NAME
    // ============================================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // ============================================================
    // LEVEL
    // ============================================================

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    // ============================================================
    // HEALTH
    // ============================================================

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {

        /*
         * Не позволяем HP быть отрицательным.
         */
        this.health = Math.max(0f, health);

        /*
         * Если maxHealth уже известен,
         * ограничиваем HP сверху.
         */
        if (maxHealth > 0f &&
                this.health > maxHealth) {

            this.health = maxHealth;
        }

        /*
         * Если ProgressBar уже создан,
         * синхронизируем его.
         */
        updateHealthBar();
    }

    // ============================================================
    // MAX HEALTH
    // ============================================================

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {

        this.maxHealth =
                Math.max(0f, maxHealth);

        /*
         * Если maxHealth уже установлен,
         * HP не может быть больше него.
         */
        if (this.maxHealth > 0f &&
                this.health > this.maxHealth) {

            this.health = this.maxHealth;
        }

        /*
         * Если HP-bar уже существует,
         * необходимо создать модель с новым диапазоном.
         */
        if (hpBar != null) {

            rebuildHealthBarModel();

        } else {

            updateHealthBar();
        }
    }

    // ============================================================
    // DAMAGE
    // ============================================================

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    // ============================================================
    // ATTACK RANGE
    // ============================================================

    public float getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(float attackRange) {
        this.attackRange = attackRange;
    }

    // ============================================================
    // MOVE SPEED
    // ============================================================

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    // ============================================================
    // AGGRO RANGE
    // ============================================================

    public float getAggroRange() {
        return aggroRange;
    }

    public void setAggroRange(float aggroRange) {
        this.aggroRange = aggroRange;
    }

    // ============================================================
    // MODEL NODE
    // ============================================================

    public Node getModelNode() {
        return modelNode;
    }

    public void setModelNode(Node modelNode) {

        this.modelNode = modelNode;

        if (modelNode == null) {
            return;
        }

        /*
         * Поворот модели.
         */
        modelNode.rotate(
                0,
                -FastMath.HALF_PI,
                0
        );

        modelNode.setName("Monster");

        /*
         * Позиция появления.
         */
        if (spawnPosition != null) {

            modelNode.setLocalTranslation(
                    spawnPosition
            );
        }

        /*
         * Текущая позиция.
         */
        currentPosition =
                spawnPosition != null
                        ? spawnPosition.clone()
                        : new Vector3f(0, 0, 0);

        // ========================================================
        // ANIMATION COMPOSER
        // ========================================================

        animComposer =
                findAnimComposer(modelNode);

        if (animComposer != null) {

            try {

                animComposer.setCurrentAction("Idle");

            } catch (Exception e) {

                System.err.println(
                        "[Monster] Idle animation error: "
                                + e.getMessage()
                );
            }
        }

        // ========================================================
        // HP BAR
        // ========================================================

        createHealthBar();
    }

    // ============================================================
    // SPAWN POSITION
    // ============================================================

    public Vector3f getSpawnPosition() {
        return spawnPosition;
    }

    public void setSpawnPosition(
            Vector3f spawnPosition
    ) {

        if (spawnPosition == null) {

            this.spawnPosition =
                    new Vector3f(0, 0, 0);

        } else {

            this.spawnPosition =
                    spawnPosition.clone();
        }

        this.currentPosition =
                this.spawnPosition.clone();

        if (modelNode != null) {

            modelNode.setLocalTranslation(
                    this.spawnPosition
            );
        }
    }

    // ============================================================
    // CURRENT POSITION
    // ============================================================

    public Vector3f getPosition() {
        return currentPosition;
    }

    public void setPosition(
            Vector3f position
    ) {

        if (position == null) {
            return;
        }

        this.currentPosition =
                position.clone();

        if (modelNode != null) {

            modelNode.setLocalTranslation(
                    this.currentPosition
            );
        }
    }

    // ============================================================
    // LOOT
    // ============================================================

    public LootTable getLootTable() {
        return lootTable;
    }

    public void setLootTable(
            LootTable lootTable
    ) {

        this.lootTable = lootTable;
    }

    // ============================================================
    // ALIVE
    // ============================================================

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    // ============================================================
    // DROP MANAGER
    // ============================================================

    public void setDropManager(
            DropManager dropManager
    ) {

        this.dropManager = dropManager;
    }

    // ============================================================
    // PLAYER MANAGER
    // ============================================================

    public void setPlayerManager(
            PlayerManager playerManager
    ) {

        this.playerManager =
                playerManager;

        if (ai != null) {

            ai.setPlayerManager(
                    playerManager
            );
        }
    }

    // ============================================================
    // WORLD MANAGER
    // ============================================================

    public void setWorldManager(
            WorldManager worldManager
    ) {

        this.worldManager =
                worldManager;
    }

    // ============================================================
    // BOSS
    // ============================================================

    public boolean isBoss() {
        return isBoss;
    }

    public void setBoss(boolean boss) {
        isBoss = boss;
    }

    // ============================================================
    // FINAL BOSS
    // ============================================================

    public boolean isFinalBoss() {
        return isFinalBoss;
    }

    public void setFinalBoss(
            boolean finalBoss
    ) {

        isFinalBoss = finalBoss;
    }

    // ============================================================
    // NEXT DUNGEON
    // ============================================================

    public String getNextDungeonId() {
        return nextDungeonId;
    }

    public void setNextDungeonId(
            String nextDungeonId
    ) {

        this.nextDungeonId =
                nextDungeonId;
    }

    // ============================================================
    // DIFFICULTY
    // ============================================================

    public boolean isIncreaseDifficultyOnDeath() {
        return increaseDifficultyOnDeath;
    }

    public void setIncreaseDifficultyOnDeath(
            boolean increase
    ) {

        this.increaseDifficultyOnDeath =
                increase;
    }

    // ============================================================
    // TAKE DAMAGE
    // ============================================================

    public void takeDamage(float amount) {

        /*
         * Мёртвый монстр больше не получает урон.
         */
        if (!isAlive) {
            return;
        }

        /*
         * Некорректный урон игнорируем.
         */
        if (amount <= 0f) {
            return;
        }

        // ========================================================
        // УРОН
        // ========================================================

        health -= amount;

        /*
         * HP минимум 0.
         */
        if (health < 0f) {

            health = 0f;
        }

        System.out.println(
                "[Monster] " +
                        name +
                        " took " +
                        amount +
                        " damage"
        );

        System.out.println(
                "[Monster] HP = " +
                        health +
                        "/" +
                        maxHealth
        );

        // ========================================================
        // ОБНОВЛЕНИЕ HP BAR
        // ========================================================

        updateHealthBar();

        // ========================================================
        // СМЕРТЬ
        // ========================================================

        if (health <= 0f) {

            isAlive = false;

            onDeath();

        } else {

            // ====================================================
            // GET HIT
            // ====================================================

            playAnimation("GetHit");
        }
    }

    // ============================================================
    // CREATE HEALTH BAR
    // ============================================================

    private void createHealthBar() {

        if (app == null) {

            System.err.println(
                    "[Monster] app == null. " +
                            "Call Monster.setApp() first."
            );

            return;
        }

        if (modelNode == null) {

            System.err.println(
                    "[Monster] modelNode == null."
            );

            return;
        }

        // ========================================================
        // УДАЛЯЕМ СТАРЫЙ BAR
        // ========================================================

        if (healthBarNode != null) {

            healthBarNode.removeFromParent();
        }

        healthBarNode =
                new Node("HealthBarNode");

        healthBarNode.setQueueBucket(
                RenderQueue.Bucket.Transparent
        );

        // ========================================================
        // ДИАПАЗОН HP
        // ========================================================

        /*
         * Если maxHealth ещё не установлен,
         * временно используем 1.
         */
        double max =
                maxHealth > 0f
                        ? maxHealth
                        : 1.0;

        /*
         * Текущее значение.
         */
        double value =
                Math.max(
                        0.0,
                        Math.min(
                                health,
                                max
                        )
                );

        // ========================================================
        // СОЗДАЁМ МОДЕЛЬ
        // ========================================================

        hpModel =
                new DefaultRangedValueModel(
                        0.0,
                        max,
                        value
                );

        // ========================================================
        // СОЗДАЁМ PROGRESS BAR
        // ========================================================

        hpBar =
                new ProgressBar(
                        hpModel
                );

        hpBar.setPreferredSize(
                new Vector3f(
                        1.2f,
                        0.15f,
                        0f
                )
        );

        // ========================================================
        // СЕРЫЙ ФОН
        // ========================================================

        hpBar.setBackground(
                new QuadBackgroundComponent(
                        new ColorRGBA(
                                0.2f,
                                0.2f,
                                0.2f,
                                0.8f
                        )
                )
        );

        // ========================================================
        // ЗЕЛЁНАЯ ЗАПОЛНЕННАЯ ЧАСТЬ
        // ========================================================

        /*
         * ВАЖНО:
         *
         * Получаем indicator только здесь.
         *
         * Больше НЕ меняем его в updateHealthBar().
         */
        Panel indicator =
                hpBar.getValueIndicator();

        if (indicator != null) {

            indicator.setBackground(
                    new QuadBackgroundComponent(
                            ColorRGBA.Green
                    )
            );
        }

        // ========================================================
        // ПОЗИЦИЯ
        // ========================================================

        hpBar.setLocalTranslation(
                0f,
                1.2f,
                0f
        );

        // ========================================================
        // BILLBOARD
        // ========================================================

        BillboardControl billboard =
                new BillboardControl();

        healthBarNode.addControl(
                billboard
        );

        // ========================================================
        // ATTACH BAR
        // ========================================================

        healthBarNode.attachChild(
                hpBar
        );

        modelNode.attachChild(
                healthBarNode
        );

        // ========================================================
        // VISIBILITY
        // ========================================================

        if (health <= 0f) {

            hpBar.setCullHint(
                    Spatial.CullHint.Always
            );

        } else {

            hpBar.setCullHint(
                    Spatial.CullHint.Inherit
            );
        }

        System.out.println(
                "[Monster] Health bar created: " +
                        health +
                        "/" +
                        maxHealth
        );
    }

    // ============================================================
    // REBUILD HP MODEL
    // ============================================================

    private void rebuildHealthBarModel() {

        if (hpBar == null) {
            return;
        }

        if (maxHealth <= 0f) {
            return;
        }

        /*
         * Текущее значение.
         */
        double value =
                Math.max(
                        0.0,
                        Math.min(
                                health,
                                maxHealth
                        )
                );

        /*
         * Создаём новую модель.
         */
        hpModel =
                new DefaultRangedValueModel(
                        0.0,
                        maxHealth,
                        value
                );

        /*
         * Передаём новую модель ProgressBar.
         */
        hpBar.setModel(
                hpModel
        );

        /*
         * После смены модели снова устанавливаем
         * зелёный фон индикатора.
         *
         * Но это происходит только при изменении
         * maxHealth, а НЕ при каждом ударе.
         */
        Panel indicator =
                hpBar.getValueIndicator();

        if (indicator != null) {

            indicator.setBackground(
                    new QuadBackgroundComponent(
                            ColorRGBA.Green
                    )
            );
        }

        if (health <= 0f) {

            hpBar.setCullHint(
                    Spatial.CullHint.Always
            );

        } else {

            hpBar.setCullHint(
                    Spatial.CullHint.Inherit
            );
        }
    }

    // ============================================================
    // UPDATE HEALTH BAR
    // ============================================================

    public void updateHealthBar() {

        /*
         * HP bar может ещё не существовать.
         *
         * Например:
         *
         * setHealth()
         * вызывается до
         * setModelNode()
         */
        if (hpBar == null) {
            return;
        }

        if (hpModel == null) {
            return;
        }

        if (maxHealth <= 0f) {
            return;
        }

        // ========================================================
        // CLAMP
        // ========================================================

        if (health < 0f) {

            health = 0f;
        }

        if (health > maxHealth) {

            health = maxHealth;
        }

        // ========================================================
        // ГЛАВНОЕ ИЗМЕНЕНИЕ
        // ========================================================

        /*
         * НЕ используем:
         *
         * hpBar.setProgressPercent(...)
         *
         * НЕ меняем:
         *
         * getValueIndicator()
         *
         * НЕ меняем цвет.
         *
         * Просто передаём новое значение модели.
         *
         * Lemur сам изменит размер заполненной части.
         */
        hpModel.setValue(
                health
        );

        // ========================================================
        // ПРОЦЕНТ ДЛЯ DEBUG
        // ========================================================

        double percent =
                health / maxHealth;

        System.out.println(
                "[Monster] HP BAR UPDATE: " +
                        health +
                        "/" +
                        maxHealth +
                        " = " +
                        (percent * 100.0) +
                        "%"
        );

        // ========================================================
        // VISIBILITY
        // ========================================================

        /*
         * Скрываем только при смерти.
         *
         * При первом/обычном ударе CullHint НЕ меняется.
         */
        if (health <= 0f) {

            hpBar.setCullHint(
                    Spatial.CullHint.Always
            );

        } else {

            hpBar.setCullHint(
                    Spatial.CullHint.Inherit
            );
        }
    }

    // ============================================================
    // DEATH
    // ============================================================

    protected void onDeath() {

        // ========================================================
        // SOUND
        // ========================================================

        SoundManager.playSound(
                SoundManager.SOUND_MONSTER_DIE
        );

        // ========================================================
        // АНИМАЦИЯ СМЕРТИ
        // ========================================================

        if (animComposer != null) {

            try {

                Action dieAction =
                        animComposer.makeAction(
                                "Die"
                        );

                Tween doneTween =
                        Tweens.callMethod(
                                this,
                                "removeModel"
                        );

                Action sequence =
                        animComposer.actionSequence(
                                "die_sequence",
                                dieAction,
                                doneTween
                        );

                animComposer.setCurrentAction(
                        "die_sequence"
                );

            } catch (Exception e) {

                System.err.println(
                        "[Monster] Death animation error: " +
                                e.getMessage()
                );

                removeModel();
            }

        } else {

            removeModel();
        }

        // ========================================================
        // HIDE HP BAR
        // ========================================================

        if (healthBarNode != null) {

            healthBarNode.setCullHint(
                    Spatial.CullHint.Always
            );
        }

        // ========================================================
        // DROP
        // ========================================================

        if (lootTable != null &&
                dropManager != null) {

            int difficulty =
                    playerManager != null
                            ? playerManager.getCurrentDifficulty()
                            : 1;

            List<Item> items =
                    lootTable.rollForLoot(
                            difficulty
                    );

            if (!items.isEmpty()) {

                dropManager.spawnDrops(
                        currentPosition,
                        items
                );
            }
        }

        // ========================================================
        // BOSS DUNGEON
        // ========================================================

        if ((isBoss || isFinalBoss) &&
                nextDungeonId != null &&
                worldManager != null) {

            worldManager.changeDungeon(
                    nextDungeonId,
                    increaseDifficultyOnDeath
            );
        }
    }

    // ============================================================
    // REMOVE MODEL
    // ============================================================

    public void removeModel() {

        if (modelNode != null &&
                modelNode.getParent() != null) {

            modelNode.getParent()
                    .detachChild(
                            modelNode
                    );
        }

        modelNode = null;
    }

    // ============================================================
    // FIND ANIM COMPOSER
    // ============================================================

    private AnimComposer findAnimComposer(
            Spatial spatial
    ) {

        if (spatial instanceof Node) {

            for (Spatial child :
                    ((Node) spatial).getChildren()) {

                AnimComposer found =
                        findAnimComposer(child);

                if (found != null) {

                    return found;
                }
            }
        }

        return spatial.getControl(
                AnimComposer.class
        );
    }

    // ============================================================
    // PLAY ANIMATION
    // ============================================================

    public void playAnimation(
            String animName
    ) {

        if (animComposer == null) {
            return;
        }

        if (animName == null ||
                animName.isEmpty()) {

            return;
        }

        try {

            animComposer.setCurrentAction(
                    animName
            );

        } catch (Exception e) {

            System.err.println(
                    "[Monster] Animation error: " +
                            animName +
                            " -> " +
                            e.getMessage()
            );
        }
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public void update(float tpf) {

        // ========================================================
        // DEAD
        // ========================================================

        if (!isAlive) {

            if (deathTimer > 0f) {

                deathTimer -= tpf;

                if (deathTimer <= 0f) {

                    removeModel();
                }
            }

            return;
        }

        // ========================================================
        // AI
        // ========================================================

        if (ai != null) {

            ai.update(tpf);
        }
    }
}