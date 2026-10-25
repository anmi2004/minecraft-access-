package org.mcaccess.minecraftaccess.features.inventory_controls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FocusedVariantTrackerTest {
    @Test
    void reportsOnlyRealFocusOrVariantChanges() {
        FocusedVariantTracker<String, String> tracker = new FocusedVariantTracker<>();

        assertThat(tracker.update("first-button", "recipe-a")).isTrue();
        assertThat(tracker.update("first-button", "recipe-a")).isFalse();
        assertThat(tracker.update("first-button", "recipe-b")).isTrue();
        assertThat(tracker.update("second-button", "recipe-b")).isTrue();
    }

    @Test
    void resetMakesSameFocusReportAgain() {
        FocusedVariantTracker<String, String> tracker = new FocusedVariantTracker<>();

        tracker.update("button", "recipe");
        tracker.reset();

        assertThat(tracker.update("button", "recipe")).isTrue();
    }
}
