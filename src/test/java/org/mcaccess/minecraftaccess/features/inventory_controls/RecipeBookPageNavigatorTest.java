package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeBookPageNavigatorTest {
    private final List<SlotItem> slots = List.of(
            new SlotItem(10, 10, "first"),
            new SlotItem(35, 10, "second"),
            new SlotItem(10, 35, "third"),
            new SlotItem(35, 35, "fourth")
    );

    @Test
    void forwardPageTargetUsesTopRowAndClosestColumn() {
        SlotItem target = RecipeBookPageNavigator.nearestInEdgeRow(slots, true, 34);

        assertThat(target).isSameAs(slots.get(1));
    }

    @Test
    void backwardPageTargetUsesBottomRowAndClosestColumn() {
        SlotItem target = RecipeBookPageNavigator.nearestInEdgeRow(slots, false, 11);

        assertThat(target).isSameAs(slots.get(2));
    }
}
