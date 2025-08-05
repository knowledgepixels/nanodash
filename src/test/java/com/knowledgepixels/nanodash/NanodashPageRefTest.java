package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NanodashPageRefTest {

    @Test
    void constructorWithLabelOnlySetsLabel() {
        NanodashPageRef ref = new NanodashPageRef("Only Label");
        assertEquals("Only Label", ref.getLabel());
        assertNull(ref.getPageClass());
        assertNull(ref.getParameters());
    }

    @Test
    void constructorWithPageClassParametersAndLabelInitializesFieldsCorrectly() {
        PageParameters params = new PageParameters();
        params.add("key", "value");
        NanodashPageRef ref = new NanodashPageRef(NanodashPage.class, params, "Test Label");
        assertEquals(NanodashPage.class, ref.getPageClass());
        assertEquals(params, ref.getParameters());
        assertEquals("Test Label", ref.getLabel());
    }

    @Test
    void constructorWithPageClassAndLabelInitializesFieldsCorrectly() {
        NanodashPageRef ref = new NanodashPageRef(NanodashPage.class, "Test Label");
        assertEquals(NanodashPage.class, ref.getPageClass());
        assertNull(ref.getParameters());
        assertEquals("Test Label", ref.getLabel());
    }

    @Test
    void constructorWithLabelOnlyInitializesFieldsCorrectly() {
        NanodashPageRef ref = new NanodashPageRef("Test Label");
        assertNull(ref.getPageClass());
        assertNull(ref.getParameters());
        assertEquals("Test Label", ref.getLabel());
    }

    @Test
    void createComponentReturnsExternalLinkWhenPageClassIsNull() {
        NanodashPageRef ref = new NanodashPageRef("External Link");
        WicketTester tester = new WicketTester();
        WebMarkupContainer component = ref.createComponent("link");
        assertInstanceOf(ExternalLink.class, component);
        assertEquals("#", component.getDefaultModelObjectAsString());
    }

    @Test
    void createComponentReturnsBookmarkablePageLinkWhenPageClassIsNotNull() {
        WicketTester tester = new WicketTester();
        PageParameters params = new PageParameters();
        params.add("key", "value");
        NanodashPageRef ref = new NanodashPageRef(NanodashPage.class, params, "Bookmarkable Link");
        WebMarkupContainer component = ref.createComponent("link");
        assertInstanceOf(BookmarkablePageLink.class, component);
        assertEquals(NanodashPage.class, ((BookmarkablePageLink<?>) component).getPageClass());
        assertEquals(params, ((BookmarkablePageLink<?>) component).getPageParameters());
    }

    @Test
    void getLabelReturnsCorrectLabel() {
        NanodashPageRef ref = new NanodashPageRef("Sample Label");
        assertEquals("Sample Label", ref.getLabel());
    }

    @Test
    void getLabelReturnsNullWhenLabelIsNotSet() {
        NanodashPageRef ref = new NanodashPageRef(null);
        assertNull(ref.getLabel());
    }

    @Test
    void getParametersReturnsCorrectParameters() {
        PageParameters params = new PageParameters();
        params.add("key", "value");
        NanodashPageRef ref = new NanodashPageRef(NanodashPage.class, params, "Test Label");
        assertEquals(params, ref.getParameters());
    }

    @Test
    void getParametersReturnsNullWhenParametersAreNotSet() {
        NanodashPageRef ref = new NanodashPageRef(NanodashPage.class, "Test Label");
        assertNull(ref.getParameters());
    }

    @Test
    void getPageClassReturnsCorrectPageClass() {
        NanodashPageRef ref = new NanodashPageRef(NanodashPage.class, "Test Label");
        assertEquals(NanodashPage.class, ref.getPageClass());
    }

    @Test
    void getPageClassReturnsNullWhenPageClassIsNotSet() {
        NanodashPageRef ref = new NanodashPageRef("Test Label");
        assertNull(ref.getPageClass());
    }

}