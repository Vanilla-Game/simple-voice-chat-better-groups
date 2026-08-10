# Simple Voice Chat Better Groups

A Bukkit/Paper addon and companion Fabric client mod for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/). The server plugin adds password-free, one-time group invites and leader-authorized member removal. The optional client mod integrates those actions and a current-leader marker into the existing Simple Voice Chat group screen.

Developed for [Vanilla Game](https://vanilla-game.ru).

The plugin ID used with the Simple Voice Chat API is `vanilla_game_svc_better_groups`.

The repository builds three separate artifacts with one shared project version:

- `build/libs/svc-better-groups-<version>.jar` — the authoritative Paper/Leaf server plugin.
- `client-fabric/mc26_1/build/libs/svc-better-groups-fabric-26.1-<version>.jar` — the optional Fabric client UI addon for Minecraft 26.1.x.
- `client-fabric/mc26_2/build/libs/svc-better-groups-fabric-26.2-<version>.jar` — the optional Fabric client UI addon for Minecraft 26.2.x.

Release-ready copies are staged under `build/release/`. Install exactly one of the two Fabric jars: they intentionally share the same mod ID and differ only in the Minecraft line they target.

## Requirements and version choices

- Paper-compatible server for Minecraft `26.1.2` or `26.2`; Leaf support is experimental and smoke-tested
- Java `25` or newer
- Simple Voice Chat Bukkit `2.6.16`–`2.6.21` on Minecraft `26.1.2`, or `2.6.19`–`2.6.21` on Minecraft `26.2`

The optional client module additionally requires:

- for Minecraft `26.1`, `26.1.1`, or `26.1.2`: the `fabric-26.1` jar, Fabric Loader `0.18.4` or newer, Fabric API `0.144.3+26.1` or newer for that Minecraft line, and Simple Voice Chat Fabric `2.6.14`–`2.6.22`;
- for Minecraft `26.2.x`: the `fabric-26.2` jar, Fabric Loader `0.19.3` or newer, Fabric API `0.152.1+26.2` or newer, and Simple Voice Chat Fabric `2.6.18`–`2.6.22`.

The server project compiles against the minimum supported Paper API, `26.1.2.build.74-stable`, and the public Simple Voice Chat API `2.6.13`, then boots the same release jar across the complete supported Paper/SVC matrix. It emits Java 25 bytecode. Minecraft server versions 26.1 and 26.1.1 are not supported: 26.1 has no applicable Paper server build, and 26.1.1 did not reach the stable Paper baseline used by this project.

Both Fabric variants use the same source and targeted Mixins instead of copying or replacing the Simple Voice Chat group screen. Each published jar is run unchanged against every declared Minecraft/SVC combination before release. The dependency ranges are deliberately capped; see "Simple Voice Chat compatibility automation" below.

Simple Voice Chat supports different patch releases on the server and client when both remain on the compatible `2.6` protocol line. They do not need identical patch numbers. The supported ranges above are nevertheless strict: SVC `2.5.x`, `2.7.x`, beta, and unlisted builds are outside this project's tested contract.

## Installation

1. Install a Simple Voice Chat Bukkit release supported by the server's Minecraft version.
2. Build this plugin with `./gradlew build`.
3. Copy `svc-better-groups-<version>.jar` into the server's `plugins` directory.
4. Restart the server. A full restart is recommended instead of Bukkit plugin reload tools.

The plugin declares a hard dependency on `voicechat`. If Simple Voice Chat is missing, the server will not load this plugin. If its `BukkitVoicechatService` is unexpectedly unavailable, this plugin logs a severe error and disables itself.

### Optional Fabric client

1. Install Fabric Loader, Fabric API, and a supported Simple Voice Chat Fabric build for the client's exact Minecraft version.
2. Copy `svc-better-groups-fabric-26.1-<version>.jar` for Minecraft 26.1.x, or `svc-better-groups-fabric-26.2-<version>.jar` for Minecraft 26.2.x, into the client's `mods` directory. Do not install both.
3. Connect to a server that runs the matching Paper plugin.

The client addon waits for a valid response to its versioned handshake with the Paper plugin and keeps its controls hidden until a compatible server confirms support. On supported servers it adds:

- an invite button to the footer of the existing Simple Voice Chat group screen; it opens a searchable player picker listing everyone connected to voice chat and not in a group, and clicking a player sends the invite;
- a gold crown next to the current group leader;
- a remove button next to each other group member's existing volume slider when the local player is the current leader; it executes `/voicegroup kick <player>`;
- a request button on the password screen of a locked group; it executes `/voicegroup request <group UUID>` so the player can knock without knowing the password.

The server still performs every permission, leadership, membership, and live-state check. Protocol negotiation carries only version bytes; the client never sends a group or leader UUID. Installing or modifying the client cannot grant kick authority. Players without the client addon retain the complete command workflow. Invite acceptance stays in the existing clickable server chat message because an invited player is not yet inside the group screen.

## Releases

Releases follow the same Release Please workflow used by other Vanilla Game plugins. The server plugin and both client variants share one release cycle and one version: every `v<version>` GitHub Release carries all three jars. Modrinth publishes separate Paper, Fabric 26.1, and Fabric 26.2 version entries with that shared project version. Historical client releases keep their `svc-better-groups-fabric-v*` tags.

Pull request titles are checked for Conventional Commit format, and every pull request to `main` runs the Gradle build and unit tests.

Each Fabric Modrinth entry declares Fabric API and Simple Voice Chat as required dependencies and lists only the Minecraft versions supported by its jar. The Paper entry lists Minecraft 26.1.2 and 26.2. Publishing, deploying, and restarting servers remain separate operations.

## Commands

All commands are player-only. Everything is also available under the legacy `/vcgroup` alias, which released clients use on the wire; the alias is permanent.

- `/voicegroup invite <player>` — sends an online player a clickable, password-free invite to the sender's current voice chat group. The target must not already be in a group. A configurable cooldown per inviter and target pair prevents chat spam.
- `/voicegroup accept <token>` — accepts an invite that belongs to the executing player's UUID. Tokens expire after the configured lifetime and are removed after successful use.
- `/voicegroup kick <player>` — removes an online member from the caller's current voice chat group. Only the current leader tracked by the server is authorized.
- `/voicegroup transfer <player>` — hands leadership to another online member of the caller's current voice chat group. Only the current leader is authorized; the new leader is notified and synced to compatible clients immediately.
- `/voicegroup request <group>` — asks the leader of a password-protected, visible group to let the caller in; the group is matched by name or UUID. The leader receives a clickable one-time request that expires after the configured lifetime; approving it puts the requester into the group without revealing the password. Approval is bound to whoever holds leadership at click time and is only offered for groups whose leader is known.

Invites contain a random 192-bit URL-safe token. They never contain, log, or display a group password.

## Group leadership

The player observed in `CreateGroupEvent#getConnection()` becomes the initial leader. Members are tracked in join order. When the leader leaves the group, moves to another group, or quits the Minecraft server, leadership passes to the longest-standing remaining member. A former leader who rejoins is appended to the end of that order and does not reclaim leadership automatically. The current leader can also hand the role to another member with `/voicegroup transfer <player>`; the transfer does not change the join order used for later automatic passes.

A transient Simple Voice Chat connection loss does not change leadership: Simple Voice Chat keeps the player's group UUID and marks only the voice connection as disconnected. Membership-changing events and Bukkit player quit are handled idempotently, so duplicate leave/disconnect signals are harmless.

## Client-server protocol

The server plugin and client mod use three versioned plugin messages:

- `svc_better_groups:client_hello` — client to server, one requested protocol-version byte;
- `svc_better_groups:server_hello` — server to client, one selected protocol-version byte and no group state;
- `svc_better_groups:group_state` — server to client, version and flags followed by optional group and leader UUIDs.

After negotiation, the server sends an initial `group_state` only when the player is already in a group. Later membership and leadership transitions send updated state to affected compatible clients. Leaving a group sends an empty state because the client must clear its previous group cache. The byte layouts are pinned by golden vectors in `GroupSyncProtocolTest`, and the Fabric payload codecs must match those vectors exactly.

Evolution rules:

- Version 2 byte semantics never change. Any format change ships as a new protocol version.
- The server answers only compatible client hellos. A mismatched or absent server leaves the optional client UI inactive while the normal command workflow remains available.
- Deploy the server plugin before shipping the matching client update. The UI becomes available only after `server_hello` confirms the selected protocol version.

## Configuration

The generated `plugins/SVCBetterGroups/config.yml` file contains:

```yaml
invites:
  expiration-minutes: 5
  cooldown-seconds: 10
  sound: block.anvil.land:0.5:2

requests:
  expiration-minutes: 5
  cooldown-seconds: 30
  sound: block.anvil.land:0.5:2
```

`expiration-minutes` values must be at least `1`. `invites.cooldown-seconds` is applied per inviter and target pair, `requests.cooldown-seconds` per requester and group pair; either may be set to `0` to disable that cooldown. `invites.sound` and `requests.sound` are the sounds played to the invited player and to the leader receiving a join request, in the single-line form `<sound id>[:volume[:pitch]]` — any sound event id works, `none` disables it, and unknown-but-well-formed ids fail silently on the client. Volume accepts `0.0`–`10.0`, pitch `0.5`–`2.0` (the range Minecraft supports); omitted or out-of-range values fall back to the defaults `0.5` and `2`. Invalid values are reported in the server log and replaced with safe defaults. Restart the plugin/server after changing the file; this MVP does not add a reload command.

## Permission

- `vanillagame.svc_better_groups.use` — allows `/voicegroup`; granted to all players by default. This permission does not grant kick authority: `/voicegroup kick` always checks the current leader UUID for the exact group.

## Localization

Player-facing messages are available in English (`en_US`), Catalan (`ca_ES`), Czech (`cs_CZ`), German (`de_DE`), Spanish (`es_ES`), French (`fr_FR`), Italian (`it_IT`), Dutch (`nl_NL`), Norwegian (`no_NO`), Polish (`pl_PL`), Portuguese (`pt_PT`), Russian (`ru_RU`), Swedish (`sv_SE`), Turkish (`tr_TR`), and Ukrainian (`uk_UA`). The plugin uses Paper's Adventure translation system and automatically renders each message in the language selected by that player's Minecraft client. English is used as the fallback for unsupported locales.

Server translations are bundled in `src/main/resources/lang/`, with matching client translations under `client-fabric/src/main/resources/assets/svc_better_groups_client/lang/`. Text is translated server-side; the clickable invite command, token, colors, and click/hover events remain controlled by Java code.

## State and public API limitations

- Invites and cooldowns are deliberately in memory only. Invites expire after the configured lifetime and are invalidated when used, superseded for the same player/group pair, when the invited player disconnects, when the group is removed, or when the plugin/voice chat server stops.
- Initial leadership is learned only from public `CreateGroupEvent` events observed while this addon is active. Membership and succession are mirrored through `JoinGroupEvent`, `LeaveGroupEvent`, `RemoveGroupEvent`, and Bukkit player quit.
- The public API does not expose the creator or historical join order of an already existing group. After a plugin reload, or for a persistent group created before this addon observed its creation event, leadership cannot be safely reconstructed. In that case no crown is shown and `/voicegroup kick` is denied with a clear message; the plugin never guesses or grants fallback leadership.
- The plugin only acts on online Bukkit players and current public `VoicechatConnection` objects.

## Simple Voice Chat compatibility automation

`compatibility.json` is the single source of truth for the server and both client variants. It records:

- minimum compile-time Paper and Simple Voice Chat APIs;
- pinned Paper and experimental Leaf runtime builds;
- each Fabric line's Minecraft, Loader, Fabric API, compile-time SVC artifact, and published dependency range;
- every exact Minecraft runtime and SVC artifact pair verified by CI.

Two workflows consume it:

- Every pull request builds each release jar once, then launches that exact jar against every declared runtime pair. Fabric runs use a separate non-published Loom/GameTest runner that force-loads every mixin target class; `required: true` with `defaultRequire: 1` turns any missing injection point into a non-zero exit. Paper runs boot the same server jar with every supported Bukkit SVC build, while Leaf runs the minimum and maximum SVC build for each server version. No test harness code is included in a published mod.
- A daily discovery workflow queries listed Modrinth release builds independently for Fabric 26.1.x, Fabric 26.2, Bukkit 26.1.2, and Bukkit 26.2. It widens only the contiguous green prefix for each target and never skips a failed version or crosses into a new SVC minor. New Minecraft lines and SVC 2.7 require manual review.

The CI workflow finishes with one stable `Compatibility gate` check that aggregates the build and both compatibility suites. Repository branch protection or a ruleset must require that check to make it merge-blocking; the workflow itself cannot enforce GitHub merge policy.

For discovery only, the harness may generate a probe jar with the same compiled addon classes and a temporary unrestricted SVC dependency. Published jars never relax their metadata. The probe, GameTest jar, and runner outputs are never staged or attached to releases.

## Unsupported combinations

- Minecraft servers 26.1 and 26.1.1, and all Minecraft 1.21.x versions.
- Simple Voice Chat 2.5.x, 2.7.x, beta, and unlisted builds.
- Forge, NeoForge, and Quilt clients. The optional UI addon is Fabric-only.
- Two Better Groups Fabric variants installed together.

## Development

Run the focused server unit tests and build both artifacts:

```shell
./gradlew build
```

The unit tests cover secure token shape/randomness, configurable expiry, one-time consumption, player/group UUID binding, replacement of stale invites, cooldown timing, command accept/kick mutation checks, ordered leader succession, group moves without a leave event, idempotent leave/disconnect handling, fail-closed unknown leadership, sync-protocol encoding, per-target invite throttling, vanish-aware tab completion, server and client translation-key/placeholder parity, configured lifetime rendering, per-player locale rendering, and English fallback.
