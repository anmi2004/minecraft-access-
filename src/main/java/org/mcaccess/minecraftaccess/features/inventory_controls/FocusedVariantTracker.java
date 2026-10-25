package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.Objects;

public final class FocusedVariantTracker<F, V> {
    private F focus;
    private V variant;

    public boolean update(F focus, V variant) {
        boolean changed = !Objects.equals(this.focus, focus) || !Objects.equals(this.variant, variant);
        this.focus = focus;
        this.variant = variant;
        return changed;
    }

    public void reset() {
        focus = null;
        variant = null;
    }
}
