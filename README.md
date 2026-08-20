# 🎙️ Simple Voice Chat Better Groups

[![Latest release](https://img.shields.io/github/v/release/Vanilla-Game/simple-voice-chat-better-groups?style=flat-square&logo=github&label=Release)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Vanilla-Game/simple-voice-chat-better-groups/total?style=flat-square&logo=github&label=Downloads)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases)
[![CI Build](https://img.shields.io/github/actions/workflow/status/Vanilla-Game/simple-voice-chat-better-groups/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=Build)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/actions/workflows/build.yml)
[![bStats](https://bstats.org/signatures/bukkit/Simple%20Voice%20Chat%20Better%20Groups.svg)](https://bstats.org/plugin/bukkit/Simple%20Voice%20Chat%20Better%20Groups/33318)

<!-- modrinth:start -->

Better group controls for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/): password-free invites, join requests, group leaders, member removal, and leadership transfer.

The Paper plugin contains all gameplay logic. Players can use every feature through chat; the optional Fabric mod adds buttons and a leader crown to the existing Simple Voice Chat screens.

## ✨ Features

- One-time invites that expire without exposing a group password.
- Join requests for visible, password-protected groups. The leader accepts them with one click.
- A group leader who can remove members or transfer leadership.
- Automatic succession to the longest-standing member when the leader leaves.
- Group chat announcements when a player joins, including who invited them.
- In-chat notifications in 15 languages, selected based on each player’s Minecraft locale.

## 📦 Installation

### Server

<!-- modrinth:exclude:start -->

<!-- generated:server-downloads:start -->

| Artifact                                                                                                                                              | Minecraft | Server software            | Java  | Simple Voice Chat        |
| ----------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | -------------------------- | ----- | ------------------------ |
| [`svc-better-groups-0.9.1.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.9.1/svc-better-groups-0.9.1.jar) | `26.1.2`  | Paper; Leaf (experimental) | `25`+ | Bukkit `2.6.16`–`2.6.21` |
| [`svc-better-groups-0.9.1.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.9.1/svc-better-groups-0.9.1.jar) | `26.2`    | Paper; Leaf (experimental) | `25`+ | Bukkit `2.6.19`–`2.6.21` |

<!-- generated:server-downloads:end -->

<!-- modrinth:exclude:end -->

Place the server artifact in the server's `plugins` directory and restart the server. Simple Voice Chat is a required dependency.

### Client (optional)

<!-- modrinth:exclude:start -->

<!-- generated:fabric-downloads:start -->

| Artifact                                                                                                                                                                            | Minecraft       | Mod loader              | Fabric API         | Java  | Simple Voice Chat        |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------- | ----------------------- | ------------------ | ----- | ------------------------ |
| [`svc-better-groups-fabric-1.21.11-0.9.1.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.9.1/svc-better-groups-fabric-1.21.11-0.9.1.jar) | `1.21.11`       | Fabric Loader `0.18.1`+ | `0.139.4+1.21.11`+ | `21`+ | Fabric `2.6.6`–`2.6.22`  |
| [`svc-better-groups-fabric-26.1-0.9.1.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.9.1/svc-better-groups-fabric-26.1-0.9.1.jar)       | `26.1`–`26.1.2` | Fabric Loader `0.18.4`+ | `0.144.3+26.1`+    | `25`+ | Fabric `2.6.14`–`2.6.22` |
| [`svc-better-groups-fabric-26.2-0.9.1.jar`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.9.1/svc-better-groups-fabric-26.2-0.9.1.jar)       | `26.2.x`        | Fabric Loader `0.19.3`+ | `0.152.1+26.2`+    | `25`+ | Fabric `2.6.18`–`2.6.22` |

<!-- generated:fabric-downloads:end -->

<!-- modrinth:exclude:end -->

The client mod is optional but recommended: the server plugin works without it, while the mod makes group management much easier.

## 🎮 Usage

All commands are player-only. `/vcgroup` is a permanent alias for `/voicegroup`.

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
