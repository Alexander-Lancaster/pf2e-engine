package dev.ttrpg.pf2e;
import java.util.Objects;
public record GameTransition(GameState state, TransitionRecord record) {
    public GameTransition {
        Objects.requireNonNull(state); Objects.requireNonNull(record);
        if (state.history().isEmpty() || !state.history().getLast().equals(record))
            throw new IllegalArgumentException("Transition must be in state history");
    }
}
