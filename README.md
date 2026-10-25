# PF2e character domain

A Java 21 foundation with no runtime dependencies for shared GM/player encounter software.
No UI, server, or database is required. Open this directory in your Java IDE;
import `pom.xml` as a Maven project.

Run compilation and regression checks with a JDK 21 or newer:

```sh
./mvnw test
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
The action engine below manages a basic three-action turn. Reaction availability
and initiative automation remain future features. No persistence or HTTP endpoints are implemented.

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

The history records alone do not enforce gameplay. Use the action engine below
for supported actions, turn budgets, HP updates, and shield duration. Initiative,
Delay/Ready, nested interruptions, general conditions, fortune/rerolls, damage
types, and ability-specific outcome adjustments remain unsupported.

Stacking and degree-of-success reference:
[Player Core: Playing the Game](https://2e.aonprd.com/Rules.aspx?ID=2263).

## Executable actions

Run the deterministic demonstration after compiling:

```sh
./mvnw test
java -cp target/classes dev.ttrpg.pf2e.ActionDemo
```

The demo shows a Strike dealing damage, Raise Shield increasing AC from 18 to 20,
a subsequent attack missing because of that shield, a critical hit with a multiple
attack penalty, and the shield expiring at the start of its owner's next turn.
It is a scripted demonstration, not an interactive command-line application.

- `ActionDefinition` defines executable behavior; `ActionEngine` registers definitions
  by stable ID. The standard registry includes `strike` and `raise-shield`.
- `ActionRequest` selects an actor, target, and equipped attack ID. Equipment values
  come from the combatant, not the request.
- `ActionResolution` returns the new `BattleState` and its appended `ActionRecord`.
- `BattleState` holds current combatants, encounter history, remaining actions,
  and attacks made. Use its lifecycle methods instead of independently updating
  the underlying `Encounter`. Turns start with three actions and zero attacks.
- `Combatant` adds configured equipped attacks, an optional shield, and situational
  AC modifiers to an existing character. The character's AC must exclude these
  additional modifiers. `armorClass()` calculates the effective AC without changing
  sheet statistics.
- `DiceRoller` supports manual/scripted rolls or injected `RandomGenerator` randomness.

```java
var engine = ActionEngine.standard();
var hero = ActionDemo.fighter("Hero");
var enemy = ActionDemo.fighter("Enemy");
var battle = BattleState.create("Duel", java.util.List.of(hero, enemy))
        .startRound().startTurn(hero.character().id());
var dice = DiceRoller.random(new java.util.Random());
var resolution = engine.resolve(battle,
        ActionRequest.strike(hero.character().id(), enemy.character().id(), "sword"), dice);
battle = resolution.state();
battle = engine.resolve(battle, ActionRequest.raiseShield(hero.character().id()), dice).state();
battle = battle.finishTurn().startTurn(enemy.character().id());
```

Strike spends one action on a hit or miss and increments the attack count. It uses
0/-5/-10 penalties, or 0/-4/-8 for the selected agile attack. Success rolls damage;
critical success doubles the basic damage including its flat modifier. Actual die
faces remain recorded, with a labeled additional component explaining doubling.
Failed checks roll no damage. Damage reduces temporary HP first, then current HP.

Raise Shield requires a wielded, unbroken shield and spends one action. Its bonus
is a circumstance modifier and therefore competes with cover and other circumstance
bonuses. Repeated raising does not stack. It persists through other turns and round
boundaries, expiring when its owner's next turn begins. Historical target AC remains
unchanged after expiration. Shield Block and shield damage are not implemented.

Definitions validate requests before rolling. Invalid requests leave the original
immutable battle unchanged. A failure from a dice provider also leaves game state
unchanged, though consumed random draws are not rolled back. Actors at zero HP
cannot act in this simplified engine; full dying/unconscious rules are deferred.

Scope: configured melee/unarmed attacks only; the caller must establish reach,
visibility, and other spatial eligibility. No ranged ammunition, resistance,
weakness, weapon critical traits, generic timed effects, reaction resolution,
quickened/slowed action budgets, or automatic victory detection yet. Equipment
profiles represent already equipped gear, not a full inventory/hand model. The
engine does not support arbitrary historical edits or automatic state replay.

Rules references:
- [Multiple attack penalty](https://2e.aonprd.com/Rules.aspx?ID=2289)
- [Agile](https://2e.aonprd.com/Traits.aspx?ID=526)
- [Raise Shield and core combat rules](https://2e.aonprd.com/Rules.aspx?ID=2263)

## Integrated game state

Application code should use `GameState` and `GameEngine.execute`, rather than
calling the lower-level action engine or battle lifecycle methods directly.
`GameState.battle()` owns the current character snapshots, equipment, effects,
turn counters and encounter history; `GameState.character(id)` is the same HP and
resources that a character sheet should display. Never keep a second mutable
character-sheet copy.

```java
var engine = GameEngine.standard();
var state = GameState.create("Duel", java.util.List.of(
        ActionDemo.fighter("Hero"), ActionDemo.fighter("Enemy")));
