package com.mygame.monsters;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.SoundManager;

public class MonsterAI {

    private Monster monster;

    private PlayerManager playerManager;

    private enum State {
        IDLE,
        CHASING,
        ATTACKING
    }

    private State currentState =
            State.IDLE;

    private boolean isStunned = false;

    private boolean bossMusicStarted = false;

    private boolean stopped = false;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public MonsterAI(
            Monster monster
    ) {

        this.monster =
                monster;
    }

    // ============================================================
    // PLAYER
    // ============================================================

    public void setPlayerManager(
            PlayerManager pm
    ) {

        this.playerManager =
                pm;
    }

    // ============================================================
    // STUN
    // ============================================================

    public void setStunned(
            boolean stunned
    ) {

        if (this.isStunned == stunned) {
            return;
        }

        this.isStunned =
                stunned;

        if (stunned) {

            currentState =
                    State.IDLE;

            monster.stopWalking();
        }
    }

    // ============================================================
    // MUSIC
    // ============================================================

    public void resetMusicFlag() {

        bossMusicStarted =
                false;
    }

    // ============================================================
    // STOP
    // ============================================================

    public void stop() {

        stopped = true;

        monster = null;
        playerManager = null;

        currentState =
                State.IDLE;

        isStunned = false;


    }

    // ============================================================
    // UPDATE
    // ============================================================

    public void update(
            float tpf
    ) {

        if (stopped ||
                monster == null ||
                !monster.isAlive()) {

            return;
        }

        // ========================================================
        // STUN
        // ========================================================

        if (isStunned ||
                monster.getStunTimer() > 0f) {

            monster.stopWalking();

            return;
        }

        // ========================================================
        // PLAYER
        // ========================================================

        Vector3f playerPos =
                getPlayerPosition();

        if (playerPos == null) {

            monster.playIdle();

            return;
        }

        Vector3f monsterPos =
                monster.getPosition();

        if (monsterPos == null) {
            return;
        }

        float distance =
                horizontalDistance(
                        monsterPos,
                        playerPos
                );

        // ========================================================
        // IDLE
        // ========================================================

        if (currentState == State.IDLE) {

            if (distance <=
                    monster.getAggroRange()) {

                currentState =
                        State.CHASING;


            } else {

                monster.playIdle();

                return;
            }
        }

        // ========================================================
        // CHASING
        // ========================================================

        if (currentState ==
                State.CHASING) {

            /*
             * Уже подошли достаточно близко.
             */
            if (distance <=
                    monster.getAttackRange()) {

                currentState =
                        State.ATTACKING;

                monster.playIdle();



                return;
            }

            /*
             * Идём к игроку.
             */
            moveTowards(
                    playerPos,
                    tpf
            );

            monster.playWalk();

            return;
        }

        // ========================================================
        // ATTACKING
        // ========================================================

        if (currentState ==
                State.ATTACKING) {

            /*
             * Игрок отошёл.
             */
            if (distance >
                    monster.getAttackRange() * 1.15f) {

                currentState =
                        State.CHASING;



                monster.playWalk();

                return;
            }

            /*
             * Не двигаемся во время атаки.
             */
            rotateTowardsPlayer(
                    playerPos
            );

            /*
             * Босс музыка.
             */
            if (monster.isBoss() &&
                    !bossMusicStarted) {

                SoundManager.stopMusic();

                SoundManager.playMusic(
                        SoundManager.MUSIC_BOSS
                );

                bossMusicStarted =
                        true;
            }

            /*
             * Передаём управление конкретному
             * типу монстра.
             */
            monster.updateCombat(
                    tpf
            );
        }
    }

    // ============================================================
    // MOVEMENT
    // ============================================================

    private void moveTowards(
            Vector3f target,
            float tpf
    ) {

        Vector3f current =
                monster.getPosition();

        Vector3f direction =
                new Vector3f(
                        target.x - current.x,
                        0f,
                        target.z - current.z
                );

        float distance =
                direction.length();

        if (distance < 0.001f) {

            monster.playIdle();

            return;
        }

        direction.normalizeLocal();

        float stopDistance =
                monster.getAttackRange() * 0.85f;

        if (distance <= stopDistance) {

            monster.playIdle();

            return;
        }

        float movement =
                monster.getMoveSpeed()
                * tpf;

        /*
         * Не перелетаем через игрока.
         */
        movement =
                Math.min(
                        movement,
                        distance - stopDistance
                );

        if (movement > 0f) {

            Vector3f newPosition =
                    current.add(
                            direction.mult(
                                    movement
                            )
                    );

            monster.setPosition(
                    newPosition
            );
        }

        rotateTowardsDirection(
                direction
        );
    }

    // ============================================================
    // ROTATION
    // ============================================================

    private void rotateTowardsPlayer(
            Vector3f playerPosition
    ) {

        Vector3f monsterPosition =
                monster.getPosition();

        float dx =
                playerPosition.x
                - monsterPosition.x;

        float dz =
                playerPosition.z
                - monsterPosition.z;

        if (FastMath.abs(dx) < 0.000001f &&
                FastMath.abs(dz) < 0.000001f) {

            return;
        }

        Vector3f direction =
                new Vector3f(
                        dx,
                        0f,
                        dz
                );

        direction.normalizeLocal();

        rotateTowardsDirection(
                direction
        );
    }

    private void rotateTowardsDirection(
            Vector3f direction
    ) {

        if (monster.getModelNode() == null) {
            return;
        }

        if (direction.lengthSquared() <
                0.000001f) {

            return;
        }

        Quaternion rotation =
                new Quaternion();

        rotation.lookAt(
                direction,
                Vector3f.UNIT_Y
        );

        /*
         * Базовая модель уже имеет -90°.
         */
        rotation.multLocal(
                new Quaternion().fromAngleAxis(
                        -FastMath.HALF_PI,
                        Vector3f.UNIT_Y
                )
        );

        monster.getModelNode()
                .setLocalRotation(
                        rotation
                );
    }

    // ============================================================
    // DISTANCE
    // ============================================================

    private float horizontalDistance(
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
    // PLAYER POSITION
    // ============================================================

    private Vector3f getPlayerPosition() {

        if (playerManager != null) {

            return playerManager.getPosition();
        }

        return null;
    }
}