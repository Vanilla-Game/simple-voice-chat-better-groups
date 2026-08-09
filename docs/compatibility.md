# Simple Voice Chat compatibility automation

Source of truth: `client-fabric/compatibility.properties`
(`minecraft`, `fabric-api`, `voicechat.compile`, `voicechat.range`,
`voicechat.tested`). The client always compiles against `voicechat.compile`
(the floor) so newer-only APIs cannot creep in.

## Harness

`voicechat-compat.yml` (reusable, one leg per SVC version):

1. Compile `:client-fabric:build -PvoicechatVersion=<artifact>` — the mixins
   reference SVC internals directly, so a removed class/field/method fails here.
2. Headless `runClient` with `-PvoicechatCompatCheck=true` under Xvfb: the
   entrypoint force-loads every mixin target class (`Class.forName(…, false)`);
   `required: true` turns any missing injection point into a non-zero exit.
   The compat flavor relaxes the `fabric.mod.json` range to `*` (Fabric Loader
   would otherwise reject a candidate before mixins are checked) and renames
   the jar to `…-compat-test-…` so it can never ship.

Every PR runs the harness against **all** tested versions — it guards our own
changes as much as SVC updates.

## Discovery

`voicechat-discovery.yml` (daily cron + manual dispatch) queries Modrinth for
new SVC Fabric releases for our Minecraft version, filters to listed releases
strictly newer than the floor, sorts by semver, runs the harness, and opens a
draft `fix:` PR widening `voicechat.range` across the **contiguous green
prefix** only. A red version is never skipped, and the prefix never crosses a
minor-version boundary — a shipped range must not cover an untested gap that a
later backport could land in. `voicechat.compile` is never bumped
automatically.

## Manual procedures

- Test any SVC version locally: `./gradlew :client-fabric:build
  -PvoicechatVersion=fabric-X.Y.Z+MC` (compile), plus `runClient` with
  `-PvoicechatCompatCheck=true` for the mixin check.
- Lower the floor: verify the older versions through the harness (a draft PR
  with them added to `voicechat.tested` runs the full matrix), then move
  `voicechat.compile` and the range's lower bound together.
- New SVC minor line (e.g. 2.7.x): discovery reports it but stops widening; a
  human reviews the mixin surface and widens deliberately.

## Fabric API floor

`0.152.1` — the oldest fabric-api build published for Minecraft 26.2; every
26.2 build ships the same networking module (the only part we use). The client
compiles against the floor for the same reason as with SVC.
