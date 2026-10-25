package dev.ttrpg.pf2e;
import java.util.*;
/** Inputs identify configured equipment; callers do not supply weapon statistics. */
public record ActionRequest(String actionId, UUID actorId, Optional<UUID> targetId, Optional<String> attackId) {
    public ActionRequest {
        actionId = Checks.text(actionId); Objects.requireNonNull(actorId);
        Objects.requireNonNull(targetId); Objects.requireNonNull(attackId); attackId.ifPresent(Checks::text);
    }
    public static ActionRequest strike(UUID actor, UUID target, String attack) {
        return new ActionRequest("strike", actor, Optional.of(target), Optional.of(attack));
    }
    public static ActionRequest raiseShield(UUID actor) {
        return new ActionRequest("raise-shield", actor, Optional.empty(), Optional.empty());
    }
}
