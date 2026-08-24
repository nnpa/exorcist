package com.mygame.monsters;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.SoundManager;

public class MonsterAI {

    private Monster monster;
    private PlayerManager playerManager;
    private enum State { IDLE, CHASING, ATTACKING }
    private State currentState = State.IDLE;
    private float attackCooldown = 0f;
    private static final float ATTACK_COOLDOWN_TIME = 1.5f;
    private float attackAnimTimer = 0f;
    private boolean isAttacking = false;
    private boolean bossMusicStarted = false; // <-- новое поле

    public MonsterAI(Monster monster) {
        this.monster = monster;
    }

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
    }

    public void update(float tpf) {
        if (attackCooldown > 0) attackCooldown -= tpf;
        if (attackAnimTimer > 0) attackAnimTimer -= tpf;

        Vector3f playerPos = getPlayerPosition();
        if (playerPos == null) return;

        Vector3f monsterPos = monster.getPosition();
        float dist = monsterPos.distance(playerPos);

        // Логирование для отладки
        // System.out.println("Monster state: " + currentState + ", dist: " + dist + ", attackRange: " + monster.getAttackRange());

        switch (currentState) {
            case IDLE:
                if (dist < monster.getAggroRange()) {
                    currentState = State.CHASING;
                    monster.playAnimation("Walk");
                    System.out.println("Monster CHASING");
                }
                break;
            case CHASING:
                if (dist < monster.getAttackRange() * 0.9f) {
                    currentState = State.ATTACKING;
                    // Если кулдаун активен, показываем Idle, иначе атакуем в следующем шаге
                    if (attackCooldown > 0) {
                        monster.playAnimation("Idle");
                    }
                    System.out.println("Monster ATTACKING, dist=" + dist);
                } else {
                    moveTowards(playerPos, tpf);
                }
                break;
            case ATTACKING:
                if (dist > monster.getAttackRange() * 1.2f) {
                    currentState = State.CHASING;
                    monster.playAnimation("Walk");
                    System.out.println("Monster CHASING (lost target)");
                } else {
                    if (monster.isBoss() && !bossMusicStarted) {
                        SoundManager.playMusic(SoundManager.MUSIC_BOSS);
                        bossMusicStarted = true;
                    }
                    attackPlayer();
                }
                break;
        }
    }

    private void moveTowards(Vector3f target, float tpf) {
        Vector3f currentPos = monster.getPosition();
        Vector3f direction = new Vector3f(target.x - currentPos.x, 0, target.z - currentPos.z);
        
        float dist = direction.length();
        if (dist < 0.01f) return;
        direction.normalizeLocal();

        // Останавливаемся на дистанции атаки с небольшим запасом
        float stopDistance = monster.getAttackRange() * 0.85f;
        if (dist <= stopDistance) {
            // Если мы уже достаточно близко, но еще не в ATTACKING (может быть, не перешли из-за задержки)
            // Можно принудительно переключить состояние, но мы это делаем в update
            return;
        }

        Vector3f newPos = currentPos.add(direction.mult(monster.getMoveSpeed() * tpf));
        monster.setPosition(newPos);

        if (monster.getModelNode() != null) {
            Quaternion rot = new Quaternion();
            rot.lookAt(direction, Vector3f.UNIT_Y);
            rot.multLocal(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
            monster.getModelNode().setLocalRotation(rot);
        }
    }

    private void attackPlayer() {
        // Если кулдаун активен, не атакуем
        if (attackCooldown > 0) {
            // Если анимация атаки закончилась, показываем Idle
            if (attackAnimTimer <= 0) {
                monster.playAnimation("Idle");
            }
            return;
        }

        // Атакуем
        if (playerManager != null) {
            playerManager.takeDamage((int) monster.getDamage());
            System.out.println("[MonsterAI] Attacked player for " + monster.getDamage());
        }
        // Запускаем анимацию атаки
        monster.playAnimation("Attack");
        attackCooldown = ATTACK_COOLDOWN_TIME;
        attackAnimTimer = 0.5f; // Длительность анимации, чтобы не перебивать раньше времени
        // После атаки монстр должен остаться на месте и ждать кулдаун
    }

    private Vector3f getPlayerPosition() {
        if (playerManager != null) {
            return playerManager.getPosition();
        }
        return new Vector3f(0, 0, 0);
    }
}