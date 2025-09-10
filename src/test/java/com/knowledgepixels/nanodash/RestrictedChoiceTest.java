package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.Test;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;
import java.util.Map;

import static com.knowledgepixels.nanodash.utils.TestUtils.vf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestrictedChoiceTest {

    @Test
    void construct() {
        TemplateContext mockContext = mock(TemplateContext.class);
        Template mockTemplate = mock(Template.class);
        when(mockContext.getTemplate()).thenReturn(mockTemplate);
        IRI placeholderIri = vf.createIRI("https://knowledgepixels.com/placeholder");
        RestrictedChoice restrictedChoice = new RestrictedChoice(placeholderIri, mockContext);
        assertNotNull(restrictedChoice);
    }

    @Test
    void getPossibleValuesWithNullComponentModels() {
        TemplateContext mockContext = mock(TemplateContext.class);
        Template mockTemplate = mock(Template.class);
        when(mockContext.getTemplate()).thenReturn(mockTemplate);
        when(mockContext.getComponentModels()).thenReturn(mock(Map.class));
        when(mockContext.getComponentModels().get(any(IRI.class))).thenReturn(null);
        List<Value> expectedPossibleValues = List.of(vf.createLiteral("value1"),
                vf.createLiteral("value2"),
                NTEMPLATE.VALUE_PLACEHOLDER);
        when(mockTemplate.getPossibleValues(any(IRI.class))).thenReturn(expectedPossibleValues);
        when(mockTemplate.isPlaceholder(any(IRI.class))).thenReturn(true);

        RestrictedChoice restrictedChoice = new RestrictedChoice(vf.createIRI("https://knowledgepixels.com/placeholder"), mockContext);
        List<String> possibleValues = restrictedChoice.getPossibleValues();

        assertEquals(List.of("\"value1\"", "\"value2\""), possibleValues);
    }

    @Test
    void hasPossibleRefValues() {
        TemplateContext mockContext = mock(TemplateContext.class);
        Template mockTemplate = mock(Template.class);
        when(mockContext.getTemplate()).thenReturn(mockTemplate);
        List<Value> expectedPossibleValues = List.of(vf.createLiteral("value1"),
                vf.createLiteral("value2"),
                NTEMPLATE.VALUE_PLACEHOLDER);
        when(mockTemplate.getPossibleValues(any(IRI.class))).thenReturn(expectedPossibleValues);
        when(mockTemplate.isPlaceholder(any(IRI.class))).thenReturn(true);

        RestrictedChoice restrictedChoice = new RestrictedChoice(vf.createIRI("https://knowledgepixels.com/placeholder"), mockContext);
        assertTrue(restrictedChoice.hasPossibleRefValues());
    }

    @Test
    void hasFixedPossibleValue() {
        TemplateContext mockContext = mock(TemplateContext.class);
        Template mockTemplate = mock(Template.class);
        when(mockContext.getTemplate()).thenReturn(mockTemplate);
        List<Value> expectedPossibleValues = List.of(vf.createLiteral("value1"),
                vf.createLiteral("value2"),
                NTEMPLATE.VALUE_PLACEHOLDER);
        when(mockTemplate.getPossibleValues(any(IRI.class))).thenReturn(expectedPossibleValues);
        when(mockTemplate.isPlaceholder(any(IRI.class))).thenReturn(true);

        RestrictedChoice restrictedChoice = new RestrictedChoice(vf.createIRI("https://knowledgepixels.com/placeholder"), mockContext);
        assertTrue(restrictedChoice.hasFixedPossibleValue("\"value1\""));
        assertFalse(restrictedChoice.hasFixedPossibleValue("\"value3\""));
    }

}