package dev.ttrpg.pf2e;
import java.util.*;
/** Basic equipped melee/unarmed Strike. Position/reach is a caller precondition. */
public final class StrikeAction implements ActionDefinition {
    public String id() { return "strike"; }
    public String name() { return "Strike"; }
    public int actionCost() { return 1; }
    public ActionResolution resolve(BattleState state, ActionRequest request, DiceRoller dice) {
        if (!id().equals(request.actionId())) throw new IllegalArgumentException("Wrong action ID");
        state.requireAction(request.actorId(), actionCost());
        UUID targetId = request.targetId().orElseThrow(() -> new IllegalArgumentException("Strike needs a target"));
        if (targetId.equals(request.actorId())) throw new IllegalArgumentException("Self-targeting is not supported");
        var target = state.combatant(targetId);
        String attackId = request.attackId().orElseThrow(() -> new IllegalArgumentException("Strike needs an attack"));
        var profile = state.combatant(request.actorId()).attacks().get(attackId);
        if (profile == null) throw new IllegalArgumentException("Attack is not equipped");
        int penalty = -Math.min(2, state.attacksMade()) * (profile.agile() ? 4 : 5);
        var modifiers = new ArrayList<>(state.combatant(request.actorId()).modifiers(ActiveEffect.Statistic.ATTACK));
        modifiers.add(new Modifier("Multiple attack penalty", ModifierType.UNTYPED, penalty));
        var calculation = new CalculationRecord("Attack", List.of(new BaseComponent(profile.name(), profile.attackBonus())),
                List.of(dice.die(20)), modifiers);
        var attack = new RollRecord(UUID.randomUUID(), request.actorId(), name() + ": " + profile.name(), calculation, Optional.of(target.armorClass()));
        var rolls = new ArrayList<RollRecord>(); rolls.add(attack);
        var degree = attack.outcome().orElseThrow();
        if (degree == DegreeOfSuccess.SUCCESS || degree == DegreeOfSuccess.CRITICAL_SUCCESS) {
            var damageDice = new ArrayList<DieRoll>();
            for (int i = 0; i < profile.diceCount(); i++) damageDice.add(dice.die(profile.dieSides()));
            var base = new ArrayList<BaseComponent>(); base.add(new BaseComponent("Damage modifier", profile.damageBonus()));
            var normal = new CalculationRecord("Damage", base, damageDice, List.of());
            // Basic Strike damage has a minimum of 1 before critical doubling.
            int damage = Math.max(1, normal.total());
            if (normal.total() < 1) base.add(new BaseComponent("Minimum damage adjustment", Math.subtractExact(1, normal.total())));
            if (degree == DegreeOfSuccess.CRITICAL_SUCCESS) base.add(new BaseComponent("Critical doubling (additional damage)", damage));
            var damageCalculation = new CalculationRecord("Damage", base, damageDice, List.of());
            rolls.add(new RollRecord(UUID.randomUUID(), request.actorId(), "Damage: " + profile.name(), damageCalculation, Optional.empty()));
            target = target.withCharacter(target.character().damage(damageCalculation.total()));
        }
        var record = new ActionRecord(UUID.randomUUID(), request.actorId(), name() + ": " + profile.name(),
                ActionKind.ACTION, actionCost(), List.of(targetId), rolls);
        return new ActionResolution(state.apply(record, target, true), record);
    }
}
