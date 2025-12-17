package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

/**
 * Represents a sequence element item.
 */
public class SequenceElementItem extends AbstractContextComponent {

    /**
     * Prefix for sequence element properties in RDF.
     */
    public static final String SEQUENCE_ELEMENT_PROPERTY_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#_";

    private final int number;

    /**
     * Constructor for creating a sequence element item.
     *
     * @param id      the component ID
     * @param iri     the IRI of the sequence element
     * @param number  the position of the element in the sequence
     * @param context the template context
     */
    public SequenceElementItem(String id, final IRI iri, int number, TemplateContext context) {
        super(id, context);
        this.number = number;
        context.getComponentModels().put(iri, Model.of(SEQUENCE_ELEMENT_PROPERTY_PREFIX + number));

        String labelString = "has element number ${number}";
        if (context.getTemplate().getLabel(iri) != null) {
            labelString = context.getTemplate().getLabel(iri);
        }
        String description = "This relation links a sequence/list to its element at position " + number + ".";
        if (labelString.contains(" - ")) description = labelString.replaceFirst("^.* - ", "");
        String label = labelString.replaceFirst(" - .*$", "");
        label = label.replaceAll("\\$\\{number\\}", number + "");

        add(new Label("description", description));
        add(Utils.getUriLink("uri", SEQUENCE_ELEMENT_PROPERTY_PREFIX + number));

        add(new ExternalLink("text", "", label));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (v instanceof Literal) return false;
        return v.stringValue().equals(SEQUENCE_ELEMENT_PROPERTY_PREFIX + number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (v == null) return;
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFinished() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "[Sequence element]";
    }

}
