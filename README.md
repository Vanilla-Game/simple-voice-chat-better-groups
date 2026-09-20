# 🎙️ Simple Voice Chat Better Groups

[![Latest release](https://img.shields.io/github/v/release/Vanilla-Game/simple-voice-chat-better-groups?style=flat-square&logo=github&label=Release)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Vanilla-Game/simple-voice-chat-better-groups/total?style=flat-square&logo=github&label=Downloads)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases)
[![CI Build](https://img.shields.io/github/actions/workflow/status/Vanilla-Game/simple-voice-chat-better-groups/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=Build)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/actions/workflows/build.yml)
[![bStats](https://bstats.org/signatures/bukkit/Simple%20Voice%20Chat%20Better%20Groups.svg)](https://bstats.org/plugin/bukkit/Simple%20Voice%20Chat%20Better%20Groups/33318)

<!-- modrinth:start -->

Better group controls for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/): password-free invites, join requests, group leaders, member removal, and leadership transfer.

The Paper plugin handles group management through chat commands. The optional Fabric mod adds buttons, a leader crown, and a group pause with password-free return.

## ✨ Features

- One-time invites that expire without exposing a group password.
- Join requests for visible, password-protected groups. The leader accepts them with one click.
- A group leader who can remove members or transfer leadership.
- Automatic succession to the longest-standing member when the leader leaves.
- Group chat announcements when a player joins, including who invited them.
- In-chat notifications in 15 languages, selected based on each player’s Minecraft locale.
- Group pause: temporarily leave for local voice chat and return with one key, without entering the password again.

## 📦 Installation

### Server

<!-- modrinth:exclude:start -->

<!-- generated:server-downloads:start -->

| Artifact                                                                                                                                                 | Minecraft | Server software            | Java  | Simple Voice Chat                           |
| -------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | -------------------------- | ----- | ------------------------------------------- |
| [`svc-better-groups-0.11.0.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.11.0/svc-better-groups-0.11.0.jar) | `26.1.2`  | Paper; Leaf (experimental) | `25`+ | Bukkit `2.6.16`–`2.6.21`, `2.6.23`–`2.6.24` |
| [`svc-better-groups-0.11.0.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.11.0/svc-better-groups-0.11.0.jar) | `26.2`    | Paper; Leaf (experimental) | `25`+ | Bukkit `2.6.19`–`2.6.21`, `2.6.23`–`2.6.24` |

<!-- generated:server-downloads:end -->

<!-- modrinth:exclude:end -->

Place the server artifact in the server's `plugins` directory and restart the server. Simple Voice Chat is a required dependency.

### Client (optional)

<!-- modrinth:exclude:start -->

<!-- generated:fabric-downloads:start -->

| Artifact                                                                                                                                                                               | Minecraft       | Mod loader              | Fabric API         | Java  | Simple Voice Chat        |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------- | ----------------------- | ------------------ | ----- | ------------------------ |
| [`svc-better-groups-fabric-1.21.11-0.11.0.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.11.0/svc-better-groups-fabric-1.21.11-0.11.0.jar) | `1.21.11`       | Fabric Loader `0.18.1`+ | `0.139.4+1.21.11`+ | `21`+ | Fabric `2.6.6`–`2.6.24`  |
| [`svc-better-groups-fabric-26.1-0.11.0.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.11.0/svc-better-groups-fabric-26.1-0.11.0.jar)       | `26.1`–`26.1.2` | Fabric Loader `0.18.4`+ | `0.144.3+26.1`+    | `25`+ | Fabric `2.6.14`–`2.6.24` |
| [`svc-better-groups-fabric-26.2-0.11.0.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.11.0/svc-better-groups-fabric-26.2-0.11.0.jar)       | `26.2.x`        | Fabric Loader `0.19.3`+ | `0.152.1+26.2`+    | `25`+ | Fabric `2.6.18`–`2.6.24` |

<!-- generated:fabric-downloads:end -->

Simple Voice Chat's Fabric builds 2.6.23 and 2.6.24 are published as beta versions.

<!-- modrinth:exclude:end -->

The client mod is optional but recommended: the server plugin works without it, while the mod makes group management much easier.

Without the Better Groups server plugin, the client mod still provides the **+** invite button and player picker, using Simple Voice Chat's `/voicechat invite <player>` command. With the plugin available, it uses Better Groups invites automatically.

Use **Pause group** in the group screen, or assign **Pause / return to group** under **Controls → Key Binds → Better Groups** (unbound by default). This requires the updated Better Groups plugin on the server and works with every SVC group type. Press the same key again, or use **Return to group** on the group selection screen, to rejoin without entering the password. The return permission stays on the server; no password is stored by the client mod.

Pause and return use the Minecraft connection and remain available even when voice UDP is disconnected; actual proximity audio still requires a working SVC connection. The last operation status is also shown in the pause/return button tooltip.

Pausing performs a real SVC group leave. You disappear from the member list and use normal proximity voice, including SVC's usual distance, whisper, spectator, permission, and group-type rules. An OPEN group member nearby can still hear and be heard locally; an ISOLATED group retains SVC's isolation. Individual player volumes remain unchanged. Leaving as leader transfers leadership normally; returning does not reclaim it. If the last participant pauses, SVC disbands the empty nonpersistent group and all saved return permissions for it are cleared.

A crossed-out group icon beside the normal voice chat status icon indicates that a group is paused and available for return, even when the group member list is hidden. HUD position, scale, and visibility settings still apply. There are no additional badges on other players' avatars.

Pause does not keep empty groups alive. Persistent groups retain SVC's native lifetime rules. Manual group joining/creation, voice or server disconnection, and plugin shutdown release the player's return permission; unused nonpersistent groups are then eligible for deletion. Persistent groups remain intact. Group pause does not survive a server/plugin restart.

Microphone transmission pauses until the server acknowledges a transition. If confirmation is lost, it stays paused and displays a message; pressing the key again or clicking **Retry** retries the same operation. Repeated requests cannot accidentally toggle the state twice. If the server unregisters the pause channel, the pending operation is cleared and the microphone is unblocked, with a message to check the actual SVC group membership. The pause protocol uses its own channels, preserving the existing version-2 leadership/invite protocol. New controls have English and Russian text; other locales currently use English for these controls.

## 🎮 Usage

All commands are player-only and use `/voicegroup`.

- `/voicegroup invite <player>` — send a clickable invite to an online player who is not in a group.
- `/voicegroup accept <token>` — accept an invite issued to you. The chat button fills this command automatically.
- `/voicegroup kick <player>` — remove a member from your group; leader only.
- `/voicegroup transfer <player>` — make another member the leader; leader only.
- `/voicegroup request <group>` — ask to join a visible password-protected group by name or UUID.

The optional client mod adds:

- a **+** button with a searchable player list;
- a golden crown beside the current leader;
- a remove button beside each member, visible only to the leader;
- a join-request button on the password screen.

## 👑 Leadership

The group creator becomes its first leader. If the leader leaves the group or quits the server, the longest-standing remaining member takes over. A returning former leader joins the end of that order. The current leader can hand the role to another member with `/voicegroup transfer`.

## ⚙️ Server configuration

> Settings are stored in [`plugins/SVCBetterGroups/config.yml`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/blob/main/src/main/resources/config.yml). See the comments in the file for descriptions and examples.

The `vanillagame.svc_better_groups.use` permission allows `/voicegroup` and is granted to all players by default. Leader-only commands always require actual group leadership.

<!-- modrinth:end -->

## Development

Run `./gradlew build` to execute the tests and build the Paper plugin and all three Fabric variants. Release artifacts are staged in `build/release/`.
