package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks SPARQL code and says, in plain words, what is wrong with it when something is.
 * <p>
 * Nanodash meets invalid SPARQL at two moments, and both are served from here: when someone
 * fills in the SPARQL of a query they are about to publish, and when a query that was
 * published anyway turns out not to parse. The second is the more expensive one — a
 * nanopublication can't be edited, so a query published with broken SPARQL stays broken and
 * can only be superseded — which is why the check is worth running before publishing too.
 * <p>
 * The parser used is the one that will later decide whether the published query can run at
 * all ({@link SPARQLParser}, via {@code QueryTemplate} in nanopub-java), so what passes here
 * passes there.
 */
public class SparqlSyntax {

    private SparqlSyntax() {
    }  // no instances allowed

    /**
     * Picks the numeric character code out of the lexical errors the RDF4J SPARQL parser
     * reports, which read like {@code Lexical error at line 20, column 56.  Encountered: "..."
     * (160), after : ""} — where the number in brackets is the code of the character it
     * tripped over.
     */
    private static final Pattern LEXICAL_ERROR_CHARACTER_PATTERN = Pattern.compile("Encountered:[^(]*\\((\\d+)\\)");

    /**
     * Checks whether the given SPARQL code parses, and explains what is wrong with it if it
     * doesn't. Blank input is not an error here: whether a value is required is a question for
     * the form, not for the parser.
     *
     * @param sparql the SPARQL code to check
     * @return the explanation of what is wrong, or null if the code parses (or is blank)
     */
    public static String checkQuery(String sparql) {
        if (sparql == null || sparql.isBlank()) return null;
        try {
            new SPARQLParser().parseQuery(sparql, null);
            return null;
        } catch (MalformedQueryException ex) {
            return explain(ex);
        } catch (RuntimeException ex) {
            // The RDF4J tokenizer signals some failures as an Error rather than an exception,
            // and other query-shaped input can trip up the parser in its own ways; none of
            // that should reach the user as a stack trace.
            return "This is not valid SPARQL: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    /**
     * Puts a SPARQL parser failure into plain words: what the parser tripped over and where,
     * followed by the identity of the offending character where that is the real problem.
     *
     * @param ex the parser failure
     * @return the explanation, never null
     */
    public static String explain(MalformedQueryException ex) {
        String detail = summarize(ex.getMessage());
        String characterHint = explainOffendingCharacter(ex.getMessage());
        return "This is not valid SPARQL."
                + (detail == null ? "" : " The SPARQL parser reports: " + detail)
                + (characterHint == null ? "" : " " + characterHint);
    }

    /**
     * Reduces a SPARQL parser failure to the one line that says what it tripped over and
     * where, leaving out the list of what it was expecting instead. That list runs to dozens
     * of grammar rules, which is more than a form field or an error page can carry; it is kept
     * in the log.
     *
     * @param parserMessage the message of the SPARQL parser failure
     * @return the gist of it as a single sentence, or null if there is nothing to say
     */
    public static String summarize(String parserMessage) {
        if (parserMessage == null) return null;
        // Lexical errors trail off into a dangling comma, which would run into what follows.
        String firstLine = parserMessage.split("\\R", 2)[0].trim().replaceAll("[,;:\\s]+$", "");
        if (firstLine.isEmpty()) return null;
        return firstLine.endsWith(".") ? firstLine : firstLine + ".";
    }

    /**
     * Names the character a SPARQL lexical error tripped over, as long as it is one that
     * doesn't belong in SPARQL in the first place. Such characters — a non-breaking space, a
     * typographic quote — look like their plain ASCII counterparts or like nothing at all, so
     * the parser's report of a numeric character code leaves the author of the query none the
     * wiser about what to correct.
     *
     * @param parserMessage the message of the SPARQL parser failure
     * @return the explanation, or null if the message doesn't point at such a character
     */
    public static String explainOffendingCharacter(String parserMessage) {
        if (parserMessage == null) return null;
        Matcher m = LEXICAL_ERROR_CHARACTER_PATTERN.matcher(parserMessage);
        if (!m.find()) return null;
        int codePoint;
        try {
            codePoint = Integer.parseInt(m.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
        // Only non-ASCII characters are worth naming: for the ASCII ones the parser's own
        // report already shows what is there.
        if (codePoint < 128 || !Character.isValidCodePoint(codePoint)) return null;
        String name = Character.getName(codePoint);
        return "The character in that position is " + String.format("U+%04X", codePoint)
                + (name == null ? "" : " (" + name + ")")
                + ", which SPARQL doesn't allow there. Characters like this one tend to slip in when a query is"
                + " copied from a word processor or a web page, and replacing them with their plain equivalents"
                + " makes the query valid again.";
    }

}
