---
description: >-
  Every currency your server needs in one plugin, behind an API that takes one
  line to use—with exact balances and a transaction history behind every change.
---

# InfPoints

InfPoints is a Bukkit/Paper plugin for points and currencies: coins, tokens, donation currency, event scores—as many as your server needs, each defined in its own file. Other plugins read and change balances through its API, so one plugin owns the money and everything else asks it.

Most economy plugins give a server a single currency, and every plugin that needs a second one ends up shipping its own. InfPoints exists to be the one place for all of them, with an API that needs no setup: look a point up by its name, call `add` or `subtract`, check the result.

It is also built for the case where losing a point is not acceptable—real-money purchases end up in these balances. Every change on a database-backed point is one database transaction that records what happened, and amounts are stored exactly instead of as floating-point numbers.

## What it gives you

* **As many currencies as you need, in one plugin.** Each point is a file of its own and picks where its balances live: a SQL database, memory, the player's data file, or vanilla experience. Adding a currency is adding a file. See [Defining points](defining-points.md).
* **An API without ceremony.** `IPApi.getInstance().getPoint("coins").add(uuid, 10)` is a complete integration—no economy provider to implement, no registration, no boilerplate. Call it synchronously when you need the answer now, or through `async()` when you don't. See [Using it from your plugin](using-it-from-your-plugin.md).
* **A transaction history.** SQL points record every change—who, how much, the balance after, which plugin or command did it, and why. `/points history` shows it, and `/points audit` checks that every stored balance matches its history.
* **Exact amounts.** Balances are whole numbers of the smallest unit, with the number of decimals set per point, so `0.1` added ten times is exactly `1`.
* **Nothing half-applied.** A spend checks the balance and changes it in one step inside the database, so concurrent spends can't overdraw a balance, and a transfer either moves the money or doesn't. Requests can carry an idempotency key, so a retried purchase is applied only once.
* **Integrations.** [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) placeholders, a [Vault](https://github.com/MilkBowl/VaultAPI) economy, and [CommandAPI](https://docs.commandapi.dev/) commands. See [Placeholders and messages](placeholders-and-messages.md) and [Commands and permissions](commands-and-permissions.md).

## Requirements

|          |                                                                                             |
| -------- | ------------------------------------------------------------------------------------------- |
| Server   | Bukkit/Paper 1.16 or later                                                                  |
| Java     | 16 or later                                                                                 |
| Optional | [ConnectionSource](https://github.com/Wyne10/ConnectionSource-public) 2.0.0, for SQL points |
| Optional | [CommandAPI](https://docs.commandapi.dev/), for the commands                                |
| Optional | [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI), for placeholders        |
| Optional | [Vault](https://github.com/MilkBowl/Vault), to act as the server's economy                  |

Every dependency is optional, and InfPoints enables without any of them. Without ConnectionSource, SQL points load as unavailable and every operation on them fails—they never fall back to memory, which would quietly lose everything earned until the next restart. Without CommandAPI there are no commands, but the API, placeholders and Vault work as usual.

## How it fits together

1. InfPoints enables, reads `config.yml`, and loads every point from `plugins/InfPoints/points/`.
2. SQL points get their database connection from ConnectionSource and create their tables if they don't exist yet.
3. It publishes its API through `IPApi` and the Bukkit services manager, and registers the Vault economy if you enabled it.
4. Other plugins look up a point by its key and read or change balances through it. Each change fires an event beforehand, which listeners can cancel, and another once it's applied.
5. On shutdown InfPoints waits for queued asynchronous changes to finish, and logs any it couldn't apply.

To compile the plugin yourself, see [Building from source](building-from-source.md).
