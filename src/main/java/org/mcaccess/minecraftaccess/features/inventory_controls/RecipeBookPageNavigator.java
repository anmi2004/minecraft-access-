package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public final class RecipeBookPageNavigator {
    private RecipeBookPageNavigator() {
    }

    public static @Nullable SlotItem nearestInEdgeRow(List<SlotItem> slots, boolean topRow, int referenceX) {
        if (slots.isEmpty()) return null;

        int edgeY = topRow
                ? slots.stream().mapToInt(slot -> slot.y).min().orElseThrow()
                : slots.stream().mapToInt(slot -> slot.y).max().orElseThrow();

        SlotItem nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (SlotItem slot : slots) {
            if (slot.y != edgeY) continue;

            int distance = Math.abs(slot.x - referenceX);
            if (distance < nearestDistance) {
                nearest = slot;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
