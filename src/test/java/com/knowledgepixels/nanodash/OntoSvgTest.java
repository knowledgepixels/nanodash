package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Serializing the RDF description of an SVG image, as a CONSTRUCT view query returns it,
 * into the markup an SVG view renders (issue #592).
 */
class OntoSvgTest {

    private static final String PREFIXES = """
            prefix doc: <https://example.org/doc/id>
            prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            prefix svg: <http://www.w3.org/SVG/model/def/>
            prefix xml: <http://www.w3.org/XML/model/def/>
            prefix xlink: <https://www.w3.org/1999/xlink/model/def/>
            """;

    private static Model parse(String turtleBody) {
        try {
            return Rio.parse(new StringReader(PREFIXES + turtleBody), "", RDFFormat.TURTLE);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String serializeOne(String turtleBody) {
        List<String> figures = OntoSvg.toSvgMarkup(parse(turtleBody));
        assertEquals(1, figures.size(), "expected exactly one figure, got " + figures);
        return figures.getFirst();
    }

    // The smiley from the OntoSVG repository, whose published SVG rendering is a 200x200
    // yellow face: head, two eyes, and a mouth path, in that order.
    private static final String SMILEY = """
            doc:1 a svg:Document ;
                rdf:_1 doc:1.1 ;
                rdf:_2 doc:1.2 .
            doc:1.1 rdf:type xml:DocumentType ;
                xml:documentTypeName 'svg' .
            doc:1.2 a svg:Svg ;
                rdf:_1 doc:10.0 ; rdf:_2 doc:10.1 ; rdf:_3 doc:10.2 ; rdf:_4 doc:10.3 ;
                rdf:_5 doc:10.4 ; rdf:_6 doc:10.5 ; rdf:_7 doc:10.6 ; rdf:_8 doc:10.7 ;
                rdf:_9 doc:10.8 ; rdf:_10 doc:10.9 ; rdf:_11 doc:10.10 ; rdf:_12 doc:10.11 ;
                rdf:_13 doc:10.12 ; rdf:_14 doc:10.13 ; rdf:_15 doc:10.14 ;
                xml:xmlns "http://www.w3.org/2000/svg" ;
                svg:height "200" ;
                svg:width "200" .
            doc:10.0 a svg:Text ; xml:fragment "" .
            doc:10.1 a svg:Text ; xml:fragment "" .
            doc:10.2 a svg:Text ; xml:fragment "" .
            doc:10.3 a svg:Circle ;
                svg:cx "100" ; svg:cy "100" ; svg:fill "yellow" ; svg:r "90" ;
                svg:stroke "black" ; svg:stroke-width "2" .
            doc:10.4 a svg:Text ; xml:fragment "" .
            doc:10.5 a svg:Text ; xml:fragment "" .
            doc:10.6 a svg:Text ; xml:fragment "" .
            doc:10.7 a svg:Circle ;
                svg:cx "70" ; svg:cy "70" ; svg:fill "black" ; svg:r "10" .
            doc:10.8 a svg:Text ; xml:fragment "" .
            doc:10.9 a svg:Circle ;
                svg:cx "130" ; svg:cy "70" ; svg:fill "black" ; svg:r "10" .
            doc:10.10 a svg:Text ; xml:fragment "" .
            doc:10.11 a svg:Text ; xml:fragment "" .
            doc:10.12 a svg:Text ; xml:fragment "" .
            doc:10.13 a svg:Path ;
                svg:d "M 60 120 Q 100 150 140 120" ; svg:fill "none" ;
                svg:stroke "black" ; svg:stroke-width "3" .
            doc:10.14 a svg:Text ; xml:fragment "" .
            """;

    @Test
    void serializesTheReferenceSmiley() {
        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"200\" width=\"200\">"
                        + "<circle cx=\"100\" cy=\"100\" fill=\"yellow\" r=\"90\" stroke=\"black\" stroke-width=\"2\"></circle>"
                        + "<circle cx=\"70\" cy=\"70\" fill=\"black\" r=\"10\"></circle>"
                        + "<circle cx=\"130\" cy=\"70\" fill=\"black\" r=\"10\"></circle>"
                        + "<path d=\"M 60 120 Q 100 150 140 120\" fill=\"none\" stroke=\"black\" stroke-width=\"3\"></path>"
                        + "</svg>",
                serializeOne(SMILEY));
    }

    @Test
    void serializedSmileySurvivesSanitizationUnchanged() {
        String markup = serializeOne(SMILEY);
        String sanitized = Utils.sanitizeSvg(markup);
        assertTrue(sanitized.contains("<path d=\"M 60 120 Q 100 150 140 120\""), sanitized);
        assertEquals(3, sanitized.split("<circle", -1).length - 1, sanitized);
    }

    @Test
    void childrenAreOrderedNumericallyNotLexically() {
        String markup = serializeOne("""
                doc:s a svg:Svg ;
                    rdf:_1 doc:a ; rdf:_2 doc:b ; rdf:_10 doc:c .
                doc:a a svg:TextElement ; rdf:_1 doc:at .
                doc:b a svg:TextElement ; rdf:_1 doc:bt .
                doc:c a svg:TextElement ; rdf:_1 doc:ct .
                doc:at a svg:Text ; xml:fragment "first" .
                doc:bt a svg:Text ; xml:fragment "second" .
                doc:ct a svg:Text ; xml:fragment "tenth" .
                """);
        assertEquals("<svg><text>first</text><text>second</text><text>tenth</text></svg>", markup);
    }

    @Test
    void textElementIsTheElementAndTextIsCharacterData() {
        String markup = serializeOne("""
                doc:s a svg:Svg ; rdf:_1 doc:label .
                doc:label a svg:TextElement ;
                    svg:x "10" ; svg:y "20" ;
                    rdf:_1 doc:content .
                doc:content a svg:Text ; xml:fragment "Hello" .
                """);
        assertEquals("<svg><text x=\"10\" y=\"20\">Hello</text></svg>", markup);
    }

    @Test
    void hyphenatedTagNamesComeFromTheVocabulary() {
        String markup = serializeOne("""
                doc:s a svg:Svg ; rdf:_1 doc:ff .
                doc:ff a svg:FontFace .
                """);
        assertEquals("<svg><font-face></font-face></svg>", markup);
    }

    @Test
    void xlinkHrefBecomesTheHrefThatSurvivesSanitization() {
        String markup = serializeOne("""
                doc:s a svg:Svg ; rdf:_1 doc:link .
                doc:link a svg:A ;
                    xlink:href "https://example.org/thing" ;
                    rdf:_1 doc:t .
                doc:t a svg:Text ; xml:fragment "label" .
                """);
        assertEquals("<svg><a href=\"https://example.org/thing\">label</a></svg>", markup);
        assertTrue(Utils.sanitizeSvg(markup).contains("href=\"https://example.org/thing\""));
    }

    @Test
    void dataDerivedTextAndAttributesAreEscaped() {
        String markup = serializeOne("""
                doc:s a svg:Svg ; rdf:_1 doc:label .
                doc:label a svg:TextElement ;
                    svg:font-family "a\\"b" ;
                    rdf:_1 doc:t .
                doc:t a svg:Text ; xml:fragment "<script>alert(1)</script> & more" .
                """);
        assertFalse(markup.contains("<script>"), markup);
        assertTrue(markup.contains("&lt;script&gt;"), markup);
        assertTrue(markup.contains("&amp; more"), markup);
        assertTrue(markup.contains("font-family=\"a&quot;b\""), markup);
    }

    @Test
    void nestedSvgIsRenderedInPlaceAndNotAlsoAsItsOwnFigure() {
        String markup = serializeOne("""
                doc:outer a svg:Svg ; rdf:_1 doc:inner .
                doc:inner a svg:Svg ; svg:width "10" .
                """);
        assertEquals("<svg><svg width=\"10\"></svg></svg>", markup);
    }

    @Test
    void severalFiguresAreReturnedInAStableOrder() {
        List<String> figures = OntoSvg.toSvgMarkup(parse("""
                doc:b a svg:Svg ; svg:width "2" .
                doc:a a svg:Svg ; svg:width "1" .
                """));
        assertEquals(List.of("<svg width=\"1\"></svg>", "<svg width=\"2\"></svg>"), figures);
    }

    @Test
    void unknownClassesAndPropertiesAreLeftOut() {
        String markup = serializeOne("""
                doc:s a svg:Svg ;
                    rdf:_1 doc:x ;
                    <https://example.org/vocab#note> "not an svg attribute" .
                doc:x a <https://example.org/vocab#Widget> .
                """);
        assertEquals("<svg></svg>", markup);
    }

    @Test
    void aCycleInTheChildLinksTerminates() {
        String markup = serializeOne("""
                doc:s a svg:Svg ; rdf:_1 doc:g1 .
                doc:g1 a svg:G ; rdf:_1 doc:g2 .
                doc:g2 a svg:G ; rdf:_1 doc:g1 .
                """);
        assertEquals("<svg><g><g></g></g></svg>", markup);
    }

    @Test
    void anEmptyOrAbsentModelYieldsNoFigures() {
        assertTrue(OntoSvg.toSvgMarkup(null).isEmpty());
        assertTrue(OntoSvg.toSvgMarkup(parse("doc:x a <https://example.org/vocab#Widget> .")).isEmpty());
    }

}
