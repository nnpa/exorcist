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
    private boolean bossMusicStarted = false;

    public MonsterAI(Monster monster) {
        this.monster = monster;
    }

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
    }

    public void resetMusicFlag() {
        bossMusicStarted = false;
    }

    public void update(float tpf) {
        if (stopped || monster == null) {
            return;
        }
        if (attackCooldown > 0) attackCooldown -= tpf;
        if (attackAnimTimer > 0) attackAnimTimer -= tpf;

        Vector3f playerPos = getPlayerPosition();
        if (playerPos == null) return;

        Vector3f monsterPos = monster.getPosition();
        float dist = monsterPos.distance(playerPos);

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
                    if (attackCooldown > 0) {
                        monster.playAnimation("Idle");
                    }
                    //System.out.println("Monster ATTACKING, dist=" + dist);
                } else {
                    moveTowards(playerPos, tpf);
                }
                break;

            case ATTACKING:
                // ===== ВКЛЮЧАЕМ МУЗЫКУ БОССА ТОЛЬКО ЗДЕСЬ =====
                if (monster.isBoss() && !bossMusicStarted) {
                    SoundManager.stopMusic();
                    SoundManager.playMusic(SoundManager.MUSIC_BOSS);
                    bossMusicStarted = true;
                   // System.out.println("[MonsterAI] Boss music started!");
                }

                if (dist > monster.getAttackRange() * 1.2f) {
                    currentState = State.CHASING;
                    monster.playAnimation("Walk");
                   // System.out.println("Monster CHASING (lost target)");
                } else {
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

        float stopDistance = monster.getAttackRange() * 0.85f;
        if (dist <= stopDistance) {
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
        if (attackCooldown > 0) {
            if (attackAnimTimer <= 0) {
                monster.playAnimation("Idle");
            }
            return;
        }

        if (playerManager != null) {
            playerManager.takeDamage((int) monster.getDamage());
           // System.out.println("[MonsterAI] Attacked player for " + monster.getDamage());
        }
        monster.playAnimation("Attack");
        attackCooldown = ATTACK_COOLDOWN_TIME;
        attackAnimTimer = 0.5f;
    }

    private Vector3f getPlayerPosition() {
        if (playerManager != null) {
            return playerManager.getPosition();
        }
        return new Vector3f(0, 0, 0);
    }
    
    private boolean stopped = false;

// Добавьте метод stop
public void stop() {
    this.stopped = true;
    this.monster = null;
    this.playerManager = null;
    currentState = State.IDLE;
    System.out.println("[MonsterAI] AI stopped.");
}

}