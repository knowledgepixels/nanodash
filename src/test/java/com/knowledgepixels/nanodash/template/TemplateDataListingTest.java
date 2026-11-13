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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;

/**
 * How {@link TemplateData} copes with template listings the query service doesn't answer
 * (issue #916): no exception, the previous listing kept, and the missing listing asked for again.
 */
class TemplateDataListingTest {

    private static final String TEMPLATE_NP = "https://w3id.org/np/RAexampleTemplate";

    private MockedStatic<ApiCache> apiCache;

    @BeforeEach
    void setUp() {
        TemplateData.forget();
        apiCache = mockStatic(ApiCache.class);
    }

    @AfterEach
    void tearDown() {
        apiCache.close();
        TemplateData.forget();
    }

    private void answerAssertionListingWith(ApiResponse response) {
        apiCache.when(() -> ApiCache.retrieveResponseSync(
                        argThat((QueryRef ref) -> ref != null && QueryApiAccess.GET_ASSERTION_TEMPLATES.equals(ref.getQueryId())),
                        anyBoolean()))
                .thenReturn(response);
    }

    private static ApiResponse listingOfOneTemplate() {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("np", TEMPLATE_NP);
        entry.add("label", "Example template");
        ApiResponse response = new ApiResponse();
        response.getData().add(entry);
        return response;
    }

    private static List<String> assertionTemplateIds() {
        return TemplateData.get().getAssertionTemplates().stream().map(entry -> entry.get("np")).toList();
    }

    /**
     * With nothing from the query service, the listings are empty instead of the load failing.
     */
    @Test
    void noAnswerGivesEmptyListings() {
        TemplateData templateData = assertDoesNotThrow(TemplateData::get);

        assertTrue(templateData.getAssertionTemplates().isEmpty());
        assertTrue(templateData.getProvenanceTemplates().isEmpty());
        assertTrue(templateData.getPubInfoTemplates().isEmpty());
    }

    /**
     * A listing that got no answer is asked for again on a later call, on the same instance.
     */
    @Test
    void missingListingIsLoadedOnALaterCall() {
        TemplateData first = TemplateData.get();
        assertTrue(first.getAssertionTemplates().isEmpty());

        answerAssertionListingWith(listingOfOneTemplate());

        assertSame(first, TemplateData.get());
        assertEquals(List.of(TEMPLATE_NP), assertionTemplateIds());
    }

    /**
     * A refresh the query service doesn't answer keeps the listing loaded before.
     */
    @Test
    void refreshWithoutAnswerKeepsThePreviousListing() {
        answerAssertionListingWith(listingOfOneTemplate());
        assertEquals(List.of(TEMPLATE_NP), assertionTemplateIds());

        answerAssertionListingWith(null);
        TemplateData.refreshTemplates();

        assertEquals(List.of(TEMPLATE_NP), assertionTemplateIds());
    }

}
