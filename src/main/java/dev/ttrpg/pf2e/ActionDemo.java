package dev.ttrpg.pf2e;
import java.util.*;

/** Run with: java -cp target/classes dev.ttrpg.pf2e.ActionDemo */
public final class ActionDemo {
    private ActionDemo() {}
    public static Combatant fighter(String name) {
        var attributes = new EnumMap<Attribute, Integer>(Attribute.class);
        for (var attribute : Attribute.values()) attributes.put(attribute, 0);
        attributes.put(Attribute.STRENGTH, 4);
        var skills = new EnumMap<Skill, Integer>(Skill.class);
        for (var skill : Skill.values()) skills.put(skill, 0);
        var stats = new CharacterStatistics(attributes, 18, 5, 7, 5, 3, 25, skills, Map.of());
        return new Combatant(GameCharacter.create(name, 1, stats, 24),
                Map.of("sword", new AttackProfile("Sword", 9, 1, 8, 4, false),
                       "dagger", new AttackProfile("Dagger", 9, 1, 4, 4, true)),
                Optional.of(new ShieldProfile("Steel shield", 2, true, false)), false, List.of());
    }
    public static void main(String[] args) {
        var hero = fighter("Valeros"); var foe = fighter("Bandit");
        UUID heroId = hero.character().id(), foeId = foe.character().id();
        var state = BattleState.create("Shield demonstration", List.of(hero, foe)).startRound().startTurn(heroId);
        var engine = ActionEngine.standard();
        var faces = new ArrayDeque<>(List.of(12, 5, 10, 20, 4));
        DiceRoller dice = sides -> faces.removeFirst();
        state = show(engine.resolve(state, ActionRequest.strike(heroId, foeId, "sword"), dice));
        state = show(engine.resolve(state, ActionRequest.raiseShield(heroId), dice));
        state = state.finishTurn().startTurn(foeId);
        state = show(engine.resolve(state, ActionRequest.strike(foeId, heroId, "sword"), dice));
        state = show(engine.resolve(state, ActionRequest.strike(foeId, heroId, "sword"), dice));
        state = state.finishTurn().startRound().startTurn(heroId);
        System.out.println("Next turn: Valeros AC returns to " + state.combatant(heroId).armorClass().total());
        state = state.finishTurn().finish();
        System.out.println("Encounter completed: " + state.encounter().completed());
    }
    private static BattleState show(ActionResolution result) {
        System.out.println(result.record().description());
        for (var roll : result.record().rolls()) {
            System.out.println("  " + roll.calculation().statistic() + " dice=" + roll.calculation().dice()
                    + " total=" + roll.calculation().total() + " " + roll.outcome().map(Enum::name).orElse(""));
            for (var modifier : roll.calculation().breakdown()) System.out.println("    " + modifier);
            roll.difficultyClass().ifPresent(dc -> System.out.println("    vs AC " + dc.total() + " " + dc.breakdown()));
        }
        for (var participant : result.state().combatants().values())
            System.out.println("  " + participant.character().name() + ": HP " + participant.character().resources().hitPoints().current()
                    + ", AC " + participant.armorClass().total());
        System.out.println("  Actions remaining: " + result.state().actionsRemaining());
        return result.state();
    }
}
