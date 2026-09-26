---
description: >-
  Compile against infpoints-api, look up a point by its key, and read or change
  balances—synchronously, asynchronously, or as idempotent requests with a
  reason in the history.
---

# Using it from your plugin

## The short version

```java
Point coins = IPApi.getInstance().getPoint("coins");

if (coins.subtract(player.getUniqueId(), 100))
    giveItem(player);
```

That's a complete integration: no economy provider to implement, no listener to register, no service to wait for. The rest of this page covers the dependency and the options you have when you need more.

## Adding the dependency

The API artifact is published to Maven Central:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.wyne10:infpoints-api:3.0.0")
}
```

Keep it `compileOnly`: the API classes ship inside the InfPoints plugin jar at their real package names, so shading your own copy leaves you with two unrelated `Point` classes and a `ClassCastException`. Everything lives under `me.wyne.infpoints.api`.

Then declare the plugin dependency in `plugin.yml`:

```yaml
depend: [InfPoints]
```

Use `softdepend` instead if your plugin works without it, and check that `IPApi.getInstance()` isn't `null` before using it. Either way the declaration matters: it makes the server enable InfPoints first.

## Getting a point

Points are looked up by their key—the name of the point's file:

```java
// Static access point.
Point coins = IPApi.getInstance().getPoint("coins");

// Bukkit services manager, registered at ServicePriority.Normal.
PointApi api = Bukkit.getServicesManager().getRegistration(PointApi.class).getProvider();
Point coins = api.getPoint("coins");
```

`getPoint` returns `null` for a key that isn't configured; `getKeys()` and `getPoints()` list what is. It's safe to keep a `Point` for the lifetime of your plugin: the same object stays valid across `/points reload`, and picks up the new configuration. If the point is removed or its storage is down, `isAvailable()` returns `false` and changes fail instead of throwing.

## Reading balances

| Method                           | Blocks                               | Use it for                                                                                            |
| -------------------------------- | ------------------------------------ | ----------------------------------------------------------------------------------------------------- |
| `double get(UUID)`               | Can read the database for SQL points | A value you need now, off the main thread or occasionally on it.                                      |
| `OptionalDouble getCached(UUID)` | Never                                | Anything evaluated often—GUIs, scoreboards, placeholders. Empty until the balance has been read once. |
| `async().get(UUID)`              | Never                                | A fresh value, delivered to a callback.                                                               |
| `String getFormat(UUID)`         | Like `get`                           | The balance formatted with the point's `decimalFormat`.                                               |

For SQL points, `get` answers from the balance cache when it can—see [Cache](configuration.md#cache).

## Changing balances

```java
if (coins.subtract(player.getUniqueId(), 100)) {
    giveItem(player);
}
```

`add`, `subtract`, `set` and `transfer` return whether the change was applied. They return `false` rather than throwing when the balance is too low, the amount is invalid, a listener cancelled it, the player is offline for a `PDC`, `LEVEL` or `EXP` point, or the database is unavailable. **Always check the result before handing something out.** The `Player` overloads do the same and also message the player.

Amounts must be finite and above zero—`set` also accepts zero—and are rounded half-up to the point's `decimals`. An amount that rounds to zero is invalid, so with 2 decimals `add(uuid, 0.001)` returns `false`.

A `transfer` is one operation: the sender is only charged if the receiver is credited, and the other way around.

### Asynchronously

On an SQL point, a synchronous change on the main thread waits for the database. That's fine for a purchase, but a reward handed out per block broken or per mob killed would put a database round trip on the main thread every time. Use `async()` for those:

```java
coins.async().add(player.getUniqueId(), 1);
```

`async()` has the same methods, each returning a `CompletableFuture`. Changes for one player are always applied in the order you submitted them. For SQL points the future completes on a worker thread, so switch back to the main thread before touching the world:

```java
coins.async().subtract(uuid, 100).thenAccept(applied -> {
    if (applied)
        Bukkit.getScheduler().runTask(plugin, () -> giveItem(uuid));
});
```

### With a reason and an idempotency key

`execute` takes a `TransactionRequest`, which can carry more than an amount:

```java
TransactionRequest request = TransactionRequest.add(uuid, 500)
        .withReason("Store order #1234")
        .withIdempotencyKey("store:1234");

