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
import com.jme3.scene.shape.Quad;
import com.jme3.scene.control.BillboardControl;
import com.mygame.items.Item;
import com.mygame.items.LootTable;
import com.mygame.managers.DropManager;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.WorldManager;

import java.util.List;

public class Monster {

    // ===== БАЗОВЫЕ ПОЛЯ =====
    private String id;
    private String name;
    private int level;
    private float health;
    private float maxHealth;
    private float damage;
    private float attackRange;
    private float moveSpeed;
    private float aggroRange;

    // ===== МОДЕЛЬ И АНИМАЦИЯ =====
    private Node modelNode;
    private Node healthBarNode;
    private Geometry hpBarBackground;
    private Geometry hpBarForeground;
    private AnimComposer animComposer;
    private Vector3f spawnPosition;
    private Vector3f currentPosition;

    // ===== СИСТЕМЫ =====
    private LootTable lootTable;
    private MonsterAI ai;
    private DropManager dropManager;
    private PlayerManager playerManager;
    private WorldManager worldManager; // для смены данжа

    // ===== СОСТОЯНИЕ =====
    private boolean isAlive = true;
    private float deathTimer = -1f;
    private static final float DEATH_DELAY_FALLBACK = 3.0f;

    // ===== ФЛАГИ БОССА (НОВЫЕ ПОЛЯ) =====
    private boolean isBoss = false;
    private boolean isFinalBoss = false;
    private String nextDungeonId = null;
    private boolean increaseDifficultyOnDeath = false;

    // ===== ВСПОМОГАТЕЛЬНОЕ =====
    private static SimpleApplication app;

    // ===== КОНСТРУКТОРЫ =====
    public Monster() {
        this.ai = new MonsterAI(this);
        System.out.println("[Monster] AI created");
    }

    public static void setApp(SimpleApplication application) {
        app = application;
    }

