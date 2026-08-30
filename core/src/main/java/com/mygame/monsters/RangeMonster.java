package com.mygame.monsters;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.nio.ByteBuffer;

public class RangeMonster extends Monster {

    // ============================================================
    // ДАЛЬНЯЯ АТАКА
    // ============================================================

    protected float rangedAttackCooldown = 1.5f;

    protected float projectileCastDelay = 0.45f;

    protected float projectileSpeed = 8.0f;

    protected float projectileSize = 0.45f;

    protected float projectileMaxLife = 5.0f;

    protected float projectileHitDistance = 0.7f;

    protected float projectileHeight = 1.2f;

    protected float projectileForwardOffset = 0.7f;

    // ============================================================
    // ЦВЕТ СНАРЯДА
    // ============================================================

    protected ColorRGBA projectileColor =
            new ColorRGBA(
                    1f,
                    0f,
                    0f,
                    1f
            );

    // ============================================================
    // СОСТОЯНИЕ АТАКИ
    // ============================================================

    protected boolean rangedAttacking = false;

    protected float rangedAttackTimer = 0f;

    protected float projectileCastTimer = -1f;

    protected boolean projectileReleased = false;

    // ============================================================
    // СНАРЯД
    // ============================================================

    protected Node projectileNode;

    protected ParticleEmitter projectileEmitter;

    protected Vector3f projectilePosition;

    protected Vector3f projectileDirection =
            Vector3f.UNIT_Z.clone();

    protected float projectileLife;

    protected Texture2D projectileTexture;

    // ============================================================
    // АНИМАЦИЯ
    // ============================================================

