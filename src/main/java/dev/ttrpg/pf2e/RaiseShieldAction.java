package dev.ttrpg.pf2e;
import java.util.*;
public final class RaiseShieldAction implements ActionDefinition {
    public String id() { return "raise-shield"; }
    public String name() { return "Raise a Shield"; }
    public int actionCost() { return 1; }
    public ActionResolution resolve(BattleState state, ActionRequest request, DiceRoller dice) {
        if (!id().equals(request.actionId()) || request.targetId().isPresent() || request.attackId().isPresent())
            throw new IllegalArgumentException("Raise Shield takes no target or attack");
        state.requireAction(request.actorId(), actionCost());
        var actor = state.combatant(request.actorId());
        var shield = actor.shield().orElseThrow(() -> new IllegalStateException("No shield equipped"));
        if (!shield.wielded() || shield.broken()) throw new IllegalStateException("Shield is not usable");
        var record = new ActionRecord(UUID.randomUUID(), request.actorId(), name() + ": " + shield.name(),
                ActionKind.ACTION, actionCost(), List.of(), List.of());
        return new ActionResolution(state.apply(record, actor.withShieldRaised(true), false), record);
    }
}
