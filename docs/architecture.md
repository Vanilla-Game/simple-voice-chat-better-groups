# Architecture decisions

Two artifacts, one project: the authoritative Paper plugin (repo root) and an
optional Fabric client mod (`client-fabric/`). They are separate Gradle modules
by necessity — Paper API and Fabric Loom toolchains cannot share a module.

## Naming tiers

Three deliberate tiers; the mismatch between them is intentional — do not "fix" it.

| Tier | Value | Where |
| --- | --- | --- |
| Display | Simple Voice Chat Better Groups | Modrinth title, version subtitles, mod list name, README title |
| Kebab | `svc-better-groups[-fabric]` | jar names, release tags, release-please package name |
| Machine | `svc_better_groups[_client]` | mod id, plugin channels, translation keys, assets namespace, permission `vanillagame.svc_better_groups.use` |

Java packages: `ru.vanillagame.voicechat.bettergroups[.client]`. Config folder:
`plugins/SVCBetterGroups/`. The Modrinth slug survives renames as an opaque id;
the project is addressed by id `scOrYDTf` in CI.

## Command ABI

`/voicegroup` is the primary command (sits next to Simple Voice Chat's own
`/voicechat`, `/voicemute` in tab completion). `/vcgroup` is a **permanent**
alias: it is the wire name — the client detects it in the command tree and
sends it (`ServerSupport`). Removing the alias breaks every released client.

## Group leadership

`GroupLeadershipRegistry` mirrors Simple Voice Chat's membership through the
public events (Create/Join/Leave/RemoveGroupEvent) and never diverges from it.
Verified against SVC sources (branch 26.2): API `setGroup(null)` funnels through
`ServerGroupManager.leaveGroup` and fires `LeaveGroupEvent`; `setGroup(group)`
fires `JoinGroupEvent`; a transient voice-connection drop does **not** change
membership. All registry operations are idempotent (quit may deliver duplicate
signals).

Rules: the creator is the first leader; on leader leave/switch/quit the role
passes to the longest-standing member (LinkedHashSet join order); `/voicegroup
transfer` moves it by hand without reordering; a returning former leader joins
at the end of the order. **Fail-closed**: a group whose creation was not
observed (plugin restarted, persistent group) has an unknown leader — kick,
transfer and join requests are denied rather than guessed.

## Leader sync protocol

Channels `svc_better_groups:hello` (client→server, 1 byte version) and
`svc_better_groups:leader_state` (server→client: version u8, flags u8,
optional group UUID, optional leader UUID as two big-endian longs).

- The byte layout is **frozen by golden vectors** in `LeaderSyncProtocolTest`.
  Version 1 semantics never change; any format change ships as a new
  `LeaderSyncProtocol.VERSION` plus an explicit decision about older clients.
- Mixed client versions are the normal operating state: the server answers only
  compatible hellos; incompatible clients silently keep the command workflow.
- The client sends hello at JOIN **and** on `ServerboundPlayChannelEvents.REGISTER`
  (Paper announces channels in a `minecraft:register` packet that arrives after
  the Fabric JOIN event). A re-announcement resets the handshake — the server
  plugin may have been reloaded and lost its state. UNREGISTER clears the cache.
- protobuf was evaluated and rejected (1.7 MB dependency + shading hazards for
  ~34 bytes of payload); a shared protocol Gradle module is deferred until the
  packet count grows beyond two.

## Invites and requests

Both are one-time SecureRandom 192-bit tokens, in memory only, with TTL and
invalidation on quit/group removal. Design differences:

- An **invite** is bound to the invited player's UUID (only they can accept)
  and carries the inviter's id/name for join-announcement attribution.
- A **request** token is handed to the leader, but approval is authorized
  against whoever holds leadership **at click time**, not whoever received the
  message — leadership may have transferred meanwhile.
- Invites reach players in *other* groups; accepting switches groups (the
  invite message carries a gold warning). Only own-group members are rejected.
- Requests are only offered for visible, password-protected groups; hidden
  groups are excluded from name and UUID resolution so their existence cannot
  be probed.

Per-pair cooldowns (inviter+target, requester+group) reuse one store class.

## Client UI integration

Mixin surface (all `required: true, defaultRequire: 1` — a missing injection
point fails class load, which the compat harness exploits):

- `GroupScreenMixin` — "+" opens the invite picker.
- `GroupEntryMixin` — crown glyph via `@Redirect` on the single
  `Component.literal` call in `extractContent`; kick buttons gated by
  client-side leader state (server still authorizes).
- `EnterPasswordScreenMixin` — the join-request door button lives on the
  password screen because `JoinGroupList.mouseClicked` consumes row clicks
  itself and never dispatches to entry child widgets.

UI icons (crown `\uE000`, ajar door `\uE001`) are 8x8 white bitmap glyphs in
the mod font `svc_better_groups_client:icons` — the unifont fallback renders
non-ASCII glyphs squished. Color comes from the component style (gold crown).

The invite picker lists players from SVC's own `ClientPlayerStateManager` and
polls it once a second while open: SVC pushes state updates only to its own
screens by a hardcoded list. Voice-disconnected players stay listed (gray
name) — the invite arrives in chat regardless; only own-group members are
hidden.
