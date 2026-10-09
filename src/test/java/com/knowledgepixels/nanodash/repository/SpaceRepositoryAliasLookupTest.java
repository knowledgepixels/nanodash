package com.knowledgepixels.nanodash.repository;

import com.knowledgepixels.nanodash.domain.Space;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * The id-or-alias lookup a space page resolves its {@code ?id=} with. A space may declare
 * another IRI as an {@code owl:sameAs} alias of itself — a renamed space naming the IRI it
 * used to have — and a reference to that IRI has to keep arriving at the space, including
 * once the alias IRI's own definition is retracted and it names nothing in its own right.
 * <p>
 * The lookups are stubbed with {@code doReturn}, not {@code when}: on a
 * {@code CALLS_REAL_METHODS} mock the latter would run the real lookup while stubbing it,
 * which goes to the query API.
 */
class SpaceRepositoryAliasLookupTest {

    private static final String CANONICAL = "https://w3id.org/spaces/nanopub/nanosessions/session35";
    private static final String ALIAS = "https://w3id.org/spaces/session35";
    private static final String UNKNOWN = "https://w3id.org/spaces/nosuchspace";

    private SpaceRepository repository() {
        return mock(SpaceRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    void findsSpaceByItsOwnId() {
        SpaceRepository spaces = repository();
        Space space = mock(Space.class);
        doReturn(space).when(spaces).findById(CANONICAL);

        assertSame(space, spaces.findByIdOrAltId(CANONICAL));
        // An id that names a space is answered from the id index alone, so a space is never
        // shadowed by another one's alias.
        verify(spaces, never()).findByAltId(anyString());
    }

    @Test
    void fallsBackToAnAliasWhenTheIdNamesNoSpace() {
        SpaceRepository spaces = repository();
        Space space = mock(Space.class);
        doReturn(null).when(spaces).findById(ALIAS);
        doReturn(space).when(spaces).findByAltId(ALIAS);

        assertSame(space, spaces.findByIdOrAltId(ALIAS));
    }

    @Test
    void returnsNullWhenNeitherIndexKnowsTheId() {
        SpaceRepository spaces = repository();
        doReturn(null).when(spaces).findById(UNKNOWN);
        doReturn(null).when(spaces).findByAltId(UNKNOWN);

        assertNull(spaces.findByIdOrAltId(UNKNOWN));
    }

}
