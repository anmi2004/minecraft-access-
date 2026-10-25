package org.mcaccess.minecraftaccess.features.inventory_controls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeBookNarrationTest {
    @Test
    void formatPreservesCraftableCountAndItemNameOrder() {
        assertThat(RecipeBookNarration.format("Craftable", 4, "Oak Planks"))
                .isEqualTo("Craftable 4 Oak Planks");
    }
}
