package com.knowledgepixels.nanodash.repository;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * A refresh of the spaces listing, such as the one that follows a publication to one space,
 * marks only the spaces whose definition changed as needing their details re-fetched, not
 * every space (#358).
 */
class SpaceRepositoryRefreshTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String SPACE_A = "https://w3id.org/spaces/test/a";
    private static final String SPACE_B = "https://w3id.org/spaces/test/b";
    private static final String SPACE_TYPE = "https://w3id.org/kpxl/gen/terms/Space";

    private MockedStatic<ApiCache> apiCache;
    private MockedStatic<Utils> utils;
    private ApiResponse answer;

    private final SpaceRepository repo = SpaceRepository.get();

    @BeforeEach
    void setUp() throws Exception {
        answer = null;
        resetRepository();
        clearResourceInstances();

        apiCache = mockStatic(ApiCache.class);
        apiCache.when(() -> ApiCache.retrieveResponseIfAvailable(any(QueryRef.class)))
                .thenAnswer(invocation -> answer);
        apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean()))
                .thenReturn(null);

        Nanopub definition = spaceDefinition();
        utils = mockStatic(Utils.class, Answers.CALLS_REAL_METHODS);
        utils.when(() -> Utils.getAsNanopub(anyString())).thenReturn(definition);
    }

    @AfterEach
    void tearDown() throws Exception {
        utils.close();
        apiCache.close();
        resetRepository();
        clearResourceInstances();
    }

    @Test
    void onlyTheSpaceWhoseDefinitionChangedNeedsItsDetailsRefetched() throws Exception {
        answer(SPACE_A, "http://example.org/np/a1", SPACE_B, "http://example.org/np/b1");
        Space a = repo.findById(SPACE_A);
        Space b = repo.findById(SPACE_B);
        assertNotNull(a);
        assertNotNull(b);
        markUpToDate(a);
        markUpToDate(b);

        answer(SPACE_A, "http://example.org/np/a2", SPACE_B, "http://example.org/np/b1");
        repo.findById(SPACE_A);

        assertTrue(needsUpdate(a), "the space with a new definition is re-fetched");
        assertFalse(needsUpdate(b), "the unchanged space is left alone");
    }

    @Test
    void aRefreshedListingWithoutChangesLeavesEverySpaceAlone() throws Exception {
        answer(SPACE_A, "http://example.org/np/a1", SPACE_B, "http://example.org/np/b1");
        Space a = repo.findById(SPACE_A);
        Space b = repo.findById(SPACE_B);
        markUpToDate(a);
        markUpToDate(b);

        answer(SPACE_A, "http://example.org/np/a1", SPACE_B, "http://example.org/np/b1");
        repo.findById(SPACE_A);

        assertFalse(needsUpdate(a));
        assertFalse(needsUpdate(b));
    }

    /**
     * Makes the spaces query answer with a new response carrying the given spaces.
     *
     * @param idsAndNanopubs alternating space IRIs and the IDs of their defining nanopubs
     */
    private void answer(String... idsAndNanopubs) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"space_iri", "np", "space_iri_label", "type"});
        for (int i = 0; i < idsAndNanopubs.length; i += 2) {
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("space_iri", idsAndNanopubs[i]);
            entry.add("np", idsAndNanopubs[i + 1]);
            entry.add("space_iri_label", idsAndNanopubs[i]);
            entry.add("type", SPACE_TYPE);
            response.add(entry);
        }
        answer = response;
    }

    private static Nanopub spaceDefinition() throws Exception {
        NanopubCreator creator = new NanopubCreator("http://purl.org/nanopub/temp/space/");
        creator.addAssertionStatement(vf.createIRI(SPACE_A), RDF.TYPE, vf.createIRI(SPACE_TYPE));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    private static void markUpToDate(Space space) throws Exception {
        dataNeedsUpdateField().setBoolean(space, false);
    }

    private static boolean needsUpdate(Space space) throws Exception {
        return dataNeedsUpdateField().getBoolean(space);
    }

    private static Field dataNeedsUpdateField() throws Exception {
        Field f = AbstractResourceWithProfile.class.getDeclaredField("dataNeedsUpdate");
        f.setAccessible(true);
        return f;
    }

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
