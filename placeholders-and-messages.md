---
description: >-
  The %points_…% PlaceholderAPI placeholders, their formatting suffixes, and how
  to edit the plugin's messages in the language files.
---

# Placeholders and messages

## Placeholders

With [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) installed, every point provides placeholders of the form `%points_<key>_<value>%`:

| Placeholder                         | Shows                                                      |
| ----------------------------------- | ---------------------------------------------------------- |
| `%points_<key>_balance%`            | The balance, formatted with the point's `decimalFormat`.   |
| `%points_<key>_balance-format%`     | The same, with thousands separated by commas: `12,345.67`. |
| `%points_<key>_balance-int%`        | The balance without decimals.                              |
| `%points_<key>_balance-int-format%` | The balance without decimals, with thousands separated.    |
| `%points_<key>_name%`               | The point's `name`.                                        |
| `%points_<key>_name-plural%`        | The point's `namePlural`.                                  |
| `%points_<key>_symbol%`             | The point's `symbol`.                                      |
| `%points_<key>_color%`              | The point's `color`, exactly as written in its file.       |

`name`, `name-plural`, `symbol` and `color` also take a suffix that picks the output format, for use in plugins that expect one in particular:

| Suffix       | Format                                                      | Example                         |
| ------------ | ----------------------------------------------------------- | ------------------------------- |
| `-legacy`    | Legacy `&` codes                                            | `%points_coins_symbol-legacy%`  |
| `-parsed`    | Legacy `§` codes                                            | `%points_coins_color-parsed%`   |
| `-mm`        | MiniMessage                                                 | `%points_coins_color-mm%`       |
| `-plain`     | Plain text, formatting removed                              | `%points_coins_name-plain%`     |
| `-plainText` | Plain text, through Adventure's newer plain-text serializer | `%points_coins_name-plainText%` |
| `-gson`      | JSON text component                                         | `%points_coins_name-gson%`      |

Use `-mm` inside MiniMessage messages: `%points_coins_color-mm%` turns `&6` into `<gold>`.

### Balances never block the server

Scoreboards and tab lists evaluate balance placeholders constantly, so they never touch the database. They answer from the balance cache and refresh it in the background—see [Cache](configuration.md#cache). A balance that hasn't been read yet shows `placeholders.loading`, `...` by default. With `cache.preload` on, that only happens briefly after startup.

## Messages

Every message the plugin sends is in `plugins/InfPoints/lang/`, one file per language. `en.yml` and `ru.yml` are bundled; `lang` in `config.yml` picks the default, and `usePlayerLanguage` answers each player in their client's language when a file for it exists. Messages are written in the format set by `serializer`—MiniMessage by default.

Keys are grouped by prefix: `info-` for information, `error-` for failures, and `success-` for confirmations. `info-help` is the list `/points` prints.

### Message placeholders

Messages can use two kinds of placeholders.

**Replacements** in angle brackets are filled in by the plugin and always work:

| Replacement     | Value                                                                     |
| --------------- | ------------------------------------------------------------------------- |
| `<key>`         | The point's key. Also used to build `%points_<key>_…%` placeholders.      |
| `<amount>`      | The amount of the change, as actually applied after rounding.             |
| `<balance>`     | The balance the command read.                                             |
| `<player-name>` | The other player the message is about—or their UUID if they never joined. |
| `<count>`       | How many players a multi-player change applied to, or failed for.         |
| `<id>`          | A delivery id, or a transaction id in the history.                        |

History entries add `<date>`, `<type>`, `<source>` and `<reason>`, and the audit adds `<checked>`, `<mismatches>` and `<ledger>`.

**PlaceholderAPI placeholders**, such as `%player_name%` or `%points_coins_color-mm%`, work when PlaceholderAPI is installed, and are filled in **for the player the message is about**:

* In messages about another player—`info-point-balance-other`, `info-point-receive`, `success-point-pay`, `success-point-set`, `-add` and `-sub`, `info-history-header`, `error-player-offline`, and `error-insufficient-funds-other`—that's the other player. `%player_name%` in `info-point-balance-other` is the player whose balance you asked for.
* In every other message, it's the player reading it.

The bundled messages use `%player_name%` where the player is known to exist, and `<player-name>` where they may never have joined—deliveries and audits—because `%player_name%` is empty for a player the server has never seen. Without PlaceholderAPI, switch `%player_name%` back to `<player-name>`.

{% hint style="info" %}
Prefer `<balance>` to `%points_<key>_balance%` in messages. It is the value the command itself read, so it can't show the loading text.
{% endhint %}

### Editing messages

Edit the file and run [`/points reload`](commands-and-permissions.md). When a new version adds messages, they are merged into your files and the ones you edited are kept—which also means an improved default never replaces your text. Delete a file to get the current defaults back.
