package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.NanodashPreferences;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NanopubActionTest {

    @Test
    void getActionsFromPreferencesReturnsEmptyListForNullPreferences() {
        List<NanopubAction> result = NanopubAction.getActionsFromPreferences(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getActionsFromPreferencesReturnsEmptyListForEmptyPreferences() {
        NanodashPreferences pref = mock(NanodashPreferences.class);
        when(pref.getNanopubActions()).thenReturn(List.of());
        List<NanopubAction> result = NanopubAction.getActionsFromPreferences(pref);
        assertTrue(result.isEmpty());
    }

    @Test
    void getActionsFromPreferencesReturnsDefaultActionsForValidPreferences() {
        NanodashPreferences pref = mock(NanodashPreferences.class);
        when(pref.getNanopubActions()).thenReturn(List.of("com.knowledgepixels.nanodash.action.ImproveAction",
                "com.knowledgepixels.nanodash.action.UpdateAction"));
        List<NanopubAction> result = NanopubAction.getActionsFromPreferences(pref);
        assertEquals(1, result.size());
        assertInstanceOf(ImproveAction.class, result.getFirst());
    }

    @Test
    void getActionsFromPreferencesIgnoresInvalidClassNames() {
        NanodashPreferences pref = mock(NanodashPreferences.class);
        when(pref.getNanopubActions()).thenReturn(List.of("com.invalid.ClassName"));
        List<NanopubAction> result = NanopubAction.getActionsFromPreferences(pref);
        assertTrue(result.isEmpty());
    }

}