coins.async().execute(request).thenAccept(result -> {
    if (result.isApplied()) {
        // SUCCESS, or DUPLICATE if this order was already delivered
    }
});
```

| Method                       | Stored in the history as                                                                                   |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `withReason(String)`         | Why the change happened.                                                                                   |
| `withSource(String)`         | What made it. Defaults to your plugin's name.                                                              |
| `withActor(UUID)`            | The player who caused it, when that isn't the player whose balance changes—an admin, say.                  |
| `withIdempotencyKey(String)` | A key of up to 128 characters. A second request with the same key changes nothing and returns `DUPLICATE`. |

An idempotency key makes a request safe to retry. If a web store's delivery times out and you can't tell whether it went through, send the same request again. `SQL` points keep the keys in the database, so they hold across restarts; `MEMORY` points remember them until the server stops. On `PDC`, `LEVEL` and `EXP` points a request with a key fails.

`execute` returns a `TransactionResult`: its `status()`, the `amount()` actually applied after rounding, the `balance()` after the change, and the `transactions()` it recorded.

| Status               | Meaning                                                                          |
| -------------------- | -------------------------------------------------------------------------------- |
| `SUCCESS`            | Applied.                                                                         |
| `DUPLICATE`          | Applied earlier by a request with the same idempotency key. Nothing changed now. |
| `INSUFFICIENT_FUNDS` | The balance was too low. Nothing changed.                                        |
| `CANCELLED`          | A listener cancelled the event. Nothing changed.                                 |
| `INVALID_AMOUNT`     | The amount was negative, not a number, or rounded to zero.                       |
| `PLAYER_OFFLINE`     | The point only stores balances of online players.                                |
| `UNAVAILABLE`        | The point is removed, or its database can't be reached.                          |
| `FAILED`             | Something else went wrong; the server log has the details.                       |

`isApplied()` is `true` for `SUCCESS` and `DUPLICATE`.

## Reading the history

For points where `supportsHistory()` is `true`:

```java
coins.async().history(uuid, 0, 10).thenAccept(transactions -> {
    for (Transaction transaction : transactions)
        // transaction.type(), amount(), balance(), source(), reason(), createdAt(), ...
});
```

`history(player, offset, limit)` returns the newest first. `transaction(id)` looks up one entry by its id. The two halves of a transfer share a `correlationId()`.

## Events

| Event                           | Fired                                          | Cancellable |
| ------------------------------- | ---------------------------------------------- | ----------- |
| `PointAddEvent`                 | Before an amount is added                      | Yes         |
| `PointSubtractEvent`            | Before an amount is subtracted                 | Yes         |
| `PointSetEvent`                 | Before a balance is set                        | Yes         |
| `PointTransferEvent`            | Before a transfer, with `getReceiver()`        | Yes         |
| `PointTransactionCompleteEvent` | After a change was applied, with `getResult()` | No          |

The first four share `getPoint()`, `getPlayer()`, `getAmount()` and `getRequest()`, and `setAmount(double)` changes what will be applied. They fire on whichever thread the change runs on, so check `isAsynchronous()` before touching the world. `PointTransactionCompleteEvent` always fires on the main thread.

The before-events don't carry a balance: the balance isn't known until the change is applied, and another change can land in between. To react to a new balance, listen to `PointTransactionCompleteEvent` and read `getResult().balance()`.

{% hint style="warning" %}
Don't cancel an event and write the balance yourself instead. The balance can change between your read and your write—from another plugin or thread—and one of the two changes is lost. React to `PointTransactionCompleteEvent` and make a normal call such as `subtract`, which checks the balance inside the database.
{% endhint %}

## Rules of thumb

1. **Check the result** of every change before granting anything in return.
2. **Use `async()` for frequent rewards**, and `getCached()` for anything evaluated often. Keep database round trips off the main thread.
3. **Don't assume the player is online.** Balances belong to UUIDs, and SQL points change them for offline players too.
4. **Don't keep your own copy of a balance.** Ask the point each time; `getCached()` is cheap.
