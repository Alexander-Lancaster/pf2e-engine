package dev.ttrpg.pf2e;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
public final class ActionTest {
    private static final ActionEngine ENGINE = ActionEngine.standard();
    private static final DiceRoller NO_DICE = sides -> { throw new AssertionError("Unexpected roll"); };
    private static DiceRoller dice(Integer... faces) {
        var queue = new ArrayDeque<>(List.of(faces));
        return sides -> queue.removeFirst();
    }
    @Test
    void strikeSpendsActionAndAppliesDamage() {

        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var initial = BattleState.create("Test", List.of(a, b)).startRound().startTurn(aid);
        var hit = ENGINE.resolve(initial, ActionRequest.strike(aid, bid, "sword"), dice(12, 5));
        assertEquals(15, hit.state().combatant(bid).character().resources().hitPoints().current());
        assertEquals(24, initial.combatant(bid).character().resources().hitPoints().current());
        assertEquals(2, hit.state().actionsRemaining()); assertEquals(1, hit.state().attacksMade());
        assertEquals(2, hit.record().rolls().size());
        assertEquals(hit.record(), hit.state().encounter().rounds().getLast().activeTurn().actions().getLast());
    }

    @Test
    void multipleAttacksUseSelectedWeaponsPenalty() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var initial = BattleState.create("Test", List.of(a, b)).startRound().startTurn(aid);
        var hit = ENGINE.resolve(initial, ActionRequest.strike(aid, bid, "sword"), dice(12, 5));
        var second = ENGINE.resolve(hit.state(), ActionRequest.strike(aid, bid, "dagger"), dice(2));
        assertEquals(-4, second.record().rolls().getFirst().calculation().modifiers().getFirst().value());
        assertEquals(1, second.record().rolls().size()); assertEquals(2, second.state().attacksMade());
        var third = ENGINE.resolve(second.state(), ActionRequest.strike(aid, bid, "sword"), dice(2));
        assertEquals(-10, third.record().rolls().getFirst().calculation().modifiers().getFirst().value());
        assertEquals(0, third.state().actionsRemaining());
        assertThrows(IllegalStateException.class, () -> ENGINE.resolve(third.state(), ActionRequest.raiseShield(aid), NO_DICE));
    }

    @Test
    void criticalStrikeDoublesDamage() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var initial = BattleState.create("Test", List.of(a, b)).startRound().startTurn(aid);
        var critical = ENGINE.resolve(initial, ActionRequest.strike(aid, bid, "sword"), dice(20, 5));
        assertEquals(18, critical.record().rolls().getLast().calculation().total());
        assertEquals(6, critical.state().combatant(bid).character().resources().hitPoints().current());
    }

    @Test
    void shieldExpiresAtOwnersNextTurnAndFreezesHistoricalAc() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var initial = BattleState.create("Test", List.of(a, b)).startRound().startTurn(aid);
        var hit = ENGINE.resolve(initial, ActionRequest.strike(aid, bid, "sword"), dice(12, 5));
        var raised = ENGINE.resolve(hit.state(), ActionRequest.raiseShield(aid), NO_DICE);
        assertEquals(20, raised.state().combatant(aid).armorClass().total()); assertEquals(1, raised.state().attacksMade());
        var reraised = ENGINE.resolve(raised.state(), ActionRequest.raiseShield(aid), NO_DICE);
        assertEquals(20, reraised.state().combatant(aid).armorClass().total());
        var enemyTurn = raised.state().finishTurn().startTurn(bid);
        assertEquals(20, enemyTurn.combatant(aid).armorClass().total());
        var miss = ENGINE.resolve(enemyTurn, ActionRequest.strike(bid, aid, "sword"), dice(10));
        assertEquals(DegreeOfSuccess.FAILURE, miss.record().rolls().getFirst().outcome().orElseThrow());
        var next = miss.state().finishTurn().startRound().startTurn(aid);
        assertEquals(18, next.combatant(aid).armorClass().total()); assertEquals(3, next.actionsRemaining()); assertEquals(0, next.attacksMade());
        assertEquals(20, miss.record().rolls().getFirst().difficultyClass().orElseThrow().total());
    }

    @Test
    void shieldCompetesWithCover() {
        var a = ActionDemo.fighter("A");
        var cover = new Combatant(a.character(), a.attacks(), a.shield(), true,
                List.of(new Modifier("Cover", ModifierType.CIRCUMSTANCE, 4)));
        assertEquals(22, cover.armorClass().total());
        assertEquals(false, cover.armorClass().breakdown().getLast().applied());
    }

    @Test
    void invalidActionsAreRejectedBeforeRolling() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var initial = BattleState.create("Test", List.of(a, b)).startRound().startTurn(aid);
        assertThrows(IllegalStateException.class, () -> ENGINE.resolve(initial, ActionRequest.strike(bid, aid, "sword"), NO_DICE));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.resolve(initial, ActionRequest.strike(aid, UUID.randomUUID(), "sword"), NO_DICE));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.resolve(initial, ActionRequest.strike(aid, bid, "missing"), NO_DICE));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.resolve(initial, ActionRequest.strike(aid, aid, "sword"), NO_DICE));
        assertThrows(IllegalStateException.class, () -> ENGINE.resolve(initial.finishTurn(), ActionRequest.raiseShield(aid), NO_DICE));
        assertThrows(IllegalStateException.class, () -> ENGINE.resolve(initial.finishTurn().finish(), ActionRequest.raiseShield(aid), NO_DICE));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.resolve(initial, new ActionRequest("unknown", aid, Optional.empty(), Optional.empty()), NO_DICE));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.resolve(initial, ActionRequest.strike(aid, bid, "sword"), dice(21)));
    }

    @Test
    void unusableShieldsAreRejected() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        for (var shield : List.of(Optional.<ShieldProfile>empty(), Optional.of(new ShieldProfile("Broken", 2, true, true)), Optional.of(new ShieldProfile("Stowed", 2, false, false)))) {
            var unequipped = new Combatant(a.character(), a.attacks(), shield, false, List.of());
            var state = BattleState.create("Test", List.of(unequipped, b)).startRound().startTurn(aid);
            assertThrows(IllegalStateException.class, () -> ENGINE.resolve(state, ActionRequest.raiseShield(aid), NO_DICE));
        }
    }

    @Test
    void damageHasMinimumOfOne() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var weak = new Combatant(a.character(), Map.of("weak", new AttackProfile("Weak", 9, 1, 4, -10, false)), a.shield(), false, List.of());
        var weakState = BattleState.create("Weak", List.of(weak, b)).startRound().startTurn(aid);
        assertEquals(1, ENGINE.resolve(weakState, ActionRequest.strike(aid, bid, "weak"), dice(12, 1)).record().rolls().getLast().calculation().total());
    }

    @Test
    void temporaryHitPointsAbsorbStrikeDamage() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();

        for (var shield : List.of(Optional.<ShieldProfile>empty(), Optional.of(new ShieldProfile("Broken", 2, true, true)), Optional.of(new ShieldProfile("Stowed", 2, false, false)))) {
            var unequipped = new Combatant(a.character(), a.attacks(), shield, false, List.of());
            var state = BattleState.create("Test", List.of(unequipped, b)).startRound().startTurn(aid);
            
        }
        var temp = b.withCharacter(b.character().withResources(b.character().resources().withHitPoints(new HitPoints(24, 24, 5))));
        var tempState = BattleState.create("Temp", List.of(a, temp)).startRound().startTurn(aid);
        assertEquals(new HitPoints(24, 20, 0), ENGINE.resolve(tempState, ActionRequest.strike(aid, bid, "sword"), dice(12, 5)).state().combatant(bid).character().resources().hitPoints());
    }

    @Test
    void zeroHpActorsCannotActAndRegistryRejectsDuplicates() {
        var a = ActionDemo.fighter("A"); var b = ActionDemo.fighter("B");
        UUID aid = a.character().id(), bid = b.character().id();
        var initial = BattleState.create("Test", List.of(a, b)).startRound().startTurn(aid);
        var down = a.withCharacter(a.character().damage(100));
        var downState = BattleState.create("Down", List.of(down, b)).startRound().startTurn(aid);
        assertThrows(IllegalStateException.class, () -> ENGINE.resolve(downState, ActionRequest.raiseShield(aid), NO_DICE));
        assertThrows(IllegalArgumentException.class, () -> new ActionEngine(List.of(new StrikeAction(), new StrikeAction())));
        assertEquals(3, initial.actionsRemaining()); assertEquals(0, initial.encounter().rounds().getLast().activeTurn().actions().size());
    }

}
