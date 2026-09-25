package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.jsonld.JSONLDMode;
import org.eclipse.rdf4j.rio.jsonld.JSONLDSettings;
import org.nanopub.Nanopub;
import org.nanopub.NanopubWithNs;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a resource page has to offer as RDF (issue #710): the download page's view of the
 * resource, plus the nanopublications that declare the resource itself, whose assertions
 * are small enough to embed in the page.
 *
 * @param type         the download page's {@code type}: user, space, resource or part
 * @param id           the resource IRI
 * @param contextId    the containing resource for a part, null otherwise
 * @param declarations the nanopublications declaring the resource; empty when unknown
 */
public record RdfSource(String type, String id, String contextId, List<Nanopub> declarations) {

    /**
     * Prefixes a nanopublication declares for its own URI space, which mean nothing outside it.
     */
    private static final List<String> NANOPUB_LOCAL_PREFIXES = List.of("this", "sub");

    /**
     * The download page parameters for one representation of this source.
     *
     * @param variant the representation
     * @return the parameters
     */
    public PageParameters downloadParameters(RdfNegotiation.Variant variant) {
        PageParameters params = new PageParameters()
                .set("type", type)
                .set("id", id);
        if (contextId != null) params.set("context", contextId);
        params.set("format", variant.format());
        if (variant.assertionsOnly()) params.set("assertions", "");
        return params;
    }

    /**
     * The absolute download URL for one representation of this source, on the configured
     * website address and without any session id.
     *
     * @param variant the representation
     * @return the URL
     */
    public String downloadUrl(RdfNegotiation.Variant variant) {
        return Utils.absolutePageUrl(DownloadRdfPage.class, downloadParameters(variant));
    }

    /**
     * The declaring assertions as a JSON-LD document for a {@code <script>} block in the
     * page head, so that HTML-reading tools and search engines find the resource's own
     * triples. A {@code void:dataDump} triple points them at the complete download.
     * <p>
     * The document keeps the nanopublications' own prefixes as its context, identifies
     * the resource by its IRI rather than by the page's URL, and has every {@code <}
     * escaped, so that a text value cannot close the script block it sits in.
     *
     * @param dumpUrl the URL of the complete TriG download
     * @return the JSON-LD document, or null when there is nothing to declare
     */
    public String toEmbeddedJsonLd(String dumpUrl) {
        if (declarations.isEmpty()) return null;
        Model model = new LinkedHashModel();
        Map<String, String> namespaces = new LinkedHashMap<>();
        for (Nanopub np : declarations) {
            if (np instanceof NanopubWithNs withNs) {
                withNs.getNs().forEach((prefix, ns) -> {
                    if (!NANOPUB_LOCAL_PREFIXES.contains(prefix)) namespaces.put(prefix, ns);
                });
            }
            for (Statement st : np.getAssertion()) {
                model.add(st.getSubject(), st.getPredicate(), st.getObject());
            }
        }
        if (model.isEmpty()) return null;
        namespaces.putIfAbsent("void", VOID.NAMESPACE);
        model.add(Utils.vf.createIRI(id), VOID.DATA_DUMP, Utils.vf.createIRI(dumpUrl));

        StringWriter out = new StringWriter();
        RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, out);
        writer.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
        writer.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
        writer.startRDF();
        namespaces.forEach(writer::handleNamespace);
        model.forEach(writer::handleStatement);
        writer.endRDF();
        return out.toString().replace("<", "\\u003c");
    }

}
