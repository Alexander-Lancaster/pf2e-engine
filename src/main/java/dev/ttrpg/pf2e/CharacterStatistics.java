package dev.ttrpg.pf2e;

import java.util.Map;

/** Final sheet totals, not a character-building or modifier-stacking engine. */
public record CharacterStatistics(
        Map<Attribute, Integer> attributes,
        int armorClass, int perception,
        int fortitude, int reflex, int will,
        int speedFeet, Map<Skill, Integer> skills, Map<String, Integer> lore) {
    public CharacterStatistics {
        attributes = Map.copyOf(attributes);
        skills = Map.copyOf(skills);
        lore = Map.copyOf(lore);
        if (attributes.size() != Attribute.values().length)
            throw new IllegalArgumentException("Supply all six attribute modifiers");
        if (skills.size() != Skill.values().length)
            throw new IllegalArgumentException("Supply all sixteen skill modifiers, including untrained skills");
        if (armorClass < 0 || speedFeet < 0)
            throw new IllegalArgumentException("AC and speed must be nonnegative");
        if (lore.keySet().stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("Lore specialties need names");
    }
}
