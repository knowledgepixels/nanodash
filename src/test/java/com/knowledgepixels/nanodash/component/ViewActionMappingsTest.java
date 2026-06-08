package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ViewActionMappingsTest {

    private static final IRI ACTION = Values.iri("https://example.org/np/view/action");

    private static View viewWith(List<String> mappings, Template template) {
        View view = mock(View.class);
        when(view.getTemplateQueryMappings(ACTION)).thenReturn(mappings);
        when(view.getTemplateForAction(ACTION)).thenReturn(template);
        return view;
    }

    private static Template templateWhereRequired(String... requiredParams) {
        Template t = mock(Template.class);
        for (String p : List.of(requiredParams)) when(t.isRequiredField(p)).thenReturn(true);
        return t;
    }

    private static ApiResponseEntry row(String col, String value) {
        ApiResponseEntry e = mock(ApiResponseEntry.class);
        when(e.get(col)).thenReturn(value);
        return e;
    }

    @Test
    void emptyRequiredParamHidesAction() {
        View view = viewWith(List.of("col:foo"), templateWhereRequired("foo"));
        PageParameters params = new PageParameters();
        assertFalse(ViewActionMappings.applyEntryMappings(view, ACTION, row("col", ""), params));
        assertTrue(params.get("param_foo").isNull());
    }

    @Test
    void emptyOptionalParamKeepsActionButSetsNothing() {
        View view = viewWith(List.of("col:foo"), templateWhereRequired(/* foo optional */));
        PageParameters params = new PageParameters();
        assertTrue(ViewActionMappings.applyEntryMappings(view, ACTION, row("col", ""), params));
        assertTrue(params.get("param_foo").isNull());
    }

    @Test
    void presentParamIsSetAsParamPrefixed() {
        View view = viewWith(List.of("col:foo"), templateWhereRequired("foo"));
        PageParameters params = new PageParameters();
        assertTrue(ViewActionMappings.applyEntryMappings(view, ACTION, row("col", "v"), params));
        assertEquals("v", params.get("param_foo").toString());
    }

    @Test
    void rawKeyEmptyHidesAction() {
        View view = viewWith(List.of("col:@derive-a"), mock(Template.class));
        PageParameters params = new PageParameters();
        assertFalse(ViewActionMappings.applyEntryMappings(view, ACTION, row("col", null), params));
    }

    @Test
    void rawKeySetWithoutParamPrefix() {
        View view = viewWith(List.of("col:@derive-a"), mock(Template.class));
        PageParameters params = new PageParameters();
        assertTrue(ViewActionMappings.applyEntryMappings(view, ACTION, row("col", "np123"), params));
        assertEquals("np123", params.get("derive-a").toString());
        assertTrue(params.get("param_derive-a").isNull());
    }

    @Test
    void multipleMappingsAllPresentAreAllSet() {
        ApiResponseEntry e = mock(ApiResponseEntry.class);
        when(e.get("a")).thenReturn("v1");
        when(e.get("b")).thenReturn("np");
        View view = viewWith(List.of("a:foo", "b:@derive-a"), templateWhereRequired("foo"));
        PageParameters params = new PageParameters();
        assertTrue(ViewActionMappings.applyEntryMappings(view, ACTION, e, params));
        assertEquals("v1", params.get("param_foo").toString());
        assertEquals("np", params.get("derive-a").toString());
    }

    @Test
    void multipleMappingsOneRequiredEmptyHidesAction() {
        ApiResponseEntry e = mock(ApiResponseEntry.class);
        when(e.get("a")).thenReturn("v1");
        when(e.get("b")).thenReturn(""); // empty raw-key target
        View view = viewWith(List.of("a:foo", "b:@derive-a"), templateWhereRequired("foo"));
        PageParameters params = new PageParameters();
        assertFalse(ViewActionMappings.applyEntryMappings(view, ACTION, e, params));
    }

    @Test
    void noMappingsRendersAction() {
        View view = viewWith(List.of(), mock(Template.class));
        PageParameters params = new PageParameters();
        assertTrue(ViewActionMappings.applyEntryMappings(view, ACTION, mock(ApiResponseEntry.class), params));
    }

}
