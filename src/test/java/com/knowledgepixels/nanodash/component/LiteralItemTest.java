package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.UnificationException;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
    void htmlLiteralIsRendered() {
        Literal literal = TestUtils.vf.createLiteral("<p>Hello <em>world</em></p>", RDF.HTML);
        LiteralItem item = new LiteralItem("literalItem", null, literal, null);

        tester.startComponentInPage(item);
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("<p>Hello <em>world</em></p>"), html);
        assertFalse(html.contains("&lt;p&gt;"), html);
    }

    @Test
    void htmlLiteralIsSanitized() {
        Literal literal = TestUtils.vf.createLiteral("<p onclick=\"alert('x')\">Hi</p><script>alert('x')</script>", RDF.HTML);
        LiteralItem item = new LiteralItem("literalItem", null, literal, null);

        tester.startComponentInPage(item);
        String html = tester.getLastResponseAsString();
        assertFalse(html.contains("onclick"), html);
        assertFalse(html.contains("<script>"), html);
        assertTrue(html.contains("<p>Hi</p>"), html);
    }

    @Test
    void plainLiteralWithMarkupIsEscapedAndQuoted() {
        Literal literal = TestUtils.vf.createLiteral("<p>not html</p>");
        LiteralItem item = new LiteralItem("literalItem", null, literal, null);

        tester.startComponentInPage(item);
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("&quot;&lt;p&gt;not html&lt;/p&gt;&quot;"),
                "escaped, and still shown in quotes as any other literal: " + html);
        assertFalse(html.contains("<p>not html</p>"), html);
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