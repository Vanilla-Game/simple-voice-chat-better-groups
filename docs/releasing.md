# Releasing

One unified cycle for both artifacts (decided after trying independent
versioning: our change cadence is coupled in practice, and the ecosystem
precedent for communicating client-server pairs — Simple Voice Chat, Plasmo
Voice — is lockstep; "install the same number on both sides" answers the
compatibility question).

## Flow

1. Conventional commits merged into `main` update the single release-please PR
   (`bump-minor-pre-major: true` — a `!` breaking marker on 0.x bumps the minor,
   not to 1.0.0; `.github` and `README.md` changes do not trigger releases).
2. Merging the release PR creates tag `v<version>` and a GitHub Release; the
   workflow builds once and attaches **both** jars.
3. mc-publish then publishes two Modrinth versions with the same number:
   `paper-<v>+<mc>` (loaders paper, purpur) and `fabric-<v>+<mc>` (loader
   fabric, dependencies simple-voice-chat + fabric-api). Subtitles follow the
   henkelmax convention: `[PAPER][26.2] Simple Voice Chat Better Groups 0.5.0`.
   The Minecraft version is read from `client-fabric/compatibility.properties`.

Historical tags: server line `v0.1.0..` continues; client-only releases
`svc-better-groups-fabric-v0.1.0..v0.3.0` predate unification and keep their
prefixed tags (unprefixed they would collide with server numbers).

## Modrinth

- Project id `scOrYDTf`; secret `MODRINTH_TOKEN` (scope: Create versions only;
  expires — refresh via `gh secret set MODRINTH_TOKEN`).
- The listing description's tracked source is `assets/modrinth-description.md`:
  edit there, paste on the site.
- Dependencies on Modrinth are "any version" by design — Modrinth cannot
  express ranges; the authoritative range lives in `fabric.mod.json` and is
  enforced by Fabric Loader.
- One project serves both files: the `/plugin/…` site view filters versions to
  plugin loaders, `/mod/…` to mod loaders (the Simple Voice Chat pattern).

## Deploy checklist (own servers)

- Ship server and client together whenever the release notes mention channel,
  protocol, or identifier changes; otherwise server first, clients catch up
  (a newer client stays inactive against an older server by design).
- 0.5.0 specifically: config folder moved to `plugins/SVCBetterGroups/`, the
  permission node changed to `vanillagame.svc_better_groups.use`.

## Escape hatches

- `release-please.yml` has `workflow_dispatch`: if the release PR is stale or
  conflicted, close it (`--delete-branch`) and dispatch the workflow — it
  recreates the PR from current main. release-please does **not** refresh a PR
  branch whose rendered content is unchanged ("PR remained the same").
- CI does not run on PRs created by workflows (GITHUB_TOKEN); close and reopen
  such a PR to trigger checks.
