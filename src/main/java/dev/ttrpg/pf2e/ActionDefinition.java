package dev.ttrpg.pf2e;
/** Executable action extension point. Implementations validate before rolling and never mutate input. */
public interface ActionDefinition {
    String id();
    String name();
    int actionCost();
    ActionResolution resolve(BattleState state, ActionRequest request, DiceRoller dice);
}
