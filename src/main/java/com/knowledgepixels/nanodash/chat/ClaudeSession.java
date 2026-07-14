package com.knowledgepixels.nanodash.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * One conversation with a local Claude Code instance, backed by a persistent
 * headless subprocess speaking newline-delimited JSON over stdin/stdout
 * (see docs/claude-code-chat.md). Not serializable: instances live in
 * {@link ClaudeChatService}, never in the Wicket session.
 */
public class ClaudeSession {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeSession.class);

    private final Process process;
    private final BufferedWriter stdin;
    private final List<ChatMessage> messages = new ArrayList<>();
    private volatile boolean busy = false;
    private volatile long lastActivity = System.currentTimeMillis();
    private volatile String claudeSessionId;
    private final Queue<String> pendingNavigations = new ConcurrentLinkedQueue<>();

    ClaudeSession(List<String> command, List<String> envVarsToScrub, File workingDir) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        for (String envVar : envVarsToScrub) {
            pb.environment().remove(envVar);
        }
        pb.directory(workingDir);
        process = pb.start();
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        Thread stdoutReader = new Thread(this::readStdout, "claude-chat-stdout");
        stdoutReader.setDaemon(true);
        stdoutReader.start();
        Thread stderrReader = new Thread(this::readStderr, "claude-chat-stderr");
        stderrReader.setDaemon(true);
        stderrReader.start();
    }

    /**
     * Send a user message to Claude. Ignored if the process has died.
     *
     * @param text the user's chat message
     */
    public synchronized void sendUserMessage(String text) {
        touch();
        addMessage(ChatMessage.Kind.USER, text);
        if (!process.isAlive()) {
            addMessage(ChatMessage.Kind.ERROR, "The Claude Code process is not running anymore. Start a new conversation.");
            return;
        }
        busy = true;
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.addProperty("content", text);
        JsonObject message = new JsonObject();
        message.addProperty("type", "user");
        message.add("message", content);
        try {
            stdin.write(message.toString());
            stdin.write("\n");
            stdin.flush();
        } catch (IOException ex) {
            logger.error("Could not write to Claude Code process", ex);
            busy = false;
            addMessage(ChatMessage.Kind.ERROR, "Could not send message to Claude Code: " + ex.getMessage());
        }
    }

    private void readStdout() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    handleEvent(JsonParser.parseString(line).getAsJsonObject());
                } catch (Exception ex) {
                    logger.warn("Could not process Claude Code event: {}", line, ex);
                }
            }
        } catch (IOException ex) {
            logger.warn("Claude Code stdout closed", ex);
        }
        busy = false;
        if (process.isAlive()) return;
        int exitValue = process.exitValue();
        if (exitValue != 0) {
            addMessage(ChatMessage.Kind.ERROR, "The Claude Code process ended with exit code " + exitValue + ".");
        }
    }

    private void readStderr() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.warn("Claude Code stderr: {}", line);
            }
        } catch (IOException ex) {
            // process ended; nothing to do
        }
    }

    private void handleEvent(JsonObject event) {
        touch();
        String type = event.has("type") ? event.get("type").getAsString() : "";
        switch (type) {
            case "system" -> {
                if ("init".equals(getString(event, "subtype")) && event.has("session_id")) {
                    claudeSessionId = event.get("session_id").getAsString();
                }
            }
            case "assistant" -> {
                JsonObject message = event.getAsJsonObject("message");
                if (message == null || !message.has("content")) return;
                JsonArray content = message.getAsJsonArray("content");
                for (JsonElement blockElement : content) {
                    JsonObject block = blockElement.getAsJsonObject();
                    String blockType = getString(block, "type");
                    if ("text".equals(blockType)) {
                        String text = getString(block, "text");
                        if (text != null && !text.isBlank()) addMessage(ChatMessage.Kind.ASSISTANT, text);
                    } else if ("tool_use".equals(blockType)) {
                        addMessage(ChatMessage.Kind.TOOL, formatToolUse(block));
                    }
                }
            }
            case "result" -> {
                busy = false;
                if (event.has("is_error") && event.get("is_error").getAsBoolean()) {
                    String result = getString(event, "result");
                    addMessage(ChatMessage.Kind.ERROR, result == null ? "Claude Code reported an error." : result);
                }
            }
            default -> {
                // stream events for other roles (echoed tool results etc.) are not shown
            }
        }
    }

    private static String formatToolUse(JsonObject block) {
        String name = getString(block, "name");
        if (name != null && name.startsWith("mcp__nanodash__")) {
            name = name.substring("mcp__nanodash__".length());
        }
        String input = "";
        if (block.has("input")) {
            input = block.get("input").toString();
            if (input.length() > 200) input = input.substring(0, 200) + "…";
        }
        return name + " " + input;
    }

    private static String getString(JsonObject o, String name) {
        return o.has(name) && o.get(name).isJsonPrimitive() ? o.get(name).getAsString() : null;
    }

    private void addMessage(ChatMessage.Kind kind, String text) {
        synchronized (messages) {
            messages.add(new ChatMessage(kind, text));
        }
    }

    /**
     * Get a snapshot of the conversation so far.
     *
     * @return a copy of the message list
     */
    public List<ChatMessage> getMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    /**
     * Whether Claude is currently working on a response.
     *
     * @return true while a response is pending
     */
    public boolean isBusy() {
        return busy && process.isAlive();
    }

    /**
     * Get the Claude-side session ID, once known from the init event.
     *
     * @return the session ID or null
     */
    public String getClaudeSessionId() {
        return claudeSessionId;
    }

    /**
     * Queue an in-app navigation for the chat panel to execute in the user's
     * browser on its next poll (the open_page MCP tool lands here).
     *
     * @param path the in-app path, starting with "/"
     */
    public void requestNavigation(String path) {
        pendingNavigations.add(path);
    }

    /**
     * Take the next queued navigation, if any.
     *
     * @return the in-app path or null
     */
    public String pollNavigation() {
        return pendingNavigations.poll();
    }

    /**
     * Milliseconds since the last message in either direction.
     *
     * @return the idle time in milliseconds
     */
    public long getIdleMillis() {
        return System.currentTimeMillis() - lastActivity;
    }

    private void touch() {
        lastActivity = System.currentTimeMillis();
    }

    /**
     * Terminate the subprocess.
     */
    public void close() {
        try {
            stdin.close();
        } catch (IOException ex) {
            // stdin already closed; proceed to destroy
        }
        process.destroy();
    }

}
