package net.cjcrispy.entity.summoning;

import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.JoeRebelEntity;
import net.cjcrispy.entity.custom.KingHajileEntity;
import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.cjcrispy.entity.custom.NickySummonerEntity;
import net.cjcrispy.entity.custom.ShadowQuinnEntity;
import net.cjcrispy.entity.custom.SlimeChrisEntity;
import net.cjcrispy.item.ModItems;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Registers default boss summonings used by the Cult Altar.
 */
public final class BossSummoningInitializer {

    private BossSummoningInitializer() {
    }

    public static void register() {
        net.cjcrispy.BookOfE.LOGGER.info("Registering boss summoning recipes");
        registerMillyKnight();
        registerNickySummoner();
        registerShadowQuinn();
        registerSlimeChris();
        registerEli();
        registerJoeRebel();
    }

    private static void registerMillyKnight() {
        BossSummoningRegistry.registerSummoning(ModItems.MILLY_KEY, (ServerWorld world, BlockPos pos) -> {
            net.cjcrispy.BookOfE.LOGGER.info("Creating MillyKnightEntity at block pos: {}", pos);
            MillyKnightEntity entity = ModEntities.MILLY_KNIGHT.create(world);
            net.cjcrispy.BookOfE.LOGGER.info("Entity created: {} (null: {})", entity != null ? "not null" : "null", entity == null);
            if (entity != null) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY();
                double z = pos.getZ() + 0.5D;
                net.cjcrispy.BookOfE.LOGGER.info("Setting entity position to: x={}, y={}, z={}", x, y, z);
                entity.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
            }
            return entity;
        });
    }

    private static void registerNickySummoner() {
        BossSummoningRegistry.registerSummoning(ModItems.NICKY_SUMMONING_KEY, (ServerWorld world, BlockPos pos) -> {
            net.cjcrispy.BookOfE.LOGGER.info("Creating NickySummonerEntity at block pos: {}", pos);
            NickySummonerEntity entity = ModEntities.NICKY_SUMMONER.create(world);
            net.cjcrispy.BookOfE.LOGGER.info("Entity created: {} (null: {})", entity != null ? "not null" : "null", entity == null);
            if (entity != null) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY();
                double z = pos.getZ() + 0.5D;
                net.cjcrispy.BookOfE.LOGGER.info("Setting entity position to: x={}, y={}, z={}", x, y, z);
                entity.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
            }
            return entity;
        });
    }

    private static void registerShadowQuinn() {
        BossSummoningRegistry.registerSummoning(Items.CRYING_OBSIDIAN, (ServerWorld world, BlockPos pos) -> {
            net.cjcrispy.BookOfE.LOGGER.info("Creating ShadowQuinnEntity at block pos: {}", pos);
            ShadowQuinnEntity entity = ModEntities.SHADOW_QUINN.create(world);
            net.cjcrispy.BookOfE.LOGGER.info("Entity created: {} (null: {})", entity != null ? "not null" : "null", entity == null);
            if (entity != null) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY();
                double z = pos.getZ() + 0.5D;
                net.cjcrispy.BookOfE.LOGGER.info("Setting entity position to: x={}, y={}, z={}", x, y, z);
                entity.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
            }
            return entity;
        });
    }

    private static void registerSlimeChris() {
        BossSummoningRegistry.registerSummoning(Items.SLIME_BLOCK, (ServerWorld world, BlockPos pos) -> {
            net.cjcrispy.BookOfE.LOGGER.info("Creating SlimeChrisEntity at block pos: {}", pos);
            SlimeChrisEntity entity = ModEntities.CHRIS_SLIME.create(world);
            net.cjcrispy.BookOfE.LOGGER.info("Entity created: {} (null: {})", entity != null ? "not null" : "null", entity == null);
            if (entity != null) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY();
                double z = pos.getZ() + 0.5D;
                net.cjcrispy.BookOfE.LOGGER.info("Setting entity position to: x={}, y={}, z={}", x, y, z);
                entity.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
            }
            return entity;
        });
    }

    private static void registerEli() {
        BossSummoningRegistry.registerSummoning(ModItems.ELI_SUMMONING_KEY, (ServerWorld world, BlockPos pos) -> {
            net.cjcrispy.BookOfE.LOGGER.info("Creating KingHajileEntity at block pos: {}", pos);
            KingHajileEntity entity = ModEntities.KING_HAJILE.create(world);
            net.cjcrispy.BookOfE.LOGGER.info("Entity created: {} (null: {})", entity != null ? "not null" : "null", entity == null);
            if (entity != null) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY();
                double z = pos.getZ() + 0.5D;
                net.cjcrispy.BookOfE.LOGGER.info("Setting entity position to: x={}, y={}, z={}", x, y, z);
                entity.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
            }
            return entity;
        });
    }

    private static void registerJoeRebel() {
        BossSummoningRegistry.registerSummoning(ModItems.JOE_KEY, (ServerWorld world, BlockPos pos) -> {
            net.cjcrispy.BookOfE.LOGGER.info("Creating JoeRebelEntity at block pos: {}", pos);
            JoeRebelEntity entity = ModEntities.JOE_REBEL.create(world);
            net.cjcrispy.BookOfE.LOGGER.info("Entity created: {} (null: {})", entity != null ? "not null" : "null", entity == null);
            if (entity != null) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY();
                double z = pos.getZ() + 0.5D;
                net.cjcrispy.BookOfE.LOGGER.info("Setting entity position to: x={}, y={}, z={}", x, y, z);
                entity.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
            }
            return entity;
        });
    }
}

