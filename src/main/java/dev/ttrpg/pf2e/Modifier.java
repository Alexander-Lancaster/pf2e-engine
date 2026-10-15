package dev.ttrpg.pf2e;
import java.util.Objects;
/** One applicable modifier, with a human-readable source captured at resolution. */
public record Modifier(String source, ModifierType type, int value) {
    public Modifier {
        source = Checks.text(source);
        Objects.requireNonNull(type);
        if (type == ModifierType.UNTYPED && value > 0)
            throw new IllegalArgumentException("Use base components for attribute/proficiency bonuses");
    }
}
