package dev.ttrpg.pf2e;
import java.util.*;
/** Ordered history, separate from a future action/reaction availability engine. */
public record TurnRecord(UUID id, UUID actorId, boolean completed, List<ActionRecord> actions) {
    public TurnRecord {
        Objects.requireNonNull(id); Objects.requireNonNull(actorId); actions = List.copyOf(actions);
        Checks.unique(actions.stream().map(ActionRecord::id).toList());
        for (var action : actions)
            if (action.kind() == ActionKind.ACTION && !action.actorId().equals(actorId))
                throw new IllegalArgumentException("Ordinary actions must belong to the turn owner");
    }
    public static TurnRecord start(UUID actorId) { return new TurnRecord(UUID.randomUUID(), actorId, false, List.of()); }
    public TurnRecord record(ActionRecord action) {
        if (completed) throw new IllegalStateException("Turn is completed");
        return new TurnRecord(id, actorId, false, Checks.append(actions, action));
    }
    public TurnRecord finish() { return new TurnRecord(id, actorId, true, actions); }
}
