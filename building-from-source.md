---
description: >-
  Clone with submodules, build the shaded plugin jar, run the tests and a test
  server, and publish the API to Maven Central.
---

# Building from source

## What you need

Git, a JDK to run Gradle with, and nothing else—the Gradle wrapper fetches Gradle itself, and the build provisions the JDKs it actually compiles and runs with: Java 16 for compilation, and a JetBrains Runtime 21 for the test server.

## Clone

`gradle/libs.versions.toml` is a symlink into the `libs` submodule, which holds the version catalog shared across these plugins. A clone without it fails while Gradle configures the build:

```bash
git clone --recurse-submodules https://github.com/Wyne10/InfPoints-public.git
```

If you already cloned without `--recurse-submodules`, run `git submodule update --init`.

## Build

```bash
./gradlew build
```

The shaded plugin jar lands in `build/libs/InfPoints-<version>.jar`, ready to drop into a server's `plugins/` folder. The build relocates Guice, Guava, Adventure, EnhancedLegacyText and WUtils under `me.wyne.infpoints.shadow`, so the plugin can't collide with another plugin bundling the same libraries. ORMLite and the ConnectionSource API are deliberately left out: ConnectionSource provides them at runtime.

| Command                        | What it does                                                                          |
| ------------------------------ | ------------------------------------------------------------------------------------- |
| `./gradlew build -Pdebug=true` | Builds without relocating anything, which keeps stack traces readable while debugging |
| `./gradlew test`               | Runs the tests on their own                                                           |
| `./gradlew :api:build`         | Builds the consumer API module on its own                                             |
| `./gradlew clean build`        | Rebuilds from scratch                                                                 |

The project version lives in `gradle.properties` and names both the jar and the published API artifact.

## Tests

`./gradlew build` runs the tests too. Most of them exercise the SQL ledger against an in-memory H2 database, once in H2's own mode and once in MySQL compatibility mode: thousands of concurrent spends from one balance, opposite transfers racing each other, a transfer whose debit fails, repeated idempotency keys, `set` racing `add`, overflow, and a changed `decimals`. They check that no balance goes negative, that nothing is applied twice, and that stored balances still match their history.

## Run a test server

```bash
./gradlew runServer
```

This downloads Paper 1.16.5 along with CommandAPI, ConnectionSource, PlaceholderAPI, Vault, LuckPerms, ViaVersion and ViaBackwards, then starts a server in `run/` with the freshly built plugin installed. Add `-Pdebug=true` to run on 1.19.4 instead, with relocation off.

To try an `SQL` point, point ConnectionSource at a database in `run/plugins/ConnectionSource/config.yml`. The quickest is a local H2 file:

```yaml
sql:
  driver: 'H2_V2'
  jdbcUrl: 'jdbc:h2:./database'
  username: 'sa'
  password: ''
```

Then set `type: SQL` in `run/plugins/InfPoints/points/primary.yml` and run [`/points reload`](commands-and-permissions.md).

## Publish the API

The `api` module is the only thing published; the plugin jar itself is distributed as a jar, not as a dependency.

To try a release locally, into `~/.m2`:

```bash
./gradlew :api:publishToMavenLocal
```
