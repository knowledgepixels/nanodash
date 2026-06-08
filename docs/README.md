# Nanodash design docs

Design notes and proposals for Nanodash features. Each doc carries a
`**Status:**` line near the top; this index is the quick overview.

Status legend: ✅ Implemented · 🚧 In progress · 📋 Proposed

| Doc | Status | Summary |
| --- | --- | --- |
| [userlist-views](userlist-views.md) | ✅ Implemented | Human / Software / Non-Approved user lists as published views on `UserListPage` |
| [presets](presets.md) | 🚧 In progress | Publishable bundles of default views + roles, assignable to resources ([#302](https://github.com/knowledgepixels/nanodash/issues/302)) |
| [magic-query-params](magic-query-params.md) | 📋 Proposed | Session-bound view-query placeholders (`LOCALPUBKEY`, `SITEURL`); path to replace the custom introductions table with a proper view |
| [role-specific-views](role-specific-views.md) | 🚧 In progress | View **action buttons** gated to a role tier (Maintainer, …) or specific role, via `gen:isVisibleTo` on the action node |
| [custom-domains](custom-domains.md) | 📋 Proposed | Serve a user's profile from their own domain |
| [draft-with-ai](draft-with-ai.md) | 📋 Proposed | Server-side "Draft with AI" nanopub authoring |

When a doc's status changes, update both its `**Status:**` line and the row here.
