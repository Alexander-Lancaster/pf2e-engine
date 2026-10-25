package dev.ttrpg.pf2e;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public final class EncounterTest {
    @Test
    void modifiersStackByTypeAndPreserveSuppressedEntries() {

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
        assertEquals(20, calc.total());
        assertEquals(List.of(2, 0, 1, -2, 0, -5, -1, 0), calc.breakdown().stream().map(ModifierContribution::contribution).toList());
        assertEquals("Suppressed by Shield", calc.breakdown().get(7).explanation());
        modifiers.clear(); assertEquals(20, calc.total());
        assertThrows(UnsupportedOperationException.class, () -> calc.modifiers().clear());
        assertThrows(IllegalArgumentException.class, () -> new Modifier("Invalid", ModifierType.UNTYPED, 2));
        assertThrows(IllegalArgumentException.class, () -> new DieRoll(20, 21));
        assertThrows(IllegalArgumentException.class, () -> new DieRoll(1, 1));
    }

    @Test
    void checksResolveDegreesAndNaturalDieAdjustments() {

        UUID hero = UUID.randomUUID(), enemy = UUID.randomUUID();
        assertEquals(DegreeOfSuccess.CRITICAL_SUCCESS, check(hero, 10, 20, 20).outcome().orElseThrow());
        assertEquals(DegreeOfSuccess.SUCCESS, check(hero, 10, 10, 20).outcome().orElseThrow());
        assertEquals(DegreeOfSuccess.FAILURE, check(hero, 9, 10, 20).outcome().orElseThrow());
        assertEquals(DegreeOfSuccess.CRITICAL_FAILURE, check(hero, 10, 0, 20).outcome().orElseThrow());
        assertEquals(DegreeOfSuccess.FAILURE, check(hero, 20, 0, 40).outcome().orElseThrow());
        assertEquals(DegreeOfSuccess.SUCCESS, check(hero, 1, 30, 20).outcome().orElseThrow());
        assertEquals(DegreeOfSuccess.CRITICAL_SUCCESS, check(hero, 20, 30, 20).outcome().orElseThrow());
        assertEquals(DegreeOfSuccess.CRITICAL_FAILURE, check(hero, 1, 0, 20).outcome().orElseThrow());
    }

    @Test
    void damageRollsHaveNoDegreeOfSuccess() {

        UUID hero = UUID.randomUUID(), enemy = UUID.randomUUID();
        var damage = new RollRecord(UUID.randomUUID(), hero, "Slashing damage",
                new CalculationRecord("Damage", List.of(new BaseComponent("Strength", 4)),
                        List.of(new DieRoll(8, 3), new DieRoll(8, 5)), List.of()), Optional.empty());
        assertEquals(12, damage.calculation().total()); assertEquals(Optional.empty(), damage.outcome());
        assertThrows(IllegalArgumentException.class, () -> new RollRecord(UUID.randomUUID(), hero, "Bad check", damage.calculation(), Optional.of(dc(20))));
    }

    @Test
    void encountersRecordActionsAndRejectInvalidTransitions() {

        UUID hero = UUID.randomUUID(), enemy = UUID.randomUUID();

        var damage = new RollRecord(UUID.randomUUID(), hero, "Slashing damage",
                new CalculationRecord("Damage", List.of(new BaseComponent("Strength", 4)),
                        List.of(new DieRoll(8, 3), new DieRoll(8, 5)), List.of()), Optional.empty());
        var rolls = new ArrayList<>(List.of(check(hero, 14, 7, 18), damage));
        var strike = new ActionRecord(UUID.randomUUID(), hero, "Strike", ActionKind.ACTION, 1, List.of(enemy), rolls);
        rolls.clear(); assertEquals(2, strike.rolls().size());
        var reaction = new ActionRecord(UUID.randomUUID(), enemy, "Reaction", ActionKind.REACTION, 0, List.of(hero), List.of());
        var encounter = Encounter.create("Ambush", Set.of(hero, enemy));
        assertThrows(IllegalStateException.class, () -> encounter.startTurn(hero));
        var active = encounter.startRound().startTurn(hero);
        var recorded = active.record(strike).record(reaction);
        assertEquals(0, active.rounds().getFirst().turns().getFirst().actions().size());
        assertEquals(2, recorded.rounds().getFirst().turns().getFirst().actions().size());
        assertEquals(encounter.id(), recorded.id());
        assertThrows(IllegalStateException.class, recorded::startRound);
        assertThrows(IllegalStateException.class, () -> recorded.startTurn(enemy));
        assertThrows(IllegalStateException.class, recorded::finish);
        assertThrows(IllegalArgumentException.class, () -> recorded.record(strike));
        assertThrows(IllegalArgumentException.class, () -> active.record(new ActionRecord(UUID.randomUUID(), enemy, "Wrong actor", ActionKind.ACTION, 1, List.of(), List.of())));
        assertThrows(IllegalArgumentException.class, () -> active.record(new ActionRecord(UUID.randomUUID(), hero, "Unknown target", ActionKind.ACTION, 1, List.of(UUID.randomUUID()), List.of())));
        var finishedTurn = recorded.finishTurn();
        assertThrows(IllegalStateException.class, () -> finishedTurn.record(reaction));
        assertThrows(IllegalArgumentException.class, () -> finishedTurn.startTurn(UUID.randomUUID()));
        var secondRound = finishedTurn.startTurn(enemy).finishTurn().startRound().startTurn(hero);
        assertEquals(2, secondRound.rounds().getLast().number());
        var finished = secondRound.finishTurn().finish();
        assertThrows(IllegalStateException.class, finished::startRound);
        assertThrows(IllegalStateException.class, () -> finished.startTurn(hero));
        assertThrows(UnsupportedOperationException.class, () -> finished.rounds().clear());
        assertThrows(IllegalArgumentException.class, () -> new RoundRecord(0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RoundRecord(1, List.of(TurnRecord.start(hero), TurnRecord.start(enemy))));
        assertThrows(IllegalArgumentException.class, () -> new ActionRecord(UUID.randomUUID(), hero, "Bad cost", ActionKind.REACTION, 1, List.of(), List.of()));
        // Target saves can belong to a different actor from the action's caster.
        var save = check(enemy, 12, 3, 18);
        assertEquals(enemy, active.record(new ActionRecord(UUID.randomUUID(), hero, "Spell", ActionKind.ACTION, 2, List.of(enemy), List.of(save)))
                .rounds().getFirst().turns().getFirst().actions().getFirst().rolls().getFirst().actorId());
    }

    private static CalculationRecord dc(int value) {
        return new CalculationRecord("AC", List.of(new BaseComponent("DC", value)), List.of(), List.of());
    }
    private static RollRecord check(UUID actor, int face, int bonus, int dc) {
        return new RollRecord(UUID.randomUUID(), actor, "Check",
                new CalculationRecord("Attack", List.of(new BaseComponent("Base", bonus)), List.of(new DieRoll(20, face)), List.of()), Optional.of(dc(dc)));
    }
}
