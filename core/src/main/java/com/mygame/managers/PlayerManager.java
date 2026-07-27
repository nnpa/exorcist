package com.mygame.managers;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.mygame.Main;
import com.mygame.managers.GameManager.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerManager {

    private SimpleApplication app;
    private Node playerNode;

    private AnimComposer animComposer;
    private SkinningControl skinningControl;

    // Базовые характеристики
    private String playerName = "Exorcist";
    private int baseLevel = 1;
    private int baseHealth = 100;
    private int baseMaxHealth = 100;
    private int baseMana = 50;
    private int baseMaxMana = 50;
    private int experience = 0;
    private int gold = 100;

    private int healthPotions = 0;
    private int manaPotions = 0;

    private Map<String, Float> statBonuses = new HashMap<>();

    private int finalMaxHealth;
    private int finalHealth;
    private int finalMaxMana;
    private int finalMana;
    private float physicalDefense = 0;
    private float magicalDefense = 0;
    private float blockChance = 0;
    private float critChance = 0;
    private float critDamage = 0;
    private float attackSpeed = 1.0f;
    private float baseDamage = 10f;

    private boolean isAlive = true;
    private boolean isMoving = false;
    private boolean isAttacking = false;
    private String currentAnimation = "";

    private Vector3f position = new Vector3f(0, 0, 0);
    private Vector3f targetPosition = null;
    private float speed = 12f;
    private float arrivalThreshold = 0.2f;

    private Spatial currentTarget = null;
    private float attackRange = 2.0f;
    private float attackCooldown = 0.8f;
    private float attackTimer = 0f;

    private float skillAnimationTimer = 0f;
    private static final float SKILL_ANIMATION_DURATION = 1.2f;

    private WorldManager worldManager;
    private DropManager dropManager;
    private TalentManager talentManager;

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_WALK = "Walk";
    private static final String ANIM_RUN = "Run";
    private static final String ANIM_ATTACK = "Attack";
    private static final String ANIM_BLOCK = "Block";
    private static final String ANIM_SPIN = "SpinAttack";
    private static final String ANIM_KICK = "Kick";
    private static final String ANIM_HEAL = "Heal";
    private static final String ANIM_DIE = "Die";
    private static final String ANIM_HIT = "GetHit";

    // ===== НАСТРОЙКИ ПЕРСОНАЖА =====
    private static final float MODEL_SCALE = 2.5f;
    private static final float PLAYER_HEIGHT_ABOVE_GROUND = 1.4f;

    public PlayerManager(SimpleApplication app) {
        this.app = app;
        playerNode = new Node("PlayerNode");
        playerNode.setLocalTranslation(position);
    }

    public void initialize() {
        System.out.println("[PlayerManager] Initializing...");
        loadPlayerModel();
        loadPlayerData();
        attachToScene();

        talentManager = new TalentManager(this);
        talentManager.addPointsForLevel(baseLevel);
        recalculateStats();

        healthPotions = 3;
        manaPotions = 3;
    }

 private void loadPlayerModel() {
    try {
        Spatial model = app.getAssetManager().loadModel("Models/Player/player.gltf");
        if (model == null) {
            System.err.println("[PlayerManager] Model not found. Creating placeholder.");
            createPlaceholderModel();
            return;
        }

        // ===== ПОВОРОТ МОДЕЛИ НА -90 ГРАДУСОВ ВОКРУГ Y =====
        // Это исправляет ориентацию: модель будет смотреть по оси Z (вперед)
        model.rotate(0, -FastMath.HALF_PI, 0);

        model.scale(MODEL_SCALE);
        model.updateModelBound();
        BoundingBox bb = (BoundingBox) model.getWorldBound();
        float bottomY = bb.getCenter().y - bb.getYExtent();
        float offsetY = PLAYER_HEIGHT_ABOVE_GROUND - bottomY;
        model.move(0, offsetY, 0);

        playerNode.detachAllChildren();
        playerNode.attachChild(model);
        playerNode.setLocalTranslation(position);

        animComposer = findAnimComposer(model);
        if (animComposer != null) {
            Set<String> clips = animComposer.getAnimClipsNames();
            System.out.println("[PlayerManager] Animations found:");
            for (String clip : clips) {
                System.out.println("  - " + clip);
            }
            if (clips.contains(ANIM_IDLE)) {
                animComposer.setCurrentAction(ANIM_IDLE);
                currentAnimation = ANIM_IDLE;
            } else if (!clips.isEmpty()) {
                String first = clips.iterator().next();
                animComposer.setCurrentAction(first);
                currentAnimation = first;
            }
        }
        skinningControl = findSkinningControl(model);
    } catch (Exception e) {
        System.err.println("[PlayerManager] Model load error: " + e.getMessage());
        e.printStackTrace();
        createPlaceholderModel();
    }
}

    private void createPlaceholderModel() {
        System.out.println("[PlayerManager] Creating placeholder model");
        Node modelNode = new Node("Placeholder");

        // Тело
        Geometry body = new Geometry("Body",
            new com.jme3.scene.shape.Box(0.4f, 0.6f, 0.3f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        body.setMaterial(mat);
        body.move(0, 0.6f, 0);
        modelNode.attachChild(body);

        // Голова
        Geometry head = new Geometry("Head",
            new com.jme3.scene.shape.Sphere(8, 8, 0.2f));
        Material headMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        headMat.setColor("Color", new ColorRGBA(1f, 0.8f, 0.6f, 1f));
        head.setMaterial(headMat);
        head.move(0, 1.2f, 0);
        modelNode.attachChild(head);

        // Ноги
        Geometry leftLeg = new Geometry("LeftLeg",
            new com.jme3.scene.shape.Box(0.12f, 0.4f, 0.12f));
        Material legMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        legMat.setColor("Color", ColorRGBA.DarkGray);
        leftLeg.setMaterial(legMat);
        leftLeg.move(-0.15f, 0.2f, 0);
        modelNode.attachChild(leftLeg);

        Geometry rightLeg = new Geometry("RightLeg",
            new com.jme3.scene.shape.Box(0.12f, 0.4f, 0.12f));
        rightLeg.setMaterial(legMat);
        rightLeg.move(0.15f, 0.2f, 0);
        modelNode.attachChild(rightLeg);

        modelNode.scale(MODEL_SCALE);
        modelNode.move(0, PLAYER_HEIGHT_ABOVE_GROUND, 0);
        playerNode.attachChild(modelNode);
        playerNode.setLocalTranslation(position);
    }

    private AnimComposer findAnimComposer(Spatial spatial) {
        AnimComposer c = spatial.getControl(AnimComposer.class);
        if (c != null) return c;
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                AnimComposer found = findAnimComposer(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private SkinningControl findSkinningControl(Spatial spatial) {
        SkinningControl c = spatial.getControl(SkinningControl.class);
        if (c != null) return c;
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                SkinningControl found = findSkinningControl(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    public void attachToScene() {
        if (playerNode != null && !app.getRootNode().hasChild(playerNode)) {
            app.getRootNode().attachChild(playerNode);
        }
    }

    public void setWorldManager(WorldManager wm) {
        this.worldManager = wm;
    }

    public void setDropManager(DropManager dm) {
        this.dropManager = dm;
    }

    // ===== ДВИЖЕНИЕ =====
    public void moveTo(Vector3f target) {
        if (playerNode == null || target == null) return;
        if (currentTarget != null) {
            currentTarget = null;
            isAttacking = false;
        }
        this.targetPosition = new Vector3f(target.x, 0, target.z);
        setMoving(true);
        lookAt(targetPosition);
    }

    // ===== ПОВОРОТ =====

public void lookAt(Vector3f target) {
    if (playerNode != null) {
        Vector3f direction = new Vector3f(target.x - position.x, 0, target.z - position.z);
        if (direction.length() > 0.01f) {
            direction.normalizeLocal();
            // Создаём кватернион, смотрящий в направлении direction
            Quaternion rot = new Quaternion();
            rot.lookAt(direction, Vector3f.UNIT_Y);
            // Применяем дополнительный поворот, если модель всё ещё смотрит не туда
            // rot.multLocal(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));
            playerNode.setLocalRotation(rot);
        }
    }
}

    // ---------- АТАКА ----------
    public void attackTarget(Spatial target) {
        if (target == null) return;
        this.currentTarget = target;
        this.isAttacking = true;
        Vector3f targetPos = target.getWorldTranslation();
        this.targetPosition = new Vector3f(targetPos.x, 0, targetPos.z);
        setMoving(true);
        lookAt(targetPos);
        attackTimer = 0;
        skillAnimationTimer = 0;
    }

    private void performAttack() {
        if (currentTarget == null) return;
        lookAt(currentTarget.getWorldTranslation());
        float damage = baseDamage + getBonusStat("base_damage");
        dealDamageToTarget(damage);
        playAnimation(ANIM_ATTACK);
        skillAnimationTimer = 0.6f;
        attackTimer = attackCooldown / (1 + getBonusStat("attack_speed") / 100f);

        if (currentTarget instanceof Geometry) {
            Geometry geom = (Geometry) currentTarget;
            WorldManager.MonsterData md = worldManager.getMonsterByGeometry(geom);
            if (md != null && !md.isDead) {
                md.hp -= (int) damage;
                if (md.hp <= 0) {
                    md.isDead = true;
                    Vector3f pos = geom.getWorldTranslation();
                    List<InventoryManager.Item> items = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        items.add(generateRandomItem());
                    }
                    if (dropManager != null) {
                        dropManager.spawnDrops(pos, items);
                    }
                    geom.setCullHint(Node.CullHint.Always);
                    currentTarget = null;
                    isAttacking = false;
                    targetPosition = null;
                    setMoving(false);
                }
            }
        }
    }

    private InventoryManager.Item generateRandomItem() {
        String[] names = {"Health Potion", "Mana Potion", "Iron Helmet", "Leather Boots", "Steel Sword"};
        String[] types = {"Consumable", "Consumable", "Helmet", "Boots", "Weapon"};
        ColorRGBA[] colors = {ColorRGBA.Green, ColorRGBA.Blue, ColorRGBA.White, ColorRGBA.White, ColorRGBA.White};
        int idx = (int)(Math.random() * names.length);
        return new InventoryManager.Item(names[idx], types[idx], 1, colors[idx], "Random drop");
    }

    private void dealDamageToTarget(float amount) {
        if (currentTarget == null) return;
        System.out.println("[Player] Damage dealt: " + amount + " to " + currentTarget.getName());
        if (currentTarget instanceof Geometry) {
            Geometry geom = (Geometry) currentTarget;
            Material mat = geom.getMaterial();
            if (mat != null) {
                mat.setColor("Color", ColorRGBA.Orange);
            }
        }
    }

    // ---------- СКИЛЛЫ ----------
    public void castSkill(String skillName) {
        if (animComposer == null) return;
        System.out.println("[Player] Using skill: " + skillName);

        switch (skillName) {
            case "Heal":
                playAnimation(ANIM_HEAL);
                float healPower = 20 + getBonusStat("heal_power");
                heal((int) healPower);
                skillAnimationTimer = SKILL_ANIMATION_DURATION;
                break;

            case "ShieldBash":
                if (currentTarget != null) {
                    lookAt(currentTarget.getWorldTranslation());
                    playAnimation(ANIM_BLOCK);
                    float bashDmg = 15 + getBonusStat("shield_bash_damage");
                    dealDamageToTarget(bashDmg);
                    skillAnimationTimer = SKILL_ANIMATION_DURATION;
                }
                break;

            case "Whirlwind":
                if (currentTarget != null) {
                    lookAt(currentTarget.getWorldTranslation());
                    playAnimation(ANIM_SPIN);
                    float wwDmg = 25 + getBonusStat("base_damage");
                    dealDamageToTarget(wwDmg);
                    skillAnimationTimer = SKILL_ANIMATION_DURATION;
                }
                break;

            case "Kick":
                if (currentTarget != null) {
                    lookAt(currentTarget.getWorldTranslation());
                    playAnimation(ANIM_KICK);
                    float kickDmg = 10 + getBonusStat("base_damage");
                    dealDamageToTarget(kickDmg);
                    skillAnimationTimer = SKILL_ANIMATION_DURATION;
                }
                break;

            default:
                System.out.println("[Player] Unknown skill: " + skillName);
                break;
        }
    }

    // ---------- ОБНОВЛЕНИЕ ----------
    public void update(float tpf) {
        if (skillAnimationTimer > 0) {
            skillAnimationTimer -= tpf;
            return;
        }

        // Бой
        if (currentTarget != null) {
            Vector3f targetPos = currentTarget.getWorldTranslation();
            Vector3f currentPos = playerNode.getWorldTranslation();
            float dist = targetPos.distance(currentPos);

            if (dist <= attackRange) {
                if (isMoving) setMoving(false);
                targetPosition = null;
                isAttacking = true;
                attackTimer -= tpf;
                if (attackTimer <= 0) {
                    performAttack();
                    attackTimer = attackCooldown / (1 + getBonusStat("attack_speed") / 100f);
                }
            } else {
                // Движение к цели
                Vector3f targetPosFlat = new Vector3f(targetPos.x, 0, targetPos.z);
                Vector3f currentPosFlat = new Vector3f(currentPos.x, 0, currentPos.z);
                Vector3f dir = targetPosFlat.subtract(currentPosFlat).normalizeLocal();
                float step = speed * tpf;
                Vector3f newPos = currentPosFlat.add(dir.mult(step));
                if (newPos.distance(targetPosFlat) < 0.1f || newPos.distance(currentPosFlat) > targetPosFlat.distance(currentPosFlat)) {
                    playerNode.setLocalTranslation(targetPosFlat.x, currentPos.y, targetPosFlat.z);
                } else {
                    playerNode.setLocalTranslation(newPos.x, currentPos.y, newPos.z);
                }
                position.set(playerNode.getLocalTranslation());
                setMoving(true);
                lookAt(targetPos);
                isAttacking = false;
                attackTimer = 0;
            }
        } else {
            // Движение по земле
            if (targetPosition != null) {
                Vector3f currentPos = playerNode.getLocalTranslation();
                float dx = targetPosition.x - currentPos.x;
                float dz = targetPosition.z - currentPos.z;
                float dist = (float) Math.sqrt(dx * dx + dz * dz);

                if (dist < arrivalThreshold) {
                    playerNode.setLocalTranslation(targetPosition.x, currentPos.y, targetPosition.z);
                    position.set(targetPosition.x, currentPos.y, targetPosition.z);
                    targetPosition = null;
                    setMoving(false);
                } else {
                    Vector3f direction = new Vector3f(dx, 0, dz).normalizeLocal();
                    float step = speed * tpf;
                    if (step > dist) {
                        playerNode.setLocalTranslation(targetPosition.x, currentPos.y, targetPosition.z);
                        position.set(targetPosition.x, currentPos.y, targetPosition.z);
                        targetPosition = null;
                        setMoving(false);
                    } else {
                        Vector3f newPos = currentPos.add(direction.mult(step));
                        playerNode.setLocalTranslation(newPos);
                        position.set(newPos);
                        lookAt(targetPosition);
                        setMoving(true);
                    }
                }
            } else {
                if (isMoving) setMoving(false);
            }
        }

        // Анимации
        if (skillAnimationTimer > 0) return;
        if (isMoving) {
            playAnimation(ANIM_WALK);
        } else if (isAttacking && currentTarget != null) {
            // атака уже запущена
        } else {
            playAnimation(ANIM_IDLE);
        }
    }

    public void playAnimation(String animationName) {
        if (animComposer == null) return;
        if (animationName.equals(currentAnimation)) return;
        animComposer.setCurrentAction(animationName);
        currentAnimation = animationName;
    }

    public void setMoving(boolean moving) {
        this.isMoving = moving;
    }

    // ---------- ХАРАКТЕРИСТИКИ ----------
    public void applyTalentBonuses(Map<String, Float> bonuses) {
        statBonuses.clear();
        statBonuses.putAll(bonuses);
        recalculateStats();
    }

    private void recalculateStats() {
        float healthBonusPercent = statBonuses.getOrDefault("max_health", 0f);
        finalMaxHealth = (int)(baseMaxHealth * (1 + healthBonusPercent / 100f));
        if (finalHealth > finalMaxHealth) finalHealth = finalMaxHealth;
        else if (finalHealth <= 0) finalHealth = finalMaxHealth;

        float manaBonusPercent = statBonuses.getOrDefault("max_mana", 0f);
        finalMaxMana = (int)(baseMaxMana * (1 + manaBonusPercent / 100f));
        if (finalMana > finalMaxMana) finalMana = finalMaxMana;
        else if (finalMana <= 0) finalMana = finalMaxMana;

        physicalDefense = statBonuses.getOrDefault("physical_defense", 0f);
        magicalDefense = statBonuses.getOrDefault("magical_defense", 0f);
        blockChance = statBonuses.getOrDefault("block_chance", 0f);
        critChance = statBonuses.getOrDefault("crit_chance", 0f);
        critDamage = statBonuses.getOrDefault("crit_damage", 0f);
        attackSpeed = 1f + statBonuses.getOrDefault("attack_speed", 0f) / 100f;
        baseDamage = 10f + statBonuses.getOrDefault("base_damage", 0f);

        System.out.println("[Player] Stats recalculated. HP: " + finalMaxHealth + ", Damage: " + baseDamage);
    }

    private float getBonusStat(String key) {
        return statBonuses.getOrDefault(key, 0f);
    }

    // ---------- УРОВНИ И ОПЫТ ----------
    public void addExperience(int exp) {
        experience += exp;
        int newLevel = baseLevel + experience / 1000;
        if (newLevel > baseLevel) {
            baseLevel = newLevel;
            talentManager.addPointsForLevel(baseLevel);
            baseMaxHealth += 10;
            baseMaxMana += 5;
            recalculateStats();
            System.out.println("[Player] Level up! Level: " + baseLevel);
        }
    }

    // ---------- ЗДОРОВЬЕ И МАНА ----------
    public void takeDamage(int damage) {
        if (!isAlive) return;
        float reduction = 1 - Math.min(0.8f, physicalDefense / 100f);
        int actualDamage = (int)(damage * reduction);
        finalHealth -= actualDamage;
        if (finalHealth <= 0) {
            finalHealth = 0;
            isAlive = false;
            playAnimation(ANIM_DIE);
            System.out.println("[Player] Player died!");
        } else {
            playAnimation(ANIM_HIT);
        }
        System.out.println("[Player] Taken damage: " + actualDamage + ", HP: " + finalHealth + "/" + finalMaxHealth);
    }

    public void heal(int amount) {
        if (!isAlive) return;
        float incomingBonus = 1 + getBonusStat("incoming_heal") / 100f;
        int healAmount = (int)(amount * incomingBonus);
        finalHealth = Math.min(finalHealth + healAmount, finalMaxHealth);
        System.out.println("[Player] Healed: " + healAmount + ", HP: " + finalHealth + "/" + finalMaxHealth);
    }

    public void useMana(int amount) {
        if (finalMana < amount) return;
        finalMana -= amount;
    }

    public int getHealthPotions() { return healthPotions; }
    public int getManaPotions() { return manaPotions; }
    public void setHealthPotions(int count) { this.healthPotions = Math.max(0, count); }
    public void setManaPotions(int count) { this.manaPotions = Math.max(0, count); }
    public void addHealthPotions(int count) { this.healthPotions += count; }
    public void addManaPotions(int count) { this.manaPotions += count; }

    public void useHealthPotion() {
        if (healthPotions > 0) {
            healthPotions--;
            heal(50);
            System.out.println("[Player] Used health potion. Remaining: " + healthPotions);
        } else {
            System.out.println("[Player] No health potions!");
        }
    }

    public void useManaPotion() {
        if (manaPotions > 0) {
            manaPotions--;
            useMana(30);
            System.out.println("[Player] Used mana potion. Remaining: " + manaPotions);
        } else {
            System.out.println("[Player] No mana potions!");
        }
    }

    private void loadPlayerData() {
        this.playerName = "Test Player";
        this.baseLevel = 1;
        this.baseHealth = 100;
        this.baseMaxHealth = 100;
        this.baseMana = 50;
        this.baseMaxMana = 50;
        this.finalHealth = baseMaxHealth;
        this.finalMana = baseMaxMana;
        this.gold = 100;
    }

    public void updatePlayerData(Map<String, Object> data) {
        if (data.containsKey("name")) playerName = (String) data.get("name");
        if (data.containsKey("level")) baseLevel = (int) data.get("level");
        if (data.containsKey("health")) baseHealth = (int) data.get("health");
        if (data.containsKey("maxHealth")) baseMaxHealth = (int) data.get("maxHealth");
        if (data.containsKey("mana")) baseMana = (int) data.get("mana");
        if (data.containsKey("maxMana")) baseMaxMana = (int) data.get("maxMana");
        if (data.containsKey("experience")) experience = (int) data.get("experience");
        if (data.containsKey("gold")) gold = (int) data.get("gold");
        recalculateStats();
    }

    public void attachToBone(String boneName, Spatial item) {
        if (skinningControl != null) {
            try {
                Node attachNode = skinningControl.getAttachmentsNode(boneName);
                if (attachNode != null) {
                    attachNode.attachChild(item);
                }
            } catch (Exception e) {
                System.err.println("[Player] Attach error: " + e.getMessage());
            }
        }
    }

    public void cleanup() {
        if (playerNode != null) {
            app.getRootNode().detachChild(playerNode);
        }
    }

    // ---------- ГЕТТЕРЫ И СЕТТЕРЫ ----------
    public Node getPlayerNode() { return playerNode; }
    public Vector3f getPosition() { return position; }
    public String getPlayerName() { return playerName; }
    public int getLevel() { return baseLevel; }
    public int getHealth() { return finalHealth; }
    public int getMaxHealth() { return finalMaxHealth; }
    public int getMana() { return finalMana; }
    public int getMaxMana() { return finalMaxMana; }
    public int getExperience() { return experience; }
    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = Math.max(0, gold); }
    public boolean isAlive() { return isAlive; }
    public AnimComposer getAnimComposer() { return animComposer; }
    public SkinningControl getSkinningControl() { return skinningControl; }
    public Spatial getCurrentTarget() { return currentTarget; }
    public TalentManager getTalentManager() { return talentManager; }

    public void setPlayerName(String name) { this.playerName = name; }
    public void setLevel(int level) { this.baseLevel = level; }
    public void setHealth(int hp) { this.finalHealth = Math.min(hp, finalMaxHealth); }
    public void setMaxHealth(int maxHp) { this.baseMaxHealth = maxHp; recalculateStats(); }
    public void setMana(int mana) { this.finalMana = Math.min(mana, finalMaxMana); }
    public void setMaxMana(int maxMana) { this.baseMaxMana = maxMana; recalculateStats(); }
    public void setExperience(int exp) { this.experience = exp; }
    public void setPosition(Vector3f pos) { this.position.set(pos); }
    public void setSpeed(float speed) { this.speed = speed; }
}