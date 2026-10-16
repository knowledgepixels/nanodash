package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ServiceMode;
import com.knowledgepixels.nanodash.WicketApplication;
import org.mockito.MockedStatic;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(SystemStubsExtension.class)
class PrivacyPageTest {

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    private String renderedPage() {
        return tester.getLastResponse().getDocument();
    }

    // The one thing a user of any instance has to understand before they publish: it
    // cannot be undone afterwards.
    @Test
    void saysThatPublishingCannotBeUndone() {
        tester.startPage(PrivacyPage.class);
        tester.assertRenderedPage(PrivacyPage.class);
        tester.assertContains("public and permanent");
        tester.assertContains("withdrawn or deleted");
    }

    // The protected option exists only where the main registry is a local instance. Naming
    // it anywhere else would soften the warning above with a way out the reader has not got.
    @Test
    void mentionsProtectedNanopubsOnlyWhereTheyArePossible() {
        try (MockedStatic<ServiceMode> serviceMode = mockStatic(ServiceMode.class)) {
            serviceMode.when(ServiceMode::isRegistryLocal).thenReturn(false);
            tester.startPage(PrivacyPage.class);
            tester.assertInvisible("protected");
            tester.assertContains("public and permanent");

            serviceMode.when(ServiceMode::isRegistryLocal).thenReturn(true);
            tester.startPage(PrivacyPage.class);
            tester.assertVisible("protected");
            tester.assertContains("mark as protected");
        }
    }

    // Nanodash is run by whoever deploys it, so the page can only name an operator that
    // the deployment has configured.
    @Test
    void namesTheOperatorWhenOneIsConfigured() {
        envVars.set("NANODASH_PRIVACY_OPERATOR", "Example Org, Somewhere - privacy@example.org");
        tester.startPage(PrivacyPage.class);
        tester.assertVisible("operator");
        tester.assertInvisible("no-operator");
        tester.assertContains("privacy@example.org");
    }

    // Saying nothing is better than implying that whoever built the software is answerable
    // for an instance they have never heard of.
    @Test
    void admitsWhenNoOperatorIsConfigured() {
        tester.startPage(PrivacyPage.class);
        tester.assertInvisible("operator");
        tester.assertVisible("no-operator");
        // The markup wraps, so match only as far as the line break.
        tester.assertContains("has not been configured to name its");
    }

    // An instance that runs no analytics should not claim to.
    @Test
    void mentionsAnalyticsOnlyWhenTheyAreConfigured() {
        tester.startPage(PrivacyPage.class);
        tester.assertInvisible("analytics");

        envVars.set("NANODASH_UMAMI_SCRIPT_URL", "https://umami.example.org/script.js");
        envVars.set("NANODASH_UMAMI_WEBSITE_ID", "some-id");
        tester.startPage(PrivacyPage.class);
        tester.assertVisible("analytics");
    }

    // A privacy notice has to be reachable from wherever the visitor is, so the link is in
    // the footer of every page rather than on the home page alone.
    @Test
    void everyPageLinksIt() {
        tester.startPage(ErrorPage.class);
        tester.assertVisible("privacy-link");
        tester.assertInvisible("privacy-link-external");
    }

    // A deployment with legal pages of its own points the footer at them instead.
    @Test
    void aConfiguredUrlReplacesTheBuiltInPage() {
        envVars.set("NANODASH_PRIVACY_URL", "https://example.org/privacy");
        tester.startPage(ErrorPage.class);
        tester.assertInvisible("privacy-link");
        tester.assertVisible("privacy-link-external");
        assertTrue(renderedPage().contains("href=\"https://example.org/privacy\""), renderedPage());
    }

}
