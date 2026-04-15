# Server-side "Draft with AI" for Nanopub Authoring

Add a server-side "Draft with AI" feature to Nanodash that calls a model
provider (Claude / OpenAI / Gemini / OpenAI-compatible) directly from the
backend and returns an unsigned TriG draft the user can then sign and publish
via Nanodash's normal flow. Users interact through a chat-style panel inside
Nanodash -- no external AI tool required. Supports per-user BYOK keys and an
optional operator-paid free tier, with cost controls via prompt caching, model
tiering, and per-user quotas.

This is an alternative to -- and composes with -- the "Ask AI" hand-off
approach proposed in [#434](https://github.com/knowledgepixels/nanodash/issues/434).
#434 packages context and hands it to the user's own external AI tool;
this design puts the AI directly inside Nanodash.

## Motivation

#434 serves users who already run a local AI tool (Claude Code, Cursor,
ChatGPT, ...) and just need a way to get Nanodash context + rules into it. But
many Nanodash users -- especially researchers coming to publish a nanopub once
-- don't use external AI tools at all and shouldn't have to sign up for one to
get help drafting a template or query. A native "Draft with AI" feature closes
the loop entirely inside Nanodash, at the cost of substantially more backend
work and a real operational footprint (key management, cost, rate limiting).

## Shared UI components (used by both #434 and this design)

Both Tier 1 (#434, hand-off to external AI) and Tier 3 (this design,
server-side AI) share the same user-facing surface for collecting requests.
The only difference is the dispatch mechanism: #434 copies to clipboard / opens
a URL; this design calls the backend and streams a response. The spec below is
duplicated in #434's issue body; if the two drift, #434 is the source of truth
for the shared surface.

### Entry points

Pages that get an **Ask AI** button:

- Template editor
- Query editor
- Resource view editor
- Publish form
- Nanopub view / `_nanopub_trig` preview page (for iteration on a draft)
- Global top-nav "Ask AI" for the "nothing in progress yet" case

### Task input and quick actions

The panel/modal contains:

