package org.mcaccess.minecraftaccess.features.inventory_controls;

import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

public final class RecipeBookNarration {
    private RecipeBookNarration() {
    }

    public static String describe(RecipeButton button) {
        ItemStack itemStack = button.getDisplayStack();
        String craftable = button.getCollection().hasCraftable()
                ? I18n.get("minecraft_access.other.craftable")
                : I18n.get("minecraft_access.other.not_craftable");
        return format(craftable, itemStack.getCount(), itemStack.getHoverName().getString());
    }

    static String format(String craftable, int count, String itemName) {
        return "%s %d %s".formatted(craftable, count, itemName);
    }
}
