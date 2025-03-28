package net.cjcrispy.entity;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.*;
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

    public static final EntityType<QuinnKnightEntity> QUINN_KNIGHT = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "quinn_knight"),
            EntityType.Builder.create(QuinnKnightEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<NickySummonerEntity> NICKY_SUMMONER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "nicky_summoner"),
            EntityType.Builder.create(NickySummonerEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<JoeRebelEntity> JOE_REBEL = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "joe_rebel"),
            EntityType.Builder.create(JoeRebelEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<SlimeChrisEntity> CHRIS_SLIME = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "slime_chris"),
            EntityType.Builder.create(SlimeChrisEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<KingHajileEntity> KING_HAJILE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "king_hajile"),
            EntityType.Builder.create(KingHajileEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<DarkWizardEntity> DARK_WIZARD = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "dark_wizard"),
            EntityType.Builder.create(DarkWizardEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<BlackBirdEntity> BLACKBIRD_WARRIOR = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "blackbird"),
            EntityType.Builder.create(BlackBirdEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<ShadowQuinnEntity> SHADOW_QUINN = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BookOfE.MOD_ID, "shadowquinn"),
            EntityType.Builder.create(ShadowQuinnEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static void registerModEntities() {
        BookOfE.LOGGER.info("Registering Mod Entities for " + BookOfE.MOD_ID);
    }
}
