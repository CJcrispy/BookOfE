package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.BlackBirdEntity;
import net.cjcrispy.entity.custom.NickySummonerEntity;
import net.cjcrispy.procedure.nicky.NickySummonerBeamAttack;
import net.cjcrispy.procedure.nicky.NickySummonerCommand;
import net.cjcrispy.procedure.nicky.NickySummonerSoulBurst;
import net.cjcrispy.procedure.nicky.NickySummonerSummonAttack;
import net.cjcrispy.procedure.nicky.NickySummonerTeleport;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class NickySummonerGoal extends Goal {

    protected final NickySummonerEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private int updateCountdownTicks;
    private int cooldown;
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private long lastUpdateTime;
    private int attackCooldown;
    private static final int ATTACK_COOLDOWN_TICKS = 60; // 3 seconds between attacks
    private int tooCloseTimer;
    private static final int TOO_CLOSE_THRESHOLD = 40; // 2 seconds (40 ticks) of being too close triggers teleport
    private static final double TOO_CLOSE_DISTANCE = 64.0; // 8 blocks squared
    private int teleportCooldown;
    private static final int TELEPORT_COOLDOWN_TICKS = 100; // 5 seconds cooldown between teleports

    public NickySummonerGoal(NickySummonerEntity mob, double speed, boolean pauseWhenMobIdle) {
        this.mob = mob;
        this.speed = speed;
        this.pauseWhenMobIdle = pauseWhenMobIdle;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        long currentTime = this.mob.getWorld().getTime();
        if (currentTime - this.lastUpdateTime < ATTACK_INTERVAL_TICKS) {
            return false;
        } else {
            this.lastUpdateTime = currentTime;
            LivingEntity livingEntity = this.mob.getTarget();
            if (livingEntity == null || !livingEntity.isAlive()) {
                return false;
            } else {
                this.path = this.mob.getNavigation().findPathTo(livingEntity, 0);
                return this.path != null || this.mob.isInAttackRange(livingEntity);
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity == null || !livingEntity.isAlive()) {
            return false;
        } else if (!this.pauseWhenMobIdle) {
            return !this.mob.getNavigation().isIdle();
        } else {
            return this.mob.isInWalkTargetRange(livingEntity.getBlockPos());
        }
    }

    @Override
    public void start() {
        this.mob.getNavigation().startMovingAlong(this.path, this.speed);
        this.mob.setAttacking(true);
        this.updateCountdownTicks = 0;
        this.cooldown = 0;
        this.attackCooldown = 0;
        this.tooCloseTimer = 0;
        this.teleportCooldown = 0;
    }

    @Override
    public void stop() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null) {
            this.mob.setTarget(null);
        }
        this.mob.setAttacking(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null) {
            this.mob.getLookControl().lookAt(livingEntity, 30.0F, 30.0F);
            this.updateCountdownTicks = Math.max(this.updateCountdownTicks - 1, 0);
            this.attackCooldown = Math.max(this.attackCooldown - 1, 0);
            this.teleportCooldown = Math.max(this.teleportCooldown - 1, 0);
            
            double d = this.mob.squaredDistanceTo(livingEntity);
            
            // Track how long player has been too close
            if (d < TOO_CLOSE_DISTANCE) {
                this.tooCloseTimer++;
            } else {
                this.tooCloseTimer = Math.max(0, this.tooCloseTimer - 2); // Decay timer when not too close
            }
            
            // Check if should teleport (player too close for too long)
            if (this.tooCloseTimer >= TOO_CLOSE_THRESHOLD && this.teleportCooldown <= 0) {
                NickySummonerTeleport.execute(this.mob);
                this.tooCloseTimer = 0;
                this.teleportCooldown = TELEPORT_COOLDOWN_TICKS;
                this.attackCooldown = ATTACK_COOLDOWN_TICKS; // Reset attack cooldown after teleport
                return; // Skip movement and attack this tick
            }

            if ((this.pauseWhenMobIdle || this.mob.getVisibilityCache().canSee(livingEntity)) && this.updateCountdownTicks <= 0) {
                this.updateCountdownTicks = 4 + this.mob.getRandom().nextInt(7);
                
                // Nicky wants to keep distance - prefer 8-15 blocks away
                double preferredMinDistance = 64.0; // 8 blocks squared
                double preferredMaxDistance = 225.0; // 15 blocks squared
                
                if (d < preferredMinDistance) {
                    // Too close - back away
                    Vec3d awayDirection = this.mob.getPos().subtract(livingEntity.getPos()).normalize();
                    Vec3d backAwayPos = this.mob.getPos().add(awayDirection.multiply(5.0));
                    this.mob.getNavigation().startMovingTo(backAwayPos.x, backAwayPos.y, backAwayPos.z, this.speed);
                    this.updateCountdownTicks += 5;
                } else if (d > preferredMaxDistance) {
                    // Too far - move closer
                    if (!this.mob.getNavigation().startMovingTo(livingEntity, this.speed)) {
                        this.updateCountdownTicks += 15;
                    }
                    if (d > 1024.0F) {
                        this.updateCountdownTicks += 10;
                    } else if (d > 256.0F) {
                        this.updateCountdownTicks += 5;
                    }
                } else {
                    // At preferred distance - stop moving and prioritize spells
                    this.mob.getNavigation().stop();
                }

                this.updateCountdownTicks = this.getTickCount(this.updateCountdownTicks);
            }

            this.cooldown = Math.max(this.cooldown - 1, 0);
            
            // Perform attacks when ready
            if (this.attackCooldown <= 0 && this.canAttack(livingEntity)) {
                this.attack(livingEntity);
                this.attackCooldown = ATTACK_COOLDOWN_TICKS;
            }
        }
    }

    // Attack method to choose between summoning, beam attack, command, and soul burst
    protected void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            String attackType = this.getCurrentAttack(target);
            
            switch (attackType) {
                case "Summon":
                    NickySummonerSummonAttack.execute(this.mob);
                    break;
                    
                case "Beam":
                    NickySummonerBeamAttack.execute(this.mob);
                    break;
                    
                case "Command":
                    NickySummonerCommand.execute(this.mob);
                    break;
                    
                case "SoulBurst":
                    NickySummonerSoulBurst.execute(this.mob);
                    break;
            }
        }
    }

    // Method to determine which attack type to perform
    private String getCurrentAttack(LivingEntity target) {
        double distance = this.mob.squaredDistanceTo(target);
        double random = this.mob.getRandom().nextDouble();
        
        // Check if at appropriate distance (8-15 blocks)
        boolean atPreferredDistance = distance >= 64.0 && distance <= 225.0;
        
        // Check if there are minions nearby (within 30 blocks)
        int nearbyMinions = countNearbyMinions();
        
        // If at preferred distance, prioritize spells
        if (atPreferredDistance) {
            if (nearbyMinions > 0) {
                // With minions at preferred distance
                if (random < 0.4) {
                    return "Command"; // 40% chance
                } else if (random < 0.7) {
                    return "SoulBurst"; // 30% chance
                } else {
                    return "Beam"; // 30% chance
                }
            } else {
                // No minions, at preferred distance - prioritize spells
                if (random < 0.45) {
                    return "SoulBurst"; // 45% chance
                } else if (random < 0.8) {
                    return "Beam"; // 35% chance
                } else {
                    return "Summon"; // 20% chance (to get minions)
                }
            }
        }
        
        // Not at preferred distance - use varied logic
        // If there are minions nearby, Command becomes more likely
        if (nearbyMinions > 0) {
            if (random < 0.35) {
                return "Command"; // 35% chance if minions exist
            } else if (random < 0.55) {
                return "SoulBurst"; // 20% chance for Soul Burst
            } else if (random < 0.75) {
                return distance > 100 ? "Beam" : "Summon";
            } else {
                return distance > 100 ? "Beam" : "Summon";
            }
        }
        
        // No minions nearby and not at preferred distance
        // If target is far away, prefer beam or soul burst
        // If target is close, prefer summoning or soul burst
        if (distance > 100) { // More than 10 blocks away
            if (random < 0.4) {
                return "Beam";
            } else if (random < 0.7) {
                return "SoulBurst"; // 30% chance for area denial
            } else {
                return "Summon";
            }
        } else { // Close range
            if (random < 0.35) {
                return "Summon";
            } else if (random < 0.65) {
                return "SoulBurst"; // 30% chance for area denial
            } else {
                return "Beam";
            }
        }
    }
    
    // Count nearby BlackBird minions
    private int countNearbyMinions() {
        if (this.mob.getWorld().isClient()) {
            return 0;
        }
        
        Box searchBox = this.mob.getBoundingBox().expand(30.0);
        List<BlackBirdEntity> minions = this.mob.getWorld().getEntitiesByClass(
                BlackBirdEntity.class, 
                searchBox, 
                minion -> minion.isAlive() && this.mob.squaredDistanceTo(minion) <= 900.0 // 30 blocks squared
        );
        
        return minions.size();
    }

    // Check if NickySummoner can attack the target
    protected boolean canAttack(LivingEntity target) {
        return this.mob.getVisibilityCache().canSee(target) && 
               this.mob.squaredDistanceTo(target) <= 400; // Within 20 blocks
    }

    // Convert ticks to the required time for cooldowns
    protected int getTickCount(int ticks) {
        return ticks;
    }
}
