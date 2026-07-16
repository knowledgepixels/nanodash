# Nanodash design docs

Design notes and proposals for Nanodash features. Each doc carries a
`**Status:**` line near the top; this index is the quick overview.

Status legend: ✅ Implemented · 🚧 In progress · 📋 Proposed

| Doc | Status | Summary |
| --- | --- | --- |
| [userlist-views](userlist-views.md) | ✅ Implemented | Human / Software / Non-Approved user lists as published views on `UserListPage` |
| [presets](presets.md) | ✅ Implemented | Publishable bundles of default views + roles, assignable to (and deactivatable on) resources ([#302](https://github.com/knowledgepixels/nanodash/issues/302)) |
| [magic-query-params](magic-query-params.md) | ✅ Implemented | Session-bound view-query placeholders (`LOCALPUBKEY`, `SITEURL`, `CURRENTUSER`); replaced the custom introductions table with proper views (`ProfileIntroItem` removed) |
| [role-specific-views](role-specific-views.md) | ✅ Implemented | View **action buttons** gated to a role tier (Maintainer, …) or specific role, via `gen:isVisibleTo` on the action node |
| [structural-position](structural-position.md) | ✅ Implemented | `gen:hasStructuralPosition` `<section>.<sub>.<label>` strings order & group views on a page; strict format, primary digits 3–7 (intro…outro) in use ([#279](https://github.com/knowledgepixels/nanodash/issues/279)) |
| [fill-modes](fill-modes.md) | ✅ Implemented | Publish-form fill modes (use / supersede / derive / override): which IDs & root definition are kept vs. re-minted; derive now resets root, new `override` mode added ([#527](https://github.com/knowledgepixels/nanodash/issues/527)) |
| [space-ref-identity](space-ref-identity.md) | 🚧 In progress | A space's identity is IRI + root-definition NPID (rival roots = distinct spaces); ref-keyed `SpaceRepository` + ref-scoped per-space authority queries shipped; one-`Space`-per-ref identity + disambiguation UI still to come |
| [shacl-alignment](shacl-alignment.md) | 📋 Proposed | Build on the SHACL standard: adopt `sh:`/`dash:` constraint terms inside templates (A) and export SHACL shapes for external validation (B) |
| [custom-domains](custom-domains.md) | 📋 Proposed | Serve a user's profile from their own domain |
| [draft-with-ai](draft-with-ai.md) | 📋 Proposed | Server-side "Draft with AI" nanopub authoring |
| [claude-code-chat](claude-code-chat.md) | 🚧 In progress | Chat panel backed by the user's local Claude Code, acting on Nanodash via an MCP endpoint (Tier 2 of [#434](https://github.com/knowledgepixels/nanodash/issues/434)) |

When a doc's status changes, update both its `**Status:**` line and the row here.
