package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.UnificationException;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiteralItemTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @Test
    void testLiteralItemRendersLiteral() {
        Literal literal = TestUtils.vf.createLiteral("hello world");
        LiteralItem item = new LiteralItem("literalItem", null, literal, null);

        assertNotNull(item.get(LiteralItem.LABEL_ID));
        tester.startComponentInPage(item);
        tester.assertComponent("literalItem", LiteralItem.class);
    }

    @Test
    void isUnifiableWithReturnsTrueForSameLiteralValue() {
        Literal lit1 = TestUtils.vf.createLiteral("value");
        Literal lit2 = TestUtils.vf.createLiteral("value");
        LiteralItem item = new LiteralItem("id", "parent", lit1, null);

        assertTrue(item.isUnifiableWith(lit2));
    }

    @Test
    void isUnifiableWithReturnsFalseForDifferentLiteralValue() {
        Literal lit1 = TestUtils.vf.createLiteral("value1");
        Literal lit2 = TestUtils.vf.createLiteral("value2");
        LiteralItem item = new LiteralItem("id", "parent", lit1, null);

        assertFalse(item.isUnifiableWith(lit2));
    }

    @Test
    void isUnifiableWithReturnsFalseForNonLiteralValue() {
        Literal lit1 = TestUtils.vf.createLiteral("value");
        Value nonLiteral = TestUtils.vf.createIRI("https://example.org");
        LiteralItem item = new LiteralItem("id", "parent", lit1, null);

        assertFalse(item.isUnifiableWith(nonLiteral));
    }

    @Test
    void isUnifiableWithReturnsFalseForNullValue() {
        Literal lit1 = TestUtils.vf.createLiteral("value");
        LiteralItem item = new LiteralItem("id", "parent", lit1, null);

        assertFalse(item.isUnifiableWith(null));
    }

    @Test
    void unifyWithDoesNotThrowForUnifiableLiteral() {
        Literal lit1 = TestUtils.vf.createLiteral("value");
        Literal lit2 = TestUtils.vf.createLiteral("value");
        LiteralItem item = new LiteralItem("id", "parent", lit1, null);

        assertDoesNotThrow(() -> item.unifyWith(lit2));
    }

    @Test
    void unifyWithThrowsForNonUnifiableLiteral() {
        Literal lit1 = TestUtils.vf.createLiteral("value1");
        Literal lit2 = TestUtils.vf.createLiteral("value2");
        LiteralItem item = new LiteralItem("id", "parent", lit1, null);

        assertThrows(UnificationException.class, () -> item.unifyWith(lit2));
    }

    @Test
    void toStringReturnsCorrectFormatForNonEmptyLiteral() {
        Literal literal = TestUtils.vf.createLiteral("example");
        LiteralItem item = new LiteralItem("id", "parent", literal, null);

        assertEquals("[Literal item: example]", item.toString());
    }

    @Test
    void toStringReturnsCorrectFormatForEmptyLiteral() {
        Literal literal = TestUtils.vf.createLiteral("");
        LiteralItem item = new LiteralItem("id", "parent", literal, null);

        assertEquals("[Literal item: ]", item.toString());
    }

}