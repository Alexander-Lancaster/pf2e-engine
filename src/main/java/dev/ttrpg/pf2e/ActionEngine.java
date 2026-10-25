package dev.ttrpg.pf2e;
import java.util.*;
public final class ActionEngine {
    private final Map<String, ActionDefinition> definitions;
    public ActionEngine(List<ActionDefinition> actions) {
        var map = new HashMap<String, ActionDefinition>();
        for (var action : actions) {
            Checks.text(action.id());
            if (map.put(action.id(), action) != null) throw new IllegalArgumentException("Duplicate action ID");
        }
        definitions = Map.copyOf(map);
    }
    public static ActionEngine standard() { return new ActionEngine(List.of(new StrikeAction(), new RaiseShieldAction())); }
    public Map<String, ActionDefinition> definitions() { return definitions; }
    public ActionResolution resolve(BattleState state, ActionRequest request, DiceRoller dice) {
        Objects.requireNonNull(state); Objects.requireNonNull(request); Objects.requireNonNull(dice);
        var action = definitions.get(request.actionId());
        if (action == null) throw new IllegalArgumentException("Unknown action: " + request.actionId());
        return action.resolve(state, request, dice);
    }
}
