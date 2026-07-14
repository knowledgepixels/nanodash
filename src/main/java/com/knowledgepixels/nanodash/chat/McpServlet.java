package com.knowledgepixels.nanodash.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Minimal MCP (Model Context Protocol) server endpoint: JSON-RPC 2.0 over a
 * single POST endpoint (streamable HTTP transport, stateless, JSON response
 * mode). Serves the tool catalog from {@link McpTools} to the Claude Code
 * subprocesses spawned by {@link ClaudeChatService}, which authenticate with
 * the service's per-startup bearer token. See docs/claude-code-chat.md.
 */
public class McpServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(McpServlet.class);

    private static final String PROTOCOL_VERSION = "2025-03-26";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!ClaudeChatService.get().isEnabled()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.equals("Bearer " + ClaudeChatService.get().getMcpToken())) {
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
            case "initialize" -> result(id, initialize());
            case "ping" -> result(id, new JsonObject());
            case "tools/list" -> result(id, toolsList());
            case "tools/call" -> result(id, toolsCall(params, req.getHeader(ClaudeChatService.SESSION_HEADER)));
            default -> error(id, -32601, "Method not found: " + method);
        };
        sendJson(resp, response);
    }

    private JsonObject initialize() {
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "nanodash");
        serverInfo.addProperty("version", "0.1");
        JsonObject r = new JsonObject();
        r.addProperty("protocolVersion", PROTOCOL_VERSION);
        r.add("capabilities", capabilities);
        r.add("serverInfo", serverInfo);
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

    private JsonObject toolsCall(JsonObject params, String sessionKey) {
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
                text = tool.executor.apply(arguments, sessionKey);
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
