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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.form.datetime.AjaxDateTimePicker;
import org.wicketstuff.kendo.ui.form.datetime.DateTimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * A component that represents a text field for entering literal values.
 */
public class LiteralDateTimeItem extends Panel implements ContextComponent {

    private TemplateContext context;
    private DateTimePicker dateTimePicker;
    private Label datatypeComp;
    private IModel<String> datatypeModel;
    private final String regex;
    private final IRI iri;
    private final static Logger logger = LoggerFactory.getLogger(LiteralDateTimeItem.class);
    public static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final String DATE_PATTERN = "d MMM yyyy";
    private final String TIME_PATTERN = "HH:mm:ss";

    /**
     * Constructs a LiteralDateTimeItem with the specified ID, IRI, optional flag, and template context.
     *
     * @param id       the component ID
     * @param iri      the IRI associated with this text field
     * @param optional whether this field is optional
     * @param context  the template context containing models and parameters
     */
    public LiteralDateTimeItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id);
        this.context = context;
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
                logger.error("Could not parse date from parameter: {}", context.getParam(postfix), e);
            }
        }
        DateTimePicker dateTimeComponent = initDateTimeComponent(model);
        if (!optional) dateTimeComponent.setRequired(true);
        if (context.getTemplate().getLabel(iri) != null) {
            dateTimeComponent.add(new AttributeModifier("placeholder", context.getTemplate().getLabel(iri)));
        }

        context.getComponentModels().put(iri, dateTimeComponent.getModel());
        context.getComponents().add(dateTimeComponent);
        dateTimeComponent.add(new ValueItem.KeepValueAfterRefreshBehavior());
        dateTimeComponent.add(new InvalidityHighlighting());
        dateTimeComponent.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == dateTimeComponent) continue;
                    if (c.getDefaultModel() == dateTimeComponent.getModel()) {
                        c.modelChanged();
                        target.add(c);
                    }
                }
            }
        });
        add(dateTimeComponent);

        datatypeModel = Model.of("");
        datatypeComp = new Label("datatype", datatypeModel);
        add(datatypeComp);

        DropDownChoice<String> timeZoneDropDown = new DropDownChoice<String>("timezone-dropdown",
                Model.of(TimeZone.getDefault().getID()),
                Arrays.stream(TimeZone.getAvailableIDs()).toList());
        add(timeZoneDropDown);
    }

    /**
     * Initializes the DateTimePicker component with the given model.
     *
     * @param model the model for the DateTimePicker
     * @return the initialized DateTimePicker component
     */
    protected DateTimePicker initDateTimeComponent(IModel<Date> model) {
        dateTimePicker = new AjaxDateTimePicker("datetime", model, DATE_PATTERN, TIME_PATTERN);
        return dateTimePicker;
    }

    /**
     * Returns the DateTimePicker component.
     *
     * @return the DateTimePicker component
     */
    protected DateTimePicker getDateTimeComponent() {
        return dateTimePicker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(getDateTimeComponent());
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
            if (getDateTimeComponent().getModelObject() == null) {
                return true;
            }
            IRI datatype = context.getTemplate().getDatatype(iri);
            if (!vL.getDatatype().equals(datatype)) {
                return false;
            }
            try {
                return format.parse(vL.stringValue()).toString().equals(getDateTimeComponent().getModelObject().toString());
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
        if (!isUnifiableWith(v)) {
            throw new UnificationException(v.stringValue());
        }
        Literal vL = (Literal) v;
        try {
            getDateTimeComponent().setModelObject(format.parse(vL.stringValue()));
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
        return "[Literal datetime item]";
    }

}
