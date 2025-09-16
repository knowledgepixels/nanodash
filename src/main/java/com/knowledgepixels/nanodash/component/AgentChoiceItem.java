package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.IriTextfieldItem.Validator;
import com.knowledgepixels.nanodash.page.ProfilePage;
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
import org.apache.wicket.validation.Validatable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.util.*;

/**
 * A component that allows users to select an agent (user) from a list or enter an ORCID or URL.
 */
public class AgentChoiceItem extends Panel implements ContextComponent {

    private static final long serialVersionUID = 1L;
    private TemplateContext context;
    private Select2Choice<String> textfield;
    private ExternalLink tooltipLink;
    private Label tooltipDescription;
    private IRI iri;
    private IModel<String> model;
    private static final Logger logger = LoggerFactory.getLogger(AgentChoiceItem.class);

    private String getChoiceLabel(String choiceId) {
        IRI iri = vf.createIRI(choiceId);
        String name = User.getName(iri);
        if (name != null) return name;
        return choiceId;
    }

    /**
     * Constructor for AgentChoiceItem.
     *
     * @param id       the component ID
     * @param parentId the parent component ID
     * @param iriP     the IRI of the agent choice item
     * @param optional whether the choice is optional
     * @param context  the template context
     */
    public AgentChoiceItem(String id, String parentId, final IRI iriP, boolean optional, final TemplateContext context) {
        super(id);
        this.context = context;
        this.iri = iriP;
        final Template template = context.getTemplate();
        model = context.getComponentModels().get(iri);
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
        }
        String postfix = Utils.getUriPostfix(iri);
        if (context.hasParam(postfix)) {
            model.setObject(context.getParam(postfix));
        }
        final List<String> possibleValues = new ArrayList<>();
        for (Value v : template.getPossibleValues(iri)) {
            possibleValues.add(v.toString());
        }

        ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

            private static final long serialVersionUID = 1L;

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
                    if (possibleValues.isEmpty()) {
                        if (NanodashSession.get().getUserIri() != null) {
                            response.add(NanodashSession.get().getUserIri().stringValue());
                        }
                    } else {
                        response.addAll(possibleValues);
                    }
                    return;
                }
                if (term.startsWith("https://") || term.startsWith("http://")) {
                    response.add(term);
                } else if (term.matches(ProfilePage.ORCID_PATTERN)) {
                    response.add("https://orcid.org/" + term);
                }
                Map<String, Boolean> alreadyAddedMap = new HashMap<>();
                term = term.toLowerCase();
                for (String s : possibleValues) {
                    if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
                        response.add(s);
                        alreadyAddedMap.put(s, true);
                    }
                }

                // TODO: We'll need some indexing to perform this more efficiently at some point:
                for (IRI iri : User.getUsers(true)) {
                    // Collect approved users
                    if (response.size() > 9) break;
                    if (response.getResults().contains(iri.stringValue())) continue;
                    String name = User.getName(iri);
                    if (iri.stringValue().contains(term)) {
                        response.add(iri.stringValue());
                    } else if (name != null && name.toLowerCase().contains(term)) {
                        response.add(iri.stringValue());
                    }
                }
                for (IRI iri : User.getUsers(false)) {
                    // Collect non-approved users
                    if (response.size() > 9) break;
                    if (response.getResults().contains(iri.stringValue())) continue;
                    String name = User.getName(iri);
                    if (iri.stringValue().contains(term)) {
                        response.add(iri.stringValue());
                    } else if (name != null && name.toLowerCase().contains(term)) {
                        response.add(iri.stringValue());
                    }
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
        if (placeholder == null) placeholder = "select user or paste ORCID/URL";
        textfield.getSettings().setPlaceholder(placeholder);
        Utils.setSelect2ChoiceMinimalEscapeMarkup(textfield);
        textfield.getSettings().setAllowClear(true);

        if (!optional) textfield.setRequired(true);
        textfield.add(new AttributeAppender("class", " wide"));
        textfield.add(new Validator(iri, template, "", context));
        context.getComponents().add(textfield);

        tooltipDescription = new Label("description", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                String obj = AgentChoiceItem.this.getModel().getObject();
                if (obj == null || obj.isEmpty()) return "choose a value";
                String label = getChoiceLabel(AgentChoiceItem.this.getModel().getObject());
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

            private static final long serialVersionUID = 1L;

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
     * Returns the IRI of the agent choice item.
     *
     * @return the IRI of the agent choice item
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
            if (Utils.isLocalURI(vs)) vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
            Validatable<String> validatable = new Validatable<>(vs);
            if (context.getTemplate().isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
                vs = Utils.getUriPostfix(vs);
            }
            new Validator(iri, context.getTemplate(), "", context).validate(validatable);
            if (!validatable.isValid()) {
                return false;
            }
            if (textfield.getModelObject().isEmpty()) {
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
        if (Utils.isLocalURI(vs)) {
            vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
        }
        textfield.setModelObject(vs);
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
        if (NTEMPLATE.CREATOR_PLACEHOLDER.equals(defaultValue)) {
            defaultValue = NanodashSession.get().getUserIri();
        }
        if (isUnifiableWith(defaultValue)) {
            try {
                unifyWith(defaultValue);
            } catch (UnificationException ex) {
                logger.error("Could not unify default value: {}", defaultValue, ex);
            }
        }
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "[Agent choiced item: " + iri + "]";
    }

}
