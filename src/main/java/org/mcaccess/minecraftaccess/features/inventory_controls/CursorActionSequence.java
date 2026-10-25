package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.function.BooleanSupplier;

public final class CursorActionSequence {
    private boolean active;

    public boolean begin(BooleanSupplier syncCursor, Runnable press) {
        if (active) return false;
        if (!syncCursor.getAsBoolean()) return false;

        press.run();
        active = true;
        return true;
    }

    public boolean end(Runnable release) {
        if (!active) return false;

        release.run();
        active = false;
        return true;
    }
}
