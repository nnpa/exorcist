package com.mygame.managers;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.mygame.Main;
import com.mygame.items.Item;
import com.mygame.items.ItemGenerator;
import com.mygame.monsters.Monster;

import java.util.*;

public class PlayerManager {
    private Vector3f lastDungeonPosition = new Vector3f(0f, 2.5f, 0f);
    public Vector3f getLastDungeonPosition() { return lastDungeonPosition; }
    public void setLastDungeonPosition(Vector3f pos) {
        if (pos != null) this.lastDungeonPosition.set(pos);
    }
    public int getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(int difficulty) { this.currentDifficulty = difficulty; }
    public String getCurrentDungeonId() { return currentDungeonId; }
    public void setCurrentDungeonId(String dungeonId) { this.currentDungeonId = dungeonId; }
    private int currentDifficulty = 1;
    private String currentDungeonId = "dungeon_1";

    private SimpleApplication app;
    private Node playerNode;
    private BetterCharacterControl characterControl;
    private AnimComposer animComposer;
    private SkinningControl skinningControl;

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

    private Vector3f position = new Vector3f(0f, 2.5f, 0f);
    private Vector3f targetPosition = null;
    private float arrivalThreshold = 0.3f;
    private boolean isMovingToTarget = false;

    private Spatial currentTarget = null;
    private float attackRange = 1.6f;
    private float attackCooldown = 0.8f;
    private float attackTimer = 0f;

    // ===== УПРАВЛЕНИЕ АНИМАЦИЕЙ (БЕЗ ТАЙМЕРА) =====
    private boolean skillAnimationPlaying = false;
    private String skillAnimationName = null;
    private com.jme3.anim.tween.action.Action activeOneShotAction = null;

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

    private static final float MODEL_SCALE = 2.5f;
    private static final float PLAYER_HEIGHT_ABOVE_GROUND = 1.4f;

    private Vector3f smoothPosition = new Vector3f();
    private float interpolationSpeed = 0.25f;

    private float deathTimer = 0f;
    private boolean isRespawning = false;

    private int killsCounter = 5;
    private static final int KILLS_PER_LEVEL = 6;

    private UIManager uiManager;
    private NetworkManager networkManager;

    private AudioNode footstepNode;
    private float footstepTimer = 0f;
    private static final float FOOTSTEP_INTERVAL = 0.45f;

    public PlayerManager(SimpleApplication app) {
        this.app = app;
        playerNode = new Node("PlayerNode");
        playerNode.setLocalTranslation(position);
        smoothPosition.set(position);
    }

    public void initialize() {
        System.out.println("[PlayerManager] Initializing...");
        loadPlayerModel();
        loadPlayerData();
        attachToScene();
        createPhysicsBody();
        recalculateStats();
        healthPotions = 3;
        manaPotions = 3;
        this.currentDifficulty = 1;
        this.currentDungeonId = "dungeon_1";

        footstepNode = SoundManager.getSoundNode(SoundManager.SOUND_FOOTSTEP);
        if (footstepNode == null) {
            System.err.println("[PlayerManager] Footstep sound not loaded!");
        } else {
            footstepNode.setLooping(false);
        }
    }

    public void setTalentManager(TalentManager tm) {
        this.talentManager = tm;
        if (tm != null) {
            tm.addPointsForLevel(baseLevel);
            recalculateStats();
        }
    }
    public TalentManager getTalentManager() { return talentManager; }

    private void createPhysicsBody() {
        float radius = 0.4f;
        float height = 1.8f;
        float mass = 150f;
        characterControl = new BetterCharacterControl(radius, height, mass);
        characterControl.setGravity(new Vector3f(0, -9.81f * 3, 0));
        characterControl.warp(new Vector3f(0f, 2.5f, 0f));
        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.setPhysicsDamping(0.95f);
        characterControl.getRigidBody().setDamping(0.15f, 0.95f);
        playerNode.addControl(characterControl);
        System.out.println("[PlayerManager] BetterCharacterControl created with mass " + mass);
    }

    public void setPhysicsSpace(PhysicsSpace space) {
        if (characterControl != null && space != null) {
            space.add(characterControl);
            System.out.println("[PlayerManager] BetterCharacterControl added to physics space.");
        }
    }

