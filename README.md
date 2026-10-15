# PF2e character domain

A dependency-free Java 21 foundation for shared GM/player encounter software.
No UI, server, or database is required. Open this directory in your Java IDE;
mark `src/main/java` as sources and `src/test/java` as tests if necessary.

Run compilation and regression checks with a JDK 21 or newer:

```sh
sh test.sh
```

## Model

- `GameCharacter`: stable UUID, name, level (-1..25 for PCs and creatures), statistics, resources.
- `CharacterStatistics`: all six Remaster attribute **modifiers**, AC, Perception,
  Fortitude/Reflex/Will, land Speed in feet, all sixteen general skills, named Lore specialties.
  Check modifiers and AC are supplied as final sheet totals, including applicable proficiency.
- `HitPoints`: maximum/current/temporary HP. Damage consumes temporary HP first;
  HP stops at zero. Healing caps at maximum and does not replenish temporary HP.
  Accepting new temporary HP replaces the old amount; the caller chooses whether to accept it.
- `ResourcePool`: bounded current/maximum values, strict spending, capped restoration.
- `CharacterResources`: HP, Focus Points (capacity 0..3), Hero Points (capacity 3),
  and spell-slot counts by rank (1..10). Missing ranks have no slots.

All records are immutable; maps are defensively copied. Keep the returned snapshot
when applying an update. UUID identifies the entity; record equality compares state.
The model does not assign a GM/player role or account owner.

## Example

```java
var attributes = new java.util.EnumMap<Attribute, Integer>(Attribute.class);
for (var attribute : Attribute.values()) attributes.put(attribute, 0);
attributes.put(Attribute.STRENGTH, 4);
var skills = new java.util.EnumMap<Skill, Integer>(Skill.class);
for (var skill : Skill.values()) skills.put(skill, 0);
skills.put(Skill.ATHLETICS, 7);
var stats = new CharacterStatistics(attributes, 18, 5, 7, 5, 3, 25,
        skills, java.util.Map.of("Warfare", 3));
var hero = GameCharacter.create("Valeros", 1, stats, 20);
hero = hero.withResources(hero.resources().withHeroPoints(new ResourcePool(3, 1)));
hero = hero.damage(8); // 12/20 HP, same UUID
hero = hero.heal(4);   // 16/20 HP
```

Import `dev.ttrpg.pf2e.*` for this example. Skill totals here are illustrative,
not a validated character build. Basic resources start with full HP, no Focus
capacity, zero Hero Points, and no spell slots. Session setup should explicitly
award Hero Points to eligible characters.

## Deliberate boundaries

This is bookkeeping, not full rules adjudication. No automatic dying, wounded,
unconsciousness, death, recovery, resistance, weakness, condition effects,
proficiency calculation, equipment, character-building validation, or spell
preparation. Damage input is the final damage after mitigation. Temporary HP
source/duration and recovery eligibility remain caller responsibilities.
Spell slots are aggregate counts for one casting pool; separate casting entries,
prepared spell identities, and restricted slots require a richer model later.
Action/reaction availability and initiative automation remain future encounter
features; the encounter history framework below records resolved actions. No persistence or HTTP endpoints are implemented.

Rules references:
- [Temporary Hit Points](https://2e.aonprd.com/Rules.aspx?ID=2321)
- [Player Core spellcasting rules](https://2e.aonprd.com/Rules.aspx?ID=2221)

## Encounters and calculation history

`Encounter` holds participant UUIDs (the IDs of `GameCharacter` entities), ordered
`RoundRecord`s, `TurnRecord`s, and resolved `ActionRecord`s. Start rounds and turns
explicitly, append actions, finish the turn, and finally finish the encounter.
Updates return new snapshots and leave earlier history unchanged. Only the last
turn of the current round can be active. Unknown participants, duplicate record
IDs, and invalid lifecycle transitions are rejected.

Each action has an actor, targets, action kind/cost, and zero or more `RollRecord`s.
Reactions and free actions can be recorded during another participant's turn.
Each roll also has its own actor, allowing a spell action to contain target saves.
Records appear in insertion order; no timestamps or automatic initiative sorting
are assumed. Finish an active turn before finishing the encounter.

`CalculationRecord` stores a statistic label, labeled base components, actual die
faces, and applicable modifiers. `total()` and `breakdown()` operate purely on
these immutable inputs. The breakdown retains suppressed modifiers and their
reasons, including deterministic handling of equal bonuses (first wins).

Stacking applies the highest bonus **and** the most severe penalty of each typed
category independently. Circumstance, status, and item categories stack with one
another. Untyped penalties all contribute. Attribute/proficiency values go in
`BaseComponent`, not untyped bonuses. Callers determine applicability and remove
duplicate instances of the same effect before supplying modifiers.

A `RollRecord` optionally snapshots its DC as another calculation (including a DC
modifier breakdown). Checks require one d20 and expose a degree of success using
the +/-10 thresholds and natural 1/20 adjustments. Rolls without a DC, such as
basic damage sums, have no degree of success. Dice are supplied explicitly for
manual input, deterministic tests, or a future random roller.

```java
var attack = new CalculationRecord("Melee attack",
        java.util.List.of(new BaseComponent("Strength", 4), new BaseComponent("Proficiency", 3)),
        java.util.List.of(new DieRoll(20, 14)),
        java.util.List.of(new Modifier("Bless", ModifierType.STATUS, 1)));
var ac = new CalculationRecord("AC", java.util.List.of(new BaseComponent("AC", 18)),
        java.util.List.of(), java.util.List.of());
var roll = new RollRecord(java.util.UUID.randomUUID(), hero.id(), "Attack vs goblin",
        attack, java.util.Optional.of(ac)); // 22 vs 18: success
var strike = new ActionRecord(java.util.UUID.randomUUID(), hero.id(), "Strike",
        ActionKind.ACTION, 1, java.util.List.of(goblin.id()), java.util.List.of(roll));
var encounter = Encounter.create("Roadside ambush", java.util.Set.of(hero.id(), goblin.id()))
        .startRound().startTurn(hero.id()).record(strike).finishTurn();
```

Here `hero` and `goblin` are existing `GameCharacter` instances. A UI can display
`roll.calculation().breakdown()` on hover/click and `roll.outcome()` alongside the
total. Records do not read live character statistics, so later effects cannot
change previous results. Do not add component/modifier values a second time if
you use an already-final character sheet total as the calculation's base.

This framework records history; it does not yet enforce action budgets, reaction
refresh, initiative, Delay/Ready, nested activities or interruptions, apply damage
to characters, or resolve effect durations. It does not yet model fortune rolls,
rerolls, damage types/critical damage, or ability-specific outcome adjustments.
Characters remain separate from encounter history; callers manage current state.

Stacking and degree-of-success reference:
[Player Core: Playing the Game](https://2e.aonprd.com/Rules.aspx?ID=2263).
