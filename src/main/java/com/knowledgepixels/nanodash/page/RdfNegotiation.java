package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import org.commonjava.mimeparse.MIMEParse;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides, from an HTTP {@code Accept} header, whether a resource page should answer with
 * RDF instead of HTML, and in which of the download page's formats (issue #710).
 * <p>
 * Graph-aware formats carry the complete nanopublications, as the download tab's top
 * section does. Triple-only formats cannot hold named graphs, so they carry the merged
 * assertions instead.
 */
public final class RdfNegotiation {

    /**
     * One RDF representation the download page can serve.
     *
     * @param mediaType      the media type a client asks for, and the download page answers with
     * @param format         the download page's {@code format} parameter
     * @param assertionsOnly whether the download page's {@code assertions} switch is set
     */
    public record Variant(String mediaType, String format, boolean assertionsOnly) {
    }

    /**
     * The representations offered, in the order a link list should show them.
     */
    public static final List<Variant> VARIANTS = List.of(
            new Variant(Utils.TYPE_TRIG, "trig", false),
            new Variant(Utils.TYPE_NQUADS, "nq", false),
            new Variant(Utils.TYPE_JSONLD, "jsonld", false),
            new Variant(Utils.TYPE_TRIX, "trix", false),
            new Variant("text/turtle", "turtle", true),
            new Variant("application/n-triples", "nt", true),
            new Variant("application/rdf+xml", "rdfxml", true)
    );

    /**
     * The types offered to the media-type matcher. HTML comes last on purpose: the matcher
     * breaks ties in favour of the last entry, so a wildcard such as {@code *}{@code /}{@code *}
     * from a command-line client or a browser resolves to the page, not to RDF.
     */
    private static final List<String> OFFERED_TYPES;

    static {
        List<String> types = new ArrayList<>();
        for (Variant v : VARIANTS) types.add(v.mediaType());
        types.add(Utils.TYPE_HTML);
        OFFERED_TYPES = List.copyOf(types);
    }

    private RdfNegotiation() {
    }

    /**
     * Picks the RDF representation a client asked for.
     *
     * @param acceptHeader the request's {@code Accept} header; may be null or blank
     * @return the variant to serve, or null when the client gets HTML, which it does when
     * it asks for it, when it accepts anything, when it names no type this page offers, or
     * when the header cannot be parsed
     */
    public static Variant negotiate(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) return null;
        String best;
        try {
            best = MIMEParse.bestMatch(OFFERED_TYPES, acceptHeader);
        } catch (Exception ex) {
            return null;
        }
        if (best == null || best.isEmpty() || best.equals(Utils.TYPE_HTML)) return null;
        for (Variant v : VARIANTS) {
            if (v.mediaType().equals(best)) return v;
        }
        return null;
    }

}
