# Local Claude Code Chat (MCP + Embedded Chat Panel)

**Status:** 🚧 In progress

Let a locally running Nanodash instance connect to the user's local Claude
Code installation: a chat panel in the browser sends messages to Claude Code
running headless on the same machine, and Claude Code can call back into
Nanodash through an MCP interface to search nanopublications, inspect
templates, prepare drafts, and even navigate the user's browser session.

This is the "Tier 2" option referenced in [draft-with-ai](draft-with-ai.md)
and [#434](https://github.com/knowledgepixels/nanodash/issues/434), expanded
from "just an MCP server" to a full in-Nanodash chat experience. It uses the
user's existing Claude plan (subscription) via their logged-in Claude Code
CLI -- no API keys, no per-token credits, no operator cost.

## Motivation

[#434](https://github.com/knowledgepixels/nanodash/issues/434) hands context
*out* to an external AI tool; [draft-with-ai](draft-with-ai.md) brings a
provider API *in* with all the key-management and cost machinery that
implies. Between the two sits the user who already runs Claude Code locally
and runs Nanodash locally (or on their own machine for development): for
them, the AI is already installed, already authenticated, already paid for.
What's missing is only the wiring:

- a way for Claude to **act on Nanodash** (query, fetch, draft) instead of
  the user copy-pasting context back and forth, and
- a way to **talk to Claude from inside Nanodash** so the conversation
  happens next to the thing being worked on.

Unlike the Tier 3 design, there is no provider abstraction, no key storage,
no quota accounting: the entire operational surface is "spawn the `claude`
binary the user already has".

## Architecture overview

Two independent channels, one per direction:

```
   Browser                        Nanodash (Jetty/Tomcat)              Claude Code
┌────────────┐  AJAX poll /   ┌──────────────────────────┐
│ chat panel │◀──WebSocket──▶│ ClaudeSessionManager      │  stdin/stdout
│            │                │  └─ claude subprocess ────┼──NDJSON──▶ claude -p
│  UI cmds ◀─┼── queue ───────│                           │             │
└────────────┘                │ /mcp servlet (MCP server) │◀───HTTP─────┘
                              │  └─ tool implementations  │  mcp__nanodash__* calls
                              └──────────────────────────┘
```

1. **Chat channel (browser → Claude):** the chat panel posts the user's
   message to Nanodash, which forwards it to a long-lived `claude` headless
   subprocess over stdin. Streamed response events come back over stdout and
   are pushed to the panel.
2. **Action channel (Claude → Nanodash):** Nanodash exposes an MCP server
   over streamable HTTP at `/mcp`. The spawned Claude process is configured
   to connect to it, giving Claude tools like `search_nanopubs` and
   `get_template`.

A third, smaller piece rides on the chat channel: MCP tools that affect the
*browser* (e.g. `open_page`) enqueue a UI command that the chat panel picks
up and executes (`window.location`, trigger an AJAX behavior). MCP calls
land on the server, so this queue is the only way Claude can reach the
user's open tab.

## Components

### 1. Claude session manager

A backend service that owns one headless Claude Code process per active chat
session:

```
claude --print \
  --input-format stream-json --output-format stream-json \
  --mcp-config '{"mcpServers":{"nanodash":{"type":"http","url":"<base-url>/mcp", "headers":{"Authorization":"Bearer <session token>"}}}}' \
  --strict-mcp-config \
  --allowedTools "mcp__nanodash__*" \
  --permission-mode dontAsk
```

- **Persistent, bidirectional:** `--input-format stream-json` keeps the
  process alive across turns; each chat message is written as a JSON user
  message to stdin, response/tool-use events stream back as
  newline-delimited JSON on stdout. No per-turn process startup cost.
- **Session lifecycle:** processes are reaped after an idle timeout; the
  Claude session ID (from the init event) is remembered so a reaped
  conversation can be revived with `--resume <id>`.
- **Auth = the user's plan:** the subprocess inherits the CLI's existing
  login, so usage counts against the user's Claude subscription. The
  manager must **scrub `ANTHROPIC_API_KEY`** (and related provider
  variables) from the child environment so the CLI can't silently switch
  to pay-per-token API billing.
- **`--strict-mcp-config`** prevents the subprocess from also loading the
  user's personal MCP servers (filesystem access, other services) into a
  Nanodash-branded chat.
- **Feature detection:** the whole feature is off unless (a) it is enabled
  in the Nanodash config and (b) the `claude` binary is found (configurable
  path). Hosted multi-user instances keep it off (see Security).

### 2. MCP endpoint (`/mcp`)

An MCP server servlet mounted next to the `WicketFilter` (which passes
non-Wicket requests down the chain; add an `ignorePaths` init-param if
needed).

**Implementation options:**

- **Official MCP Java SDK** (`io.modelcontextprotocol.sdk:mcp`, maintained
  with the Spring AI team) ships a servlet-based streamable-HTTP transport
  (`HttpServletStreamableServerTransportProvider`). Tool registration is
  declarative; protocol handling comes for free. Caveat: the transport
  targets Jakarta Servlet 6.0 async. The Docker deployment (Tomcat 11,
  Servlet 6.1) is fine; the dev setup (`jetty-maven-plugin`, Jetty 11 =
  Servlet 5.0) likely needs a bump to Jetty 12 (`ee10`).
- **Hand-rolled minimal server:** streamable HTTP MCP is JSON-RPC 2.0 over
  a single POST endpoint; a stateless server that answers `initialize`,
  `tools/list`, and `tools/call` is a few hundred lines with the JSON
  libraries already in the tree. Viable if the SDK dependency or the Jetty
  bump turns out to be annoying, and sufficient for a single known client.

The SDK is the default choice; the hand-rolled fallback is the escape
hatch.

**Standalone value:** the endpoint is useful without the chat panel at all.
Any local user can register it against their own terminal Claude Code:

```
claude mcp add --transport http nanodash http://localhost:37373/mcp
```

and get "Claude Code with Nanodash tools" -- which is precisely Tier 2 of
#434. The chat panel is an optional layer on top.

### 3. Tool catalog

First wave, read-only (safe under `dontAsk`):

| Tool | Does |
| --- | --- |
| `search_nanopubs` | free-text / typed search via the existing query APIs |
| `get_nanopub` | fetch a nanopub as TriG by URI |
| `list_templates` / `get_template` | discover and inspect assertion templates |
| `get_space` | space info, members, views for a space IRI |
| `run_query` | run a published grlc query with parameters |

Second wave, action tools:

| Tool | Does |
| --- | --- |
| `prepare_publication` | build + validate a draft from template/params; returns a `/view?_nanopub_trig=...` preview URL. **Does not publish.** |
| `open_page` | enqueue a browser navigation for the chat panel to execute |

Publishing itself is deliberately **not** a tool in v1: `prepare_publication`
ends with a preview the user opens and publishes through Nanodash's normal
signed flow. This keeps the human signature step human.

### 4. Chat panel (Wicket component)

- A docked panel (or dedicated page, for v1) with a message list, input box,
  and a visible "Claude is using tool X..." activity line driven by the
  tool-use events in the output stream.
- **Streaming to the browser:** v1 uses `AjaxSelfUpdatingTimerBehavior`
  polling the session's message queue -- dead simple, no new infrastructure.
  Upgrade path: Wicket native WebSocket (`wicket-native-websocket-*`) or a
  plain SSE servlet next to `/mcp`, as also discussed in
  [draft-with-ai](draft-with-ai.md).
- **Shared UI surface:** the entry points, quick actions, and "context being
  sent" panel specified in #434 / draft-with-ai apply here unchanged; this
  design is a third dispatch mechanism (local Claude Code) behind the same
  surface. Auto-attached context per turn can include the current page URL
  and, on editor pages, the current draft TriG.
- The same queue that streams chat messages also delivers UI commands from
  `open_page` (see Architecture).

## Security

This feature hands a web page a channel into a process that runs as the
local user. The containment story:

- **Tool allowlist, not denylist:** the subprocess runs with
  `--allowedTools "mcp__nanodash__*"` and `--permission-mode dontAsk` --
  everything not allowlisted is denied. No `Bash`, no file tools, no
  `WebFetch` unless deliberately enabled in config. Without this, a chat
  message could ask Claude to run arbitrary shell commands.
- **Local-only by design:** the feature is opt-in config, intended for
  `localhost` instances. On hosted multi-user instances it must stay off:
  every visitor would share one machine's Claude subscription (against its
  terms) and one machine's process privileges. The Tier 3 design is the
  hosted answer.
- **MCP endpoint auth:** `/mcp` accepts only requests bearing a
  per-process random token that Nanodash generates at startup and injects
  into the spawned process's `--mcp-config` headers. Other local processes
  (or other browser tabs) can't drive the tools directly.
- **UI commands are a fixed vocabulary** (navigate to an in-app path), not
  arbitrary JavaScript.

## Configuration

```
claude-chat-enabled: false          # master switch
claude-chat-binary: claude          # path to the Claude Code CLI
claude-chat-extra-tools:            # optional additions to the allowlist,
                                    #   e.g. WebFetch
claude-chat-model:                  # optional --model override
claude-chat-idle-timeout: 10m       # subprocess reaping
```

## Relationship to #434 and draft-with-ai

|                    | #434 (Tier 1)          | **This design (Tier 2)**       | draft-with-ai (Tier 3)   |
| ------------------ | ---------------------- | ------------------------------ | ------------------------ |
| AI runs            | user's external tool   | user's local Claude Code       | provider API from server |
| Reach              | users with an AI tool  | local users with Claude Code   | all users                |
| Cost / keys        | none                   | user's existing plan; no keys  | BYOK / operator-paid     |
| AI can act on Nanodash | no (copy-paste)    | yes (MCP tools)                | partially (draft only)   |
| Backend work       | none                   | moderate                       | substantial              |
| Hosted instances   | yes                    | **no** (local only)            | yes                      |

The three tiers compose: the shared UI surface (entry points, quick actions,
context panel) and the vendored authoring-rules file serve all of them, and
the MCP endpoint built here is exactly the "MCP server for local Claude Code
users" follow-up already listed in draft-with-ai. A local Claude Code
session pointed at the MCP endpoint benefits from the nanopub-skill /
authoring-rules conventions with no extra work.

## Scope

**First PR (vertical slice):**

- `/mcp` servlet with the read-only tool wave (SDK or hand-rolled, see open
  question)
- `ClaudeSessionManager`: spawn, stream-json protocol, env scrubbing, idle
  reaping
- Minimal chat page (poll-based streaming, tool-activity line)
- Config keys + feature detection; endpoint token auth

**Follow-ups:**

- `prepare_publication` + preview hand-off; `open_page` UI-command channel
- Docked panel on all pages; shared entry-point surface from #434
- WebSocket/SSE streaming upgrade
- `--resume`-based conversation revival across restarts
- Advertise the MCP endpoint for terminal use (docs + `claude mcp add`
  snippet on a help page)

## Open questions

- **MCP SDK vs hand-rolled.** Try the official Java SDK first; if the
  Servlet 6.0 requirement forces a disruptive dev-Jetty upgrade, is the
  Jetty 12 bump wanted anyway (Jetty 11 is EOL), or is the ~300-line
  hand-rolled server the better trade?
- **Session identity.** One Claude process per Wicket session, per user, or
  per browser tab? Per-session is the simple default; per-user enables
  "continue where I left off" via `--resume`.
- **Context auto-attach.** Send the current page URL every turn? Current
  draft TriG on editor pages? Follow the "context being sent" checkbox
  panel from #434 or keep v1 implicit?
- **stream-json protocol stability.** The NDJSON event format is documented
  but versioned with Claude Code; the parser should tolerate unknown event
  types and be covered by a smoke test against the installed CLI.
- **Windows.** Process spawning + path discovery for the `claude` binary is
  OS-dependent; is Windows dev a target or is localhost-Linux/macOS enough?
- **Other agent CLIs.** The subprocess contract (spawn, NDJSON in/out, MCP
  config flag) is Claude Code-specific today. Worth abstracting if e.g.
  a Gemini/Codex CLI equivalent is requested later, but not before.