    private String currentAnimation = "";

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public RangeMonster() {

        super();

        setMoveSpeed(0f);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public void update(float tpf) {

        if (!Monster.isGameRunning) {
            return;
        }

        // --------------------------------------------------------
        // Снаряд обновляется независимо от состояния монстра
        // --------------------------------------------------------

        updateProjectile(tpf);

        // --------------------------------------------------------
        // STUN
        // --------------------------------------------------------

        if (isStunned()) {

            updateStun(tpf);

            return;
        }

        // --------------------------------------------------------
        // BLEED
        // --------------------------------------------------------

        updateBleed(tpf);

        // --------------------------------------------------------
        // DEATH
        // --------------------------------------------------------

        if (!isAlive()) {

            removeProjectile();

            return;
        }

        // --------------------------------------------------------
        // COOLDOWN
        // --------------------------------------------------------

        if (rangedAttackTimer > 0f) {

            rangedAttackTimer -= tpf;

            if (rangedAttackTimer < 0f) {
                rangedAttackTimer = 0f;
            }
        }

        // --------------------------------------------------------
        // НЕТ ИГРОКА
        // --------------------------------------------------------

        if (getPlayerManager() == null) {

            rangedAttacking = false;

            projectileCastTimer = -1f;

            playIdle();

            return;
        }

        // --------------------------------------------------------
        // ПОЗИЦИИ
        // --------------------------------------------------------

        Vector3f monsterPosition =
                getPosition();

        Vector3f playerPosition =
                getPlayerManager().getPosition();

        if (monsterPosition == null ||
                playerPosition == null) {

            rangedAttacking = false;

            projectileCastTimer = -1f;

            playIdle();

            return;
        }

        // --------------------------------------------------------
        // ДИСТАНЦИЯ
        // --------------------------------------------------------

        float distance =
                getHorizontalDistance(
                        monsterPosition,
                        playerPosition
                );

        // --------------------------------------------------------
        // ИГРОК ВНЕ РАДИУСА
        // --------------------------------------------------------

        if (distance > getAttackRange()) {

            rangedAttacking = false;

            if (projectileCastTimer >= 0f) {

                projectileCastTimer = -1f;
            }

            playIdle();

            return;
        }

        // --------------------------------------------------------
        // ИГРОК В РАДИУСЕ
        // --------------------------------------------------------

        rangedAttacking = true;

        // --------------------------------------------------------
        // ПОВОРОТ
        // --------------------------------------------------------

        rotateTowardsPlayer(
                playerPosition
        );

        // --------------------------------------------------------
        // ПОДГОТОВКА ВЫСТРЕЛА
        // --------------------------------------------------------

        if (projectileCastTimer >= 0f) {

            projectileCastTimer -= tpf;

            if (projectileCastTimer <= 0f) {

                projectileCastTimer = -1f;

                if (isPlayerInAttackRange()) {

                    releaseProjectile();

                } else {

                    rangedAttacking = false;

                    playIdle();
                }
            }

            return;
        }

        // --------------------------------------------------------
        // НОВАЯ АТАКА
        // --------------------------------------------------------

        if (!projectileReleased &&
                rangedAttackTimer <= 0f) {

            startRangedAttack();
        }
    }

    // ============================================================
    // STUN
    // ============================================================

    protected void updateStun(float tpf) {

        stunTimer -= tpf;

        if (stunTimer < 0f) {
            stunTimer = 0f;
        }

        if (getAI() != null) {

            getAI().setStunned(
                    stunTimer > 0f
            );
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

            if (getAI() != null) {

                getAI().setStunned(false);
            }

            currentAnimation = "";
        }
    }

    // ============================================================
    // BLEED
    // ============================================================

    protected void updateBleed(float tpf) {

        if (bleedTimer > 0f) {

            bleedTimer -= tpf;

            takeDamage(
                    bleedDamage * tpf
            );
        }
    }

    // ============================================================
    // НАЧАЛО АТАКИ
    // ============================================================

    protected void startRangedAttack() {

        if (!isAlive()) {
            return;
        }

        if (getPlayerManager() == null) {
            return;
        }

        if (rangedAttackTimer > 0f) {
            return;
        }

        if (projectileCastTimer >= 0f) {
            return;
        }

        if (!isPlayerInAttackRange()) {

            rangedAttacking = false;

            playIdle();

            return;
        }

        Vector3f playerPosition =
                getPlayerManager().getPosition();

        if (playerPosition != null) {

            rotateTowardsPlayer(
                    playerPosition
            );
        }

        rangedAttacking = true;

        projectileReleased = false;

        playAttack();

        projectileCastTimer =
                projectileCastDelay;
    }

    // ============================================================
    // ATTACK
    // ============================================================

    protected void playAttack() {

        /*
         * Сбрасываем название предыдущей анимации,
         * чтобы AnimComposer получил новую команду Attack.
         */

        currentAnimation = "";

        playAnimation(
                "Attack"
        );

        currentAnimation = "Attack";
    }

    // ============================================================
    // IDLE
    // ============================================================

    protected void playIdle() {

        if (!"Idle".equals(currentAnimation)) {

            currentAnimation = "Idle";

            playAnimation(
                    "Idle"
            );
        }
    }

    // ============================================================
    // ПОВОРОТ К ИГРОКУ
    // ============================================================

protected void rotateTowardsPlayer(
        Vector3f playerPosition
) {

    if (modelNode == null ||
            playerPosition == null) {

        return;
    }

    Vector3f monsterPosition =
            getPosition();

    if (monsterPosition == null) {
        return;
    }

    float dx =
            playerPosition.x -
            monsterPosition.x;

    float dz =
            playerPosition.z -
            monsterPosition.z;

    if (FastMath.abs(dx) < 0.000001f &&
            FastMath.abs(dz) < 0.000001f) {

        return;
    }

    float angle =
            FastMath.atan2(
                    dx,
                    dz
            );

    /*
     * Коррекция поворота модели:
     * -45 градусов.
     */
    angle -= FastMath.QUARTER_PI;

    Quaternion rotation =
            new Quaternion();

    rotation.fromAngles(
            0f,
            angle,
            0f
    );

    modelNode.setLocalRotation(
            rotation
    );
}
    // ============================================================
    // ВЫПУСК СНАРЯДА
    // ============================================================

    protected void releaseProjectile() {

        if (!isAlive()) {
            return;
        }

        if (getPlayerManager() == null) {
            return;
        }

        if (!isPlayerInAttackRange()) {

            rangedAttacking = false;

            projectileCastTimer = -1f;

            projectileReleased = false;

            playIdle();

            return;
        }

        Vector3f monsterPosition =
                getPosition();

        Vector3f playerPosition =
                getPlayerManager().getPosition();

        if (monsterPosition == null ||
                playerPosition == null) {

            return;
        }

        /*
         * Начальное направление.
         */

        Vector3f direction =
                playerPosition.subtract(
                        monsterPosition
                );

        direction.y = 0f;

        if (direction.lengthSquared() <
                0.000001f) {

            direction.set(
                    0f,
                    0f,
                    1f
            );

        } else {

            direction.normalizeLocal();
        }

        projectileDirection =
                direction.clone();

        /*
         * Начальная позиция.
         */

        projectilePosition =
                monsterPosition.clone();

        projectilePosition.addLocal(
                direction.mult(
                        projectileForwardOffset
                )
        );

        projectilePosition.y +=
                projectileHeight;

        projectileLife =
                projectileMaxLife;

        /*
         * Теперь снаряд считается выпущенным.
         */

        projectileReleased = true;

        createProjectile(
                projectilePosition
        );

        /*
         * Запускаем cooldown сразу после выпуска.
         */

        rangedAttackTimer =
                rangedAttackCooldown;

        /*
         * Атака закончена.
         */

        rangedAttacking = false;

        projectileCastTimer = -1f;

        currentAnimation = "";

        playIdle();
    }

    // ============================================================
    // СОЗДАНИЕ СНАРЯДА
    // ============================================================

    protected void createProjectile(
            Vector3f position
    ) {

        removeProjectileVisualOnly();

        if (app == null) {
            return;
        }

        if (projectileTexture == null) {

            projectileTexture =
                    createProjectileTexture();
        }

        projectileNode =
                new Node(
                        "RangeMonsterProjectileNode"
                );

        projectileNode.setLocalTranslation(
                position
        );

        projectileEmitter =
                new ParticleEmitter(
                        "RangeMonsterProjectile",
                        ParticleMesh.Type.Triangle,
                        1
                );

        Material material =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Particle.j3md"
                );

        material.setTexture(
                "Texture",
                projectileTexture
        );

        material.getAdditionalRenderState()
                .setBlendMode(
                        RenderState.BlendMode.Alpha
                );

        material.getAdditionalRenderState()
                .setDepthWrite(false);

        material.getAdditionalRenderState()
                .setDepthTest(true);

        projectileEmitter.setMaterial(
                material
        );

        projectileEmitter.setQueueBucket(
                RenderQueue.Bucket.Translucent
        );

        projectileEmitter.setStartColor(
                projectileColor.clone()
        );

        projectileEmitter.setEndColor(
                projectileColor.clone()
        );

        projectileEmitter.setStartSize(
                projectileSize
        );

        projectileEmitter.setEndSize(
                projectileSize
        );

        projectileEmitter.setLowLife(
                projectileMaxLife
        );

        projectileEmitter.setHighLife(
                projectileMaxLife
        );

        projectileEmitter.setParticlesPerSec(
                0f
        );

        projectileEmitter
                .getParticleInfluencer()
                .setInitialVelocity(
                        Vector3f.ZERO
                );

        projectileEmitter
                .getParticleInfluencer()
                .setVelocityVariation(
                        0f
                );

        projectileEmitter.setGravity(
                0f,
                0f,
                0f
        );

        projectileEmitter.setInWorldSpace(
                false
        );

        projectileEmitter.setRandomAngle(
                true
        );

        projectileEmitter.setRotateSpeed(
                3f
        );

        projectileNode.attachChild(
                projectileEmitter
        );

        app.getRootNode().attachChild(
                projectileNode
        );

        projectileEmitter.setEnabled(
                true
        );

        projectileEmitter.emitAllParticles();
    }

