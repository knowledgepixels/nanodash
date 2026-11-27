package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.templates.FillRequest;
import org.nanopub.templates.NanopubResolver;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TemplateFillGuard}: how it compares, and that it never gets in the way. That
 * the form and the library make the same nanopublication is tested in
 * {@code PublishFormFillGuardTest}.
 */
class TemplateFillGuardTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String TEMPLATE = "https://w3id.org/np/RAFillGuardFillGuardFillGuardFillGuardFillGua";
    private static final IRI ME = vf.createIRI("https://orcid.org/0000-0000-0000-0001");

    private static Nanopub nanopub(String uri, String name, Date created) throws Exception {
        NanopubCreator creator = new NanopubCreator(uri);
        creator.addAssertionStatement(vf.createIRI(uri + "apple"), RDFS.LABEL, vf.createLiteral(name));
        creator.addProvenanceStatement(vf.createIRI("http://www.w3.org/ns/prov#wasAttributedTo"), ME);
        creator.addPubinfoStatement(DCTERMS.CREATOR, ME);
        creator.addPubinfoStatement(vf.createIRI(uri + "apple"), RDFS.SEEALSO, vf.createIRI(uri));
        creator.addTimestamp(created);
        return creator.finalizeNanopub();
    }

    @Test
    void triplesAreWrittenRelativeToTheNanopublication() throws Exception {
        Set<String> form = TemplateFillGuard.triples(nanopub("https://w3id.org/np/~~~ARTIFACTCODE~~~/", "Apple", new Date(0)));
        Set<String> library = TemplateFillGuard.triples(nanopub("http://purl.org/nanopub/temp/abc/", "Apple", new Date()));
        // Another URI and another creation time make no difference.
        assertEquals(form, library);
        assertTrue(form.contains("assertion: sub:apple <" + RDFS.LABEL + "> \"Apple\""), form.toString());
        assertTrue(form.contains("pubinfo: this: <" + DCTERMS.CREATOR + "> <" + ME + ">"), form.toString());
        assertTrue(form.stream().noneMatch(t -> t.contains(DCTERMS.CREATED.stringValue())), form.toString());

        Set<String> other = TemplateFillGuard.triples(nanopub("https://w3id.org/np/~~~ARTIFACTCODE~~~/", "Pear", new Date(0)));
        assertNotEquals(form, other);
    }

    @Test
    void whatTheLibraryCannotFillIsReported() throws Exception {
        NanopubResolver nothing = id -> null;
        TemplateFillGuard.Comparison comparison = TemplateFillGuard.check(nanopub("https://w3id.org/np/~~~ARTIFACTCODE~~~/", "Apple", new Date(0)),
                () -> new FillRequest(TEMPLATE).set("name", "Apple").setCreator(ME), nothing);
        assertNotNull(comparison);
        assertFalse(comparison.isSame());
        assertFalse(comparison.problems().isEmpty());
        assertTrue(comparison.problems().get(0).contains(TEMPLATE), comparison.problems().toString());
    }

    @Test
    void failureOfTheComparisonIsSwallowed() throws Exception {
        Nanopub np = nanopub("https://w3id.org/np/~~~ARTIFACTCODE~~~/", "Apple", new Date(0));
        assertNull(TemplateFillGuard.check(np, () -> {
            throw new IllegalStateException("no session");
        }, id -> null));
        NanopubResolver failing = id -> {
            throw new IllegalStateException("no network");
        };
        assertDoesNotThrow(() -> TemplateFillGuard.check(np, () -> new FillRequest(TEMPLATE), failing));
        assertDoesNotThrow(() -> TemplateFillGuard.check(null, () -> new FillRequest(TEMPLATE).setCreator(ME), id -> null));
    }

}
