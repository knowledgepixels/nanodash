package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.eclipse.rdf4j.model.util.Values.iri;

/**
 * A nanopublication can name templates that this instance cannot load: ones that were never
 * published to the registry it reads, or that are no templates at all. Showing such a
 * nanopublication used to fail with a server error, because the pubinfo section built a template
 * context per declared template and asked it for its statements without checking that the
 * template had loaded — unlike the assertion and provenance sections, which fall back to their
 * generic templates.
 */
class NanopubItemUnloadableTemplateTest {

    /**
     * A well-formed trusty URI no registry has a nanopublication for. Whether the lookup comes
     * back empty or cannot be made at all, the template stays unloaded, which is the condition
     * under test.
     */
    private static final IRI UNPUBLISHED_TEMPLATE = iri("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_PiNf1");

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    /**
     * The nanopub is made trusty because the header renders its artifact code when it has no
     * label, which a plain nanopub does not have.
     */
    private Nanopub nanopubDeclaring(IRI templatePredicate) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("http://example.org/unloadable-template-item/");
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        creator.addPubinfoStatement(templatePredicate, UNPUBLISHED_TEMPLATE);
        return MakeTrustyNanopub.transform(creator.finalizeNanopub());
    }

    /**
     * Only that the nanopublication is shown at all is asserted: which of its pubinfo triples end
     * up rendered depends on the built-in pubinfo templates being retrievable, and that is not
     * what this test is about.
     */
    private void assertItStillRenders(IRI templatePredicate) throws Exception {
        tester.startComponentInPage(new NanopubItem("item", NanopubElement.get(nanopubDeclaring(templatePredicate))).setFooterHidden(true));

        tester.assertComponent("item:pubinfo", WebMarkupContainer.class);
        tester.assertVisible("item:pubinfo");
    }

    @Test
    void anUnpublishedPubinfoTemplateDoesNotBreakTheNanopub() throws Exception {
        assertItStillRenders(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE);
    }

    @Test
    void anUnpublishedAssertionTemplateDoesNotBreakTheNanopub() throws Exception {
        assertItStillRenders(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE);
    }

    @Test
    void anUnpublishedProvenanceTemplateDoesNotBreakTheNanopub() throws Exception {
        assertItStillRenders(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE);
    }

}
