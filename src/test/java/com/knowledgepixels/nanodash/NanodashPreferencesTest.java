package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static com.knowledgepixels.nanodash.NanodashPreferences.DEFAULT_SETTING_PATH;
import static com.knowledgepixels.nanodash.NanodashPreferences.get;
import static org.junit.jupiter.api.Assertions.*;

class NanodashPreferencesTest {

    @BeforeEach
    void setUp() throws IOException {
        File prefFile = new File(System.getProperty("user.home") + DEFAULT_SETTING_PATH);
        if (prefFile.exists()) {
            Files.delete(Path.of(prefFile.toURI()));
        }
        assertFalse(prefFile.exists());
    }

    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        // Reset the singleton instance to null for isolation between tests
        Field objField = NanodashPreferences.class.getDeclaredField("obj");
        objField.setAccessible(true);
        objField.set(null, null);
    }

    @Test
    void getWhenInstanceAlreadyExists() throws IOException {
        File prefFile = new File(System.getProperty("user.home") + DEFAULT_SETTING_PATH);
        Files.copy(
                Path.of("src/test/resources/nanodash-preferences-test.yml"),
                prefFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );
        assertTrue(prefFile.exists());

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
        File prefFile = new File(System.getProperty("user.home") + DEFAULT_SETTING_PATH);
        Files.createFile(prefFile.toPath());
        assertTrue(prefFile.exists());

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
        File prefFile = new File(System.getProperty("user.home") + DEFAULT_SETTING_PATH);
        Files.copy(
                Path.of("src/test/resources/nanodash-preferences-test.yml"),
                prefFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );
        assertTrue(prefFile.exists());
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

}