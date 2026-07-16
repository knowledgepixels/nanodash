package com.knowledgepixels.nanodash.chat;

/**
 * The authenticated caller of an MCP tool: either a local Claude Code
 * subprocess identified by its chat-session key (authenticated with the
 * per-startup token of {@link ClaudeChatService}), or a remote agent acting
 * for a user identified by their IRI (authenticated with a personal API
 * token; see docs/remote-mcp.md). Exactly one of the two fields is non-null.
 *
 * @param chatSessionKey the local chat session key, or null for remote callers
 * @param userIri        the user's IRI, or null for local chat callers
 */
public record McpCaller(String chatSessionKey, String userIri) {
}
