package dev.ttrpg.pf2e;
/** Equipped melee/unarmed attack; bonus is the final sheet total before situational modifiers. */
public record AttackProfile(String name, int attackBonus, int diceCount, int dieSides,
        int damageBonus, boolean agile) {
    public AttackProfile {
        name = Checks.text(name);
        if (diceCount < 1 || diceCount > 100 || dieSides < 2)
            throw new IllegalArgumentException("Invalid damage dice");
    }
}
