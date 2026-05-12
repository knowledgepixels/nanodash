package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.page.HomePage;
import org.apache.wicket.settings.RequestCycleSettings;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WicketApplicationTest {

    private final WicketTester tester = new WicketTester(new WicketApplication());

    @Test
    void initSetsDefaultMarkupEncodingToUtf8() {
        assertEquals("UTF-8", tester.getApplication().getMarkupSettings().getDefaultMarkupEncoding());
    }

    @Test
    void initDisablesCspBlocking() {
        assertFalse(tester.getApplication().getCspSettings().isEnabled());
    }

    @Test
    void getHomePageReturnsCorrectClass() {
        Class<?> result = tester.getApplication().getHomePage();
        assertEquals(HomePage.class, result);
    }

    @Test
    void getThisVersionReturnsVersionFromProperties() {
        WicketApplication.properties.setProperty("nanodash.version", "1.0");
        String result = WicketApplication.getThisVersion();
        assertEquals("1.0", result);
    }

    @Test
    void getThisVersionHandlesMissingVersionProperty() {
        WicketApplication.properties.clear();
        String result = WicketApplication.getThisVersion();
        assertNull(result);
    }

    @Test
    void getLatestVersionWhenFetchSucceeds() {
        String result = WicketApplication.getLatestVersion();
        assertTrue(result.matches("\\d+.\\d+(.\\d+)*"));
    }

    // Guards #456: ONE_PASS_RENDER must not be re-introduced — it breaks F5 form-state preservation.
    @Test
    void initUsesDefaultRedirectToBufferRenderStrategy() {
        assertEquals(
                RequestCycleSettings.RenderStrategy.REDIRECT_TO_BUFFER,
                tester.getApplication().getRequestCycleSettings().getRenderStrategy()
        );
    }

}