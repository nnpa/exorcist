package com.mygame.monsters;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.mygame.items.Item;
import com.mygame.items.LootTable;
import com.mygame.managers.DropManager;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.SoundManager;
import com.mygame.managers.WorldManager;

import java.util.List;

public class Monster {

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

    protected Node modelNode;
    protected Node healthBarNode;

    protected Geometry hpBarBackground;
    protected Geometry hpBarForeground;

    protected AnimComposer animComposer;

    protected Vector3f spawnPosition;
    protected Vector3f currentPosition;

    // ============================================================
    // СИСТЕМЫ
    // ============================================================

    protected LootTable lootTable;
    protected MonsterAI ai;
    protected DropManager dropManager;
    protected PlayerManager playerManager;
    protected WorldManager worldManager;

    // ============================================================
    // СОСТОЯНИЕ
    // ============================================================

    protected boolean isAlive = true;

    protected float deathTimer = -1f;

    protected static final float DEATH_DELAY_FALLBACK = 3.0f;

    // ============================================================
    // БОСС
    // ============================================================

    protected boolean isBoss = false;
    protected boolean isFinalBoss = false;

    protected String nextDungeonId = null;

    protected boolean increaseDifficultyOnDeath = false;

    // ============================================================
    // ПЕРЕХОД В СЛЕДУЮЩИЙ ДАНЖ
    // ============================================================

    protected float nextDungeonTimer = -1f;

    protected static final float NEXT_DUNGEON_DELAY = 10.0f;

    // ============================================================
    // ПРИЛОЖЕНИЕ
    // ============================================================

    protected static SimpleApplication app;

    // ============================================================
    // HP BAR
    // ============================================================

    protected static final float HP_BAR_WIDTH = 1.2f;
    protected static final float HP_BAR_HEIGHT = 0.08f;
    protected static final float HP_BAR_HEIGHT_OFFSET = 3.0f;

    protected float hpBarHeightOffset =
            HP_BAR_HEIGHT_OFFSET;

    protected static final float HP_BAR_LEFT_OFFSET = 0.6f;
    protected static final float HP_BAR_Z_OFFSET = 0.01f;

    // ============================================================
    // СТАТУСЫ
    // ============================================================

    public static boolean isGameRunning = true;

    protected float stunTimer = 0f;

    protected float bleedTimer = 0f;

    protected float bleedDamage = 0f;

    // ============================================================
    // ЭФФЕКТ СТАНА
    // ============================================================

    protected Node stunEffectNode;

    protected Spatial[] stunParticles;

    protected boolean stunEffectActive = false;

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public Monster() {

        this.ai = new MonsterAI(this);

        System.out.println(
                "[Monster] AI created"
        );
    }

    // ============================================================
    // APP
    // ============================================================

    public static void setApp(
            SimpleApplication application
    ) {

        app = application;
    }

    // ============================================================
    // GETTERS / SETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {

        this.health =
                Math.max(
                        0f,
                        health
                );

