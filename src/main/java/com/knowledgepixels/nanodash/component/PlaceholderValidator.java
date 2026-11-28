package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.templates.RawValue;
import org.nanopub.templates.Violation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Checks what is entered into the field of a placeholder by the rules of nanopub-java, which are
 * the ones the nanopublication is made by (#936): the text is turned into the RDF value it stands
 * for and checked against the placeholder ({@link TemplateContext#check}). Each problem is told
 * in the words the form has always used for it, or else in those of the library.
 * <p>
 * Some problems are another control's to tell: the picker of a space-/namespace-dependent prefix
 * is required while it has nothing picked, and the SPARQL of a query is checked with a message of
 * its own ({@link LiteralTextfieldItem}). They are left out here.
 */
public class PlaceholderValidator extends InvalidityHighlighting implements IValidator<String> {

    private static final Set<Violation.Kind> TOLD_ELSEWHERE = EnumSet.of(Violation.Kind.UNRESOLVED_PREFIX, Violation.Kind.INVALID_SPARQL);

    private final TemplateContext context;
    private final IRI placeholder;
    private final Set<Violation.Kind> leftOut;

    /**
     * Creates a validator for the field of a placeholder.
     *
     * @param context     the context of the form the field is in
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     */
    public PlaceholderValidator(TemplateContext context, IRI placeholder) {
        this(context, placeholder, EnumSet.noneOf(Violation.Kind.class));
    }

    /**
     * Creates a validator for the field of a placeholder that leaves some problems to another
     * control.
     *
     * @param context     the context of the form the field is in
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     * @param leftOut     the kinds of problems another control tells
     */
    public PlaceholderValidator(TemplateContext context, IRI placeholder, Set<Violation.Kind> leftOut) {
        this.context = context;
        this.placeholder = placeholder;
        this.leftOut = EnumSet.copyOf(TOLD_ELSEWHERE);
        this.leftOut.addAll(leftOut);
    }

    /**
     * Returns the value the library checks for a text entered into the field. A field with a
     * language of its own to pick gives it here.
     *
     * @param text the text entered
     * @return the raw value
     */
    protected RawValue toRawValue(String text) {
        return context.toRawValue(placeholder, text, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(IValidatable<String> validatable) {
        for (String problem : getProblems(validatable.getValue())) {
            validatable.error(new ValidationError(problem));
        }
    }

    /**
     * Returns the problems of a text entered into the field, as the form tells them.
     *
     * @param text the text entered
     * @return the problems, empty if there are none
     */
    public List<String> getProblems(String text) {
        List<String> problems = new ArrayList<>();
        if (text == null || text.isEmpty()) return problems;
        TemplateContext.FieldCheck check = context.check(placeholder, toRawValue(text));
        // A name that is not to be minted here at all does not collide with a part either.
        boolean notMinted = check.violations().stream().anyMatch(v -> v.kind() == Violation.Kind.NOT_EXTERNAL);
        for (Violation violation : check.violations()) {
            if (leftOut.contains(violation.kind())) continue;
            if (notMinted && violation.kind() == Violation.Kind.RESERVED_NAME) continue;
            String problem = getMessage(violation, check);
            if (!problems.contains(problem)) problems.add(problem);
        }
        return problems;
    }

    private static String getMessage(Violation violation, TemplateContext.FieldCheck check) {
        return switch (violation.kind()) {
            case MALFORMED_IRI -> "IRI not well-formed";
            case SCHEME_NOT_ALLOWED -> "IRI scheme not allowed here; use one of: " + Utils.getAllowedUriSchemesLabel();
            case NOT_EXTERNAL -> "Not an external IRI";
            case NOT_TRUSTY -> "Not a trusty URI";
            case NOT_A_CREATOR -> "Not a creator of nanopub";
            case MALFORMED_LITERAL -> "Invalid literal value";
            case RESERVED_NAME -> "'" + check.context().getLocalName(violation.value()) + "' is what this nanopublication calls one"
                    + " of its own parts, so a resource of that name would be that part rather than something the"
                    + " nanopublication is about. Pick a different name.";
            default -> violation.message();
        };
    }

}
