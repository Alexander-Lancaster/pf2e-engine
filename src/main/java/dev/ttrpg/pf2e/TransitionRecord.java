package dev.ttrpg.pf2e;
import java.util.*;
/** A committed command, its action (if any), and its frozen consequences. */
public record TransitionRecord(long revision, GameCommand command, Optional<ActionRecord> action,
        List<StateChange> changes) {
    public TransitionRecord {
        if (revision < 1) throw new IllegalArgumentException("Revision must be positive");
        Objects.requireNonNull(command); Objects.requireNonNull(action); changes = List.copyOf(changes);
    }
}
