package org.mcaccess.minecraftaccess.features.inventory_controls;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

final class InventoryScreenInput {
    private InventoryScreenInput() {
    }

    static boolean shouldHandle(@Nullable GuiEventListener focusedElement, boolean searchBoxCapturesInput) {
        if (searchBoxCapturesInput) return false;
        return !(focusedElement instanceof EditBox editBox) || !editBox.canConsumeInput();
    }
}
