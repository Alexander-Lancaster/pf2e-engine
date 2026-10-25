package dev.ttrpg.pf2e;
import java.util.*;
/** Authoritative current battle with an append-only command history. */
public record GameState(BattleState battle, long revision, List<TransitionRecord> history) {
    public GameState {
        Objects.requireNonNull(battle); history = List.copyOf(history);
        if (revision < 0 || revision != history.size()) throw new IllegalArgumentException("Revision/history mismatch");
        for (int i = 0; i < history.size(); i++)
            if (history.get(i).revision() != i + 1L) throw new IllegalArgumentException("History must be consecutive");
    }
    public static GameState create(String name, List<Combatant> participants) {
        return new GameState(BattleState.create(name, participants), 0, List.of());
    }
    /** Character sheet and combat UI both read this current snapshot. */
    public GameCharacter character(UUID id) { return battle.combatant(id).character(); }
}
