package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTokenServiceTest {

    private static final String USER = "https://orcid.org/0000-0001-2345-6789";
    private static final String OTHER_USER = "https://orcid.org/0000-0009-8765-4321";

    @TempDir
    Path tempDir;

    private File storeFile() {
        return tempDir.resolve("api-tokens.yml").toFile();
    }

    @Test
    void createAuthenticateListRevoke() {
        ApiTokenService service = new ApiTokenService(storeFile());

        String token = service.createToken(USER, "  my laptop  ");
        assertTrue(token.startsWith("ndmcp_"));

        assertEquals(USER, service.authenticate(token));
        assertNull(service.authenticate("ndmcp_" + "0".repeat(64)));
        assertNull(service.authenticate(null));
        assertNull(service.authenticate(""));

        assertEquals(1, service.getTokens(USER).size());
        ApiTokenService.ApiToken stored = service.getTokens(USER).get(0);
        assertEquals("my laptop", stored.getLabel());
        assertTrue(stored.getLastUsed() > 0);
        assertTrue(service.getTokens(OTHER_USER).isEmpty());

        assertFalse(service.revokeToken(OTHER_USER, stored.getTokenHash()));
        assertEquals(USER, service.authenticate(token));
        assertTrue(service.revokeToken(USER, stored.getTokenHash()));
        assertNull(service.authenticate(token));
        assertTrue(service.getTokens(USER).isEmpty());
    }

    @Test
    void storeSurvivesReloadAndHoldsNoPlaintext() throws Exception {
        ApiTokenService service = new ApiTokenService(storeFile());
        String token1 = service.createToken(USER, "one");
        String token2 = service.createToken(OTHER_USER, "two");

        String fileContent = Files.readString(storeFile().toPath(), StandardCharsets.UTF_8);
        assertFalse(fileContent.contains(token1));
        assertFalse(fileContent.contains(token2));
        assertTrue(fileContent.contains(Utils.createSha256HexHash(token1)));

        ApiTokenService reloaded = new ApiTokenService(storeFile());
        assertEquals(USER, reloaded.authenticate(token1));
        assertEquals(OTHER_USER, reloaded.authenticate(token2));
        assertEquals("one", reloaded.getTokens(USER).get(0).getLabel());
    }

    @Test
    void missingFileStartsEmpty() {
        ApiTokenService service = new ApiTokenService(storeFile());
        assertTrue(service.getTokens(USER).isEmpty());
        assertNull(service.authenticate("ndmcp_anything"));
    }

    @Test
    void longLabelIsTruncated() {
        ApiTokenService service = new ApiTokenService(storeFile());
        service.createToken(USER, "x".repeat(200));
        assertEquals(80, service.getTokens(USER).get(0).getLabel().length());
    }

}