    private void loadPlayerModel() {
        try {
            Spatial model = app.getAssetManager().loadModel("Models/Player/player.gltf");
            if (model == null) {
                System.err.println("[PlayerManager] Model not found. Creating placeholder.");
                createPlaceholderModel();
                return;
            }
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
                    playBaseAnimation(ANIM_IDLE);
                } else if (!clips.isEmpty()) {
                    String first = clips.iterator().next();
                    changeAnimation(first, true);
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
        Geometry body = new Geometry("Body", new Box(0.4f, 0.6f, 0.3f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        body.setMaterial(mat);
        body.move(0, 0.6f, 0);
        modelNode.attachChild(body);
        Geometry head = new Geometry("Head", new Sphere(8, 8, 0.2f));
        Material headMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        headMat.setColor("Color", new ColorRGBA(1f, 0.8f, 0.6f, 1f));
        head.setMaterial(headMat);
        head.move(0, 1.2f, 0);
        modelNode.attachChild(head);
        Geometry leftLeg = new Geometry("LeftLeg", new Box(0.12f, 0.4f, 0.12f));
        Material legMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        legMat.setColor("Color", ColorRGBA.DarkGray);
        leftLeg.setMaterial(legMat);
        leftLeg.move(-0.15f, 0.2f, 0);
        modelNode.attachChild(leftLeg);
        Geometry rightLeg = new Geometry("RightLeg", new Box(0.12f, 0.4f, 0.12f));
        rightLeg.setMaterial(legMat);
        rightLeg.move(0.15f, 0.2f, 0);
        modelNode.attachChild(rightLeg);
        modelNode.scale(MODEL_SCALE);
        modelNode.move(0, PLAYER_HEIGHT_ABOVE_GROUND, 0);
        playerNode.attachChild(modelNode);
    }

    private AnimComposer findAnimComposer(Spatial spatial) {
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                AnimComposer found = findAnimComposer(child);
                if (found != null) return found;
            }
        }
        return spatial.getControl(AnimComposer.class);
    }

    private SkinningControl findSkinningControl(Spatial spatial) {
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                SkinningControl found = findSkinningControl(child);
                if (found != null) return found;
            }
        }
        return spatial.getControl(SkinningControl.class);
    }

    public void attachToScene() {
        if (playerNode != null && !app.getRootNode().hasChild(playerNode)) {
            app.getRootNode().attachChild(playerNode);
        }
    }

    public void setWorldManager(WorldManager wm) { this.worldManager = wm; }
    public void setDropManager(DropManager dm) { this.dropManager = dm; }

    public void moveTo(Vector3f target) {
        if (playerNode == null || target == null || characterControl == null) return;
        if (currentTarget != null) {
            currentTarget = null;
            isAttacking = false;
        }
        this.targetPosition = new Vector3f(target.x, 0, target.z);
        this.isMovingToTarget = true;
        setMoving(true);
        lookAt(target);
    }

    public void lookAt(Vector3f target) {
        if (playerNode == null || characterControl == null) return;
        Vector3f currentPos = playerNode.getWorldTranslation();
        Vector3f direction = new Vector3f(target.x - currentPos.x, 0, target.z - currentPos.z);
        if (direction.length() > 0.01f) {
            direction.normalizeLocal();
            characterControl.setViewDirection(direction);
        }
    }

    public void stopMoving() {
        if (characterControl != null) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
        }
        setMoving(false);
        targetPosition = null;
        isMovingToTarget = false;
        isAttacking = false;
        currentTarget = null;
        if (!skillAnimationPlaying) {
            playBaseAnimation(ANIM_IDLE);
        }
    }

