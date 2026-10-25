package dev.ttrpg.pf2e;
import java.util.*;
/** Pure state transition boundary. Persist/publish the returned state atomically. */
public final class GameEngine {
    private final ActionEngine actions;
    public GameEngine(ActionEngine actions) { this.actions = Objects.requireNonNull(actions); }
    public static GameEngine standard() { return new GameEngine(ActionEngine.standard()); }
    public GameTransition execute(GameState state, long expectedRevision, GameCommand command, DiceRoller dice) {
        Objects.requireNonNull(state); Objects.requireNonNull(command); Objects.requireNonNull(dice);
        if (expectedRevision != state.revision()) throw new IllegalStateException("Stale game revision");
        if (state.battle().encounter().completed()) throw new IllegalStateException("Encounter completed");
        BattleState before = state.battle();
        BattleState after;
        Optional<ActionRecord> action = Optional.empty();
        switch (command) {
            case GameCommand.StartRound ignored -> after = before.startRound();
            case GameCommand.StartTurn start -> after = before.startTurn(start.actorId());
            case GameCommand.EndTurn ignored -> after = before.finishTurn();
            case GameCommand.EndEncounter ignored -> after = before.finish();
            case GameCommand.PerformAction perform -> {
                var result = actions.resolve(before, perform.request(), dice);
                after = result.state(); action = Optional.of(result.record());
            }
            case GameCommand.ApplyDamage damage -> {
                var target = before.combatant(damage.targetId());
                var combatants = new HashMap<>(before.combatants());
                combatants.put(damage.targetId(), target.withCharacter(target.character().damage(damage.amount())));
                after = new BattleState(before.encounter(), combatants, before.actionsRemaining(), before.attacksMade());
            }
        }
        var record = new TransitionRecord(Math.incrementExact(state.revision()), command, action, StateChange.between(before, after));
        return new GameTransition(new GameState(after, record.revision(), Checks.append(state.history(), record)), record);
    }
}
