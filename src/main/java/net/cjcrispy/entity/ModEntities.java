package net.cjcrispy.entity;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<MillyKnightEntity> MILLY_KNIGHT = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "milly_knight"),
            EntityType.Builder.create(MillyKnightEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static void registerModEntities() {
        BookOfE.LOGGER.info("Registering Mod Entities for " + BookOfE.MOD_ID);
    }
}
