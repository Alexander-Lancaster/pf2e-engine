package dev.ttrpg.pf2e;
import java.util.Objects;
public record ModifierContribution(Modifier modifier, boolean applied, String explanation) {
    public ModifierContribution { Objects.requireNonNull(modifier); explanation = Checks.text(explanation); }
    public int contribution() { return applied ? modifier.value() : 0; }
}
