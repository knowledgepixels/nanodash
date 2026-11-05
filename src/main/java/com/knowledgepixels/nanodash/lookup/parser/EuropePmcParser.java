package com.knowledgepixels.nanodash.lookup.parser;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.lookup.JsonLookupApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parser for the publication search of Europe PMC, e.g.
 * {@code https://www.ebi.ac.uk/europepmc/webservices/rest/search?format=json&query=}. The
 * search term is appended to the query, so a template can narrow the search with a search field,
 * e.g. {@code ...&query=PMCID:} to look papers up by their PMC ID. A paper's value is the
 * identifiers.org URI of its PMC ID, or of its PubMed ID if it has none; papers with neither,
 * such as most preprints, are left out. The label of a paper is its title, followed by its
 * first author, journal and year.
 */
public class EuropePmcParser extends JsonLookupApi {

    static final String PMC_PREFIX = "https://identifiers.org/pmc:";

    static final String PUBMED_PREFIX = "https://identifiers.org/pubmed:";

    private static final Pattern PMCID = Pattern.compile("PMC[0-9]+");

    private static final Pattern PMID = Pattern.compile("[0-9]+");

    // Europe PMC sends titles with their markup escaped, e.g. "B&lt;sub&gt;1&lt;/sub&gt;". Once
    // unescaped, these inline tags are dropped without a space, so that "B<sub>1</sub>" reads "B1".
    private static final Pattern INLINE_TAG = Pattern.compile("</?[A-Za-z][A-Za-z0-9]*(\\s[^<>]*)?/?>");

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONArray results = new JSONObject(response).getJSONObject("resultList").getJSONArray("result");
        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            String value = valueOf(result);
            if (value == null) continue;
            addIfNew(value, labelOf(result), labels, values);
        }
    }

    private static String valueOf(JSONObject result) {
        String pmcid = textOf(result, "pmcid");
        if (pmcid != null && PMCID.matcher(pmcid).matches()) return PMC_PREFIX + pmcid;
        String pmid = textOf(result, "pmid");
        if (pmid != null && PMID.matcher(pmid).matches()) return PUBMED_PREFIX + pmid;
        return null;
    }

    private static String labelOf(JSONObject result) {
        String title = titleOf(result);
        String citation = citationOf(result);
        if (title == null) return citation;
        if (citation.isEmpty()) return title;
        return title + " - " + citation;
    }

    /**
     * Gives the title of a paper as plain text, without the period it ends with. A " - " in it
     * is written " -- ", as the first " - " of a label separates its description.
     */
    private static String titleOf(JSONObject result) {
        String title = textOf(result, "title");
        if (title == null) return null;
        title = INLINE_TAG.matcher(Utils.htmlToPlainText(title)).replaceAll("");
        title = title.replaceAll("\\s+", " ").trim().replaceAll("\\.$", "").replaceAll(" - ", " -- ");
        return title.isEmpty() ? null : title;
    }

    private static String citationOf(JSONObject result) {
        List<String> parts = new ArrayList<>();
        String firstAuthor = firstAuthorOf(result);
        if (firstAuthor != null) parts.add(firstAuthor);
        String venue = textOf(result, "journalTitle");
        if (venue == null) {
            // Preprints have no journal, but name the server they are on as their publisher.
            JSONObject book = result.optJSONObject("bookOrReportDetails");
            if (book != null) venue = textOf(book, "publisher");
        }
        if (venue != null) parts.add(venue);
        String year = textOf(result, "pubYear");
        if (year != null) parts.add(year);
        return String.join(", ", parts);
    }

    /**
     * Gives the first author of a paper, with "et al." if there are more. Europe PMC lists the
     * authors in one string, e.g. "Nirudodhi S, Parsonage D, Maier CS.".
     */
    private static String firstAuthorOf(JSONObject result) {
        String authors = textOf(result, "authorString");
        if (authors == null) return null;
        authors = authors.replaceAll("\\.$", "");
        int comma = authors.indexOf(", ");
        if (comma < 0) return authors;
        return authors.substring(0, comma) + " et al.";
    }

}
