package dev.ttrpg.pf2e;
import java.util.*;

/** Immutable encounter history. Turn order is explicitly selected by the caller. */
public record Encounter(UUID id, String name, Set<UUID> participantIds,
        boolean completed, List<RoundRecord> rounds) {
    public Encounter {
        Objects.requireNonNull(id); name = Checks.text(name);
        participantIds = Set.copyOf(participantIds); rounds = List.copyOf(rounds);
        var turnIds = new ArrayList<UUID>(); var actionIds = new ArrayList<UUID>(); var rollIds = new ArrayList<UUID>();
        for (int i = 0; i < rounds.size(); i++) {
            var round = rounds.get(i);
            if (round.number() != i + 1) throw new IllegalArgumentException("Rounds must be consecutive");
            for (var turn : round.turns()) {
                turnIds.add(turn.id()); requireParticipant(participantIds, turn.actorId());
                if (!turn.completed() && (completed || i < rounds.size() - 1))
                    throw new IllegalArgumentException("Active turn in a closed round or encounter");
                for (var action : turn.actions()) {
                    actionIds.add(action.id()); requireParticipant(participantIds, action.actorId());
                    for (var target : action.targetIds()) requireParticipant(participantIds, target);
                    for (var roll : action.rolls()) { rollIds.add(roll.id()); requireParticipant(participantIds, roll.actorId()); }
                }
            }
        }
        Checks.unique(turnIds); Checks.unique(actionIds); Checks.unique(rollIds);
    }
    private static void requireParticipant(Set<UUID> participants, UUID actor) {
        if (!participants.contains(actor)) throw new IllegalArgumentException("Unknown participant: " + actor);
    }
    public static Encounter create(String name, Set<UUID> participants) {
        return new Encounter(UUID.randomUUID(), name, participants, false, List.of());
    }
    private void requireOpen() { if (completed) throw new IllegalStateException("Encounter is completed"); }
    private RoundRecord currentRound() {
        requireOpen();
        if (rounds.isEmpty()) throw new IllegalStateException("Start a round first");
        return rounds.getLast();
    }
    public Encounter startRound() {
        requireOpen();
        if (!rounds.isEmpty() && !rounds.getLast().turns().isEmpty() && !rounds.getLast().turns().getLast().completed())
            throw new IllegalStateException("Finish the active turn first");
        return new Encounter(id, name, participantIds, false, Checks.append(rounds, new RoundRecord(rounds.size() + 1, List.of())));
    }
    private Encounter replaceRound(RoundRecord round) {
        var copy = new ArrayList<>(rounds); copy.set(copy.size() - 1, round);
        return new Encounter(id, name, participantIds, false, copy);
    }
    public Encounter startTurn(UUID actorId) { return replaceRound(currentRound().startTurn(actorId)); }
    public Encounter record(ActionRecord action) {
        var round = currentRound(); return replaceRound(round.replaceActive(round.activeTurn().record(action)));
    }
    public Encounter finishTurn() {
        var round = currentRound(); return replaceRound(round.replaceActive(round.activeTurn().finish()));
    }
    public Encounter finish() {
        requireOpen();
        if (!rounds.isEmpty() && !rounds.getLast().turns().isEmpty() && !rounds.getLast().turns().getLast().completed())
            throw new IllegalStateException("Finish the active turn first");
        return new Encounter(id, name, participantIds, true, rounds);
    }
}
