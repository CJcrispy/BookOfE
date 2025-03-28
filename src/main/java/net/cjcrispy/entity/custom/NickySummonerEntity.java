package net.cjcrispy.entity.custom;

import net.cjcrispy.entity.ai.NickySummonerGoal;
import net.cjcrispy.entity.ai.QuinnKnightGoal;
import net.cjcrispy.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class NickySummonerEntity extends HostileEntity{

    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Nicky, Sassy Summoner"),
            BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);


    public NickySummonerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        System.out.println("Nicky Summoner initialized");
    }

    @Override
    protected void initGoals() {
        // Attack-related goals
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false)); // Attack logic
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true)); // Target players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true)); // Target iron golems
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, KingHajileEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MillyKnightEntity.class, true)); // Target Milly proactively
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, QuinnKnightEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, JoeRebelEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, SlimeChrisEntity.class, true));
        this.targetSelector.add(4, new RevengeGoal(this)); // Revenge against the last attacker

        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0)); // Wander randomly
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // Look at players
        this.goalSelector.add(4, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F)); // Look at golems
        this.goalSelector.add(5, new LookAtEntityGoal(this, MillyKnightEntity.class, 8.0F)); // Look at Milly
        this.goalSelector.add(5, new LookAtEntityGoal(this, QuinnKnightEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, SlimeChrisEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, KingHajileEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, JoeRebelEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this)); // Look around when idle
        this.goalSelector.add(7, new SwimGoal(this)); // Swim when underwater
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 18)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 400)
                .add(EntityAttributes.GENERIC_ARMOR, 10)
                .add(EntityAttributes.GENERIC_SCALE, 1.35);
    }

    /**
     * Entity tick logic (runs every tick).
     */
    @Override
    public void tick() {
        super.tick(); // Call parent tick logic
        // Add custom behavior here, such as effects or AI adjustments
        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations

        } else {
            // Server-side behavior

        }
    }


    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);

        this.dropItem(ModItems.BEACH_BLADE);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        // Example: Add a death sound or custom effects
        this.getWorld().sendEntityStatus(this, (byte) 60); // Play the death particles
    }

    /* BOSS BAR */
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
    }
}

