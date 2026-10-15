package dev.ttrpg.pf2e;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Resource accounting only; recovery eligibility and spell preparation are external. */
public record CharacterResources(HitPoints hitPoints, ResourcePool focusPoints,
        ResourcePool heroPoints, Map<Integer, ResourcePool> spellSlots) {
    public CharacterResources {
        Objects.requireNonNull(hitPoints);
        Objects.requireNonNull(focusPoints);
        Objects.requireNonNull(heroPoints);
        spellSlots = Map.copyOf(spellSlots);
        if (focusPoints.maximum() > 3 || heroPoints.maximum() != 3)
            throw new IllegalArgumentException("Focus capacity is 0..3; Hero Point capacity is 3");
        if (spellSlots.keySet().stream().anyMatch(rank -> rank < 1 || rank > 10))
            throw new IllegalArgumentException("Spell ranks must be 1..10; cantrips do not consume slots");
    }
    public static CharacterResources basic(int maximumHp) {
        return new CharacterResources(HitPoints.full(maximumHp), ResourcePool.full(0),
                new ResourcePool(3, 0), Map.of());
    }
    public CharacterResources withHitPoints(HitPoints hp) {
        return new CharacterResources(hp, focusPoints, heroPoints, spellSlots);
    }
    public CharacterResources withFocusPoints(ResourcePool focus) {
        return new CharacterResources(hitPoints, focus, heroPoints, spellSlots);
    }
    public CharacterResources withHeroPoints(ResourcePool hero) {
        return new CharacterResources(hitPoints, focusPoints, hero, spellSlots);
    }
    public CharacterResources spendSpellSlot(int rank) {
        ResourcePool pool = spellSlots.get(rank);
        if (pool == null) throw new IllegalArgumentException("No slots at rank " + rank);
        var updated = new HashMap<>(spellSlots);
        updated.put(rank, pool.spend(1));
        return new CharacterResources(hitPoints, focusPoints, heroPoints, updated);
    }
}
