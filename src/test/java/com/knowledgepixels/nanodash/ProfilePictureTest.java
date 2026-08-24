package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.ProfilePicture;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePictureTest {

    private static String decodeSvg(ProfilePicture picture) {
        String prefix = "data:image/svg+xml;base64,";
        assertTrue(picture.getSrc().startsWith(prefix), picture.getSrc());
        return new String(Base64.getDecoder().decode(picture.getSrc().substring(prefix.length())),
                StandardCharsets.UTF_8);
    }

    @Test
    void linkIsUsedAsIs() {
        ProfilePicture picture = ProfilePicture.of("https://example.com/logo.png");
        assertNotNull(picture);
        assertFalse(picture.isSvg());
        assertEquals("https://example.com/logo.png", picture.getSrc());
    }

    @Test
    void surroundingWhitespaceIsIgnored() {
        assertEquals("https://example.com/logo.png",
                ProfilePicture.of("  https://example.com/logo.png\n").getSrc());
    }

    @Test
    void svgLiteralBecomesDataUri() {
        ProfilePicture picture = ProfilePicture.of(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" fill=\"red\"/></svg>");
        assertNotNull(picture);
        assertTrue(picture.isSvg());
        String svg = decodeSvg(picture);
        assertTrue(svg.contains("<rect"), svg);
        assertTrue(svg.contains("</rect>"), svg);  // self-closed tags are expanded
        assertTrue(svg.contains("viewBox"), svg);
    }

    @Test
    void missingNamespaceIsAdded() {
        // Without it the browser refuses to render the data: URI as an image.
        String svg = decodeSvg(ProfilePicture.of("<svg viewBox=\"0 0 10 10\"><circle cx=\"5\" cy=\"5\" r=\"4\"/></svg>"));
        assertTrue(svg.contains("xmlns=\"http://www.w3.org/2000/svg\""), svg);
    }

    @Test
    void scriptingIsStrippedFromSvg() {
        String svg = decodeSvg(ProfilePicture.of(
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script>"
                        + "<rect width=\"10\" height=\"10\" onload=\"alert(2)\"/></svg>"));
        assertFalse(svg.contains("script"), svg);
        assertFalse(svg.contains("alert"), svg);
        assertFalse(svg.contains("onload"), svg);
    }

    @Test
    void unusableValuesYieldNull() {
        assertNull(ProfilePicture.of(null));
        assertNull(ProfilePicture.of("   "));
        assertNull(ProfilePicture.of("just a label"));
        assertNull(ProfilePicture.of("<p>not an image</p>"));
    }

}
