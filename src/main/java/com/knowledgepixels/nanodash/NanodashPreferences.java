package com.knowledgepixels.nanodash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to manage Nanodash preferences.
 */
public class NanodashPreferences implements Serializable {

    private static NanodashPreferences obj;
    private static final Logger logger = LoggerFactory.getLogger(NanodashPreferences.class);

    /**
     * Get the singleton instance of NanodashPreferences.
     *
     * @return the NanodashPreferences instance
     */
    public static NanodashPreferences get() {
        if (obj == null) {
            File prefFile = new File(System.getProperty("user.home") + DEFAULT_SETTING_PATH);
            if (!prefFile.exists()) {
                return new NanodashPreferences();
            }
            ObjectMapper mapper = new YAMLMapper();
            try {
                obj = mapper.readValue(prefFile, NanodashPreferences.class);
            } catch (JacksonException ex) {
                obj = new NanodashPreferences();
                logger.error("Could not read preferences file at '{}' using defaults", DEFAULT_SETTING_PATH, ex);
            }
        }
        return obj;
    }

    private List<String> nanopubActions = new ArrayList<>();
    private boolean readOnlyMode = false;
    private String websiteUrl;
    private boolean orcidLoginMode = false;
    private String orcidClientId;
    private String orcidClientSecret;
    private String settingUri;
    private String umamiScriptUrl;
    private String umamiWebsiteId;
    private String homeResource = "https://w3id.org/spaces/knowledgepixels/nanodash/r/home";
    private boolean claudeChatEnabled = false;
    private String claudeChatBinary = "claude";
    private String claudeChatModel;
    private boolean mcpRemoteEnabled = false;
    private String apiCacheFile;
    public static final String DEFAULT_SETTING_PATH = "/.nanopub/nanodash-preferences.yml";

    /**
     * Default location of the API cache snapshot file, inside {@code ~/.nanopub} — the
     * directory the standard Docker setup mounts as a volume, so the snapshot survives
     * container restarts and upgrades without configuration.
     */
    public static final String DEFAULT_API_CACHE_PATH = "/.nanopub/nanodash-api-cache.ser";

    /**
     * Value for {@link #getApiCacheFile()} that disables API cache persistence.
     */
    public static final String API_CACHE_DISABLED = "none";

    /** Where an instance is assumed to be reachable when nothing says otherwise: a local run. */
    public static final String DEFAULT_WEBSITE_URL = "http://localhost:37373/";

    /**
     * Return the list of nanopub actions.
     *
     * @return list of nanopub actions
     */
    public List<String> getNanopubActions() {
        String s = System.getenv("NANODASH_NANOPUB_ACTIONS");
        if (s != null && !s.isBlank()) {
            return Arrays.asList(s.split(" "));
        }
        return nanopubActions;
    }

    /**
     * Set the list of nanopub actions.
     *
     * @param nanopubActions the list of nanopub actions
     */
    public void setNanopubActions(List<String> nanopubActions) {
        this.nanopubActions = nanopubActions;
    }

    /**
     * Check if the application is in read-only mode.
     *
     * @return true if in read-only mode, false otherwise
     */
    public boolean isReadOnlyMode() {
        if ("true".equals(System.getenv("NANODASH_READ_ONLY_MODE"))) {
            logger.debug("Found environment variable NANODASH_READ_ONLY_MODE with value: {}", true);
            return true;
        }
        logger.debug("Environment variable NANODASH_READ_ONLY_MODE not set, using default: {}", readOnlyMode);
        return readOnlyMode;
    }

    /**
     * Set the read-only mode.
     *
     * @param readOnlyMode true to enable read-only mode, false to disable
     */
    public void setReadOnlyMode(boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;
    }

    /**
     * Get the website URL.
     *
     * @return the website URL, falling back to {@link #DEFAULT_WEBSITE_URL} when this instance
     *         has not been told where it is reachable
     */
    public String getWebsiteUrl() {
        String s = getConfiguredWebsiteUrl();
        if (s != null) return s;
        logger.debug("No website URL configured, using default: {}", DEFAULT_WEBSITE_URL);
        return DEFAULT_WEBSITE_URL;
    }