- **Task textarea** -- free text, with a context-appropriate placeholder hint
  (e.g. on the template editor: *"Create a template for describing research
  software with name, version, license, and repository URL"*).
- **Quick-action buttons** -- page-specific presets that prefill the textarea:
  - Template editor: *Draft new template from description*, *Add a field
    for...*, *Convert this field to a guided-choice lookup*, *Review for
    best-practice issues*
  - Query editor: *Write SPARQL for...*, *Add a multi-value parameter*,
    *Explain why this returns no results*, *Add a label column*
  - View editor: *Create a view showing X on Y pages*, *Add action templates*
  - Publish form: *Help me fill this out for...*, *Suggest values for this
    field*
  - Nanopub view page: *Explain this nanopub*, *Create a similar one*, *Write a
    query to find more like this*

### "Context being sent" panel

A collapsible section showing the chunks Nanodash is auto-attaching alongside
the task: current draft TriG, template URI, user ORCID, current space, rules
file. Each chunk has a checkbox so users can trim what gets sent. No surprises.

### Provider picker

Dropdown of AI providers (Claude.ai, ChatGPT, Gemini, Claude Code, Cursor,
custom). Built-in registry + user-configurable custom entries in profile
settings. Nanodash remembers the last used provider.

## Design (Tier 3-specific)

### Chat-style "Draft with AI" panel

Uses the shared UI components above. Instead of a copy/hand-off modal (#434),
it opens a chat panel that stays docked while the user iterates.

Flow for each turn:

1. User types a task (or picks a quick-action), optionally adjusts the "context
   being sent" checkboxes.
2. Nanodash assembles the prompt: `<authoring rules prefix>` + `<selected
   context chunks>` + `<conversation history>` + `<user message>`.
3. Backend calls the configured LLM provider and streams the response back to
   the panel.
4. A TriG block in the response is extracted, validated against nanopub
   structural rules, and shown as a preview card with **Copy**, **Preview**
   (opens `/view?_nanopub_trig=...`), and **Edit in form** buttons.
5. User sends a follow-up message to iterate, or hands the draft to the publish
   form.

The authoring-rules prefix is cached via the provider's prompt-caching
mechanism, so follow-ups within the cache TTL pay cache-read rates instead of
full input rates.

### Backend LLM client

A thin provider interface with at least one implementation for the first PR:

```java
interface LlmProvider {
    Stream<Chunk> chat(PromptRequest request);
    TokenUsage estimateTokens(PromptRequest request);
}
```

Candidate implementations: Anthropic Messages API (first), then OpenAI Chat
Completions, Gemini, and an OpenAI-compatible adapter for OpenRouter / LM
Studio / Ollama / self-hosted.

**Library choice.** LangChain4j covers most providers with a unified Java
interface including streaming and caching. The alternative is hand-rolling HTTP
against each REST API (more control, more maintenance) or using Anthropic's
official Java SDK (first-party but Claude-only). LangChain4j is the pragmatic
default unless the team has prior experience with another option or a specific
provider's caching/streaming is poorly supported.

Each implementation handles: auth (API key + optional base URL), prompt
caching (Anthropic uses explicit `cache_control` markers, OpenAI caches common
input prefixes automatically, Gemini uses an explicit cache API), streaming,
token accounting (for quotas), and error translation (rate limit, auth failure,
provider outage).

### Shared authoring-rules file

Same as #434: extract the authoring best-practice rules from [nanopub-skill][]'s
`SKILL.md` into a standalone `nanopub-authoring-rules.md`, vendor it in
Nanodash (copy committed to this repo, kept in sync with nanopub-skill by
periodic manual pull), and embed it as a stable cached prefix in every prompt.

Benefits:

- Single source of truth for template/query/view conventions across tools
- Provider-agnostic (any AI can consume it)
- Stable prefix caches well on API-based providers, reducing cost on iterative
  follow-ups

If #434 ships first and already vendors this file, this design reuses it
unchanged.

[nanopub-skill]: https://github.com/knowledgepixels/nanopub-skill

### API key management

**Per-user keys** stored in Nanodash's database, encrypted at rest. New section
in profile settings ("AI providers") where users add/remove keys per provider.
Fields: provider, API key, optional base URL override, optional default model.
Keys are write-only after save (never displayed), with rotate/delete actions
and a "test connection" button.

**Operator default key** configured at the instance level (env var or config
file), with per-user daily token and request quotas. When present, users
without their own key can use the feature until their quota is exhausted.

**Resolution cascade:**

1. User's key (if configured) -- use it.
2. Operator key (if user is under quota) -- use it.
3. Neither -- feature disabled, with a "configure a provider" link pointing at
   profile settings.

### TriG extraction and validation

The LLM returns free text that may contain a TriG code block. Extract it, parse
with Nanodash's existing RDF4J-backed parsing, validate against nanopub
structural rules (four named graphs, correct head links, template links
present, no obviously malformed IRIs), and reject or warn on broken output.
Valid output flows into the existing `/view?_nanopub_trig=...` preview path.

### Return path: "Edit in form"

Same bridge proposed in #434: the preview page reads `nt:wasCreatedFromTemplate`
from pubinfo and redirects to `/publish?template=<uri>&param_<name>=<value>&...`.
If #434 ships first, this design inherits the button for free. The nanopub
skill's authoring rules already insist on `nt:wasCreatedFromTemplate` in every
draft, so the bridge works automatically for any AI that follows the rules
file.

For drafts that don't match an existing template (e.g. novel template
authoring), the fallback is to iterate with the AI and re-open the updated
preview URL.

### Operator configuration

Instance admins configure (in a single config section):

- Default provider + API key (optional; absent = BYOK-only instance)
- Per-user daily token quota + request quota
- Allowed providers (whitelist -- e.g. an institution might only allow its
  self-hosted endpoint)
- Default and max model tiers (e.g. default Haiku, max Sonnet, Opus blocked
  unless BYOK)
- Privacy-disclosure text

### Privacy disclosure

First time a user invokes Draft with AI, show a one-time disclosure: *"Your
prompt will be sent to \<provider\>. It includes the current draft, the
authoring-rules file, and the context chunks you've selected. See
\<provider\>'s data-handling policy."* User acknowledges once per provider.
Stored against the user profile.

### Audit logging (operator-paid only)

For instances using an operator key, log aggregate usage per user -- request
count, token count, estimated cost -- **not** prompt/response content. Surfaces
in a simple admin view later. An opt-in per-user "debug log" that captures
prompts/responses can be added for troubleshooting, gated on explicit user
consent.

## Cost expectations

Indicative numbers per request (~10k input / ~1k output, with prompt caching on
follow-ups):

| Model      | Fresh request | Cached follow-up |
|------------|---------------|------------------|
| Haiku 4.5  | ~$0.015       | ~$0.006          |
| Sonnet 4.6 | ~$0.045       | ~$0.020          |
| Opus 4.6   | ~$0.23        | ~$0.09           |

A typical iterative session (1 fresh + 3 follow-ups) runs ~$0.03 on Haiku,
~$0.11 on Sonnet, ~$0.50 on Opus.

Scaling for operator-paid instances:

| Daily active sessions | Haiku/month | Sonnet/month | Opus/month |
|-----------------------|-------------|--------------|------------|
| 10                    | ~$9         | ~$33         | ~$150      |
| 100                   | ~$90        | ~$330        | ~$1,500    |
| 1,000                 | ~$900       | ~$3,300      | ~$15,000   |

Cost-control levers: prompt caching (5-10x cheaper on follow-ups), model
tiering (default Haiku, upgrade Sonnet on opt-in), per-user daily caps, and
`max_tokens` limits (~2k default).

## Tradeoffs vs #434

|                     | #434 (Tier 1)                        | This design (Tier 3)             |
|---------------------|--------------------------------------|----------------------------------|
| Reach               | Users with a local AI tool           | All Nanodash users               |
| Backend work        | None                                 | Substantial                      |
| Cost to operator    | Zero                                 | $0--$thousands/month             |
| Key management      | None                                 | Encrypted DB storage             |
| Iteration UX        | Context-switch to external tool      | In-Nanodash chat                 |
| Time to ship        | Small PR                             | Multi-week PR                    |
| Preview/edit path   | Shared                               | Shared                           |
| Rules file          | Shared                               | Shared                           |

## Relationship to #434

The two designs are largely compatible, not alternatives:

- **Shared components** (rules file, context adapters, quick-action taxonomy,
  "Edit in form" bridge) should be built once and reused.
- **The frontend modal/panel** can be one component with pluggable dispatch --
  #434 dispatches via clipboard/URL, this design dispatches via backend
  streaming. Same UX surface.
- **If #434 ships first**, this design inherits its scaffolding and becomes
  substantially smaller.
- **If this ships first (skipping #434)**, local-agent users (Claude Code,
  Cursor) lose a useful hand-off path until #434 is added. Those users will
  want it even after a native Draft with AI exists, because they prefer their
  own tool's context and signing pipeline.

## Scope

**First PR:**

- Backend `LlmProvider` abstraction with one implementation (Anthropic Messages
  API)
- Vendored `nanopub-authoring-rules.md` (shared with nanopub-skill)
- "Draft with AI" chat panel component, reused across entry-point pages
- Per-page context adapters (same set as #434)
- Streaming response rendering (SSE or WebSocket)
- TriG extraction + structural validation
- Per-user API key management in profile settings (encrypted storage)
- Operator default-key configuration + per-user daily quotas
- "Edit in form" bridge on `/view?_nanopub_trig=...` (shared with #434)
- One-time privacy disclosure per provider
- Aggregate audit logging for operator-paid usage

**Follow-ups:**

- Additional provider implementations (OpenAI, Gemini, OpenAI-compatible)
- MCP server for local Claude Code users (Tier 2, orthogonal)
- Deep-link hand-off (#434, still useful for local-agent users)
- Whole-space-aware drafting with 1M-context models
- Fine-grained audit logging
- Admin dashboard UI for usage stats
- Conversation history persisted across sessions (v1 is session-only)

## Open questions

- **LLM library.** LangChain4j (unified, covers most providers) vs Anthropic's
  official Java SDK (first-party, Claude-only) vs hand-rolled HTTP (most
  control, most maintenance)?
- **Streaming in Wicket.** Wicket is server-side component-based; streaming LLM
  output usually means SSE or a WebSocket. Wicket has first-class WebSocket
  support via `wicket-native-websocket-*`, and SSE can be served via a plain
  `AbstractResource` / servlet. Pick one; either should work without new
  infrastructure.
- **Key encryption scheme.** Single instance master key in config? Per-user key
  derived from ORCID login? Integrate with existing secret-handling?
- **Default model for operator-paid instances.** Haiku (cheapest) vs Sonnet
  (~3x cost, noticeably better on complex tasks). Probably configurable with
  Haiku as default.
- **Conversation persistence.** Session-only (simpler, lost on logout) vs
  DB-backed (enables "my drafts" history, more storage and privacy surface).
  V1 probably session-only.
- **What gets sent.** Minimum is draft + rules + selected context chunks. Should
  user ORCID and space membership be included by default, or opt-in?
- **Default free-tier quota.** Suggestion: 50k input + 10k output tokens/day
  per user (~20-30 typical sessions before BYOK needed).
- **Ship sequence.** Ship #434 first and layer this on top, or skip straight
  here? This design assumes the latter but composes cleanly either way.
- **Rules file canonical location.** (a) Vendored in Nanodash, synced from
  nanopub-skill; (b) a third shared repo; (c) published as a nanopub and
  fetched at runtime. (c) is most "nanopub-native" but adds a runtime
  dependency.
