package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component that represents a text field for entering literal values.
 */
public class LiteralTextfieldItem extends Panel implements ContextComponent {

    private TemplateContext context;
    private AbstractTextComponent<String> textfield;
    private Label languageComp, datatypeComp;
    private IModel<String> languageModel, datatypeModel;
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
        super(id);
        this.context = context;
        final Template template = context.getTemplate();
        this.iri = iri;
        regex = template.getRegex(iri);
        IModel<String> model = (IModel<String>) context.getComponentModels().get(iri);
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
        }
        String postfix = Utils.getUriPostfix(iri);
        if (context.hasParam(postfix)) {
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
        add(languageComp);
        add(datatypeComp);
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
            if (getTextComponent().getModelObject().isEmpty()) {
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
        if (context.getTemplate().getLanguageTag(iri) == null && vL.getLanguage().isPresent()) {
            languageModel.setObject("(" + vL.getLanguage().get().toLowerCase() + ")");
            languageComp.setVisible(true);
        } else if (context.getTemplate().getDatatype(iri) == null && !vL.getDatatype().equals(XSD.STRING)) {
            datatypeModel.setObject("(" + vL.getDatatype().stringValue().replace(XSD.NAMESPACE, "xsd:") + ")");
            datatypeComp.setVisible(true);
        }

        getTextComponent().setModelObject(v.stringValue());
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
