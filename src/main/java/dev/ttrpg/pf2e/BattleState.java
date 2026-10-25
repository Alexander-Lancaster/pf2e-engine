package dev.ttrpg.pf2e;
import java.util.*;
/** Current combatants plus historical encounter and active-turn counters. */
public record BattleState(Encounter encounter, Map<UUID, Combatant> combatants,
        int actionsRemaining, int attacksMade) {
    public BattleState {
        Objects.requireNonNull(encounter); combatants = Map.copyOf(combatants);
        if (!combatants.keySet().equals(encounter.participantIds())) throw new IllegalArgumentException("Participants must match");
        combatants.forEach((id, combatant) -> {
            if (!id.equals(combatant.character().id())) throw new IllegalArgumentException("Combatant key mismatch");
        });
        for (var participant : combatants.values())
            for (var effect : participant.effects())
                if (effect.expiresAtTurnStart().isPresent() && !combatants.containsKey(effect.expiresAtTurnStart().get()))
                    throw new IllegalArgumentException("Effect expiry references unknown participant");
        if (actionsRemaining < 0 || actionsRemaining > 3 || attacksMade < 0)
            throw new IllegalArgumentException("Invalid turn counters");
        boolean active = !encounter.completed() && !encounter.rounds().isEmpty()
                && !encounter.rounds().getLast().turns().isEmpty()
                && !encounter.rounds().getLast().turns().getLast().completed();
        if (!active && (actionsRemaining != 0 || attacksMade != 0))
            throw new IllegalArgumentException("Counters require an active turn");
    }
    public static BattleState create(String name, List<Combatant> participants) {
        var map = new HashMap<UUID, Combatant>();
        for (var participant : participants)
            if (map.put(participant.character().id(), participant) != null) throw new IllegalArgumentException("Duplicate participant");
        return new BattleState(Encounter.create(name, map.keySet()), map, 0, 0);
    }
    public BattleState startRound() { return new BattleState(encounter.startRound(), combatants, 0, 0); }
    public BattleState startTurn(UUID actor) {
        var history = encounter.startTurn(actor);
        var updated = new HashMap<>(combatants);
        updated.replaceAll((id, participant) -> participant.expireAtTurnStart(actor));
        return new BattleState(history, updated, 3, 0);
    }
    public BattleState finishTurn() { return new BattleState(encounter.finishTurn(), combatants, 0, 0); }
    public BattleState finish() { return new BattleState(encounter.finish(), combatants, 0, 0); }
    public Combatant combatant(UUID id) {
        var result = combatants.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown combatant");
        return result;
    }
    public void requireAction(UUID actor, int cost) {
        if (encounter.completed() || encounter.rounds().isEmpty()) throw new IllegalStateException("No active encounter turn");
        var turn = encounter.rounds().getLast().activeTurn();
        if (!turn.actorId().equals(actor)) throw new IllegalStateException("Not this actor's turn");
        if (combatant(actor).character().resources().hitPoints().current() == 0) throw new IllegalStateException("Actor has zero HP");
        if (cost < 1 || actionsRemaining < cost) throw new IllegalStateException("Insufficient actions");
    }
    BattleState apply(ActionRecord record, Combatant updated, boolean attack) {
        requireAction(record.actorId(), record.actionCost());
        var map = new HashMap<>(combatants);
        UUID updatedId = updated.character().id();
        combatant(updatedId);
        map.put(updatedId, updated);
        return new BattleState(encounter.record(record), map, actionsRemaining - record.actionCost(),
                attack ? Math.incrementExact(attacksMade) : attacksMade);
    }
}
