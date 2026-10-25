package dev.ttrpg.pf2e;
import java.util.*;
public record Combatant(GameCharacter character, Map<String, AttackProfile> attacks,
        Optional<ShieldProfile> shield, List<ActiveEffect> effects, List<Modifier> acModifiers) {
    public Combatant {
        Objects.requireNonNull(character); attacks = Map.copyOf(attacks);
        Objects.requireNonNull(shield); effects = List.copyOf(effects); acModifiers = List.copyOf(acModifiers);
        attacks.keySet().forEach(Checks::text);
        Checks.unique(effects.stream().map(ActiveEffect::id).toList());
    }
    /** Compatibility constructor; shield state is now represented by an effect. */
    public Combatant(GameCharacter character, Map<String, AttackProfile> attacks,
            Optional<ShieldProfile> shield, boolean raised, List<Modifier> acModifiers) {
        this(character, attacks, shield, raised ? List.of(shieldEffect(character.id(), shield)) : List.of(), acModifiers);
    }
    private static ActiveEffect shieldEffect(UUID owner, Optional<ShieldProfile> shield) {
        var equipment = shield.orElseThrow(() -> new IllegalStateException("No shield equipped"));
        if (!equipment.wielded() || equipment.broken()) throw new IllegalStateException("Shield is not usable");
        return new ActiveEffect(UUID.randomUUID(), "raise-shield", ActiveEffect.Statistic.ARMOR_CLASS,
                new Modifier(equipment.name(), ModifierType.CIRCUMSTANCE, equipment.armorBonus()), Optional.of(owner));
    }
    public boolean shieldRaised() { return effects.stream().anyMatch(e -> e.source().equals("raise-shield")); }
    public Combatant withCharacter(GameCharacter updated) {
        if (!character.id().equals(updated.id())) throw new IllegalArgumentException("Identity changed");
        return new Combatant(updated, attacks, shield, effects, acModifiers);
    }
    public Combatant withEffects(List<ActiveEffect> updated) {
        return new Combatant(character, attacks, shield, updated, acModifiers);
    }
    public Combatant withShieldRaised(boolean raised) {
        var updated = new ArrayList<>(effects.stream().filter(e -> !e.source().equals("raise-shield")).toList());
        if (raised) updated.add(shieldEffect(character.id(), shield));
        return withEffects(updated);
    }
    public Combatant expireAtTurnStart(UUID actor) {
        return withEffects(effects.stream().filter(e -> !e.expiresAtTurnStart().equals(Optional.of(actor))).toList());
    }
    public List<Modifier> modifiers(ActiveEffect.Statistic statistic) {
        return effects.stream().filter(e -> e.statistic() == statistic).map(ActiveEffect::modifier).toList();
    }
    public CalculationRecord armorClass() {
        var modifiers = new ArrayList<>(acModifiers);
        modifiers.addAll(modifiers(ActiveEffect.Statistic.ARMOR_CLASS));
        return new CalculationRecord("AC", List.of(new BaseComponent("Sheet AC", character.statistics().armorClass())), List.of(), modifiers);
    }
}
