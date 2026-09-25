package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.utils.TestUtils;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.knowledgepixels.nanodash.utils.TestUtils.vf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the classes {@link ResourcePartPage} resolves for a part: those its defining
 * nanopublication states, plus {@code gen:IndividualAgent} for an agent, which no
 * nanopublication states.
 */
class ResourcePartPageClassesTest {

    private static final IRI ORCID = vf.createIRI("https://orcid.org/0000-0002-1825-0097");
    private static final IRI BOT = vf.createIRI("https://example.com/np/RAbot/my-bot");
    private static final IRI AIDA = vf.createIRI("http://purl.org/aida/Something+is+the+case.");
    private static final IRI AIDA_SENTENCE = vf.createIRI("http://purl.org/petapico/o/hycl#AIDA-Sentence");
    private static final IRI KEY_DECLARATION = vf.createIRI("https://example.com/np/RAintro/keyDeclaration");
    private static final IRI DECLARED_BY = vf.createIRI("http://purl.org/nanopub/x/declaredBy");

    /** Treats exactly the given ids as agents. */
    private static Predicate<String> agents(IRI... agentIris) {
        Set<String> ids = Arrays.stream(agentIris).map(IRI::stringValue).collect(Collectors.toSet());
        return ids::contains;
    }

    /**
     * Builds a nanopublication whose assertion holds the given statements, each given as
     * subject, predicate and object.
     *
     * @param triples the assertion statements, three values each
     * @return the finalized nanopublication
     */
    private static Nanopub nanopubWith(Object... triples) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        for (int i = 0; i < triples.length; i += 3) {
            IRI subject = (IRI) triples[i];
            IRI predicate = (IRI) triples[i + 1];
            Object object = triples[i + 2];
            if (object instanceof IRI objectIri) {
                creator.addAssertionStatement(vf.createStatement(subject, predicate, objectIri));
            } else {
                creator.addAssertionStatement(vf.createStatement(subject, predicate, vf.createLiteral((String) object)));
            }
        }
        return creator.finalizeNanopub();
    }

    @Test
    void takesTheTypesTheDefinitionStates() throws Exception {
        Nanopub definition = nanopubWith(AIDA, RDF.TYPE, AIDA_SENTENCE);

        Set<IRI> classes = ResourcePartPage.getPartClasses(definition, AIDA.stringValue(), agents());

        assertEquals(Set.of(AIDA_SENTENCE), classes);
    }

    @Test
    void ignoresTypesOfOtherSubjects() throws Exception {
        Nanopub definition = nanopubWith(KEY_DECLARATION, RDF.TYPE, AIDA_SENTENCE);

        Set<IRI> classes = ResourcePartPage.getPartClasses(definition, AIDA.stringValue(), agents());

        assertTrue(classes.isEmpty());
    }

    /**
     * The case this whole helper exists for: a person's defining nanopublication is their key
     * introduction, which gives their IRI a name and no type at all.
     */
    @Test
    void typesAnAgentWhoseIntroductionStatesNoType() throws Exception {
        Nanopub introduction = nanopubWith(
                ORCID, FOAF.NAME, "Josiah Carberry",
                KEY_DECLARATION, DECLARED_BY, ORCID);

        Set<IRI> classes = ResourcePartPage.getPartClasses(introduction, ORCID.stringValue(), agents(ORCID));

        assertEquals(Set.of(KPXL_TERMS.INDIVIDUAL_AGENT), classes);
    }

    @Test
    void typesAnAgentWithNoDefinitionAtAll() {
        Set<IRI> classes = ResourcePartPage.getPartClasses(null, ORCID.stringValue(), agents(ORCID));

        assertEquals(Set.of(KPXL_TERMS.INDIVIDUAL_AGENT), classes);
    }

    @Test
    void typesASoftwareAgentTheSameWay() {
        Set<IRI> classes = ResourcePartPage.getPartClasses(null, BOT.stringValue(), agents(BOT));

        assertEquals(Set.of(KPXL_TERMS.INDIVIDUAL_AGENT), classes);
    }

    @Test
    void leavesANonAgentPartUntyped() {
        Set<IRI> classes = ResourcePartPage.getPartClasses(null, AIDA.stringValue(), agents(ORCID));

        assertTrue(classes.isEmpty());
    }

    /** An agent that does state types keeps them alongside the one added here. */
    @Test
    void keepsStatedTypesAlongsideTheAgentType() throws Exception {
        Nanopub definition = nanopubWith(ORCID, RDF.TYPE, AIDA_SENTENCE);

        Set<IRI> classes = ResourcePartPage.getPartClasses(definition, ORCID.stringValue(), agents(ORCID));

        assertEquals(Set.of(KPXL_TERMS.INDIVIDUAL_AGENT, AIDA_SENTENCE), classes);
    }

}
