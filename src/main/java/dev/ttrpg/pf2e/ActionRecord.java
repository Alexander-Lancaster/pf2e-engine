package dev.ttrpg.pf2e;
import java.util.*;
/** Resolved action; rolls may belong to targets (e.g. saving throws). */
public record ActionRecord(UUID id, UUID actorId, String description, ActionKind kind,
        int actionCost, List<UUID> targetIds, List<RollRecord> rolls) {
    public ActionRecord {
        Objects.requireNonNull(id); Objects.requireNonNull(actorId); Objects.requireNonNull(kind);
        description = Checks.text(description); targetIds = List.copyOf(targetIds); rolls = List.copyOf(rolls);
        if (kind == ActionKind.ACTION ? actionCost < 1 || actionCost > 3 : actionCost != 0)
            throw new IllegalArgumentException("Actions cost 1..3; reactions/free actions cost 0");
        Checks.unique(targetIds); Checks.unique(rolls.stream().map(RollRecord::id).toList());
    }
}
