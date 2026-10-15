package dev.ttrpg.pf2e;
/** Additive foundation such as attribute, proficiency, or AC's starting 10. */
public record BaseComponent(String label, int value) {
    public BaseComponent { label = Checks.text(label); }
}
