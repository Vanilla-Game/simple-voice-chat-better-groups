# Architecture

This repository builds four release artifacts:

- one Paper-compatible plugin that owns group state and authorizes every action;
- an optional Fabric 1.21.11 mod built with Mojang mappings, remapped to intermediary names, and targeting Java 21;
- an optional Fabric 26.1.x mod that adds controls to the Simple Voice Chat UI;
- an optional Fabric 26.2.x mod built from the same client sources.

The client is never trusted with membership or leadership decisions. Players
without the client mod retain the complete command workflow.

## Compatibility boundary

The server jar targets the lowest stable baseline, Paper 26.1.2 and Java 25,
and is tested unchanged on Paper 26.1.2 and 26.2. Leaf uses the Paper API but
is treated as experimental: minimum and maximum supported SVC releases receive
blocking smoke tests on both Minecraft versions. Server versions 26.1 and
26.1.1 are outside the contract.

The three Fabric jars deliberately share protocol and mod ID while carrying
Minecraft-specific dependency metadata. The 1.21.11 jar uses a small legacy UI
and networking adapter over the shared protocol; the 26.1 jar covers 26.1,
26.1.1, and 26.1.2; the 26.2 jar covers 26.2.x. A client must install exactly
one. Forge, NeoForge, and Quilt clients are not supported.

Compatibility is defined by `compatibility.json`. The complete published jar,
not a separately recompiled substitute, is launched for every declared
Minecraft/SVC pair. SVC server and client patch numbers may differ inside the
supported 2.6 line, but 2.5.x, 2.7.x, beta, and unlisted builds are excluded.

## Stable identifiers

The public display name, artifact name, and machine identifiers intentionally
use different formats:

| Purpose | Value |
| --- | --- |
| Display name | Simple Voice Chat Better Groups |
| Artifact names | `svc-better-groups`, `svc-better-groups-fabric-1.21.11`, `svc-better-groups-fabric-26.1`, `svc-better-groups-fabric-26.2` |
| Protocol and resource namespaces | `svc_better_groups[_client]` |

`/voicegroup` is the primary command. `/vcgroup` is a permanent compatibility
alias used by released clients and must not be removed.

## Leadership

The server mirrors Simple Voice Chat membership through its public events. The
group creator is the initial leader. When the leader leaves, leadership passes
to the longest-standing remaining member; rejoining puts a player at the end
of that order. Manual transfer does not change the succession order.

Leadership is fail-closed. If the plugin did not observe a group's creation, it
cannot reconstruct the creator or historical join order from the public API.
For such a group, leader-only actions remain disabled and clients receive no
leader identity.

## Client protocol

The client first negotiates a protocol version, then receives group state on a
separate server-to-client channel. The client never submits group or leader
identities. A client with no compatible handshake keeps its extra UI disabled.

Published message layouts are immutable. Any byte-level format change requires
a new protocol version and updated golden vectors in `GroupSyncProtocolTest`.
Mixed client versions are expected during deployments, so the server must
continue to reject incompatible handshakes without affecting commands.

## Invites and join requests

Invites and join requests use random, single-use, expiring tokens stored only in
memory. An invite is bound to its recipient. A join request is authorized
against the current leader when it is approved, because leadership may change
after the request is sent.

Hidden groups are excluded from join-request lookup. This prevents name or UUID
probing from revealing their existence.
