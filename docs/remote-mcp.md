# Remote MCP Access (Personal API Tokens)

**Status: 🚧 implemented on this branch, verification pending**

Nanodash's `/mcp` endpoint (built for the [local Claude Code chat](claude-code-chat.md))
can also be opened up on a hosted multi-user instance, so that users point *their own*
AI agents at it: a local Claude Code, an agent built on the Claude API, or any other
MCP client that can send a bearer token. The agent gets the same read-only tool catalog
as the local chat — searching and reading nanopublications, inspecting templates,
running queries, preparing (never performing) publications, and steering the user's
open Nanodash browser tab.

This inverts the deployment story of the local chat: there, Nanodash spawns the model
process; here, the user brings their own agent and Nanodash is just the MCP server.
The user's own subscription or API key pays for the model; the Nanodash operator only
serves tool calls.

## Enabling

Off by default. In `~/.nanopub/nanodash-preferences.yml`:

```
mcp-remote-enabled: true
```

or via the environment: `NANODASH_MCP_REMOTE_ENABLED=true`.

This is independent of `claude-chat-enabled` (the local chat feature): either can be
on without the other. `/mcp` answers if at least one of the two is enabled, and only
accepts the credentials of the enabled paths.

## Personal API tokens

A logged-in user creates tokens on their own user page (About tab, "🔑 Remote AI
access" section), each with a label. The token (`ndmcp_` + 64 hex characters) is shown
**once** at creation and cannot be retrieved again; the server stores only its SHA-256
hash, with owner, label, and creation/last-used timestamps, in
`~/.nanopub/nanodash-api-tokens.yml`. Tokens don't expire but can be revoked in the
same UI at any time.

An authenticated MCP call resolves the token to the owning user's IRI. That identity
is currently used only for `open_page` routing (below); all other tools are the same
public read-only queries the web UI performs.

## Client setup

**Local Claude Code:**

```
claude mcp add --transport http nanodash https://<instance>/mcp \
  --header "Authorization: Bearer ndmcp_..."
```

**Claude API** (`mcp_servers` in the Messages API): use the token as
`authorization_token` of the server entry.

**Anything else** that speaks MCP's streamable HTTP transport in JSON mode and can
send an `Authorization: Bearer` header.

Protocol notes: the endpoint is POST-only JSON-RPC 2.0 (no SSE stream; GET answers
405, which streamable-HTTP clients take as "no stream offered"). Protocol versions
2024-11-05 through 2025-06-18 are accepted. The `initialize` response carries the
domain background (nanopub anatomy, template usage, the never-publish rule) as MCP
`instructions`, so any client's model gets the same grounding as the local chat.

## open_page on a hosted instance

The `open_page` tool queues navigations **per user** (not per browser session): any
Nanodash page the token's owner has open picks them up and navigates. To keep hosted
instances free of background polling, a page only polls (every 3 s) if, when it
rendered, the user's agent had made an authenticated call within the previous 30
minutes; the poll stops itself when that window lapses.

Consequences:

- A tab opened *before* the agent's first call doesn't react until the user navigates
  or reloads once (the tool's return text tells the agent to say so).
- With several tabs open, whichever tab polls first executes the navigation.

## Security scope

- Tools are read-only plus browser navigation (validated in-app paths only) and
  publish-form prefilling. **Nothing is ever published server-side**; publishing
  stays behind the user's own review, signature, and click in the web UI.
- A leaked token therefore lets an attacker read public nanopub data (already
  public), watch nothing, and queue navigations for the owner's open tabs — revoke
  it in the UI.
- Tokens are stored hashed; the plaintext exists only in the creation response.
- The endpoint should only be exposed over HTTPS on hosted instances.

## Follow-ups (not implemented)

- **OAuth 2.1 authorization** (PKCE, RFC 9728 protected-resource metadata, dynamic
  client registration or CIMD) — what claude.ai custom connectors need; would
  piggyback on the ORCID login for the consent step.
- Rate limiting / abuse controls.
- SSE / full streamable-HTTP transport and `Mcp-Session-Id`.
- Per-token tool scoping and an operator/admin view of issued tokens.
- Serving only the local addendum via `--append-system-prompt` to the local chat
  subprocess (it currently gets the core background twice: once there, once as MCP
  instructions).
