package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.DynamicPrefix;
import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.util.Collection;
import java.util.Map;

/**
 * A text field for entering IRIs, with a prefix label and validation.
 */
public class IriTextfieldItem extends AbstractContextComponent {

    private IModel<String> prefixModel;
    private TextField<String> textfield;
    private IRI iri;
    // Non-null if the template's prefix is space-/namespace-dependent and the base has to
    // be picked by the user (see DynamicPrefix):
    private String dynamicPrefixToken;
    private Select2Choice<String> prefixChoice;
    private static final Logger logger = LoggerFactory.getLogger(IriTextfieldItem.class);

    /**
     * Constructor for creating an IRI text field item.
     *
     * @param id       the component ID
     * @param parentId the parent ID (e.g., "subj", "pred", "obj")
     * @param iriP     the IRI placeholder for this item
     * @param optional whether the field is optional
     * @param context  the template context containing models and components
     */
    public IriTextfieldItem(String id, String parentId, final IRI iriP, boolean optional, final TemplateContext context) {
        super(id, context);
        this.iri = iriP;
        final Template template = context.getTemplate();
        // Note this deliberately includes local resources: "mint the new resource under the
        // space / maintained resource instead of under the nanopublication" is the headline
        // use case of the dynamic prefix, and such placeholders are typically declared
        // nt:LocalResource + nt:IntroducedResource (issue #571).
        dynamicPrefixToken = DynamicPrefix.getToken(template.getPrefix(iri));
        IModel<String> model = (IModel<String>) context.getComponentModels().get(iri);
        boolean modelIsNew = false;
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
            modelIsNew = true;
        }
        String postfix = Utils.getUriPostfix(iri);
        // A space-/namespace-dependent prefix decides this field's namespace from the
        // navigation context (or the picker), and the field itself holds only the name
        // below it. A "param_<postfix>" carrying a value for such a field fights that --
        // typically with a full IRI, which bypasses the prefix altogether -- so it is
        // ignored here, leaving the field to be filled the same way whatever the prefix's
        // trailing path is.
        if (modelIsNew && dynamicPrefixToken == null && context.hasParam(postfix)) {
            model.setObject(context.getParam(postfix));
        }
        prefixModel = new PrefixModel(iri, context);
        // The navigation context takes precedence; only when it determines no base does the
        // user get to pick one.
        boolean showPrefixChoice = dynamicPrefixToken != null
                && DynamicPrefix.resolveFromContext(dynamicPrefixToken, context.getNavigationContextId()) == null;
        String prefix = prefixModel.getObject();
        String prefixLabel = template.getPrefixLabel(iri);
        Label prefixLabelComp;
        // The picker names what is being chosen and shows the chosen base, so the template's
        // static prefix label would only duplicate it (and read as a stray word in front of
        // a dropdown).
        if (prefixLabel == null || showPrefixChoice) {
            prefixLabelComp = new Label("prefix", "");
            prefixLabelComp.setVisible(false);
        } else {
            if (!prefixLabel.isEmpty() && parentId.equals("subj") && !Utils.isUriValue(prefixLabel)) {
                // Capitalize first letter of label if at subject position:
                prefixLabel = prefixLabel.substring(0, 1).toUpperCase() + prefixLabel.substring(1);
            }
            prefixLabelComp = new Label("prefix", prefixLabel);
        }
        add(prefixLabelComp);
        ExternalLink prefixTooltip = new ExternalLink("prefixtooltiptext", Model.of(""), new PrefixTooltipModel(iri, context));
        prefixTooltip.setOutputMarkupId(true);
        add(prefixTooltip);
        textfield = new TextField<>("textfield", model);
        if (!optional) textfield.setRequired(true);
        if (template.isLocalResource(iri) || !prefix.isEmpty() || dynamicPrefixToken != null) {
            textfield.add(new AttributeAppender("class", " short"));
        }
        textfield.add(new Validator(iri, template, prefixModel, context));
        context.getComponents().add(textfield);
        if (template.getLabel(iri) != null) {
            textfield.add(new AttributeModifier("placeholder", template.getLabel(iri).replaceFirst(" - .*$", "")));
            textfield.setLabel(Model.of(template.getLabel(iri)));
        }
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
            }

        });
        if (template.isIntroducedResource(iri) || template.isEmbeddedResource(iri)) {
            textfield.add(AttributeAppender.append("class", "introduced"));
        }
        add(textfield);
        if (showPrefixChoice) {
            initPrefixChoice(prefixTooltip);
        } else {
            add(new WebMarkupContainer("prefixchoice").setVisible(false));
        }
    }

    /**
     * Renders the picker for the base of a space-/namespace-dependent prefix, used when the
     * navigation context doesn't determine one (see docs/space-namespace-prefixes.md).
     */
    @SuppressWarnings("unchecked")
    private void initPrefixChoice(ExternalLink prefixTooltip) {
        final String token = dynamicPrefixToken;
        // One model per token, shared by every field whose prefix depends on the same
        // thing, so a base picked in one picker applies to all of them.
        IRI prefixModelKey = TemplateContext.getPrefixModelKey(token);
        IModel<String> baseModel = (IModel<String>) context.getComponentModels().get(prefixModelKey);
        if (baseModel == null) {
            baseModel = Model.of("");
            context.getComponentModels().put(prefixModelKey, baseModel);
        }
        // Any of the sharing placeholders may carry the pre-fill parameter; the first
        // one that does wins, and later ones leave an already-chosen base alone.
        String prefixParam = Utils.getUriPostfix(iri) + TemplateContext.PREFIX_SUFFIX;
        if (baseModel.getObject().isEmpty() && context.hasParam(prefixParam)) {
            baseModel.setObject(context.getParam(prefixParam));
        }
        prefixChoice = new Select2Choice<String>("prefixchoice", baseModel, new PrefixChoiceProvider(token)) {

            // The base is needed exactly when the postfix field holds something that isn't
            // already a full URI; reading the raw input keeps this independent of
            // processing order. An empty (optional) field needs no base.
            @Override
            public boolean isRequired() {
                if (super.isRequired()) return true;
                String raw = textfield.getInput();
                return raw != null && !raw.isBlank() && !Utils.isUriValue(raw);
            }

        };
        prefixChoice.setLabel(Model.of(DynamicPrefix.getSelectionLabel(token)));
        prefixChoice.getSettings().setCloseOnSelect(true);
        prefixChoice.getSettings().setPlaceholder(DynamicPrefix.getSelectionLabel(token) + "...");
        prefixChoice.getSettings().setAllowClear(true);
        prefixChoice.getSettings().setWidth("16em");
        Utils.setSelect2ChoiceMinimalEscapeMarkup(prefixChoice);
        prefixChoice.add(new ValueItem.KeepValueAfterRefreshBehavior());
        prefixChoice.add((IValidator<String>) s -> {
            if (s.getValue() != null && !s.getValue().isEmpty() && !DynamicPrefix.getOptions(token).containsKey(s.getValue())) {
                s.error(new ValidationError("Invalid choice"));
            }
        });
        prefixChoice.add(new InvalidityHighlighting());
        prefixChoice.add(new OnChangeAjaxBehavior() {

            // Every other picker on the same token holds the very same model, so refresh
            // them (and their prefix tooltips) the way shared placeholder fields are
            // refreshed elsewhere -- otherwise they'd keep showing the old base until the
            // next full render.
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == prefixChoice) continue;
                    if (c.getDefaultModel() == prefixChoice.getModel()) {
                        c.modelChanged();
                        target.add(c);
                        IriTextfieldItem sibling = c.findParent(IriTextfieldItem.class);
                        if (sibling != null) target.add(sibling.get("prefixtooltiptext"));
                    }
                }
                target.add(prefixTooltip);
            }

        });
        context.getComponents().add(prefixChoice);
        add(prefixChoice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(textfield);
        if (prefixChoice != null) {
            context.getComponents().remove(prefixChoice);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (v instanceof IRI) {
            String prefix = prefixModel.getObject();
            String vs = v.stringValue();
            if (vs.startsWith(prefix)) vs = vs.substring(prefix.length());
            if (Utils.isLocalURI(vs)) vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
            if (context.getTemplate().isAutoEscapePlaceholder(iri)) {
                vs = Utils.urlDecode(vs);
            }
            Validatable<String> validatable = new Validatable<>(vs);
            if (context.getTemplate().isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
                vs = Utils.getUriPostfix(vs);
            }
            new Validator(iri, context.getTemplate(), prefixModel, context).validate(validatable);
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
        String prefix = prefixModel.getObject();
        String vs = v.stringValue();
        if (!isUnifiableWith(v)) throw new UnificationException(vs);
        if (!prefix.isEmpty() && vs.startsWith(prefix)) {
            vs = vs.substring(prefix.length());
        } else if (Utils.isLocalURI(vs)) {
            vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
        } else if (context.getTemplate().isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
            vs = Utils.getUriPostfix(vs);
        }
        if (context.getTemplate().isAutoEscapePlaceholder(iri)) {
            vs = Utils.urlDecode(vs);
        }
        textfield.setModelObject(vs);
    }

    /**
     * The prefix a placeholder's value is built on, re-resolved on every access so that a
     * space-/namespace-dependent prefix picked after the form was built is taken into
     * account. Empty (rather than null) when the prefix is undeclared or unresolved, so
     * callers can concatenate without extra null checks.
     */
    private static class PrefixModel implements IModel<String> {

        private final IRI iri;
        private final TemplateContext context;

        PrefixModel(IRI iri, TemplateContext context) {
            this.iri = iri;
            this.context = context;
        }

        @Override
        public String getObject() {
            // A dynamic prefix wins over the template-local prefix of a local resource:
            // it is what the resource will actually be minted under (issue #571).
            if (!context.hasDynamicPrefix(iri) && context.getTemplate().isLocalResource(iri)) {
                return Utils.getUriPrefix(iri);
            }
            String prefix = context.getPrefix(iri);
            return prefix == null ? "" : prefix;
        }

    }

    /**
     * The text shown in the prefix tooltip: the resolved prefix followed by an ellipsis, or
     * the local-URI prefix for local resources; empty when there is no prefix to show.
     */
    private static class PrefixTooltipModel implements IModel<String> {

        private final IRI iri;
        private final TemplateContext context;

        PrefixTooltipModel(IRI iri, TemplateContext context) {
            this.iri = iri;
            this.context = context;
        }

        @Override
        public String getObject() {
            if (!context.hasDynamicPrefix(iri) && context.getTemplate().isLocalResource(iri)) {
                return LocalUri.PREFIX + "...";
            }
            String prefix = context.getPrefix(iri);
            if (prefix == null || prefix.isEmpty()) return "";
            return prefix + "...";
        }

    }

    /**
     * Offers the spaces / maintained resources a space-/namespace-dependent prefix can be
     * based on.
     */
    private static class PrefixChoiceProvider extends ChoiceProvider<String> {

        private final String token;

        PrefixChoiceProvider(String token) {
            this.token = token;
        }

        @Override
        public String getDisplayValue(String choiceId) {
            if (choiceId == null || choiceId.isEmpty()) return "";
            String label = DynamicPrefix.getOptions(token).get(choiceId);
            if (label == null || label.isBlank()) return choiceId;
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
            Map<String, String> options = DynamicPrefix.getOptions(token);
            if (term == null) {
                response.addAll(options.keySet());
                return;
            }
            String lowerCaseTerm = term.toLowerCase();
            for (Map.Entry<String, String> option : options.entrySet()) {
                if (option.getKey().toLowerCase().contains(lowerCaseTerm)
                        || option.getValue().toLowerCase().contains(lowerCaseTerm)) {
                    response.add(option.getKey());
                }
            }
        }

        @Override
        public Collection<String> toChoices(Collection<String> ids) {
            return ids;
        }

    }

    /**
     * Validator class for validating IRI text fields.
     */
    protected static class Validator extends InvalidityHighlighting implements IValidator<String> {

        private IRI iri;
        private Template template;
        private IModel<String> prefixModel;
        private TemplateContext context;

        /**
         * Constructor for creating a validator for an IRI text field.
         *
         * @param iri      the IRI placeholder for this item
         * @param template the template containing validation rules
         * @param prefix   the prefix to be used in validation
         * @param context  the template context containing models and components
         */
        public Validator(IRI iri, Template template, String prefix, TemplateContext context) {
            this(iri, template, Model.of(prefix), context);
        }

        /**
         * Constructor for creating a validator for an IRI text field whose prefix can change
         * while the form is open (a space-/namespace-dependent prefix).
         *
         * @param iri         the IRI placeholder for this item
         * @param template    the template containing validation rules
         * @param prefixModel the model of the prefix to be used in validation
         * @param context     the template context containing models and components
         */
        public Validator(IRI iri, Template template, IModel<String> prefixModel, TemplateContext context) {
            this.iri = iri;
            this.template = template;
            this.prefixModel = prefixModel;
            this.context = context;
        }

        @Override
        public void validate(IValidatable<String> s) {
            String sv = s.getValue();
            String p = prefixModel.getObject();
            if (p == null) p = "";
            if (template.isAutoEscapePlaceholder(iri)) {
                sv = Utils.urlEncode(sv);
            }
            if (Utils.isUriValue(sv)) {
                p = "";
            } else if (sv.contains(":") || sv.contains("#")) {
                s.error(new ValidationError("Invalid character in postfix (e.g., colon, hash)"));
            }
            String iriString = p + sv;
            if (iriString.matches("[^:# ]+")) {
                p = LocalUri.PREFIX;
                iriString = p + sv;
            }
            if (!Utils.isWellFormedUri(iriString)) {
                s.error(new ValidationError("IRI not well-formed"));
            }
            if (p.isEmpty() && !Utils.isLocalURI(sv) && !Utils.isUriValue(sv)) {
                s.error(new ValidationError("IRI scheme not allowed here; use one of: " + Utils.getAllowedUriSchemesLabel()));
            }
            String regex = template.getRegex(iri);
            if (regex != null) {
                if (!sv.matches(regex)) {
                    s.error(new ValidationError("Value '" + sv + "' doesn't match the pattern '" + regex + "'"));
                }
            }
            if (template.isExternalUriPlaceholder(iri)) {
                if (!Utils.isUriValue(iriString)) {
                    s.error(new ValidationError("Not an external IRI"));
                }
            }
            if (template.isTrustyUriPlaceholder(iri)) {
                if (!TrustyUriUtils.isPotentialTrustyUri(iriString)) {
                    s.error(new ValidationError("Not a trusty URI"));
                }
            }
            if (iri.equals(NTEMPLATE.CREATOR_PLACEHOLDER) && context.getExistingNanopub() != null) {
                boolean found = false;
                for (IRI creator : SimpleCreatorPattern.getCreators(context.getExistingNanopub())) {
                    if (creator.stringValue().equals(iriString)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    s.error(new ValidationError("Not a creator of nanopub"));
                }
            }
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
                logger.error("Could not unify default value {} with text field {}", defaultValue, this, ex);
            }
        }
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "[IRI textfield item: " + iri + "]";
    }

}
