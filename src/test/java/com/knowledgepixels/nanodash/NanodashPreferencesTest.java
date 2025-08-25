package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NanodashPreferencesTest {

    @Test
    void getReturnsNewInstanceWhenPreferencesFileDoesNotExist() {
        NanodashPreferences preferences = NanodashPreferences.get();
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
    void setOrcidLoginMode() {
        NanodashPreferences preferences = NanodashPreferences.get();
        assertFalse(preferences.isOrcidLoginMode());

        preferences.setOrcidLoginMode(true);
        assertTrue(preferences.isOrcidLoginMode());

        preferences.setOrcidLoginMode(false);
        assertFalse(preferences.isOrcidLoginMode());
    }

    @Test
    void setSettingUri() {
        NanodashPreferences preferences = NanodashPreferences.get();
        assertNull(preferences.getSettingUri());

        preferences.setSettingUri("/.nanopub/prefs.yml");
        assertEquals("/.nanopub/prefs.yml", preferences.getSettingUri());

        preferences.setSettingUri(null);
        assertNull(preferences.getSettingUri());
    }

    @Test
    void setOrcidClientSecret() {
        NanodashPreferences preferences = NanodashPreferences.get();
        assertNull(preferences.getOrcidClientSecret());

        preferences.setOrcidClientSecret("secret");
        assertEquals("secret", preferences.getOrcidClientSecret());
    }

    @Test
    void setOrcidClientId() {
        NanodashPreferences preferences = NanodashPreferences.get();
        assertNull(preferences.getOrcidClientId());

        preferences.setOrcidClientId("0000-0000-0000-0000");
        assertEquals("0000-0000-0000-0000", preferences.getOrcidClientId());
    }

    @Test
    void setWebsiteUrl() {
        NanodashPreferences preferences = NanodashPreferences.get();
        assertEquals("http://localhost:37373/", preferences.getWebsiteUrl());

        preferences.setWebsiteUrl("http://example.com");
        assertEquals("http://example.com", preferences.getWebsiteUrl());
    }

    @Test
    void setReadOnlyMode() {
        NanodashPreferences preferences = NanodashPreferences.get();
        assertFalse(preferences.isReadOnlyMode());

        preferences.setReadOnlyMode(true);
        assertTrue(preferences.isReadOnlyMode());

        preferences.setReadOnlyMode(false);
        assertFalse(preferences.isReadOnlyMode());
    }

    @Test
    void setNanopubActions() {
        NanodashPreferences preferences = NanodashPreferences.get();
        assertTrue(preferences.getNanopubActions().isEmpty());

        List<String> actions = List.of("com.knowledgepixels.nanodash.action.ImproveAction",
                "com.knowledgepixels.nanodash.action.UpdateAction");
        preferences.setNanopubActions(actions);
        assertEquals(actions, preferences.getNanopubActions());
    }

}