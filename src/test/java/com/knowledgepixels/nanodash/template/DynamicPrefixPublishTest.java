package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.DynamicPrefix;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Publish-path tests for space-/namespace-dependent {@code nt:hasPrefix} values
 * (issue #571, docs/space-namespace-prefixes.md): the prefix is resolved against the
 * navigation context, or against the base the user picked when the context determines
 * none; an unresolved prefix never mints a wrongly-namespaced IRI.
 */
public class DynamicPrefixPublishTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI RESOURCE = vf.createIRI(NP_URI + "/resource");
    private static final IRI SUBJECT = vf.createIRI("http://example.com/subject");
    private static final IRI HAS_PART = vf.createIRI("http://example.com/hasPart");

    private static final String SPACE_IRI = "https://w3id.org/space/foo/bar";

    private MockedStatic<TemplateData> templateDataMockedStatic;
    private MockedStatic<NavigationContext> navigationContextMockedStatic;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
        // Only resolve() is stubbed; the rest of NavigationContext stays real, as the
        // rendered items use its link-fallback behaviors.
        navigationContextMockedStatic = mockStatic(NavigationContext.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
        navigationContextMockedStatic.close();
    }

    /**
     * Builds a template with one optional statement (subject ex:hasPart [resource]) whose
     * object is a URI placeholder carrying the given prefix, and returns an initialized
     * context for it, set to the given navigation context.
     */
    private TemplateContext contextWith(String prefix, String navigationContextId) throws Exception {
        return contextWith(prefix, navigationContextId, false);
    }

    /**
     * As above, but optionally declaring the placeholder a local + introduced resource, the
     * shape real "define a new X in this space" templates use.
     */
    private TemplateContext contextWith(String prefix, String navigationContextId, boolean localResource) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Dynamic prefix test template"));
        // Constant statement st0 keeps the assertion graph non-empty when the optional
        // statement st1 is dropped:
        IRI st0 = vf.createIRI(NP_URI + "/st0");
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st0);
        creator.addAssertionStatement(st0, RDF.SUBJECT, SUBJECT);
        creator.addAssertionStatement(st0, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(st0, RDF.OBJECT, vf.createIRI("http://example.com/SomeClass"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, SUBJECT);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, HAS_PART);
        creator.addAssertionStatement(ST1, RDF.OBJECT, RESOURCE);
        creator.addAssertionStatement(RESOURCE, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        if (localResource) {
            creator.addAssertionStatement(RESOURCE, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
            creator.addAssertionStatement(RESOURCE, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        }
        creator.addAssertionStatement(RESOURCE, NTEMPLATE.HAS_PREFIX, vf.createLiteral(prefix));
        creator.addAssertionStatement(RESOURCE, RDFS.LABEL, vf.createLiteral("resource"));
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.setNavigationContextId(navigationContextId);
        context.initStatements();
        return context;
    }

    /**
     * Makes the given context id resolve to a space with {@link #SPACE_IRI}.
     */
    private void withSpaceContext(String contextId) {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(SPACE_IRI);
        when(space.getSpace()).thenReturn(space);
        navigationContextMockedStatic.when(() -> NavigationContext.resolve(contextId)).thenReturn(space);
    }

    /**
     * Makes the given context id resolve to a maintained resource of {@link #SPACE_IRI}
     * with the given (possibly absent) declared namespace.
     */
    private void withResourceContext(String contextId, String resourceIri, String namespace) {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(SPACE_IRI);
        MaintainedResource resource = mock(MaintainedResource.class);
        when(resource.getId()).thenReturn(resourceIri);
        when(resource.getNamespace()).thenReturn(namespace);
        when(resource.getSpace()).thenReturn(space);
        navigationContextMockedStatic.when(() -> NavigationContext.resolve(contextId)).thenReturn(resource);
    }

    @SuppressWarnings("unchecked")
    private void setText(TemplateContext context, String value) {
        ((IModel<Object>) context.getComponentModels().get(RESOURCE)).setObject(value);
    }

    private void pickPrefixBase(TemplateContext context, String rawPrefix, String base) {
        context.getComponentModels().put(TemplateContext.getPrefixModelKey(DynamicPrefix.getToken(rawPrefix)), Model.of(base));
    }

    private Nanopub publish(TemplateContext context) throws Exception {
        NanopubCreator creator = new NanopubCreator("http://purl.org/nanopub/temp/result/");
        context.propagateStatements(creator);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    private static Value partOf(Nanopub np) {
        for (Statement st : np.getAssertion()) {
            if (st.getPredicate().equals(HAS_PART)) return st.getObject();
        }
        return null;
    }

    @Test
    void spaceTokenResolvesFromNavigationContext() throws Exception {
        withSpaceContext(SPACE_IRI);
        TemplateContext context = contextWith("~~SPACE~~/r/", SPACE_IRI);
        assertEquals(SPACE_IRI + "/r/", context.getPrefix(RESOURCE));
        setText(context, "thing");
        assertEquals(vf.createIRI(SPACE_IRI + "/r/thing"), partOf(publish(context)));
    }

    @Test
    void spaceTokenResolvesToTheSpaceOfAMaintainedResourceContext() throws Exception {
        String resourceIri = SPACE_IRI + "/collection";
        withResourceContext(resourceIri, resourceIri, null);
        TemplateContext context = contextWith("~~SPACE~~/", resourceIri);
        setText(context, "thing");
        assertEquals(vf.createIRI(SPACE_IRI + "/thing"), partOf(publish(context)));
    }

    @Test
    void namespaceTokenResolvesFromMaintainedResourceContext() throws Exception {
        String resourceIri = SPACE_IRI + "/collection";
        withResourceContext(resourceIri, resourceIri, SPACE_IRI + "/ns/");
        TemplateContext context = contextWith("~~NAMESPACE~~", resourceIri);
        setText(context, "thing");
        assertEquals(vf.createIRI(SPACE_IRI + "/ns/thing"), partOf(publish(context)));
    }

    @Test
    void namespaceTokenFallsBackToTheResourceIriWhenNoNamespaceIsDeclared() throws Exception {
        String resourceIri = SPACE_IRI + "/collection";
        withResourceContext(resourceIri, resourceIri, null);
        TemplateContext context = contextWith("~~NAMESPACE~~", resourceIri);
        setText(context, "thing");
        assertEquals(vf.createIRI(resourceIri + "/thing"), partOf(publish(context)));
    }

    /**
     * The headline case: a template that introduces a new resource declares it
     * nt:LocalResource + nt:IntroducedResource *and* gives it a dynamic prefix. The prefix
     * must win, otherwise the resource is minted under the nanopublication's own namespace
     * and the declared namespace is silently dropped.
     */
    @Test
    void dynamicPrefixWinsOverLocalResourceMinting() throws Exception {
        String resourceIri = SPACE_IRI + "/r/test-ontology";
        withResourceContext(resourceIri, resourceIri, null);
        TemplateContext context = contextWith("~~NAMESPACE~~", resourceIri, true);
        assertEquals(resourceIri + "/", context.getPrefix(RESOURCE));
        setText(context, "my-class");
        assertEquals(vf.createIRI(resourceIri + "/my-class"), partOf(publish(context)));
    }

    @Test
    void dynamicSpacePrefixWinsOverLocalResourceMinting() throws Exception {
        withSpaceContext(SPACE_IRI);
        TemplateContext context = contextWith("~~SPACE~~/r/", SPACE_IRI, true);
        setText(context, "thing");
        assertEquals(vf.createIRI(SPACE_IRI + "/r/thing"), partOf(publish(context)));
    }

    /**
     * A local resource without a dynamic prefix keeps being minted under the target
     * namespace, exactly as before.
     */
    @Test
    void localResourceWithoutDynamicPrefixIsStillMintedUnderTheTargetNamespace() throws Exception {
        TemplateContext context = contextWith("https://example.org/", null, true);
        setText(context, "thing");
        Value v = partOf(publish(context));
        assertNotNull(v);
        assertTrue(v.stringValue().startsWith(Template.DEFAULT_TARGET_NAMESPACE),
                "expected minting under the target namespace but got " + v);
    }

    @Test
    void namespaceTokenIsUnresolvedUnderASpaceContext() throws Exception {
        withSpaceContext(SPACE_IRI);
        TemplateContext context = contextWith("~~NAMESPACE~~", SPACE_IRI);
        assertNull(context.getPrefix(RESOURCE));
        assertTrue(context.hasUnresolvedPrefix(RESOURCE));
    }

    @Test
    void withoutNavigationContextThePickedBaseIsUsed() throws Exception {
        TemplateContext context = contextWith("~~SPACE~~/r/", null);
        assertTrue(context.hasUnresolvedPrefix(RESOURCE));
        pickPrefixBase(context, "~~SPACE~~/r/", "https://w3id.org/space/other");
        assertEquals("https://w3id.org/space/other/r/", context.getPrefix(RESOURCE));
        setText(context, "thing");
        assertEquals(vf.createIRI("https://w3id.org/space/other/r/thing"), partOf(publish(context)));
    }

    @Test
    void unresolvedPrefixPublishesNothing() throws Exception {
        TemplateContext context = contextWith("~~SPACE~~/r/", null);
        setText(context, "thing");
        assertNull(partOf(publish(context)), "an unresolved dynamic prefix must not mint an IRI");
    }

    @Test
    void aFullUriIsPublishedEvenWithAnUnresolvedPrefix() throws Exception {
        TemplateContext context = contextWith("~~SPACE~~/r/", null);
        setText(context, "https://example.org/elsewhere");
        assertEquals(vf.createIRI("https://example.org/elsewhere"), partOf(publish(context)));
    }

    @Test
    void staticPrefixIsUnchanged() throws Exception {
        TemplateContext context = contextWith("https://example.org/", null);
        assertEquals("https://example.org/", context.getPrefix(RESOURCE));
        setText(context, "thing");
        assertEquals(vf.createIRI("https://example.org/thing"), partOf(publish(context)));
    }

}
