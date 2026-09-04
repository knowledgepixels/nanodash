package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.server.RegistryInfo;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ServiceModeTest {

    private static final String REGISTRY_URL = "http://localhost:19292/";

    @BeforeEach
    @AfterEach
    void clearProbedModes() throws Exception {
        // The probes are cached for the lifetime of the process, so each test starts from unprobed.
        for (String name : new String[]{"registryIsLocal", "queryIsLocal"}) {
            Field f = ServiceMode.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, null);
        }
    }

    /**
     * Sets the query answer directly rather than serving a header: the query probe makes a plain
     * HTTP call, and what it does with the header is the Query service's contract, not ours.
     */
    private void setQueryIsLocal(boolean value) throws Exception {
        Field f = ServiceMode.class.getDeclaredField("queryIsLocal");
        f.setAccessible(true);
        f.set(null, value);
    }

    @Test
    void registryReportingLocalInstanceMakesTheDeploymentRestricted() throws Exception {
        setQueryIsLocal(false);
        RegistryInfo info = mock(RegistryInfo.class);
        when(info.isLocalInstance()).thenReturn(true);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryInfo> registryInfo = mockStatic(RegistryInfo.class)) {
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registryInfo.when(() -> RegistryInfo.load(REGISTRY_URL)).thenReturn(info);

            assertTrue(ServiceMode.isRegistryLocal());
            assertTrue(ServiceMode.isRestricted());
        }
    }

    @Test
    void publicServicesAreNotRestricted() throws Exception {
        setQueryIsLocal(false);
        RegistryInfo info = mock(RegistryInfo.class);
        when(info.isLocalInstance()).thenReturn(false);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryInfo> registryInfo = mockStatic(RegistryInfo.class)) {
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registryInfo.when(() -> RegistryInfo.load(REGISTRY_URL)).thenReturn(info);

            assertFalse(ServiceMode.isRestricted());
        }
    }

    @Test
    void anUnreachableRegistryIsReadAsNotLocal() throws Exception {
        // Not knowing the mode must never propagate as an error: the flag is extra information.
        setQueryIsLocal(false);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryInfo> registryInfo = mockStatic(RegistryInfo.class)) {
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registryInfo.when(() -> RegistryInfo.load(REGISTRY_URL))
                    .thenThrow(new RegistryInfo.RegistryInfoException(REGISTRY_URL));

            assertFalse(ServiceMode.isRegistryLocal());
            assertFalse(ServiceMode.isRestricted());
        }
    }

    @Test
    void aLocalQueryServiceAloneMakesTheDeploymentRestricted() throws Exception {
        setQueryIsLocal(true);
        RegistryInfo info = mock(RegistryInfo.class);
        when(info.isLocalInstance()).thenReturn(false);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<RegistryInfo> registryInfo = mockStatic(RegistryInfo.class)) {
            utils.when(Utils::getMainRegistryUrl).thenReturn(REGISTRY_URL);
            registryInfo.when(() -> RegistryInfo.load(REGISTRY_URL)).thenReturn(info);

            assertTrue(ServiceMode.isRestricted());
        }
    }

}