    /**
     * The website URL as actually configured for this instance, from the
     * {@code NANODASH_WEBSITE_URL} environment variable or the preferences file — or null if
     * neither sets one.
     *
     * <p>Distinct from {@link #getWebsiteUrl()} because a caller that builds URLs for the
     * outside world must be able to tell a real deployment address from the localhost
     * fallback: guessing {@code localhost} would be worse than deriving the address from the
     * request. See {@link Utils#absolutePageUrl}.</p>
     *
     * @return the configured website URL, or null when unconfigured
     */
    public String getConfiguredWebsiteUrl() {
        String s = System.getenv("NANODASH_WEBSITE_URL");
        if (s != null && !s.isBlank()) {
            logger.debug("Found environment variable NANODASH_WEBSITE_URL with value: {}", s);
            return s;
        }
        return (websiteUrl == null || websiteUrl.isBlank()) ? null : websiteUrl;
    }

    /**
     * Set the website URL.
     *
     * @param websiteUrl the website URL to set
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /**
     * Check if the application is in ORCID login mode.
     *
     * @return true if in ORCID login mode, false otherwise
     */
    public boolean isOrcidLoginMode() {
        if ("true".equals(System.getenv("NANODASH_ORCID_LOGIN_MODE"))) {
            logger.debug("Found environment variable NANODASH_ORCID_LOGIN_MODE with value: {}", true);
            return true;
        }
        logger.debug("Environment variable NANODASH_ORCID_LOGIN_MODE not set, using default: {}", orcidLoginMode);
        return orcidLoginMode;
    }

    /**
     * Set the ORCID login mode.
     *
     * @param orcidLoginMode true to enable ORCID login mode, false to disable
     */
    public void setOrcidLoginMode(boolean orcidLoginMode) {
        this.orcidLoginMode = orcidLoginMode;
    }

    /**
     * Get the ORCID client ID.
     *
     * @return the ORCID client ID
     */
    public String getOrcidClientId() {
        String s = System.getenv("NANOPUB_ORCID_CLIENT_ID");
        if (s != null && !s.isBlank()) {
            logger.debug("Found environment variable NANOPUB_ORCID_CLIENT_ID with value: {}", s);
            return s;
        }
        logger.debug("Environment variable NANOPUB_ORCID_CLIENT_ID not set, using default: {}", orcidClientId);
        return orcidClientId;
    }

    /**
     * Set the ORCID client ID.
     *
     * @param orcidClientId the ORCID client ID to set
     */
    public void setOrcidClientId(String orcidClientId) {
        this.orcidClientId = orcidClientId;
    }

    /**
     * Get the ORCID client secret.
     * .
     *
     * @return the ORCID client secret
     */
    public String getOrcidClientSecret() {
        String s = System.getenv("NANOPUB_ORCID_CLIENT_SECRET");
        if (s != null && !s.isBlank()) {
            logger.debug("Found environment variable NANOPUB_ORCID_CLIENT_SECRET");
            return s;
        }
        logger.debug("Environment variable NANOPUB_ORCID_CLIENT_SECRET not set, using default");
        return orcidClientSecret;
    }

    /**
     * Set the ORCID client secret.
     *
     * @param orcidClientSecret the ORCID client secret to set
     */
    public void setOrcidClientSecret(String orcidClientSecret) {
        this.orcidClientSecret = orcidClientSecret;
    }

    /**
     * Get the setting URI.
     *
     * @return the setting URI
     */
    public String getSettingUri() {
        return settingUri;
    }

    /**
     * Set the setting URI.
     *
     * @param settingUri the setting URI to set
     */
    public void setSettingUri(String settingUri) {
        this.settingUri = settingUri;
    }

    /**
     * Get the Umami analytics script URL.
     *
     * @return the Umami script URL, or null if not configured
     */
    public String getUmamiScriptUrl() {
        String s = System.getenv("NANODASH_UMAMI_SCRIPT_URL");
        if (s != null && !s.isBlank()) return s;
        return umamiScriptUrl;
    }

    /**
     * Set the Umami analytics script URL.
     *
     * @param umamiScriptUrl the Umami script URL to set
     */
    public void setUmamiScriptUrl(String umamiScriptUrl) {
        this.umamiScriptUrl = umamiScriptUrl;
    }

    /**
     * Get the Umami analytics website ID.
     *
     * @return the Umami website ID, or null if not configured
     */
    public String getUmamiWebsiteId() {
        String s = System.getenv("NANODASH_UMAMI_WEBSITE_ID");
        if (s != null && !s.isBlank()) return s;
        return umamiWebsiteId;
    }

