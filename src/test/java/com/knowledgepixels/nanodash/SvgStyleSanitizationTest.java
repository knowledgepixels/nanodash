package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanitizing SVG whose paint sits in style attributes, as drawing tools export it. The
 * sanitizer allows no style attribute, so the safe declarations are rewritten into the
 * equivalent presentation attributes first — without them every shape falls back to the
 * SVG default fill and the figure renders all black.
 */
class SvgStyleSanitizationTest {

    @Test
    void styleFillBecomesPresentationAttribute() {
        String out = Utils.sanitizeSvg("<svg viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" style=\"fill:rgb(120,184,134);\"/></svg>");
        assertTrue(out.contains("fill=\"rgb(120,184,134)\""), out);
        assertFalse(out.contains("style"), out);
    }

    @Test
    void severalDeclarationsAreConverted() {
        String out = Utils.sanitizeSvg("<svg style=\"fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;\"><path d=\"M0,0\" style=\"fill:#3f3f3f;fill-opacity:0.5\"/></svg>");
        assertTrue(out.contains("fill-rule=\"evenodd\""), out);
        assertTrue(out.contains("clip-rule=\"evenodd\""), out);
        assertTrue(out.contains("stroke-linejoin=\"round\""), out);
        assertTrue(out.contains("stroke-miterlimit=\"2\""), out);
        assertTrue(out.contains("fill=\"#3f3f3f\""), out);
        assertTrue(out.contains("fill-opacity=\"0.5\""), out);
    }

    @Test
    void singleQuotedStyleIsConverted() {
        String out = Utils.sanitizeSvg("<svg viewBox='0 0 10 10'><circle cx='5' cy='5' r='4' style='fill:red'/></svg>");
        assertTrue(out.contains("fill=\"red\""), out);
    }

    @Test
    void referencesAndFunctionsAreDropped() {
        String out = Utils.sanitizeSvg("<svg><rect width=\"10\" height=\"10\" style=\"fill:url(#evil);stroke:expression(alert(1));opacity:0.5\"/></svg>");
        assertFalse(out.contains("url("), out);
        assertFalse(out.contains("expression"), out);
        // The safe declaration in the same attribute still survives.
        assertTrue(out.contains("opacity=\"0.5\""), out);
    }

    @Test
    void unlistedPropertiesAreDropped() {
        String out = Utils.sanitizeSvg("<svg><rect width=\"10\" height=\"10\" style=\"position:absolute;behavior:url(x.htc);fill:blue\"/></svg>");
        assertFalse(out.contains("position"), out);
        assertFalse(out.contains("behavior"), out);
        assertTrue(out.contains("fill=\"blue\""), out);
    }

    @Test
    void presentationAttributesStillWork() {
        String out = Utils.sanitizeSvg("<svg><circle cx=\"5\" cy=\"5\" r=\"4\" fill=\"#ffd400\"/></svg>");
        assertTrue(out.contains("fill=\"#ffd400\""), out);
    }

    @Test
    void styleInHtmlDoesNotSurviveAsStyling() {
        String out = Utils.sanitizeHtml("<p style=\"fill:red;position:absolute\">text</p>");
        assertFalse(out.contains("style"), out);
        assertFalse(out.contains("position"), out);
        assertTrue(out.contains("text"), out);
    }

    @Test
    void inlineSvgInHtmlKeepsItsPaint() {
        String out = Utils.sanitizeHtml("<p>see <svg viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" style=\"fill:green\"/></svg></p>");
        assertTrue(out.contains("fill=\"green\""), out);
    }

}
