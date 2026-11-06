package com.knowledgepixels.nanodash.repository;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * What the repository does with an answer that comes back short of the resources it
 * carried before. The query API is a set of instances with independently built indexes,
 * so a partial or empty result set is a normal kind of failure — and taking it at face
 * value is what made the home page report its own configuration as broken (issue #623).
 */
@ExtendWith(SystemStubsExtension.class)
class MaintainedResourceRepositoryTest {

    private static final String RESOURCE_A = "https://w3id.org/spaces/test/a/r/home";
    private static final String RESOURCE_B = "https://w3id.org/spaces/test/b/r/other";
    private static final String SPACE = "https://w3id.org/spaces/test";
    // Trusty-looking, but only ever handed to a stubbed Utils.getAsNanopub.
    private static final String NP_1 = "https://w3id.org/np/RA1111111111111111111111111111111111111111111";
    private static final String NP_2 = "https://w3id.org/np/RA2222222222222222222222222222222222222222222";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    private MockedStatic<SpaceRepository> spaceRepository;
    private MockedStatic<ApiCache> apiCache;
    // What the query API is currently answering with; null until the first answer, i.e.
    // a cold cache. Read by the stubbed ApiCache, so no test goes near the network.
    private ApiResponse answer;

    private final MaintainedResourceRepository repo = MaintainedResourceRepository.get();

    @BeforeEach
    void setUp() throws Exception {
        answer = null;
        resetRepository();
        clearResourceInstances();

        // Whichever retrieval the repository reaches for, it gets the current answer and
        // never the network. Stubbing only the one it happens to call today would leave
        // the other returning the static mock's null, i.e. a repository that knows
        // nothing — which is how this test last went wrong.
        apiCache = mockStatic(ApiCache.class);
        apiCache.when(() -> ApiCache.retrieveResponseIfAvailable(any(QueryRef.class)))
                .thenAnswer(invocation -> answer);
        apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean()))
                .thenAnswer(invocation -> answer);

        // The spaces are beside the point here: every row's space resolves, so what the
        // repository does is decided by the maintained-resource answer alone.
        SpaceRepository spaces = mock(SpaceRepository.class);
        when(spaces.findById(SPACE)).thenReturn(mock(Space.class));
        spaceRepository = mockStatic(SpaceRepository.class);
        spaceRepository.when(SpaceRepository::get).thenReturn(spaces);
    }

    @AfterEach
    void tearDown() throws Exception {
        spaceRepository.close();
        apiCache.close();
        resetRepository();
        clearResourceInstances();
    }

    @Test
    void anAnswerWithoutResourcesDoesNotUnseatTheOnesAlreadyKnown() {
        answer(RESOURCE_A, RESOURCE_B);
        MaintainedResource a = repo.findById(RESOURCE_A);
        assertNotNull(a);

        answer(); // the query API answers, with nothing in it

        assertSame(a, repo.findById(RESOURCE_A), "the known resources stay");
        assertNotNull(AbstractResourceWithProfile.get(RESOURCE_A),
                "and are not wiped from the instance registry either");
        assertFalse(repo.isAbsent(RESOURCE_A), "nothing about them is absent");
    }

    @Test
    void aResourceMissingFromTheNewestAnswerIsStillFoundAsLastKnown() {
        answer(RESOURCE_A, RESOURCE_B);
        MaintainedResource a = repo.findById(RESOURCE_A);

        answer(RESOURCE_B); // an answer short of one resource, not of all of them

        assertNull(repo.findById(RESOURCE_A), "the newest answer does not carry it");
        assertSame(a, repo.findLastKnownById(RESOURCE_A), "but it is not forgotten");
        assertFalse(repo.isAbsent(RESOURCE_A), "and it is not reported as unknown");
    }

    @Test
    void anIdNoAnswerEverCarriedIsReportedAbsent() {
        answer(RESOURCE_B);

        assertNull(repo.findLastKnownById(RESOURCE_A));
        assertTrue(repo.isAbsent(RESOURCE_A),
                "a warm repository holding resources knows this one is none of them");
    }

    @Test
    void nothingIsAbsentWhileNothingIsKnown() {
        assertFalse(repo.isAbsent(RESOURCE_A), "cold cache: no answer at all yet");

        answer(); // the first answer ever carries no resources

        assertFalse(repo.isAbsent(RESOURCE_A),
                "an instance that has been told of no resources cannot tell this one apart");
    }

    /**
     * Building the snapshot fetches no nanopub (issue #892). It runs under the repository's
     * lock, for every resource at once, so a registry fetch per resource there had every page
     * waiting ~10 minutes after a restart.
     */
    @Test
    void buildingTheSnapshotFetchesNoNanopub() {
        // The background pass runs on a pool thread, which the thread-local static mock of
        // Utils would not reach; keep it off so nothing here can go to a registry.
        envVars.set("NANODASH_DATA_REFRESH", "false");
        Nanopub declaration = mock(Nanopub.class);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_1)).thenReturn(declaration);

            answerWithNanopub(RESOURCE_A, NP_1);
            MaintainedResource a = repo.findById(RESOURCE_A);
            assertNotNull(a);
            utils.verify(() -> Utils.getAsNanopub(anyString()), never());
            assertFalse(a.isNanopubLoaded());

            assertSame(declaration, a.getNanopub(), "fetched when first asked for");
            assertSame(declaration, a.getNanopub(), "and kept");
            utils.verify(() -> Utils.getAsNanopub(NP_1), times(1));
            assertTrue(a.isNanopubLoaded());
        }
    }

    @Test
    void aNewVersionOfTheResourceIsNotHandedTheOldNanopub() {
        envVars.set("NANODASH_DATA_REFRESH", "false");
        Nanopub v1 = mock(Nanopub.class);
        Nanopub v2 = mock(Nanopub.class);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_1)).thenReturn(v1);
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(v2);

            answerWithNanopub(RESOURCE_A, NP_1);
            MaintainedResource a = repo.findById(RESOURCE_A);
            assertSame(v1, a.getNanopub());

            answerWithNanopub(RESOURCE_A, NP_2); // the resource was redeclared
            assertSame(a, repo.findById(RESOURCE_A), "the same resource object, updated");
            utils.verify(() -> Utils.getAsNanopub(NP_2), never());
            assertFalse(a.isNanopubLoaded(), "what it held belongs to the previous version");
            assertSame(v2, a.getNanopub());
        }
    }

    @Test
    void aFailedFetchIsTriedAgainOnTheNextCall() {
        envVars.set("NANODASH_DATA_REFRESH", "false");
        Nanopub declaration = mock(Nanopub.class);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_1)).thenReturn(null, declaration);

            answerWithNanopub(RESOURCE_A, NP_1);
            MaintainedResource a = repo.findById(RESOURCE_A);
            assertNull(a.getNanopub(), "the registry did not answer");
            assertSame(declaration, a.getNanopub(), "a later call fetches it after all");
        }
    }

    /** Makes the query API answer with the given resources, and nothing else. */
    private void answer(String... resourceIds) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"resource", "space", "np", "label"});
        for (String resourceId : resourceIds) {
            // Deliberately not a trusty URI: Utils.getAsNanopub then answers null
            // without going to the registry.
            response.add(row(resourceId, "http://example.org/np/" + resourceId.hashCode()));
        }
        answer = response;
    }

    /** Makes the query API answer with one resource, declared by the given nanopub. */
    private void answerWithNanopub(String resourceId, String npId) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"resource", "space", "np", "label"});
        response.add(row(resourceId, npId));
        answer = response;
    }

    private static ApiResponseEntry row(String resourceId, String npId) {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("resource", resourceId);
        entry.add("space", SPACE);
        entry.add("np", npId);
        entry.add("label", resourceId);
        return entry;
    }

    /** Puts the singleton back to how it starts, since the tests share it. */
    private void resetRepository() throws Exception {
        Field cachedFor = MaintainedResourceRepository.class.getDeclaredField("cachedFor");
        cachedFor.setAccessible(true);
        cachedFor.set(repo, null);
        Class<?> snapshotClass = Class.forName(
                "com.knowledgepixels.nanodash.repository.MaintainedResourceRepository$Snapshot");
        Field empty = snapshotClass.getDeclaredField("EMPTY");
        empty.setAccessible(true);
        Field snapshot = MaintainedResourceRepository.class.getDeclaredField("snapshot");
        snapshot.setAccessible(true);
        snapshot.set(repo, empty.get(null));
        Field lastKnown = MaintainedResourceRepository.class.getDeclaredField("lastKnownById");
        lastKnown.setAccessible(true);
        ((Map<?, ?>) lastKnown.get(repo)).clear();
    }

    @SuppressWarnings("unchecked")
    private void clearResourceInstances() throws Exception {
        ((Map<Object, Map<?, ?>>) field(AbstractResourceWithProfile.class, "instances")).clear();
    }

    private static Object field(Class<?> owner, String name) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

}
