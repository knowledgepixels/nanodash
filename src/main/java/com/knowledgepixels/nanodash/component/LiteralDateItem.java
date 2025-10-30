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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.form.datetime.AjaxDatePicker;
import org.wicketstuff.kendo.ui.form.datetime.DatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A component that represents a text field for entering literal values.
 */
public class LiteralDateItem extends AbstractContextComponent {

    private DatePicker datePicker;
    private Label datatypeComp;
    private IModel<String> datatypeModel;
    private final String regex;
    private final IRI iri;
    private final static Logger logger = LoggerFactory.getLogger(LiteralDateItem.class);
    public static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private final String DATE_PATTERN = "d MMM yyyy";

    /**
     * Constructs a LiteralDateTimeItem with the specified ID, IRI, optional flag, and template context.
     *
     * @param id       the component ID
     * @param iri      the IRI associated with this text field
     * @param optional whether this field is optional
     * @param context  the template context containing models and parameters
     */
    public LiteralDateItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, context);
        final Template template = context.getTemplate();
        this.iri = iri;
        regex = template.getRegex(iri);
        IModel<Date> model = (IModel<Date>) context.getComponentModels().get(iri);
        if (model == null) {
            model = Model.of((Date) null);
            context.getComponentModels().put(iri, model);
        }
        String postfix = Utils.getUriPostfix(iri);
        if (context.hasParam(postfix)) {
            try {
                model.setObject(format.parse(context.getParam(postfix)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        DatePicker dateComponent = initDateComponent(model);
        if (!optional) dateComponent.setRequired(true);
        if (context.getTemplate().getLabel(iri) != null) {
            dateComponent.add(new AttributeModifier("placeholder", context.getTemplate().getLabel(iri)));
        }

        context.getComponentModels().put(iri, dateComponent.getModel());
        context.getComponents().add(dateComponent);
        dateComponent.add(new ValueItem.KeepValueAfterRefreshBehavior());
        dateComponent.add(new InvalidityHighlighting());
        add(dateComponent);
        dateComponent.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == dateComponent) continue;
                    if (c.getDefaultModel() == dateComponent.getModel()) {
                        c.modelChanged();
                        target.add(c);
                    }
                }
            }
        });

        datatypeModel = Model.of("");
        datatypeComp = new Label("datatype", datatypeModel);
        add(datatypeComp);
    }

    /**
     * Initializes the date picker component with the given model.
     *
     * @param model the model to bind to the date picker
     * @return the initialized date picker component
     */
    protected DatePicker initDateComponent(IModel<Date> model) {
        datePicker = new AjaxDatePicker("date", model, DATE_PATTERN);
        return datePicker;
    }

    /**
     * Accessor for the date picker component.
     *
     * @return the date picker component
     */
    protected DatePicker getDateComponent() {
        return datePicker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(getDateComponent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) {
            return true;
        }
        if (v instanceof Literal vL) {
            if (regex != null && !vL.stringValue().matches(regex)) {
                return false;
            }
            if (getDateComponent().getModelObject() == null) {
                return true;
            }
            IRI datatype = context.getTemplate().getDatatype(iri);
            if (!vL.getDatatype().equals(datatype)) {
                return false;
            }
            try {
                return format.parse(vL.stringValue()).toString().equals(getDateComponent().getModelObject().toString());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (v == null) {
            return;
        }
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
        Literal vL = (Literal) v;
        try {
            getDateComponent().setModelObject(format.parse(vL.stringValue()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (context.getTemplate().getDatatype(iri) == null && !vL.getDatatype().equals(XSD.STRING)) {
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
        return "[Literal date item]";
    }

}
