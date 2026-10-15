package dev.ttrpg.pf2e;

/** Tracks damage already resolved against resistances, weaknesses and immunities. */
public record HitPoints(int maximum, int current, int temporary) {
    public HitPoints {
        if (maximum <= 0 || current < 0 || current > maximum || temporary < 0)
            throw new IllegalArgumentException("Invalid hit points");
    }
    public static HitPoints full(int maximum) { return new HitPoints(maximum, maximum, 0); }
    public HitPoints damage(int amount) {
        ResourcePool.requireNonnegative(amount);
        int absorbed = Math.min(temporary, amount);
        return new HitPoints(maximum, Math.max(0, current - (amount - absorbed)), temporary - absorbed);
    }
    public HitPoints heal(int amount) {
        ResourcePool.requireNonnegative(amount);
        return new HitPoints(maximum, (int) Math.min(maximum, (long) current + amount), temporary);
    }
    /** Accept a new source, replacing the existing temporary HP; use 0 to expire it. */
    public HitPoints replaceTemporary(int amount) { return new HitPoints(maximum, current, amount); }
}
