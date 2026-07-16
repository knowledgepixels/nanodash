package com.knowledgepixels.nanodash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-user API tokens for remote MCP access (see docs/remote-mcp.md).
 * Tokens are random secrets shown to the user once at creation; only their
 * SHA-256 hashes are kept, in memory and in a YAML file that survives
 * restarts. Authentication is a hash lookup that resolves the token to the
 * owning user's IRI.
 */
public class ApiTokenService {

    private static final Logger logger = LoggerFactory.getLogger(ApiTokenService.class);

    private static final String TOKEN_PREFIX = "ndmcp_";
    private static final int MAX_LABEL_LENGTH = 80;
    private static final long FLUSH_INTERVAL_MILLIS = 5 * 60 * 1000;
    private static final String DEFAULT_STORE_PATH = "/.nanopub/nanodash-api-tokens.yml";

    private static ApiTokenService instance;

    /**
     * Get the singleton instance, backed by the default store file.
     *
     * @return the ApiTokenService instance
     */
    public static synchronized ApiTokenService get() {
        if (instance == null) {
            instance = new ApiTokenService(new File(System.getProperty("user.home") + DEFAULT_STORE_PATH));
        }
        return instance;
    }

    /**
     * A stored API token: the hash of the secret plus metadata. The secret
     * itself is never stored.
     */
    public static class ApiToken implements Serializable {

        private String tokenHash;
        private String userIri;
        private String label;
        private long created;
        private long lastUsed;

        public String getTokenHash() {
            return tokenHash;
        }

        public void setTokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
        }

        public String getUserIri() {
            return userIri;
        }

        public void setUserIri(String userIri) {
            this.userIri = userIri;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getCreated() {
            return created;
        }

        public void setCreated(long created) {
            this.created = created;
        }

        public long getLastUsed() {
            return lastUsed;
        }

        public void setLastUsed(long lastUsed) {
            this.lastUsed = lastUsed;
        }

    }

    /**
     * The serialized form of the store file.
     */
    public static class TokenStore implements Serializable {

        private List<ApiToken> tokens = new ArrayList<>();

        public List<ApiToken> getTokens() {
            return tokens;
        }

        public void setTokens(List<ApiToken> tokens) {
            this.tokens = tokens;
        }

    }

    private final File storeFile;
    private final Map<String, ApiToken> byHash = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    ApiTokenService(File storeFile) {
        this.storeFile = storeFile;
        if (storeFile.exists()) {
            try {
                ObjectMapper mapper = new YAMLMapper();
                TokenStore store = mapper.readValue(storeFile, TokenStore.class);
                for (ApiToken token : store.getTokens()) {
                    byHash.put(token.getTokenHash(), token);
                }
            } catch (Exception ex) {
                // Starting empty is safe: nothing is written until the next create/revoke.
                logger.error("Could not read API token store at '{}'; starting with no tokens", storeFile, ex);
            }
        }
        ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "api-token-flusher");
            t.setDaemon(true);
            return t;
        });
        flusher.scheduleAtFixedRate(this::flushIfDirty, FLUSH_INTERVAL_MILLIS, FLUSH_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new token for the given user and returns the token secret,
     * which is not stored and cannot be retrieved again.
     *
     * @param userIri the owning user's IRI
     * @param label   a user-chosen label for the token
     * @return the plaintext token
     */
    public String createToken(String userIri, String label) {
        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        String token = TOKEN_PREFIX + HexFormat.of().formatHex(secretBytes);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenHash(Utils.createSha256HexHash(token));
        apiToken.setUserIri(userIri);
        label = label == null || label.isBlank() ? "token" : label.trim();
        if (label.length() > MAX_LABEL_LENGTH) label = label.substring(0, MAX_LABEL_LENGTH);
        apiToken.setLabel(label);
        apiToken.setCreated(System.currentTimeMillis());
        byHash.put(apiToken.getTokenHash(), apiToken);
        persist();
        return token;
    }

    /**
     * Revokes the token with the given hash, if it belongs to the given user.
     *
     * @param userIri   the requesting user's IRI
     * @param tokenHash the hash of the token to revoke
     * @return true if the token existed and was revoked
     */
    public boolean revokeToken(String userIri, String tokenHash) {
        ApiToken token = byHash.get(tokenHash);
        if (token == null || !token.getUserIri().equals(userIri)) return false;
        byHash.remove(tokenHash);
        persist();
        return true;
    }

    /**
     * Lists the given user's tokens, oldest first.
     *
     * @param userIri the user's IRI
     * @return the user's tokens
     */
    public List<ApiToken> getTokens(String userIri) {
        return byHash.values().stream()
                .filter((t) -> t.getUserIri().equals(userIri))
                .sorted(Comparator.comparingLong(ApiToken::getCreated))
                .toList();
    }

    /**
     * Resolves a presented token to the owning user.
     *
     * @param rawToken the token as presented by the client
     * @return the owning user's IRI, or null if the token is unknown
     */
    public String authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        ApiToken token = byHash.get(Utils.createSha256HexHash(rawToken));
        if (token == null) return null;
        // lastUsed is informational; it is flushed to disk lazily.
        token.setLastUsed(System.currentTimeMillis());
        dirty = true;
        return token.getUserIri();
    }

    private synchronized void persist() {
        try {
            TokenStore store = new TokenStore();
            store.setTokens(byHash.values().stream()
                    .sorted(Comparator.comparingLong(ApiToken::getCreated))
                    .toList());
            ObjectMapper mapper = new YAMLMapper();
            mapper.writeValue(storeFile, store);
            dirty = false;
        } catch (Exception ex) {
            logger.error("Could not write API token store at '{}'", storeFile, ex);
        }
    }

    private void flushIfDirty() {
        if (dirty) persist();
    }

}
