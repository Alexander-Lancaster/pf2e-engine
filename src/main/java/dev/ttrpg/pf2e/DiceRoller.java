package dev.ttrpg.pf2e;
import java.util.Objects;
import java.util.random.RandomGenerator;
@FunctionalInterface
public interface DiceRoller {
    int roll(int sides);
    default DieRoll die(int sides) { return new DieRoll(sides, roll(sides)); }
    static DiceRoller random(RandomGenerator random) {
        Objects.requireNonNull(random);
        return sides -> random.nextInt(sides) + 1;
    }
}
