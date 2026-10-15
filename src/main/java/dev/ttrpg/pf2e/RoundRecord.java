package dev.ttrpg.pf2e;
import java.util.*;
public record RoundRecord(int number, List<TurnRecord> turns) {
    public RoundRecord {
        if (number < 1) throw new IllegalArgumentException("Rounds start at 1");
        turns = List.copyOf(turns); Checks.unique(turns.stream().map(TurnRecord::id).toList());
        for (int i = 0; i < turns.size() - 1; i++)
            if (!turns.get(i).completed()) throw new IllegalArgumentException("Only the last turn may be active");
    }
    public RoundRecord startTurn(UUID actorId) {
        if (!turns.isEmpty() && !turns.getLast().completed()) throw new IllegalStateException("Finish the active turn first");
        return new RoundRecord(number, Checks.append(turns, TurnRecord.start(actorId)));
    }
    RoundRecord replaceActive(TurnRecord turn) {
        var copy = new ArrayList<>(turns); copy.set(copy.size() - 1, turn);
        return new RoundRecord(number, copy);
    }
    public TurnRecord activeTurn() {
        if (turns.isEmpty() || turns.getLast().completed()) throw new IllegalStateException("No active turn");
        return turns.getLast();
    }
}
