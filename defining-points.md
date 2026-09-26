---
description: >-
  One file per currency—where its balances are stored, how many decimals they
  keep, how they are displayed, and which personal commands it gets.
---

# Defining points

Each point—each currency—is a file in `plugins/InfPoints/points/`, and you can have as many as you like. The file name is the point's key, which commands, placeholders and other plugins use to refer to it: `points/coins.yml` defines the point `coins`, used as `/points add coins` and `%points_coins_balance%`.

On a fresh install InfPoints writes three documented examples: `primary.yml` (a `MEMORY` point), `level.yml` and `exp.yml` (vanilla experience). Edit them, delete the ones you don't need, and add your own. They are only written once, so a deleted example stays deleted.

```yaml
# points/coins.yml
type: SQL
decimals: 2
defaultBalance: 100

name: 'Coin'
namePlural: 'Coins'
color: '&6'
symbol: '🪙'
decimalFormat: '#.##'

payCommand: 'pay'
payAliases: []
balanceCommand: 'balance'
balanceAliases: ['bal']
```

| Key              | What it does                                                                                                                                                                                                                   |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `type`           | Required. Where balances are stored—see [Storage types](defining-points.md#storage-types).                                                                                                                                     |
| `decimals`       | How many decimal places balances keep, from `0` to `8`. Default `2`. Amounts are rounded half-up to it. See [Decimals](defining-points.md#decimals).                                                                           |
| `defaultBalance` | The balance of a player who doesn't have one yet. Default `0`.                                                                                                                                                                 |
| `name`           | Singular name, for `%points_<key>_name%`.                                                                                                                                                                                      |
| `namePlural`     | Plural name, for `%points_<key>_name-plural%`.                                                                                                                                                                                 |
| `color`          | Color in legacy format, such as `&6` or `&#FCC700`, for `%points_<key>_color%`.                                                                                                                                                |
| `symbol`         | Symbol shown next to balances, for `%points_<key>_symbol%`.                                                                                                                                                                    |
| `decimalFormat`  | A Java [`DecimalFormat`](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/text/DecimalFormat.html) pattern for displayed balances. Defaults to showing every kept decimal, for example `#.##` for 2 decimals. |
| `payCommand`     | Name of a personal pay command for this point, such as `pay`. Leave empty to register none.                                                                                                                                    |
| `payAliases`     | Aliases for the pay command.                                                                                                                                                                                                   |
| `balanceCommand` | Name of a personal balance command for this point, such as `balance`. Leave empty to register none.                                                                                                                            |
| `balanceAliases` | Aliases for the balance command.                                                                                                                                                                                               |

A key may contain letters, digits and `-`, up to 64 characters. It can't contain `_`, which is what separates the key from the rest of a placeholder.

The personal commands are shortcuts: `/pay Steve 10` does the same as `/points pay coins Steve 10`, and `/balance` the same as `/points balance coins`. Give each command name to one point only.

## Storage types

| Type     | Balances live in                          | History | Offline players |
| -------- | ----------------------------------------- | ------- | --------------- |
| `SQL`    | The database provided by ConnectionSource | Yes     | Yes             |
| `MEMORY` | Server memory                             | No      | Yes             |
| `PDC`    | The player's data file                    | No      | No              |
| `LEVEL`  | The player's vanilla experience level     | No      | No              |
| `EXP`    | The player's vanilla experience points    | No      | No              |

* **`SQL`** is the one to use for anything players earn, buy, or would miss. It requires [ConnectionSource](https://github.com/Wyne10/ConnectionSource-public) 2.0.0 to be installed and configured; without it the point loads as unavailable and every operation on it fails.
* **`MEMORY`** keeps balances until the server stops. They survive `/points reload` as long as the point's type and decimals don't change. Useful for event scores and tests.
* **`PDC`** stores the balance inside the player's own data file, so it follows the player's world data and needs no database. Only online players can be read or changed.
* **`LEVEL`** and **`EXP`** make vanilla experience a point, so other plugins can charge levels through the same API. Balances are always whole numbers, and `decimals` and `defaultBalance` are ignored. Only the first point of each type is used. Vanilla experience changes—killing mobs, enchanting—fire the same point events as any other change, so listeners can cancel or change them.

An operation on an offline player for `PDC`, `LEVEL` or `EXP` fails with `PLAYER_OFFLINE` rather than doing nothing.

## Decimals

Balances are stored as whole numbers of the smallest unit. With `decimals: 2`, a balance of `10.55` is stored as `1055`, and adding `0.1` ten times gives exactly `1`. An amount that rounds to `0`—say `0.001` with 2 decimals—is rejected as invalid.

Because the stored number only means something together with `decimals`, **an `SQL` point remembers its decimals in the database** the first time it loads. If the config later disagrees, the point refuses to load and logs, for example:

```
Point 'coins' stores balances with 2 decimals but is configured with 0, set 'decimals: 2' back
```

That protects every balance from being silently reinterpreted a hundredfold. Choose `decimals` before the first start: enough for the smallest amount any plugin will ever pay.

## How SQL points store balances

Each `SQL` point keeps two things for every player, in tables named after `storage.tablePrefix`:

* **`<prefix>account`**—the current balance.
* **`<prefix>transaction`**—one row per change: the type, the signed amount, the balance after it, the plugin or command that made it, the player who ran it, the reason, the server, and the time.

Every change updates both inside **one database transaction**. A spend is a single conditional update—"subtract 10 where the balance is at least 10"—so two spends from the same balance at the same moment can't take it below zero or overwrite each other. A transfer moves the money between both players in the same transaction, so it's never half-done.

The history is also a check on the balances. [`/points audit`](commands-and-permissions.md) adds up every player's history and compares it with the stored balance; anything that doesn't match was changed outside InfPoints. `<prefix>meta` holds the schema version and each point's decimals.

## Defining points in config.yml

Points can also be listed in `config.yml`, under a `point` section keyed the same way:

```yaml
point:
  coins:
    type: SQL
    decimals: 2
```

A key defined both there and in `points/` uses the `config.yml` definition, and the file is ignored with an error in the log.

## Changing and removing points

`/points reload` applies changes to point files without a restart. A point whose file you delete stops working: every operation on it fails as unavailable, until you add it back. Other plugins holding that point keep a working reference and get the same answer, instead of an error.
