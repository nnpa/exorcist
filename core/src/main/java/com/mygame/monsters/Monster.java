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

    private Node modelNode;
    private Node healthBarNode;
    private Geometry hpBarBackground;
    private Geometry hpBarForeground;
    private AnimComposer animComposer;
    private Vector3f spawnPosition;
    private Vector3f currentPosition;

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
    // БОСС: Задержка перехода на следующий уровень
    // ============================================================

    private float nextDungeonTimer = -1f;
    private static final float NEXT_DUNGEON_DELAY = 10.0f;

    // ============================================================
    // APPLICATION
    // ============================================================

    private static SimpleApplication app;

    // ============================================================
    // HP BAR SETTINGS
    // ============================================================

    private static final float HP_BAR_WIDTH = 1.2f;
    private static final float HP_BAR_HEIGHT = 0.08f;
    private static final float HP_BAR_HEIGHT_OFFSET = 3.0f;
    private static final float HP_BAR_LEFT_OFFSET = 0.6f;
    private static final float HP_BAR_Z_OFFSET = 0.01f;

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public Monster() {
        this.ai = new MonsterAI(this);
        System.out.println("[Monster] AI created");
    }

    // ============================================================
    // APP
    // ============================================================

    public static void setApp(SimpleApplication application) {
        app = application;
    }

    // ============================================================
    // GETTERS / SETTERS
    // ============================================================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    // ============================================================
    // HEALTH
    // ============================================================

    public float getHealth() { return health; }
    public void setHealth(float health) {
        this.health = Math.max(0f, health);
        updateHealthBar();
    }
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = Math.max(0f, maxHealth);
        updateHealthBar();
    }

    // ============================================================
    // DAMAGE
    // ============================================================

    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }
    public float getAttackRange() { return attackRange; }
    public void setAttackRange(float attackRange) { this.attackRange = attackRange; }
    public float getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }
    public float getAggroRange() { return aggroRange; }
    public void setAggroRange(float aggroRange) { this.aggroRange = aggroRange; }

    // ============================================================
    // MODEL
    // ============================================================

    public Node getModelNode() { return modelNode; }
    public void setModelNode(Node modelNode) {
        this.modelNode = modelNode;
        if (modelNode == null) return;

        modelNode.rotate(0, -FastMath.HALF_PI, 0);
        modelNode.setName("Monster");

        if (spawnPosition != null) {
            modelNode.setLocalTranslation(spawnPosition);
        }
        currentPosition = spawnPosition != null ? spawnPosition.clone() : new Vector3f(0,0,0);

        animComposer = findAnimComposer(modelNode);
        if (animComposer != null) {
            animComposer.setCurrentAction("Idle");
        }
        createHealthBar();
    }

    // ============================================================
    // POSITION
    // ============================================================

    public Vector3f getSpawnPosition() { return spawnPosition; }
    public void setSpawnPosition(Vector3f spawnPosition) {
        if (spawnPosition == null) { this.spawnPosition = null; return; }
        this.spawnPosition = spawnPosition.clone();
        this.currentPosition = spawnPosition.clone();
        if (modelNode != null) { modelNode.setLocalTranslation(spawnPosition); }
    }
    public Vector3f getPosition() { return currentPosition; }
