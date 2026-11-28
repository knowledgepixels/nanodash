package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.SparqlSyntax;
import com.knowledgepixels.nanodash.Utils;
import org.nanopub.templates.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.templates.RawValue;
import org.nanopub.templates.Violation;
import org.wicketstuff.select2.Select2Choice;

import java.util.EnumSet;
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
    private final IRI iri;
    // The language a text is checked in while its language is not picked yet.
    private static final String UNDETERMINED_LANGUAGE = "und";

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
        IModel<String> model = (IModel<String>) context.getComponentModels().get(iri);
        boolean modelIsNew = false;
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
            modelIsNew = true;
        }
        String postfix = Utils.getUriPostfix(iri);
        if (modelIsNew && context.hasValue(postfix)) {
            model.setObject(context.getValue(postfix));
        }
        AbstractTextComponent<String> tc = initTextComponent(model);
        if (!optional) tc.setRequired(true);
        labelField(tc, iri);
        if (context.getTemplate().getLabel(iri) != null) {
            tc.add(new AttributeModifier("placeholder", context.getTemplate().getLabel(iri)));
        }
        tc.add(new TextValidator());

        // A query whose SPARQL doesn't parse can never run, and a nanopublication can't be
        // edited afterwards: publishing one is a mistake that only a corrected version can
        // undo. So the same parser that will later decide whether the query works runs here,
        // while the text can still be changed (#615).
        if (template.isSparqlPlaceholder(iri)) {
            tc.add((IValidator<String>) s -> {
                String problem = SparqlSyntax.checkQuery(s.getValue());
                if (problem != null) s.error(new ValidationError(problem));
            });
        }

        tc.setOutputMarkupId(true);
        tc.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                // What is marked while typing is a value that is there but not valid. A required
                // field that has been emptied is told so on submit, like one never filled in:
                // its only error is the missing value (the other validators don't run on an
                // empty input), and it is dropped here so that it neither marks the field nor
                // turns up when the field is rendered again before then.
                if (Strings.isEmpty(tc.getInput())) {
                    tc.getFeedbackMessages().clear();
                }
                InvalidityHighlighting.refresh(target, tc);
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                InvalidityHighlighting.refresh(target, tc);
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
        lockIfNeeded(tc, iri);
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
                datatypeModel.setObject("(" + Utils.getDatatypeLabel(template.getDatatype(iri)) + ")");
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
            if (context.hasValue(langParam)) {
                langModel.setObject(context.getValue(langParam));
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
        langChoice.setLabel(Model.of("language of " + getFieldLabel(iri)));
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
        lockIfNeeded(langChoice, iri);
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
                s.error(new ValidationError("'" + tag + "' is not a valid language tag for '" + getFieldLabel(iri) + "'"));
            } else if (possibleTags != null && !possibleTags.contains(Literals.normalizeLanguageTag(tag))) {
                s.error(new ValidationError("Language '" + tag + "' is not among the allowed languages for '" + getFieldLabel(iri) + "'"));
            }
        }

    }

    /**
     * Checks the text by the rules of nanopub-java, such as the pattern of the placeholder and
     * the lexical space of a built-in XSD datatype, as the nanopublication would not be valid
     * otherwise (#751). The language is the picker's to check, so a field whose language is
     * picked is checked here as one in a language not determined. A value the form starts with,
     * such as one filled in from an existing nanopublication, is marked before the form is
     * submitted when it is not valid.
     */
    protected class TextValidator extends PlaceholderValidator {

        /**
         * Creates the validator for the text of this field.
         */
        public TextValidator() {
            super(context, iri, EnumSet.of(Violation.Kind.LANGUAGE_TAG));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected RawValue toRawValue(String text) {
            return new RawValue(text, context.getTemplate().isLanguageTagSelectable(iri) ? UNDETERMINED_LANGUAGE : null);
        }

        /**
         * {@inheritDoc}
         * <p>
         * A field whose input is not valid is marked by its {@link InvalidityHighlighting}.
         */
        @Override
        public void onComponentTag(Component c, ComponentTag tag) {
            FormComponent<?> fc = (FormComponent<?>) c;
            if (fc.isValid() && !getProblems(fc.getDefaultModelObjectAsString()).isEmpty()) {
                tag.append("class", "invalid", " ");
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
        String current = getTextComponent().getModelObject();
        if (current != null && !current.isEmpty()) return;
        if (!(context.getDefaultValue(iri) instanceof Literal defaultValue)) return;
        getTextComponent().setModelObject(defaultValue.getLabel());
        Template template = context.getTemplate();
        if (template.isLanguageTagSelectable(iri)) {
            // The dropdown shows the tag; the static "(tag)" label stays hidden.
            defaultValue.getLanguage().ifPresent(tag -> langModel.setObject(Literals.normalizeLanguageTag(tag)));
        } else if (template.getLanguageTag(iri) == null && defaultValue.getLanguage().isPresent()) {
            languageModel.setObject("(" + defaultValue.getLanguage().get().toLowerCase() + ")");
            languageComp.setVisible(true);
        } else if (template.getDatatype(iri) == null && !defaultValue.getDatatype().equals(XSD.STRING)) {
            datatypeModel.setObject("(" + Utils.getDatatypeLabel(defaultValue.getDatatype()) + ")");
            datatypeComp.setVisible(true);
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
