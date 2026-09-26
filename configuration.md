---
description: >-
  The language and logging settings, the database and threading limits, the
  balance cache, and the Vault economy—the whole of
  plugins/InfPoints/config.yml.
---

# Configuration

Plugin-wide settings live in `plugins/InfPoints/config.yml`, which the plugin generates on first startup. The points themselves are defined in separate files—see [Defining points](defining-points.md).

```yaml
lang: 'en.yml'
usePlayerLanguage: true
# LEGACY/ENHANCED_LEGACY/MINI_MESSAGE
serializer: MINI_MESSAGE
# OFF/FATAL/ERROR/WARN/INFO/DEBUG/TRACE/ALL
logLevel: INFO

storage:
  # Prefix of the tables InfPoints creates in the ConnectionSource database
  tablePrefix: 'infpoints_'
  # Name of this server, stored with every transaction
  serverName: 'server'
  # Seconds a database statement may run before it is cancelled and its transaction rolled back
  queryTimeoutSeconds: 5
  # Milliseconds the main thread waits for a free database connection before an operation fails
  syncWaitMillis: 1000
  # Worker threads for asynchronous operations, operations of one player always run in order. Use 1 for SQLite. Requires a restart
  threads: 4
  # Seconds the server waits on shutdown for queued asynchronous operations before failing them
  shutdownTimeoutSeconds: 30
  # Milliseconds another thread waits for the main thread to change a PDC, LEVEL or EXP balance
  mainThreadTimeoutMillis: 5000

cache:
  # Seconds after which a cached SQL balance is refreshed in the background, 0 reads the database on every request
  refreshSeconds: 5
  # Read every stored balance of SQL points into the cache on load, so balances of offline players are known right away
  preload: true
  # Seconds an offline player's balance stays cached after it was last used, 0 keeps it until the point is reloaded
  offlineExpireSeconds: 0
  # Drop the cached balances of a player when they leave, instead of keeping them for offline lookups
  evictOnQuit: false

placeholders:
  # Text balance placeholders show until the balance is loaded
  loading: '...'

vault:
  # Provide a point as the Vault economy
  enabled: false
  # Key of the point provided as the Vault economy
  point: 'primary'
```

## General

| Key                 | What it does                                                                                                                              |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `lang`              | The language file under `lang/` used for messages. `en.yml` and `ru.yml` are bundled.                                                     |
| `usePlayerLanguage` | Answer each player in their client's language when a matching file exists, falling back to `lang`.                                        |
| `serializer`        | How messages in the language files are written: `MINI_MESSAGE` (`<green>`), `LEGACY` (`&a`), or `ENHANCED_LEGACY` (`&a` plus hex colors). |
| `logLevel`          | Threshold for the plugin's own logging.                                                                                                   |

## Storage

These settings apply to SQL points, apart from `threads`, `shutdownTimeoutSeconds` and `mainThreadTimeoutMillis`, which govern asynchronous work for every point.

| Key                       | What it does                                                                                                                                                                |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `tablePrefix`             | Prefix of the three tables InfPoints creates: `<prefix>account`, `<prefix>transaction` and `<prefix>meta`.                                                                  |
| `serverName`              | Stored with every transaction, so the history shows which server a change came from.                                                                                        |
| `queryTimeoutSeconds`     | How long one statement may run. A statement that times out rolls back its whole transaction, so nothing is half-applied.                                                    |
| `syncWaitMillis`          | How long the **main thread** waits for a free database connection. When the database is down, a main-thread operation fails after this long instead of freezing the server. |
| `threads`                 | Worker threads for asynchronous operations. Operations for one player always run in order on the same worker. Use `1` with SQLite, which allows a single writer.            |
| `shutdownTimeoutSeconds`  | How long shutdown waits for queued asynchronous operations. Any still queued after that are logged one by one as not applied—never dropped silently.                        |
| `mainThreadTimeoutMillis` | PDC, LEVEL and EXP balances can only change on the main thread. A call from another thread waits this long for its turn, and is cancelled if it hasn't started by then.     |

{% hint style="info" %}
After the database has failed three times in a row, SQL points stop trying for ten seconds and fail straight away. A dead database costs the server one short wait, not one per operation.
{% endhint %}

## Cache

Reading a balance from the database on every placeholder refresh would put network round trips on the main thread. InfPoints keeps a cache of SQL balances instead, and answers reads from it.

| Key                    | What it does                                                                                                                                    |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `refreshSeconds`       | A cached balance older than this is still returned, and refreshed in the background. `0` turns the cache off and reads the database every time. |
| `preload`              | Read every stored balance into the cache in the background when a point loads, so offline players and leaderboards have a value straight away.  |
| `offlineExpireSeconds` | Drop an offline player's balance this long after anything last read it. `0` keeps it until the point is reloaded.                               |
| `evictOnQuit`          | Drop a player's balances the moment they leave.                                                                                                 |

The cache only affects what is **displayed**. Spending is always checked inside the database, so a stale cached balance can never let a player spend money they don't have. Changes made through InfPoints update the cache immediately; a balance changed any other way shows up within `refreshSeconds` of the next read.

Every entry costs roughly 100 bytes per player per point. With `preload` on and a million accounts that is around 100 MB, so on very large databases turn `preload` off, or set `offlineExpireSeconds`.

## Placeholders

| Key       | What it does                                                                                                                             |
| --------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `loading` | What a balance placeholder shows while the balance hasn't been read yet. With `cache.preload` on, that only happens right after startup. |

## Vault

| Key       | What it does                                                                                                     |
| --------- | ---------------------------------------------------------------------------------------------------------------- |
| `enabled` | Register a point as the server's [Vault](https://github.com/MilkBowl/Vault) economy, for plugins that use Vault. |
| `point`   | Key of the point to register.                                                                                    |

Vault amounts are rounded to the point's `decimals`, and Vault reports that number as the economy's fractional digits. Negative and invalid amounts are refused.

## Applying changes

Edit the file, then run [`/points reload`](commands-and-permissions.md) or restart the server. A reload re-reads `config.yml`, the language files and every point, and re-registers the per-point commands; only `storage.threads` needs a restart.

## Files the plugin writes

| Path                           | Contents                                                                                                      |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| `plugins/InfPoints/config.yml` | Your config. Regenerated on each startup: keys added by a new version are merged in and your values are kept. |
| `plugins/InfPoints/points/`    | One file per point. On a fresh install, `primary.yml`, `level.yml` and `exp.yml` are written as examples.     |
| `plugins/InfPoints/lang/`      | The language files. Messages added by a new version are merged in, and messages you edited are kept.          |
| `plugins/InfPoints/defaults/`  | The generated defaults the merges work from. Don't edit them.                                                 |
| `plugins/InfPoints/backups/`   | A copy of `config.yml` from before each regeneration.                                                         |
