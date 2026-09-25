# Nanodash design docs

Design notes and proposals for Nanodash features. Each doc carries a
`**Status:**` line near the top; this index is the quick overview.

Status legend: ✅ Implemented · 🚧 In progress · 📋 Proposed

| Doc | Status | Summary |
| --- | --- | --- |
| [resource-models](resource-models.md) | ✅ Implemented | `ViewList`, `ButtonList`, `SpaceUserList` and `ItemListPanel` take the resource as an `IModel` and derive from it at render time, so a page restored on browser refresh is current ([#459](https://github.com/knowledgepixels/nanodash/issues/459)) |
| [userlist-views](userlist-views.md) | ✅ Implemented | Human / Software / Non-Approved user lists as published views (now displayed on the home page; `UserListPage` retired) |
| [presets](presets.md) | ✅ Implemented | Publishable bundles of default views + roles, assignable to (and deactivatable on) resources ([#302](https://github.com/knowledgepixels/nanodash/issues/302)) |
| [magic-query-params](magic-query-params.md) | ✅ Implemented | Session-bound view-query placeholders (`LOCALPUBKEY`, `SITEURL`, `CURRENTUSER`); replaced the custom introductions table with proper views (`ProfileIntroItem` removed) |
| [role-specific-views](role-specific-views.md) | ✅ Implemented | View **action buttons** gated to a role tier (Maintainer, …) or specific role, via `gen:isVisibleTo` on the action node |
| [section-anchors](section-anchors.md) | ✅ Implemented | Display views generate fragment identifiers (`…/spaces/nanosuggestions#messages`) from their titles, with a copy-link handle and Ajax-aware scrolling |
| [structural-position](structural-position.md) | ✅ Implemented | `gen:hasStructuralPosition` `<section>.<sub>.<label>` strings order & group views on a page; strict format, primary digits 3–7 (intro…outro) in use ([#279](https://github.com/knowledgepixels/nanodash/issues/279)) |
| [space-namespace-prefixes](space-namespace-prefixes.md) | ✅ Implemented | `nt:hasPrefix "~~SPACE~~/"` / `"~~NAMESPACE~~"` mint resources under the space or maintained resource the form was opened from, with a picker when the `context` param determines none ([#571](https://github.com/knowledgepixels/nanodash/issues/571)) |
| [reserved-local-names](reserved-local-names.md) | ✅ Implemented | A value minted under one of the names a nanopublication keeps for its own graphs or signature is refused, in the field, before publishing, and in the template ([#29](https://github.com/knowledgepixels/nanodash/issues/29)) |
| [new-uri-placeholder](new-uri-placeholder.md) | ✅ Implemented | `nt:NewUriPlaceholder` marks a placeholder whose value names a resource that does not exist yet; publishing is refused when that identifier is already in use ([#646](https://github.com/knowledgepixels/nanodash/issues/646)) |
| [unattached-role-warning](unattached-role-warning.md) | ✅ Implemented | The About tab of a space warns when a view lists the holders of a role the space has not attached, which leaves it empty ([#648](https://github.com/knowledgepixels/nanodash/issues/648)) |
| [html-literals](html-literals.md) | ✅ Implemented | Literals declared `rdf:HTML` render as HTML and are written with a rich-text editor; sanitized before publishing and at render time ([#378](https://github.com/knowledgepixels/nanodash/issues/378)) |
| [fill-modes](fill-modes.md) | ✅ Implemented | Publish-form fill modes (use / supersede / derive / override): which IDs & root definition are kept vs. re-minted; derive now resets root, new `override` mode added ([#527](https://github.com/knowledgepixels/nanodash/issues/527)); `param_` values override the source instead of blocking its fill ([#73](https://github.com/knowledgepixels/nanodash/issues/73)) |
| [space-ref-identity](space-ref-identity.md) | 🚧 In progress | A space's identity is IRI + root-definition NPID (rival roots = distinct spaces); ref-keyed `SpaceRepository` + ref-scoped per-space authority queries shipped; one-`Space`-per-ref identity + disambiguation UI still to come |
| [shacl-alignment](shacl-alignment.md) | 📋 Proposed | Build on the SHACL standard: adopt `sh:`/`dash:` constraint terms inside templates (A) and export SHACL shapes for external validation (B) |
| [custom-domains](custom-domains.md) | 📋 Proposed | Serve a user's profile from their own domain |
| [site-views](site-views.md) | 🚧 In progress | An instance as the website of one space: the space's page as home page, the space as everyone's context, its name and logo instead of Nanodash's, outside links left plain (Stage A, `NANODASH_SITE_SPACE`); several sites, each on its own host name, and custom domains to come ([#692](https://github.com/knowledgepixels/nanodash/issues/692)) |
| [draft-with-ai](draft-with-ai.md) | 📋 Proposed | Server-side "Draft with AI" nanopub authoring |
| [uri-schemes](uri-schemes.md) | ✅ Implemented | Accepts and renders `ipfs:`, `ipns:`, `did:` and `at:` URIs alongside `http(s)`, with configurable outbound resolvers and scheme-aware short labels ([#655](https://github.com/knowledgepixels/nanodash/issues/655)) |
| [locked-prefilled-values](locked-prefilled-values.md) | ✅ Implemented | `locked=` states that a value pre-filled via URL args cannot be changed in the form (per field and per repetition, so pre-filled keys can be fixed while more can still be added); `locked-statements=` fixes the set of repetitions ([#678](https://github.com/knowledgepixels/nanodash/issues/678)) |
| [protected-nanopublications](protected-nanopublications.md) | ✅ Implemented | Publishing nanopublications typed `npx:ProtectedNanopub` from a deployment connected to a local/private registry: an unlisted pubinfo template behind its own checkbox, a deployment default, and the cases where protection is not the user's to turn off ([#671](https://github.com/knowledgepixels/nanodash/issues/671)) |
| [claude-code-chat](claude-code-chat.md) | 🚧 In progress | Chat panel backed by the user's local Claude Code, acting on Nanodash via an MCP endpoint (Tier 2 of [#434](https://github.com/knowledgepixels/nanodash/issues/434)) |

When a doc's status changes, update both its `**Status:**` line and the row here.
