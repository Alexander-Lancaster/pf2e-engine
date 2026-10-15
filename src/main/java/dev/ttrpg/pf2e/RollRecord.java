package dev.ttrpg.pf2e;
import java.util.*;

/** A check has a frozen DC calculation; damage and other rolls have no DC. */
public record RollRecord(UUID id, UUID actorId, String description,
        CalculationRecord calculation, Optional<CalculationRecord> difficultyClass) {
    public RollRecord {
        Objects.requireNonNull(id); Objects.requireNonNull(actorId);
        description = Checks.text(description); Objects.requireNonNull(calculation);
        Objects.requireNonNull(difficultyClass);
        if (difficultyClass.isPresent()) {
            if (calculation.dice().size() != 1 || calculation.dice().getFirst().sides() != 20)
                throw new IllegalArgumentException("A basic check requires exactly one d20");
            if (!difficultyClass.get().dice().isEmpty()) throw new IllegalArgumentException("DC cannot contain dice");
        }
    }
    public Optional<DegreeOfSuccess> outcome() {
        return difficultyClass.map(dc -> {
            long margin = (long) calculation.total() - dc.total();
            int degree = margin >= 10 ? 3 : margin >= 0 ? 2 : margin <= -10 ? 0 : 1;
            int face = calculation.dice().getFirst().face();
            if (face == 20) degree++; else if (face == 1) degree--;
            return DegreeOfSuccess.values()[Math.clamp(degree, 0, 3)];
        });
    }
}
