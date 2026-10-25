package dev.ttrpg.pf2e;
import java.util.*;
/** Commands are requests, not records of completed events. */
public sealed interface GameCommand {
    record StartRound() implements GameCommand {}
    record StartTurn(UUID actorId) implements GameCommand { public StartTurn { Objects.requireNonNull(actorId); } }
    record EndTurn() implements GameCommand {}
    record EndEncounter() implements GameCommand {}
    record PerformAction(ActionRequest request) implements GameCommand { public PerformAction { Objects.requireNonNull(request); } }
    /** Explicit GM adjustment; authorization belongs to the future application layer. */
    record ApplyDamage(UUID targetId, int amount, String reason) implements GameCommand {
        public ApplyDamage { Objects.requireNonNull(targetId); ResourcePool.requireNonnegative(amount); reason = Checks.text(reason); }
    }
}
