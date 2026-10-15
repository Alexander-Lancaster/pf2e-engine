package dev.ttrpg.pf2e;
import java.util.*;

/** Immutable calculation snapshot; the breakdown is recomputed only from frozen inputs. */
public record CalculationRecord(String statistic, List<BaseComponent> base,
        List<DieRoll> dice, List<Modifier> modifiers) {
    public CalculationRecord {
        statistic = Checks.text(statistic);
        base = List.copyOf(base); dice = List.copyOf(dice); modifiers = List.copyOf(modifiers);
    }
    /** Highest bonus and worst penalty of each type both apply. Ties favor first entry. */
    public List<ModifierContribution> breakdown() {
        var result = new ArrayList<ModifierContribution>();
        for (int i = 0; i < modifiers.size(); i++) {
            Modifier current = modifiers.get(i);
            int winner = i;
            if (current.type() != ModifierType.UNTYPED) {
                for (int j = 0; j < modifiers.size(); j++) {
                    Modifier candidate = modifiers.get(j);
                    if (candidate.type() != current.type() || Integer.signum(candidate.value()) != Integer.signum(current.value())) continue;
                    int best = modifiers.get(winner).value();
                    if ((current.value() >= 0 ? candidate.value() > best : candidate.value() < best)
                            || (candidate.value() == best && j < winner)) winner = j;
                }
            }
            result.add(new ModifierContribution(current, winner == i,
                    winner == i ? "Applied" : "Suppressed by " + modifiers.get(winner).source()));
        }
        return List.copyOf(result);
    }
    public int total() {
        long total = 0;
        for (var part : base) total += part.value();
        for (var die : dice) total += die.face();
        for (var part : breakdown()) total += part.contribution();
        return Math.toIntExact(total);
    }
}
