package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import java.util.Optional;

/**
 * Checks whether the identifier a new-resource field forms is already in use (#646, #713).
 * <p>
 * Such an identifier carries no artifact code, so nothing makes it unique. The publish form
 * checks every one of them on submit and refuses to publish a taken one; the fields check their
 * own value as it is typed, so that a taken name shows up next to the field that needs changing
 * rather than after the whole form has been filled in. Both decide what to check the same way,
 * here and in {@link TemplateContext#getNewUriIri(IRI, String)}.
 */
public final class NewUriCheck {

    /**
     * The outcome of checking the value of one field.
     */
    public enum Result {

        /**
         * The identifier is already in use.
         */
        TAKEN,

        /**
         * The identifier is not in use yet.
         */
        FREE,

        /**
         * The query service could not be asked, so nothing is known either way.
         */
        UNKNOWN,

        /**
         * There is nothing to check: the field is not tagged as naming a new resource, its value
         * forms no identifier yet, or the identifier is exempt.
         */
        NOT_CHECKED
    }

    private NewUriCheck() {
    }

    /**
     * Checks the identifier a value entered into a field would give a new resource.
     *
     * @param context     the template context of the field
     * @param placeholder the field's placeholder
     * @param value       the value entered into it
     * @return whether the identifier is taken, free or could not be checked, or that there is
     * nothing to check
     */
    public static Result check(TemplateContext context, IRI placeholder, String value) {
        IRI identifier = context.getNewUriIri(placeholder, value);
        if (identifier == null || isExempt(context, identifier)) {
            return Result.NOT_CHECKED;
        }
        Optional<Boolean> introduced = QueryApiAccess.checkUriIntroduced(identifier.stringValue());
        if (introduced.isEmpty()) {
            return Result.UNKNOWN;
        }
        return introduced.get() ? Result.TAKEN : Result.FREE;
    }

    /**
     * Whether an identifier for a new resource is left unchecked because of how the form is
     * filled. Superseding and overriding keep the identifiers the source already carries, since a
     * new version keeps the resource it is a version of, and finding that one in use is the
     * expected answer rather than a collision. Without a source to compare against, nothing is
     * checked at all, so an unrecognised fill publishes exactly as before.
     *
     * @param context    the template context the identifier was formed in
     * @param identifier the identifier
     * @return true if the identifier is not to be checked
     */
    static boolean isExempt(TemplateContext context, IRI identifier) {
        FillMode fillMode = context.getFillMode();
        if (fillMode != FillMode.SUPERSEDE && fillMode != FillMode.OVERRIDE) {
            return false;
        }
        Nanopub source = context.getReferenceNanopub();
        return source == null || isUsedIn(source, identifier);
    }

    /**
     * Tells whether the given nanopublication already mentions the given IRI, which is how a
     * superseding version says it is carrying the source's resource over rather than naming a
     * new one. Every graph counts: the resource appears as a subject in the assertion and again
     * under {@code npx:introduces} in the publication info.
     *
     * @param nanopub the nanopublication to look in
     * @param iri     the identifier to look for
     * @return true if the nanopublication uses the identifier
     */
    static boolean isUsedIn(Nanopub nanopub, IRI iri) {
        for (Statement st : NanopubUtils.getStatements(nanopub)) {
            if (iri.equals(st.getSubject()) || iri.equals(st.getObject())) return true;
        }
        return false;
    }
}
