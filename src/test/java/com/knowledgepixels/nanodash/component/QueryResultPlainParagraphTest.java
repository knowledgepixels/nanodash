package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A paragraph whose row names its own IRI (a {@code paragraph} column) links to its
 * part page from the heading and from the row menu; the body stays prose (issue #701).
 */
class QueryResultPlainParagraphTest {

    private static final String QUERY_ID = "RAqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq/get-paragraphs";
    private static final String CONTEXT = "https://w3id.org/spaces/example/r/docs";
    private static final String PARAGRAPH = "https://w3id.org/np/RApppppppppppppppppppppppppppppppppppppppppp/paragraph";

    private WicketTester tester;
    private MockedStatic<GrlcQuery> grlc;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        GrlcQuery query = mock(GrlcQuery.class);
        when(query.getLabel()).thenReturn("Paragraphs");
        grlc = mockStatic(GrlcQuery.class);
        grlc.when(() -> GrlcQuery.get(any(QueryRef.class))).thenReturn(query);
    }

    @AfterEach
    void tearDown() {
        grlc.close();
        tester.destroy();
    }

    private static ApiResponse rows(boolean withParagraphColumn) {
        ApiResponse response = new ApiResponse();
        response.setHeader(withParagraphColumn
                ? new String[]{"paragraph", "title", "content"}
                : new String[]{"title", "content"});
        ApiResponseEntry row = new ApiResponseEntry();
        if (withParagraphColumn) row.add("paragraph", PARAGRAPH);
        row.add("title", "Placeholder structure");
        row.add("content", "<p>Some prose about placeholders.</p>");
        response.add(row);
        return response;
    }

    private String render(ApiResponse response, String contextId) {
        QueryResultPlainParagraph component = new QueryResultPlainParagraph("r", new QueryRef(QUERY_ID), response, new ViewDisplay(10));
        component.setContextId(contextId);
        tester.startComponentInPage(component);
        return tester.getLastResponseAsString();
    }

    @Test
    void headingLinksToThePartPageUnderTheContext() {
        String html = render(rows(true), CONTEXT);
        assertTrue(html.contains("class=\"paragraph-title-link\""), html);
        assertTrue(html.contains("/part?id=" + PARAGRAPH + "&amp;context=" + CONTEXT), html);
        assertTrue(html.contains("label=Placeholder+structure"), html);
        assertTrue(html.contains(">Placeholder structure</span></a></h5>"), html);
    }

    @Test
    void rowMenuOffersAnOpenEntry() {
        String html = render(rows(true), CONTEXT);
        assertTrue(html.contains("</span>open</a>"), html);
    }

    @Test
    void bodyIsNotALink() {
        String html = render(rows(true), CONTEXT);
        assertTrue(html.contains("<div class=\"paragraph-content\"><p>Some prose about placeholders.</p></div>"), html);
    }

    @Test
    void withoutAParagraphColumnTheHeadingStaysPlain() {
        String html = render(rows(false), CONTEXT);
        assertFalse(html.contains("paragraph-title-link"), html);
        assertFalse(html.contains("</span>open</a>"), html);
        assertTrue(html.contains(">Placeholder structure</span></h5>"), html);
    }

    @Test
    void withoutANavigationContextThereIsNoPartPageToLinkTo() {
        String html = render(rows(true), null);
        assertFalse(html.contains("paragraph-title-link"), html);
        assertFalse(html.contains("</span>open</a>"), html);
        assertTrue(html.contains(">Placeholder structure</span></h5>"), html);
    }

}
