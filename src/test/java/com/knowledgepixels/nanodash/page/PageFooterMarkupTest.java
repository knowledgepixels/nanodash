package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
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
 * production: no test rendered a page with its own markup. The connector pages went the
 * same way, unnoticed for three weeks, because the check below only looked in the
 * {@code page} package.
 */
class PageFooterMarkupTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    private static final Path APP_DIR = SOURCE_ROOT.resolve("com/knowledgepixels/nanodash");

    /**
     * The page classes, in any package, whose markup is a complete page of its own rather
     * than an extension of {@link NanodashPage}'s. Pages that inherit such markup without
     * markup of their own (like the journals' connector pages) are covered by the class
     * whose markup they inherit. Looking only in the {@code page} package missed the
     * connector pages, which then all failed in production.
     */
    private static List<Class<? extends NanodashPage>> pagesWithOwnMarkup() {
        try (Stream<Path> files = Files.walk(APP_DIR)) {
            List<Class<? extends NanodashPage>> pages = new ArrayList<>();
            for (Path f : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                String className = SOURCE_ROOT.relativize(f).toString()
                        .replace(".html", "").replace(File.separatorChar, '.');
                Class<?> cls;
                try {
                    // Loaded without being initialized: no static setup is run.
                    cls = Class.forName(className, false, PageFooterMarkupTest.class.getClassLoader());
                } catch (ClassNotFoundException ex) {
                    continue;
                }
                // Only pages built on NanodashPage get a footer from it; the ones extending
                // WebPage directly were never part of this, and panels are not pages.
                if (cls == NanodashPage.class || !NanodashPage.class.isAssignableFrom(cls)) continue;
                if (read(f).contains("wicket:extend")) continue;
                pages.add(cls.asSubclass(NanodashPage.class));
            }
            return pages;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // Whether the page, or a class between it and NanodashPage, overrides hasPageFooter():
    // the overrides there are all to say that the page has none.
    private static boolean optsOutOfFooter(Class<?> page) {
        for (Class<?> c = page; c != NanodashPage.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals("hasPageFooter") && m.getParameterCount() == 0) return true;
            }
        }
        return false;
    }

    @Test
    void theConnectorPagesAreAmongThoseChecked() {
        assertTrue(pagesWithOwnMarkup().contains(GenOverviewPage.class),
                "the check has to see pages outside the page package too");
    }

    /**
     * The pairing that matters: a page with markup of its own either has the footer tags in
     * that markup, or says it has no footer. Either is fine; disagreeing is the bug. This is
     * a static check so that it covers pages no test happens to render.
     */
    @Test
    void everyStandalonePageAgreesWithItsOwnMarkup() {
        for (Class<? extends NanodashPage> page : pagesWithOwnMarkup()) {
            Path markupFile = SOURCE_ROOT.resolve(page.getName().replace('.', File.separatorChar) + ".html");
            boolean markupHasFooter = read(markupFile).contains("wicket:id=\"privacy-link\"");
            boolean optedOut = optsOutOfFooter(page);
            assertTrue(markupHasFooter != optedOut,
                    page.getSimpleName() + ".html brings its own markup, so it has to agree with "
                    + page.getSimpleName() + " about the footer: either put the footer tags in the "
                    + "markup (see QueryPage.html), or override hasPageFooter() to return false. "
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

    // The same for the journals' connector pages, all of which failed in production from
    // 5.19.0 (15 October 2026) on: the entry page /connector/pensoft/bdj forwards here.
    @Test
    void aConnectorPageRendersAndCarriesTheFooter() {
        tester.startPage(GenOverviewPage.class, new PageParameters().set("journal", "pensoft/bdj"));
        tester.assertRenderedPage(GenOverviewPage.class);
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
