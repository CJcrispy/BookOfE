package net.cjcrispy.enchant;

import net.cjcrispy.BookOfE;
import net.minecraft.util.Identifier;

public class ModEnchantments {
    
    // Enchantment is now data-driven via JSON files in 1.21+
    // The enchantment is defined in data/bookofe/enchantment/rend.json
    public static final Identifier REND_ID = Identifier.of(BookOfE.MOD_ID, "rend");
    
    public static void registerModEnchantments() {
        BookOfE.LOGGER.info("Registering Mod Enchantments for " + BookOfE.MOD_ID);
        // Enchantments are automatically loaded from data files in 1.21+
    }
}

