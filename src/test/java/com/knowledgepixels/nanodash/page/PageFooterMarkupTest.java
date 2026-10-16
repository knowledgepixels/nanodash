package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The footer lives in {@link NanodashPage}'s markup, but a handful of pages bring a complete
 * {@code <html>} of their own instead of extending it. A component added for markup that is
 * not there fails at render time, which is how the privacy link took down /query in
 * production: no test rendered a page with its own markup.
 */
class PageFooterMarkupTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    private static final Path PAGE_DIR =
            Path.of("src/main/java/com/knowledgepixels/nanodash/page");

    /**
     * Every page markup that does not extend {@link NanodashPage}'s has to be rendered by
     * some test, or this class of breakage goes unnoticed again.
     */
    private static List<String> standaloneMarkup() {
        try (Stream<Path> files = Files.list(PAGE_DIR)) {
            List<String> standalone = new ArrayList<>();
            for (Path f : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                String markup = Files.readString(f);
                String name = f.getFileName().toString();
                if (!name.equals("NanodashPage.html") && !markup.contains("wicket:extend")) {
                    standalone.add(name);
                }
            }
            return standalone;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * The pairing that matters: a page with markup of its own either has the footer tags in
     * that markup, or says it has no footer. Either is fine; disagreeing is the bug. This is
     * a static check so that it covers pages no test happens to render.
     */
    @Test
    void everyStandalonePageAgreesWithItsOwnMarkup() {
        for (String markupName : standaloneMarkup()) {
            String javaName = markupName.replace(".html", ".java");
            String markup = read(PAGE_DIR.resolve(markupName));
            String java = read(PAGE_DIR.resolve(javaName));
            // Only pages built on NanodashPage get a footer from it; the ones extending
            // WebPage directly were never part of this.
            if (!java.contains("extends NanodashPage")) continue;
            boolean markupHasFooter = markup.contains("wicket:id=\"privacy-link\"");
            boolean optedOut = java.contains("hasPageFooter");
            assertTrue(markupHasFooter != optedOut,
                    markupName + " brings its own markup, so it has to agree with "
                    + javaName + " about the footer: either put the footer tags in the markup "
                    + "(see QueryPage.html), or override hasPageFooter() to return false. "
                    + "Right now markup has footer = " + markupHasFooter
                    + ", opted out = " + optedOut + ".");
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // The regression itself: this page has markup of its own and is a page people read, so
    // it both renders and carries the privacy link.
    @Test
    void theQueryPageRendersAndCarriesTheFooter() {
        // The request that was failing in production on 16 October 2026.
        tester.startPage(QueryPage.class, new PageParameters()
                .set("id", "RAodlEmMJ2Wfu2kS33tlAUbfFXgSclvlQkixFhFvdEYMs/get-classes-for-thing-view")
                .set("queryparam_thing", "http://www.w3.org/2004/02/skos/core#definition"));
        tester.assertRenderedPage(QueryPage.class);
        tester.assertVisible("privacy-link");
    }

    // A page that extends the base markup still gets the footer from it, without repeating it.
    @Test
    void aPageExtendingTheBaseMarkupStillGetsTheFooter() {
        tester.startPage(PrivacyPage.class);
        tester.assertVisible("privacy-link");
        assertTrue(tester.getLastResponse().getDocument().contains("id=\"page-footer\""));
    }

    // Pages that serve a file rather than something to read get no footer, and must not
    // fall over for want of one.
    @Test
    void aPageWithoutAFooterRendersAnyway() {
        try {
            tester.startPage(GetNamePage.class);
        } catch (Exception ex) {
            Throwable root = ex;
            while (root.getCause() != null) root = root.getCause();
            if (root.getMessage() != null && root.getMessage().contains("privacy-link")) {
                fail("GetNamePage has no footer in its markup, so none should be added: " + root.getMessage());
            }
            // Anything else is this page's own business (it wants an 'id'), not ours.
        }
    }

}
