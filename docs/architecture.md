# Architecture

This repository builds two artifacts:

- a Paper plugin that owns group state and authorizes every action;
- an optional Fabric mod that adds controls to the Simple Voice Chat UI.

The client is never trusted with membership or leadership decisions. Players
without the client mod retain the complete command workflow.

## Stable identifiers

The public display name, artifact name, and machine identifiers intentionally
use different formats:

| Purpose | Value |
| --- | --- |
| Display name | Simple Voice Chat Better Groups |
| Artifact names | `svc-better-groups[-fabric]` |
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