var dice = DiceRoller.random(new java.util.Random());
state = engine.execute(state, state.revision(), new GameCommand.StartRound(), dice).state();
var actor = state.battle().combatants().keySet().iterator().next();
state = engine.execute(state, state.revision(), new GameCommand.StartTurn(actor), dice).state();
var result = engine.execute(state, state.revision(),
        new GameCommand.PerformAction(ActionRequest.raiseShield(actor)), dice);
state = result.state();
```

Supported commands: start round, start turn, end turn, end encounter, perform an
action, and explicit GM damage adjustment with a reason. Each accepted command
increments the revision exactly once and appends a `TransitionRecord`. The record
contains the action/roll snapshot where applicable and typed `StateChange` entries:
participant before/after snapshots, turn budgets, and encounter progression.
Participant snapshots expose HP, resources and effects without parsing log text.
Rejected commands return no replacement state and append no history.

`ActiveEffect` supplies a source, unique instance ID, target statistic (AC or
attack), typed modifier, and optional participant whose next turn expires it.
Expiry is processed across all participants when that turn starts, including
effects on someone other than the turn owner. Empty expiry means no automatic
expiration. Raise Shield replaces its previous effect, so it cannot accumulate;
its convenience boolean accessor is derived from effects. Historical calculations
remain frozen. Generic effect creation/removal commands are not implemented yet.

The caller supplies the last observed revision. A mismatch is rejected before
rolling. This is an optimistic-concurrency building block, **not a concurrent
server**: a future repository must atomically compare-and-swap the stored revision
and publish the returned state. Two independent calls against the same old snapshot
can both compute results; only the storage boundary can choose one to commit.
Command authentication (including authorization for GM adjustments), networking,
save-file serialization, and undo/replay are not implemented. State constructors
and lower-level methods remain available for fixtures/imports; clients must route
changes through the game engine to maintain revision history.

## Maven build

Requires JDK 21 or newer. The checked-in Maven Wrapper downloads the pinned Maven
version on first use; no global Maven installation is needed. First use requires
network access for Maven, build plugins, and JUnit dependencies. On Windows use
`mvnw.cmd` in place of `./mvnw`.

```sh
./mvnw test           # Run named JUnit 5 tests
./mvnw clean verify   # Clean build, test, and package the library JAR
./mvnw -Dtest=ActionTest test  # Run one test class
java -cp target/classes dev.ttrpg.pf2e.ActionDemo
```

JUnit and Surefire replace the custom main-method test harness. Reports are in
`target/surefire-reports/`; the library JAR is `target/pf2e-engine-0.1.0-SNAPSHOT.jar`.
`sh test.sh` remains a compatibility shortcut that delegates to Maven. Include
`pom.xml`, `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties` in Git.
The wrapper uses script-only mode and requires no checked-in JAR. Existing IDE
metadata remains ignored. Production rules and state classes are unchanged by
this build migration.