    // ============================================================
    // ОБНОВЛЕНИЕ СНАРЯДА
    // ============================================================

    protected void updateProjectile(
            float tpf
    ) {

        if (projectileNode == null ||
                projectilePosition == null) {

            return;
        }

        projectileLife -= tpf;

        if (projectileLife <= 0f) {

            removeProjectile();

            return;
        }

        // ========================================================
        // САМОНАВЕДЕНИЕ
        // ========================================================

        if (getPlayerManager() != null) {

            Vector3f playerPosition =
                    getPlayerManager()
                            .getPosition();

            if (playerPosition != null) {

                /*
                 * Каждый кадр заново вычисляем направление
                 * на текущую позицию игрока.
                 */

                Vector3f targetDirection =
                        playerPosition.subtract(
                                projectilePosition
                        );

                /*
                 * Для наведения используем горизонтальное
                 * направление.
                 */

                targetDirection.y = 0f;

                if (targetDirection.lengthSquared() >
                        0.000001f) {

                    targetDirection.normalizeLocal();

                    projectileDirection =
                            targetDirection;
                }
            }
        }

        // ========================================================
        // ДВИЖЕНИЕ
        // ========================================================

        Vector3f movement =
                projectileDirection.mult(
                        projectileSpeed * tpf
                );

        projectilePosition.addLocal(
                movement
        );

        projectileNode.setLocalTranslation(
                projectilePosition
        );

        // ========================================================
        // ПОПАДАНИЕ
        // ========================================================

        if (getPlayerManager() == null) {
            return;
        }

        Vector3f playerPosition =
                getPlayerManager()
                        .getPosition();

        if (playerPosition == null) {
            return;
        }

        /*
         * Для попадания также проверяем горизонтальную
         * дистанцию, чтобы высота игрока не мешала.
         */

        float dx =
                projectilePosition.x -
                playerPosition.x;

        float dz =
                projectilePosition.z -
                playerPosition.z;

        float horizontalDistance =
                FastMath.sqrt(
                        dx * dx +
                        dz * dz
                );

        if (horizontalDistance <=
                projectileHitDistance) {

            hitPlayer();

            removeProjectile();
        }
    }

