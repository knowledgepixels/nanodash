package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.domain.UserData;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewDataFetcherTest {

    @Nested
    @DisplayName("retrieveResponseWithWait")
    class RetrieveResponseWithWaitTest {

        @Test
        @DisplayName("should return response immediately when available")
        void returnsImmediately() {
            QueryRef queryRef = mock(QueryRef.class);
            ApiResponse expected = mock(ApiResponse.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false)).thenReturn(expected);

                ApiResponse result = ViewDataFetcher.retrieveResponseWithWait(queryRef);

                assertSame(expected, result);
                apiCache.verify(() -> ApiCache.retrieveResponseSync(queryRef, false), times(1));
            }
        }

        @Test
        @DisplayName("should return null when no result and not running")
        void returnsNullWhenNotRunning() {
            QueryRef queryRef = mock(QueryRef.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false)).thenReturn(null);
                apiCache.when(() -> ApiCache.isRunning(queryRef)).thenReturn(false);

                ApiResponse result = ViewDataFetcher.retrieveResponseWithWait(queryRef);

                assertNull(result);
            }
        }

        @Test
        @DisplayName("should retry and return result when query finishes running")
        void retriesWhileRunning() {
            QueryRef queryRef = mock(QueryRef.class);
            ApiResponse expected = mock(ApiResponse.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                // First call: null (running), second call: result available
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false))
                        .thenReturn(null)
                        .thenReturn(expected);
                apiCache.when(() -> ApiCache.isRunning(queryRef)).thenReturn(true);

                ApiResponse result = ViewDataFetcher.retrieveResponseWithWait(queryRef);

                assertSame(expected, result);
                apiCache.verify(() -> ApiCache.retrieveResponseSync(queryRef, false), times(2));
            }
        }
    }

    @Nested
    @DisplayName("partDefinitionPubkeys")
    class PartDefinitionPubkeysTest {

        private static final IRI ADMIN = Values.iri("https://orcid.org/0000-0000-0000-0001");
        private static final IRI MEMBER = Values.iri("https://orcid.org/0000-0000-0000-0002");
        private static final IRI OBSERVER = Values.iri("https://orcid.org/0000-0000-0000-0003");
        private static final String SPACE = "https://w3id.org/spaces/example";

        /** A space with one member of each tier, restricted to the given minimum rank. */
        private Space spaceWithTier(int minTierRank) {
            Space space = mock(Space.class);
            when(space.getPartDefinitionTierRank()).thenReturn(minTierRank);
            when(space.getUsers()).thenReturn(List.of(ADMIN, MEMBER, OBSERVER));
            lenient().when(space.userTier(ADMIN)).thenReturn(4);
            lenient().when(space.userTier(MEMBER)).thenReturn(2);
            lenient().when(space.userTier(OBSERVER)).thenReturn(1);
            return space;
        }

        private AbstractResourceWithProfile contextOf(Space space) {
            AbstractResourceWithProfile resource = mock(AbstractResourceWithProfile.class);
            when(resource.getSpace()).thenReturn(space);
            return resource;
        }

        private UserData userDataWithAKeyEach() {
            UserData userData = mock(UserData.class);
            when(userData.getPubkeyHashes(ADMIN, true)).thenReturn(List.of("keyAdmin"));
            lenient().when(userData.getPubkeyHashes(MEMBER, true)).thenReturn(List.of("keyMember"));
            lenient().when(userData.getPubkeyHashes(OBSERVER, true)).thenReturn(List.of("keyObserver"));
            return userData;
        }

        @Test
        @DisplayName("an undeclared space keeps every role-holder, observers included")
        void undeclaredSpaceKeepsEveryone() {
            // Built before the static stubbing: stubbing inside a thenReturn() argument
            // leaves Mockito mid-stub.
            UserData userData = userDataWithAKeyEach();
            AbstractResourceWithProfile context = contextOf(spaceWithTier(0));
            try (MockedStatic<User> user = mockStatic(User.class)) {
                user.when(User::getUserData).thenReturn(userData);

                assertEquals(List.of("keyAdmin", "keyMember", "keyObserver"),
                        ViewDataFetcher.partDefinitionPubkeys(SPACE, context));
            }
        }

        @Test
        @DisplayName("a space declaring the member tier drops the observers")
        void declaredTierDropsLowerTiers() {
            UserData userData = userDataWithAKeyEach();
            AbstractResourceWithProfile context = contextOf(spaceWithTier(2));
            try (MockedStatic<User> user = mockStatic(User.class)) {
                user.when(User::getUserData).thenReturn(userData);

                assertEquals(List.of("keyAdmin", "keyMember"),
                        ViewDataFetcher.partDefinitionPubkeys(SPACE, context));
            }
        }

        @Test
        @DisplayName("a user page uses the user's own keys, with no tier to apply")
        void userContextUsesItsOwnKeys() {
            AbstractResourceWithProfile resource = mock(AbstractResourceWithProfile.class);
            when(resource.getSpace()).thenReturn(null);
            UserData userData = mock(UserData.class);
            when(userData.getPubkeyHashes(Values.iri(SPACE), true)).thenReturn(List.of("keyOwn"));
            try (MockedStatic<User> user = mockStatic(User.class)) {
                user.when(User::getUserData).thenReturn(userData);

                assertEquals(List.of("keyOwn"), ViewDataFetcher.partDefinitionPubkeys(SPACE, resource));
            }
        }

    }

}
