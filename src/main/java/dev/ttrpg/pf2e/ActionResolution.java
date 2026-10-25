package dev.ttrpg.pf2e;
import java.util.Objects;
/** New state and the resolved action appended to its history. */
public record ActionResolution(BattleState state, ActionRecord record) {
    public ActionResolution { Objects.requireNonNull(state); Objects.requireNonNull(record); }
}
