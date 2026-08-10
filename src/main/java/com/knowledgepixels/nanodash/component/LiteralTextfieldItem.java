package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.Select2Choice;

import java.util.List;

/**
 * A component that represents a text field for entering literal values.
 */
public class LiteralTextfieldItem extends AbstractContextComponent {

    private AbstractTextComponent<String> textfield;
    private Label languageComp, datatypeComp;
    private IModel<String> languageModel, datatypeModel;
    private Select2Choice<String> langChoice;
    private IModel<String> langModel;
    private final String regex;
    private final IRI iri;
    private final static Logger logger = LoggerFactory.getLogger(LiteralTextfieldItem.class);

    /**
     * Constructs a LiteralTextfieldItem with the specified ID, IRI, optional flag, and template context.
     *
     * @param id       the component ID
     * @param iri      the IRI associated with this text field
     * @param optional whether this field is optional
     * @param context  the template context containing models and parameters
     */
    public LiteralTextfieldItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, context);
        final Template template = context.getTemplate();
        this.iri = iri;
        regex = template.getRegex(iri);
        IModel<String> model = (IModel<String>) context.getComponentModels().get(iri);
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
        AbstractTextComponent<String> tc = initTextComponent(model);
        if (!optional) tc.setRequired(true);
        if (context.getTemplate().getLabel(iri) != null) {
            tc.add(new AttributeModifier("placeholder", context.getTemplate().getLabel(iri)));
        }
        tc.add((IValidator<String>) s -> {
            if (regex != null) {
                if (!s.getValue().matches(regex)) {
                    s.error(new ValidationError("Value '" + s.getValue() + "' doesn't match the pattern '" + regex + "'"));
                }
            }
        });

        tc.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == tc) continue;
                    if (c.getDefaultModel() == tc.getModel()) {
                        c.modelChanged();
                        target.add(c);
                    }
                }
            }
        });
        context.getComponentModels().put(iri, tc.getModel());
        context.getComponents().add(tc);
        tc.add(new ValueItem.KeepValueAfterRefreshBehavior());
        tc.add(new InvalidityHighlighting());
        add(tc);

        languageModel = Model.of("");
        languageComp = new Label("language", languageModel);
        datatypeModel = Model.of("");
        datatypeComp = new Label("datatype", datatypeModel);
        if (template.isLanguageTagSelectable(iri)) {
            languageComp.setVisible(false);
            datatypeComp.setVisible(false);
            initLanguageChoice(optional);
        } else {
            if (template.getLanguageTag(iri) != null) {
                languageModel.setObject("(" + template.getLanguageTag(iri) + ")");
                datatypeComp.setVisible(false);
            } else if (template.getDatatype(iri) != null && !template.getDatatype(iri).equals(XSD.STRING)) {
                datatypeModel.setObject("(" + template.getDatatype(iri).stringValue().replace(XSD.NAMESPACE, "xsd:") + ")");
                languageComp.setVisible(false);
            } else {
                datatypeComp.setVisible(false);
                languageComp.setVisible(false);
            }
            add(new WebMarkupContainer("langchoice").setVisible(false));
        }
        add(languageComp);
        add(datatypeComp);
    }

    /**
     * Sets up the language-tag dropdown for a language-tag-selectable placeholder.
     */
    @SuppressWarnings("unchecked")
    private void initLanguageChoice(boolean optional) {
        final Template template = context.getTemplate();
        IRI langModelKey = TemplateContext.getLanguageModelKey(iri);
        langModel = (IModel<String>) context.getComponentModels().get(langModelKey);
        if (langModel == null) {
            langModel = Model.of("");
            context.getComponentModels().put(langModelKey, langModel);
            String langParam = Utils.getUriPostfix(iri) + TemplateContext.LANGUAGE_SUFFIX;
            if (context.hasParam(langParam)) {
                langModel.setObject(context.getParam(langParam));
            } else if (template.getLanguageTag(iri) != null) {
                langModel.setObject(template.getLanguageTag(iri));
            }
        }
        List<String> possibleTags = template.getPossibleLanguageTags(iri);
        langChoice = new Select2Choice<String>("langchoice", langModel, new LanguageTagChoiceProvider(possibleTags)) {

            // A tag is also required whenever the paired text field has input, even on
            // optional fields, so a language-tagged literal is never published untagged.
            // Reading the raw request parameter keeps this independent of processing order.
            @Override
            public boolean isRequired() {
                if (super.isRequired()) return true;
                String raw = getTextComponent().getInput();
                return raw != null && !raw.isBlank();
            }

        };
        if (!optional) langChoice.setRequired(true);
        String label = template.getLabel(iri);
        langChoice.setLabel(Model.of("language" + (label == null ? "" : " of '" + label + "'")));
        langChoice.getSettings().setCloseOnSelect(true);
        langChoice.getSettings().setPlaceholder("language");
        langChoice.getSettings().setAllowClear(true);
        langChoice.getSettings().setDropdownCssClass("langtag-dropdown");
        langChoice.getSettings().setWidth("10em");
        if (possibleTags == null) {
            langChoice.getSettings().setTags(true);
        }
        langChoice.add(new ValueItem.KeepValueAfterRefreshBehavior());
        langChoice.add(new LangTagValidator(possibleTags));
        context.getComponents().add(langChoice);
        add(langChoice);
    }

    /**
     * <p>initTextComponent.</p>
     *
     * @param model a {@link org.apache.wicket.model.IModel} object
     * @return a {@link org.apache.wicket.markup.html.form.AbstractTextComponent} object
     */
    protected AbstractTextComponent<String> initTextComponent(IModel<String> model) {
        textfield = new TextField<>("textfield", model);
        return textfield;
    }

    /**
     * <p>getTextComponent.</p>
     *
     * @return a {@link org.apache.wicket.markup.html.form.AbstractTextComponent} object
     */
    protected AbstractTextComponent<String> getTextComponent() {
        return textfield;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(getTextComponent());
        if (langChoice != null) {
            context.getComponents().remove(langChoice);
        }
    }

    /**
     * Validator for user-chosen language tags.
     */
    protected class LangTagValidator extends InvalidityHighlighting implements IValidator<String> {

        private final List<String> possibleTags;

        /**
         * Creates a validator against the given allowed tags, or any well-formed tag if null.
         *
         * @param possibleTags the allowed (normalized) language tags, or null for unrestricted
         */
        public LangTagValidator(List<String> possibleTags) {
            this.possibleTags = possibleTags;
        }

        @Override
        public void validate(IValidatable<String> s) {
            String tag = s.getValue();
            if (tag == null || tag.isEmpty()) return;
            if (!tag.matches("[a-zA-Z]{2,8}(-[0-9a-zA-Z]{1,8})*")) {
                s.error(new ValidationError("'" + tag + "' is not a valid language tag"));
            } else if (possibleTags != null && !possibleTags.contains(Literals.normalizeLanguageTag(tag))) {
                s.error(new ValidationError("Language '" + tag + "' is not among the allowed languages"));
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (v instanceof Literal vL) {
            if (regex != null && !vL.stringValue().matches(regex)) {
                return false;
            }
            if (context.getTemplate().isLanguageTagSelectable(iri)) {
                // The value must carry a tag from the allowed set (any tag if
                // unrestricted); a declared nt:hasLanguageTag is only the picker's
                // default, not a constraint. Tag checks come before the empty-text
                // shortcut so out-of-set values never unify vacuously.
                if (vL.getLanguage().isEmpty()) return false;
                String vTag = Literals.normalizeLanguageTag(vL.getLanguage().get());
                List<String> possibleTags = context.getTemplate().getPossibleLanguageTags(iri);
                if (possibleTags != null && !possibleTags.contains(vTag)) return false;
                if (getTextComponent().getModelObject() == null || getTextComponent().getModelObject().isEmpty()) {
                    return true;
                }
                if (langModel.getObject() != null && !langModel.getObject().isEmpty()
                        && !vTag.equals(Literals.normalizeLanguageTag(langModel.getObject()))) {
                    return false;
                }
                return vL.stringValue().equals(getTextComponent().getModelObject());
            }
            if (getTextComponent().getModelObject() == null || getTextComponent().getModelObject().isEmpty()) {
                return true;
            }
            String languagetag = context.getTemplate().getLanguageTag(iri);
            IRI datatype = context.getTemplate().getDatatype(iri);
            if (languagetag != null) {
                if (!vL.getLanguage().isPresent() || !Literals.normalizeLanguageTag(vL.getLanguage().get()).equals(languagetag)) {
                    return false;
                }
            } else if (datatype != null) {
                if (!vL.getDatatype().equals(datatype)) {
                    return false;
                }
            }
            return vL.stringValue().equals(getTextComponent().getModelObject());
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
        Literal vL = (Literal) v;
        getTextComponent().setModelObject(vL.stringValue());
        if (context.getTemplate().isLanguageTagSelectable(iri)) {
            // The dropdown shows the tag; the static "(tag)" label stays hidden.
            if (vL.getLanguage().isPresent()) {
                langModel.setObject(Literals.normalizeLanguageTag(vL.getLanguage().get()));
            }
        } else if (context.getTemplate().getLanguageTag(iri) == null && vL.getLanguage().isPresent()) {
            languageModel.setObject("(" + vL.getLanguage().get().toLowerCase() + ")");
            languageComp.setVisible(true);
        } else if (context.getTemplate().getDatatype(iri) == null && !vL.getDatatype().equals(XSD.STRING)) {
            datatypeModel.setObject("(" + vL.getDatatype().stringValue().replace(XSD.NAMESPACE, "xsd:") + ")");
            datatypeComp.setVisible(true);
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
                logger.error("Could not unify with default value.", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[Literal textfield item]";
    }

}
