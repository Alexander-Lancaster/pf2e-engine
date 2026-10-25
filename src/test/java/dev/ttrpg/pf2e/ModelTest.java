package dev.ttrpg.pf2e;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ModelTest {
    @Test
    void hitPointsAbsorbDamageAndCapHealing() {

        var hp = HitPoints.full(20).replaceTemporary(5);
        assertEquals(new HitPoints(20, 17, 0), hp.damage(8));
        assertEquals(new HitPoints(20, 20, 2), hp.damage(3));
        assertEquals(new HitPoints(20, 0, 0), hp.damage(Integer.MAX_VALUE));
        assertEquals(new HitPoints(20, 20, 5), hp.heal(Integer.MAX_VALUE));
        assertEquals(new HitPoints(20, 20, 2), hp.replaceTemporary(2));
        assertThrows(IllegalArgumentException.class, () -> hp.damage(-1));
        assertThrows(IllegalArgumentException.class, () -> hp.heal(-1));
        assertThrows(IllegalArgumentException.class, () -> new HitPoints(20, 21, 0));
    }

    @Test
    void resourcePoolsRejectOverspending() {

        var pool = ResourcePool.full(3).spend(2);
        assertEquals(new ResourcePool(3, 1), pool);
        assertEquals(ResourcePool.full(3), pool.restore(Integer.MAX_VALUE));
        assertThrows(IllegalStateException.class, () -> pool.spend(2));
        assertThrows(IllegalArgumentException.class, () -> pool.spend(-1));
    }

    @Test
    void spellSlotsAreImmutableAndBounded() {
        var hp = HitPoints.full(20).replaceTemporary(5);

        var pool = ResourcePool.full(3).spend(2);
        var slots = new HashMap<Integer, ResourcePool>();
        slots.put(1, ResourcePool.full(2));
        var resources = new CharacterResources(hp, ResourcePool.full(1), new ResourcePool(3, 1), slots);
        slots.clear();
        assertEquals(1, resources.spendSpellSlot(1).spellSlots().get(1).current());
        assertEquals(2, resources.spellSlots().get(1).current());
        assertThrows(IllegalArgumentException.class, () -> resources.spendSpellSlot(2));
        assertThrows(IllegalStateException.class, () -> resources.spendSpellSlot(1).spendSpellSlot(1).spendSpellSlot(1));
        assertThrows(UnsupportedOperationException.class, () -> resources.spellSlots().clear());
        assertThrows(IllegalArgumentException.class, () -> resources.withFocusPoints(ResourcePool.full(4)));
        assertThrows(IllegalArgumentException.class, () -> new CharacterResources(hp, pool, pool, Map.of(0, pool)));
    }

    @Test
    void statisticsRequireCompleteDefensiveCopies() {

        var attributes = new EnumMap<Attribute, Integer>(Attribute.class);
        for (var attribute : Attribute.values()) attributes.put(attribute, 0);
        attributes.put(Attribute.STRENGTH, 4);
        var skills = new EnumMap<Skill, Integer>(Skill.class);
        for (var skill : Skill.values()) skills.put(skill, 0);
        skills.put(Skill.ATHLETICS, 7);
        var stats = new CharacterStatistics(attributes, 18, 5, 7, 5, 3, 25, skills, Map.of("Warfare", 3));
        attributes.clear();
        assertEquals(4, stats.attributes().get(Attribute.STRENGTH));
        assertThrows(IllegalArgumentException.class, () -> new CharacterStatistics(Map.of(), 18, 5, 7, 5, 3, 25, skills, Map.of()));
    }

    @Test
    void characterUpdatesPreserveIdentity() {
        var hp = HitPoints.full(20).replaceTemporary(5);

        var slots = new HashMap<Integer, ResourcePool>();
        slots.put(1, ResourcePool.full(2));
        var resources = new CharacterResources(hp, ResourcePool.full(1), new ResourcePool(3, 1), slots);

        var attributes = new EnumMap<Attribute, Integer>(Attribute.class);
        for (var attribute : Attribute.values()) attributes.put(attribute, 0);
        attributes.put(Attribute.STRENGTH, 4);
        var skills = new EnumMap<Skill, Integer>(Skill.class);
        for (var skill : Skill.values()) skills.put(skill, 0);
        skills.put(Skill.ATHLETICS, 7);
        var stats = new CharacterStatistics(attributes, 18, 5, 7, 5, 3, 25, skills, Map.of("Warfare", 3));
        var character = GameCharacter.create("  Valeros  ", 1, stats, 20);
        var injured = character.damage(8);
        assertEquals(character.id(), injured.id());
        assertEquals("Valeros", character.name());
        assertEquals(20, character.resources().hitPoints().current());
        assertEquals(12, injured.resources().hitPoints().current());
        assertEquals(character, injured.heal(8));
        assertThrows(IllegalArgumentException.class, () -> GameCharacter.create(" ", 1, stats, 20));
        assertThrows(IllegalArgumentException.class, () -> GameCharacter.create("Hero", 26, stats, 20));
    }

}
