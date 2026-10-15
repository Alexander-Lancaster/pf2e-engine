package dev.ttrpg.pf2e;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Dependency-free regression suite; assertions are explicit (no -ea required). */
public final class ModelTest {
    private static int checks;
    public static void main(String[] args) {
        var hp = HitPoints.full(20).replaceTemporary(5);
        equal(new HitPoints(20, 17, 0), hp.damage(8));
        equal(new HitPoints(20, 20, 2), hp.damage(3));
        equal(new HitPoints(20, 0, 0), hp.damage(Integer.MAX_VALUE));
        equal(new HitPoints(20, 20, 5), hp.heal(Integer.MAX_VALUE));
        equal(new HitPoints(20, 20, 2), hp.replaceTemporary(2));
        fails(IllegalArgumentException.class, () -> hp.damage(-1));
        fails(IllegalArgumentException.class, () -> hp.heal(-1));
        fails(IllegalArgumentException.class, () -> new HitPoints(20, 21, 0));
        var pool = ResourcePool.full(3).spend(2);
        equal(new ResourcePool(3, 1), pool);
        equal(ResourcePool.full(3), pool.restore(Integer.MAX_VALUE));
        fails(IllegalStateException.class, () -> pool.spend(2));
        fails(IllegalArgumentException.class, () -> pool.spend(-1));
        var slots = new HashMap<Integer, ResourcePool>();
        slots.put(1, ResourcePool.full(2));
        var resources = new CharacterResources(hp, ResourcePool.full(1), new ResourcePool(3, 1), slots);
        slots.clear();
        equal(1, resources.spendSpellSlot(1).spellSlots().get(1).current());
        equal(2, resources.spellSlots().get(1).current());
        fails(IllegalArgumentException.class, () -> resources.spendSpellSlot(2));
        fails(IllegalStateException.class, () -> resources.spendSpellSlot(1).spendSpellSlot(1).spendSpellSlot(1));
        fails(UnsupportedOperationException.class, () -> resources.spellSlots().clear());
        fails(IllegalArgumentException.class, () -> resources.withFocusPoints(ResourcePool.full(4)));
        fails(IllegalArgumentException.class, () -> new CharacterResources(hp, pool, pool, Map.of(0, pool)));
        var attributes = new EnumMap<Attribute, Integer>(Attribute.class);
        for (var attribute : Attribute.values()) attributes.put(attribute, 0);
        attributes.put(Attribute.STRENGTH, 4);
        var skills = new EnumMap<Skill, Integer>(Skill.class);
        for (var skill : Skill.values()) skills.put(skill, 0);
        skills.put(Skill.ATHLETICS, 7);
        var stats = new CharacterStatistics(attributes, 18, 5, 7, 5, 3, 25, skills, Map.of("Warfare", 3));
        attributes.clear();
        equal(4, stats.attributes().get(Attribute.STRENGTH));
        fails(IllegalArgumentException.class, () -> new CharacterStatistics(Map.of(), 18, 5, 7, 5, 3, 25, skills, Map.of()));
        var character = GameCharacter.create("  Valeros  ", 1, stats, 20);
        var injured = character.damage(8);
        equal(character.id(), injured.id());
        equal("Valeros", character.name());
        equal(20, character.resources().hitPoints().current());
        equal(12, injured.resources().hitPoints().current());
        equal(character, injured.heal(8));
        fails(IllegalArgumentException.class, () -> GameCharacter.create(" ", 1, stats, 20));
        fails(IllegalArgumentException.class, () -> GameCharacter.create("Hero", 26, stats, 20));
        System.out.println("Passed " + checks + " checks.");
    }
    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
