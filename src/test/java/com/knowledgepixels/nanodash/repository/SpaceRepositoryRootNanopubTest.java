package com.knowledgepixels.nanodash.repository;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * How the space build gets the spaces' root nanopubs. The build runs under the lock every
 * space lookup waits for, and each space used to fetch its own root nanopub there, one
 * after the other: a few hundred fetches after a restart, with every page waiting (issue
 * #892). The repository now fetches what it needs in one go, and a nanopub that cannot be
 * fetched no longer breaks the build.
 */
class SpaceRepositoryRootNanopubTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String SPACE_A = "https://w3id.org/spaces/test/a";
    private static final String SPACE_B = "https://w3id.org/spaces/test/b";
    private static final String NP_A1 = np('1');
    private static final String NP_A2 = np('2');
    private static final String NP_B = np('3');

    private static String np(char c) {
        return "https://w3id.org/np/RA" + String.valueOf(c).repeat(43);
    }

    private static String alias(String npId) {
        return "https://example.org/alias/" + npId.substring(npId.length() - 1);
    }

    private final SpaceRepository repo = SpaceRepository.get();

    private MockedStatic<ApiCache> apiCache;
    private MockedStatic<Utils> utils;
    // What the spaces query currently answers; null until the first answer.
    private ApiResponse answer;
    // The root nanopubs a registry can deliver, and what the repository asked for.
    private final Map<String, Nanopub> registry = new HashMap<>();
    private final List<Set<String>> requests = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        answer = null;
        resetRepository();
        clearResourceInstances();
        apiCache = mockStatic(ApiCache.class);
        apiCache.when(() -> ApiCache.retrieveResponseIfAvailable(any(QueryRef.class))).thenAnswer(invocation -> {
            QueryRef ref = invocation.getArgument(0);
            return QueryApiAccess.GET_SPACES_REF.equals(ref.getQueryId()) ? answer : null;
        });
        utils = mockStatic(Utils.class);
        utils.when(() -> Utils.getNanopubs(anyCollection())).thenAnswer(invocation -> {
            Collection<String> ids = invocation.getArgument(0);
            requests.add(Set.copyOf(ids));
            Map<String, Nanopub> found = new HashMap<>();
            for (String id : ids) {
                if (registry.containsKey(id)) found.put(id, registry.get(id));
            }
            return found;
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        utils.close();
        apiCache.close();
        resetRepository();
        clearResourceInstances();
    }

    @Test
    void theRootNanopubsAreFetchedInOneGoAndOnlyOnce() {
        publish(SPACE_A, NP_A1);
        publish(SPACE_B, NP_B);
        answer(SPACE_A, NP_A1, SPACE_B, NP_B);

        Space a = repo.findById(SPACE_A);
        assertNotNull(a);
        assertNotNull(repo.findById(SPACE_B));
        assertEquals(List.of(Set.of(NP_A1, NP_B)), requests);
        assertSame(a, repo.findByAltId(alias(NP_A1)), "what the root nanopub says is read");

        answer(SPACE_A, NP_A1, SPACE_B, NP_B); // the query refreshed, nothing changed
        assertSame(a, repo.findById(SPACE_A));
        assertEquals(1, requests.size(), "unchanged spaces keep the nanopubs they have");
    }

    @Test
    void aRootNanopubThatCannotBeFetchedDoesNotBreakTheBuild() {
        publish(SPACE_A, NP_A1);
        answer(SPACE_A, NP_A1, SPACE_B, NP_B);

        assertNotNull(repo.findById(SPACE_A));
        Space b = repo.findById(SPACE_B);
        assertNotNull(b, "the space is known all the same");
        assertNull(b.getNanopub());

        publish(SPACE_B, NP_B);
        answer(SPACE_A, NP_A1, SPACE_B, NP_B);
        assertSame(b, repo.findById(SPACE_B));
        assertEquals(Set.of(NP_B), requests.get(1), "the next build asks for the missing one only");
        assertSame(b, repo.findByAltId(alias(NP_B)), "and the space takes on its definition");
    }

    @Test
    void aNewDefinitionIsTakenOnOnceItsNanopubIsThere() {
        publish(SPACE_A, NP_A1);
        answer(SPACE_A, NP_A1);
        Space a = repo.findById(SPACE_A);

        answer(SPACE_A, NP_A2); // redefined, but the new definition cannot be fetched yet
        assertSame(a, repo.findById(SPACE_A));
        assertEquals(NP_A1, a.getNanopubId(), "the space keeps the definition it has");
        assertSame(a, repo.findByAltId(alias(NP_A1)));

        publish(SPACE_A, NP_A2);
        answer(SPACE_A, NP_A2);
        assertSame(a, repo.findById(SPACE_A));
        assertEquals(NP_A2, a.getNanopubId());
        assertSame(a, repo.findByAltId(alias(NP_A2)));
        assertNull(repo.findByAltId(alias(NP_A1)));
    }

    /** Makes a registry able to deliver a root nanopub that declares an alias of the space. */
    private void publish(String spaceId, String npId) {
        Nanopub np = mock(Nanopub.class);
        when(np.getAssertion()).thenReturn(Set.of(
                vf.createStatement(vf.createIRI(spaceId), OWL.SAMEAS, vf.createIRI(alias(npId)))));
        registry.put(npId, np);
    }

    /** Makes the spaces query answer with the given (space, root nanopub) pairs. */
    private void answer(String... spaceAndNp) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"space_iri", "np", "space_iri_label", "type"});
        for (int i = 0; i < spaceAndNp.length; i += 2) {
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("space_iri", spaceAndNp[i]);
            entry.add("np", spaceAndNp[i + 1]);
            entry.add("space_iri_label", spaceAndNp[i]);
            entry.add("type", "https://w3id.org/kpxl/gen/terms/Project");
            response.add(entry);
        }
        answer = response;
    }

    /** Puts the singleton back to how it starts, since the tests share it. */
    private void resetRepository() throws Exception {
        Field cachedFor = SpaceRepository.class.getDeclaredField("cachedFor");
        cachedFor.setAccessible(true);
        cachedFor.set(repo, null);
        Class<?> snapshotClass = Class.forName("com.knowledgepixels.nanodash.repository.SpaceRepository$Snapshot");
        Field empty = snapshotClass.getDeclaredField("EMPTY");
        empty.setAccessible(true);
        Field snapshot = SpaceRepository.class.getDeclaredField("snapshot");
        snapshot.setAccessible(true);
        snapshot.set(repo, empty.get(null));
    }

    @SuppressWarnings("unchecked")
    private static void clearResourceInstances() throws Exception {
        Field f = AbstractResourceWithProfile.class.getDeclaredField("instances");
        f.setAccessible(true);
        ((Map<Object, Map<?, ?>>) f.get(null)).clear();
    }

}
