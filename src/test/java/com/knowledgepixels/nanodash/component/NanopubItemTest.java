package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.NPX;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NanopubItemTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    /**
     * The nanopub is made trusty because the header renders its artifact code when it has no
     * label, which a plain nanopub does not have.
     */
    private Nanopub nanopub(String uri, boolean isProtected) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator(uri);
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        if (isProtected) creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        return MakeTrustyNanopub.transform(creator.finalizeNanopub());
    }

    private NanopubItem minimalItem(Nanopub np) {
        return new NanopubItem("item", NanopubElement.get(np)).setMinimal().setFooterHidden(true);
    }

    @Test
    void protectedNanopubShowsTheFlag() throws Exception {
        tester.startComponentInPage(minimalItem(nanopub("http://example.org/protected-item/", true)));

        tester.assertComponent("item:header:protected-flag", Label.class);
        tester.assertVisible("item:header:protected-flag");
        tester.assertLabel("item:header:protected-flag", "🔒 protected");
    }

    @Test
    void plainNanopubShowsNoFlag() throws Exception {
        tester.startComponentInPage(minimalItem(nanopub("http://example.org/plain-item/", false)));

        tester.assertInvisible("item:header:protected-flag");
    }

    @Test
    void protectedTypeIsNotAlsoShownAsATypeTag() throws Exception {
        // The type tags come from Utils.getTypes, which leaves npx:ProtectedNanopub out so that it
        // is not shown twice, once as a flag and once as an ordinary grey type tag linking to a
        // listing of everything else that happens to be protected.
        tester.startComponentInPage(minimalItem(nanopub("http://example.org/protected-item-tags/", true)));

        assertFalse(tester.getLastResponseAsString().contains("ProtectedNanopub"));
    }

}
