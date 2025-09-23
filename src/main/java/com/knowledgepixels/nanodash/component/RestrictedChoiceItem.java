package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.RestrictedChoice;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.util.Collection;
import java.util.List;

/**
 * RestrictedChoiceItem is a Wicket component that provides a select2 choice input
 */
public class RestrictedChoiceItem extends Panel implements ContextComponent {

    private TemplateContext<String> context;
    private IRI iri;
    private Select2Choice<String> choice;
    private ExternalLink tooltipLink;
    private Label tooltipDescription;
    private IModel<String> model;
    private RestrictedChoice restrictedChoice;
    private static final Logger logger = LoggerFactory.getLogger(RestrictedChoiceItem.class);

    /**
     * Constructor for RestrictedChoiceItem.
     *
     * @param id       the component id
     * @param parentId the parent id (e.g., "subj" or "obj")
     * @param iri      the IRI of the restricted choice
     * @param optional whether the choice is optional
     * @param context  the template context
     */
    public RestrictedChoiceItem(String id, String parentId, IRI iri, boolean optional, final TemplateContext<String> context) {
        super(id);
        this.context = context;
        this.iri = iri;
        Template template = context.getTemplate();
        model = context.getComponentModels().get(iri);
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
        }
        String postfix = Utils.getUriPostfix(iri);
        if (context.hasParam(postfix)) {
            model.setObject(context.getParam(postfix));
        }
        restrictedChoice = new RestrictedChoice(iri, context);

        String prefixLabel = template.getPrefixLabel(iri);
        Label prefixLabelComp;
        if (prefixLabel == null) {
            prefixLabelComp = new Label("prefix", "");
            prefixLabelComp.setVisible(false);
        } else {
            if (!prefixLabel.isEmpty() && parentId.equals("subj") && !prefixLabel.matches("https?://.*")) {
                // Capitalize first letter of label if at subject position:
                prefixLabel = prefixLabel.substring(0, 1).toUpperCase() + prefixLabel.substring(1);
            }
            prefixLabelComp = new Label("prefix", prefixLabel);
        }
        add(prefixLabelComp);

        ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

            @Override
            public String getDisplayValue(String choiceId) {
                if (choiceId == null || choiceId.isEmpty()) return "";
                if (!choiceId.matches("https?://.+")) {
                    return choiceId;
                }
                String label = "";
                if (restrictedChoice.hasFixedPossibleValue(choiceId)) {
                    label = template.getLabel(vf.createIRI(choiceId));
                }
                if (label == null || label.isBlank()) {
                    return choiceId;
                }
                return label + " (" + choiceId + ")";
            }

            @Override
            public String getIdValue(String object) {
                return object;
            }

            // Getting strange errors with Tomcat if this method is not overridden:
            @Override
            public void detach() {
            }

            @Override
            public void query(String term, int page, Response<String> response) {
                List<String> possibleValues = restrictedChoice.getPossibleValues();

                if (term == null) {
                    response.addAll(possibleValues);
                    return;
                }
                term = term.toLowerCase();
                for (String s : possibleValues) {
                    if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term))
                        response.add(s);
                }
            }

            @Override
            public Collection<String> toChoices(Collection<String> ids) {
                return ids;
            }

        };
        choice = new Select2Choice<String>("choice", model, choiceProvider);
        if (!optional) choice.setRequired(true);
        if (template.isLocalResource(iri)) {
            choice.add(new AttributeAppender("class", " short"));
        }
        choice.getSettings().setCloseOnSelect(true);
        String placeholder = template.getLabel(iri);
        if (placeholder == null) placeholder = "";
        choice.getSettings().setPlaceholder(placeholder);
        Utils.setSelect2ChoiceMinimalEscapeMarkup(choice);
        choice.getSettings().setAllowClear(true);
        choice.add(new ValueItem.KeepValueAfterRefreshBehavior());
        choice.add(new Validator());
        context.getComponents().add(choice);

        tooltipDescription = new Label("description", new IModel<String>() {

            @Override
            public String getObject() {
                String obj = RestrictedChoiceItem.this.getModel().getObject();
                if (obj == null || obj.isEmpty()) return "choose a value";
                String label = null;
                if (restrictedChoice.hasFixedPossibleValue(obj)) {
                    label = template.getLabel(vf.createIRI(obj));
                }
                if (label == null || !label.contains(" - ")) return "";
                return label.substring(label.indexOf(" - ") + 3);
            }

        });
        tooltipDescription.setOutputMarkupId(true);
        add(tooltipDescription);

        tooltipLink = Utils.getUriLink("uri", model);
        tooltipLink.setOutputMarkupId(true);
        add(tooltipLink);

        choice.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == choice) continue;
                    if (c.getDefaultModel() == choice.getModel()) {
                        c.modelChanged();
                        target.add(c);
                    }
                }
                target.add(tooltipLink);
                target.add(tooltipDescription);
            }

        });
        add(choice);
    }

    /**
     * Get the model for this restricted choice item.
     *
     * @return the model containing the selected value
     */
    public IModel<String> getModel() {
        return model;
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(choice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (v instanceof IRI) {
            String vs = v.stringValue();
            if (Utils.isLocalURI(vs)) vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
            if (!restrictedChoice.hasPossibleRefValues() && !restrictedChoice.hasFixedPossibleValue(vs)) {
                return false;
            }
            if (choice.getModelObject().isEmpty()) {
                return true;
            }
            return vs.equals(choice.getModelObject());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (v == null) return;
        String vs = v.stringValue();
        if (Utils.isLocalURI(vs)) vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
        if (!isUnifiableWith(v)) throw new UnificationException(vs);
        choice.setModelObject(vs);
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
        Value defaultValue = context.getTemplate().getDefault(iri);
        if (isUnifiableWith(defaultValue)) {
            try {
                unifyWith(defaultValue);
            } catch (UnificationException ex) {
                logger.error("Could not unify with default value: {}", defaultValue, ex);
            }
        }
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "[Restricted choice item: " + iri + "]";
    }


    /**
     * Validator for the restricted choice item.
     */
    protected class Validator extends InvalidityHighlighting implements IValidator<String> {

        /**
         * Default constructor.
         */
        public Validator() {
        }

        @Override
        public void validate(IValidatable<String> s) {
            if (!restrictedChoice.getPossibleValues().contains(s.getValue())) {
                s.error(new ValidationError("Invalid choice"));
            }
        }

    }

}
