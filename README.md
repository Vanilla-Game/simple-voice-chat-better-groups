# Simple Voice Chat Group Management

A Bukkit/Paper addon and companion Fabric client mod for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/). The server plugin adds password-free, one-time group invites and leader-authorized member removal. The optional client mod integrates those actions and a current-leader marker into the existing Simple Voice Chat group screen.

Developed for [Vanilla Game](https://vanilla-game.ru).

The plugin ID used with the Simple Voice Chat API is `vanilla_game_svc_group_management`.

The repository builds two separate artifacts:

- `build/libs/simple-voice-chat-group-management-<version>.jar` — the authoritative Paper/Leaf server plugin.
- `client-fabric/build/libs/simple-voice-chat-group-management-fabric-<version>.jar` — the optional Fabric client UI addon.

## Requirements and version choices

- Leaf or Paper-compatible server for Minecraft `26.2`
- Java `25` or newer
- Simple Voice Chat server plugin `2.6.21`

The optional client module additionally requires:

- Minecraft `26.2`
- Fabric Loader `0.19.3` or newer
- Fabric API `0.152.1` or newer for Minecraft `26.2`
- Simple Voice Chat Fabric `2.6.18` or newer within the supported range

The server project compiles against Paper API `26.2.build.84-stable` and emits Java 25 bytecode. Paper's 26.2 development documentation specifies Java 25 and the `26.2.build.*` API line. Simple Voice Chat 2.6.21 exposes the separately published public API artifact `voicechat-api:2.6.20`, which is used as a compile-only dependency by the server plugin.

The Fabric module intentionally targets Simple Voice Chat's client UI classes for version `2.6.21`. It uses two small Mixins instead of copying or replacing the group screen. The client dependency range is deliberately capped; every Simple Voice Chat update is checked by CI before the range is widened. See "Simple Voice Chat compatibility automation" below.

## Installation

1. Install Simple Voice Chat 2.6.21 on the server.
2. Build this plugin with `./gradlew build`.
3. Copy the `build/libs/simple-voice-chat-group-management-*.jar` file into the server's `plugins` directory.
4. Restart the server. A full restart is recommended instead of Bukkit plugin reload tools.

The plugin declares a hard dependency on `voicechat`. If Simple Voice Chat is missing, the server will not load this plugin. If its `BukkitVoicechatService` is unexpectedly unavailable, this plugin logs a severe error and disables itself.

### Optional Fabric client

1. Install Fabric Loader, Fabric API, and Simple Voice Chat `2.6.21` for Minecraft `26.2` on the client.
2. Copy `client-fabric/build/libs/simple-voice-chat-group-management-fabric-*.jar` into the client's `mods` directory.
3. Connect to a server that runs the matching Paper plugin.

The client addon checks the server command tree and changes nothing when `/vcgroup` is unavailable. On supported servers it adds:

- an invite button to the footer of the existing Simple Voice Chat group screen; it opens chat with `/vcgroup invite ` prefilled so normal command suggestions remain available;
- a gold crown next to the current group leader;
- a remove button next to each other group member's existing volume slider when the local player is the current leader; it executes `/vcgroup kick <player>`;
- a request button on the password screen of a locked group; it executes `/vcgroup request <group UUID>` so the player can knock without knowing the password.

The server still performs every permission, leadership, membership, and live-state check. The client sends only a versioned capability message; it never sends a player or leader UUID. Installing or modifying the client cannot grant kick authority. Players without the client addon retain the complete command workflow. Invite acceptance stays in the existing clickable server chat message because an invited player is not yet inside the group screen.

## Releases

Releases follow the same Release Please workflow used by other Vanilla Game plugins, with the server plugin and the client mod versioned independently. Commits touching the repository outside `client-fabric/` attribute to the server plugin; commits touching `client-fabric/` attribute to the client mod. Each component gets its own release pull request, changelog, and GitHub Release: the server releases as `simple-voice-chat-group-management-v<version>` with the server jar attached, the client as `simple-voice-chat-group-management-fabric-v<version>` with the Fabric jar attached.

Each component starts at `0.1.0`. Pull request titles are checked for Conventional Commit format, and every pull request to `main` runs the Gradle build and unit tests.

After the client release is published, manually update its Modrinth listing: replace any "creator only" wording with "current leader", add Fabric API as a Required dependency for the released client version, and note in the version changelog that the full leader UI requires the server plugin `0.3.0` or newer. This is a post-release metadata step and is not performed by the repository release workflow.

## Commands

All commands are player-only.

- `/vcgroup invite <player>` — sends an online player a clickable, password-free invite to the sender's current voice chat group. The target must not already be in a group. A configurable cooldown per inviter and target pair prevents chat spam.
- `/vcgroup accept <token>` — accepts an invite that belongs to the executing player's UUID. Tokens expire after the configured lifetime and are removed after successful use.
- `/vcgroup kick <player>` — removes an online member from the caller's current voice chat group. Only the current leader tracked by the server is authorized.
- `/vcgroup transfer <player>` — hands leadership to another online member of the caller's current voice chat group. Only the current leader is authorized; the new leader is notified and synced to compatible clients immediately.
- `/vcgroup request <group>` — asks the leader of a password-protected, visible group to let the caller in; the group is matched by name or UUID. The leader receives a clickable one-time request that expires after the configured lifetime; approving it puts the requester into the group without revealing the password. Approval is bound to whoever holds leadership at click time and is only offered for groups whose leader is known.

Invites contain a random 192-bit URL-safe token. They never contain, log, or display a group password.

## Group leadership

The player observed in `CreateGroupEvent#getConnection()` becomes the initial leader. Members are tracked in join order. When the leader leaves the group, moves to another group, or quits the Minecraft server, leadership passes to the longest-standing remaining member. A former leader who rejoins is appended to the end of that order and does not reclaim leadership automatically. The current leader can also hand the role to another member with `/vcgroup transfer <player>`; the transfer does not change the join order used for later automatic passes.

A transient Simple Voice Chat connection loss does not change leadership: Simple Voice Chat keeps the player's group UUID and marks only the voice connection as disconnected. Membership-changing events and Bukkit player quit are handled idempotently, so duplicate leave/disconnect signals are harmless.

## Leader sync protocol

The server plugin and the client mod exchange two plugin messages: the client sends a one-byte versioned `svc_group_management:hello`, and the server answers on `svc_group_management:leader_state` with a version byte, a flags byte, and optional group and leader UUIDs. This byte layout is the compatibility contract between the two components. It is pinned by golden vectors in `LeaderSyncProtocolTest`, and the Fabric payload codecs must match those vectors exactly.

Evolution rules:

- Version 1 byte semantics never change. Any format change ships as a new protocol version, together with an explicit decision about older clients: keep answering them in their format, or stay silent so they degrade to the command workflow.
- Mixed client versions are the normal operating state, not an error. The server answers only compatible hellos; clients facing an incompatible or absent server keep the complete command workflow and simply show no leader UI.
- Deploy the server plugin before shipping client updates. A newer client against an older server stays inactive until the server catches up; the reverse pairing is fully supported.

## Configuration

The generated `plugins/SimpleVoiceChatGroupManagement/config.yml` file contains:

```yaml
invites:
  expiration-minutes: 5
  cooldown-seconds: 10

requests:
  expiration-minutes: 5
  cooldown-seconds: 30
```

`expiration-minutes` values must be at least `1`. `invites.cooldown-seconds` is applied per inviter and target pair, `requests.cooldown-seconds` per requester and group pair; either may be set to `0` to disable that cooldown. Invalid values are reported in the server log and replaced with safe defaults. Restart the plugin/server after changing the file; this MVP does not add a reload command.

## Permission

- `vanillagame.svc_group_management.use` — allows `/vcgroup`; granted to all players by default. This permission does not grant kick authority: `/vcgroup kick` always checks the current leader UUID for the exact group.

## Localization

Player-facing messages are available in English (`en_US`) and Russian (`ru_RU`). The plugin uses Paper's Adventure translation system and automatically renders each message in the language selected by that player's Minecraft client. English is used as the fallback for unsupported locales.

Translations are bundled in `src/main/resources/lang/Messages_en_US.properties` and `Messages_ru_RU.properties`. Text is translated server-side; the clickable invite command, token, colors, and click/hover events remain controlled by Java code.

## State and public API limitations

- Invites and cooldowns are deliberately in memory only. Invites expire after the configured lifetime and are invalidated when used, superseded for the same player/group pair, when the invited player disconnects, when the group is removed, or when the plugin/voice chat server stops.
- Initial leadership is learned only from public `CreateGroupEvent` events observed while this addon is active. Membership and succession are mirrored through `JoinGroupEvent`, `LeaveGroupEvent`, `RemoveGroupEvent`, and Bukkit player quit.
- The public API does not expose the creator or historical join order of an already existing group. After a plugin reload, or for a persistent group created before this addon observed its creation event, leadership cannot be safely reconstructed. In that case no crown is shown and `/vcgroup kick` is denied with a clear message; the plugin never guesses or grants fallback leadership.
- The plugin only acts on online Bukkit players and current public `VoicechatConnection` objects.

## Simple Voice Chat compatibility automation

`client-fabric/compatibility.properties` is the single source of truth for the client module:

- `minecraft` — the targeted Minecraft version, used by Gradle and `fabric.mod.json`.
- `fabric-api` — the Fabric API version used to compile and run the client networking channel, including compatibility-harness launches.
- `voicechat.compile` — the minimum supported Simple Voice Chat artifact; the client always compiles against it so newer-only APIs cannot creep in.
- `voicechat.range` — the dependency range shipped in `fabric.mod.json`.
- `voicechat.tested` — every Simple Voice Chat version verified by CI.

Two workflows consume it:

- Every pull request re-checks the client against every version in `voicechat.tested` (compile plus a headless client launch that force-loads both mixin target classes; `required: true` with `defaultRequire: 1` turns any missing injection point into a non-zero exit). This guards our own changes as much as Simple Voice Chat updates.
- A daily discovery workflow queries the Modrinth API for new Simple Voice Chat Fabric releases for the targeted Minecraft version, runs the same harness against them, and opens a draft `fix:` pull request that widens `voicechat.range` and extends `voicechat.tested` across the contiguous green prefix only. A red version is never skipped, and the prefix never crosses a minor-version boundary — a shipped range must not cover an untested gap that a later backport could land in, so moving to a new Simple Voice Chat minor line is a manual decision. `voicechat.compile` is never bumped automatically. The `fix:` type makes release-please ship the widened range in a patch release of the client mod; the server plugin is versioned independently and is not re-released.

The compatibility harness builds a test-only flavor (`-PvoicechatCompatCheck`) that relaxes the `fabric.mod.json` range to `*` — otherwise Fabric Loader would reject a candidate version before mixins are even applied — and renames the jar to `simple-voice-chat-group-management-fabric-compat-test-<version>.jar`. This flavor exists only inside the compatibility jobs and is never uploaded as an artifact or attached to releases.

## Development

Run the focused server unit tests and build both artifacts:

```shell
./gradlew build
```

The unit tests cover secure token shape/randomness, configurable expiry, one-time consumption, player/group UUID binding, replacement of stale invites, cooldown timing, command accept/kick mutation checks, ordered leader succession, group moves without a leave event, idempotent leave/disconnect handling, fail-closed unknown leadership, sync-protocol encoding, per-target invite throttling, vanish-aware tab completion, translation-key parity, configured lifetime rendering, per-player Russian/English rendering, and English fallback.
