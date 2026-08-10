# Simple Voice Chat Better Groups

[![Latest release](https://img.shields.io/github/v/release/Vanilla-Game/simple-voice-chat-better-groups?style=flat-square&logo=github&label=Release)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Vanilla-Game/simple-voice-chat-better-groups/total?style=flat-square&logo=github&label=Downloads)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases)
[![CI Build](https://img.shields.io/github/actions/workflow/status/Vanilla-Game/simple-voice-chat-better-groups/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=Build)](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/actions/workflows/build.yml)

Better group controls for [Simple Voice Chat](https://modrepo.de/minecraft/voicechat/): password-free invites, join requests, group leaders, member removal, and leadership transfer.

The Paper plugin contains all gameplay logic. Players can use every feature through chat; the optional Fabric mod adds buttons and a leader crown to the existing Simple Voice Chat screens.

Developed for [Vanilla Game](https://vanilla-game.ru).

## Features

- One-time invites that expire without exposing a group password.
- Join requests for visible, password-protected groups. The leader accepts them with one click.
- A group leader who can remove members or transfer leadership.
- Automatic succession to the longest-standing member when the leader leaves.
- Group chat announcements when a player joins, including who invited them.
- Messages in 15 languages, selected from each player's Minecraft locale.

## Installation

### Players

You do not need the client mod to use Better Groups on a supported server.

For buttons and the leader crown, download the matching Fabric jar from [GitHub Releases](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest) and place it in your `mods` directory:

- `fabric-26.1` for Minecraft 26.1, 26.1.1, or 26.1.2;
- `fabric-26.2` for Minecraft 26.2.x.

Install only one variant. Fabric API and Simple Voice Chat Fabric are required. On servers without a compatible Better Groups plugin, the extra controls stay hidden.

### Server owners

Download the Paper jar from [GitHub Releases](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest), place it in the server's `plugins` directory, and restart the server. Simple Voice Chat is a required dependency. Plugin reload tools are not supported.

Paper is fully tested. Leaf support is experimental and smoke-tested.

## Compatibility

- Minecraft server `26.1.2` or `26.2`
- Java `25` or newer
- Simple Voice Chat Bukkit `2.6.16`–`2.6.21` on Minecraft `26.1.2`, or `2.6.19`–`2.6.21` on Minecraft `26.2`

For the optional client mod:

- Minecraft 26.1–26.1.2 requires Fabric Loader `0.18.4`+, Fabric API `0.144.3+26.1`+, and Simple Voice Chat Fabric `2.6.14`–`2.6.22`;
- Minecraft 26.2.x requires Fabric Loader `0.19.3`+, Fabric API `0.152.1+26.2`+, and Simple Voice Chat Fabric `2.6.18`–`2.6.22`.

Client and server Simple Voice Chat patch versions may differ when both are in the supported `2.6` range. SVC `2.5.x`, `2.7.x`, beta, and unlisted builds are unsupported. The client addon does not support Forge, NeoForge, or Quilt.

See [`compatibility.json`](compatibility.json) for the exact tested combinations.

## Usage

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

The server checks every action. Installing or modifying the client mod cannot grant leader permissions.

## Leadership

The group creator becomes its first leader. If the leader leaves the group or quits the server, the longest-standing remaining member takes over. A returning former leader joins the end of that order. The current leader can hand the role to another member with `/voicegroup transfer`.

Leadership exists only in memory because the public Simple Voice Chat API does not expose historical group ownership. If the plugin is reloaded while groups still exist, it cannot safely identify their leaders. For those groups, leader-only actions are denied and the crown stays hidden until the groups are recreated.

## Server configuration

Settings are generated in `plugins/SVCBetterGroups/config.yml`. Server owners can change invite and join-request expiration, cooldown, and notification sound. Set a cooldown to `0` or a sound to `none` to disable it. Restart the plugin or server after editing the file.

The `vanillagame.svc_better_groups.use` permission allows `/voicegroup` and is granted to all players by default. Leader-only commands always require actual group leadership.

## Development

Run `./gradlew build` to execute the tests and build the Paper plugin and both Fabric variants. Release artifacts are staged in `build/release/`.
