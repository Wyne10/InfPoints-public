---
description: >-
  The /points command and its subcommands, the per-point pay and balance
  shortcuts, and the permissions that guard them.
---

# Commands and permissions

## Commands

Every command takes the point's key first. Where a command accepts `<players>`, that can be a player name, a UUID, or a selector such as `@a`.

| Command                                                                             | Permission                                                               | What it does                                                     |
| ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------ | ---------------------------------------------------------------- |
| `/points` or `/points help`                                                         | —                                                                        | Lists the commands.                                              |
| `/points balance <key> [player]`                                                    | `points.balance.<key>`, or `points.balance-other.<key>` for someone else | Shows a balance.                                                 |
| `/points pay <key> <player> <amount>`                                               | `points.pay.<key>`                                                       | Transfers from your balance to another player.                   |
| `/points set <key> <players> <amount> [sender-message] [receiver-message] [reason]` | `points.set.<key>`                                                       | Sets balances.                                                   |
| `/points add <key> <players> <amount> [sender-message] [receiver-message] [reason]` | `points.add.<key>`                                                       | Adds to balances.                                                |
| `/points sub <key> <players> <amount> [sender-message] [receiver-message] [reason]` | `points.sub.<key>`                                                       | Subtracts from balances.                                         |
| `/points deliver <key> <player> <amount> <id> [reason]`                             | `points.add.<key>`                                                       | Adds an amount at most once per delivery `<id>`.                 |
| `/points exchange <key> <player> <amount> <command>`                                | `points.exchange.<key>`                                                  | Takes points from an online player, then runs a console command. |
| `/points history <key> [player] [page]`                                             | `points.history.<key>`, or `points.history-other.<key>` for someone else | Shows the transaction history, 10 entries per page.              |
| `/points audit <key> [player]`                                                      | `points.admin.audit`                                                     | Compares stored balances with their history.                     |
| `/points reload`                                                                    | `points.admin.reload`                                                    | Reloads the config, the language files and every point.          |

### Changing balances

`set`, `add` and `sub` work on one player or many: `/points add coins @a 10` pays everyone online. With more than one player, the sender gets a single summary line and each player still gets their own message. `sender-message` and `receiver-message` are `true` by default; set them to `false` to change balances silently. Anything after them is stored as the reason in the history:

```
/points add coins Steve 50 true true Won the building contest
```

A `sub` that would take a balance below zero fails for that player and changes nothing.

### Deliveries

`deliver` is meant for web stores and anything else that may send the same order twice. The first run with a given `<id>` adds the amount; every later run with the same id changes nothing and says it was already applied—even after a restart. Retrying a delivery that may or may not have gone through is always safe.

```
/points deliver coins Steve 500 order-1234 Store purchase
```

The player can be a UUID of someone who has never joined. `deliver` needs a point with a history, which means an `SQL` point.

### Exchanges

`exchange` takes points from an online player and, only if that succeeded, runs a command from the console. The command can use PlaceholderAPI placeholders, which are filled in for that player:

```
/points exchange coins Steve 100 give %player_name% diamond 1
```

### Personal commands

Each point can also register its own `pay` and `balance` commands, named in its file—see [Defining points](defining-points.md). They take the same arguments minus the key, and need the same permissions:

| Command                           | Same as                               |
| --------------------------------- | ------------------------------------- |
| `/<payCommand> <player> <amount>` | `/points pay <key> <player> <amount>` |
| `/<balanceCommand> [player]`      | `/points balance <key> [player]`      |

## Permissions

Every point gets its own set of nodes, where `<key>` is the point's key:

| Permission                   | Grants                                      |
| ---------------------------- | ------------------------------------------- |
| `points.balance.<key>`       | Viewing your own balance.                   |
| `points.balance-other.<key>` | Viewing other players' balances.            |
| `points.pay.<key>`           | Paying other players.                       |
| `points.set.<key>`           | `/points set`.                              |
| `points.add.<key>`           | `/points add` and `/points deliver`.        |
| `points.sub.<key>`           | `/points sub`.                              |
| `points.exchange.<key>`      | `/points exchange`.                         |
| `points.history.<key>`       | Viewing your own transaction history.       |
| `points.history-other.<key>` | Viewing other players' transaction history. |
| `points.admin.audit`         | `/points audit`.                            |
| `points.admin.reload`        | `/points reload`.                           |

Each action also has a wildcard for every point, such as `points.pay.*`, and `points.admin.*` covers both admin permissions.

{% hint style="warning" %}
Every permission defaults to operators, including viewing your own balance. To let players check and send money, grant them `points.balance.<key>` and `points.pay.<key>`—or `points.balance.*` and `points.pay.*` for every point.
{% endhint %}

## When CommandAPI is missing

The commands are registered through [CommandAPI](https://docs.commandapi.dev/), a soft dependency. Without it, InfPoints runs without commands—the API, placeholders and Vault work as usual, and point changes take effect on the next restart instead of a reload.
