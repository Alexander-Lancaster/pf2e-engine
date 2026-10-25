package dev.ttrpg.pf2e;
import java.util.*;
/** Typed before/after values for logs and inspection; not a replay protocol. */
public sealed interface StateChange {
    record Participant(UUID participantId, Combatant before, Combatant after) implements StateChange {
        public Participant { Objects.requireNonNull(participantId); Objects.requireNonNull(before); Objects.requireNonNull(after); }
    }
    record TurnBudget(int actionsBefore, int actionsAfter, int attacksBefore, int attacksAfter) implements StateChange {}
    record Progress(int roundBefore, int roundAfter, Optional<UUID> turnBefore,
            Optional<UUID> turnAfter, boolean completedBefore, boolean completedAfter) implements StateChange {
        public Progress { Objects.requireNonNull(turnBefore); Objects.requireNonNull(turnAfter); }
    }
    static List<StateChange> between(BattleState before, BattleState after) {
        var changes = new ArrayList<StateChange>();
        for (UUID id : before.combatants().keySet().stream().sorted().toList()) {
            var old = before.combatant(id); var current = after.combatant(id);
            if (!old.equals(current)) changes.add(new Participant(id, old, current));
        }
        if (before.actionsRemaining() != after.actionsRemaining() || before.attacksMade() != after.attacksMade())
            changes.add(new TurnBudget(before.actionsRemaining(), after.actionsRemaining(), before.attacksMade(), after.attacksMade()));
        if (round(before) != round(after) || !turn(before).equals(turn(after)) || before.encounter().completed() != after.encounter().completed())
            changes.add(new Progress(round(before), round(after), turn(before), turn(after), before.encounter().completed(), after.encounter().completed()));
        return List.copyOf(changes);
    }
    private static int round(BattleState state) { return state.encounter().rounds().size(); }
    private static Optional<UUID> turn(BattleState state) {
        if (state.encounter().rounds().isEmpty()) return Optional.empty();
        var turns = state.encounter().rounds().getLast().turns();
        return turns.isEmpty() || turns.getLast().completed() ? Optional.empty() : Optional.of(turns.getLast().id());
    }
}