public void attackTarget(Spatial target) {
    if (target == null) return;
    if (skillAnimationPlaying) return;

    currentTarget = target;
    isAttacking = true;

    if (characterControl != null) {
        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
    }
    setMoving(false);
    isMovingToTarget = false;
    targetPosition = null;
    lookAt(target.getWorldTranslation());

    // УБИРАЕМ сброс attackTimer
    // Теперь атака будет происходить только когда timer достигнет 0 в update()
    // Это обеспечит постоянную задержку между ударами
}

    // ===== АНИМАЦИИ (без actionSequence) =====
    private boolean hasAnimation(String animationName) {
        return animComposer != null && animationName != null && !animationName.isEmpty()
                && animComposer.getAnimClipsNames().contains(animationName);
    }

    private void playBaseAnimation(String animationName) {
        if (!isAlive || animComposer == null || animationName == null || animationName.isEmpty()) return;
        if (skillAnimationPlaying) return;
        if (!hasAnimation(animationName)) return;
        if (animationName.equals(currentAnimation) && animComposer.getCurrentAction(AnimComposer.DEFAULT_LAYER) != null) return;
        changeAnimation(animationName, true);
    }

    private boolean playSkillAnimation(String animationName) {
        if (animComposer == null || !hasAnimation(animationName) || !isAlive) return false;
        if (skillAnimationPlaying) return false;

        // Удаляем текущую анимацию
        animComposer.removeCurrentAction(AnimComposer.DEFAULT_LAYER);

        // Запускаем один раз (loop = false)
        com.jme3.anim.tween.action.Action action = animComposer.setCurrentAction(
                animationName,
                AnimComposer.DEFAULT_LAYER,
                false
        );

        if (action == null) return false;

        activeOneShotAction = action;
        skillAnimationName = animationName;
        skillAnimationPlaying = true;
        currentAnimation = animationName;

        System.out.println("[PlayerAnimation] Started one-shot: " + animationName);
        return true;
    }

    private boolean changeAnimation(String animationName, boolean loop) {
        if (animComposer == null || !hasAnimation(animationName)) return false;
        animComposer.removeCurrentAction(AnimComposer.DEFAULT_LAYER);
        try {
            animComposer.setCurrentAction(animationName, AnimComposer.DEFAULT_LAYER, loop);
            currentAnimation = animationName;
            return true;
        } catch (Exception e) {
            System.err.println("[PlayerAnimation] Cannot play '" + animationName + "': " + e.getMessage());
            return false;
        }
    }

    private boolean isActiveOneShot() {
        if (!skillAnimationPlaying || animComposer == null) return false;
        com.jme3.anim.tween.action.Action current = animComposer.getCurrentAction(AnimComposer.DEFAULT_LAYER);
        return current != null && current == activeOneShotAction;
    }

    private void finishSkillAnimation() {
        if (!skillAnimationPlaying) return;
        System.out.println("[PlayerAnimation] Finished one-shot: " + skillAnimationName);
        skillAnimationPlaying = false;
        skillAnimationName = null;
        activeOneShotAction = null;
        animComposer.removeCurrentAction(AnimComposer.DEFAULT_LAYER);
        currentAnimation = "";
        if (isAlive) {
            if (currentTarget != null) {
                // Если есть цель, остаёмся в Attack, но это будет обработано в update
            } else if (isMovingToTarget || isMoving) {
                playBaseAnimation(ANIM_WALK);
            } else {
                playBaseAnimation(ANIM_IDLE);
            }
        }
    }

    public void playAnimation(String animationName) {
        if (animationName == null || animationName.isEmpty()) return;
        if (ANIM_DIE.equals(animationName)) {
            changeAnimation(animationName, false);
            return;
        }
        if (ANIM_IDLE.equals(animationName) || ANIM_WALK.equals(animationName) || ANIM_RUN.equals(animationName)) {
            playBaseAnimation(animationName);
            return;
        }
        if (ANIM_ATTACK.equals(animationName) || ANIM_BLOCK.equals(animationName) ||
                ANIM_SPIN.equals(animationName) || ANIM_KICK.equals(animationName) ||
                ANIM_HEAL.equals(animationName)) {
            playSkillAnimation(animationName);
            return;
        }
        if (!skillAnimationPlaying) {
            changeAnimation(animationName, true);
        }
    }

    private void performAttack() {
        if (currentTarget == null) return;
        if (skillAnimationPlaying) return;

        lookAt(currentTarget.getWorldTranslation());
        float damage = baseDamage + getBonusStat("base_damage");
        playSkillAnimation(ANIM_ATTACK);
        attackTimer = attackCooldown / (1 + getBonusStat("attack_speed") / 100f);

        Monster monster = worldManager.getMonsterByModel(currentTarget);
        if (monster != null && monster.isAlive()) {
            SoundManager.playSound(SoundManager.SOUND_ATTACK_PLAYER);
            monster.takeDamage(damage);
            if (!monster.isAlive()) {
                onMonsterKilled();
                currentTarget = null;
                isAttacking = false;
                setMoving(false);
                if (characterControl != null) {
                    characterControl.setWalkDirection(Vector3f.ZERO);
                    characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
                }
                // Не форсируем Idle, анимация завершится сама
            }
            return;
        }

        if (currentTarget instanceof Geometry) {
            Geometry geom = (Geometry) currentTarget;
            WorldManager.MonsterData md = worldManager.getMonsterByGeometry(geom);
            if (md != null && !md.isDead) {
                md.hp -= (int) damage;
                if (md.hp <= 0) {
                    md.isDead = true;
                    Vector3f pos = geom.getWorldTranslation();
                    int difficulty = this.getCurrentDifficulty();
                    List<Item> items = ItemGenerator.generateDrop(baseLevel, 3, difficulty);
                    if (dropManager != null) {
                        dropManager.spawnDrops(pos, items);
                    }
                    geom.setCullHint(Node.CullHint.Always);
                    currentTarget = null;
                    isAttacking = false;
                    setMoving(false);
                    if (characterControl != null) {
                        characterControl.setWalkDirection(Vector3f.ZERO);
                        characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
                    }
                }
            }
        }
    }

    public void setMoving(boolean moving) {
        if (this.isMoving && !moving) {
            footstepTimer = 0f;
        }
        this.isMoving = moving;
    }

    public void update(float tpf) {
        // Шаги
        if (isMoving && isAlive) {
            footstepTimer += tpf;
            if (footstepTimer >= FOOTSTEP_INTERVAL) {
                footstepTimer = 0f;
                if (footstepNode != null) {
                    footstepNode.play();
                }
            }
        } else {
            if (footstepNode != null && footstepNode.getStatus() == AudioSource.Status.Playing) {
                footstepNode.stop();
            }
            footstepTimer = 0f;
        }

        if (!isAlive) {
            deathTimer += tpf;
            if (deathTimer >= 0.3f && !isRespawning) {
                respawnPlayer();
            }
            return;
        }

        // Проверка окончания одноразовой анимации
        if (skillAnimationPlaying) {
            if (!isActiveOneShot()) {
                finishSkillAnimation();
            }
        }

        // Интерполяция позиции
        if (characterControl != null) {
            Vector3f physPos = playerNode.getWorldTranslation();
            smoothPosition.interpolateLocal(physPos, 0.25f);
            playerNode.setLocalTranslation(smoothPosition);
            position.set(smoothPosition);
        }

        // Движение к точке
        if (currentTarget == null && isMovingToTarget && targetPosition != null && characterControl != null) {
            Vector3f currentPos = smoothPosition;
            float dist = currentPos.distance(targetPosition);

            if (dist < arrivalThreshold) {
                characterControl.setWalkDirection(Vector3f.ZERO);
                characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
                setMoving(false);
                isMovingToTarget = false;
                targetPosition = null;
                if (!skillAnimationPlaying) {
                    playBaseAnimation(ANIM_IDLE);
                }
                return;
            }

            Vector3f dir = new Vector3f(
                targetPosition.x - currentPos.x,
                0,
                targetPosition.z - currentPos.z
            );
            if (dir.length() > 0.01f) {
                dir.normalizeLocal();
                characterControl.setWalkDirection(dir.mult(4f));
                setMoving(true);
                if (!skillAnimationPlaying) {
                    playBaseAnimation(ANIM_WALK);
                }
            } else {
                characterControl.setWalkDirection(Vector3f.ZERO);
                characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
                setMoving(false);
                isMovingToTarget = false;
                targetPosition = null;
                if (!skillAnimationPlaying) {
                    playBaseAnimation(ANIM_IDLE);
                }
            }
            return;
        }

        // Бой
        if (currentTarget != null) {
            Vector3f targetPos = currentTarget.getWorldTranslation();
            Vector3f currentPos = characterControl != null ? characterControl.getRigidBody().getPhysicsLocation() : position;
            float dist = targetPos.distance(currentPos);

            if (dist <= attackRange) {
                if (isMoving) {
                    setMoving(false);
                    if (characterControl != null) {
                        characterControl.setWalkDirection(Vector3f.ZERO);
                        characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
                    }
                }
                isAttacking = true;
                attackTimer -= tpf;
                if (attackTimer <= 0) {
                    performAttack();
                    attackTimer = attackCooldown / (1 + getBonusStat("attack_speed") / 100f);
                }
            } else {
                Vector3f dir = new Vector3f(
                    targetPos.x - currentPos.x,
                    0,
                    targetPos.z - currentPos.z
                );
                if (dir.length() > 0.01f) {
                    dir.normalizeLocal();
                    characterControl.setWalkDirection(dir.mult(4f));
                    setMoving(true);
                    if (!skillAnimationPlaying) {
                        playBaseAnimation(ANIM_WALK);
                    }
                }
                isAttacking = false;
                attackTimer = 0;
            }
            return;
        }

        // Idle
        if (!isMoving && !isAttacking && !skillAnimationPlaying) {
            playBaseAnimation(ANIM_IDLE);
        }
    }

    // ===== СКИЛЛЫ =====
    public void castSkill(String skillName) {
        if (animComposer == null) return;
        if (skillAnimationPlaying) {
            System.out.println("[Player] Skill already playing: " + skillAnimationName);
            return;
        }

        System.out.println("[Player] Using skill: " + skillName);

        switch (skillName) {
            case "Heal":
                if (finalMana < 10) {
                    System.out.println("[Player] Not enough mana!");
                    return;
                }
                if (!playSkillAnimation(ANIM_HEAL)) return;
                SoundManager.playSound(SoundManager.SOUND_HEAL);
                useMana(10);
                float healPower = 20 + getBonusStat("heal_power");
                heal((int) healPower);
                break;
            case "ShieldBash":
                if (finalMana < 15) {
                    System.out.println("[Player] Not enough mana!");
                    return;
                }
                if (currentTarget != null) {
                    if (!hasAnimation(ANIM_BLOCK)) return;
                    lookAt(currentTarget.getWorldTranslation());
                    if (!playSkillAnimation(ANIM_BLOCK)) return;
                    SoundManager.playSound(SoundManager.SOUND_SHIELD_BASH);
                    useMana(15);
                    float bashDmg = 15 + getBonusStat("shield_bash_damage");
                    dealDamageToTarget(bashDmg);
                }
                break;
            case "Whirlwind":
                if (finalMana < 20) {
                    System.out.println("[Player] Not enough mana!");
                    return;
                }
                if (currentTarget != null) {
                    if (!hasAnimation(ANIM_SPIN)) return;
                    lookAt(currentTarget.getWorldTranslation());
                    if (!playSkillAnimation(ANIM_SPIN)) return;
                    SoundManager.playSound(SoundManager.SOUND_WHIRLWIND);
                    useMana(20);
                    float wwDmg = 25 + getBonusStat("base_damage");
                    dealDamageToTarget(wwDmg);
                }
                break;
            case "Kick":
                if (finalMana < 10) {
                    System.out.println("[Player] Not enough mana!");
                    return;
                }
                if (currentTarget != null) {
                    if (!hasAnimation(ANIM_KICK)) return;
                    lookAt(currentTarget.getWorldTranslation());
                    if (!playSkillAnimation(ANIM_KICK)) return;
                    SoundManager.playSound(SoundManager.SOUND_KICK);
                    useMana(10);
                    float kickDmg = 10 + getBonusStat("base_damage");
                    dealDamageToTarget(kickDmg);
                }
                break;
            default:
                System.out.println("[Player] Unknown skill: " + skillName);
                break;
        }
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

    private void respawnPlayer() {
        if (isRespawning) return;
        isRespawning = true;
        SoundManager.playSound(SoundManager.SOUND_PLAYER_DIE);
        System.out.println("[PlayerManager] Respawning player...");
        skillAnimationPlaying = false;
        skillAnimationName = null;
        activeOneShotAction = null;
        animComposer.removeCurrentAction(AnimComposer.DEFAULT_LAYER);
        setHealth(getMaxHealth());
        setMana(getMaxMana());
        setAlive(true);
        setMoving(false);
        targetPosition = null;
        isMovingToTarget = false;
        currentTarget = null;
        isAttacking = false;
        if (characterControl != null) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
        }
        if (worldManager != null) {
            worldManager.returnToCity();
        }
        currentAnimation = "";
        playBaseAnimation(ANIM_IDLE);
        deathTimer = 0f;
        isRespawning = false;
        System.out.println("[PlayerManager] Respawn complete.");
    }

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

    public void takeDamage(int damage) {
        if (!isAlive) return;
        float reduction = 1 - Math.min(0.8f, physicalDefense / 100f);
        int actualDamage = (int)(damage * reduction);
        finalHealth -= actualDamage;
        if (finalHealth <= 0) {
            finalHealth = 0;
            isAlive = false;
            skillAnimationPlaying = false;
            skillAnimationName = null;
            activeOneShotAction = null;
            animComposer.removeCurrentAction(AnimComposer.DEFAULT_LAYER);
            currentAnimation = "";
            changeAnimation(ANIM_DIE, false);
            System.out.println("[Player] Player died!");
        }
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
        System.out.println("[Player] Mana used: " + amount + ", remaining: " + finalMana);
        if (app instanceof Main) {
            UIManager ui = ((Main) app).getUIManager();
            if (ui != null) ui.updatePlayerStats();
        }
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
            if (app instanceof Main) {
                UIManager ui = ((Main) app).getUIManager();
                if (ui != null) {
                    ui.updatePotionCounts();
                    ui.updatePlayerStats();
                }
            }
        } else {
            System.out.println("[Player] No health potions!");
        }
    }

    public void useManaPotion() {
        if (manaPotions > 0) {
            manaPotions--;
            finalMana = Math.min(finalMana + 30, finalMaxMana);
            System.out.println("[Player] Used mana potion. Remaining: " + manaPotions + ", Mana: " + finalMana + "/" + finalMaxMana);
            if (app instanceof Main) {
                UIManager ui = ((Main) app).getUIManager();
                if (ui != null) {
                    ui.updatePotionCounts();
                    ui.updatePlayerStats();
                }
            }
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

    public void onMonsterKilled() {
        if (baseLevel >= 50) {
            System.out.println("[PlayerManager] Max level reached, kills ignored.");
            return;
        }
        killsCounter++;
        System.out.println("[PlayerManager] Kills: " + killsCounter + "/" + KILLS_PER_LEVEL);
        if (killsCounter >= KILLS_PER_LEVEL) {
            killsCounter = 0;
            if (networkManager != null) {
                SoundManager.playSound(SoundManager.SOUND_LEVEL_UP);
                networkManager.levelUp().thenAccept(response -> {
                    app.enqueue(() -> {
                        if (response != null && uiManager != null) {
                            uiManager.applyCharacterData(response);
                            if (uiManager.getTalentManager() != null) {
                                networkManager.loadTalents().thenAccept(talentData -> {
                                    app.enqueue(() -> {
                                        if (talentData != null && uiManager.getTalentManager() != null) {
                                            Map<String, Integer> talents = (Map<String, Integer>) talentData.get("talents");
                                            int points = (int) talentData.get("availablePoints");
                                            uiManager.getTalentManager().loadFromServer(talents, points);
                                            if (uiManager.getTalentWindow() != null) {
                                                uiManager.getTalentWindow().updateUI();
                                            }
                                        }
                                    });
                                });
                            }
                        }
                    });
                });
            }
        }
    }

    public void resetKillsCounter() {
        killsCounter = 0;
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
        if (characterControl != null && characterControl.getPhysicsSpace() != null) {
            characterControl.getPhysicsSpace().remove(characterControl);
        }
        if (playerNode != null) {
            app.getRootNode().detachChild(playerNode);
        }
    }

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
    public void setAlive(boolean alive) { this.isAlive = alive; }
    public AnimComposer getAnimComposer() { return animComposer; }
    public SkinningControl getSkinningControl() { return skinningControl; }
    public Spatial getCurrentTarget() { return currentTarget; }
    public BetterCharacterControl getCharacterControl() { return characterControl; }

    public void setPlayerName(String name) { this.playerName = name; }
    public void setLevel(int level) { this.baseLevel = level; }
    public void setHealth(int hp) { this.finalHealth = Math.min(hp, finalMaxHealth); }
    public void setMaxHealth(int maxHp) { this.baseMaxHealth = maxHp; recalculateStats(); }
    public void setMana(int mana) { this.finalMana = Math.min(mana, finalMaxMana); }
    public void setMaxMana(int maxMana) { this.baseMaxMana = maxMana; recalculateStats(); }
    public void setExperience(int exp) { this.experience = exp; }
    public void setPosition(Vector3f pos) {
        this.position.set(pos);
        smoothPosition.set(pos);
        if (characterControl != null) {
            characterControl.warp(pos);
        }
        playerNode.setLocalTranslation(pos);
    }
    public void setSpeed(float speed) { /* not used */ }
    public String getCurrentDungeon() { return currentDungeonId; }
    public void setCurrentDungeon(String dungeonId) { this.currentDungeonId = dungeonId; }

    public void setUIManager(UIManager ui) { this.uiManager = ui; }
    public void setNetworkManager(NetworkManager nm) { this.networkManager = nm; }
}