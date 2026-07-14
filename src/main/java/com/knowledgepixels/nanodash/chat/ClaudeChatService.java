package com.knowledgepixels.nanodash.chat;

import com.google.gson.JsonObject;
import com.knowledgepixels.nanodash.NanodashPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Owns the local Claude Code chat sessions: spawns and reaps the headless
 * subprocesses and holds the bearer token that guards the /mcp endpoint.
 * See docs/claude-code-chat.md.
 */
public class ClaudeChatService {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeChatService.class);

    private static final long IDLE_TIMEOUT_MILLIS = 10 * 60 * 1000;

    private static final List<String> ENV_VARS_TO_SCRUB = List.of(
            // Ensure the CLI uses the user's plan login, not API-credit billing:
            "ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "ANTHROPIC_BASE_URL");

    private static ClaudeChatService instance;

    /**
     * Get the singleton service instance.
     *
     * @return the service
     */
    public static synchronized ClaudeChatService get() {
        if (instance == null) {
            instance = new ClaudeChatService();
        }
        return instance;
    }

    private final String mcpToken;
    private final Map<String, ClaudeSession> sessions = new ConcurrentHashMap<>();

    private ClaudeChatService() {
        byte[] tokenBytes = new byte[32];
        new SecureRandom().nextBytes(tokenBytes);
        mcpToken = HexFormat.of().formatHex(tokenBytes);
        ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "claude-chat-reaper");
            t.setDaemon(true);
            return t;
        });
        reaper.scheduleAtFixedRate(this::reapIdleSessions, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Whether the Claude chat feature is switched on in the preferences.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return NanodashPreferences.get().isClaudeChatEnabled();
    }

    /**
     * Get the bearer token that MCP requests must carry.
     *
     * @return the token, random per application start
     */
    public String getMcpToken() {
        return mcpToken;
    }

    /**
     * Get the chat session for the given key (typically the Wicket session ID),
     * spawning a Claude Code subprocess if there is none yet or the previous
     * one has been reaped.
     *
     * @param key the session key
     * @return the running chat session
     * @throws IOException if the subprocess could not be started
     */
    public ClaudeSession getOrCreateSession(String key) throws IOException {
        synchronized (sessions) {
            ClaudeSession session = sessions.get(key);
            if (session == null) {
                session = new ClaudeSession(buildCommand(key), ENV_VARS_TO_SCRUB, getWorkingDir());
                sessions.put(key, session);
                logger.info("Started Claude Code chat session for {}", key);
            }
            return session;
        }
    }

    /**
     * Get the existing chat session for the given key, if any.
     *
     * @param key the session key
     * @return the session or null
     */
    public ClaudeSession getSession(String key) {
        return sessions.get(key);
    }

    /**
     * Close and remove the chat session for the given key, if any.
     *
     * @param key the session key
     */
    public void closeSession(String key) {
        ClaudeSession session = sessions.remove(key);
        if (session != null) session.close();
    }

    /**
     * The header carrying the chat-session key on MCP requests, so tools like
     * open_page know which session they act for.
     */
    public static final String SESSION_HEADER = "X-Nanodash-Chat-Session";

    private File getWorkingDir() {
        // A neutral working directory, so the CLI doesn't pick up the CLAUDE.md
        // or settings of whatever directory the server was started from:
        File dir = new File(System.getProperty("user.home") + "/.nanodash/claude-chat");
        dir.mkdirs();
        return dir;
    }

    private List<String> buildCommand(String sessionKey) {
        NanodashPreferences prefs = NanodashPreferences.get();
        List<String> command = new ArrayList<>();
        command.add(prefs.getClaudeChatBinary());
        command.add("--print");
        command.add("--input-format");
        command.add("stream-json");
        command.add("--output-format");
        command.add("stream-json");
        command.add("--verbose");
        command.add("--mcp-config");
        command.add(buildMcpConfig(prefs, sessionKey));
        command.add("--strict-mcp-config");
        command.add("--allowedTools");
        command.add("mcp__nanodash");
        command.add("mcp__nanodash__*");
        if (prefs.getClaudeChatModel() != null) {
            command.add("--model");
            command.add(prefs.getClaudeChatModel());
        }
        return command;
    }

    private String buildMcpConfig(NanodashPreferences prefs, String sessionKey) {
        String baseUrl = prefs.getWebsiteUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        JsonObject headers = new JsonObject();
        headers.addProperty("Authorization", "Bearer " + mcpToken);
        headers.addProperty(SESSION_HEADER, sessionKey);
        JsonObject nanodash = new JsonObject();
        nanodash.addProperty("type", "http");
        nanodash.addProperty("url", baseUrl + "mcp");
        nanodash.add("headers", headers);
        JsonObject servers = new JsonObject();
        servers.add("nanodash", nanodash);
        JsonObject config = new JsonObject();
        config.add("mcpServers", servers);
        return config.toString();
    }

    private void reapIdleSessions() {
        for (Map.Entry<String, ClaudeSession> entry : sessions.entrySet()) {
            if (entry.getValue().getIdleMillis() > IDLE_TIMEOUT_MILLIS) {
                logger.info("Reaping idle Claude Code chat session for {}", entry.getKey());
                closeSession(entry.getKey());
            }
        }
    }

}
