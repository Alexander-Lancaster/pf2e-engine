package dev.ttrpg.pf2e;
import java.util.*;
/** A named effect instance; expiry is tied to a participant, not a round number. */
public record ActiveEffect(UUID id, String source, Statistic statistic, Modifier modifier,
        Optional<UUID> expiresAtTurnStart) {
    public enum Statistic { ARMOR_CLASS, ATTACK }
    public ActiveEffect {
        Objects.requireNonNull(id); source = Checks.text(source); Objects.requireNonNull(statistic);
        Objects.requireNonNull(modifier); Objects.requireNonNull(expiresAtTurnStart);
    }
}
