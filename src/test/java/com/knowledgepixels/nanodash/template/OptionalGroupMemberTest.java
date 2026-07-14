package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for member-level optionality inside grouped statements
 * (docs/optional-statements-in-groups.md): the member flags survive parsing, an
 * all-optional group is normalized to a group-level optional, and out-of-scope
 * member flags (repeatable, nested group) are ignored.
 */
public class OptionalGroupMemberTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI GROUP = vf.createIRI(NP_URI + "/group");
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI ST2 = vf.createIRI(NP_URI + "/st2");

    /**
     * Builds a template whose assertion is a group with two members: st1 (class
     * assignment) and st2 (label). Callers add optionality/other flags on top.
     */
    private static NanopubCreator groupTemplateCreator() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI thing = vf.createIRI(NP_URI + "/thing");
        IRI name = vf.createIRI(NP_URI + "/name");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Group test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, GROUP);
        // Real templates also link the members directly from the template node (the
        // redundant links the group post-processing removes again):
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST2);
        creator.addAssertionStatement(GROUP, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        creator.addAssertionStatement(GROUP, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(GROUP, NTEMPLATE.HAS_STATEMENT, ST2);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, thing);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(ST1, RDF.OBJECT, vf.createIRI("http://example.com/SomeClass"));
        creator.addAssertionStatement(ST2, RDF.SUBJECT, thing);
        creator.addAssertionStatement(ST2, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST2, RDF.OBJECT, name);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("name"));
        return creator;
    }

    @Test
    void mixedGroupKeepsMemberOptionality() throws Exception {
        NanopubCreator creator = groupTemplateCreator();
        creator.addAssertionStatement(ST2, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isOptionalStatement(GROUP), "group with a required member must not become optional");
        assertFalse(t.isOptionalStatement(ST1));
        assertTrue(t.isOptionalStatement(ST2), "member-level optional flag must survive parsing");
        assertTrue(t.getStatementIris().contains(GROUP));
        assertFalse(t.getStatementIris().contains(ST1), "members must be detached from the top level");
        assertFalse(t.getStatementIris().contains(ST2), "members must be detached from the top level");
    }

    @Test
    void allOptionalGroupNormalizedToOptionalGroup() throws Exception {
        NanopubCreator creator = groupTemplateCreator();
        creator.addAssertionStatement(ST1, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(ST2, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isOptionalStatement(GROUP), "all-optional group must be normalized to a group-level optional");
        assertFalse(t.isOptionalStatement(ST1), "member flags must be stripped by the normalization");
        assertFalse(t.isOptionalStatement(ST2), "member flags must be stripped by the normalization");
    }

    @Test
    void allOptionalMembersInAlreadyOptionalGroupAreStripped() throws Exception {
        NanopubCreator creator = groupTemplateCreator();
        creator.addAssertionStatement(GROUP, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(ST1, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(ST2, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isOptionalStatement(GROUP));
        assertFalse(t.isOptionalStatement(ST1));
        assertFalse(t.isOptionalStatement(ST2));
    }

    @Test
    void optionalGroupWithRequiredMembersIsUnchanged() throws Exception {
        NanopubCreator creator = groupTemplateCreator();
        creator.addAssertionStatement(GROUP, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isOptionalStatement(GROUP));
        assertFalse(t.isOptionalStatement(ST1));
        assertFalse(t.isOptionalStatement(ST2));
    }

    @Test
    void repeatableMemberFlagIsIgnored() throws Exception {
        NanopubCreator creator = groupTemplateCreator();
        creator.addAssertionStatement(ST2, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isRepeatableStatement(ST2), "repeatable flag on a group member is out of scope and must be dropped");
    }

    @Test
    void nestedGroupFlagIsIgnored() throws Exception {
        NanopubCreator creator = groupTemplateCreator();
        creator.addAssertionStatement(ST2, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        creator.addAssertionStatement(ST2, NTEMPLATE.HAS_STATEMENT, ST1);
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isGroupedStatement(ST2), "nested group flag on a group member is out of scope and must be dropped");
    }

    @Test
    void memberlessGroupStillThrows() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Broken template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, GROUP);
        creator.addAssertionStatement(GROUP, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        Nanopub np = creator.finalizeNanopub();
        assertThrows(MalformedTemplateException.class, () -> new Template(np));
    }

}
