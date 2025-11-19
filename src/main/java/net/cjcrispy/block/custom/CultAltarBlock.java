package net.cjcrispy.block.custom;

import com.mojang.serialization.MapCodec;
import net.cjcrispy.entity.summoning.AltarRitual;
import net.cjcrispy.entity.summoning.BossSummoningRegistry;
import net.cjcrispy.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CultAltarBlock extends HorizontalFacingBlock {

    public static final MapCodec<CultAltarBlock> CODEC = createCodec(CultAltarBlock::new);

    private static final VoxelShape SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public CultAltarBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        net.cjcrispy.BookOfE.LOGGER.info("Cult Altar: onUse called by {} (client: {})", player.getName().getString(), world.isClient());

        if (world.isClient()) {
            net.cjcrispy.BookOfE.LOGGER.info("Cult Altar: Returning early (client side)");
            return ActionResult.SUCCESS;
        }

        net.cjcrispy.BookOfE.LOGGER.info("Cult Altar: Processing on server side");
        ItemStack heldItem = player.getMainHandStack();
        Hand usedHand = Hand.MAIN_HAND;
        net.cjcrispy.BookOfE.LOGGER.info("Cult Altar: Main hand item: {} (empty: {})", heldItem.getItem(), heldItem.isEmpty());

        if (heldItem.isEmpty() || !BossSummoningRegistry.isSummoningKey(heldItem.getItem())) {
            net.cjcrispy.BookOfE.LOGGER.info("Cult Altar: Main hand not valid, checking off hand");
            heldItem = player.getOffHandStack();
            usedHand = Hand.OFF_HAND;
            net.cjcrispy.BookOfE.LOGGER.info("Cult Altar: Off hand item: {} (empty: {})", heldItem.getItem(), heldItem.isEmpty());
        }

        if (!BossSummoningRegistry.isSummoningKey(heldItem.getItem())) {
            String itemId = heldItem.isEmpty() ? "<empty>" : Registries.ITEM.getId(heldItem.getItem()).toString();
            String registered = BossSummoningRegistry.describeRegisteredKeys();
            net.cjcrispy.BookOfE.LOGGER.debug("Cult Altar used by {} without a valid key in either hand (holding: {})", player.getName().getString(), itemId);
            player.sendMessage(net.minecraft.text.Text.literal("The altar rejects this offering (" + itemId + ")."), true);
            player.sendMessage(net.minecraft.text.Text.literal("You need one of these keys: " + registered), true);
            return ActionResult.PASS;
        }

        // Check if the held item is a summoning key
        if (BossSummoningRegistry.isSummoningKey(heldItem.getItem())) {
            net.cjcrispy.BookOfE.LOGGER.info("Cult Altar activated by {} using {} hand holding {}", player.getName().getString(), usedHand, heldItem);
            
            // Get lore-friendly ritual message based on the key/item
            String ritualMessage = getRitualMessage(heldItem.getItem());
            player.sendMessage(net.minecraft.text.Text.translatable(ritualMessage), true);
            
            // Spawn position is 1 block above the altar
            BlockPos spawnPos = pos.up();
            
            // Check if there's space to spawn
            if (!world.getBlockState(spawnPos).isAir() || !world.getBlockState(spawnPos.up()).isAir()) {
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0F, 0.5F);
                net.cjcrispy.BookOfE.LOGGER.warn("Cult Altar spawn blocked at {} or {}", spawnPos, spawnPos.up());
                player.sendMessage(net.minecraft.text.Text.literal("The air above the altar must be clear."), true);
                return ActionResult.FAIL;
            }
            
            // Consume the key immediately (unless player is in creative)
            if (!player.getAbilities().creativeMode) {
                player.getStackInHand(usedHand).decrement(1);
            }
            
            // Start the ritual with particle effects
            AltarRitual.startRitual((ServerWorld) world, pos, heldItem.getItem(), spawnPos);
            
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // Check if block is being broken (not just moved)
        if (!state.isOf(newState.getBlock()) && !moved) {
            // Block is being broken
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                double centerX = pos.getX() + 0.5D;
                double centerY = pos.getY() + 0.5D;
                double centerZ = pos.getZ() + 0.5D;
                
                // Spawn enchanting particles like enchanting table (simple burst)
                serverWorld.spawnParticles(
                    ParticleTypes.ENCHANT,
                    centerX,
                    centerY,
                    centerZ,
                    20,
                    0.5,
                    0.5,
                    0.5,
                    0.1
                );
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        // Call parent first - this should handle loot table drops
        super.onStacksDropped(state, world, pos, tool, dropExperience);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Spawn occasional enchanting particles while the block exists (like enchanting table)
        if (random.nextInt(3) == 0) {
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.75D;
            double centerZ = pos.getZ() + 0.5D;
            
            double offsetX = (random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (random.nextDouble() - 0.5) * 0.5;
            
            world.addParticle(
                ParticleTypes.ENCHANT,
                centerX + offsetX,
                centerY,
                centerZ + offsetZ,
                0.0,
                0.0,
                0.0
            );
        }
    }
    
    /**
     * Returns a translation key for a lore-friendly ritual message based on the key/item used.
     */
    private static String getRitualMessage(net.minecraft.item.Item keyItem) {
        if (keyItem == ModItems.NICKY_SUMMONING_KEY) {
            return "ritual.bookofe.chaos_summoner";
        } else if (keyItem == ModItems.MILLY_KEY) {
            return "ritual.bookofe.crimson_knight";
        } else if (keyItem == ModItems.ELI_SUMMONING_KEY) {
            return "ritual.bookofe.golden_king";
        } else if (keyItem == ModItems.JOE_KEY) {
            return "ritual.bookofe.shadow_rebel";
        } else if (keyItem == net.minecraft.item.Items.CRYING_OBSIDIAN) {
            return "ritual.bookofe.fallen_shadow";
        } else if (keyItem == net.minecraft.item.Items.SLIME_BLOCK) {
            return "ritual.bookofe.slime_king";
        } else {
            return "ritual.bookofe.generic";
        }
    }

}
