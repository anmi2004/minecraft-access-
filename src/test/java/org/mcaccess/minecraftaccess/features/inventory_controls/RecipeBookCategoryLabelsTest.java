package org.mcaccess.minecraftaccess.features.inventory_controls;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeBookCategoryLabelsTest {
    @Test
    void registeredCategoryUsesNamespaceAndPathKey() {
        Identifier id = Identifier.fromNamespaceAndPath("example", "building_blocks");

        assertThat(RecipeBookCategoryLabels.translationKey(id))
                .isEqualTo("minecraft_access.recipe_book.category.example.building_blocks");
    }

    @Test
    void fallbackLabelPreservesReadableRegistryIdentity() {
        Identifier id = Identifier.fromNamespaceAndPath("example", "building_blocks");

        assertThat(RecipeBookCategoryLabels.fallbackLabel(id))
                .isEqualTo("example building blocks");
    }
}
