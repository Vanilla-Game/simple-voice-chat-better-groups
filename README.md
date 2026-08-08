# Voice Chat Group Tools

A Bukkit/Paper addon and companion Fabric client mod for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/). The server plugin adds password-free, one-time group invites and creator-authorized member removal. The optional client mod integrates those actions into the existing Simple Voice Chat group screen.

Developed for [Vanilla Game](https://vanilla-game.ru).

The plugin ID used with the Simple Voice Chat API is `vanilla_game_voicechat_group_tools`.

The repository builds two separate artifacts:

- `build/libs/voicechat-group-tools-<version>.jar` — the authoritative Paper/Leaf server plugin.
- `client-fabric/build/libs/voicechat-group-tools-fabric-<version>.jar` — the optional Fabric client UI addon.

## Requirements and version choices

- Leaf or Paper-compatible server for Minecraft `26.2`
- Java `25` or newer
- Simple Voice Chat server plugin `2.6.21`

The optional client module additionally requires:

- Minecraft `26.2`
- Fabric Loader `0.19.3` or newer
- Simple Voice Chat Fabric `2.6.21`

The server project compiles against Paper API `26.2.build.84-stable` and emits Java 25 bytecode. Paper's 26.2 development documentation specifies Java 25 and the `26.2.build.*` API line. Simple Voice Chat 2.6.21 exposes the separately published public API artifact `voicechat-api:2.6.20`, which is used as a compile-only dependency by the server plugin.

The Fabric module intentionally targets Simple Voice Chat's client UI classes for version `2.6.21`. It uses two small Mixins instead of copying or replacing the group screen. The client dependency range is deliberately capped; every Simple Voice Chat update is checked by CI before the range is widened. See "Simple Voice Chat compatibility automation" below.

## Installation

1. Install Simple Voice Chat 2.6.21 on the server.
2. Build this plugin with `./gradlew build`.
3. Copy the `build/libs/voicechat-group-tools-*.jar` file into the server's `plugins` directory.
4. Restart the server. A full restart is recommended instead of Bukkit plugin reload tools.

The plugin declares a hard dependency on `voicechat`. If Simple Voice Chat is missing, the server will not load this plugin. If its `BukkitVoicechatService` is unexpectedly unavailable, this plugin logs a severe error and disables itself.

### Optional Fabric client

1. Install Fabric Loader and Simple Voice Chat `2.6.21` for Minecraft `26.2` on the client.
2. Copy `client-fabric/build/libs/voicechat-group-tools-fabric-*.jar` into the client's `mods` directory.
3. Connect to a server that runs the matching Paper plugin.

The client addon checks the server command tree and changes nothing when `/vcgroup` is unavailable. On supported servers it adds:

- an invite button to the footer of the existing Simple Voice Chat group screen; it opens chat with `/vcgroup invite ` prefilled so normal command suggestions remain available;
- a remove button next to each other group member's existing volume slider; it executes `/vcgroup kick <player>`.

The server still performs every permission, ownership, membership, and live-state check. Installing or modifying the client cannot grant kick authority. Players without the client addon retain the complete command workflow. Invite acceptance stays in the existing clickable server chat message because an invited player is not yet inside the group screen.

## Releases

Releases follow the same Release Please workflow used by other Vanilla Game plugins. Conventional commits merged into `main` update an automated release pull request and changelog. Merging that release pull request creates a `v<version>` GitHub Release, builds both artifacts with Java 25, and attaches the versioned server and Fabric JARs.

The first automated release starts at `0.1.0`. Pull request titles are checked for Conventional Commit format, and every pull request to `main` runs the Gradle build and unit tests.

## Commands

All commands are player-only.

- `/vcgroup invite <player>` — sends an online player a clickable, password-free invite to the sender's current voice chat group. The target must not already be in a group. A configurable cooldown per inviter and target pair prevents chat spam.
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

`expiration-minutes` must be at least `1`. `cooldown-seconds` is applied per inviter and target pair, so inviting a different player is not throttled, and may be set to `0` to disable it. Invalid values are reported in the server log and replaced with safe defaults. Restart the plugin/server after changing the file; this MVP does not add a reload command.

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

## Simple Voice Chat compatibility automation

`compatibility.properties` at the repository root is the single source of truth for the client module:

- `minecraft` — the targeted Minecraft version, used by Gradle and `fabric.mod.json`.
- `voicechat.compile` — the minimum supported Simple Voice Chat artifact; the client always compiles against it so newer-only APIs cannot creep in.
- `voicechat.range` — the dependency range shipped in `fabric.mod.json`.
- `voicechat.tested` — every Simple Voice Chat version verified by CI.

Two workflows consume it:

- Every pull request re-checks the client against every version in `voicechat.tested` (compile plus a headless client launch that force-loads both mixin target classes; `required: true` with `defaultRequire: 1` turns any missing injection point into a non-zero exit). This guards our own changes as much as Simple Voice Chat updates.
- A daily discovery workflow queries the Modrinth API for new Simple Voice Chat Fabric releases for the targeted Minecraft version, runs the same harness against them, and opens a draft `fix:` pull request that widens `voicechat.range` and extends `voicechat.tested` across the contiguous green prefix only. A red version is never skipped, and the prefix never crosses a minor-version boundary — a shipped range must not cover an untested gap that a later backport could land in, so moving to a new Simple Voice Chat minor line is a manual decision. `voicechat.compile` is never bumped automatically. The `fix:` type makes release-please ship the widened range in a patch release; because the repository shares one version, that patch re-releases the server jar as well.

The compatibility harness builds a test-only flavor (`-PvoicechatCompatCheck`) that relaxes the `fabric.mod.json` range to `*` — otherwise Fabric Loader would reject a candidate version before mixins are even applied — and renames the jar to `voicechat-group-tools-fabric-compat-test-<version>.jar`. This flavor exists only inside the compatibility jobs and is never uploaded as an artifact or attached to releases.

## Development

Run the focused server unit tests and build both artifacts:

```shell
./gradlew build
```

The unit tests cover secure token shape/randomness, configurable expiry, one-time consumption, player/group UUID binding, replacement of stale invites, cooldown timing, command accept/kick mutation checks, per-target invite throttling, vanish-aware tab completion, fail-closed creator authorization, translation-key parity, configured lifetime rendering, per-player Russian/English rendering, and English fallback.
