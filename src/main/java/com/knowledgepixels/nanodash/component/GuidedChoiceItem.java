package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.ResourceWithProfile;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.IriTextfieldItem.Validator;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.Validatable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.util.*;

/**
 * A guided choice item that allows users to select from a list of predefined values.
 */
public class GuidedChoiceItem extends AbstractContextComponent {

    private Select2Choice<String> textfield;
    private ExternalLink tooltipLink;
    private Label tooltipDescription;
    private IRI iri;
    private IModel<String> model;
    private static final Logger logger = LoggerFactory.getLogger(GuidedChoiceItem.class);

    private String prefix;

    // TODO: This map being static could mix up labels if the same URI is described at different places:
    // TODO: This should maybe go into a different class?
    private static Map<String, String> labelMap = new HashMap<>();

    /**
     * Get the label for a given value.
     *
     * @param value The value for which to get the label.
     * @return The label associated with the value, or null if not found.
     */
    public static String getLabel(String value) {
        return labelMap.get(value);
    }

    /**
     * Set a label for a given value.
     *
     * @param value The value for which to set the label.
     * @param label The label to associate with the value.
     * @return The previous label associated with the value, or null if there was none.
     */
    public static String setLabel(String value, String label) {
        return labelMap.put(value, label);
    }

    private String getChoiceLabel(String choiceId) {
        Template template = context.getTemplate();
        String label = null;
        if (choiceId.matches("https?://.+") && template.getLabel(vf.createIRI(choiceId)) != null) {
            label = template.getLabel(vf.createIRI(choiceId));
        } else if (labelMap.containsKey(choiceId)) {
            label = labelMap.get(choiceId);
            if (label.length() > 160) label = label.substring(0, 157) + "...";
        }
        return label;
    }

    /**
     * Constructor for the GuidedChoiceItem.
     *
     * @param id       The Wicket component ID.
     * @param parentId The parent ID, used for context.
     * @param iriP     The IRI associated with this choice item.
     * @param optional Whether the choice is optional or required.
     * @param context  The template context containing the template and models.
     */
    public GuidedChoiceItem(String id, String parentId, final IRI iriP, boolean optional, final TemplateContext context) {
        super(id, context);
        this.iri = iriP;
        final Template template = context.getTemplate();
        model = (IModel<String>) context.getComponentModels().get(iri);
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
        }
        String postfix = Utils.getUriPostfix(iri);
        if (context.hasParam(postfix)) {
            String objId = context.getParam(postfix);
            if (ResourceWithProfile.get(objId) != null) {
                labelMap.put(objId, ResourceWithProfile.get(objId).getLabel());
            }
            model.setObject(objId);
        }
        final List<String> possibleValues = new ArrayList<>();
        for (Value v : template.getPossibleValues(iri)) {
            possibleValues.add(v.toString());
        }

        prefix = template.getPrefix(iri);
        if (prefix == null) prefix = "";
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
        String prefixTooltip = prefix;
        if (!prefix.isEmpty()) {
            prefixTooltip += "...";
        }
        add(new Label("prefixtooltiptext", prefixTooltip));

        ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

            @Override
            public String getDisplayValue(String choiceId) {
                if (choiceId == null || choiceId.isEmpty()) return "";
                String label = getChoiceLabel(choiceId);
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
                if (term == null) {
                    response.addAll(possibleValues);
                    return;
                }
                if (term.startsWith("https://") || term.startsWith("http://")) {
                    if (prefix == null || term.startsWith(prefix)) {
                        response.add(term);
                    }
                }
                Map<String, Boolean> alreadyAddedMap = new HashMap<>();
                term = term.toLowerCase();
                for (String s : possibleValues) {
                    if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
                        response.add(s);
                        alreadyAddedMap.put(s, true);
                    }
                }
                for (String v : context.getTemplate().getPossibleValuesFromApi(iri, term, labelMap)) {
                    if (!alreadyAddedMap.containsKey(v)) response.add(v);
                }
            }

            @Override
            public Collection<String> toChoices(Collection<String> ids) {
                return ids;
            }

        };
        textfield = new Select2Choice<String>("textfield", model, choiceProvider);
        textfield.getSettings().getAjax(true).setDelay(500);
        textfield.getSettings().setCloseOnSelect(true);
        String placeholder = template.getLabel(iri);
        if (placeholder == null) placeholder = "";
        textfield.getSettings().setPlaceholder(placeholder);
        Utils.setSelect2ChoiceMinimalEscapeMarkup(textfield);
        textfield.getSettings().setAllowClear(true);

        if (!optional) textfield.setRequired(true);
        textfield.add(new AttributeAppender("class", " wide"));
        textfield.add(new Validator(iri, template, prefix, context));
        context.getComponents().add(textfield);

        tooltipDescription = new Label("description", new IModel<String>() {

            @Override
            public String getObject() {
                String obj = GuidedChoiceItem.this.getModel().getObject();
                if (obj == null || obj.isEmpty()) return "choose a value";
                String label = getChoiceLabel(GuidedChoiceItem.this.getModel().getObject());
                if (label == null || !label.contains(" - ")) return "";
                return label.substring(label.indexOf(" - ") + 3);
            }

        });
        tooltipDescription.setOutputMarkupId(true);
        add(tooltipDescription);

        tooltipLink = Utils.getUriLink("uri", model);
        tooltipLink.setOutputMarkupId(true);
        add(tooltipLink);

        textfield.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == textfield) continue;
                    if (c.getDefaultModel() == textfield.getModel()) {
                        c.modelChanged();
                        target.add(c);
                    }
                }
                target.add(tooltipLink);
                target.add(tooltipDescription);
            }

        });
        add(textfield);
    }

    /**
     * Get the model associated with this choice item.
     *
     * @return The model containing the selected value.
     */
    public IModel<String> getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(textfield);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (v instanceof IRI) {
            String vs = v.stringValue();
            if (vs.startsWith(prefix)) vs = vs.substring(prefix.length());
            if (Utils.isLocalURI(vs)) vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
            Validatable<String> validatable = new Validatable<>(vs);
            if (context.getTemplate().isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
                vs = Utils.getUriPostfix(vs);
            }
            new Validator(iri, context.getTemplate(), prefix, context).validate(validatable);
            if (!validatable.isValid()) {
                return false;
            }
            if (textfield.getModelObject() == null || textfield.getModelObject().isEmpty()) {
                return true;
            }
            return vs.equals(textfield.getModelObject());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (v == null) return;
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
        String vs = v.stringValue();
        if (prefix != null && vs.startsWith(prefix)) {
            vs = vs.substring(prefix.length());
        } else if (Utils.isLocalURI(vs)) {
            vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
        }
        textfield.setModelObject(vs);
        // TODO: This should be done differently, at a different place (can slow down unification):
        if (!labelMap.containsKey(vs)) {
            context.getTemplate().getPossibleValuesFromApi(iri, vs, labelMap);
        }
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
                logger.error("Could not unify default value: {}", defaultValue, ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[Guided choiced item: " + iri + "]";
    }

}
