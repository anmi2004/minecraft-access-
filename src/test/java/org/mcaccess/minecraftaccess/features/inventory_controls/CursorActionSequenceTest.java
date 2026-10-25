package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CursorActionSequenceTest {
    @Test
    void beginSynchronizesBeforePressAndOnlyOnce() {
        List<String> calls = new ArrayList<>();
        CursorActionSequence sequence = new CursorActionSequence();

        assertThat(sequence.begin(() -> {
            calls.add("sync");
            return true;
        }, () -> calls.add("press"))).isTrue();
        assertThat(sequence.begin(() -> true, () -> calls.add("press-again"))).isFalse();

        assertThat(calls).containsExactly("sync", "press");
    }

    @Test
    void beginDoesNotPressWhenTargetCannotBeSynchronized() {
        List<String> calls = new ArrayList<>();
        CursorActionSequence sequence = new CursorActionSequence();

        assertThat(sequence.begin(() -> false, () -> calls.add("press"))).isFalse();

        assertThat(calls).isEmpty();
    }

    @Test
    void endReleasesWithoutMovingCursor() {
        List<String> calls = new ArrayList<>();
        CursorActionSequence sequence = new CursorActionSequence();
        sequence.begin(() -> true, () -> calls.add("press"));

        assertThat(sequence.end(() -> calls.add("release"))).isTrue();

        assertThat(calls).containsExactly("press", "release");
    }

    @Test
    void endStillReleasesAfterScreenTargetDisappears() {
        List<String> calls = new ArrayList<>();
        CursorActionSequence sequence = new CursorActionSequence();
        sequence.begin(() -> true, () -> calls.add("press"));

        assertThat(sequence.end(() -> calls.add("release-after-close"))).isTrue();

        assertThat(calls).containsExactly("press", "release-after-close");
    }
}
