package com.knowledgepixels.nanodash.export;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.export.DocumentModel.Inline;
import com.knowledgepixels.nanodash.export.DocumentModel.ListBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.TableBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentModelBuilderTest {

    private static ApiResponseEntry entry(Map<String, String> values) {
        ApiResponseEntry entry = mock(ApiResponseEntry.class);
        when(entry.get(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> values.get(inv.getArgument(0, String.class)));
        return entry;
    }

    private static ApiResponse response(List<String> header, List<ApiResponseEntry> data) {
        ApiResponse response = mock(ApiResponse.class);
        when(response.getHeader()).thenReturn(header.toArray(new String[0]));
        when(response.getData()).thenReturn(data);
        return response;
    }

    private static View view(Set<String> actionMappingSourceColumns) {
        View view = mock(View.class);
        when(view.getActionMappingSourceColumns()).thenReturn(actionMappingSourceColumns);
        return view;
    }

    @Test
    @DisplayName("buildTable should skip label, action-mapping, and source columns")
    void tableSkipsHiddenColumns() {
        ApiResponse response = response(
                List.of("name", "name_label", "hiddenCol", "np"),
                List.of(entry(Map.of(
                        "name", "https://example.org/thing",
                        "name_label", "Thing",
                        "hiddenCol", "x",
                        "np", "https://w3id.org/np/RAabc"))));

        TableBlock table = DocumentModelBuilder.buildTable(view(Set.of("hiddenCol")), response);

        assertEquals(List.of("name"), table.headers());
        assertEquals(1, table.rows().size());
        assertEquals(1, table.rows().get(0).size());
        Inline cell = table.rows().get(0).get(0).get(0);
        assertEquals("Thing", cell.text());
        assertEquals("https://example.org/thing", cell.href());
    }

    @Test
    @DisplayName("buildTable should strip type suffixes and underscores from headers")
    void tableHeaderNames() {
        ApiResponse response = response(
                List.of("creation_date_iri", "some_value_multi"),
                List.of());

        TableBlock table = DocumentModelBuilder.buildTable(view(Set.of()), response);

        assertEquals(List.of("creation date", "some value"), table.headers());
    }

    @Test
    @DisplayName("buildTable should drop the header row when all columns are _noheader")
    void tableNoHeaderRow() {
        ApiResponse response = response(
                List.of("name_noheader"),
                List.of(entry(Map.of("name_noheader", "plain text"))));

        TableBlock table = DocumentModelBuilder.buildTable(view(Set.of()), response);

        assertNull(table.headers());
        assertEquals("plain text", table.rows().get(0).get(0).get(0).text());
    }

    @Test
    @DisplayName("renderCell should fall back to the URI short name when the label equals the URI")
    void labelEqualToUriTreatedAsAbsent() {
        ApiResponseEntry entry = entry(Map.of(
                "thing", "https://example.org/my-thing",
                "thing_label", "https://example.org/my-thing"));

        List<Inline> inlines = DocumentModelBuilder.renderCell(entry, "thing", "https://example.org/my-thing");

        assertEquals("my-thing", inlines.get(0).text());
        assertEquals("https://example.org/my-thing", inlines.get(0).href());
    }

    @Test
    @DisplayName("renderCell should split multi-IRI values and use per-value labels")
    void multiIriSplitting() {
        ApiResponseEntry entry = entry(Map.of(
                "things_multi_iri", "https://example.org/a https://example.org/b",
                "things_label_multi", "Label A\n"));

        List<Inline> inlines = DocumentModelBuilder.renderCell(entry, "things_multi_iri",
                "https://example.org/a https://example.org/b");

        assertEquals(3, inlines.size());
        assertEquals("Label A", inlines.get(0).text());
        assertEquals("https://example.org/a", inlines.get(0).href());
        assertEquals(", ", inlines.get(1).text());
        assertEquals("b", inlines.get(2).text());
        assertEquals("https://example.org/b", inlines.get(2).href());
    }

    @Test
    @DisplayName("renderCell should use the display label for literals")
    void literalLabel() {
        ApiResponseEntry entry = entry(Map.of(
                "note", "full text value",
                "note_label", "short label"));

        List<Inline> inlines = DocumentModelBuilder.renderCell(entry, "note", "full text value");

        assertEquals("short label", inlines.get(0).text());
        assertNull(inlines.get(0).href());
    }

    @Test
    @DisplayName("buildItemList should render only the first non-empty visible column per row")
    void itemListFirstColumnOnly() {
        ApiResponse response = response(
                List.of("first", "second"),
                List.of(
                        entry(Map.of("first", "https://example.org/x", "second", "ignored")),
                        entry(Map.of("second", "only second"))));

        ListBlock list = DocumentModelBuilder.buildItemList(response);

        assertEquals(2, list.items().size());
        assertEquals("https://example.org/x", list.items().get(0).get(0).href());
        assertEquals("only second", list.items().get(1).get(0).text());
        assertEquals(1, list.items().get(0).size());
    }

    @Test
    @DisplayName("buildList should join columns with a separator and drop the ^-labeled source column")
    void listSeparatorAndSourceColumn() {
        ApiResponse response = response(
                List.of("name", "date", "np"),
                List.of(entry(Map.of(
                        "name", "Alice",
                        "date", "2026-01-01",
                        "np", "https://w3id.org/np/RAabc",
                        "np_label", "^"))));

        ListBlock list = DocumentModelBuilder.buildList(view(Set.of()), response);

        assertEquals(1, list.items().size());
        List<Inline> item = list.items().get(0);
        assertEquals(3, item.size());
        assertEquals("Alice", item.get(0).text());
        assertEquals(" · ", item.get(1).text());
        assertEquals("2026-01-01", item.get(2).text());
    }

    @Test
    @DisplayName("htmlToPlainText should strip tags and unescape entities")
    void htmlToPlainText() {
        assertEquals("a < b & c d",
                DocumentModelBuilder.htmlToPlainText("<p>a &lt; b &amp; c</p> <b>d</b>"));
    }

    @Test
    @DisplayName("htmlToPlainText should decode numeric entities as the sanitizer emits them")
    void htmlToPlainTextNumericEntities() {
        assertEquals("\"quoted\" and 'single' and é",
                DocumentModelBuilder.htmlToPlainText("&#34;quoted&#34; and &#39;single&#39; and &#xe9;"));
    }

}
