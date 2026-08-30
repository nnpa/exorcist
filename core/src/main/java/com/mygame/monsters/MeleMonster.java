package com.mygame.monsters;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * Монстр ближнего боя.
 *
 * Общая логика находится в Monster:
 *
 * - HP
 * - урон
 * - смерть
 * - HP bar
 * - stun
 * - bleed
 * - AI
 * - позиция
 * - модель
 * - анимации
 * - loot
 * - boss
 *
 * Здесь находится только логика ближнего боя.
 */
public class MeleMonster extends Monster {

    // ============================================================
    // СОСТОЯНИЕ АТАКИ
    // ============================================================

    private boolean meleeAttacking = false;

    private float meleeAttackTimer = 0f;

    /**
     * Задержка от начала Attack до нанесения урона.
     */
    private float meleeAttackDelay = 0.35f;

    /**
     * Был ли уже нанесён урон
     * в текущей атаке.
     */
    private boolean meleeDamageApplied = false;

    /**
     * Текущая анимация.
     */
    private String currentAnimation = "";

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public MeleMonster() {
        super();
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public void update(float tpf) {

        if (!Monster.isGameRunning) {
            return;
        }

        /*
         * Monster обрабатывает:
         *
         * - stun
         * - bleed
         * - смерть
         * - AI
         *
         * Поэтому сначала вызываем базовый update.
         */
        super.update(tpf);

        if (!isAlive()) {
            return;
        }

        // ========================================================
        // STUN
        // ========================================================

        if (isStunned()) {

            meleeAttacking = false;
            meleeAttackTimer = 0f;
            meleeDamageApplied = false;

            return;
        }

        // ========================================================
        // НЕТ ИГРОКА
        // ========================================================

        if (getPlayerManager() == null) {

            meleeAttacking = false;
            meleeAttackTimer = 0f;
            meleeDamageApplied = false;

            playIdle();

            return;
        }

        Vector3f monsterPosition = getPosition();

        Vector3f playerPosition =
                getPlayerManager().getPosition();

        if (monsterPosition == null ||
                playerPosition == null) {

            return;
        }

        // ========================================================
        // ДИСТАНЦИЯ
        // ========================================================

        float distance =
                getHorizontalDistance(
                        monsterPosition,
                        playerPosition
                );

        // ========================================================
        // ИГРОК ВНЕ РАДИУСА АТАКИ
        // ========================================================

        if (distance > getAttackRange()) {

            meleeAttacking = false;
            meleeAttackTimer = 0f;
            meleeDamageApplied = false;

            playIdle();

            return;
        }

        // ========================================================
        // ИГРОК В РАДИУСЕ
        // ========================================================

        meleeAttacking = true;

        /*
         * Поворачиваем монстра к игроку.
         *
         * ВАЖНО:
         *
         * Этот метод учитывает базовую ориентацию
         * модели из Monster.setModelNode().
         */
        rotateTowardsPlayer(playerPosition);

        // ========================================================
        // НАЧАЛО УДАРА
        // ========================================================

        if (!meleeDamageApplied &&
                meleeAttackTimer <= 0f) {

            startMeleeAttack();
        }

        // ========================================================
        // ТАЙМЕР УДАРА
        // ========================================================

        if (meleeAttackTimer > 0f) {

            meleeAttackTimer -= tpf;

            if (meleeAttackTimer <= 0f) {

                meleeAttackTimer = 0f;

                applyMeleeDamage();
            }
        }
    }

    // ============================================================
    // НАЧАЛО АТАКИ
    // ============================================================

    private void startMeleeAttack() {

        if (!isAlive()) {
            return;
        }

        if (getPlayerManager() == null) {
            return;
        }

        if (!isPlayerInAttackRange()) {

            playIdle();

            return;
        }

        meleeAttacking = true;

        meleeDamageApplied = false;

        playAttack();

        meleeAttackTimer =
                meleeAttackDelay;
    }

    // ============================================================
    // УРОН
    // ============================================================

    private void applyMeleeDamage() {

        if (meleeDamageApplied) {
            return;
        }

        if (!isAlive()) {
            return;
        }

        if (getPlayerManager() == null) {
            return;
        }

        /*
         * Повторно проверяем дистанцию.
         *
         * Игрок мог убежать во время
         * подготовки удара.
         */
        if (!isPlayerInAttackRange()) {

            meleeAttacking = false;
            meleeDamageApplied = false;

            playIdle();

            return;
        }

        int damage =
                Math.round(
                        getDamage()
                );

        if (damage <= 0) {
            return;
        }

        getPlayerManager().takeDamage(damage);

        meleeDamageApplied = true;

        /*
         * После удара начинается
         * cooldown следующей атаки.
         */
        meleeAttackTimer =
                getMeleeAttackCooldown();
    }

    // ============================================================
    // ATTACK
    // ============================================================

    private void playAttack() {

        playAnimationOnce("Attack");
    }

    // ============================================================
    // IDLE
    // ============================================================

    private void playIdle() {

        playAnimationOnce("Idle");
    }

    // ============================================================
    // ANIMATION ONCE
    // ============================================================

    private void playAnimationOnce(
            String animationName
    ) {

        if (animationName == null ||
                animationName.isEmpty()) {

            return;
        }

        if (animationName.equals(currentAnimation)) {
            return;
        }

        currentAnimation = animationName;

        playAnimation(animationName);
    }

    // ============================================================
    // ПОВОРОТ К ИГРОКУ
    // ============================================================

// ============================================================
// ПОВОРОТ К ИГРОКУ
// ============================================================

private void rotateTowardsPlayer(
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

    /*
     * Направление на игрока.
     *
     * atan2(dx, dz):
     *
     * Игрок спереди  (+Z) ->   0°
     * Игрок справа    (+X) -> +90°
     * Игрок сзади     (-Z) -> 180°
     * Игрок слева     (-X) -> -90°
     */
    float angle =
            FastMath.atan2(
                    dx,
                    dz
            );

    /*
     * ВАЖНО:
     *
     * Monster.setModelNode() уже задаёт
     * базовый поворот модели:
     *
     *     -90° по Y
     *
     * Поэтому здесь НЕ добавляем HALF_PI.
     *
     * Иначе модель будет повёрнута
     * относительно игрока неправильно.
     */
    angle -= FastMath.HALF_PI;

    com.jme3.math.Quaternion rotation =
            new com.jme3.math.Quaternion();

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
    // COOLDOWN
    // ============================================================

    protected float getMeleeAttackCooldown() {

        return 0.8f;
    }

    // ============================================================
    // GETTERS / SETTERS
    // ============================================================

    public float getMeleeAttackDelay() {

        return meleeAttackDelay;
    }

    public void setMeleeAttackDelay(
            float delay
    ) {

        meleeAttackDelay =
                Math.max(
                        0f,
                        delay
                );
    }

    public boolean isMeleeAttacking() {

        return meleeAttacking;
    }
}