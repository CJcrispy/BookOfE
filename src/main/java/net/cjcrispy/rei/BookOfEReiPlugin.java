package net.cjcrispy.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.cjcrispy.BookOfE;
import net.cjcrispy.block.ModBlocks;
import net.cjcrispy.item.ModItems;

public class BookOfEReiPlugin implements REIClientPlugin {
    
    @Override
    public void registerEntries(EntryRegistry registry) {
        // Hide all spawn eggs
        registry.removeEntry(EntryStacks.of(ModItems.MILLY_KNIGHT_SPAWN_EGG));
        registry.removeEntry(EntryStacks.of(ModItems.NICKY_SUMMONER_SPAWN_EGG));
        registry.removeEntry(EntryStacks.of(ModItems.JOE_REBEL_SPAWN_EGG));
        registry.removeEntry(EntryStacks.of(ModItems.SLIME_CHRIS_SPAWN_EGG));
        registry.removeEntry(EntryStacks.of(ModItems.KING_HAJILE_SPAWN_EGG));
        registry.removeEntry(EntryStacks.of(ModItems.DARK_WIZARD_SPAWN_EGG));
        registry.removeEntry(EntryStacks.of(ModItems.BLACKBIRD_WARRIOR_SPAWN_EGG));
        registry.removeEntry(EntryStacks.of(ModItems.SHADOWQUINN_WARRIOR_SPAWN_EGG));
        
        // Add weapons to REI (ensure they are visible)
        registry.addEntry(EntryStacks.of(ModItems.WORM_HAMMER));
        registry.addEntry(EntryStacks.of(ModItems.SLIME_HAMMER));
        registry.addEntry(EntryStacks.of(ModItems.MOON_SWORD));
        registry.addEntry(EntryStacks.of(ModItems.BEACH_BLADE));
        registry.addEntry(EntryStacks.of(ModItems.BLACKBORN));
        registry.addEntry(EntryStacks.of(ModItems.CALAMITY));
        registry.addEntry(EntryStacks.of(ModItems.ELI_SUMMONING_KEY));
        registry.addEntry(EntryStacks.of(ModItems.NICKY_SUMMONING_KEY));
        registry.addEntry(EntryStacks.of(ModItems.MILLY_KEY));
        registry.addEntry(EntryStacks.of(ModItems.JOE_KEY));

        registry.addEntry(EntryStacks.of(ModBlocks.CULT_ALTAR));
        
        // Note: Enchanted books with mod enchantments should be automatically discovered by REI
        // since enchantments are registered in the registry. If they don't appear, we may need
        // to manually add them using the correct API for creating enchanted books with components.
        
        BookOfE.LOGGER.info("REI Plugin registered for " + BookOfE.MOD_ID + " - Weapons added, spawn eggs hidden");
    }
}