        updateHealthBar();
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {

        this.maxHealth =
                Math.max(
                        0f,
                        maxHealth
                );

        updateHealthBar();
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(float attackRange) {
        this.attackRange = attackRange;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getAggroRange() {
        return aggroRange;
    }

    public void setAggroRange(float aggroRange) {
        this.aggroRange = aggroRange;
    }

    // ============================================================
    // MODEL
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
         * Базовая ориентация модели.
         */
        modelNode.rotate(
                0f,
                -FastMath.HALF_PI,
                0f
        );

        modelNode.setName("Monster");

        if (spawnPosition != null) {

            modelNode.setLocalTranslation(
                    spawnPosition
            );
        }

        currentPosition =
                spawnPosition != null
                        ? spawnPosition.clone()
                        : new Vector3f(
                                0f,
                                0f,
                                0f
                        );

        animComposer =
                findAnimComposer(
                        modelNode
                );

        if (animComposer != null) {

            animComposer.setCurrentAction(
                    "Idle"
            );
        }

        createHealthBar();

        createStunEffect();
    }

    // ============================================================
    // POSITION
    // ============================================================

    public Vector3f getSpawnPosition() {
        return spawnPosition;
    }

    public void setSpawnPosition(
            Vector3f spawnPosition
    ) {

        if (spawnPosition == null) {

            this.spawnPosition = null;

            return;
        }

        this.spawnPosition =
                spawnPosition.clone();

        this.currentPosition =
                spawnPosition.clone();

        if (modelNode != null) {

            modelNode.setLocalTranslation(
                    spawnPosition
            );
        }
    }

    public Vector3f getPosition() {

        if (currentPosition == null) {

            currentPosition =
                    new Vector3f(
                            0f,
                            0f,
                            0f
                    );
        }

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
                    position
            );
        }
    }

    // ============================================================
    // СИСТЕМЫ
    // ============================================================

    public LootTable getLootTable() {
        return lootTable;
    }

    public void setLootTable(
            LootTable lootTable
    ) {

        this.lootTable = lootTable;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void setDropManager(
            DropManager dropManager
    ) {

        this.dropManager = dropManager;
    }

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

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public void setWorldManager(
            WorldManager worldManager
    ) {

        this.worldManager =
                worldManager;
    }

    public MonsterAI getAI() {
        return ai;
    }

    // ============================================================
    // БОСС
    // ============================================================

    public boolean isBoss() {
        return isBoss;
    }

    public void setBoss(boolean boss) {
        isBoss = boss;
    }

    public boolean isFinalBoss() {
        return isFinalBoss;
    }

    public void setFinalBoss(
            boolean finalBoss
    ) {

        isFinalBoss =
                finalBoss;
    }

    public String getNextDungeonId() {
        return nextDungeonId;
    }

    public void setNextDungeonId(
            String nextDungeonId
    ) {

        this.nextDungeonId =
                nextDungeonId;
    }

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
    // ATTACK RANGE
    // ============================================================

    public boolean isPlayerInAttackRange() {

        if (playerManager == null) {
            return false;
        }

        Vector3f monsterPos =
                getPosition();

        Vector3f playerPos =
                playerManager.getPosition();

        if (monsterPos == null ||
                playerPos == null) {

            return false;
        }

        float distance =
                getHorizontalDistance(
                        monsterPos,
                        playerPos
                );

        return distance <= attackRange;
    }

    protected float getHorizontalDistance(
            Vector3f a,
            Vector3f b
    ) {

        float dx =
                b.x - a.x;

        float dz =
                b.z - a.z;

        return FastMath.sqrt(
                dx * dx +
                dz * dz
        );
    }

    // ============================================================
    // СТАН
    // ============================================================

    public void applyStun(float duration) {

        stunTimer =
                Math.max(
                        stunTimer,
                        duration
                );

        System.out.println(
                ">>> СТАН НАЛОЖЕН НА: "
                + name
                + " на "
                + duration
                + " сек"
        );

        showStunEffect(true);
    }

    public float getStunTimer() {
        return stunTimer;
    }

    public boolean isStunned() {
        return stunTimer > 0f;
    }

    // ============================================================
    // BLEED
    // ============================================================

    public void applyBleed(
            float duration,
            float damagePerSecond
    ) {

        bleedTimer =
                duration;

        bleedDamage =
                damagePerSecond;

        System.out.println(
                ">>> КРОВОТЕЧЕНИЕ НАЛОЖЕНО НА: "
                + name
                + " на "
                + duration
                + " сек"
        );
    }

    // ============================================================
    // STOP WALKING
    // ============================================================

    public void stopWalking() {

        if (animComposer != null) {

            animComposer.setCurrentAction(
                    "Idle"
            );
        }
    }

    // ============================================================
    // DAMAGE
    // ============================================================

    public void takeDamage(float amount) {

        if (!isAlive) {
            return;
        }

        if (amount <= 0f) {
            return;
        }

        health -= amount;

        if (health < 0f) {
            health = 0f;
        }

        System.out.println(
                "[Monster] "
                + name
                + " took "
                + amount
                + " damage, HP: "
                + health
                + "/"
                + maxHealth
        );

        updateHealthBar();

        if (health <= 0f) {

            isAlive = false;

            onDeath();
        }
    }

    // ============================================================
    // HP BAR
    // ============================================================

    protected void createHealthBar() {

        if (app == null) {

            System.err.println(
                    "[Monster] app is null!"
            );

            return;
        }

        if (modelNode == null) {
            return;
        }

        if (healthBarNode != null) {
            return;
        }

        healthBarNode =
                new Node(
                        "HealthBarNode"
                );

        healthBarNode.setQueueBucket(
                RenderQueue.Bucket.Transparent
        );

        healthBarNode.setLocalTranslation(
                HP_BAR_LEFT_OFFSET,
                hpBarHeightOffset,
                0f
        );

        BillboardControl billboard =
                new BillboardControl();

        healthBarNode.addControl(
                billboard
        );

        Material backgroundMaterial =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        backgroundMaterial.setColor(
                "Color",
                new ColorRGBA(
                        0.12f,
                        0.12f,
                        0.12f,
                        0.95f
                )
        );

        Quad backgroundQuad =
                new Quad(
                        HP_BAR_WIDTH,
                        HP_BAR_HEIGHT
                );

        hpBarBackground =
                new Geometry(
                        "HPBarBackground",
                        backgroundQuad
                );

        hpBarBackground.setMaterial(
                backgroundMaterial
        );

        hpBarBackground.setQueueBucket(
                RenderQueue.Bucket.Transparent
        );

        Material foregroundMaterial =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        foregroundMaterial.setColor(
                "Color",
                ColorRGBA.Green
        );

        Quad foregroundQuad =
                new Quad(
                        HP_BAR_WIDTH,
                        HP_BAR_HEIGHT
                );

        hpBarForeground =
                new Geometry(
                        "HPBarForeground",
                        foregroundQuad
                );

        hpBarForeground.setMaterial(
                foregroundMaterial
        );

        hpBarForeground.setQueueBucket(
                RenderQueue.Bucket.Transparent
        );

        hpBarForeground.setLocalTranslation(
                0f,
                0f,
                HP_BAR_Z_OFFSET
        );

        healthBarNode.attachChild(
                hpBarBackground
        );

        healthBarNode.attachChild(
                hpBarForeground
        );

        modelNode.attachChild(
                healthBarNode
        );

        updateHealthBar();

        System.out.println(
                "[Monster] HP bar created for "
                + name
        );
    }

    public void updateHealthBar() {

        if (hpBarForeground == null) {
            return;
        }

        if (maxHealth <= 0f) {
            return;
        }

        float percent =
                health / maxHealth;

        percent =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                percent
                        )
                );

        hpBarForeground.setLocalScale(
                percent,
                1f,
                1f
        );
    }

    public void setHpBarHeightOffset(
            float offset
    ) {

        hpBarHeightOffset =
                offset;

        if (healthBarNode != null) {

            healthBarNode.setLocalTranslation(
                    HP_BAR_LEFT_OFFSET,
                    hpBarHeightOffset,
                    0f
            );
        }
    }

    // ============================================================
    // STUN EFFECT
    // ============================================================

    protected void createStunEffect() {

        if (app == null ||
                modelNode == null) {

            return;
        }

        if (stunEffectNode != null) {
            return;
        }

        stunEffectNode =
                new Node(
                        "StunEffectNode"
                );

        stunEffectNode.setLocalTranslation(
                0f,
                3.0f,
                0f
        );

        stunEffectNode.setCullHint(
                Spatial.CullHint.Always
        );

        Material particleMat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        particleMat.setColor(
                "Color",
                new ColorRGBA(
                        0.5f,
                        0.5f,
                        0.5f,
                        0.9f
                )
        );

        stunParticles =
                new Spatial[4];

        float radius = 0.6f;

        for (int i = 0; i < 4; i++) {

            float angle =
                    i * FastMath.HALF_PI;

            float x =
                    radius *
                    FastMath.cos(angle);

            float z =
                    radius *
                    FastMath.sin(angle);

            Quad quad =
                    new Quad(
                            0.2f,
                            0.2f
                    );

            Geometry particle =
                    new Geometry(
                            "StunParticle_" + i,
                            quad
                    );

            particle.setMaterial(
                    particleMat
            );

            particle.setLocalTranslation(
                    x,
                    0f,
                    z
            );

            BillboardControl billboard =
                    new BillboardControl();

            particle.addControl(
                    billboard
            );

            particle.setQueueBucket(
                    RenderQueue.Bucket.Transparent
            );

            stunParticles[i] =
                    particle;

            stunEffectNode.attachChild(
                    particle
            );
        }

        modelNode.attachChild(
                stunEffectNode
        );

        System.out.println(
                "[Monster] Stun effect created for "
                + name
        );
    }

    protected void showStunEffect(
            boolean show
    ) {

        if (stunEffectNode == null) {
            return;
        }

        if (show && isAlive) {

            stunEffectNode.setCullHint(
                    Spatial.CullHint.Inherit
            );

            stunEffectActive = true;

        } else {

            stunEffectNode.setCullHint(
                    Spatial.CullHint.Always
            );

            stunEffectActive = false;
        }
    }

    // ============================================================
    // ANIMATION
    // ============================================================

    protected AnimComposer findAnimComposer(
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

        animComposer.setCurrentAction(
                animName
        );
    }

    // ============================================================
    // DEATH
    // ============================================================

    protected void onDeath() {

        SoundManager.playSound(
                SoundManager.SOUND_MONSTER_DIE
        );

        showStunEffect(false);

        if (healthBarNode != null) {

            healthBarNode.setCullHint(
                    Spatial.CullHint.Always
            );
        }

        if (animComposer != null) {

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

        } else {

            removeModel();
        }

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

        if ((isBoss || isFinalBoss)
                && nextDungeonId != null
                && worldManager != null) {

            System.out.println(
                    "[Monster] Boss defeated! "
                    + "Transitioning to next dungeon in "
                    + NEXT_DUNGEON_DELAY
                    + " seconds..."
            );

            nextDungeonTimer =
                    NEXT_DUNGEON_DELAY;
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

            modelNode = null;
        }

        stunEffectNode = null;

        stunParticles = null;
    }

    // ============================================================
    // STOP
    // ============================================================

    public void stop() {

        isAlive = false;

        showStunEffect(false);

        if (ai != null) {

            ai.stop();

            ai = null;
        }

        if (modelNode != null &&
                modelNode.getParent() != null) {

            modelNode.getParent()
                    .detachChild(
                            modelNode
                    );
        }

        modelNode = null;

        stunEffectNode = null;

        stunParticles = null;

        System.out.println(
                "[Monster] "
                + name
                + " stopped."
        );
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public void update(float tpf) {

        if (!isGameRunning) {
            return;
        }

        // ========================================================
        // STUN
        // ========================================================

        if (isStunned()) {

            stunTimer -= tpf;

            if (stunTimer < 0f) {
                stunTimer = 0f;
            }

            if (ai != null) {
                ai.setStunned(true);
            }

            if (stunEffectActive &&
                    stunEffectNode != null) {

                stunEffectNode.rotate(
                        0f,
                        tpf * 4f,
                        0f
                );
            }

            if (stunTimer <= 0f) {

                showStunEffect(false);
            }

            return;
        }

        if (ai != null) {
            ai.setStunned(false);
        }

        if (stunEffectActive) {
            showStunEffect(false);
        }

        // ========================================================
        // BLEED
        // ========================================================

        if (bleedTimer > 0f) {

            bleedTimer -= tpf;

            takeDamage(
                    bleedDamage * tpf
            );
        }

        // ========================================================
        // DEATH
        // ========================================================

        if (!isAlive) {

            if (deathTimer > 0f) {

                deathTimer -= tpf;

                if (deathTimer <= 0f) {

                    removeModel();
                }
            }

            if (nextDungeonTimer > 0f) {

                nextDungeonTimer -= tpf;

                if (nextDungeonTimer <= 0f) {

                    nextDungeonTimer = -1f;

                    if (worldManager != null &&
                            nextDungeonId != null) {

                        int newDifficulty =
                                playerManager != null
                                        ? playerManager.getCurrentDifficulty()
                                        : 1;

                        if (increaseDifficultyOnDeath) {
                            newDifficulty++;
                        }

                        if (playerManager != null) {

                            playerManager
                                    .updateDungeonProgress(
                                            nextDungeonId,
                                            newDifficulty
                                    );
                        }

                        if (app != null) {

                            app.enqueue(() -> {

                                worldManager.changeDungeon(
                                        nextDungeonId,
                                        increaseDifficultyOnDeath
                                );

                                return null;
                            });
                        }
                    }
                }
            }

            return;
        }

        // ========================================================
        // Обычный Monster обновляет AI
        // ========================================================

        if (ai != null) {

            ai.update(tpf);
        }
    }
}