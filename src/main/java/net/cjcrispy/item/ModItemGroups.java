package net.cjcrispy.item;

import net.cjcrispy.BookOfE;
import net.cjcrispy.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static ItemGroup BOOK_OF_E_ITEMS_GROUP;

    // Register Groups
    public static void registerItemGroups() {
        BookOfE.LOGGER.info("Registering Item Groups for " + BookOfE.MOD_ID);
        
        // Register item group after items and blocks are loaded
        BOOK_OF_E_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
                Identifier.of(BookOfE.MOD_ID, "book_of_e_items"),
                FabricItemGroup.builder().icon(() -> new ItemStack(ModBlocks.CULT_ALTAR))
                        .displayName(Text.translatable("itemgroup.bookofe.book_of_e_items"))
                        .entries((displayContext, entries) -> {
                            entries.add(ModItems.NICKY_SUMMONING_KEY);
                            entries.add(ModItems.ELI_SUMMONING_KEY);
                            entries.add(ModItems.MILLY_KEY);
                            entries.add(ModItems.JOE_KEY);

                            entries.add(ModBlocks.CULT_ALTAR);

                            entries.add(ModItems.WORM_HAMMER);
                            entries.add(ModItems.CALAMITY);
                            entries.add(ModItems.BLACKBORN);
                            entries.add(ModItems.BEACH_BLADE);
                            entries.add(ModItems.MOON_SWORD);
                            entries.add(ModItems.SLIME_HAMMER);

                        }).build());
    }
}
