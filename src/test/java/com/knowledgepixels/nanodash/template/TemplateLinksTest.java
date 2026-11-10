package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests that the provenance and pubinfo template links of a nanopublication are only taken when
 * they lead to a template of that kind (issue #908). Nanopublications made outside Nanodash have
 * been seen to name, for instance, the assertion template for making provenance templates as
 * their provenance template, which made the publish form show that whole template as provenance.
 */
public class TemplateLinksTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String BASE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Link0";
    private static final String PROVENANCE_TEMPLATE = BASE + "1";
    private static final String PUBINFO_TEMPLATE = BASE + "2";
    private static final String ASSERTION_TEMPLATE = BASE + "3";
    private static final String UNLOADABLE = BASE + "4";
    private static final String SOURCE = BASE + "5";

    private TemplateData td;

    @BeforeEach
    void setUp() throws Exception {
        // A partial mock, so that no template list is fetched from the query services:
        td = mock(TemplateData.class, CALLS_REAL_METHODS);
        doReturn(template(PROVENANCE_TEMPLATE, NTEMPLATE.PROVENANCE_TEMPLATE)).when(td).getTemplate(PROVENANCE_TEMPLATE);
        doReturn(template(PUBINFO_TEMPLATE, NTEMPLATE.PUBINFO_TEMPLATE)).when(td).getTemplate(PUBINFO_TEMPLATE);
        doReturn(template(ASSERTION_TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE)).when(td).getTemplate(ASSERTION_TEMPLATE);
        doReturn(null).when(td).getTemplate(UNLOADABLE);
    }

    private static Template template(String npUri, IRI kind) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI value = vf.createIRI(npUri + "/value");
        creator.addAssertionStatement(templateNode, RDF.TYPE, kind);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Template link test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI(npUri + "/thing"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.SEEALSO);
        creator.addAssertionStatement(st1, RDF.OBJECT, value);
        creator.addAssertionStatement(value, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        return new Template(creator.finalizeNanopub());
    }

    private static Nanopub source(String provenanceTemplate, String... pubinfoTemplates) throws Exception {
        NanopubCreator creator = new NanopubCreator(SOURCE);
        creator.addAssertionStatement(vf.createIRI(SOURCE + "/thing"), RDFS.LABEL, vf.createLiteral("a thing"));
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, vf.createIRI(provenanceTemplate));
        for (String t : pubinfoTemplates) {
            creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE, vf.createIRI(t));
        }
        return creator.finalizeNanopub();
    }

    @Test
    void templatesKnowTheirKind() throws Exception {
        Template pr = td.getTemplate(PROVENANCE_TEMPLATE);
        Template pi = td.getTemplate(PUBINFO_TEMPLATE);
        Template a = td.getTemplate(ASSERTION_TEMPLATE);
        assertTrue(pr.isProvenanceTemplate());
        assertFalse(pr.isPubinfoTemplate());
        assertTrue(pi.isPubinfoTemplate());
        assertFalse(pi.isProvenanceTemplate());
        assertFalse(a.isProvenanceTemplate());
        assertFalse(a.isPubinfoTemplate());
    }

    @Test
    void provenanceLinkToAProvenanceTemplateIsTaken() throws Exception {
        assertEquals(vf.createIRI(PROVENANCE_TEMPLATE), td.getProvenanceTemplateId(source(PROVENANCE_TEMPLATE)));
    }

    @Test
    void provenanceLinkToAnotherKindOfTemplateIsPassedOver() throws Exception {
        assertNull(td.getProvenanceTemplateId(source(ASSERTION_TEMPLATE)));
        assertNull(td.getProvenanceTemplateId(source(PUBINFO_TEMPLATE)));
        assertNull(td.getProvenanceTemplate(source(ASSERTION_TEMPLATE)));
    }

    @Test
    void provenanceLinkToAnUnloadableTemplateIsPassedOver() throws Exception {
        assertNull(td.getProvenanceTemplateId(source(UNLOADABLE)));
    }

    @Test
    void pubinfoLinksKeepOnlyPubinfoTemplates() throws Exception {
        Nanopub np = source(PROVENANCE_TEMPLATE, PUBINFO_TEMPLATE, ASSERTION_TEMPLATE, PROVENANCE_TEMPLATE, UNLOADABLE);
        assertEquals(Set.of(vf.createIRI(PUBINFO_TEMPLATE)), td.getPubinfoTemplateIds(np));
    }

}
