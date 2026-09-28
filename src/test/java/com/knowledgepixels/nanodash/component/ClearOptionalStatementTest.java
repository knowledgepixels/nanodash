package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.NanopubImpl;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteCategory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests clearing an optional statement in one step (issue #145), on the "Announcing a paper
 * that I have read" template from the nanopub test suite. Its optional statement
 * {@code paper rdfs:comment comment} shares {@code paper} with the required statement and has
 * {@code comment} to itself.
 */
class ClearOptionalStatementTest {

    private static final String HAS_READ_TEMPLATE = "RAEzjboN_ipuwsFnvhqAl9Fy7fZl34cGZ1QhJ-7cbbGGM";

    private static final String TEMPLATE_URI = "https://w3id.org/np/" + HAS_READ_TEMPLATE;

    private static final IRI PAPER = Utils.vf.createIRI(TEMPLATE_URI + "/paper");

    private static final IRI COMMENT = Utils.vf.createIRI(TEMPLATE_URI + "/comment");

    private TemplateContext context;

    private StatementItem optionalStatement;

    /**
     * Builds the form context of the template, taken from the nanopub test suite, and fills
     * in both of its placeholders.
     *
     * @throws Exception if the template cannot be read from the test suite
     */
    @BeforeEach
    void setUp() throws Exception {
        new WicketTester(new WicketApplication());
        Utils.cacheNanopub(new NanopubImpl(NanopubTestSuite.getLatest()
                .getByArtifactCode(HAS_READ_TEMPLATE, TestSuiteCategory.VALID)
                .orElseThrow(() -> new IllegalStateException("Not in the nanopub test suite: " + HAS_READ_TEMPLATE))
                .toFile()));
        context = new TemplateContext(ContextType.ASSERTION, TEMPLATE_URI, "statement", (String) null);
        context.initStatements();
        optionalStatement = context.getStatementItems().stream()
                .filter(item -> item.getStatementId().stringValue().endsWith("/st2"))
                .findFirst()
                .orElseThrow();
        modelOf(PAPER).setObject("10.3233/DS-170001");
        modelOf(COMMENT).setObject("A comment that should go");
    }

    @SuppressWarnings("unchecked")
    private IModel<String> modelOf(IRI placeholder) {
        return (IModel<String>) context.getComponentModels().computeIfAbsent(placeholder, iri -> Model.of(""));
    }

    /**
     * Only the placeholder the optional statement has to itself is cleared; one it shares with
     * another statement is not its own.
     */
    @Test
    void ownPlaceholdersAreThoseNoOtherStatementUses() {
        assertEquals(List.of(COMMENT), optionalStatement.getRepetitionGroup(0).getOwnPlaceholderIris());
    }

    /**
     * Clearing empties the statement's own placeholder, keeps the shared one, and so leaves
     * the optional statement out of the nanopublication.
     */
    @Test
    void clearingEmptiesOnlyTheStatementsOwnPlaceholders() {
        assertFalse(optionalStatement.hasEmptyElements());
        optionalStatement.getRepetitionGroup(0).clear();
        assertNull(modelOf(COMMENT).getObject());
        assertEquals("10.3233/DS-170001", modelOf(PAPER).getObject());
        assertTrue(optionalStatement.hasEmptyElements());
    }

    /**
     * The clear button shows once the statement's own placeholder is filled, and hides again
     * once it is cleared.
     */
    @Test
    void theButtonShowsOnlyWhileAnOwnPlaceholderIsFilled() {
        StatementItem.RepetitionGroup group = optionalStatement.getRepetitionGroup(0);
        assertTrue(group.hasFilledOwnPlaceholder());
        group.clear();
        assertFalse(group.hasFilledOwnPlaceholder());
    }

    /**
     * A value in a placeholder the statement shares with another one does not count as
     * something to clear.
     */
    @Test
    void aFilledSharedPlaceholderDoesNotShowTheButton() {
        modelOf(COMMENT).setObject("  ");
        assertFalse(optionalStatement.getRepetitionGroup(0).hasFilledOwnPlaceholder());
    }

    /**
     * An optional statement can be cleared while none of its own values is locked.
     */
    @Test
    void anUnlockedStatementCanBeCleared() {
        assertTrue(optionalStatement.getRepetitionGroup(0).canBeCleared());
    }

    /**
     * A value the form was opened with and that the user may not change is not cleared with
     * the rest of its statement.
     */
    @Test
    void aStatementWithALockedValueCannotBeCleared() {
        context.setParam("comment", "A pre-filled comment");
        context.setLocked("comment");
        assertFalse(optionalStatement.getRepetitionGroup(0).canBeCleared());
    }

}
