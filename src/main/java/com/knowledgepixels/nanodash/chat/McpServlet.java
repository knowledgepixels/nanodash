package com.knowledgepixels.nanodash.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowledgepixels.nanodash.ApiTokenService;
import com.knowledgepixels.nanodash.NanodashPreferences;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Minimal MCP (Model Context Protocol) server endpoint: JSON-RPC 2.0 over a
 * single POST endpoint (streamable HTTP transport, stateless, JSON response
 * mode). Serves the tool catalog from {@link McpTools} to two kinds of
 * callers: the Claude Code subprocesses spawned by {@link ClaudeChatService}
 * (authenticating with the service's per-startup bearer token; see
 * docs/claude-code-chat.md) and, when remote MCP access is enabled, users'
 * own AI agents (authenticating with personal API tokens from
 * {@link ApiTokenService}; see docs/remote-mcp.md).
 */
public class McpServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(McpServlet.class);

    private static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of("2025-06-18", "2025-03-26", "2024-11-05");
    private static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean localEnabled = ClaudeChatService.get().isEnabled();
        boolean remoteEnabled = NanodashPreferences.get().isMcpRemoteEnabled();
        if (!localEnabled && !remoteEnabled) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        McpCaller caller = authenticate(req, localEnabled, remoteEnabled);
        if (caller == null) {
            resp.setHeader("WWW-Authenticate", "Bearer realm=\"nanodash-mcp\"");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        JsonObject message;
        try {
            message = JsonParser.parseReader(req.getReader()).getAsJsonObject();
        } catch (Exception ex) {
            sendJson(resp, error(null, -32700, "Parse error"));
            return;
        }
        if (!message.has("id")) {
            // A notification (e.g. notifications/initialized): acknowledge without a body.
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
        }
        JsonElement id = message.get("id");
        String method = message.has("method") ? message.get("method").getAsString() : "";
        JsonObject params = message.has("params") ? message.getAsJsonObject("params") : new JsonObject();
        logger.debug("MCP request: {}", method);
        JsonObject response = switch (method) {
            case "initialize" -> result(id, initialize(params));
            case "ping" -> result(id, new JsonObject());
            case "tools/list" -> result(id, toolsList());
            case "tools/call" -> result(id, toolsCall(params, caller));
            default -> error(id, -32601, "Method not found: " + method);
        };
        sendJson(resp, response);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // No SSE stream is offered; per the streamable HTTP transport spec,
        // clients take 405 as "POST-only server".
        resp.setHeader("Allow", "POST");
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Stateless server: there is no session to terminate.
        resp.setHeader("Allow", "POST");
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Resolves the request's bearer token to a caller: the local chat
     * service's per-startup token (carrying the chat-session header), or a
     * user's personal API token.
     *
     * @return the caller, or null if the token is missing or unknown
     */
    private McpCaller authenticate(HttpServletRequest req, boolean localEnabled, boolean remoteEnabled) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String token = auth.substring(7).trim();
        if (localEnabled && token.equals(ClaudeChatService.get().getMcpToken())) {
            return new McpCaller(req.getHeader(ClaudeChatService.SESSION_HEADER), null);
        }
        if (remoteEnabled) {
            String userIri = ApiTokenService.get().authenticate(token);
            if (userIri != null) {
                RemoteAgentService.get().markActive(userIri);
                return new McpCaller(null, userIri);
            }
        }
        return null;
    }

    private JsonObject initialize(JsonObject params) {
        String requestedVersion = params.has("protocolVersion") ? params.get("protocolVersion").getAsString() : null;
        String version = SUPPORTED_PROTOCOL_VERSIONS.contains(requestedVersion) ? requestedVersion : DEFAULT_PROTOCOL_VERSION;
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "nanodash");
        serverInfo.addProperty("version", ResourceBundle.getBundle("nanodash").getString("nanodash.version"));
        JsonObject r = new JsonObject();
        r.addProperty("protocolVersion", version);
        r.add("capabilities", capabilities);
        r.add("serverInfo", serverInfo);
        String instructions = ClaudeChatService.getCoreBackground();
        if (instructions != null) {
            r.addProperty("instructions", instructions);
        }
        return r;
    }

    private JsonObject toolsList() {
        JsonArray tools = new JsonArray();
        for (McpTools.Tool tool : McpTools.getAll()) {
            JsonObject t = new JsonObject();
            t.addProperty("name", tool.name);
            t.addProperty("description", tool.description);
            t.add("inputSchema", tool.inputSchema);
            tools.add(t);
        }
        JsonObject r = new JsonObject();
        r.add("tools", tools);
        return r;
    }

    private JsonObject toolsCall(JsonObject params, McpCaller caller) {
        String name = params.has("name") ? params.get("name").getAsString() : "";
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();
        McpTools.Tool tool = McpTools.get(name);
        String text;
        boolean isError = false;
        if (tool == null) {
            text = "Unknown tool: " + name;
            isError = true;
        } else {
            try {
                text = tool.executor.apply(arguments, caller);
            } catch (Exception ex) {
                logger.error("MCP tool {} failed", name, ex);
                text = "Error: " + ex.getMessage();
                isError = true;
            }
        }
        JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", text);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject r = new JsonObject();
        r.add("content", contents);
        r.addProperty("isError", isError);
        return r;
    }

    private static JsonObject result(JsonElement id, JsonObject result) {
        JsonObject r = new JsonObject();
        r.addProperty("jsonrpc", "2.0");
        r.add("id", id);
        r.add("result", result);
        return r;
    }

    private static JsonObject error(JsonElement id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        JsonObject r = new JsonObject();
        r.addProperty("jsonrpc", "2.0");
        r.add("id", id);
        r.add("error", error);
        return r;
    }

    private static void sendJson(HttpServletResponse resp, JsonObject body) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(body.toString());
    }

}
