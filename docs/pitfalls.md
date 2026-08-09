# Pitfalls (learned the hard way)

## Tests

- Mockito: never create or call mocks *inside* stubbing/matcher expressions.
  `when(x.f(connection(null)))`, `eq(mock.getId())` between matcher recordings
  → `UnfinishedStubbingException` / `InvalidUseOfMatchersException`. Hoist
  values into locals first. This bit this repo three times.
- The build does not validate client lang JSONs: a syntactically broken
  `assets/**/lang/*.json` silently disables *all* client translations (raw
  keys in tooltips). Server-side `.properties` are covered by the parity test;
  the client side is not.

## GitHub / CI

- `gh pr checks` immediately after PR creation reports "no checks reported" —
  a wait-loop treats that as success and falls through. Sleep ~90 s first and
  require a positive pass count before auto-merging.
- Stacked PRs: GitHub only retargets a PR to `main` when its base branch is
  *deleted*. Merging a stacked PR while the base branch survives lands the
  squash in the stale base (happened with the rename). Verify
  `baseRefName == main` before merging anything.
- Workflow-created PRs (GITHUB_TOKEN) do not trigger CI; close/reopen them.

## release-please

- It will not refresh a release-PR branch whose rendered content is unchanged,
  even when the branch conflicts with main — close the PR with
  `--delete-branch` and re-run the workflow via `workflow_dispatch`.
- A `!` breaking marker on a 0.x version proposes 1.0.0 unless
  `bump-minor-pre-major: true` is set (it is).

## Wire format

- The client-server protocol bytes are pinned by golden vectors in
  `GroupSyncProtocolTest`; the client payload codecs mirror them by hand.
  Any byte change without a protocol VERSION bump breaks deployed clients
  while every unit test stays green — the vectors are the only tripwire.
