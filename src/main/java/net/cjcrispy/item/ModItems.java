package net.cjcrispy.item;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    // Summoning keys
    public static final Item ELI_SUMMONING_KEY = registerItem("summon_key_e", new Item(new Item.Settings()));


    // Weapons
    public static final Item WORM_HAMMER = registerItem("worm_hammer",
            new SwordItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 3, -2.4f))));

    public static final Item SLIME_SWORD = registerItem("slime_sword",
            new SwordItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 2, -1.4f))));

    public static final Item ANGEL_SWORD = registerItem("angel_sword",
            new SwordItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 2, -1.4f))));

    public static final Item BEACH_BLADE = registerItem("beach_blade",
            new SwordItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 2, -1.4f))));

    public static final Item BLACKBORN = registerItem("the_black_blade",
            new SwordItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 2, -1.4f))));

    public static final Item CALAMITY = registerItem("calamityblade",
            new SwordItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 2, -1.4f))));

    // Spawn Eggs
    public static final Item MILLY_KNIGHT_SPAWN_EGG = registerItem("milly_knight_spawn_egg",
            new SpawnEggItem(ModEntities.MILLY_KNIGHT, 0x9d9d9d, 0xabdbe3, new Item.Settings()));

    public static final Item QUINN_KNIGHT_SPAWN_EGG = registerItem("quinn_knight_spawn_egg",
            new SpawnEggItem(ModEntities.QUINN_KNIGHT, 0xf10000, 0xabdbe3, new Item.Settings()));

    public static final Item NICKY_SUMMONER_SPAWN_EGG = registerItem("nicky_summoner_spawn_egg",
            new SpawnEggItem(ModEntities.NICKY_SUMMONER, 0x109607, 0xabdbe3, new Item.Settings()));

    public static final Item JOE_REBEL_SPAWN_EGG = registerItem("joe_rebel_spawn_egg",
            new SpawnEggItem(ModEntities.JOE_REBEL, 0x672dbe, 0xabdbe3, new Item.Settings()));

    public static final Item SLIME_CHRIS_SPAWN_EGG = registerItem("slime_chris_spawn_egg",
            new SpawnEggItem(ModEntities.CHRIS_SLIME, 0x1366e8, 0xabdbe3, new Item.Settings()));

    public static final Item KING_HAJILE_SPAWN_EGG = registerItem("king_hajile_spawn_egg",
            new SpawnEggItem(ModEntities.KING_HAJILE, 0xe8ce13, 0xabdbe3, new Item.Settings()));

    public static final Item DARK_WIZARD_SPAWN_EGG = registerItem("dark_wizard_spawn_egg",
            new SpawnEggItem(ModEntities.DARK_WIZARD, 0x6e33ff, 0x1e1a27, new Item.Settings()));

    public static final Item BLACKBIRD_WARRIOR_SPAWN_EGG = registerItem("blackbird_spawn_egg",
            new SpawnEggItem(ModEntities.BLACKBIRD_WARRIOR, 0x000000, 0x524d4d, new Item.Settings()));

    public static final Item SHADOWQUINN_WARRIOR_SPAWN_EGG = registerItem("shadowquinn_spawn_egg",
            new SpawnEggItem(ModEntities.SHADOW_QUINN, 0x6e33ff, 0x524d4d, new Item.Settings()));


    // helper function
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(BookOfE.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BookOfE.LOGGER.info("Registering Mod Items for " + BookOfE.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(WORM_HAMMER);
            entries.add(SLIME_SWORD);
            entries.add(ANGEL_SWORD);
            entries.add(BEACH_BLADE);
            entries.add(BLACKBORN);
            entries.add(CALAMITY);
        });
    }
}
