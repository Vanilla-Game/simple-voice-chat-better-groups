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

Grab the **Paper** file for your server — that's the plugin itself, and it works standalone: players don't need to install anything.

The **Fabric** file is an optional client mod for players who want buttons instead of commands:

- **+** on the group screen opens a searchable player list — click someone to invite them;
- **×** next to each member kicks them (only the leader sees it);
- a golden crown marks the current leader;
- a door button on a locked group's password screen sends a join request.

On servers without Better Groups, its extra controls stay hidden, so the client mod can remain installed when joining other servers.

## Usage

Everything works from chat, no client mod needed:

- `/voicegroup invite <player>` — send a clickable invite
- `/voicegroup accept` — join by clicking the invite
- `/voicegroup kick <player>` — remove a member (leader only)
- `/voicegroup transfer <player>` — hand leadership to another member
- `/voicegroup request <group>` — ask to join a password-protected group

---

*Minecraft 26.2 · Simple Voice Chat 2.6.18+ · 15 languages, matches your game language*
