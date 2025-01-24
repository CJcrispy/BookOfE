package net.cjcrispy.item;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

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

    public static final Item MILLY_KNIGHT_SPAWN_EGG = registerItem("milly_knight_spawn_egg",
            new SpawnEggItem(ModEntities.MILLY_KNIGHT, 0x9dc783, 0xbfaf5f, new Item.Settings()));


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
            entries.add(MILLY_KNIGHT_SPAWN_EGG);
        });
    }
}
