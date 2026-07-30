package com.mygame.monsters;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mygame.managers.PlayerManager;

public class MonsterAI {

    private Monster monster;
    private PlayerManager playerManager;
    private enum State { IDLE, CHASING, ATTACKING }
    private State currentState = State.IDLE;
    private float attackCooldown = 0f;
    private static final float ATTACK_COOLDOWN_TIME = 1.5f;

    public MonsterAI(Monster monster) {
        this.monster = monster;
    }

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
    }

    public void update(float tpf) {
        if (attackCooldown > 0) attackCooldown -= tpf;

        Vector3f playerPos = getPlayerPosition();
        if (playerPos == null) return;

        float dist = monster.getPosition().distance(playerPos);

        switch (currentState) {
            case IDLE:
                if (dist < monster.getAggroRange()) {
                    currentState = State.CHASING;
                    monster.playAnimation("Walk");
                }
                break;
            case CHASING:
                if (dist > monster.getAggroRange() * 1.5f) {
                    currentState = State.IDLE;
                    monster.playAnimation("Idle");
                } else if (dist < monster.getAttackRange()) {
                    currentState = State.ATTACKING;
                    monster.playAnimation("Attack");
                } else {
                    moveTowards(playerPos, tpf);
                }
                break;
            case ATTACKING:
                if (dist > monster.getAttackRange() * 1.2f) {
                    currentState = State.CHASING;
                    monster.playAnimation("Walk");
                } else {
                    attackPlayer();
                }
                break;
        }
    }

private void moveTowards(Vector3f target, float tpf) {
    Vector3f currentPos = monster.getPosition();
    Vector3f direction = new Vector3f(target.x - currentPos.x, 0, target.z - currentPos.z);
    
    if (direction.length() < 0.01f) return;
    direction.normalizeLocal();

    // Движение
    Vector3f newPos = currentPos.add(direction.mult(monster.getMoveSpeed() * tpf));
    monster.setPosition(newPos);

    // Поворот модели в сторону движения с компенсацией
    if (monster.getModelNode() != null) {
        // 1. Вычисляем поворот, чтобы смотреть в направлении direction
        Quaternion rot = new Quaternion();
        rot.lookAt(direction, Vector3f.UNIT_Y);
        
        // 2. Добавляем постоянный поворот на -90° вокруг Y (компенсация)
        // Если модель смотрит в противоположную сторону, меняем знак на +HALF_PI
        rot.multLocal(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
        
        monster.getModelNode().setLocalRotation(rot);
    }
}

    private void attackPlayer() {
        if (attackCooldown <= 0) {
            if (playerManager != null) {
                playerManager.takeDamage((int) monster.getDamage());
            }
            attackCooldown = ATTACK_COOLDOWN_TIME;
        }
    }

    private Vector3f getPlayerPosition() {
        if (playerManager != null) {
            return playerManager.getPosition();
        }
        return new Vector3f(0, 0, 0);
    }
}