    /**
     * Set the Umami analytics website ID.
     *
     * @param umamiWebsiteId the Umami website ID to set
     */
    public void setUmamiWebsiteId(String umamiWebsiteId) {
        this.umamiWebsiteId = umamiWebsiteId;
    }

    /**
     * Check whether the local Claude Code chat feature is enabled.
     *
     * Intended for locally running single-user instances only; see
     * docs/claude-code-chat.md.
     *
     * @return true if the Claude chat feature is enabled
     */
    public boolean isClaudeChatEnabled() {
        if ("true".equals(System.getenv("NANODASH_CLAUDE_CHAT_ENABLED"))) {
            return true;
        }
        return claudeChatEnabled;
    }

    /**
     * Set whether the local Claude Code chat feature is enabled.
     *
     * @param claudeChatEnabled true to enable
     */
    public void setClaudeChatEnabled(boolean claudeChatEnabled) {
        this.claudeChatEnabled = claudeChatEnabled;
    }

    /**
     * Get the command to run the Claude Code CLI.
     *
     * @return the binary name or path (default "claude")
     */
    public String getClaudeChatBinary() {
        String s = System.getenv("NANODASH_CLAUDE_CHAT_BINARY");
        if (s != null && !s.isBlank()) return s;
        return claudeChatBinary;
    }

    /**
     * Set the command to run the Claude Code CLI.
     *
     * @param claudeChatBinary the binary name or path
     */
    public void setClaudeChatBinary(String claudeChatBinary) {
        this.claudeChatBinary = claudeChatBinary;
    }

    /**
     * Get the model override for Claude Code chat sessions.
     *
     * @return the model name, or null to use the CLI's default
     */
    public String getClaudeChatModel() {
        String s = System.getenv("NANODASH_CLAUDE_CHAT_MODEL");
        if (s != null && !s.isBlank()) return s;
        return claudeChatModel;
    }

    /**
     * Set the model override for Claude Code chat sessions.
     *
     * @param claudeChatModel the model name
     */
    public void setClaudeChatModel(String claudeChatModel) {
        this.claudeChatModel = claudeChatModel;
    }

    /**
     * Check whether remote MCP access with per-user API tokens is enabled.
     *
     * Lets users point their own AI agents at this instance's /mcp endpoint;
     * independent of the local Claude chat feature (either can be enabled
     * without the other). See docs/remote-mcp.md.
     *
     * @return true if remote MCP access is enabled
     */
    public boolean isMcpRemoteEnabled() {
        if ("true".equals(System.getenv("NANODASH_MCP_REMOTE_ENABLED"))) {
            return true;
        }
        return mcpRemoteEnabled;
    }

    /**
     * Set whether remote MCP access with per-user API tokens is enabled.
     *
     * @param mcpRemoteEnabled true to enable
     */
    public void setMcpRemoteEnabled(boolean mcpRemoteEnabled) {
        this.mcpRemoteEnabled = mcpRemoteEnabled;
    }

    /**
     * Get the file where the API cache is persisted across restarts, from the
     * {@code NANODASH_API_CACHE_FILE} environment variable or the preferences file, falling
     * back to {@link #DEFAULT_API_CACHE_PATH} in the user's home directory.
     *
     * @return the snapshot file path, or null when persistence is disabled with the value
     *         {@value #API_CACHE_DISABLED}
     */
    public String getApiCacheFile() {
        String s = System.getenv("NANODASH_API_CACHE_FILE");
        if (s == null || s.isBlank()) s = apiCacheFile;
        if (s == null || s.isBlank()) s = System.getProperty("user.home") + DEFAULT_API_CACHE_PATH;
        if (API_CACHE_DISABLED.equalsIgnoreCase(s.trim())) return null;
        return s;
    }

    /**
     * Set the file where the API cache is persisted across restarts.
     *
     * @param apiCacheFile the snapshot file path, or {@value #API_CACHE_DISABLED} to disable
     *                     persistence
     */
    public void setApiCacheFile(String apiCacheFile) {
        this.apiCacheFile = apiCacheFile;
    }

    public String getHomeResource() {
        String s = System.getenv("NANODASH_HOME_RESOURCE");
        if (s != null && !s.isBlank()) {
            logger.debug("Found environment variable NANODASH_HOME_RESOURCE with value: {}", s);
            return s;
        }
        logger.debug("Environment variable NANODASH_HOME_RESOURCE not set, using default: {}", homeResource);
        return homeResource;
    }

    public void setHomeResource(String homeResource) {
        this.homeResource = homeResource;
    }

}
