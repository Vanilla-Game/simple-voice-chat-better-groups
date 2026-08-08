# Voice Chat Group Tools

A minimal server-side Bukkit/Paper addon for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/) that adds password-free, one-time group invites and creator-authorized member removal.

Developed for [Vanilla Game](https://vanilla-game.ru).

The plugin ID used with the Simple Voice Chat API is `vanilla_game_voicechat_group_tools`.

## Requirements and version choices

- Leaf or Paper-compatible server for Minecraft `26.2`
- Java `25` or newer
- Simple Voice Chat server plugin `2.6.21`

The project compiles against Paper API `26.2.build.84-stable` and emits Java 25 bytecode. Paper's 26.2 development documentation specifies Java 25 and the `26.2.build.*` API line. Simple Voice Chat 2.6.21 exposes the separately published public API artifact `voicechat-api:2.6.20`, which is used as a compile-only dependency. No Simple Voice Chat internal classes, reflection, source changes, or bundled API copy are used.

## Installation

1. Install Simple Voice Chat 2.6.21 on the server.
2. Build this plugin with `./gradlew build`.
3. Copy the `build/libs/voicechat-group-tools-*.jar` file into the server's `plugins` directory.
4. Restart the server. A full restart is recommended instead of Bukkit plugin reload tools.

The plugin declares a hard dependency on `voicechat`. If Simple Voice Chat is missing, the server will not load this plugin. If its `BukkitVoicechatService` is unexpectedly unavailable, this plugin logs a severe error and disables itself.

## Releases

Releases follow the same Release Please workflow used by other Vanilla Game plugins. Conventional commits merged into `main` update an automated release pull request and changelog. Merging that release pull request creates a `v<version>` GitHub Release, builds the plugin with Java 25, and attaches the versioned JAR.

The first automated release starts at `0.1.0`. Pull request titles are checked for Conventional Commit format, and every pull request to `main` runs the Gradle build and unit tests.

## Commands

All commands are player-only.

- `/vcgroup invite <player>` — sends an online player a clickable, password-free invite to the sender's current voice chat group. The target must not already be in a group. A configurable per-inviter cooldown prevents chat spam.
- `/vcgroup accept <token>` — accepts an invite that belongs to the executing player's UUID. Tokens expire after the configured lifetime and are removed after successful use.
- `/vcgroup kick <player>` — removes an online member from the caller's current voice chat group. Only the UUID observed in `CreateGroupEvent#getConnection()` for that group is authorized.

Invites contain a random 192-bit URL-safe token. They never contain, log, or display a group password.

## Configuration

The generated `plugins/VoiceChatGroupTools/config.yml` file contains:

```yaml
invites:
  expiration-minutes: 5
  cooldown-seconds: 10
```

`expiration-minutes` must be at least `1`. `cooldown-seconds` is applied per inviting player and may be set to `0` to disable it. Invalid values are reported in the server log and replaced with safe defaults. Restart the plugin/server after changing the file; this MVP does not add a reload command.

## Permission

- `vanillagame.voicechat_group_tools.use` — allows `/vcgroup`; granted to all players by default. This permission does not grant kick authority: `/vcgroup kick` always checks the recorded creator UUID for the exact group.

## Localization

Player-facing messages are available in English (`en_US`) and Russian (`ru_RU`). The plugin uses Paper's Adventure translation system and automatically renders each message in the language selected by that player's Minecraft client. English is used as the fallback for unsupported locales.

Translations are bundled in `src/main/resources/lang/Messages_en_US.properties` and `Messages_ru_RU.properties`. Text is translated server-side; the clickable invite command, token, colors, and click/hover events remain controlled by Java code.

## State and public API limitations

- Invites and cooldowns are deliberately in memory only. Invites expire after the configured lifetime and are invalidated when used, superseded for the same player/group pair, when the invited player disconnects, when the group is removed, or when the plugin/voice chat server stops.
- Group ownership is learned only from public `CreateGroupEvent` events observed while this addon is active and is removed on `RemoveGroupEvent`.
- The public API does not expose the creator of an already existing group. After a plugin reload, or for a persistent group created before this addon observed its creation event, ownership cannot be safely reconstructed. In that case `/vcgroup kick` is denied with a clear message; the plugin never guesses or grants fallback ownership.
- The plugin only acts on online Bukkit players and current public `VoicechatConnection` objects.

## Development

Run the focused unit tests and build the plugin:

```shell
./gradlew build
```

The unit tests cover secure token shape/randomness, configurable expiry, one-time consumption, player/group UUID binding, replacement of stale invites, cooldown timing, command accept/kick mutation checks, invite throttling, vanish-aware tab completion, fail-closed creator authorization, translation-key parity, configured lifetime rendering, per-player Russian/English rendering, and English fallback.
