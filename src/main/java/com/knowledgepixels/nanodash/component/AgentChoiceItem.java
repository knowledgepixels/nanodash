package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.IriTextfieldItem.Validator;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.ProfilePage;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import com.knowledgepixels.nanodash.domain.ProfilePicture;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.validation.Validatable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
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
public class AgentChoiceItem extends AbstractContextComponent {

    private Select2Choice<String> textfield;
    private ExternalLink tooltipLink;
    private Label tooltipDescription;
    private IRI iri;
    private IModel<String> model;
    private static final Logger logger = LoggerFactory.getLogger(AgentChoiceItem.class);

    private String getChoiceLabel(String choiceId) {
        IRI iri;
        try {
            iri = vf.createIRI(choiceId);
        } catch (IllegalArgumentException ex) {
            // A manually entered local name (issue #652) isn't a URI yet -- it is minted under
            // the namespace of the nanopublication at publication time -- so there is no agent
            // to look up, and the name itself is the best label we have.
            return choiceId;
        }
        String name = User.getName(iri);
        if (name != null) return name;
        return choiceId;
    }

    /**
     * Whether a manually entered term can be minted as a local identifier, i.e. everything that
     * is neither a URI nor an ORCID and that {@link IriTextfieldItem.Validator} accepts once the
     * local prefix is put in front of it (issue #652).
     *
     * @param term the term typed into the field
     * @return true if the term can be offered as a locally minted identifier
     */
    private static boolean isLocalName(String term) {
        if (term == null || term.isBlank()) return false;
        if (Utils.isUriValue(term) || term.matches(ProfilePage.ORCID_PATTERN)) return false;
        // Same rule as the validator: no colon, hash or whitespace, and well-formed as a local URI.
        if (!term.matches("[^:#\\s]+")) return false;
        return Utils.isWellFormedUri(LocalUri.PREFIX + term);
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
        super(id, context);
        this.iri = iriP;
        final Template template = context.getTemplate();
        model = (IModel<String>) context.getComponentModels().get(iri);
        boolean modelIsNew = false;
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
            modelIsNew = true;
        }
        String postfix = Utils.getUriPostfix(iri);
        if (modelIsNew && context.hasParam(postfix)) {
            model.setObject(context.getParam(postfix));
        }
        final List<String> possibleValues = new ArrayList<>();
        for (Value v : template.getPossibleValues(iri)) {
            possibleValues.add(v.toString());
        }

        ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

            @Override
            public String getDisplayValue(String choiceId) {
                if (choiceId == null || choiceId.isEmpty()) return "";
                // A manually entered name is not an identifier yet, so it is shown as the local
                // URI it will be minted into rather than as a bare word (issue #652).
                if (context.isToBeMinted(iri, choiceId)) return Utils.getToBeMintedLabel(choiceId);
                String label = getChoiceLabel(choiceId);
                // No name to show for an agent that isn't a known user -- a manually entered URI
                // (issue #652) -- and repeating the value as its own label would only render it
                // twice.
                if (label == null || label.isBlank() || label.equals(choiceId)) {
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
                // Any URI in an allowed scheme can be entered manually, not just http(s) ones
                // (issue #652).
                final String typedTerm = term;
                if (Utils.isUriValue(term)) {
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

                // Anything else the validator accepts is offered as a locally minted identifier
                // (issue #652): typing "john-doe" mints it under the namespace of the
                // nanopublication being published. It comes last, so that the known users the
                // term matches keep the top of the list.
                if (isLocalName(typedTerm) && !response.getResults().contains(typedTerm)) {
                    response.add(typedTerm);
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
        if (placeholder == null) placeholder = "select user or type name/ORCID/URI";
        textfield.getSettings().setPlaceholder(placeholder);
        Utils.setSelect2ChoiceMinimalEscapeMarkup(textfield);
        textfield.getSettings().setAllowClear(true);

        if (!optional) textfield.setRequired(true);
        textfield.add(new AttributeAppender("class", " wide"));
        textfield.add(new Validator(iri, template, "", context));
        context.getComponents().add(textfield);

        tooltipDescription = new Label("description", new IModel<String>() {

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

        final WebMarkupContainer userIcon = new WebMarkupContainer("user-icon") {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                String selectedValue = model.getObject();
                IRI selectedIri;
                if (selectedValue != null && !selectedValue.isEmpty()) {
                    try {
                        selectedIri = vf.createIRI(selectedValue);
                    } catch (IllegalArgumentException e) {
                        // A locally minted name (issue #652) identifies somebody other than the
                        // logged-in user, so it gets the generic icon rather than their picture.
                        selectedIri = null;
                    }
                } else {
                    selectedIri = NanodashSession.get().getUserIri();
                }
                ProfilePicture profilePicture = (selectedIri != null) ? User.getProfilePicture(selectedIri) : null;
                if (profilePicture != null) {
                    tag.put("src", profilePicture.getSrc());
                } else if (selectedIri != null && IndividualAgent.isSoftware(selectedIri)) {
                    tag.put("src", urlFor(new ContextRelativeResourceReference("images/bot-icon.svg", false), null).toString());
                } else {
                    tag.put("src", urlFor(new ContextRelativeResourceReference("images/user-icon.svg", false), null).toString());
                }
            }
        };
        userIcon.setOutputMarkupId(true);
        add(userIcon);

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
                target.add(userIcon);
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

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "[Agent choiced item: " + iri + "]";
    }

}
