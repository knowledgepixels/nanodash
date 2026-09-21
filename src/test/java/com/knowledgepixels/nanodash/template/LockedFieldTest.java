package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.apache.wicket.model.IModel;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for locked pre-filled fields (issue #678): locking holds per repetition, and the lock
 * belongs to the pre-filled value rather than to the repetition slot it sits in, so it travels
 * with the value when a repetition group is removed.
 */
public class LockedFieldTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI COMMENT = vf.createIRI(NP_URI + "/comment");
    private static final IRI COMMENT_REP1 = vf.createIRI(NP_URI + "/comment__1");
    private static final IRI COMMENT_REP2 = vf.createIRI(NP_URI + "/comment__2");

    private MockedStatic<TemplateData> templateDataMockedStatic;
    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    /**
     * A template with one repeatable statement holding a single literal placeholder, mirroring
     * the repeatable key group of the "Introducing a user" template.
     */
    private TemplateContext repeatableContext() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Lock test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(ST1, RDF.OBJECT, COMMENT);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT, RDFS.LABEL, vf.createLiteral("comment"));
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        return new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
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

    /**
     * Two pre-filled repetitions produce two repetition groups, so a link can hand the form the
     * values a user already has.
     */
    @Test
    void prefilledRepetitionsProduceTheirOwnGroups() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "first");
        context.setParam("comment__1", "second");
        context.initStatements();
        context.finalizeStatements();
        assertEquals(2, context.getStatementItems().get(0).getRepetitionCount());
    }

    /**
     * The point of the issue: the pre-filled repetitions are locked, but a repetition the user
     * adds afterwards is theirs to fill.
     */
    @Test
    void lockHoldsPerRepetition() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "first");
        context.setParam("comment__1", "second");
        context.setLocked("comment");
        context.setLocked("comment__1");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        si.addRepetitionGroup();

        assertTrue(context.isLocked(COMMENT));
        assertTrue(context.isLocked(COMMENT_REP1));
        assertFalse(context.isLocked(COMMENT_REP2), "a repetition the user adds must stay editable");
    }

    /**
     * Removing a repetition group shifts the values of the following groups up into its slot, so
     * the lock has to shift with them: the value that lands in the locked slot is the user's own
     * and must stay editable.
     */
    @Test
    void removingTheLockedRepetitionUnlocksTheSlot() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "locked value");
        context.setLocked("comment");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        si.addRepetitionGroup();
        model(context, COMMENT_REP1).setObject("user value");

        removeRepetitionGroup(si, 0);

        assertEquals("user value", model(context, COMMENT).getObject());
        assertFalse(context.isLocked(COMMENT), "the user's own value must not inherit the lock of the removed one");
        assertFalse(context.isLocked(COMMENT_REP1));
    }

    /**
     * Removing an unrelated repetition leaves the locked one alone.
     */
    @Test
    void removingAnotherRepetitionKeepsTheLock() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "locked value");
        context.setLocked("comment");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        si.addRepetitionGroup();
        model(context, COMMENT_REP1).setObject("user value");

        removeRepetitionGroup(si, 1);

        assertEquals("locked value", model(context, COMMENT).getObject());
        assertTrue(context.isLocked(COMMENT));
        assertFalse(context.isLocked(COMMENT_REP1));
    }

    /**
     * With the second of three repetitions locked, removing the first has to carry the lock down
     * to the slot the locked value moves into.
     */
    @Test
    void lockFollowsTheValueItBelongsTo() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "free value");
        context.setParam("comment__1", "locked value");
        context.setLocked("comment__1");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        assertEquals(2, si.getRepetitionCount());

        removeRepetitionGroup(si, 0);

        assertEquals("locked value", model(context, COMMENT).getObject());
        assertTrue(context.isLocked(COMMENT), "the lock must travel with the value that moved up");
        assertFalse(context.isLocked(COMMENT_REP1));
    }

    /**
     * A lock stated on the relative repetition name has to end up on the absolute name the value
     * is filed under, or it would never match a placeholder.
     */
    @Test
    void relativeRepetitionNameLockIsCarriedOver() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment__.1", "locked value");
        context.setLocked("comment__.1");
        context.initStatements();
        context.finalizeStatements();

        assertTrue(context.isLocked(COMMENT));
    }

    /**
     * The rendered field carries the pre-filled value and is marked readonly, with a title saying
     * where the value came from. Readonly rather than disabled: browsers submit readonly fields,
     * and a field the browser does not submit is read by the form as an emptied one.
     */
    @Test
    void lockedFieldRendersReadonlyWithItsValue() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "locked value");
        context.setLocked("comment");
        context.initStatements();
        context.finalizeStatements();

        tester.startComponentInPage(context.getStatementItems().get(0));
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("locked value"), html);
        assertTrue(html.contains("readonly=\"readonly\""), html);
        assertTrue(html.contains("locked-value"), html);
        assertTrue(html.contains("cannot be changed here"), html);
    }

    /**
     * The repetition the user adds next to a locked one renders as an ordinary editable field.
     */
    @Test
    void unlockedRepetitionRendersEditable() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "locked value");
        context.setLocked("comment");
        context.initStatements();
        context.finalizeStatements();
        StatementItem si = context.getStatementItems().get(0);
        si.addRepetitionGroup();

        tester.startComponentInPage(si);
        String html = tester.getLastResponseAsString();
        assertEquals(1, html.split("readonly=\"readonly\"", -1).length - 1,
                "only the locked repetition may render as readonly: " + html);
    }

    /**
     * A choice field renders as a {@code select}, which HTML has no readonly for, so it is
     * disabled and its value mirrored in a hidden field of the same name — without that mirror
     * the browser would submit nothing for it and the form would read the field as emptied.
     */
    @Test
    void lockedChoiceFieldIsDisabledWithAHiddenMirror() throws Exception {
        String npUri = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Choice";
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI colour = vf.createIRI(npUri + "/colour");
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Choice lock test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.SEEALSO);
        creator.addAssertionStatement(st1, RDF.OBJECT, colour);
        creator.addAssertionStatement(colour, RDF.TYPE, NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        creator.addAssertionStatement(colour, RDFS.LABEL, vf.createLiteral("colour"));
        creator.addAssertionStatement(colour, NTEMPLATE.POSSIBLE_VALUE, vf.createIRI("http://example.com/red"));
        creator.addAssertionStatement(colour, NTEMPLATE.POSSIBLE_VALUE, vf.createIRI("http://example.com/blue"));
        Template template = new Template(creator.finalizeNanopub());
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(npUri)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, npUri, "statement", (String) null);
        context.setParam("colour", "http://example.com/red");
        context.setLocked("colour");
        context.initStatements();
        context.finalizeStatements();

        tester.startComponentInPage(context.getStatementItems().get(0));
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("disabled=\"disabled\""), html);
        assertTrue(html.contains("type=\"hidden\""), html);
        assertTrue(html.contains("http://example.com/red"), html);
    }

    /**
     * A locked statement keeps the repetitions the form was opened with: the buttons that add and
     * remove them are gone. Hiding them is enough — Wicket does not invoke a listener of a
     * component that is not visible — so unlike a locked value this one is not just a guardrail.
     */
    @Test
    void lockedStatementHidesTheRepetitionButtons() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "first");
        context.setParam("comment__1", "second");
        context.setStatementLocked("st1");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        assertEquals(2, si.getRepetitionCount());
        tester.startComponentInPage(si);
        String html = tester.getLastResponseAsString();
        assertFalse(html.contains(">+<"), "the add-repetition button must be gone: " + html);
        assertFalse(html.contains(">-<"), "the remove-repetition button must be gone: " + html);
        assertTrue(html.contains("first") && html.contains("second"), html);
    }

    /**
     * Without the lock the same statement shows both buttons, so the assertion above is about the
     * lock and not about the markup.
     */
    @Test
    void unlockedStatementShowsTheRepetitionButtons() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "first");
        context.setParam("comment__1", "second");
        context.initStatements();
        context.finalizeStatements();

        tester.startComponentInPage(context.getStatementItems().get(0));
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains(">+<"), html);
        assertTrue(html.contains(">-<"), html);
    }

    /**
     * A statement can also be named by a placeholder that occurs in it and no other, which is the
     * name a link author is more likely to have at hand.
     */
    @Test
    void statementCanBeLockedByOneOfItsPlaceholders() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "first");
        context.setStatementLocked("comment");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        assertTrue(context.isStatementLocked(si.getStatementId()));
        tester.startComponentInPage(si);
        assertFalse(tester.getLastResponseAsString().contains(">+<"));
    }

    /**
     * A name that matches neither the statement nor one of its placeholders locks nothing.
     */
    @Test
    void unrelatedStatementLockDoesNothing() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "first");
        context.setStatementLocked("nosuchthing");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        assertFalse(context.isStatementLocked(si.getStatementId()));
        tester.startComponentInPage(si);
        assertTrue(tester.getLastResponseAsString().contains(">+<"));
    }

    /**
     * Locking the value and locking the statement are independent: a locked value in a statement
     * that is not locked can still be dropped by removing its repetition, which is the split the
     * two parameters are there to make.
     */
    @Test
    void valueLockAndStatementLockAreIndependent() throws Exception {
        TemplateContext context = repeatableContext();
        context.setParam("comment", "locked value");
        context.setParam("comment__1", "free value");
        context.setLocked("comment");
        context.initStatements();
        context.finalizeStatements();

        StatementItem si = context.getStatementItems().get(0);
        assertFalse(context.isStatementLocked(si.getStatementId()));
        tester.startComponentInPage(si);
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("readonly=\"readonly\""), html);
        assertTrue(html.contains(">-<"), "a locked value must not by itself fix the repetitions: " + html);
    }

    /**
     * Locking a name the form has no value for would leave a field empty and uneditable, and a
     * required one would make the form unpublishable.
     */
    @Test
    void lockWithoutValueIsIgnored() throws Exception {
        TemplateContext context = repeatableContext();
        context.setLocked("comment");
        assertFalse(context.isLocked(COMMENT));
    }

}
