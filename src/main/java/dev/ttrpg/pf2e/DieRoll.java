package dev.ttrpg.pf2e;
/** Actual die face, supplied by a roller or manual input; no hidden randomness. */
public record DieRoll(int sides, int face) {
    public DieRoll {
        if (sides < 2 || face < 1 || face > sides) throw new IllegalArgumentException("Invalid die result");
    }
}