public void setPosition(Vector3f position) {
    if (position == null) return;
    //System.out.println("[Monster] setPosition called for " + name + " -> " + position);
    this.currentPosition = position.clone();
    if (modelNode != null) {
        modelNode.setLocalTranslation(position);
       // System.out.println("[Monster] Model moved to " + modelNode.getLocalTranslation());
    }
}

    // ============================================================
    // SYSTEMS
    // ============================================================

    public LootTable getLootTable() { return lootTable; }
    public void setLootTable(LootTable lootTable) { this.lootTable = lootTable; }
    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }
    public void setDropManager(DropManager dropManager) { this.dropManager = dropManager; }
    public void setPlayerManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
        if (ai != null) { ai.setPlayerManager(playerManager); }
    }
    public void setWorldManager(WorldManager worldManager) { this.worldManager = worldManager; }

    // ============================================================
    // BOSS
    // ============================================================

    public boolean isBoss() { return isBoss; }
    public void setBoss(boolean boss) { isBoss = boss; }
    public boolean isFinalBoss() { return isFinalBoss; }
    public void setFinalBoss(boolean finalBoss) { isFinalBoss = finalBoss; }
    public String getNextDungeonId() { return nextDungeonId; }
    public void setNextDungeonId(String nextDungeonId) { this.nextDungeonId = nextDungeonId; }
    public boolean isIncreaseDifficultyOnDeath() { return increaseDifficultyOnDeath; }
    public void setIncreaseDifficultyOnDeath(boolean increase) { this.increaseDifficultyOnDeath = increase; }

    // ============================================================
    // TAKE DAMAGE
    // ============================================================

    public void takeDamage(float amount) {
        if (!isAlive) return;
        if (amount <= 0f) return;
        health -= amount;
        if (health < 0f) health = 0f;
        System.out.println("[Monster] " + name + " took " + amount + " damage, HP: " + health + "/" + maxHealth);
        updateHealthBar();

        if (health <= 0f) {
            isAlive = false;
            onDeath();
        } else {
            //playAnimation("GetHit");
        }
    }

    // ============================================================
    // CREATE HP BAR
    // ============================================================

    private void createHealthBar() {
        if (app == null) {
            System.err.println("[Monster] app is null!");
            return;
        }
        if (modelNode == null) return;
        if (healthBarNode != null) return;

        healthBarNode = new Node("HealthBarNode");
        healthBarNode.setQueueBucket(RenderQueue.Bucket.Transparent);
        healthBarNode.setLocalTranslation(HP_BAR_LEFT_OFFSET, HP_BAR_HEIGHT_OFFSET, 0);

        BillboardControl billboard = new BillboardControl();
        healthBarNode.addControl(billboard);

        Material backgroundMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        backgroundMaterial.setColor("Color", new ColorRGBA(0.12f, 0.12f, 0.12f, 0.95f));
        Quad backgroundQuad = new Quad(HP_BAR_WIDTH, HP_BAR_HEIGHT);
        hpBarBackground = new Geometry("HPBarBackground", backgroundQuad);
        hpBarBackground.setMaterial(backgroundMaterial);
        hpBarBackground.setQueueBucket(RenderQueue.Bucket.Transparent);

        Material foregroundMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        foregroundMaterial.setColor("Color", ColorRGBA.Green);
        Quad foregroundQuad = new Quad(HP_BAR_WIDTH, HP_BAR_HEIGHT);
        hpBarForeground = new Geometry("HPBarForeground", foregroundQuad);
        hpBarForeground.setMaterial(foregroundMaterial);
        hpBarForeground.setQueueBucket(RenderQueue.Bucket.Transparent);
        hpBarForeground.setLocalTranslation(0, 0, HP_BAR_Z_OFFSET);

        healthBarNode.attachChild(hpBarBackground);
        healthBarNode.attachChild(hpBarForeground);
        modelNode.attachChild(healthBarNode);

        updateHealthBar();
        System.out.println("[Monster] HP bar created for " + name);
    }

    // ============================================================
    // UPDATE HP BAR
    // ============================================================

    public void updateHealthBar() {
        if (hpBarForeground == null) return;
        if (maxHealth <= 0f) return;
        float percent = health / maxHealth;
        percent = Math.max(0f, Math.min(1f, percent));
        hpBarForeground.setLocalScale(percent, 1f, 1f);
    }

    // ============================================================
    // DEATH
    // ============================================================

    protected void onDeath() {
        SoundManager.playSound(SoundManager.SOUND_MONSTER_DIE);
        if (healthBarNode != null) {
            healthBarNode.setCullHint(Spatial.CullHint.Always);
        }

        if (animComposer != null) {
            Action dieAction = animComposer.makeAction("Die");
            Tween doneTween = Tweens.callMethod(this, "removeModel");
            Action sequence = animComposer.actionSequence("die_sequence", dieAction, doneTween);
            animComposer.setCurrentAction("die_sequence");
        } else {
            removeModel();
        }

        if (lootTable != null && dropManager != null) {
            int difficulty = playerManager != null ? playerManager.getCurrentDifficulty() : 1;
            List<Item> items = lootTable.rollForLoot(difficulty);
            if (!items.isEmpty()) {
                dropManager.spawnDrops(currentPosition, items);
            }
        }

        // ========================================================
        // NEXT DUNGEON (ИЗМЕНЕНО: Таймер вместо мгновенного перехода)
        // ========================================================

        if ((isBoss || isFinalBoss) && nextDungeonId != null && worldManager != null) {
            System.out.println("[Monster] Boss defeated! Transitioning to next dungeon in " + NEXT_DUNGEON_DELAY + " seconds...");
            nextDungeonTimer = NEXT_DUNGEON_DELAY;
        }
    }

    // ============================================================
    // REMOVE MODEL
    // ============================================================

    public void removeModel() {
        if (modelNode != null && modelNode.getParent() != null) {
            modelNode.getParent().detachChild(modelNode);
            modelNode = null;
        }
    }

    // ============================================================
    // FIND ANIM COMPOSER
    // ============================================================

    private AnimComposer findAnimComposer(Spatial spatial) {
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                AnimComposer found = findAnimComposer(child);
                if (found != null) return found;
            }
        }
        return spatial.getControl(AnimComposer.class);
    }

    // ============================================================
    // PLAY ANIMATION
    // ============================================================

    public void playAnimation(String animName) {
        if (animComposer == null) return;
        if (animName == null || animName.isEmpty()) return;
        animComposer.setCurrentAction(animName);
    }

    // ============================================================
    // UPDATE (ИЗМЕНЕНО: Добавлен отсчет таймера перехода)
    // ============================================================

        // ============================================================
    // UPDATE
    // ============================================================
