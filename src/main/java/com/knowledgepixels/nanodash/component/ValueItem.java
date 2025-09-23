package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.vocabulary.NTEMPLATE;

/**
 * ValueItem is a panel that represents a single value in a statement.
 */
public class ValueItem extends Panel implements ContextComponent {

    private ContextComponent component;
    private Value value;

    /**
     * Constructor for ValueItem.
     *
     * @param id              the component id
     * @param value           the value to be represented
     * @param statementPartId the IRI of the statement part this value belongs to
     * @param rg              the repetition group this value is part of
     */
    public ValueItem(String id, Value value, IRI statementPartId, RepetitionGroup rg) {
        super(id);
        this.value = value;
        final Template template = rg.getContext().getTemplate();
        if (value instanceof IRI) {
            IRI iri = (IRI) value;
            if (template.isSequenceElementPlaceholder(iri)) {
                component = new SequenceElementItem("value", iri, rg.getRepeatIndex() + 1, rg.getContext());
            } else if (iri.equals(NTEMPLATE.CREATOR_PLACEHOLDER)) {
                // This is a special placeholder that is always read-only
                component = new ReadonlyItem("value", id, iri, id.equals("obj"), statementPartId, rg);
            } else if (rg.getContext().isReadOnly()) {
                if (template.isPlaceholder(iri)) {
                    component = new ReadonlyItem("value", id, iri, id.equals("obj"), statementPartId, rg);
                } else {
                    component = new IriItem("value", id, iri, id.equals("obj"), statementPartId, rg);
                }
            } else if (template.isRestrictedChoicePlaceholder(iri)) {
                component = new RestrictedChoiceItem("value", id, iri, rg.isOptional(), rg.getContext());
            } else if (template.isAgentPlaceholder(iri)) {
                component = new AgentChoiceItem("value", id, iri, rg.isOptional(), rg.getContext());
            } else if (template.isGuidedChoicePlaceholder(iri)) {
                component = new GuidedChoiceItem("value", id, iri, rg.isOptional(), rg.getContext());
            } else if (template.isIntroducedResource(iri) && rg.getContext().getFillMode() == FillMode.SUPERSEDE) {
                component = new ReadonlyItem("value", id, iri, id.equals("obj"), statementPartId, rg);
            } else if (template.isUriPlaceholder(iri)) {
                component = new IriTextfieldItem("value", id, iri, rg.isOptional(), rg.getContext());
            } else if (template.isLongLiteralPlaceholder(iri)) {
                component = new LiteralTextareaItem("value", iri, rg.isOptional(), rg.getContext());
            } else if (template.isLiteralPlaceholder(iri)) {
                // TODO add all date time types
                if (template.getDatatype(iri).equals(XSD.DATE)) {
                    component = new LiteralDateItem("value", iri, rg.isOptional(), rg.getContext());
                } else if (template.getDatatype(iri).equals(XSD.DATETIME)) {
                    component = new LiteralDateTimeItem("value", iri, rg.isOptional(), rg.getContext());
                } else {
                    component = new LiteralTextfieldItem("value", iri, rg.isOptional(), rg.getContext());
                }
            } else if (template.isPlaceholder(iri)) {
                component = new ValueTextfieldItem("value", id, iri, rg.isOptional(), rg.getContext());
            } else {
                component = new IriItem("value", id, iri, id.equals("obj"), statementPartId, rg);
            }
        } else {
            component = new LiteralItem("value", id, (Literal) value, rg);
        }
        add((Component) component);
    }

    /**
     * OnChangeAjaxBehavior that keeps the value after a refresh.
     */
    public static class KeepValueAfterRefreshBehavior extends OnChangeAjaxBehavior {

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            // No actual action needed here; Ajax request alone ensures values are kept after refreshing.
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        component.removeFromContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        return component.isUnifiableWith(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        component.unifyWith(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFinished() {
        component.fillFinished();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
        component.finalizeValues();
    }

    /**
     * Get the value represented by this ValueItem.
     *
     * @return the value
     */
    public Value getValue() {
        return value;
    }

    /**
     * Get the component that represents this ValueItem.
     *
     * @return the ContextComponent that represents this ValueItem
     */
    public ContextComponent getComponent() {
        return component;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return component.toString();
    }

}
