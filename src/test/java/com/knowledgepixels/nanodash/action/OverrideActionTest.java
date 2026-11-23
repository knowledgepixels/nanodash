package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;

import static com.knowledgepixels.nanodash.action.NanopubAction.getEncodedUri;
import static com.knowledgepixels.nanodash.utils.TestUtils.anyIri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverrideActionTest {

    private static final String NP_URI = TestUtils.NANOPUB_URI;

    /**
     * A nanopublication whose pubinfo states, of the nanopublication itself, each given
     * predicate with the resource of the local name after it, in the nanopublication's own
     * namespace.
     */
    private static Nanopub nanopub(Object... predicatesAndLocalNames) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator(NP_URI);
        creator.addAssertionStatement(TestUtils.vf.createStatement(anyIri, anyIri, anyIri));
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), anyIri, anyIri);
        for (int i = 0; i < predicatesAndLocalNames.length; i += 2) {
            creator.addPubinfoStatement(creator.getNanopubUri(), (IRI) predicatesAndLocalNames[i],
                    TestUtils.vf.createIRI(NP_URI + "/" + predicatesAndLocalNames[i + 1]));
        }
        return creator.finalizeNanopub();
    }

    @Test
    void getLinkLabel() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        assertEquals("edit as overriding nanopublication", new OverrideAction().getLinkLabel(TestUtils.createNanopub()));
    }

    @Test
    void getParamString() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        assertEquals("override=" + getEncodedUri(nanopub), new OverrideAction().getParamString(nanopub));
    }

    @Test
    void isApplicableToOwnAndOthersNanopubs() {
        assertTrue(new OverrideAction().isApplicableToOwnNanopubs());
        assertTrue(new OverrideAction().isApplicableToOthersNanopubs());
    }

    @Test
    void applicableToANanopubThatIntroducesSomething() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // E.g. a space definition, a view, or a statement.
        assertTrue(new OverrideAction().isApplicableTo(nanopub(NPX.INTRODUCES, "statement")));
    }

    @Test
    void applicableToANanopubThatDescribesSomething() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        assertTrue(new OverrideAction().isApplicableTo(nanopub(NPX.DESCRIBES, "thing")));
    }

    @Test
    void notApplicableToANanopubThatIntroducesNothing() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // E.g. a role assignment: overriding it would make the same nanopublication as deriving.
        assertFalse(new OverrideAction().isApplicableTo(TestUtils.createNanopub()));
    }

    @Test
    void notApplicableToANanopubThatOnlyEmbedsSomething() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // E.g. a view display: an override mints its embedded display afresh, as deriving does.
        assertFalse(new OverrideAction().isApplicableTo(nanopub(NPX.EMBEDS, "display")));
    }

    @Test
    void applicableToANanopubThatEmbedsOneThingAndIntroducesAnother() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // E.g. a template: it embeds the template and introduces its kind, which an override keeps.
        assertTrue(new OverrideAction().isApplicableTo(nanopub(NPX.EMBEDS, "template", NPX.INTRODUCES, "templateKind")));
    }

}