    // ===== ГЕТТЕРЫ И СЕТТЕРЫ (базовые) =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = health; }

    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }

    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }

    public float getAttackRange() { return attackRange; }
    public void setAttackRange(float attackRange) { this.attackRange = attackRange; }

    public float getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }

    public float getAggroRange() { return aggroRange; }
    public void setAggroRange(float aggroRange) { this.aggroRange = aggroRange; }

    public Node getModelNode() { return modelNode; }
    public void setModelNode(Node modelNode) {
        this.modelNode = modelNode;
        if (modelNode != null) {
            modelNode.rotate(0, -FastMath.HALF_PI, 0);
            modelNode.setName("Monster");
            if (spawnPosition != null) modelNode.setLocalTranslation(spawnPosition);
            this.currentPosition = spawnPosition != null ? spawnPosition.clone() : new Vector3f(0, 0, 0);
            animComposer = findAnimComposer(modelNode);
            if (animComposer != null) {
                animComposer.setCurrentAction("Idle");
            }
            createHealthBar();
        }
    }

    public Vector3f getSpawnPosition() { return spawnPosition; }
    public void setSpawnPosition(Vector3f spawnPosition) {
        this.spawnPosition = spawnPosition;
        this.currentPosition = spawnPosition.clone();
        if (modelNode != null) modelNode.setLocalTranslation(spawnPosition);
    }

    public Vector3f getPosition() { return currentPosition; }
    public void setPosition(Vector3f position) {
        this.currentPosition = position;
        if (modelNode != null) modelNode.setLocalTranslation(position);
    }

    public LootTable getLootTable() { return lootTable; }
    public void setLootTable(LootTable lootTable) { this.lootTable = lootTable; }

    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }

    public void setDropManager(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    public void setPlayerManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
        if (ai != null) {
            ai.setPlayerManager(playerManager);
        }
    }

    public void setWorldManager(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    // ===== НОВЫЕ ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ БОССОВ =====
    public boolean isBoss() { return isBoss; }
    public void setBoss(boolean boss) { isBoss = boss; }

    public boolean isFinalBoss() { return isFinalBoss; }
    public void setFinalBoss(boolean finalBoss) { isFinalBoss = finalBoss; }

    public String getNextDungeonId() { return nextDungeonId; }
    public void setNextDungeonId(String nextDungeonId) { this.nextDungeonId = nextDungeonId; }

    public boolean isIncreaseDifficultyOnDeath() { return increaseDifficultyOnDeath; }
    public void setIncreaseDifficultyOnDeath(boolean increase) { this.increaseDifficultyOnDeath = increase; }

    // ===== ЗДОРОВЬЕ И АНИМАЦИЯ =====
    public void takeDamage(float amount) {
        if (!isAlive) return;
        health -= amount;
        System.out.println("[Monster] " + name + " took " + amount + " damage, HP: " + health + "/" + maxHealth);
        updateHealthBar();
        if (health <= 0) {
            health = 0;
            isAlive = false;
            onDeath();
        } else {
            playAnimation("GetHit");
        }
    }

    protected void onDeath() {
        // Анимация смерти и удаление модели
        if (animComposer != null) {
            Action dieAction = animComposer.makeAction("Die");
            Tween doneTween = Tweens.callMethod(this, "removeModel");
            Action sequence = animComposer.actionSequence("die_sequence", dieAction, doneTween);
            animComposer.setCurrentAction("die_sequence");
        } else {
            removeModel();
        }

        if (healthBarNode != null) {
            healthBarNode.setCullHint(Spatial.CullHint.Always);
        }

        // Дроп предметов
        if (lootTable != null && dropManager != null) {
            int difficulty = playerManager != null ? playerManager.getCurrentDifficulty() : 1;
            List<Item> items = lootTable.rollForLoot(difficulty);
            if (!items.isEmpty()) {
                dropManager.spawnDrops(currentPosition, items);
            }
        }

        // ===== ЛОГИКА БОССА: СМЕНА ДАНЖА =====
        if ((isBoss || isFinalBoss) && nextDungeonId != null && worldManager != null) {
            worldManager.changeDungeon(nextDungeonId, increaseDifficultyOnDeath);
        }
    }

    public void removeModel() {
        if (modelNode != null && modelNode.getParent() != null) {
            modelNode.getParent().detachChild(modelNode);
            modelNode = null;
        }
    }

    // ===== ПОЛОСКА ЗДОРОВЬЯ =====
    private void createHealthBar() {
        if (app == null) {
            System.err.println("[Monster] app is null, cannot create health bar. Call Monster.setApp() first!");
            return;
        }
        healthBarNode = new Node("HealthBarNode");
        healthBarNode.setQueueBucket(RenderQueue.Bucket.Transparent);

        float barWidth = 1.2f;
        float barHeight = 0.15f;
        float yOffset = 1.2f;

        Quad bgQuad = new Quad(barWidth, barHeight);
        hpBarBackground = new Geometry("HPBarBg", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.8f));
        hpBarBackground.setMaterial(bgMat);
        hpBarBackground.setLocalTranslation(-barWidth/2, yOffset, 0);
        healthBarNode.attachChild(hpBarBackground);

        Quad fgQuad = new Quad(barWidth - 0.04f, barHeight - 0.04f);
        hpBarForeground = new Geometry("HPBarFg", fgQuad);
        Material fgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fgMat.setColor("Color", ColorRGBA.Green);
        hpBarForeground.setMaterial(fgMat);
        hpBarForeground.setLocalTranslation(-(barWidth - 0.04f)/2, yOffset + 0.02f, 0.01f);
        healthBarNode.attachChild(hpBarForeground);

        BillboardControl billboard = new BillboardControl();
        healthBarNode.addControl(billboard);
        modelNode.attachChild(healthBarNode);
    }

    public void updateHealthBar() {
        if (hpBarForeground == null) return;
        float percent = Math.max(0, health / maxHealth);
        float barWidth = 1.2f - 0.04f;
        float newWidth = barWidth * percent;
        Quad quad = (Quad) hpBarForeground.getMesh();
        quad.updateGeometry(newWidth, 0.15f - 0.04f);
        float yOffset = 1.2f;
        hpBarForeground.setLocalTranslation(-barWidth/2 + 0.02f, yOffset + 0.02f, 0.01f);
        Material mat = hpBarForeground.getMaterial();
        if (percent > 0.5f) mat.setColor("Color", ColorRGBA.Green);
        else if (percent > 0.25f) mat.setColor("Color", ColorRGBA.Yellow);
        else mat.setColor("Color", ColorRGBA.Red);
    }

    // ===== АНИМАЦИИ =====
    private AnimComposer findAnimComposer(Spatial spatial) {
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                AnimComposer found = findAnimComposer(child);
                if (found != null) return found;
            }
        }
        return spatial.getControl(AnimComposer.class);
    }

    public void playAnimation(String animName) {
        if (animComposer != null) {
            animComposer.setCurrentAction(animName);
        }
    }

    // ===== ОБНОВЛЕНИЕ =====
    public void update(float tpf) {
        if (!isAlive) {
            if (deathTimer > 0) {
                deathTimer -= tpf;
                if (deathTimer <= 0) {
                    removeModel();
                }
            }
            return;
        }
        if (ai != null) {
            ai.update(tpf);
        }
    }
}