package com.knowledgepixels.nanodash.repository;

import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SpaceRepository#reduceByRef} — the pure ref-reduction step of the
 * ref-aware get-spaces (v3) handling. See docs/space-ref-identity.md.
 */
class SpaceRepositoryRefReductionTest {

    /** Build a get-spaces row. ref/root may be null to simulate the pre-v3 query. */
    private static ApiResponseEntry row(String iri, String np, String ref, String root) {
        ApiResponseEntry e = new ApiResponseEntry();
        e.add("space_iri", iri);
        e.add("np", np);
        e.add("space_iri_label", "label-" + np);
        e.add("type", "https://w3id.org/kpxl/gen/terms/Group");
        if (ref != null) e.add("ref", ref);
        if (root != null) e.add("root", root);
        return e;
    }

    private static String npOf(SpaceRepository.RefReduction r, String iri) {
        return r.representatives.stream()
                .filter(e -> iri.equals(e.get("space_iri")))
                .map(e -> e.get("np"))
                .findFirst().orElse(null);
    }

    @Test
    void singleRefSpacesAreUnchanged() {
        // Rows in DESC(date) order; each IRI has exactly one ref.
        List<ApiResponseEntry> rows = List.of(
                row("https://ex/a", "npA", "ref/a", "rootA"),
                row("https://ex/b", "npB", "ref/b", "rootB"));
        SpaceRepository.RefReduction red = SpaceRepository.reduceByRef(rows);

        assertEquals(2, red.representatives.size());
        assertEquals("npA", npOf(red, "https://ex/a"));
        assertEquals("npB", npOf(red, "https://ex/b"));
        assertEquals(Set.of("rootA"), red.refRootsByIri.get("https://ex/a"));
        assertEquals(Set.of("rootB"), red.refRootsByIri.get("https://ex/b"));
        assertEquals(1, red.refRootsByIri.get("https://ex/a").size());
    }

    @Test
    void multiRefIriYieldsOneRepresentativeButCapturesAllRoots() {
        // Same IRI, two refs; the latest-date row (first) is the representative.
        List<ApiResponseEntry> rows = List.of(
                row("https://ex/m", "npLatest", "ref/r2", "root2"),
                row("https://ex/m", "npOlder", "ref/r1", "root1"));
        SpaceRepository.RefReduction red = SpaceRepository.reduceByRef(rows);

        assertEquals(1, red.representatives.size());
        assertEquals("npLatest", npOf(red, "https://ex/m"));
        assertEquals(Set.of("root1", "root2"), red.refRootsByIri.get("https://ex/m"));
        assertEquals(2, red.refRootsByIri.get("https://ex/m").size());
    }

    @Test
    void multipleDefinitionsWithinOneRefAreDedupedToLatest() {
        // One ref, two definitions (root + update); only the latest (first) survives.
        List<ApiResponseEntry> rows = List.of(
                row("https://ex/u", "npUpdate", "ref/r", "rootR"),
                row("https://ex/u", "npRoot", "ref/r", "rootR"));
        SpaceRepository.RefReduction red = SpaceRepository.reduceByRef(rows);

        assertEquals(1, red.representatives.size());
        assertEquals("npUpdate", npOf(red, "https://ex/u"));
        assertEquals(Set.of("rootR"), red.refRootsByIri.get("https://ex/u"));
    }

    @Test
    void preV3RowsWithoutRefDegradeToDedupByIri() {
        // No ref/root (pre-v3 query): dedup by IRI, first (latest) wins, no ref roots.
        List<ApiResponseEntry> rows = List.of(
                row("https://ex/p", "npLatest", null, null),
                row("https://ex/p", "npOlder", null, null),
                row("https://ex/q", "npQ", null, null));
        SpaceRepository.RefReduction red = SpaceRepository.reduceByRef(rows);

        assertEquals(2, red.representatives.size());
        assertEquals("npLatest", npOf(red, "https://ex/p"));
        assertEquals("npQ", npOf(red, "https://ex/q"));
        assertTrue(red.refRootsByIri.isEmpty(), "no ref roots without ?ref/?root");
    }

    @Test
    void blankSpaceIrisAreSkipped() {
        List<ApiResponseEntry> rows = new ArrayList<>();
        rows.add(row("", "npBlank", "ref/x", "rootX"));
        rows.add(row("https://ex/a", "npA", "ref/a", "rootA"));
        SpaceRepository.RefReduction red = SpaceRepository.reduceByRef(rows);

        List<String> iris = red.representatives.stream()
                .map(e -> e.get("space_iri")).collect(Collectors.toList());
        assertEquals(List.of("https://ex/a"), iris);
    }

    @Test
    void representativeOrderFollowsFirstAppearance() {
        List<ApiResponseEntry> rows = List.of(
                row("https://ex/b", "npB", "ref/b", "rootB"),
                row("https://ex/a", "npA", "ref/a", "rootA"),
                row("https://ex/b", "npB2", "ref/b2", "rootB2"));
        SpaceRepository.RefReduction red = SpaceRepository.reduceByRef(rows);

        List<String> iris = red.representatives.stream()
                .map(e -> e.get("space_iri")).collect(Collectors.toList());
        assertEquals(List.of("https://ex/b", "https://ex/a"), iris);
        Map<String, Set<String>> roots = red.refRootsByIri;
        assertEquals(Set.of("rootB", "rootB2"), roots.get("https://ex/b"));
    }
}
