package dev.ttrpg.pf2e;
/** Equipped shield. Hand requirements are represented by wielded; no durability engine yet. */
public record ShieldProfile(String name, int armorBonus, boolean wielded, boolean broken) {
    public ShieldProfile {
        name = Checks.text(name);
        if (armorBonus < 1) throw new IllegalArgumentException("Shield bonus must be positive");
    }
}
