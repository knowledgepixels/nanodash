package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import com.knowledgepixels.nanodash.Utils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.templates.ComplianceIssue;
import org.nanopub.templates.ComplianceReport;
import org.nanopub.templates.NanopubResolver;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests for {@link TemplateComplianceGuard}: it reports what nanopub-java finds, and never
 * gets in the way.
 */
class TemplateComplianceGuardTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String ASSERTION_TEMPLATE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String PROVENANCE_TEMPLATE = "https://w3id.org/np/RAProvProvProvProvProvProvProvProvProvProvPro";
    private static final String PUBINFO_TEMPLATE = "https://w3id.org/np/RACreatorCreatorCreatorCreatorCreatorCreator0";
    private static final String NP = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";
    private static final IRI ME = vf.createIRI("https://orcid.org/0000-0000-0000-0001");
    private static final IRI THING = vf.createIRI("http://example.com/thing");

    private final Map<String, Nanopub> templates = new HashMap<>();
    private final NanopubResolver resolver = templates::get;

    private static NanopubCreator templateCreator(String uri, IRI type) throws Exception {
        NanopubCreator creator = new NanopubCreator(uri);
        creator.addAssertionStatement(creator.getAssertionUri(), RDF.TYPE, type);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator;
    }

    private static void addStatement(NanopubCreator creator, IRI subject, IRI predicate, IRI object) throws Exception {
        IRI statement = vf.createIRI(creator.getNanopubUri() + "/st1");
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, statement);
        creator.addAssertionStatement(statement, RDF.SUBJECT, subject);
        creator.addAssertionStatement(statement, RDF.PREDICATE, predicate);
        creator.addAssertionStatement(statement, RDF.OBJECT, object);
    }

    // Templates for "<thing> rdfs:label ?name", "ASSERTION prov:wasAttributedTo CREATOR" and
    // "NANOPUB dct:creator CREATOR".
    private void registerTemplates() throws Exception {
        NanopubCreator assertion = templateCreator(ASSERTION_TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        IRI name = vf.createIRI(ASSERTION_TEMPLATE + "/name");
        assertion.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        addStatement(assertion, THING, RDFS.LABEL, name);
        templates.put(ASSERTION_TEMPLATE, assertion.finalizeNanopub());
        NanopubCreator provenance = templateCreator(PROVENANCE_TEMPLATE, NTEMPLATE.PROVENANCE_TEMPLATE);
        addStatement(provenance, NTEMPLATE.ASSERTION_PLACEHOLDER, PROV.WAS_ATTRIBUTED_TO, NTEMPLATE.CREATOR_PLACEHOLDER);
        templates.put(PROVENANCE_TEMPLATE, provenance.finalizeNanopub());
        NanopubCreator pubinfo = templateCreator(PUBINFO_TEMPLATE, NTEMPLATE.PUBINFO_TEMPLATE);
        addStatement(pubinfo, NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        templates.put(PUBINFO_TEMPLATE, pubinfo.finalizeNanopub());
    }

    // A nanopublication as the form makes it from these templates, with the given assertion.
    private static Nanopub nanopub(IRI predicate) throws Exception {
        return nanopub(predicate, ASSERTION_TEMPLATE);
    }

    // The same, naming its assertion template with the given IRI.
    private static Nanopub nanopub(IRI predicate, String assertionTemplateLink) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP);
        creator.addAssertionStatement(THING, predicate, vf.createLiteral("A thing"));
        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, ME);
        creator.addPubinfoStatement(DCTERMS.CREATOR, ME);
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, vf.createIRI(assertionTemplateLink));
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, vf.createIRI(PROVENANCE_TEMPLATE));
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE, vf.createIRI(PUBINFO_TEMPLATE));
        return creator.finalizeNanopub();
    }

    @Test
    void nanopublicationThatFollowsItsTemplatesHasNoFindings() throws Exception {
        registerTemplates();
        ComplianceReport report = TemplateComplianceGuard.check(nanopub(RDFS.LABEL), resolver);
        assertNotNull(report);
        assertEquals(List.of(), report.getIssues());
    }

    @Test
    void nanopublicationThatDoesNotFollowItsTemplatesIsReported() throws Exception {
        registerTemplates();
        ComplianceReport report = TemplateComplianceGuard.check(nanopub(RDFS.COMMENT), resolver);
        assertNotNull(report);
        assertFalse(report.isCompliant());
        assertEquals(List.of(ComplianceIssue.Kind.MISSING_STATEMENT, ComplianceIssue.Kind.UNMATCHED_TRIPLE),
                report.getErrors().stream().map(ComplianceIssue::kind).toList());
    }

    @Test
    void templateNamedByItsNodeIsRetrievedThroughNanodash() throws Exception {
        registerTemplates();
        // The check runs with Nanodash's resolver, as on the publish form. Utils.getNanopub is
        // stood in for by one that, like the real one, takes only a nanopublication URI or an
        // artifact code.
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.stripToNanopubId(anyString())).thenCallRealMethod();
            utils.when(() -> Utils.getNanopub(anyString())).thenAnswer(call -> {
                String id = call.getArgument(0);
                if (!id.matches("(.*[^A-Za-z0-9_-])?RA[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Not a well-formed trusty URI");
                return templates.get(id);
            });
            // A nanopublication made from a template with an identity of its own names the
            // template by its node:
            ComplianceReport report = TemplateComplianceGuard.check(nanopub(RDFS.LABEL, ASSERTION_TEMPLATE + "/templateVersion"));
            assertNotNull(report);
            assertEquals(List.of(), report.getIssues());
        }
    }

    @Test
    void templateThatCannotBeRetrievedIsAFindingNotAFailure() throws Exception {
        ComplianceReport report = TemplateComplianceGuard.check(nanopub(RDFS.LABEL), resolver);
        assertNotNull(report);
        assertFalse(report.isCompliant());
    }

    @Test
    void failureOfTheCheckIsSwallowed() throws Exception {
        NanopubResolver failing = id -> {
            throw new IllegalStateException("no network");
        };
        // Whatever the check does with this, it must not throw.
        assertDoesNotThrow(() -> TemplateComplianceGuard.check(nanopub(RDFS.LABEL), failing));
        assertNull(TemplateComplianceGuard.check(null, resolver));
    }

}
