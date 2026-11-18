package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;

/**
 * {@link TemplateData#getKnownTemplateLabel(String)}: the label of a template from the template
 * listings, for links to templates whose label a query did not provide.
 */
class TemplateDataKnownLabelTest {

    private static final String TEMPLATE_NP = "https://w3id.org/np/RAlibtZpJm8HM-OVzw9LZeCJg6FyK8CEtXnqqWq5JBy-Y";

    private MockedStatic<ApiCache> apiCache;
    private TemplateData templateData;

    @BeforeEach
    void setUp() {
        apiCache = mockStatic(ApiCache.class);
        apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean()))
                .thenReturn(new ApiResponse());
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("np", TEMPLATE_NP);
        entry.add("label", "I-ADOPT variable");
        ApiResponse listing = new ApiResponse();
        listing.getData().add(entry);
        apiCache.when(() -> ApiCache.retrieveResponseSync(
                        argThat((QueryRef ref) -> ref != null && QueryApiAccess.GET_ASSERTION_TEMPLATES.equals(ref.getQueryId())),
                        anyBoolean()))
                .thenReturn(listing);
        templateData = new TemplateData();
    }

    @AfterEach
    void tearDown() {
        apiCache.close();
    }

    @Test
    void labelOfListedTemplateByNanopubUri() {
        assertEquals("I-ADOPT variable", templateData.getKnownTemplateLabel(TEMPLATE_NP));
    }

    /**
     * A template with embedded identity is referred to by its template node, e.g. when it was
     * pinned by pasting that IRI rather than choosing the template from the list.
     */
    @Test
    void labelOfListedTemplateByTemplateNodeIri() {
        assertEquals("I-ADOPT variable", templateData.getKnownTemplateLabel(TEMPLATE_NP + "/template"));
    }

    @Test
    void noLabelForTemplateNotKnownHere() {
        assertNull(templateData.getKnownTemplateLabel("https://w3id.org/np/RAAnotherTemplateAAAAAAAAAAAAAAAAAAAAAAAAAAAA/template"));
    }

    @Test
    void noLabelForMissingId() {
        assertNull(templateData.getKnownTemplateLabel(null));
        assertNull(templateData.getKnownTemplateLabel(" "));
    }

}
