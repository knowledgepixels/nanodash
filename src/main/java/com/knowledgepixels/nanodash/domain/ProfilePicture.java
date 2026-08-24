package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.Utils;
import org.eclipse.rdf4j.model.util.Values;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * A profile picture of a user, space, or maintained resource, declared as
 * {@code schema:image}. The object of that triple can be either a link to an image file or
 * a literal holding the SVG markup itself (issue #634); both end up here as a ready-to-use
 * image source, so that every place showing a picture stays a plain {@code <img>} — with
 * the tilted-square mask on user icons, the object-fit rules on resource pictures, and the
 * sizing in list rows all working unchanged for either kind.
 * <p>
 * SVG markup is reduced to the same static subset as an SVG view's output before being
 * embedded ({@link Utils#sanitizeSvg}), and it travels as a {@code data:} URI rather than
 * as inline markup: inside an {@code <img>} an SVG document cannot script or fetch anything
 * regardless, so the picture of one resource can never reach into the page showing it.
 */
public class ProfilePicture implements Serializable {

    private static final Pattern SVG_ROOT = Pattern.compile("<svg\\b", Pattern.CASE_INSENSITIVE);
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

    private final String src;
    private final boolean svg;

    private ProfilePicture(String src, boolean svg) {
        this.src = src;
        this.svg = svg;
    }

    /**
     * Builds a picture from the value of a {@code schema:image} triple, which the declaring
     * templates do not constrain: an IRI pointing at an image file, or a literal holding SVG
     * markup. Anything else (a plain literal, a malformed IRI, SVG markup of which nothing
     * survives sanitizing) yields null, so callers can treat "no usable picture" and "none
     * declared" alike.
     *
     * @param value the raw value of the triple
     * @return the picture, or null if the value is not usable as one
     */
    public static ProfilePicture of(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.startsWith("<") && SVG_ROOT.matcher(trimmed).find()) {
            String sanitized = Utils.sanitizeSvg(trimmed);
            if (!SVG_ROOT.matcher(sanitized).find()) return null;
            // An <img> loads its SVG as XML, where the namespace declaration is not optional
            // (an author writing markup for inline use may well have left it out).
            if (!sanitized.contains("xmlns")) {
                sanitized = SVG_ROOT.matcher(sanitized)
                        .replaceFirst("<svg xmlns=\"" + SVG_NAMESPACE + "\"");
            }
            String encoded = Base64.getEncoder().encodeToString(sanitized.getBytes(StandardCharsets.UTF_8));
            return new ProfilePicture("data:image/svg+xml;base64," + encoded, true);
        }
        try {
            Values.iri(trimmed);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return new ProfilePicture(trimmed, false);
    }

    /**
     * The value to use as an {@code <img>} source: the declared URL, or a {@code data:} URI
     * carrying the sanitized SVG.
     *
     * @return the image source
     */
    public String getSrc() {
        return src;
    }

    /**
     * Whether the picture was declared as SVG markup rather than as a link.
     *
     * @return true for an SVG literal
     */
    public boolean isSvg() {
        return svg;
    }

    @Override
    public String toString() {
        return src;
    }

}
