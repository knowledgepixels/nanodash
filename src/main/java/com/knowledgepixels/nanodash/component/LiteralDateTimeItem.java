package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import org.nanopub.templates.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * A component that represents a text field for entering literal values.
 */
public class LiteralDateTimeItem extends AbstractContextComponent {

    private final static Logger logger = LoggerFactory.getLogger(LiteralDateTimeItem.class);

    /**
     * How a date-time value is written into a literal. Seconds are always written, so that a time
     * entered without them is recorded as :00 rather than left out, and a fraction of a second is
     * written only where the value has one.
     */
    public static final DateTimeFormatter format = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendOffsetId()
            .toFormatter();
    private final String DATE_PATTERN = "d MMM yyyy";
    // Seconds are left out: they are hardly ever what a user wants to fill in, and they end up as
    // :00 in the literal. The picker asks for them anyway once a value has them (see
    // AjaxZonedDateTimePicker#onConfigure), and takes them when they are typed.
    private final String TIME_PATTERN = "HH:mm";

    private AjaxZonedDateTimePicker zonedDateTimePicker;
    private final Label datatypeComp;
    private final IModel<String> datatypeModel;
    private final IRI iri;

    /**
     * Constructs a LiteralDateTimeItem with the specified ID, IRI, optional flag, and template context.
     *
     * @param id       the component ID
     * @param iri      the IRI associated with this text field
     * @param optional whether this field is optional
     * @param context  the template context containing models and parameters
     */
    public LiteralDateTimeItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, context);
        final Template template = context.getTemplate();
        this.iri = iri;
        IModel<ZonedDateTime> model = (IModel<ZonedDateTime>) context.getComponentModels().get(iri);
        boolean modelIsNew = false;
        if (model == null) {
            model = Model.of((ZonedDateTime) null);
            context.getComponentModels().put(iri, model);
            modelIsNew = true;
        }
        String postfix = Utils.getUriPostfix(iri);
        if (modelIsNew && context.hasValue(postfix)) {
            String zoncontext = context.getValue(postfix);
            if (zoncontext != null && zoncontext.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                // A date alone, as older nanopublications give for a date and time (see finalizeValues).
                model.setObject(ZonedDateTime.parse(zoncontext + "T00:00:00Z"));
            } else if (zoncontext != null) {
                model.setObject(ZonedDateTime.parse(zoncontext));
            } else {
                model.setObject(null);
            }
        }
        initZonedDateTimePicker(model);
        if (!optional) {
            zonedDateTimePicker.setRequired(true);
        }
        labelField(zonedDateTimePicker, iri);
        if (context.getTemplate().getLabel(iri) != null) {
            zonedDateTimePicker.add(new AttributeModifier("placeholder", context.getTemplate().getLabel(iri)));
        }

        context.getComponentModels().put(iri, zonedDateTimePicker.getModel());
        context.getComponents().add(zonedDateTimePicker);
        lockIfNeeded(zonedDateTimePicker, iri);
        zonedDateTimePicker.add(new ValueItem.KeepValueAfterRefreshBehavior());
        zonedDateTimePicker.add(new InvalidityHighlighting());

        add(zonedDateTimePicker);

        datatypeModel = Model.of("");
        datatypeComp = new Label("datatype", datatypeModel);
        add(datatypeComp);
    }

    /**
     * Initializes the DateTimePicker component with the given model.
     *
     * @param model the model for the DateTimePicker
     */
    protected void initZonedDateTimePicker(IModel<ZonedDateTime> model) {
        zonedDateTimePicker = new AjaxZonedDateTimePicker("zoned-datetime", model, DATE_PATTERN, TIME_PATTERN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(zonedDateTimePicker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
        if (zonedDateTimePicker.getModelObject() != null) return;
        if (!(context.getDefaultValue(iri) instanceof Literal defaultValue)) return;
        ZonedDateTime zdt;
        try {
            if (defaultValue.getLabel().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                // A date alone, as older templates give for a date and time.
                zdt = ZonedDateTime.parse(defaultValue.getLabel() + "T00:00:00Z");
            } else {
                zdt = ZonedDateTime.from(defaultValue.temporalAccessorValue());
            }
        } catch (DateTimeException | IllegalArgumentException ex) {
            logger.info("Default value is not a date and time: {}", defaultValue);
            return;
        }
        zonedDateTimePicker.setModelObject(zdt);
        if (context.getTemplate().getDatatype(iri) == null && !defaultValue.getDatatype().equals(XSD.STRING)) {
            datatypeModel.setObject("(" + Utils.getDatatypeLabel(defaultValue.getDatatype()) + ")");
            datatypeComp.setVisible(true);
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
