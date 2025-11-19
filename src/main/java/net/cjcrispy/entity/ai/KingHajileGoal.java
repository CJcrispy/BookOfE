package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.KingHajileEntity;
import net.cjcrispy.procedure.hajile.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class KingHajileGoal extends Goal {
    protected final KingHajileEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private int updateCountdownTicks;
    private int cooldown;
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private long lastUpdateTime;
    private int blindingFlashCooldown = 0;
    private static final int BLINDING_FLASH_COOLDOWN = 400; // 20 seconds
    
    private static final int PHASE_1 = 1;

    public KingHajileGoal(KingHajileEntity mob, double speed, boolean pauseWhenMobIdle) {
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

            if ((this.pauseWhenMobIdle || this.mob.getVisibilityCache().canSee(livingEntity)) && this.updateCountdownTicks <= 0) {
                // More aggressive movement - shorter update intervals for better responsiveness
                this.updateCountdownTicks = 2 + this.mob.getRandom().nextInt(3); // Reduced from 4-10 to 2-4
                double d = this.mob.squaredDistanceTo(livingEntity);
                
                // Phase 2 moves more aggressively
                double adjustedSpeed = this.speed;
                if (this.mob.getPhase() == 2) {
                    adjustedSpeed = this.speed * 1.3; // 30% faster in phase 2
                    this.updateCountdownTicks = Math.max(1, this.updateCountdownTicks - 1); // Even more frequent updates
                }
                
                if (d > 1024.0F) {
                    this.updateCountdownTicks += 5; // Reduced from 10
                } else if (d > 256.0F) {
                    this.updateCountdownTicks += 2; // Reduced from 5
                }

                // More persistent pathfinding
                if (!this.mob.getNavigation().startMovingTo(livingEntity, adjustedSpeed)) {
                    this.updateCountdownTicks += 8; // Reduced from 15
                    
                    // Try to flank or reposition if pathfinding fails
                    if (this.mob.getRandom().nextDouble() < 0.3) {
                        net.minecraft.util.math.Vec3d flankPos = livingEntity.getPos().add(
                                (this.mob.getRandom().nextDouble() - 0.5) * 8,
                                0,
                                (this.mob.getRandom().nextDouble() - 0.5) * 8
                        );
                        this.mob.getNavigation().startMovingTo(flankPos.x, flankPos.y, flankPos.z, adjustedSpeed);
                    }
                }

                this.updateCountdownTicks = this.getTickCount(this.updateCountdownTicks);
            }
            
            // Phase 2: More mobile, less stationary
            if (this.mob.getPhase() == 2 && this.mob.getRandom().nextDouble() < 0.1) {
                // Occasionally reposition even when not attacking (10% chance per tick)
                if (livingEntity != null && this.mob.squaredDistanceTo(livingEntity) < 64) {
                    net.minecraft.util.math.Vec3d reposition = livingEntity.getPos().add(
                            (this.mob.getRandom().nextDouble() - 0.5) * 6,
                            0,
                            (this.mob.getRandom().nextDouble() - 0.5) * 6
                    );
                    this.mob.getNavigation().startMovingTo(reposition.x, reposition.y, reposition.z, this.speed * 1.2);
                }
            }

            this.cooldown = Math.max(this.cooldown - 1, 0);
            this.blindingFlashCooldown = Math.max(this.blindingFlashCooldown - 1, 0);
            
            // Stop navigation during attack animations to prevent interruptions
            String currentAttackState = this.mob.getAttackState();
            if (!currentAttackState.isEmpty() && !"ascendant_lunge".equals(currentAttackState) && !"light_devourer".equals(currentAttackState)) {
                this.mob.getNavigation().stop();
            }
            
            // Don't attack during transition
            if (!this.mob.isInTransition()) {
                this.attack(livingEntity);
            }
        }
    }

    // Attack method using phase-based selection
    protected void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            this.resetCooldown();

            int phase = this.mob.getPhase();
            String attackType = phase == PHASE_1 ? this.getPhase1Attack(target) : this.getPhase2Attack(target);
            
            switch (attackType) {
                // Phase 1 attacks
                case "RadiantJudgment":
                    this.mob.setAttackState("radiant_judgment");
                    RadiantJudgment.execute(this.mob);
                    break;
                    
                case "SacredShockwave":
                    this.mob.setAttackState("sacred_shockwave");
                    SacredShockwave.execute(this.mob);
                    break;
                    
                case "SanctifiedWard":
                    this.mob.setAttackState("sanctified_ward");
                    SanctifiedWard.execute(this.mob);
                    break;
                    
                case "OrbOfLight":
                    this.mob.setAttackState("orb_of_light");
                    OrbOfLight.execute(this.mob);
                    break;
                    
                case "RoyalCommand":
                    this.mob.setAttackState("royal_command");
                    RoyalCommand.execute(this.mob);
                    break;
                    
                // Phase 2 attacks
                case "BlindingFlash":
                    this.mob.setAttackState("blinding_flash");
                    BlindingFlash.execute(this.mob);
                    this.blindingFlashCooldown = BLINDING_FLASH_COOLDOWN;
                    break;
                    
                case "LightfallBarrage":
                    this.mob.setAttackState("lightfall_barrage");
                    LightfallBarrage.execute(this.mob);
                    break;
                    
                case "AscendantLunge":
                    this.mob.setAttackState("ascendant_lunge");
                    AscendantLunge.execute(this.mob);
                    break;
                    
                case "JudgmentChains":
                    this.mob.setAttackState("judgment_chains");
                    JudgmentChains.execute(this.mob);
                    break;
                    
                case "SeveredHalo":
                    this.mob.setAttackState("severed_halo");
                    SeveredHalo.execute(this.mob);
                    break;
                    
                case "LightDevourer":
                    this.mob.setAttackState("light_devourer");
                    LightDevourer.execute(this.mob);
                    break;
                    
                default:
                    // Fallback to melee
                    this.mob.swingHand(Hand.MAIN_HAND);
                    if (this.mob.isInAttackRange(target)) {
                        this.mob.tryAttack(target);
                    }
            }
        }
    }
    
    // Phase 1 attack selection
    private String getPhase1Attack(LivingEntity target) {
        double distanceSq = this.mob.squaredDistanceTo(target);
        double random = this.mob.getRandom().nextDouble();
        
        // Check if ward is needed (health below 50%)
        if (this.mob.getHealth() < this.mob.getMaxHealth() * 0.5 && !this.mob.isSanctifiedWardActive() && random < 0.4) {
            return "SanctifiedWard";
        }
        
        // Long range - Radiant Judgment
        if (distanceSq > 64.0 && random < 0.5) {
            return "RadiantJudgment";
        }
        
        // Medium range - Sacred Shockwave or Orb of Light
        if (distanceSq > 16.0 && distanceSq <= 64.0) {
            if (random < 0.3) {
                return "SacredShockwave";
            } else if (random < 0.5) {
                return "OrbOfLight";
            } else if (random < 0.7) {
                return "RadiantJudgment";
            }
        }
        
        // Close range - Royal Command or Sacred Shockwave
        if (distanceSq <= 16.0) {
            if (random < 0.3) {
                return "RoyalCommand";
            } else if (random < 0.6) {
                return "SacredShockwave";
            }
        }
        
        // Default to Radiant Judgment
        return "RadiantJudgment";
    }
    
    // Phase 2 attack selection
    private String getPhase2Attack(LivingEntity target) {
        double distanceSq = this.mob.squaredDistanceTo(target);
        double random = this.mob.getRandom().nextDouble();
        double healthPercent = this.mob.getHealth() / this.mob.getMaxHealth();
        
        // Light Devourer in last 15% HP (only once)
        if (healthPercent <= 0.15 && !this.mob.isLightDevourerActive() && random < 0.8) {
            return "LightDevourer";
        }
        
        // Blinding Flash every 20 seconds
        if (this.blindingFlashCooldown <= 0 && random < 0.9) {
            return "BlindingFlash";
        }
        
        // Close range - Ascendant Lunge or Judgment Chains
        if (distanceSq <= 16.0) {
            if (random < 0.4) {
                return "AscendantLunge";
            } else if (random < 0.7) {
                return "JudgmentChains";
            } else if (random < 0.85) {
                return "SeveredHalo";
            }
        }
        
        // Medium/Long range - Lightfall Barrage or Severed Halo
        if (distanceSq > 16.0) {
            if (random < 0.5) {
                return "LightfallBarrage";
            } else if (random < 0.75) {
                return "SeveredHalo";
            } else if (random < 0.9) {
                return "JudgmentChains";
            }
        }
        
        // Default to Lightfall Barrage
        return "LightfallBarrage";
    }

    // Resets the cooldown between attacks
    protected void resetCooldown() {
        this.cooldown = this.getTickCount(20); // Set cooldown to 20 ticks (1 second)
    }

    // Check if King Hajile is cooled down and ready to attack
    protected boolean isCooledDown() {
        return this.cooldown <= 0;
    }

    // Check if King Hajile can attack the target
    protected boolean canAttack(LivingEntity target) {
        // Don't attack during transition
        if (this.mob.isInTransition()) {
            return false;
        }
        
        String currentState = this.mob.getAttackState();
        if (!currentState.isEmpty()) {
            // Allow some attacks to continue (ascendant lunge, light devourer)
            if (!"ascendant_lunge".equals(currentState) && !"light_devourer".equals(currentState)) {
                return false; // Don't start new attack while attack is playing
            }
        }
        
        return this.isCooledDown() && 
               (this.mob.isInAttackRange(target) || this.mob.squaredDistanceTo(target) < 256.0) && 
               this.mob.getVisibilityCache().canSee(target);
    }

    // Convert ticks to the required time for cooldowns
    protected int getTickCount(int ticks) {
        return ticks;
    }
}

