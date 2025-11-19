package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.cjcrispy.entity.ai.NickySummonerGoal;
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
        this.goalSelector.add(1, new NickySummonerGoal(this, 1.2, false)); // Custom attack logic with summoning and beam attacks
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, false)); // Fallback melee attack
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true)); // Target players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true)); // Target iron golems
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, KingHajileEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MillyKnightEntity.class, true)); // Target Milly proactively
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, ShadowQuinnEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, JoeRebelEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, SlimeChrisEntity.class, true));
        this.targetSelector.add(4, new RevengeGoal(this)); // Revenge against the last attacker

        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0)); // Wander randomly
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // Look at players
        this.goalSelector.add(4, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F)); // Look at golems
        this.goalSelector.add(5, new LookAtEntityGoal(this, MillyKnightEntity.class, 8.0F)); // Look at Milly
        this.goalSelector.add(5, new LookAtEntityGoal(this, ShadowQuinnEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, SlimeChrisEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, KingHajileEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, JoeRebelEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this)); // Look around when idle
        this.goalSelector.add(7, new SwimGoal(this)); // Swim when underwater
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.NickySummoner.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.NickySummoner.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.NickySummoner.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, MobConfig.NickySummoner.ATTACK_KNOCKBACK)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.NickySummoner.FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.NickySummoner.ARMOR)
                .add(EntityAttributes.GENERIC_SCALE, MobConfig.NickySummoner.SCALE);
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

        // Grant advancement to all players who participated in the fight (similar to Ender Dragon/Wither)
        if (!this.getWorld().isClient() && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            // Ensure root advancement is granted first
            net.minecraft.advancement.AdvancementEntry rootAdvancement = this.getServer().getAdvancementLoader()
                    .get(net.minecraft.util.Identifier.of("bookofe", "root"));
            net.minecraft.advancement.AdvancementEntry killAdvancement = this.getServer().getAdvancementLoader()
                    .get(net.minecraft.util.Identifier.of("bookofe", "custom/kill_nicky_summoner"));
            
            // Collect all participating players
            java.util.Set<net.minecraft.server.network.ServerPlayerEntity> participants = new java.util.HashSet<>();
            participants.addAll(this.bossBar.getPlayers());
            
            // Also add nearby players within 100 blocks
            net.minecraft.util.math.Box searchBox = this.getBoundingBox().expand(100.0);
            for (PlayerEntity player : serverWorld.getPlayers(player -> 
                    player.isAlive() && searchBox.intersects(player.getBoundingBox()))) {
                if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                    participants.add(serverPlayer);
                }
            }
            
            // Grant root advancement first, then kill advancement
            for (net.minecraft.server.network.ServerPlayerEntity player : participants) {
                net.minecraft.advancement.PlayerAdvancementTracker tracker = player.getAdvancementTracker();
                
                // Grant root if not already granted
                if (rootAdvancement != null && !tracker.getProgress(rootAdvancement).isDone()) {
                    tracker.grantCriterion(rootAdvancement, "requirement");
                }
                
                // Grant kill advancement
                if (killAdvancement != null && !tracker.getProgress(killAdvancement).isDone()) {
                    tracker.grantCriterion(killAdvancement, "killed_nicky_summoner");
                }
            }
        }

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

