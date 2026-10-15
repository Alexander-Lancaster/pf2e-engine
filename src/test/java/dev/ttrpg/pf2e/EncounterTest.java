package dev.ttrpg.pf2e;

import java.util.*;

public final class EncounterTest {
    private static int checks;
    public static void main(String[] args) {
        var modifiers = new ArrayList<>(List.of(
                new Modifier("Shield", ModifierType.CIRCUMSTANCE, 2),
                new Modifier("Cover", ModifierType.CIRCUMSTANCE, 1),
                new Modifier("Bless", ModifierType.STATUS, 1),
                new Modifier("Fear", ModifierType.STATUS, -2),
                new Modifier("Lesser fear", ModifierType.STATUS, -1),
                new Modifier("MAP", ModifierType.UNTYPED, -5),
                new Modifier("Other penalty", ModifierType.UNTYPED, -1),
                new Modifier("Equal shield", ModifierType.CIRCUMSTANCE, 2)));
        var calc = new CalculationRecord("Attack", List.of(new BaseComponent("Strength", 4), new BaseComponent("Proficiency", 7)),
                List.of(new DieRoll(20, 14)), modifiers);
        equal(20, calc.total());
        equal(List.of(2, 0, 1, -2, 0, -5, -1, 0), calc.breakdown().stream().map(ModifierContribution::contribution).toList());
        equal("Suppressed by Shield", calc.breakdown().get(7).explanation());
        modifiers.clear(); equal(20, calc.total());
        fails(UnsupportedOperationException.class, () -> calc.modifiers().clear());
        fails(IllegalArgumentException.class, () -> new Modifier("Invalid", ModifierType.UNTYPED, 2));
        fails(IllegalArgumentException.class, () -> new DieRoll(20, 21));
        fails(IllegalArgumentException.class, () -> new DieRoll(1, 1));
        UUID hero = UUID.randomUUID(), enemy = UUID.randomUUID();
        equal(DegreeOfSuccess.CRITICAL_SUCCESS, check(hero, 10, 20, 20).outcome().orElseThrow());
        equal(DegreeOfSuccess.SUCCESS, check(hero, 10, 10, 20).outcome().orElseThrow());
        equal(DegreeOfSuccess.FAILURE, check(hero, 9, 10, 20).outcome().orElseThrow());
        equal(DegreeOfSuccess.CRITICAL_FAILURE, check(hero, 10, 0, 20).outcome().orElseThrow());
        equal(DegreeOfSuccess.FAILURE, check(hero, 20, 0, 40).outcome().orElseThrow());
        equal(DegreeOfSuccess.SUCCESS, check(hero, 1, 30, 20).outcome().orElseThrow());
        equal(DegreeOfSuccess.CRITICAL_SUCCESS, check(hero, 20, 30, 20).outcome().orElseThrow());
        equal(DegreeOfSuccess.CRITICAL_FAILURE, check(hero, 1, 0, 20).outcome().orElseThrow());
        var damage = new RollRecord(UUID.randomUUID(), hero, "Slashing damage",
                new CalculationRecord("Damage", List.of(new BaseComponent("Strength", 4)),
                        List.of(new DieRoll(8, 3), new DieRoll(8, 5)), List.of()), Optional.empty());
        equal(12, damage.calculation().total()); equal(Optional.empty(), damage.outcome());
        fails(IllegalArgumentException.class, () -> new RollRecord(UUID.randomUUID(), hero, "Bad check", damage.calculation(), Optional.of(dc(20))));
        var rolls = new ArrayList<>(List.of(check(hero, 14, 7, 18), damage));
        var strike = new ActionRecord(UUID.randomUUID(), hero, "Strike", ActionKind.ACTION, 1, List.of(enemy), rolls);
        rolls.clear(); equal(2, strike.rolls().size());
        var reaction = new ActionRecord(UUID.randomUUID(), enemy, "Reaction", ActionKind.REACTION, 0, List.of(hero), List.of());
        var encounter = Encounter.create("Ambush", Set.of(hero, enemy));
        fails(IllegalStateException.class, () -> encounter.startTurn(hero));
        var active = encounter.startRound().startTurn(hero);
        var recorded = active.record(strike).record(reaction);
        equal(0, active.rounds().getFirst().turns().getFirst().actions().size());
        equal(2, recorded.rounds().getFirst().turns().getFirst().actions().size());
        equal(encounter.id(), recorded.id());
        fails(IllegalStateException.class, recorded::startRound);
        fails(IllegalStateException.class, () -> recorded.startTurn(enemy));
        fails(IllegalStateException.class, recorded::finish);
        fails(IllegalArgumentException.class, () -> recorded.record(strike));
        fails(IllegalArgumentException.class, () -> active.record(new ActionRecord(UUID.randomUUID(), enemy, "Wrong actor", ActionKind.ACTION, 1, List.of(), List.of())));
        fails(IllegalArgumentException.class, () -> active.record(new ActionRecord(UUID.randomUUID(), hero, "Unknown target", ActionKind.ACTION, 1, List.of(UUID.randomUUID()), List.of())));
        var finishedTurn = recorded.finishTurn();
        fails(IllegalStateException.class, () -> finishedTurn.record(reaction));
        fails(IllegalArgumentException.class, () -> finishedTurn.startTurn(UUID.randomUUID()));
        var secondRound = finishedTurn.startTurn(enemy).finishTurn().startRound().startTurn(hero);
        equal(2, secondRound.rounds().getLast().number());
        var finished = secondRound.finishTurn().finish();
        fails(IllegalStateException.class, finished::startRound);
        fails(IllegalStateException.class, () -> finished.startTurn(hero));
        fails(UnsupportedOperationException.class, () -> finished.rounds().clear());
        fails(IllegalArgumentException.class, () -> new RoundRecord(0, List.of()));
        fails(IllegalArgumentException.class, () -> new RoundRecord(1, List.of(TurnRecord.start(hero), TurnRecord.start(enemy))));
        fails(IllegalArgumentException.class, () -> new ActionRecord(UUID.randomUUID(), hero, "Bad cost", ActionKind.REACTION, 1, List.of(), List.of()));
        // Target saves can belong to a different actor from the action's caster.
        var save = check(enemy, 12, 3, 18);
        equal(enemy, active.record(new ActionRecord(UUID.randomUUID(), hero, "Spell", ActionKind.ACTION, 2, List.of(enemy), List.of(save)))
                .rounds().getFirst().turns().getFirst().actions().getFirst().rolls().getFirst().actorId());
        System.out.println("Passed " + checks + " encounter/calculation checks.");
    }
    private static CalculationRecord dc(int value) {
        return new CalculationRecord("AC", List.of(new BaseComponent("DC", value)), List.of(), List.of());
    }
    private static RollRecord check(UUID actor, int face, int bonus, int dc) {
        return new RollRecord(UUID.randomUUID(), actor, "Check",
                new CalculationRecord("Attack", List.of(new BaseComponent("Base", bonus)), List.of(new DieRoll(20, face)), List.of()), Optional.of(dc(dc)));
    }
    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