    // ============================================================
    // ПОПАДАНИЕ
    // ============================================================

    protected void hitPlayer() {

        if (!isAlive()) {
            return;
        }

        if (getPlayerManager() == null) {
            return;
        }

        int damage =
                Math.round(
                        getDamage()
                );

        if (damage <= 0) {
            return;
        }

        getPlayerManager()
                .takeDamage(
                        damage
                );
    }

    // ============================================================
    // УДАЛЕНИЕ ТОЛЬКО ВИЗУАЛА
    // ============================================================

    protected void removeProjectileVisualOnly() {

        if (projectileEmitter != null) {

            projectileEmitter.killAllParticles();

            projectileEmitter.removeFromParent();

            projectileEmitter = null;
        }

        if (projectileNode != null) {

            projectileNode.removeFromParent();

            projectileNode = null;
        }
    }

    // ============================================================
    // УДАЛЕНИЕ СНАРЯДА
    // ============================================================

    protected void removeProjectile() {

        removeProjectileVisualOnly();

        projectilePosition = null;

        projectileLife = 0f;

        projectileReleased = false;
    }

    // ============================================================
    // ТЕКСТУРА СНАРЯДА
    // ============================================================

    protected Texture2D createProjectileTexture() {

        final int size = 32;

        ByteBuffer buffer =
                ByteBuffer.allocateDirect(
                        size * size * 4
                );

        float center =
                (size - 1) * 0.5f;

        float maxDistance =
                center;

        for (int y = 0; y < size; y++) {

            for (int x = 0; x < size; x++) {

                float dx =
                        x - center;

                float dy =
                        y - center;

                float distance =
                        FastMath.sqrt(
                                dx * dx +
                                dy * dy
                        );

                float normalized =
                        distance /
                        maxDistance;

                float alpha =
                        1f - normalized;

                alpha =
                        FastMath.clamp(
                                alpha,
                                0f,
                                1f
                        );

                alpha =
                        FastMath.pow(
                                alpha,
                                1.4f
                        );

                int a =
                        (int)
                        (
                                alpha *
                                255f
                        );

                buffer.put(
                        (byte) 255
                );

                buffer.put(
                        (byte) 255
                );

                buffer.put(
                        (byte) 255
                );

                buffer.put(
                        (byte) a
                );
            }
        }

        buffer.flip();

        Image image =
                new Image(
                        Image.Format.RGBA8,
                        size,
                        size,
                        buffer
                );

        Texture2D texture =
                new Texture2D(
                        image
                );

        texture.setMagFilter(
                com.jme3.texture.Texture.MagFilter.Bilinear
        );

        texture.setMinFilter(
                com.jme3.texture.Texture.MinFilter.BilinearNoMipMaps
        );

        return texture;
    }

    // ============================================================
    // СМЕРТЬ
    // ============================================================

    @Override
    protected void onDeath() {

        rangedAttacking = false;

        projectileCastTimer = -1f;

        rangedAttackTimer = 0f;

        removeProjectile();

        super.onDeath();
    }

    // ============================================================
    // DISPOSE
    // ============================================================

    public void disposeRangeMonsterEffects() {

        removeProjectile();

        rangedAttacking = false;

        projectileCastTimer = -1f;

        rangedAttackTimer = 0f;

        currentAnimation = "";
    }

    // ============================================================
    // SETTERS
    // ============================================================

    public void setRangedAttackCooldown(
            float cooldown
    ) {

        rangedAttackCooldown =
                Math.max(
                        0f,
                        cooldown
                );
    }

    public float getRangedAttackCooldown() {
        return rangedAttackCooldown;
    }

    public void setProjectileCastDelay(
            float delay
    ) {

        projectileCastDelay =
                Math.max(
                        0f,
                        delay
                );
    }

    public float getProjectileCastDelay() {
        return projectileCastDelay;
    }

    public void setProjectileSpeed(
            float speed
    ) {

        projectileSpeed =
                Math.max(
                        0.1f,
                        speed
                );
    }

    public float getProjectileSpeed() {
        return projectileSpeed;
    }

    public void setProjectileSize(
            float size
    ) {

        projectileSize =
                Math.max(
                        0.01f,
                        size
                );
    }

    public float getProjectileSize() {
        return projectileSize;
    }

    public void setProjectileColor(
            ColorRGBA color
    ) {

        if (color != null) {

            projectileColor =
                    color.clone();
        }
    }

    public ColorRGBA getProjectileColor() {
        return projectileColor;
    }

    public boolean isRangedAttacking() {
        return rangedAttacking;
    }
}