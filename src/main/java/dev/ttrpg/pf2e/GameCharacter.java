package dev.ttrpg.pf2e;

import java.util.Objects;
import java.util.UUID;

/** A character snapshot. Identity is the UUID; record equality compares the entire snapshot. */
public record GameCharacter(UUID id, String name, int level,
        CharacterStatistics statistics, CharacterResources resources) {
    public GameCharacter {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(statistics);
        Objects.requireNonNull(resources);
        name = name.strip();
        if (name.isEmpty()) throw new IllegalArgumentException("Name must not be blank");
        // Includes creature levels for NPCs; PC creation restrictions belong elsewhere.
        if (level < -1 || level > 25) throw new IllegalArgumentException("Level must be -1..25");
    }
    public static GameCharacter create(String name, int level, CharacterStatistics stats, int maximumHp) {
        return new GameCharacter(UUID.randomUUID(), name, level, stats, CharacterResources.basic(maximumHp));
    }
    public GameCharacter withResources(CharacterResources updated) {
        return new GameCharacter(id, name, level, statistics, updated);
    }
    public GameCharacter damage(int amount) {
        return withResources(resources.withHitPoints(resources.hitPoints().damage(amount)));
    }
    public GameCharacter heal(int amount) {
        return withResources(resources.withHitPoints(resources.hitPoints().heal(amount)));
    }
}
