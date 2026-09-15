package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code cite-as} link of RFC 8574 says which IRI the page being read is to be cited as,
 * so it belongs on a resource's own page and nowhere else: a page that merely links to
 * nanopublications is not announcing itself as one of them (issues #633, #716).
 */
class CiteAsLinkTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    private String render(Class<? extends NanodashPage> pageClass) {
        tester.startPage(pageClass);
        return tester.getLastResponse().getDocument();
    }

    @Test
    void aResourcesOwnPageSaysWhatToCiteItAs() {
        String document = render(CiteAsSubjectPage.class);
        assertTrue(document.contains("rel=\"cite-as\""), document);
        assertTrue(document.contains("href=\"" + CiteAsSubjectPage.SUBJECT_IRI + "\""), document);
    }

    // Without the vocabulary in scope an RDFa parser has no IRI to resolve the relation to,
    // and dokieli -- the reason the link is here at all -- reads it through one.
    @Test
    void theRelationIsResolvableByAnRdfaParser() {
        String document = render(CiteAsSubjectPage.class);
        assertTrue(document.contains("vocab=\"https://www.w3.org/ns/iana/link-relations/relation#\""), document);
    }

    // The link carries the relation and nothing else. Every token of a rel resolves against
    // the vocabulary in scope, so a noopener alongside it would assert a link relation of its
    // own about the page -- which is why this is a link of its own rather than an attribute on
    // the visible IRI link, where noopener and noreferrer already sit.
    @Test
    void theCiteAsLinkCarriesNoOtherRelation() {
        String document = render(CiteAsSubjectPage.class);
        int relIndex = document.indexOf("rel=\"cite-as\"");
        assertTrue(relIndex > -1, document);
        int lineStart = document.lastIndexOf("<", relIndex);
        int lineEnd = document.indexOf(">", relIndex);
        String tag = document.substring(lineStart, lineEnd + 1);
        assertFalse(tag.contains("noopener"), tag);
        assertFalse(tag.contains("noreferrer"), tag);
    }

    // A page with no subject of its own claims nothing, which is the case every listing and
    // every page that merely links to a nanopublication falls into.
    @Test
    void aPageThatIsNotAResourcesOwnPageClaimsNothing() {
        String document = render(ErrorPage.class);
        assertFalse(document.contains("cite-as"), document);
    }

    @Test
    void exactlyOneCiteAsIsClaimed() {
        String document = render(CiteAsSubjectPage.class);
        assertEquals(1, document.split("rel=\"cite-as\"", -1).length - 1, document);
    }

}
