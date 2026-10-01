# Usage guide

Better Groups requires the server plugin and Simple Voice Chat on the server and
clients. The optional Better Groups Fabric mod adds the player picker, leader's
crown, member removal button, and pause / return controls.

## Invites and join requests

With the client mod, open your group, click **+**, and choose a player from the
searchable list. With just the server plugin, use `/voicegroup invite <player>`.
The recipient accepts the personal, expiring invitation in chat without entering
the group's password.

Use `/voicegroup request <group>` to ask to join a visible, password-protected
group. Its leader can approve with the clickable action in chat.

On servers without Better Groups, the client's **+** button uses Simple Voice
Chat's native invite command. Leadership, join requests, and group pause require
the Better Groups server plugin.

Chat messages follow each player's Minecraft language, with 15 translations
available. The pause controls have English and Russian translations.

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

## Group pause

Click **Ⅱ** beside **+**, or assign **Pause / return to group** in **Controls → Key Binds → Better Groups**. The pause button appears only after the server confirms support.

Pausing leaves the group and switches to Simple Voice Chat's normal proximity chat. Click **Return to group** on the group selection screen, or press the same key again, to return. The server remembers the return permission; the client does not store the password.

- Returning to the same group restores your previous leader role.
- If the last member leaves a nonpersistent group, it disbands and cannot be restored.
- Joining another group, disconnecting, or restarting the server clears the return permission.
- A crossed-out group icon indicates that you can return.

While a transition awaits confirmation, microphone transmission is paused. If it times out, use **Retry** or press the key again. Proximity audio follows Simple Voice Chat's usual distance and group-type rules.

## Configuration and permissions

Settings and examples are in [`plugins/SVCBetterGroups/config.yml`](https://github.com/Vanilla-Game/simple-voice-chat-better-groups/blob/main/src/main/resources/config.yml).

The `vanillagame.svc_better_groups.use` permission enables `/voicegroup` and is granted to all players by default. Removing members and transferring leadership also require the player to be the current group leader.
