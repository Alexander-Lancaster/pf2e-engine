package dev.ttrpg.pf2e;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
public final class GameStateTest {
    private static final GameEngine ENGINE = GameEngine.standard();
    private static final DiceRoller NONE = sides -> { throw new AssertionError("Unexpected dice"); };
    private static GameState run(GameState state, GameCommand command) {
        return ENGINE.execute(state, state.revision(), command, NONE).state();
    }
    @Test
    void commandsCommitStateChangesAndPreserveOriginalSnapshots() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var effect = new ActiveEffect(UUID.randomUUID(), "Blessing", ActiveEffect.Statistic.ATTACK,
                new Modifier("Blessing", ModifierType.STATUS, 1), Optional.of(bid));
        a = a.withEffects(List.of(effect));
        var original = GameState.create("Test", List.of(a, b));
        var started = run(run(original, new GameCommand.StartRound()), new GameCommand.StartTurn(aid));
        assertEquals(2L, started.revision()); assertEquals(0L, original.revision());
        var faces = new ArrayDeque<>(List.of(12, 5));
        var hit = ENGINE.execute(started, 2, new GameCommand.PerformAction(ActionRequest.strike(aid, bid, "sword")), sides -> faces.removeFirst());
        assertEquals(22, hit.record().action().orElseThrow().rolls().getFirst().calculation().total());
        assertEquals(15, hit.state().character(bid).resources().hitPoints().current());
        assertEquals(24, started.character(bid).resources().hitPoints().current());
        var hpChange = (StateChange.Participant) hit.record().changes().stream().filter(c -> c instanceof StateChange.Participant).findFirst().orElseThrow();
        assertEquals(24, hpChange.before().character().resources().hitPoints().current());
        assertEquals(15, hpChange.after().character().resources().hitPoints().current());
        assertEquals(true, hit.record().changes().contains(new StateChange.TurnBudget(3, 2, 0, 1)));
    }

    @Test
    void staleAndInvalidCommandsAreRejectedBeforeRolling() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var effect = new ActiveEffect(UUID.randomUUID(), "Blessing", ActiveEffect.Statistic.ATTACK,
                new Modifier("Blessing", ModifierType.STATUS, 1), Optional.of(bid));
        a = a.withEffects(List.of(effect));
        var original = GameState.create("Test", List.of(a, b));
        var started = run(run(original, new GameCommand.StartRound()), new GameCommand.StartTurn(aid));
         
        var faces = new ArrayDeque<>(List.of(12, 5));
        var hit = ENGINE.execute(started, 2, new GameCommand.PerformAction(ActionRequest.strike(aid, bid, "sword")), sides -> faces.removeFirst());
        assertThrows(IllegalStateException.class, () -> ENGINE.execute(hit.state(), 2, new GameCommand.PerformAction(ActionRequest.strike(aid, bid, "sword")), NONE));
        assertEquals(3L, hit.state().revision());
        assertThrows(IllegalStateException.class, () -> run(started, new GameCommand.StartTurn(bid)));
        assertThrows(IllegalStateException.class, () -> run(started, new GameCommand.PerformAction(ActionRequest.strike(bid, aid, "sword"))));
    }

    @Test
    void effectsExpireAcrossParticipantsAndPreserveHistory() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var effect = new ActiveEffect(UUID.randomUUID(), "Blessing", ActiveEffect.Statistic.ATTACK,
                new Modifier("Blessing", ModifierType.STATUS, 1), Optional.of(bid));
        a = a.withEffects(List.of(effect));
        var original = GameState.create("Test", List.of(a, b));
        var started = run(run(original, new GameCommand.StartRound()), new GameCommand.StartTurn(aid));
         
        var faces = new ArrayDeque<>(List.of(12, 5));
        var hit = ENGINE.execute(started, 2, new GameCommand.PerformAction(ActionRequest.strike(aid, bid, "sword")), sides -> faces.removeFirst());

        var shield = run(hit.state(), new GameCommand.PerformAction(ActionRequest.raiseShield(aid)));
        assertEquals(20, shield.battle().combatant(aid).armorClass().total());
        var enemyTurn = run(run(shield, new GameCommand.EndTurn()), new GameCommand.StartTurn(bid));
        assertEquals(20, enemyTurn.battle().combatant(aid).armorClass().total());
        assertEquals(false, enemyTurn.battle().combatant(aid).effects().contains(effect));
        assertEquals(true, enemyTurn.history().getLast().changes().stream().anyMatch(c -> c instanceof StateChange.Participant));
        var missed = ENGINE.execute(enemyTurn, enemyTurn.revision(), new GameCommand.PerformAction(ActionRequest.strike(bid, aid, "sword")), sides -> 10).state();
        var historical = missed.history().getLast().action().orElseThrow().rolls().getFirst();
        var next = run(run(run(missed, new GameCommand.EndTurn()), new GameCommand.StartRound()), new GameCommand.StartTurn(aid));
        assertEquals(18, next.battle().combatant(aid).armorClass().total()); assertEquals(20, historical.difficultyClass().orElseThrow().total());
        assertEquals(3, next.battle().actionsRemaining()); assertEquals(0, next.battle().attacksMade());
    }

    @Test
    void gmDamageAdjustmentsPreserveBudgetAndHistoryIsImmutable() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var effect = new ActiveEffect(UUID.randomUUID(), "Blessing", ActiveEffect.Statistic.ATTACK,
                new Modifier("Blessing", ModifierType.STATUS, 1), Optional.of(bid));
        a = a.withEffects(List.of(effect));
        var original = GameState.create("Test", List.of(a, b));
        var started = run(run(original, new GameCommand.StartRound()), new GameCommand.StartTurn(aid));
         
        var faces = new ArrayDeque<>(List.of(12, 5));
        var hit = ENGINE.execute(started, 2, new GameCommand.PerformAction(ActionRequest.strike(aid, bid, "sword")), sides -> faces.removeFirst());

        var shield = run(hit.state(), new GameCommand.PerformAction(ActionRequest.raiseShield(aid)));
        
        var enemyTurn = run(run(shield, new GameCommand.EndTurn()), new GameCommand.StartTurn(bid));

        var missed = ENGINE.execute(enemyTurn, enemyTurn.revision(), new GameCommand.PerformAction(ActionRequest.strike(bid, aid, "sword")), sides -> 10).state();
        var next = run(run(run(missed, new GameCommand.EndTurn()), new GameCommand.StartRound()), new GameCommand.StartTurn(aid));

        var adjusted = run(next, new GameCommand.ApplyDamage(aid, 4, "GM adjustment"));
        assertEquals(20, adjusted.character(aid).resources().hitPoints().current());
        assertEquals(3, adjusted.battle().actionsRemaining());
        assertEquals(Optional.empty(), adjusted.history().getLast().action());
        assertThrows(IllegalArgumentException.class, () -> run(adjusted, new GameCommand.ApplyDamage(UUID.randomUUID(), 1, "Unknown")));
        assertThrows(IllegalArgumentException.class, () -> new GameCommand.ApplyDamage(aid, -1, "Invalid"));
        assertThrows(IllegalArgumentException.class, () -> new GameState(original.battle(), 1, List.of()));
        assertThrows(UnsupportedOperationException.class, () -> adjusted.history().clear());
        assertThrows(UnsupportedOperationException.class, () -> adjusted.battle().combatant(aid).effects().clear());
    }

    @Test
    void completedEncountersRejectCommandsAndRollerFailuresAreAtomic() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var effect = new ActiveEffect(UUID.randomUUID(), "Blessing", ActiveEffect.Statistic.ATTACK,
                new Modifier("Blessing", ModifierType.STATUS, 1), Optional.of(bid));
        a = a.withEffects(List.of(effect));
        var original = GameState.create("Test", List.of(a, b));
        var started = run(run(original, new GameCommand.StartRound()), new GameCommand.StartTurn(aid));
         
        var faces = new ArrayDeque<>(List.of(12, 5));
        var hit = ENGINE.execute(started, 2, new GameCommand.PerformAction(ActionRequest.strike(aid, bid, "sword")), sides -> faces.removeFirst());

        var shield = run(hit.state(), new GameCommand.PerformAction(ActionRequest.raiseShield(aid)));
        
        var enemyTurn = run(run(shield, new GameCommand.EndTurn()), new GameCommand.StartTurn(bid));

        var missed = ENGINE.execute(enemyTurn, enemyTurn.revision(), new GameCommand.PerformAction(ActionRequest.strike(bid, aid, "sword")), sides -> 10).state();
        var next = run(run(run(missed, new GameCommand.EndTurn()), new GameCommand.StartRound()), new GameCommand.StartTurn(aid));

        var adjusted = run(next, new GameCommand.ApplyDamage(aid, 4, "GM adjustment"));

        var ended = run(run(adjusted, new GameCommand.EndTurn()), new GameCommand.EndEncounter());
        assertThrows(IllegalStateException.class, () -> run(ended, new GameCommand.ApplyDamage(aid, 1, "After end")));
        assertThrows(IllegalStateException.class, () -> run(ended, new GameCommand.StartRound()));
        // A throwing dice provider must not publish partial HP, budgets, or history.
        assertThrows(IllegalStateException.class, () -> ENGINE.execute(started, started.revision(), new GameCommand.PerformAction(ActionRequest.strike(aid, bid, "sword")), sides -> { throw new IllegalStateException("Roller offline"); }));
        assertEquals(3, started.battle().actionsRemaining()); assertEquals(2L, started.revision());
        assertEquals(ended.revision(), (long) ended.history().size());
    }

}
