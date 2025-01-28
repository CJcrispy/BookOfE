package net.cjcrispy.item;

import net.cjcrispy.BookOfE;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final ItemGroup BOOK_OF_E_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(BookOfE.MOD_ID, "book_of_e_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(Items.GOLDEN_APPLE))
                    .displayName(Text.translatable("itemgroup.bookofe.book_of_e_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.WORM_HAMMER);
                        entries.add(ModItems.CALAMITY);
                        entries.add(ModItems.BLACKBORN);
                        entries.add(ModItems.BEACH_BLADE);
                        entries.add(ModItems.ANGEL_SWORD);
                        entries.add(ModItems.SLIME_SWORD);
                        entries.add(ModItems.MILLY_KNIGHT_SPAWN_EGG);
                        entries.add(ModItems.QUINN_KNIGHT_SPAWN_EGG);
                        entries.add(ModItems.DARK_WIZARD_SPAWN_EGG);

                    }).build());

    // Register Groups
    public static void registerItemGroups() {
        BookOfE.LOGGER.info("Registering Item Groups for " + BookOfE.MOD_ID);
    }
}
