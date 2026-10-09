package com.knowledgepixels.nanodash;

import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FilteredQueryResultDataProviderTest {

    private static ApiResponse response(String[] header, String[]... rows) {
        ApiResponse r = new ApiResponse();
        r.setHeader(header);
        for (String[] row : rows) r.add(row);
        return r;
    }

    private static List<String> column(FilteredQueryResultDataProvider p, String key) {
        List<String> values = new ArrayList<>();
        Iterator<? extends ApiResponseEntry> it = p.iterator(0, p.size());
        while (it.hasNext()) values.add(it.next().get(key));
        return values;
    }

    private static FilteredQueryResultDataProvider sorted(ApiResponse r, String property, boolean ascending) {
        QueryResultDataProvider base = new QueryResultDataProvider(r.getData());
        ((SingleSortState<String>) base.getSortState()).setSort(new SortParam<>(property, ascending));
        return new FilteredQueryResultDataProvider(base, r);
    }

    @Test
    void groupColumnIsDetectedBySuffix() {
        ApiResponse r = response(new String[]{"question_group", "fsr"}, new String[]{"F1", "a"});
        assertEquals("question_group", new FilteredQueryResultDataProvider(new QueryResultDataProvider(r.getData()), r).getGroupColumn());
        ApiResponse plain = response(new String[]{"question", "fsr"}, new String[]{"F1", "a"});
        assertNull(new FilteredQueryResultDataProvider(new QueryResultDataProvider(plain.getData()), plain).getGroupColumn());
    }

    @Test
    void sortingKeepsGroupOrderAndSortsWithinGroups() {
        // Groups in query order F1, F2; rows deliberately unsorted inside them.
        ApiResponse r = response(new String[]{"question_group", "fsr"},
                new String[]{"F1", "zebra"}, new String[]{"F1", "apple"},
                new String[]{"F2", "mango"}, new String[]{"F2", "banana"});
        assertEquals(List.of("apple", "zebra", "banana", "mango"), column(sorted(r, "fsr", true), "fsr"));
        // Descending reverses the rows inside each group, not the groups.
        assertEquals(List.of("zebra", "apple", "mango", "banana"), column(sorted(r, "fsr", false), "fsr"));
        assertEquals(List.of("F1", "F1", "F2", "F2"), column(sorted(r, "fsr", false), "question_group"));
    }

    @Test
    void withoutGroupColumnSortingIsGlobal() {
        ApiResponse r = response(new String[]{"question", "fsr"},
                new String[]{"F1", "zebra"}, new String[]{"F1", "apple"},
                new String[]{"F2", "mango"}, new String[]{"F2", "banana"});
        assertEquals(List.of("apple", "banana", "mango", "zebra"), column(sorted(r, "fsr", true), "fsr"));
    }

    @Test
    void unsortedKeepsQueryOrder() {
        ApiResponse r = response(new String[]{"question_group", "fsr"},
                new String[]{"F2", "mango"}, new String[]{"F1", "zebra"});
        FilteredQueryResultDataProvider p = new FilteredQueryResultDataProvider(new QueryResultDataProvider(r.getData()), r);
        assertEquals(List.of("mango", "zebra"), column(p, "fsr"));
    }

}
