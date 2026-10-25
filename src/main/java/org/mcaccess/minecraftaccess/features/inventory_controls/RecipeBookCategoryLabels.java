package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;

public final class RecipeBookCategoryLabels {
    private static final String CATEGORY_PREFIX = "minecraft_access.recipe_book.category.";
    private static final String SEARCH_PREFIX = "minecraft_access.recipe_book.search.";
    private static final Map<ExtendedRecipeBookCategory, Component> CUSTOM_LABELS = new IdentityHashMap<>();

    private RecipeBookCategoryLabels() {
    }

    public static void register(ExtendedRecipeBookCategory category, Component label) {
        CUSTOM_LABELS.put(category, label);
    }

    public static String describe(ExtendedRecipeBookCategory category) {
        Component custom = CUSTOM_LABELS.get(category);
        if (custom != null) return custom.getString();

        if (category instanceof SearchRecipeBookCategory searchCategory) {
            String key = SEARCH_PREFIX + searchCategory.name().toLowerCase(Locale.ROOT);
            return translatedOr(key, searchCategory.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        }

        if (category instanceof RecipeBookCategory recipeBookCategory) {
            Identifier id = BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(recipeBookCategory);
            if (id != null) {
                return translatedOr(translationKey(id), fallbackLabel(id));
            }
        }

        return category.getClass().getSimpleName();
    }

    static String translationKey(Identifier id) {
        return CATEGORY_PREFIX + id.getNamespace() + "." + id.getPath();
    }

    static String fallbackLabel(Identifier id) {
        String namespace = id.getNamespace().equals("minecraft") ? "" : id.getNamespace() + " ";
        return namespace + id.getPath().replace('_', ' ');
    }

    private static String translatedOr(String key, String fallback) {
        return Language.getInstance().has(key) ? I18n.get(key) : fallback;
    }
}
