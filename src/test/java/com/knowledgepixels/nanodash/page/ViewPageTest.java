package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class ViewPageTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @Test
    void getMountPathReturnsCorrectPath() throws MalformedNanopubException {
        Nanopub mockNanopub = TestUtils.createNanopub();
        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getAsNanopub(anyString())).thenReturn(mockNanopub);

            ViewPage page = new ViewPage(new PageParameters().add("id", TestUtils.NANOPUB_URI));
            assertEquals(ViewPage.MOUNT_PATH, page.getMountPath());
        }
    }

}