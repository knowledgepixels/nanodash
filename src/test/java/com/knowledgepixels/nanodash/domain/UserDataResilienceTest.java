package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.RegistryAccountInfo;
import com.knowledgepixels.nanodash.Utils;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.setting.NanopubSetting;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * What user data does when a service it is built from cannot answer (issue #684): a cold
 * instance whose query service is unavailable has to come up all the same, since session
 * construction goes through here and a session that cannot be built means no page at all.
 */
class UserDataResilienceTest {

    private static final String REGISTRY_URL = "http://localhost:19292/";

    /**
     * A setting that needs no lookups of its own, so that these tests are about the user
     * data rather than about resolving the setting's latest version.
     */
    private NanopubSetting staticSetting() {
        Nanopub np = mock(Nanopub.class);
        when(np.getUri()).thenReturn(Values.iri("http://example.org/np/setting"));
        NanopubSetting setting = mock(NanopubSetting.class);
        when(setting.getNanopub()).thenReturn(np);
        when(setting.getUpdateStrategy()).thenReturn(Values.iri("http://example.org/no-updates"));
        return setting;
    }

    private ApiResponse emptyResponse() {
        return new ApiResponse(Collections.emptyList());
    }

    @Test
    void aFailingQueryServiceYieldsIncompleteDataRatherThanAnException() {
        NanopubSetting setting = staticSetting();
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryAccountInfo> registry = mockStatic(RegistryAccountInfo.class);
             MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
            settings.when(NanopubSetting::getLocalSetting).thenReturn(setting);
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registry.when(() -> RegistryAccountInfo.fromUrl(any())).thenReturn(List.of());
            apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean()))
                    .thenThrow(new RuntimeException("Query failed"));

            UserData userData = new UserData(false);

            assertFalse(userData.isComplete());
            // Serviceable all the same: the lookups the pages make answer with nothing.
            assertTrue(userData.getIntroNanopubs(Values.iri("https://orcid.org/0000-0000-0000-0000")).isEmpty());
            assertTrue(userData.getIntroNanopubs("some-pubkey").isEmpty());
            assertNull(userData.getProfilePicture(Values.iri("https://orcid.org/0000-0000-0000-0000")));
        }
    }

    @Test
    void anUnreachableQueryServiceYieldsIncompleteDataWhenItAnswersWithNothingAtAll() {
        NanopubSetting setting = staticSetting();
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryAccountInfo> registry = mockStatic(RegistryAccountInfo.class);
             MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
            settings.when(NanopubSetting::getLocalSetting).thenReturn(setting);
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registry.when(() -> RegistryAccountInfo.fromUrl(any())).thenReturn(List.of());
            // Nothing cached and nothing fetched: the cache hands back null.
            apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean())).thenReturn(null);

            assertFalse(new UserData(false).isComplete());
        }
    }

    @Test
    void anUnreachableRegistryYieldsIncompleteDataRatherThanAnException() {
        NanopubSetting setting = staticSetting();
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryAccountInfo> registry = mockStatic(RegistryAccountInfo.class);
             MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
            settings.when(NanopubSetting::getLocalSetting).thenReturn(setting);
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registry.when(() -> RegistryAccountInfo.fromUrl(any())).thenThrow(new IOException("connection refused"));
            apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean())).thenReturn(emptyResponse());

            assertFalse(new UserData(false).isComplete());
        }
    }

    @Test
    void dataIsCompleteWhenEverySourceAnswers() {
        NanopubSetting setting = staticSetting();
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryAccountInfo> registry = mockStatic(RegistryAccountInfo.class);
             MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
            settings.when(NanopubSetting::getLocalSetting).thenReturn(setting);
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registry.when(() -> RegistryAccountInfo.fromUrl(any())).thenReturn(List.of());
            apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean())).thenReturn(emptyResponse());

            assertTrue(new UserData(false).isComplete());
        }
    }

    @Test
    void incompleteDataNeverReplacesCompleteData() {
        UserData complete = mock(UserData.class);
        when(complete.isComplete()).thenReturn(true);
        UserData incomplete = mock(UserData.class);
        when(incomplete.isComplete()).thenReturn(false);

        // Anything is better than nothing, and a full load always wins:
        assertTrue(User.shouldReplace(null, incomplete));
        assertTrue(User.shouldReplace(null, complete));
        assertTrue(User.shouldReplace(incomplete, complete));
        assertTrue(User.shouldReplace(incomplete, incomplete));
        assertTrue(User.shouldReplace(complete, complete));
        // ... but a passing outage must not empty out user data that was perfectly good:
        assertFalse(User.shouldReplace(complete, incomplete));
    }
}
