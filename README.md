<!-- modrinth:start -->

# Simple Voice Chat Better Groups

<!-- modrinth:exclude:start -->

[![Latest release](https://img.shields.io/github/v/release/Vanilla-Game/simple-voice-chat-better-groups?style=flat-square&logo=github&label=Release)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Vanilla-Game/simple-voice-chat-better-groups/total?style=flat-square&logo=github&label=Downloads)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases)
[![CI Build](https://img.shields.io/github/actions/workflow/status/Vanilla-Game/simple-voice-chat-better-groups/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=Build)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/actions/workflows/build.yml)

<!-- modrinth:exclude:end -->

**Your voice group, without the hassle.**

- **15 languages, chosen automatically.** Chat messages follow each player's Minecraft language. [See the supported locales](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/blob/main/docs/locales.md).
- **Password-free invitations.** Send personal, one-time invitations that expire; recipients accept with a click in chat.
- **Join requests.** Ask to join a password-protected group and let its leader approve in chat.
- **Group leaders.** The creator leads the group and can remove members or transfer leadership.
- **Automatic succession.** When the leader leaves, the longest-standing remaining member takes over.
- **Group pause and return.**\* Switch to proximity chat and return without entering the password, restoring your previous leader role.
- **In-game controls.**\* Use a searchable player picker, leader crowns, member removal buttons, and pause / return buttons and key binds.
- **Server-side commands.** Manage groups with `/voicegroup`; server owners can control access through permissions.
- **Configurable notifications.** Adjust invitation and request expiry, cooldowns, and notification sounds.

\* These features require the Better Groups Fabric client mod.

[![Download](https://img.shields.io/badge/Download-2ea043?style=for-the-badge)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest)
[![Installation](https://img.shields.io/badge/Installation-1f6feb?style=for-the-badge)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/blob/main/README.md#installation)
[![Usage guide](https://img.shields.io/badge/Usage_guide-57606a?style=for-the-badge)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/blob/main/docs/usage.md)

## Features

### Invite and join

Click **+** and choose a player; they click **Accept** in chat to join without a password. Players can also request access to a protected group for its leader to approve.

![Alex invites Steve, who accepts in chat and joins the password-protected group](https://raw.githubusercontent.com/Vanilla-Game/simple-voice-chat-better-groups/455f77860613d351a6b14e8ef84c182717d9d44f/assets/demo-invite.gif)

### Manage your group

See who leads the group, remove members, or transfer leadership with `/voicegroup transfer <player>`. When the leader leaves, a remaining member takes over automatically.

![Alex transfers leadership to Steve and the crown moves to Steve](https://raw.githubusercontent.com/Vanilla-Game/simple-voice-chat-better-groups/455f77860613d351a6b14e8ef84c182717d9d44f/assets/demo-leadership.gif)

### Pause and return

Click **Ⅱ** for proximity chat, then **Return to group** to rejoin without entering the password. The group must still exist; joining another group or disconnecting clears the return permission.

![Steve pauses for proximity chat and returns to Adventure without a password prompt](https://raw.githubusercontent.com/Vanilla-Game/simple-voice-chat-better-groups/455f77860613d351a6b14e8ef84c182717d9d44f/assets/demo-pause.gif)

## Installation

### Server

Install [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat), place the Better Groups server JAR in `plugins/`, and restart. One JAR supports Paper and Folia; Leaf support is experimental.

<!-- modrinth:exclude:start -->

<details>
<summary>Server downloads and compatibility</summary>

<!-- generated:server-downloads:start -->

| Download | Minecraft | Server | Java | Simple Voice Chat |
| --- | --- | --- | --- | --- |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-0.13.0.jar) | `26.1.2` | Paper; Folia; Leaf (experimental) | `25`+ | Bukkit `2.6.16`–`2.6.21`, `2.6.23`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-0.13.0.jar) | `26.2` | Paper; Folia; Leaf (experimental) | `25`+ | Bukkit `2.6.19`–`2.6.21`, `2.6.23`–`2.6.24` |

<!-- generated:server-downloads:end -->

</details>

<!-- modrinth:exclude:end -->

### Client

Players need Simple Voice Chat on their client. For the controls shown above, install Fabric Loader and add Fabric API plus the matching **Better Groups client JAR** to `mods/`. Better Groups on the client is optional; server chat commands work without it.

<!-- modrinth:exclude:start -->

<details>
<summary>Fabric downloads and compatibility</summary>

<!-- generated:fabric-downloads:start -->

| Download | Minecraft | Fabric Loader | Fabric API | Java | Simple Voice Chat |
| --- | --- | --- | --- | --- | --- |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-1.21.11-0.13.0.jar) | `1.21.11` | `0.18.1`+ | `0.139.4+1.21.11`+ | `21`+ | Fabric `2.6.6`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-26.1-0.13.0.jar) | `26.1`–`26.1.2` | `0.18.4`+ | `0.144.3+26.1`+ | `25`+ | Fabric `2.6.14`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-26.2-0.13.0.jar) | `26.2.x` | `0.19.3`+ | `0.152.1+26.2`+ | `25`+ | Fabric `2.6.18`–`2.6.24` |
| [Download](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/download/v0.13.0/svc-better-groups-fabric-26.3-0.13.0.jar) | `26.3.x` | `0.19.5`+ | `0.161.0+26.3`+ | `25`+ | Fabric `2.6.23`–`2.6.24` |

<!-- generated:fabric-downloads:end -->

The 26.3 client is included in the next release. Simple Voice Chat 2.6.23 and 2.6.24 for 26.3 are currently beta builds.

</details>

<!-- modrinth:exclude:end -->

See the [usage guide](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/blob/main/docs/usage.md) for commands, pause controls and behavior, settings, and permissions.

<!-- modrinth:end -->
