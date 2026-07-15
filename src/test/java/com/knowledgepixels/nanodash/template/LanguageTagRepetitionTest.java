package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests that removing a repetition group shifts the language-tag models of
 * language-tag-selectable placeholders alongside the text models
 * (StatementItem.RepetitionGroup.remove(), docs/language-tag-picker.md).
 */
public class LanguageTagRepetitionTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI COMMENT = vf.createIRI(NP_URI + "/comment");
    private static final IRI COMMENT_REP1 = vf.createIRI(NP_URI + "/comment__1");

    private MockedStatic<TemplateData> templateDataMockedStatic;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    private TemplateContext repeatableContext() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Repetition test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(ST1, RDF.OBJECT, COMMENT);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT, RDFS.LABEL, vf.createLiteral("comment"));
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    @SuppressWarnings("unchecked")
    private IModel<Object> model(TemplateContext context, IRI key) {
        return (IModel<Object>) context.getComponentModels().get(key);
    }

    private void removeRepetitionGroup(StatementItem si, int index) throws Exception {
        Field rgsField = StatementItem.class.getDeclaredField("repetitionGroups");
        rgsField.setAccessible(true);
        List<?> rgs = (List<?>) rgsField.get(si);
        Object rg = rgs.get(index);
        Method removeMethod = rg.getClass().getDeclaredMethod("remove");
        removeMethod.setAccessible(true);
        removeMethod.invoke(rg);
    }

    @Test
    void removingRepetitionShiftsTextAndLanguageModels() throws Exception {
        TemplateContext context = repeatableContext();
        StatementItem si = context.getStatementItems().get(0);
        si.addRepetitionGroup();
        assertEquals(2, si.getRepetitionCount());

        model(context, COMMENT).setObject("Haus");
        context.getComponentModels().put(TemplateContext.getLanguageModelKey(COMMENT), Model.of("de"));
        model(context, COMMENT_REP1).setObject("maison");
        context.getComponentModels().put(TemplateContext.getLanguageModelKey(COMMENT_REP1), Model.of("fr"));

        removeRepetitionGroup(si, 0);

        assertEquals("maison", model(context, COMMENT).getObject());
        assertEquals("fr", model(context, TemplateContext.getLanguageModelKey(COMMENT)).getObject(),
                "language model must shift together with the text model");
        assertNull(model(context, COMMENT_REP1).getObject());
        assertNull(model(context, TemplateContext.getLanguageModelKey(COMMENT_REP1)).getObject());
    }

    @Test
    void removingLastRepetitionClearsBothModels() throws Exception {
        TemplateContext context = repeatableContext();
        StatementItem si = context.getStatementItems().get(0);
        si.addRepetitionGroup();

        model(context, COMMENT).setObject("Haus");
        context.getComponentModels().put(TemplateContext.getLanguageModelKey(COMMENT), Model.of("de"));
        model(context, COMMENT_REP1).setObject("maison");
        context.getComponentModels().put(TemplateContext.getLanguageModelKey(COMMENT_REP1), Model.of("fr"));

        removeRepetitionGroup(si, 1);

        assertEquals("Haus", model(context, COMMENT).getObject());
        assertEquals("de", model(context, TemplateContext.getLanguageModelKey(COMMENT)).getObject());
        assertNull(model(context, COMMENT_REP1).getObject());
        assertNull(model(context, TemplateContext.getLanguageModelKey(COMMENT_REP1)).getObject());
    }

}
