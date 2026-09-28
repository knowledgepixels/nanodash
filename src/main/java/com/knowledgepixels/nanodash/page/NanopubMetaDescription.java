package com.knowledgepixels.nanodash.page;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.SimpleTimestampPattern;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;

import static org.eclipse.rdf4j.model.util.Values.iri;

/**
 * The description a nanopublication's landing page gives search engines and link previews
 * (issue #168): the text the nanopublication itself gives what it is about, or else a
 * sentence naming what it is, who published it and when.
 */
final class NanopubMetaDescription {

    /**
     * Predicates whose literal object describes their subject in prose, in order of preference.
     */
    static final List<IRI> DESCRIBING_PREDICATES = List.of(
            DCTERMS.DESCRIPTION,
            DCTERMS.ABSTRACT,
            iri("http://schema.org/description"),
            iri("https://schema.org/description"),
            SKOS.DEFINITION,
            RDFS.COMMENT);

    private static final DateTimeFormatter PUBLICATION_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private NanopubMetaDescription() {
    }

    /**
     * Describes a nanopublication, or the resource minted in it, for its landing page.
     *
     * @param np          the nanopublication shown on the page
     * @param exploredId  the IRI the page is about: the nanopublication's own, or that of a
     *                    resource minted in it
     * @param creatorName resolves a creator's IRI to the name to show for them
     * @return the description, as plain text or HTML exactly as the nanopublication has it
     */
    static String describe(Nanopub np, String exploredId, Function<IRI, String> creatorName) {
        String ownDescription = findOwnDescription(np, describedSubjects(np, exploredId));
        if (ownDescription != null) return ownDescription;
        return summarize(np, creatorName);
    }

    /**
     * The subjects whose description stands for the page: the explored resource itself, and
     * for a page about a whole nanopublication the resources it introduces.
     *
     * @param np         the nanopublication shown on the page
     * @param exploredId the IRI the page is about
     * @return the subject IRIs, most specific first
     */
    static List<String> describedSubjects(Nanopub np, String exploredId) {
        List<String> subjects = new ArrayList<>();
        subjects.add(exploredId);
        if (exploredId.equals(np.getUri().stringValue())) {
            subjects.addAll(NanopubUtils.getIntroducedIriIds(np));
        }
        return subjects;
    }

    /**
     * The first describing literal the assertion gives one of the subjects, trying the
     * subjects in turn and each subject's predicates in {@link #DESCRIBING_PREDICATES} order.
     *
     * @param np       the nanopublication
     * @param subjects the subject IRIs to look for, most specific first
     * @return the description, or null if the assertion gives none of them one
     */
    static String findOwnDescription(Nanopub np, List<String> subjects) {
        for (String subject : subjects) {
            for (IRI predicate : DESCRIBING_PREDICATES) {
                String description = findLiteral(np, subject, predicate);
                if (description != null) return description;
            }
        }
        return null;
    }

    private static String findLiteral(Nanopub np, String subject, IRI predicate) {
        for (Statement st : np.getAssertion()) {
            if (!st.getSubject().stringValue().equals(subject)) continue;
            if (!st.getPredicate().equals(predicate)) continue;
            if (st.getObject() instanceof Literal literal && !literal.stringValue().isBlank()) {
                return literal.stringValue();
            }
        }
        return null;
    }

    /**
     * A sentence saying what the nanopublication is, who published it and when, for one
     * that describes nothing in prose.
     *
     * @param np          the nanopublication
     * @param creatorName resolves a creator's IRI to the name to show for them
     * @return the summary
     */
    static String summarize(Nanopub np, Function<IRI, String> creatorName) {
        StringBuilder summary = new StringBuilder();
        String label = NanopubUtils.getLabel(np);
        summary.append(label == null || label.isBlank() ? "A nanopublication" : label + ": a nanopublication");
        List<String> creators = creatorNames(np, creatorName);
        if (!creators.isEmpty()) summary.append(" by ").append(String.join(", ", creators));
        String date = publicationDate(np);
        if (date != null) summary.append(", published on ").append(date);
        return summary.append('.').toString();
    }

    private static List<String> creatorNames(Nanopub np, Function<IRI, String> creatorName) {
        List<String> names = new ArrayList<>();
        for (IRI creator : SimpleCreatorPattern.getCreators(np)) {
            String name = creatorName.apply(creator);
            names.add(name == null || name.isBlank() ? creator.stringValue() : name);
        }
        return names;
    }

    private static String publicationDate(Nanopub np) {
        Calendar created = SimpleTimestampPattern.getCreationTime(np);
        if (created == null) return null;
        return created.toInstant().atZone(ZoneOffset.UTC).toLocalDate().format(PUBLICATION_DATE);
    }

}
