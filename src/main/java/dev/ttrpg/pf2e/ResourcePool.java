package dev.ttrpg.pf2e;

/** Immutable bounded resource. Spending is strict; restoration caps at maximum. */
public record ResourcePool(int maximum, int current) {
    public ResourcePool {
        if (maximum < 0 || current < 0 || current > maximum)
            throw new IllegalArgumentException("Resource must satisfy 0 <= current <= maximum");
    }
    public static ResourcePool full(int maximum) { return new ResourcePool(maximum, maximum); }
    public ResourcePool spend(int amount) {
        requireNonnegative(amount);
        if (amount > current) throw new IllegalStateException("Insufficient resource");
        return new ResourcePool(maximum, current - amount);
    }
    public ResourcePool restore(int amount) {
        requireNonnegative(amount);
        return new ResourcePool(maximum, (int) Math.min(maximum, (long) current + amount));
    }
    public ResourcePool refill() { return full(maximum); }
    static void requireNonnegative(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must be nonnegative");
    }
}
