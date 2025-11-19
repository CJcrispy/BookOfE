package net.cjcrispy.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import net.cjcrispy.BookOfE;

public class BookOfEReiPlugin implements REIClientPlugin {
    

    public void onREIReady() {
        // REI automatically discovers items, blocks, and enchantments from the registries
        // No need to manually register them unless you have custom recipes or information
        BookOfE.LOGGER.info("REI Plugin registered for " + BookOfE.MOD_ID);
    }
}