public static boolean isGameRunning = true;

// Добавьте метод stop
public void stop() {
    isAlive = false;
    if (ai != null) {
        ai.stop();
        ai = null;
    }
    if (modelNode != null && modelNode.getParent() != null) {
        modelNode.getParent().detachChild(modelNode);
    }
    modelNode = null;
    System.out.println("[Monster] " + name + " stopped.");
}
    public void update(float tpf) {
         if (!isGameRunning) return;
        if (!isAlive) {
            if (deathTimer > 0) {
                deathTimer -= tpf;
                if (deathTimer <= 0) {
                    removeModel();
                }
            }

            // ===== ОТСЧЕТ ТАЙМЕРА ПЕРЕХОДА НА СЛЕДУЮЩИЙ УРОВЕНЬ =====
            if (nextDungeonTimer > 0) {
                nextDungeonTimer -= tpf;
                if (nextDungeonTimer <= 0) {
                    nextDungeonTimer = -1f;

                    if (worldManager != null && nextDungeonId != null) {

                        // 1. Вычисляем новую сложность (если это финальный босс)
                        int newDifficulty = playerManager != null ? playerManager.getCurrentDifficulty() : 1;
                        if (increaseDifficultyOnDeath) {
                            newDifficulty++;
                        }

                        // 2. Обновляем данные игрока и СОХРАНЯЕМ В БАЗУ
                        if (playerManager != null) {
                            playerManager.updateDungeonProgress(nextDungeonId, newDifficulty);
                        }

                        // 3. Переходим в новый данж (отложенно, чтобы избежать ConcurrentModificationException)
                        if (app != null) {
                            app.enqueue(() -> {
                                worldManager.changeDungeon(nextDungeonId, increaseDifficultyOnDeath);
                                return null;
                            });
                        } else {
                            worldManager.changeDungeon(nextDungeonId, increaseDifficultyOnDeath);
                        }
                    }
                }
            }
            // =======================================================

            return;
        }

        if (ai != null) {
            ai.update(tpf);
        }
    }
}