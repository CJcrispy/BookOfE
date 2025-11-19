package net.cjcrispy.entity.summoning;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Modular registry for boss summoning using keys on the Cult Altar.
 * Easily add new key-entity pairs by calling registerSummoning().
 */
public final class BossSummoningRegistry {
    
private static final Map<Item, BiFunction<ServerWorld, BlockPos, ?>> SUMMONING_MAP = new HashMap<>();
    
    private BossSummoningRegistry() {
    }
    
    /**
     * Register a key-item to entity-type summoning pair.
     * 
     * @param keyItem The item that triggers the summoning
     * @param entityFactory Function that creates the entity at the given position
     */
    public static <T extends net.minecraft.entity.Entity> void registerSummoning(
            Item keyItem, 
            BiFunction<ServerWorld, BlockPos, T> entityFactory) {
        SUMMONING_MAP.put(keyItem, entityFactory);
    }
    
    /**
     * Attempts to summon a boss using the given key item.
     * 
     * @param keyItem The key item used
     * @param world The server world
     * @param pos The position to spawn at (typically above the altar)
     * @return The summoned entity, or null if no summoning is registered for this key
     */
    @Nullable
    public static net.minecraft.entity.Entity trySummon(Item keyItem, ServerWorld world, BlockPos pos) {
        net.cjcrispy.BookOfE.LOGGER.info("BossSummoningRegistry.trySummon called with key: {} at position: {}", keyItem, pos);
        BiFunction<ServerWorld, BlockPos, ?> factory = SUMMONING_MAP.get(keyItem);
        if (factory == null) {
            net.cjcrispy.BookOfE.LOGGER.error("No factory found for key: {}", keyItem);
            return null;
        }
        
        net.cjcrispy.BookOfE.LOGGER.info("Factory found, creating entity...");
        try {
            net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity) factory.apply(world, pos);
            net.cjcrispy.BookOfE.LOGGER.info("Entity created: {} (null: {})", entity != null ? entity.getType().toString() : "null", entity == null);
            if (entity != null) {
                net.cjcrispy.BookOfE.LOGGER.info("Spawning entity at: x={}, y={}, z={}", entity.getX(), entity.getY(), entity.getZ());
                boolean spawned = world.spawnEntity(entity);
                net.cjcrispy.BookOfE.LOGGER.info("spawnEntity returned: {}", spawned);
                // Spawn effects are handled automatically by spawnEntity
                return entity;
            } else {
                net.cjcrispy.BookOfE.LOGGER.error("Entity factory returned null");
            }
        } catch (Exception e) {
            net.cjcrispy.BookOfE.LOGGER.error("Failed to summon entity with key " + keyItem, e);
        }
        
        return null;
    }
    
    /**
     * Checks if a key item is registered for summoning.
     */
    public static boolean isSummoningKey(Item keyItem) {
        return SUMMONING_MAP.containsKey(keyItem);
    }

    public static String describeRegisteredKeys() {
        if (SUMMONING_MAP.isEmpty()) {
            return "<none>";
        }

        return SUMMONING_MAP.keySet().stream()
                .map(item -> Registries.ITEM.getId(item).toString())
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("<none>");
    }
}

