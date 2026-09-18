# InfPoints

A point and currency plugin for Paper 1.16.5 (Java 16).

- **Several currencies.** Each point has its own storage type.
- **Transaction ledger.** SQL points record every change and keep a stored balance that can be audited.
- **Exact amounts.** Balances are fixed-point numbers, with the number of decimals set per point.
- **Shared databases.** Several servers can use one database safely, because every change runs as a single database transaction on live data.
- **Two APIs.** A sync API and an async API, both with idempotent requests.
- **Integrations.** PlaceholderAPI, Vault, and CommandAPI commands.

## Requirements

| Plugin | Needed for |
|---|---|
| [CommandAPI](https://github.com/CommandAPI/CommandAPI) 9.4.2 | Commands |
| ConnectionSource 2.0.0 | `SQL` points |
| PlaceholderAPI | Placeholders |
| Vault | Vault economy |

All of them are soft dependencies. Without ConnectionSource, `SQL` points load as unavailable. They never fall back to memory.

## Setup

1. Put InfPoints and the plugins you need into `plugins/`.
2. For `SQL` points, configure the database in ConnectionSource. InfPoints creates its tables with the `storage.tablePrefix` prefix.
3. Define points in `plugins/InfPoints/points/<key>.yml`. The file name is the point key. On a fresh install, `points/primary.yml` (MEMORY), `points/level.yml` (vanilla levels) and `points/exp.yml` (vanilla experience) are written as documented examples. Delete the ones you don't need; they aren't written again.

Point options:

| Option | Description |
|---|---|
| `type` | `SQL`, `MEMORY`, `PDC`, `LEVEL` or `EXP` |
| `decimals` | 0 to 8, default 2. Amounts are rounded half-up to this. `SQL` points refuse to load if it changes after balances exist. `LEVEL` and `EXP` always use 0 |
| `defaultBalance` | Balance of players who don't have one yet |
| `name`, `namePlural`, `color`, `symbol`, `decimalFormat` | How balances are displayed |
| `payCommand`, `payAliases`, `balanceCommand`, `balanceAliases` | Per-point commands; leave empty to disable |

Storage types:

| Type | History | Offline players | Notes |
|---|---|---|---|
| `SQL` | yes | yes | Safe for several servers sharing one database |
| `MEMORY` | no | yes | Lost on restart; kept across `/points reload` |
| `PDC` | no | no | Stored in the player's data file |
| `LEVEL`, `EXP` | no | no | Vanilla experience |

Plugin settings in `config.yml`:

- **`storage`:**
  - `tablePrefix`
  - `serverName`: stored with every transaction
  - `queryTimeoutSeconds`
  - `syncWaitMillis`: the longest the main thread waits for a database connection
  - `threads`
  - `shutdownTimeoutSeconds`
  - `mainThreadTimeoutMillis`
- **`cache`:**
  - `refreshSeconds`
  - `preload`: read every stored balance of `SQL` points into the cache on load
  - `offlineExpireSeconds`: drop an offline player's balance this long after it was last used, `0` keeps it
  - `evictOnQuit`: drop a player's balances when they leave
- **`placeholders`:**
  - `loading`
- **`vault`:**
  - `enabled`
  - `point`

## Commands

In the table below:
- `<targets>` accepts a name, a UUID or a selector such as `@a`.
- A command that expects one target fails if the selector matches several.

| Command | Permission |
|---|---|
| `/points balance <key> [target]` | `points.balance.<key>`, `points.balance-other.<key>` |
| `/points pay <key> <target> <amount>` | `points.pay.<key>` |
| `/points set\|add\|sub <key> <targets> <amount> [sender-message] [receiver-message] [reason]` | `points.<set\|add\|sub>.<key>` |
| `/points deliver <key> <target> <amount> <id> [reason]` | `points.add.<key>` |
| `/points exchange <key> <target> <amount> <command>` | `points.exchange.<key>` |
| `/points history <key> [target] [page]` | `points.history.<key>`, `points.history-other.<key>` |
| `/points audit <key> [target]` | `points.admin.audit` |
| `/points reload` | `points.admin.reload` |

Notes:
- **Wildcard permissions.** `points.<action>.*` grants an action for every point.
- **`deliver`** applies a delivery once per `<id>`. It is meant for web stores, which can safely retry a delivery. It requires a `SQL` point.
- **`audit`** compares the stored balances with the sum of the ledger.

## Placeholders

`%points_<key>_<data>%`, where `<data>` is one of:
- `balance`, `balance-format` (thousands grouped), `balance-int`, `balance-int-format`
- `name`, `name-plural`, `symbol`, `color`, each with style suffixes such as `color-mm`

Balances come from the cache and never touch the database on the main thread, so they work for offline players too. Cached balances are refreshed in the background when they are older than `cache.refreshSeconds`, and are kept until the point is reloaded unless `cache.offlineExpireSeconds` or `cache.evictOnQuit` is turned on. A placeholder shows `placeholders.loading` only for a player whose balance isn't cached, which on the default settings means while the preload is still running.

## API

```kotlin
compileOnly("org.bigcraft:InfPoints-api:3.0.0")
```

```java
Point point = IPApi.getInstance().getPoint("primary");

// Sync: SQL runs on the calling thread
if (point.subtract(player.getUniqueId(), 100))
    giveItem(player);

// Async: use this for frequent rewards such as per kill or per block
point.async().add(player.getUniqueId(), 1);

// Full control: reason, actor and an idempotency key, so a retried request is applied only once
TransactionRequest request = TransactionRequest.add(uuid, 500)
        .withReason("Store order #1234")
        .withIdempotencyKey("store:1234");
point.async().execute(request).thenAccept(result -> {
    if (result.isApplied()) { /* SUCCESS or DUPLICATE */ }
});
```

- **Sync methods.** `add`, `subtract`, `set` and `transfer` return whether the change was applied. `execute` returns a `TransactionResult` with a `Status`, the new balance and the ledger entries.
- **Balances.**
  - `get` may read the database.
  - `getCached` never blocks.
  - `async().history(...)` and `async().transaction(id)` read the ledger.
- **Events.** `PointAddEvent`, `PointSubtractEvent`, `PointSetEvent` and `PointTransferEvent` fire before a change. They are cancellable, `setAmount` changes the amount, and they can be asynchronous. `PointTransactionCompleteEvent` fires on the main thread after a successful change.
- **Source.** A transaction's `source` defaults to the name of the calling plugin.

## Migrating from 2.x

Back up your database and `plugins/InfPoints` first.

If several servers share one database, upgrade them all at the same time. A 2.x server can't see 3.0 balances.

### Config

The old `config.yml` is copied to `backups/config-2.x.yml` and updated automatically:
- `implementVault` and `vault` become `vault.enabled` and `vault.point`.
- The `sql` section is no longer used. Configure the database in ConnectionSource and remove the section.
- New `decimals` point option, default 2. Set it high enough for the smallest amount any plugin uses **before** the first start of an `SQL` point, because it can't be changed later.
- Points can now be defined in `points/<key>.yml` as well as in the inline `point` section. When a key is defined in both, the inline definition is used.
- New `storage`, `cache` and `placeholders` sections.

Language files are merged, so messages you already have keep their old text. Delete `lang/*.yml` to get the new messages.

Messages about another player (`info-point-balance-other`, `success-point-pay`, `error-player-offline`, …) now resolve their PlaceholderAPI placeholders for **that** player, so `%player_name%` and `%points_<key>_balance%` describe the player the message is about rather than whoever reads it. A 2.x message that used `%points_<key>_balance%` for someone else's balance showed the reader's own balance instead, so it is worth replacing. `%player_name%` needs PlaceholderAPI installed; the `<player-name>` replacement works without it and still fills in.

### Data

- **`SQL` points.**
  - The old `<key>` table is imported into the ledger once, as `MIGRATION` transactions.
  - Amounts are rounded to `decimals`, and the total rounding difference is logged.
  - The old table is left untouched; drop it once you have checked the balances.
- **`JSON` storage is removed.**
  - Change the point to `type: SQL`, and `data/<key>.json` is imported the same way, then renamed to `.json.migrated`.
  - A point still set to `JSON` loads as unavailable.
- **`PDC` balances** are converted to the new format the first time they are read.

### API

- **`Point` replaces `PointType`, `PointView` and `PointConfig`.** `getPointType`, `getPointView` and `getPointConfig` are removed. `PointTypes` is renamed `PointStorageType`, and `JSON` is gone.
- **`add` and `set` return `boolean`.** Every change method now reports whether it was applied. Invalid amounts are rejected, for example negative or NaN amounts, or amounts that round to 0.
- **Offline players.** Operations on offline players fail for `PDC`, `LEVEL` and `EXP`.
- **Events** no longer read balances.
  - Use `PointTransactionCompleteEvent` for anything that depends on the new balance.
  - A transfer is one atomic operation with its own `PointTransferEvent`, not a subtract plus an add.
- **Frequent rewards.** Plugins that give points often should use `point.async()`, so the main thread doesn't wait on the database.
