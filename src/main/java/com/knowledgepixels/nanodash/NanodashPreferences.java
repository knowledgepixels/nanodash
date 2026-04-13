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
    private String websiteUrl = "http://localhost:37373/";
    private boolean orcidLoginMode = false;
    private String orcidClientId;
    private String orcidClientSecret;
    private String settingUri;
    private String umamiScriptUrl;
    private String umamiWebsiteId;
    private String homeResource = "https://w3id.org/spaces/knowledgepixels/nanodash/r/home";
    public static final String DEFAULT_SETTING_PATH = "/.nanopub/nanodash-preferences.yml";

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
     * @return the website URL
     */
    public String getWebsiteUrl() {
        String s = System.getenv("NANODASH_WEBSITE_URL");
        if (s != null && !s.isBlank()) {
            logger.debug("Found environment variable NANODASH_WEBSITE_URL with value: {}", s);
            return s;
        }
        logger.debug("Environment variable NANODASH_WEBSITE_URL not set, using default: {}", websiteUrl);
        return websiteUrl;
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
