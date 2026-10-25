package org.mcaccess.minecraftaccess.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls;
import org.mcaccess.minecraftaccess.utils.condition.Interval;

@Mixin(RecipeButton.class)
abstract class RecipeButtonMixin {
    @Unique
    private final Interval interval = Interval.ms(5000);
    @Shadow
    private RecipeCollection collection;
    @Unique
    private String previousItemName = "";

    @Shadow
    public abstract ItemStack getDisplayStack();

    @Inject(method = "updateWidgetNarration", at = @At("HEAD"), cancellable = true)
    private void updateWidgetNarrationsMixin(CallbackInfo ci) {
        if (InventoryControls.ownsRecipeNarration((RecipeButton) (Object) this)) {
            ci.cancel();
            return;
        }

        ItemStack itemStack = getDisplayStack();
        String itemName = itemStack.getHoverName().getString();

        boolean sameItem = itemName.equalsIgnoreCase(previousItemName);
        if (!sameItem || interval.isReady()) {
            String craftable = collection.hasCraftable() ? "craftable" : "not_craftable";
            craftable = I18n.get("minecraft_access.other." + craftable);
            String narration = "%s %d %s".formatted(craftable, itemStack.getCount(), itemName);
            MainClass.narrate(narration, true);
        }

        // update the states
        if (!sameItem) {
            previousItemName = itemName;
            interval.reset();
        }

        ci.cancel();
    }
}
