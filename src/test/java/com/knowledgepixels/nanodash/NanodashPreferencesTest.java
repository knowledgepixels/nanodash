package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import static com.knowledgepixels.nanodash.NanodashPreferences.DEFAULT_SETTING_PATH;
import static com.knowledgepixels.nanodash.NanodashPreferences.get;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class NanodashPreferencesTest {

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    /**
     * Preferences are read from here for the length of a test, rather than from the
     * developer's own {@code ~/.nanopub} — which holds a configuration they actually use and
     * their nanopub signing keys, and which the other three test JVMs are reading at the same
     * time (issue #813). A fresh directory per test, so no test sees another's fixture.
     */
    @TempDir
    Path configDir;

    private Path prefFile;

    @BeforeEach
    void setUp() {
        prefFile = configDir.resolve("nanodash-preferences.yml");
        NanodashPreferences.setSettingFile(prefFile.toFile());
        assertFalse(Files.exists(prefFile));
    }

    @AfterEach
    void tearDown() {
        // Back to the default location, and the singleton read from this test's file forgotten.
        NanodashPreferences.setSettingFile(null);
    }

    /**
     * Put the test preferences file in place, as a configured instance would have it.
     */
    private void writePreferencesFile() throws IOException {
        Files.copy(
                Path.of("src/test/resources/nanodash-preferences-test.yml"),
                prefFile,
                StandardCopyOption.REPLACE_EXISTING
        );
        assertTrue(Files.exists(prefFile));
    }

    /**
     * The seam only says where to read from; left alone, that is the file in the user's home
     * directory which a deployment configures.
     */
    @Test
    void theDefaultSettingFileIsTheOneInTheHomeDirectory() {
        NanodashPreferences.setSettingFile(null);
        assertEquals(
                new File(System.getProperty("user.home") + DEFAULT_SETTING_PATH),
                NanodashPreferences.getSettingFile()
        );
    }

    @Test
    void getWhenInstanceAlreadyExists() throws IOException {
        writePreferencesFile();

        NanodashPreferences firstInstance = get();
        NanodashPreferences secondInstance = get();
        assertSame(firstInstance, secondInstance);
    }

    @Test
    void getWithoutExistingPreferences() {
        NanodashPreferences preferences = get();
        assertNotNull(preferences);
        assertTrue(preferences.getNanopubActions().isEmpty());
        assertFalse(preferences.isReadOnlyMode());
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());
        assertFalse(preferences.isOrcidLoginMode());
        assertNull(preferences.getOrcidClientId());
        assertNull(preferences.getOrcidClientSecret());
        assertNull(preferences.getSettingUri());
    }

    @Test
    void getWithCorruptedPreferencesFile() throws IOException {
        Files.createFile(prefFile);
        assertTrue(Files.exists(prefFile));

        NanodashPreferences preferences = get();
        assertNotNull(preferences);
        assertTrue(preferences.getNanopubActions().isEmpty());
        assertFalse(preferences.isReadOnlyMode());
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());
        assertFalse(preferences.isOrcidLoginMode());
        assertNull(preferences.getOrcidClientId());
        assertNull(preferences.getOrcidClientSecret());
        assertNull(preferences.getSettingUri());
    }


    @Test
    void getWithExistingPreferences() throws IOException {
        writePreferencesFile();
        NanodashPreferences preferences = get();
        assertFalse(preferences.isReadOnlyMode());
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());
        assertEquals("APP-W02BIN0XPD5T5PFL", preferences.getOrcidClientId());
        assertEquals("r4nd0mS3cr3t", preferences.getOrcidClientSecret());
    }

    @Test
    void setOrcidLoginMode() {
        NanodashPreferences preferences = get();
        assertFalse(preferences.isOrcidLoginMode());

        preferences.setOrcidLoginMode(true);
        assertTrue(preferences.isOrcidLoginMode());

        preferences.setOrcidLoginMode(false);
        assertFalse(preferences.isOrcidLoginMode());
    }

    @Test
    void setSettingUri() {
        NanodashPreferences preferences = get();
        assertNull(preferences.getSettingUri());

        preferences.setSettingUri("/.nanopub/prefs.yml");
        assertEquals("/.nanopub/prefs.yml", preferences.getSettingUri());

        preferences.setSettingUri(null);
        assertNull(preferences.getSettingUri());
    }

    @Test
    void setOrcidClientSecret() {
        NanodashPreferences preferences = get();
        assertNull(preferences.getOrcidClientSecret());

        preferences.setOrcidClientSecret("secret");
        assertEquals("secret", preferences.getOrcidClientSecret());
    }

    @Test
    void setOrcidClientId() {
        NanodashPreferences preferences = get();
        assertNull(preferences.getOrcidClientId());

        preferences.setOrcidClientId("0000-0000-0000-0000");
        assertEquals("0000-0000-0000-0000", preferences.getOrcidClientId());
    }

    @Test
    void setWebsiteUrl() {
        NanodashPreferences preferences = get();
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());

        preferences.setWebsiteUrl("http://example.com");
        assertEquals("http://example.com", preferences.getWebsiteUrl());
    }

    @Test
    void setReadOnlyMode() {
        NanodashPreferences preferences = get();
        assertFalse(preferences.isReadOnlyMode());

        preferences.setReadOnlyMode(true);
        assertTrue(preferences.isReadOnlyMode());

        preferences.setReadOnlyMode(false);
        assertFalse(preferences.isReadOnlyMode());
    }

    @Test
    void setNanopubActions() {
        NanodashPreferences preferences = get();
        assertTrue(preferences.getNanopubActions().isEmpty());

        List<String> actions = List.of("com.knowledgepixels.nanodash.action.ImproveAction",
                "com.knowledgepixels.nanodash.action.UpdateAction");
        preferences.setNanopubActions(actions);
        assertEquals(actions, preferences.getNanopubActions());
    }

    @Test
    void getWebsiteUrlWithDefaultValue() {
        NanodashPreferences preferences = get();
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());
    }

    @Test
    void getWebsiteUrlFromSystemEnv() {
        String websiteUrl = "https://example.com";
        envVars.set("NANODASH_WEBSITE_URL", websiteUrl);
        NanodashPreferences preferences = get();
        assertEquals(websiteUrl, preferences.getWebsiteUrl());
    }

    @Test
    void getWebsiteUrlFromSystemEnvBlankOrNull() {
        String websiteUrl = null;
        envVars.set("NANODASH_WEBSITE_URL", websiteUrl);
        NanodashPreferences preferences = get();
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());

        websiteUrl = "";
        envVars.set("NANODASH_WEBSITE_URL", websiteUrl);
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());

        websiteUrl = " ";
        envVars.set("NANODASH_WEBSITE_URL", websiteUrl);
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());
    }

    // Whether an address was configured at all is a different question from what it is: a URL
    // handed to an outside fetcher must not be built on the localhost fallback. See
    // Utils.absolutePageUrl.
    @Test
    void getConfiguredWebsiteUrlIsNullWhenNothingSetsOne() {
        assertNull(get().getConfiguredWebsiteUrl());
    }

    @Test
    void getConfiguredWebsiteUrlFromSystemEnv() {
        envVars.set("NANODASH_WEBSITE_URL", "https://nanodash.example.org/");
        assertEquals("https://nanodash.example.org/", get().getConfiguredWebsiteUrl());
    }

    @Test
    void getConfiguredWebsiteUrlFromPreferencesFile() throws IOException {
        writePreferencesFile();
        assertEquals("http://localhost:37373/", get().getConfiguredWebsiteUrl());
    }

    @Test
    void getOrcidClientIdWithDefaultValue() {
        NanodashPreferences preferences = get();
        assertNull(preferences.getOrcidClientId());
    }

    @Test
    void getOrcidClientIdFromSystemEnv() {
        String orcidClientId = "https://orcid.org/0000-0000-0000-0000";
        envVars.set("NANOPUB_ORCID_CLIENT_ID", orcidClientId);
        NanodashPreferences preferences = get();
        assertEquals(orcidClientId, preferences.getOrcidClientId());
    }

    @Test
    void getOrcidClientIdFromSystemEnvBlankOrNull() {
        String orcidClientId = null;
        envVars.set("NANOPUB_ORCID_CLIENT_ID", orcidClientId);
        NanodashPreferences preferences = get();
        assertNull(preferences.getOrcidClientId());

        orcidClientId = "";
        envVars.set("NANOPUB_ORCID_CLIENT_ID", orcidClientId);
        assertNull(preferences.getOrcidClientId());

        orcidClientId = " ";
        envVars.set("NANOPUB_ORCID_CLIENT_ID", orcidClientId);
        assertNull(preferences.getOrcidClientId());
    }

    @Test
    void getOrcidClientSecretWithDefaultValue() {
        NanodashPreferences preferences = get();
        assertNull(preferences.getOrcidClientSecret());
    }

    @Test
    void getOrcidClientSecretFromSystemEnv() {
        String orcidClientSecret = "r4nd0mS3cr3t";
        envVars.set("NANOPUB_ORCID_CLIENT_SECRET", orcidClientSecret);
        NanodashPreferences preferences = get();
        assertEquals(orcidClientSecret, preferences.getOrcidClientSecret());
    }

    @Test
    void getOrcidClientSecretFromSystemEnvBlankOrNull() {
        String orcidClientSecret = null;
        envVars.set("NANOPUB_ORCID_CLIENT_SECRET", orcidClientSecret);
        NanodashPreferences preferences = get();
        assertNull(preferences.getOrcidClientSecret());

        orcidClientSecret = "";
        envVars.set("NANOPUB_ORCID_CLIENT_SECRET", orcidClientSecret);
        assertNull(preferences.getOrcidClientSecret());

        orcidClientSecret = " ";
        envVars.set("NANOPUB_ORCID_CLIENT_SECRET", orcidClientSecret);
        assertNull(preferences.getOrcidClientSecret());
    }

    @Test
    void isOrcidLoginModeWithDefaultValue() {
        NanodashPreferences preferences = get();
        assertFalse(preferences.isOrcidLoginMode());
    }

    @Test
    void isOrcidLoginModeFromSystemEnvTrue() {
        NanodashPreferences preferences = get();
        envVars.set("NANODASH_ORCID_LOGIN_MODE", "true");
        assertTrue(preferences.isOrcidLoginMode());

        envVars.set("NANODASH_ORCID_LOGIN_MODE", true);
        assertTrue(preferences.isOrcidLoginMode());
    }

    @Test
    void isOrcidLoginModeFromSystemEnvNotTrue() {
        NanodashPreferences preferences = get();
        envVars.set("NANODASH_ORCID_LOGIN_MODE", "anythingButTrue");
        assertFalse(preferences.isOrcidLoginMode());
    }

    @Test
    void isReadOnlyModeWithDefaultValue() {
        NanodashPreferences preferences = get();
        assertFalse(preferences.isReadOnlyMode());
    }

    @Test
    void isReadOnlyModeFromSystemEnvTrue() {
        NanodashPreferences preferences = get();
        envVars.set("NANODASH_READ_ONLY_MODE", "true");
        assertTrue(preferences.isReadOnlyMode());

        envVars.set("NANODASH_READ_ONLY_MODE", true);
        assertTrue(preferences.isReadOnlyMode());
    }

    @Test
    void isReadOnlyModeFromSystemEnvNotTrue() {
        NanodashPreferences preferences = get();
        envVars.set("NANODASH_READ_ONLY_MODE", "anythingButTrue");
        assertFalse(preferences.isReadOnlyMode());
    }

    @Test
    void getNanopubActionsWithDefaultValue() {
        NanodashPreferences preferences = get();
        assertTrue(preferences.getNanopubActions().isEmpty());
    }

    @Test
    void getNanopubActionsFromSystemEnv() {
        String nanopubActions = "com.knowledgepixels.nanodash.action.ImproveAction com.knowledgepixels.nanodash.action.UpdateAction";
        List<String> expectedActions = Arrays.asList(nanopubActions.split(" "));
        envVars.set("NANODASH_NANOPUB_ACTIONS", nanopubActions);
        NanodashPreferences preferences = get();
        assertEquals(expectedActions, preferences.getNanopubActions());
    }

    @Test
    void getNanopubActionsFromSystemEnvBlankOrNull() {
        String nanopubActions = null;
        envVars.set("NANODASH_NANOPUB_ACTIONS", nanopubActions);
        NanodashPreferences preferences = get();
        assertTrue(preferences.getNanopubActions().isEmpty());

        nanopubActions = "";
        envVars.set("NANOPUB_ORCID_CLIENT_SECRET", nanopubActions);
        assertTrue(preferences.getNanopubActions().isEmpty());

        nanopubActions = " ";
        envVars.set("NANODASH_NANOPUB_ACTIONS", nanopubActions);
        assertTrue(preferences.getNanopubActions().isEmpty());
    }

    @Test
    void siteSettingsAreUnsetByDefault() {
        NanodashPreferences preferences = get();
        assertNull(preferences.getSiteSpace());
        assertNull(preferences.getSiteName());
        assertNull(preferences.getSiteLogo());
        assertNull(preferences.getSiteCss());
        assertTrue(preferences.isSiteExternalLinks());
    }

    @Test
    void siteSettingsComeFromTheEnvironment() {
        envVars.set("NANODASH_SITE_SPACE", "https://example.org/spaces/site");
        envVars.set("NANODASH_SITE_NAME", "Site");
        envVars.set("NANODASH_SITE_LOGO", "https://example.org/logo.png");
        envVars.set("NANODASH_SITE_CSS", "https://example.org/site.css");
        envVars.set("NANODASH_SITE_EXTERNAL_LINKS", "false");
        NanodashPreferences preferences = get();
        assertEquals("https://example.org/spaces/site", preferences.getSiteSpace());
        assertEquals("Site", preferences.getSiteName());
        assertEquals("https://example.org/logo.png", preferences.getSiteLogo());
        assertEquals("https://example.org/site.css", preferences.getSiteCss());
        assertFalse(preferences.isSiteExternalLinks());
    }

    @Test
    void siteSettingsCanBeSet() {
        NanodashPreferences preferences = get();
        preferences.setSiteSpace("https://example.org/spaces/site");
        preferences.setSiteName("Site");
        preferences.setSiteExternalLinks(false);
        assertEquals("https://example.org/spaces/site", preferences.getSiteSpace());
        assertEquals("Site", preferences.getSiteName());
        assertFalse(preferences.isSiteExternalLinks());
        // Blank means unset, as for the environment variables.
        preferences.setSiteSpace("  ");
        assertNull(preferences.getSiteSpace());
    }

    // The background data refresh (issue #809): on unless something switches it off, which the
    // test run does so that its applications stay off the public query services.
    @Test
    void dataRefreshIsOnWhenNothingSwitchesItOff() {
        envVars.set("NANODASH_DATA_REFRESH", null);
        assertTrue(get().isDataRefreshEnabled());
    }

    @Test
    void dataRefreshIsSwitchedOffFromTheEnvironment() {
        envVars.set("NANODASH_DATA_REFRESH", "false");
        assertFalse(get().isDataRefreshEnabled());
        envVars.set("NANODASH_DATA_REFRESH", "FALSE");
        assertFalse(get().isDataRefreshEnabled());
    }

    @Test
    void dataRefreshCanBeSet() {
        envVars.set("NANODASH_DATA_REFRESH", null);
        NanodashPreferences preferences = get();
        preferences.setDataRefresh(false);
        assertFalse(preferences.isDataRefreshEnabled());
        preferences.setDataRefresh(true);
        assertTrue(preferences.isDataRefreshEnabled());
    }

    // The environment wins over the preferences file, as it does for the other settings.
    @Test
    void dataRefreshFromTheEnvironmentBeatsThePreferencesFile() {
        envVars.set("NANODASH_DATA_REFRESH", "false");
        NanodashPreferences preferences = get();
        preferences.setDataRefresh(true);
        assertFalse(preferences.isDataRefreshEnabled());
    }

}
