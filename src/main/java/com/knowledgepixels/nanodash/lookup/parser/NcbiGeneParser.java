package com.knowledgepixels.nanodash.lookup.parser;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.knowledgepixels.nanodash.lookup.JsonLookupApi;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parser for the gene summaries of the NCBI E-utilities, e.g.
 * {@code https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gene&retmode=json&id=}.
 * Genes are looked up by their numeric NCBI Gene ID, not by name; the URL needs
 * {@code retmode=json}, as the E-utilities answer in XML otherwise. A gene's value is its
 * identifiers.org URI, and its label is its symbol, followed by its description and organism.
 */
public class NcbiGeneParser extends JsonLookupApi {

    static final String GENE_PREFIX = "https://identifiers.org/ncbigene:";

    private static final Pattern GENE_ID = Pattern.compile("[0-9]+");

    /**
     * {@inheritDoc}
     * <p>
     * A search term that is not a gene ID is answered with an error message next to an empty
     * result, not with an error status, so it finds nothing rather than failing.
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONObject result = new JSONObject(response).getJSONObject("result");
        JSONArray uids = result.getJSONArray("uids");
        for (int i = 0; i < uids.length(); i++) {
            String uid = uids.getString(i);
            JSONObject gene = result.optJSONObject(uid);
            // An ID that looks valid but names no gene gets an entry with only an error message.
            if (!GENE_ID.matcher(uid).matches() || gene == null || gene.has("error")) continue;
            addIfNew(GENE_PREFIX + uid, labelOf(gene, uid), labels, values);
        }
    }

    private static String labelOf(JSONObject gene, String uid) {
        String symbol = textOf(gene, "name");
        String label = symbol == null ? "Gene " + uid : symbol;
        String description = textOf(gene, "description");
        JSONObject organism = gene.optJSONObject("organism");
        String organismName = organism == null ? null : textOf(organism, "scientificname");
        if (description != null) label += " - " + description;
        if (organismName != null) label += " (" + organismName + ")";
        return label;
    }

}
