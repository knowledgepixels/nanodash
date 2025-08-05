package com.knowledgepixels.nanodash.component;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HorizontalLineTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @Test
    void rendersHorizontalLineComponent() {
        HorizontalLine horizontalLine = new HorizontalLine("horizontalLine");

        tester.startComponentInPage(horizontalLine);
        tester.assertComponent("horizontalLine", HorizontalLine.class);
    }

}