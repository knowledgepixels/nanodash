package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.ServiceHealth.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ServiceHealthTest {

    @BeforeEach
    @AfterEach
    void resetStates() throws Exception {
        setState("queryState", State.UNKNOWN);
        setState("registryState", State.UNKNOWN);
    }

    private void setState(String field, State state) throws Exception {
        Field f = ServiceHealth.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, state);
    }

    @Test
    void healthyServicesHaveNothingToReport() throws Exception {
        setState("queryState", State.HEALTHY);
        setState("registryState", State.HEALTHY);
        assertNull(ServiceHealth.getNote());
    }

    @Test
    void unknownStateIsNotReportedAsAnOutage() {
        // Both states start out unknown, and a failing check leaves them that way: saying nothing
        // is the only safe thing to say when the health of a service could not be established.
        assertNull(ServiceHealth.getNote());
    }

    @Test
    void aLoadingQueryServiceIsDistinguishedFromAnUnreachableOne() throws Exception {
        setState("registryState", State.HEALTHY);

        setState("queryState", State.LOADING);
        String loading = ServiceHealth.getNote();
        assertNotNull(loading);
        assertTrue(loading.contains("still loading"), loading);

        setState("queryState", State.UNREACHABLE);
        String unreachable = ServiceHealth.getNote();
        assertNotNull(unreachable);
        assertTrue(unreachable.contains("cannot be reached"), unreachable);
        assertNotEquals(loading, unreachable);
    }

    @Test
    void anUnreachableRegistryIsReported() throws Exception {
        setState("queryState", State.HEALTHY);
        setState("registryState", State.UNREACHABLE);
        String note = ServiceHealth.getNote();
        assertNotNull(note);
        assertTrue(note.contains("registry"), note);
    }

    @Test
    void bothServicesFailingAreReportedTogether() throws Exception {
        setState("queryState", State.UNREACHABLE);
        setState("registryState", State.UNREACHABLE);
        String note = ServiceHealth.getNote();
        assertNotNull(note);
        assertTrue(note.contains("query service"), note);
        assertTrue(note.contains("registry"), note);
    }

}
