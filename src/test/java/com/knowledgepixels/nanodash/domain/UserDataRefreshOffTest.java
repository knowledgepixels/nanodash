package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.RegistryAccountInfo;
import com.knowledgepixels.nanodash.Utils;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.setting.NanopubSetting;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * What the user data does where the background data refresh is switched off (issue #809).
 * The test run itself is such a place -- surefire sets {@code NANODASH_DATA_REFRESH=false} --
 * because the user lists are whole-store queries and every other test class starts an
 * application that would ask for them.
 */
@ExtendWith(SystemStubsExtension.class)
class UserDataRefreshOffTest {

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    @Test
    void theTestRunHasTheDataRefreshSwitchedOff() {
        assertFalse(NanodashPreferences.get().isDataRefreshEnabled(),
                "the test environment must keep the suite off the public query services");
    }

    @Test
    void emptyUserDataIsCompleteSoNothingRetriesIt() {
        UserData empty = UserData.empty();
        assertTrue(empty.isComplete());
        assertTrue(empty.getUsers(true).isEmpty());
        assertTrue(empty.getUsers(false).isEmpty());
    }

    /**
     * The load has to leave usable data behind even though it reads nothing: session
     * construction goes through {@link User#ensureLoaded()}, and data that stayed null would
     * mean no page at all.
     */
    @Test
    void ensureLoadedQueriesNothingAndStillLeavesUsableData() throws Exception {
        Object previous = takeUserData();
        try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
            User.ensureLoaded();
            apiCache.verifyNoInteractions();
            UserData loaded = currentUserData();
            assertNotNull(loaded);
            assertTrue(loaded.isComplete());
        } finally {
            restoreUserData(previous);
        }
    }

    /**
     * And the other way round, so that the switch cannot be left off where it matters: with
     * the refresh on, the lists are read as before.
     */
    @Test
    void ensureLoadedReadsTheUserListsWhenTheRefreshIsOn() throws Exception {
        envVars.set("NANODASH_DATA_REFRESH", null);
        // Built before the static mocks are opened, or its own stubbing lands inside theirs.
        NanopubSetting setting = staticSetting();
        Object previous = takeUserData();
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryAccountInfo> registry = mockStatic(RegistryAccountInfo.class);
             MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
            settings.when(NanopubSetting::getLocalSetting).thenReturn(setting);
            utils.when(Utils::getMainRegistryUrl).thenReturn("http://localhost:19292/");
            registry.when(() -> RegistryAccountInfo.fromUrl(any())).thenReturn(List.of());
            apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean()))
                    .thenReturn(new ApiResponse(Collections.emptyList()));

            User.ensureLoaded();

            apiCache.verify(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean()), atLeastOnce());
        } finally {
            restoreUserData(previous);
        }
    }

    /** A setting that needs no lookups of its own; see UserDataResilienceTest. */
    private NanopubSetting staticSetting() {
        Nanopub np = mock(Nanopub.class);
        when(np.getUri()).thenReturn(Values.iri("http://example.org/np/setting"));
        NanopubSetting setting = mock(NanopubSetting.class);
        when(setting.getNanopub()).thenReturn(np);
        when(setting.getUpdateStrategy()).thenReturn(Values.iri("http://example.org/no-updates"));
        return setting;
    }

    private static Field userDataField() throws NoSuchFieldException {
        Field field = User.class.getDeclaredField("userData");
        field.setAccessible(true);
        return field;
    }

    private static Object takeUserData() throws Exception {
        Field field = userDataField();
        Object previous = field.get(null);
        field.set(null, null);
        return previous;
    }

    private static UserData currentUserData() throws Exception {
        return (UserData) userDataField().get(null);
    }

    private static void restoreUserData(Object previous) throws Exception {
        userDataField().set(null, previous);
    }
}
