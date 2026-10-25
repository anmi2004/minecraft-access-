package org.mcaccess.minecraftaccess.features.inventory_controls;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryScreenInputTest {
    @Test
    void inventoryShortcutWorksWhenRecipeBookHasFocus() {
        GuiEventListener focusedRecipeBook = mock(GuiEventListener.class);

        boolean result = InventoryScreenInput.shouldHandle(focusedRecipeBook, false);

        assertThat(result).isTrue();
    }

    @Test
    void inventoryShortcutDoesNotConsumeInputFromFocusedEditBox() {
        EditBox searchBox = mock(EditBox.class);
        when(searchBox.canConsumeInput()).thenReturn(true);

        boolean result = InventoryScreenInput.shouldHandle(searchBox, false);

        assertThat(result).isFalse();
    }

    @Test
    void inventoryShortcutWorksWhenEditBoxCannotConsumeInput() {
        EditBox searchBox = mock(EditBox.class);
        when(searchBox.canConsumeInput()).thenReturn(false);

        boolean result = InventoryScreenInput.shouldHandle(searchBox, false);

        assertThat(result).isTrue();
    }

    @Test
    void inventoryShortcutDoesNotConsumeInputWhileSearchBoxIsCapturingIt() {
        GuiEventListener focusedRecipeBook = mock(GuiEventListener.class);

        boolean result = InventoryScreenInput.shouldHandle(focusedRecipeBook, true);

        assertThat(result).isFalse();
    }
}
