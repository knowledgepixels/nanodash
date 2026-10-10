package com.knowledgepixels.nanodash.lookup;

/**
 * Rewrites what a user typed into a query for full-text search indexes.
 */
public final class SearchTerms {

    private SearchTerms() {
    }

    /**
     * Turns a search term into a query that matches all of its words, the last one as a prefix
     * unless it ends a quoted phrase. Quoted phrases are kept together, and characters other
     * than letters, digits, hyphens and underscores separate words.
     *
     * @param searchTerm the search term as typed
     * @return the query, e.g. {@code ( covid AND virus* )} for {@code covid virus}
     */
    public static String expand(String searchTerm) {
        String expanded = "";
        boolean insideQuotes = false;
        searchTerm = searchTerm.replaceAll("\\s+", " ").trim();
        for (char c : searchTerm.toCharArray()) {
            if (c == '\n') {
                continue;
            } else if (c == '"') {
                expanded += '"';
                insideQuotes = !insideQuotes;
            } else if (c == ' ') {
                expanded += insideQuotes ? ' ' : '\n';
            } else if (("" + c).matches("\\w") || c == '-' || c == '_') {
                expanded += c;
            } else {
                expanded += insideQuotes ? ' ' : '\n';
            }
        }
        String extra = "*";
        expanded = expanded.replaceAll("\\n+", "\n").replaceAll("\"", "\\\\\\\"").trim();
        if (expanded.endsWith("\"") || insideQuotes) extra = "";
        return "( " + String.join(" AND ", expanded.split("\n")) + extra + " )";
    }

}
