# Simple Voice Chat Better Groups

**Better controls for Simple Voice Chat groups.**

Simple Voice Chat Better Groups adds passwordless invites, join requests, group leaders, member removal, and leadership transfer to [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat).

The Paper plugin handles permissions and group changes on the server. An optional Fabric client mod adds invite, request, and remove buttons to the existing group UI, along with a crown marking the current leader.

## Features

- **One-click invites** that never expose the password — the built-in invite pastes it right into a command for every invitee to see; here it's a one-time code that expires in minutes.
- **A leader with a golden crown.** The group's creator leads it: only the leader kicks members and approves requests. The crown passes to the longest-standing member when the leader leaves, or by hand with `/voicegroup transfer`.
- **Join requests for locked groups.** Ask instead of guessing the password: the leader gets a one-click **[Accept]** with an anvil *clang* that's hard to miss (the sound is configurable).
- **Chat announcements** when someone joins the group — including who invited them.

## Installation

Grab the **Paper** file for a Minecraft 26.1.2 or 26.2 server — that's the plugin itself, and it works standalone: players don't need to install anything. Paper is fully tested; Leaf support is experimental and smoke-tested.

The **Fabric** files are optional client mods for players who want buttons instead of commands. Choose exactly one: `fabric-26.1` for Minecraft 26.1, 26.1.1, or 26.1.2; `fabric-26.2` for Minecraft 26.2.x.

- **+** on the group screen opens a searchable player list — click someone to invite them;
- **×** next to each member kicks them (only the leader sees it);
- a golden crown marks the current leader;
- a door button on a locked group's password screen sends a join request.

On servers without Better Groups, its extra controls stay hidden, so the client mod can remain installed when joining other servers.

Java 25 is required. Supported Simple Voice Chat versions are:

- server 26.1.2: Bukkit SVC 2.6.16–2.6.21;
- server 26.2: Bukkit SVC 2.6.19–2.6.21;
- Fabric client 26.1.x: SVC 2.6.14–2.6.22;
- Fabric client 26.2.x: SVC 2.6.18–2.6.22.

Client and server SVC patch versions may differ when both are within the compatible 2.6 line. SVC 2.5.x, 2.7.x, beta, and unlisted builds are not supported. The client addon is Fabric-only; Forge, NeoForge, and Quilt are not supported.

## Usage

Everything works from chat, no client mod needed:

- `/voicegroup invite <player>` — send a clickable invite
- `/voicegroup accept` — join by clicking the invite
- `/voicegroup kick <player>` — remove a member (leader only)
- `/voicegroup transfer <player>` — hand leadership to another member
- `/voicegroup request <group>` — ask to join a password-protected group

---

*Minecraft server 26.1.2–26.2 · Fabric client 26.1.x or 26.2.x · Java 25 · 15 languages, matches your game language*
