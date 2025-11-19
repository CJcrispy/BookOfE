package net.cjcrispy.item;

import net.cjcrispy.config.WeaponConfig;
import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.item.custom.WormHammerItem;
import net.cjcrispy.item.custom.BlackbornSwordItem;
import net.cjcrispy.item.custom.BeachBladeItem;
import net.cjcrispy.item.custom.CalamitySwordItem;
import net.cjcrispy.item.custom.SlimeHammerItem;
import net.cjcrispy.item.custom.MoonveilSwordItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    // Summoning keys
    public static final Item NICKY_SUMMONING_KEY = registerItem("summon_key_nicky", new Item(new Item.Settings())); // Used for Nicky Summoner
    public static final Item ELI_SUMMONING_KEY = registerItem("summon_key_eli", new Item(new Item.Settings()));
    public static final Item MILLY_KEY = registerItem("summon_key_milly", new Item(new Item.Settings()));
    public static final Item JOE_KEY = registerItem("summon_key_joe", new Item(new Item.Settings()));


    // Weapons
    public static final Item WORM_HAMMER = registerItem("worm_hammer",
            new WormHammerItem(WeaponConfig.WormHammer.MATERIAL, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, WeaponConfig.WormHammer.ADDITIONAL_DAMAGE, WeaponConfig.WormHammer.ATTACK_SPEED))));

    public static final Item SLIME_HAMMER = registerItem("slime_hammer",
            new SlimeHammerItem(WeaponConfig.SlimeHammer.MATERIAL, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, WeaponConfig.SlimeHammer.ADDITIONAL_DAMAGE, WeaponConfig.SlimeHammer.ATTACK_SPEED))));

    public static final Item MOON_SWORD = registerItem("moon_sword",
            new MoonveilSwordItem(WeaponConfig.MoonSword.MATERIAL, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, WeaponConfig.MoonSword.ADDITIONAL_DAMAGE, WeaponConfig.MoonSword.ATTACK_SPEED))));

    public static final Item BEACH_BLADE = registerItem("beach_blade",
            new BeachBladeItem(WeaponConfig.BeachBlade.MATERIAL, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, WeaponConfig.BeachBlade.ADDITIONAL_DAMAGE, WeaponConfig.BeachBlade.ATTACK_SPEED))));

    public static final Item BLACKBORN = registerItem("the_black_blade",
            new BlackbornSwordItem(WeaponConfig.Blackborn.MATERIAL, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, WeaponConfig.Blackborn.ADDITIONAL_DAMAGE, WeaponConfig.Blackborn.ATTACK_SPEED))));

    public static final Item CALAMITY = registerItem("calamityblade",
            new CalamitySwordItem(WeaponConfig.Calamity.MATERIAL, new Item.Settings()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, WeaponConfig.Calamity.ADDITIONAL_DAMAGE, WeaponConfig.Calamity.ATTACK_SPEED))));

    // Spawn Eggs - registered after entities are loaded
    public static Item MILLY_KNIGHT_SPAWN_EGG;
    public static Item NICKY_SUMMONER_SPAWN_EGG;
    public static Item JOE_REBEL_SPAWN_EGG;
    public static Item SLIME_CHRIS_SPAWN_EGG;
    public static Item KING_HAJILE_SPAWN_EGG;
    public static Item DARK_WIZARD_SPAWN_EGG;
    public static Item BLACKBIRD_WARRIOR_SPAWN_EGG;
    public static Item SHADOWQUINN_WARRIOR_SPAWN_EGG;

    // helper function
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(BookOfE.MOD_ID, name), item);
    }


    public static void registerModItems() {
        BookOfE.LOGGER.info("Registering Mod Items for " + BookOfE.MOD_ID);

        // Register spawn eggs after entities are loaded
        MILLY_KNIGHT_SPAWN_EGG = registerItem("milly_knight_spawn_egg",
                new SpawnEggItem(ModEntities.MILLY_KNIGHT, 0x9d9d9d, 0xabdbe3, new Item.Settings()));

        NICKY_SUMMONER_SPAWN_EGG = registerItem("nicky_summoner_spawn_egg",
                new SpawnEggItem(ModEntities.NICKY_SUMMONER, 0x109607, 0xabdbe3, new Item.Settings()));

        JOE_REBEL_SPAWN_EGG = registerItem("joe_rebel_spawn_egg",
                new SpawnEggItem(ModEntities.JOE_REBEL, 0x672dbe, 0xabdbe3, new Item.Settings()));

        SLIME_CHRIS_SPAWN_EGG = registerItem("slime_chris_spawn_egg",
                new SpawnEggItem(ModEntities.CHRIS_SLIME, 0x1366e8, 0xabdbe3, new Item.Settings()));

        KING_HAJILE_SPAWN_EGG = registerItem("king_hajile_spawn_egg",
                new SpawnEggItem(ModEntities.KING_HAJILE, 0xe8ce13, 0xabdbe3, new Item.Settings()));

        DARK_WIZARD_SPAWN_EGG = registerItem("dark_wizard_spawn_egg",
                new SpawnEggItem(ModEntities.DARK_WIZARD, 0x6e33ff, 0x1e1a27, new Item.Settings()));

        BLACKBIRD_WARRIOR_SPAWN_EGG = registerItem("blackbird_spawn_egg",
                new SpawnEggItem(ModEntities.BLACKBIRD_WARRIOR, 0x000000, 0x524d4d, new Item.Settings()));

        SHADOWQUINN_WARRIOR_SPAWN_EGG = registerItem("shadowquinn_spawn_egg",
                new SpawnEggItem(ModEntities.SHADOW_QUINN, 0x6e33ff, 0x524d4d, new Item.Settings()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(WORM_HAMMER);
            entries.add(SLIME_HAMMER);
            entries.add(MOON_SWORD);
            entries.add(BEACH_BLADE);
            entries.add(BLACKBORN);
            entries.add(CALAMITY);
        });
    }
}
