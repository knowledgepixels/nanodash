package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ErrorPageTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    @Test
    void rendersWithoutMessage() {
        tester.startPage(ErrorPage.class);
        tester.assertRenderedPage(ErrorPage.class);
    }

    // Details about what went wrong are shown to the user, so that e.g. a mistyped
    // nanopublication identifier can be told apart from a malfunction (#270).
    @Test
    void rendersGivenMessage() {
        tester.startPage(ErrorPage.class, new PageParameters().add(ErrorPage.MESSAGE_PARAM, "'x' is not a valid nanopublication identifier."));
        tester.assertRenderedPage(ErrorPage.class);
        tester.assertContains("is not a valid nanopublication identifier");
    }

}
