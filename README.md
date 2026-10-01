# Simple Voice Chat Better Groups

[![Latest release](https://img.shields.io/github/v/release/Vanilla-Game/simple-voice-chat-better-groups?style=flat-square&logo=github&label=Release)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Vanilla-Game/simple-voice-chat-better-groups/total?style=flat-square&logo=github&label=Downloads)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases)
[![CI Build](https://img.shields.io/github/actions/workflow/status/Vanilla-Game/simple-voice-chat-better-groups/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=Build)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/actions/workflows/build.yml)

<!-- modrinth:start -->

**Your voice group, without the hassle.**

Invite friends without sharing passwords, approve join requests, and step away for a local conversation with an easy way back.

An add-on for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/) · **Paper & Folia** · **Optional Fabric client**

[Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest) · [Installation](#installation) · [Commands](#commands)

## Invite a friend. Click Accept. Talk together.

Open your group, click **+**, and choose a player. They accept the invitation in chat and join your group without entering its password.

![Alex invites Steve, who accepts in chat and joins the password-protected group](https://raw.githubusercontent.com/Vanilla-Game/simple-voice-chat-better-groups/3b41e7f207a2f6879e2b68f2ba535d4f6bc0299a/assets/demo-invite.gif)

The player picker requires the Better Groups Fabric client. With just the server plugin, use `/voicegroup invite <player>`; the recipient still gets a clickable invitation.

## What can you do?

| You want to… | Better Groups helps you… |
| --- | --- |
| Bring a friend into your group | Send a personal, expiring invite without sharing the password. |
| Join a password-protected group | Send a join request that its leader can accept with one click. |
| Manage your group | Remove members, transfer leadership, and automatically choose a successor when the leader leaves. |
| Talk to someone nearby | Pause your group conversation and return without entering the password again. |

With the optional Fabric client, you also get a searchable player picker, a leader's crown, and member controls in the group screen.

![Alex transfers leadership to Steve and the crown moves to Steve](https://raw.githubusercontent.com/Vanilla-Game/simple-voice-chat-better-groups/3b41e7f207a2f6879e2b68f2ba535d4f6bc0299a/assets/demo-leadership.gif)

Transfer leadership with `/voicegroup transfer <player>`; the crown identifies the current leader.

Chat messages follow each player's Minecraft language, with 15 translations available. The pause controls have English and Russian translations.

## Step away. Talk nearby. Come back.

With the Better Groups client and server plugin, click **Ⅱ** to switch from your group to proximity chat. Click **Return to group** when you are ready to rejoin.

**Group chat → Ⅱ Pause → Proximity chat → Return → Group chat**

![Steve pauses for proximity chat and returns to Adventure without a password prompt](https://raw.githubusercontent.com/Vanilla-Game/simple-voice-chat-better-groups/3b41e7f207a2f6879e2b68f2ba535d4f6bc0299a/assets/demo-pause.gif)

The group must still exist when you return. Joining another group, disconnecting, or restarting the server clears your return permission. See [Group pause details](#group-pause-details) for controls and other behavior.

## What should I install?

| Your role | What you need |
| --- | --- |
| **Server owner** | Simple Voice Chat + the Better Groups server plugin. Players get invites, join requests, and group management through chat and commands. |
| **Player** | Simple Voice Chat on your client. Add the **optional Better Groups Fabric mod** for in-game buttons, the player picker, and pause / return controls. |

Only the **Better Groups client mod** is optional; Simple Voice Chat is required. Leadership, join requests, and group pause require the Better Groups server plugin.

## Installation

### Server

Install [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat), place the Better Groups server JAR in `plugins/`, and restart the server. One JAR supports both Paper and Folia; Leaf support is experimental.

<!-- modrinth:exclude:start -->

<!-- generated:server-downloads:start -->

| Download | Minecraft | Server | Java | Simple Voice Chat |
| --- | --- | --- | --- | --- |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-0.13.0.jar) | `26.1.2` | Paper; Folia; Leaf (experimental) | `25`+ | Bukkit `2.6.16`–`2.6.21`, `2.6.23`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-0.13.0.jar) | `26.2` | Paper; Folia; Leaf (experimental) | `25`+ | Bukkit `2.6.19`–`2.6.21`, `2.6.23`–`2.6.24` |

<!-- generated:server-downloads:end -->

<!-- modrinth:exclude:end -->

### Client (optional)

Install Fabric Loader, Fabric API, Simple Voice Chat, and the matching Better Groups client JAR in `mods/`. Players can use the server plugin without this mod.

<!-- modrinth:exclude:start -->

<!-- generated:fabric-downloads:start -->

| Download | Minecraft | Fabric Loader | Fabric API | Java | Simple Voice Chat |
| --- | --- | --- | --- | --- | --- |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-1.21.11-0.13.0.jar) | `1.21.11` | `0.18.1`+ | `0.139.4+1.21.11`+ | `21`+ | Fabric `2.6.6`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-26.1-0.13.0.jar) | `26.1`–`26.1.2` | `0.18.4`+ | `0.144.3+26.1`+ | `25`+ | Fabric `2.6.14`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-26.2-0.13.0.jar) | `26.2.x` | `0.19.3`+ | `0.152.1+26.2`+ | `25`+ | Fabric `2.6.18`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-26.3-0.13.0.jar) | `26.3.x` | `0.19.5`+ | `0.161.0+26.3`+ | `25`+ | Fabric `2.6.23`–`2.6.24` |

<!-- generated:fabric-downloads:end -->

The 26.3 client is included in the next release. Simple Voice Chat 2.6.23 and 2.6.24 for 26.3 are currently beta builds.

<!-- modrinth:exclude:end -->

On servers without Better Groups, the client's **+** button uses Simple Voice Chat's native invite command.

## Group pause details

Click **Ⅱ** beside **+**, or assign **Pause / return to group** in **Controls → Key Binds → Better Groups**. The pause button appears only after the server confirms support.

Pausing leaves the group and switches to Simple Voice Chat's normal proximity chat. Click **Return to group** on the group selection screen, or press the same key again, to return. The server remembers the return permission; the client does not store the password.

- Returning to the same group restores your previous leader role.
- If the last member leaves a nonpersistent group, it disbands and cannot be restored.
- Joining another group, disconnecting, or restarting the server clears the return permission.
- A crossed-out group icon indicates that you can return.

While a transition awaits confirmation, microphone transmission is paused. If it times out, use **Retry** or press the key again. Proximity audio follows Simple Voice Chat's usual distance and group-type rules.

## Commands

Commands are available to players through `/voicegroup`. Invite and request notifications include clickable actions.

| Command | Action |
| --- | --- |
| `/voicegroup invite <player>` | Invite an online player to your group. |
| `/voicegroup accept <token>` | Accept your invite; the chat button fills in the token. |
| `/voicegroup request <group>` | Request access to a visible, password-protected group by name or UUID. |
| `/voicegroup kick <player>` | Remove a member. Leader only. |
| `/voicegroup transfer <player>` | Transfer leadership. Leader only. |

The creator leads the group. When the leader leaves, the longest-standing remaining member takes over. An ordinary rejoin places a player at the end of this order; returning through group pause restores their saved leader role.

## Configuration

Settings and examples are in [`plugins/SVCBetterGroups/config.yml`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/blob/main/src/main/resources/config.yml).

The `vanillagame.svc_better_groups.use` permission enables `/voicegroup` and is granted to all players by default. Removing members and transferring leadership also require the player to be the current group leader.

<!-- modrinth:end -->

## Development

```sh
./gradlew build
```

Builds the server plugin and all four Fabric clients, runs the tests, and stages the JARs in `build/release/`. Supported versions and dependencies are defined in [`compatibility.json`](compatibility.json).
