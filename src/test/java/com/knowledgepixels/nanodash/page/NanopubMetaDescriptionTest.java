package com.knowledgepixels.nanodash.page;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteCategory;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the description a nanopublication's landing page gives search engines and link
 * previews (issue #168), on nanopublications from the nanopub test suite.
 */
class NanopubMetaDescriptionTest {

    private static final String ARTICLE_WITH_ABSTRACT = "RAnLA9VQ9VRJiPGq5_C4xlszHbVu-t-pJCo3YDnsq51q8";

    private static final String ARTICLE_DOI = "https://doi.org/10.3233/DS-170001";

    private static final String ABSTRACT_START = "Computational manipulation of knowledge is an important";

    private static final String FDO_WITHOUT_PROSE = "RAR7wdfw9trX-4V5LnHGuXPXrNGTn4qFZKGs3MO_cwIHw";

    private static final String RESOURCE_WITH_COMMENT = "RACVkJsZq2pP7c6DE6qCbOQCRA8IOfohLUzi1h2GDrZU8";

    private static final String COMMENT_ON_ASSERTION_GRAPH = "RAnugcEH6rk4xftP3YUhhXL7FUJqCFGmxOTFGZxVjmYOQ";

    private static final Function<IRI, String> NAMES = creator -> "Tobias Kuhn";

    private static final Function<IRI, String> NO_NAMES = creator -> null;

    private static Nanopub fromTestSuite(String artifactCode) {
        try {
            return new NanopubImpl(NanopubTestSuite.getLatest()
                    .getByArtifactCode(artifactCode, TestSuiteCategory.VALID)
                    .orElseThrow(() -> new IllegalStateException("Not in the nanopub test suite: " + artifactCode))
                    .toFile());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read " + artifactCode + " from the nanopub test suite", ex);
        }
    }

    private static String uriOf(Nanopub np) {
        return np.getUri().stringValue();
    }

    /**
     * A page about a whole nanopublication describes it by what the nanopublication says
     * about the resource it introduces.
     */
    @Test
    void aNanopubIsDescribedByWhatItIntroduces() {
        Nanopub article = fromTestSuite(ARTICLE_WITH_ABSTRACT);
        String description = NanopubMetaDescription.describe(article, uriOf(article), NAMES);
        assertTrue(description.startsWith(ABSTRACT_START), description);
    }

    /**
     * A page about a resource minted in a nanopublication describes that resource.
     */
    @Test
    void anIntroducedResourceIsDescribedByItsOwnText() {
        Nanopub article = fromTestSuite(ARTICLE_WITH_ABSTRACT);
        String description = NanopubMetaDescription.describe(article, ARTICLE_DOI, NAMES);
        assertTrue(description.startsWith(ABSTRACT_START), description);
    }

    /**
     * A page about a whole nanopublication looks at the nanopublication before what it introduces.
     */
    @Test
    void theNanopubItselfComesBeforeWhatItIntroduces() {
        Nanopub article = fromTestSuite(ARTICLE_WITH_ABSTRACT);
        assertEquals(List.of(uriOf(article), ARTICLE_DOI), NanopubMetaDescription.describedSubjects(article, uriOf(article)));
    }

    /**
     * A page about an introduced resource does not fall back on the rest of the nanopublication.
     */
    @Test
    void anIntroducedResourceLooksOnlyAtItself() {
        Nanopub article = fromTestSuite(ARTICLE_WITH_ABSTRACT);
        assertEquals(List.of(ARTICLE_DOI), NanopubMetaDescription.describedSubjects(article, ARTICLE_DOI));
    }

    /**
     * A comment describes its subject when nothing more specific does.
     */
    @Test
    void aCommentDescribesWhenNothingElseDoes() {
        Nanopub resource = fromTestSuite(RESOURCE_WITH_COMMENT);
        assertEquals("Darwin Core schema", NanopubMetaDescription.describe(resource, uriOf(resource), NAMES));
    }

    /**
     * Text about something the page is not about, here the assertion graph, does not stand
     * for the page.
     */
    @Test
    void theDescriptionOfAnotherResourceIsNotUsed() {
        Nanopub np = fromTestSuite(COMMENT_ON_ASSERTION_GRAPH);
        assertNull(NanopubMetaDescription.findOwnDescription(np, NanopubMetaDescription.describedSubjects(np, uriOf(np))));
    }

    /**
     * A nanopublication that describes nothing in prose is summarized by its label, its
     * creators and its publication date.
     */
    @Test
    void aNanopubWithoutProseIsSummarized() {
        Nanopub fdo = fromTestSuite(FDO_WITHOUT_PROSE);
        assertEquals("FAIR Digital Object: ABC demo table: a nanopublication by Tobias Kuhn, published on 2024-09-18.",
                NanopubMetaDescription.describe(fdo, uriOf(fdo), NAMES));
    }

    /**
     * A creator whose name is unknown is named by their IRI.
     */
    @Test
    void aCreatorWithoutNameIsNamedByTheirIri() {
        Nanopub fdo = fromTestSuite(FDO_WITHOUT_PROSE);
        assertEquals("FAIR Digital Object: ABC demo table: a nanopublication by https://orcid.org/0000-0002-1267-0234, published on 2024-09-18.",
                NanopubMetaDescription.summarize(fdo, NO_NAMES));
    }

}
