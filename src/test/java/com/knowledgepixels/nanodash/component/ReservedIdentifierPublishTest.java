package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Publishing is refused when a value is minted under one of the names a nanopublication keeps
 * for its own parts (issue #29) — except where the nanopublication being superseded already
 * carried that name, which is published and cannot be taken back. A legacy template, whose
 * template node is its own assertion graph, is republished that way.
 */
class ReservedIdentifierPublishTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String SOURCE_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Src01";
    private static final IRI MINTED_ASSERTION = vf.createIRI("https://w3id.org/np/~~~ARTIFACTCODE~~~/assertion");

    /**
     * A nanopublication that says something about the given IRI of its own.
     */
    private static Nanopub sourceUsing(String localName) throws Exception {
        NanopubCreator creator = new NanopubCreator(SOURCE_URI);
        IRI subject = vf.createIRI(SOURCE_URI + "/" + localName);
        creator.addAssertionStatement(subject, RDFS.LABEL, vf.createLiteral("an old template"));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    private static TemplateContext contextMinting(IRI reserved, FillMode fillMode, Nanopub source) {
        TemplateContext context = mock(TemplateContext.class);
        Set<IRI> reservedIris = new LinkedHashSet<>();
        reservedIris.add(reserved);
        when(context.getReservedIris()).thenReturn(reservedIris);
        when(context.getFillMode()).thenReturn(fillMode);
        when(context.getReferenceNanopub()).thenReturn(source);
        return context;
    }

    @Test
    void aFreshlyMintedPartNameIsRefused() {
        TemplateContext context = contextMinting(MINTED_ASSERTION, FillMode.USE, null);
        assertEquals(MINTED_ASSERTION, PublishForm.findReservedIdentifier(context));
    }

    @Test
    void aNameTheSupersededNanopublicationAlreadyCarriedIsLetThrough() throws Exception {
        TemplateContext context = contextMinting(MINTED_ASSERTION, FillMode.SUPERSEDE, sourceUsing("assertion"));
        assertNull(PublishForm.findReservedIdentifier(context),
                "a new version keeps the shape of the one it supersedes");
    }

    @Test
    void supersedingSomethingElseDoesNotExcuseIt() throws Exception {
        TemplateContext context = contextMinting(MINTED_ASSERTION, FillMode.SUPERSEDE, sourceUsing("thing"));
        assertEquals(MINTED_ASSERTION, PublishForm.findReservedIdentifier(context));
    }

}
