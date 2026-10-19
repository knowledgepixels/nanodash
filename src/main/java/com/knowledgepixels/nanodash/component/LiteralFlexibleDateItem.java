package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.DatePrecision;
import com.knowledgepixels.nanodash.template.DateValue;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxClientInfoBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A component for date placeholders (see {@link Template#DATE_PLACEHOLDER}), which take a point
 * in time at whatever precision the user has it and publish it typed by what was entered: a
 * year alone as an {@code xsd:gYear}, a year and month as an {@code xsd:gYearMonth}, a full date
 * as an {@code xsd:date}, and a date with a time of day as an {@code xsd:dateTime} (issue #726).
 * <p>
 * Every part up to the finest precision the template accepts is offered from the start, so the
 * field is as open as the template allows. The parts the coarsest accepted precision needs have
 * to be filled in; the others may be left out, from the finest one backwards.
 */
public class LiteralFlexibleDateItem extends AbstractContextComponent {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(LiteralFlexibleDateItem.class);

    /**
     * The value as its parts, presented to the rest of the form as the lexical form of a literal
     * of the precision the parts reach. Keeping the parts rather than the string is what lets a
     * value be entered one part at a time.
     */
    static class DateModel implements IModel<String> {

        private String year = "", month = "", day = "", time = "";

        /**
         * The timezone a coarser value than a date and time was filled in with, kept so that
         * filling in an existing value does not drop it. Empty if it had none.
         */
        private String coarseZone = "";

        /**
         * The timezone of a date and time: the one picked, or the one of the value filled in.
         * Null as long as neither has decided it, in which case the user's own applies.
         */
        private String timeZone;

        /**
         * {@inheritDoc}
         */
        @Override
        public String getObject() {
            DateValue value = assemble(year, month, day, time, null);
            return value.getProblem() == null ? value.toLexical() : "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setObject(String lexical) {
            DateValue value = DateValue.parse(lexical);
            if (value == null) {
                year = month = day = time = coarseZone = "";
                timeZone = null;
                return;
            }
            year = value.year();
            month = value.month();
            day = value.day();
            time = asShownInField(value.time());
            boolean isDateTime = value.getPrecision() == DatePrecision.DATE_TIME;
            coarseZone = isDateTime ? "" : value.zone();
            timeZone = isDateTime ? value.zone() : null;
        }

        /**
         * Assembles a value from the given parts with the timezone that goes with its precision.
         *
         * @param year  the year
         * @param month the month as two digits
         * @param day   the day as two digits
         * @param time  the time of day
         * @param zone  the timezone the zone field holds right now, or null to take the stored one
         * @return the value
         */
        DateValue assemble(String year, String month, String day, String time, String zone) {
            DateValue parts = new DateValue(year, month, day, time, "");
            if (parts.getPrecision() != DatePrecision.DATE_TIME) return parts.withZone(coarseZone);
            return parts.withZone(zone != null ? zone : getTimeZone());
        }

        /**
         * Returns the timezone of a date and time: the one decided by the user or by the value
         * filled in, or else the offset the user's own timezone has at the date and time entered.
         *
         * @return the timezone, e.g. "+01:00" (empty for a date and time without one)
         */
        String getTimeZone() {
            if (timeZone != null) return timeZone;
            ZoneId userZone = AjaxZonedDateTimePicker.getUserZoneId();
            return userZone.getRules().getOffset(enteredDateTime().orElse(LocalDateTime.now(userZone))).getId();
        }

        private Optional<LocalDateTime> enteredDateTime() {
            try {
                LocalDate date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
                return Optional.of(LocalDateTime.of(date, LocalTime.parse(time)));
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        }

        /**
         * Whether the value filled in was a date and time without a timezone, which is then
         * kept without one unless the user picks one.
         *
         * @return true if the date and time has no timezone
         */
        boolean hasNoTimeZone() {
            return "".equals(timeZone);
        }

        private static String asShownInField(String time) {
            return time.endsWith(":00") && time.length() == "HH:mm:ss".length() ? time.substring(0, "HH:mm".length()) : time;
        }

    }

    /**
     * The parts of a value, each edited by a control of its own.
     */
    private enum Part {YEAR, MONTH, DAY, TIME, ZONE}

    /**
     * A view of one part of the value, so that each control edits its own part while they all
     * share the one model the rest of the form sees.
     */
    private static class PartModel implements IModel<String> {

        private final DateModel value;
        private final Part part;

        PartModel(DateModel value, Part part) {
            this.value = value;
            this.part = part;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getObject() {
            return switch (part) {
                case YEAR -> value.year;
                case MONTH -> value.month;
                case DAY -> value.day;
                case TIME -> value.time;
                case ZONE -> value.getTimeZone().isEmpty() ? null : value.getTimeZone();
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setObject(String object) {
            String v = (object == null) ? "" : object.trim();
            switch (part) {
                case YEAR -> value.year = v;
                case MONTH -> value.month = v;
                case DAY -> value.day = v;
                case TIME -> value.time = v;
                case ZONE -> value.timeZone = v;
            }
        }

    }

    private final IRI iri;
    private final String regex;
    private final List<DatePrecision> precisions;
    private final DateModel model;
    private final Map<ZoneOffset, List<ZoneId>> zonesByOffset = AjaxZonedDateTimePicker.getZonesByCurrentOffset(Instant.now());
    private final List<FormComponent<String>> fields = new ArrayList<>();
    private final FormComponent<String> yearField, monthField, dayField, timeField, zoneField;
    private final Label datatypeLabel;

    /**
     * Constructs a LiteralFlexibleDateItem with the specified ID, IRI, optional flag, and template
     * context.
     *
     * @param id       the component ID
     * @param iri      the IRI of the placeholder
     * @param optional whether this field is optional
     * @param context  the template context containing models and parameters
     */
    public LiteralFlexibleDateItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, context);
        final Template template = context.getTemplate();
        this.iri = iri;
        this.regex = template.getRegex(iri);
        this.precisions = template.getDatePrecisions(iri);
        this.model = obtainModel(context);

        yearField = newYearField();
        monthField = newPartChoice("month", Part.MONTH, numberedChoices(12), monthRenderer(),
                !isNeeded(DatePrecision.YEAR_MONTH));
        dayField = newPartChoice("day", Part.DAY, numberedChoices(31), dayRenderer(),
                !isNeeded(DatePrecision.DATE));
        timeField = newTimeField();
        zoneField = newZoneChoice();

        addField(yearField, accepts(DatePrecision.YEAR), !optional, template);
        addField(monthField, accepts(DatePrecision.YEAR_MONTH), !optional && isNeeded(DatePrecision.YEAR_MONTH), template);
        addField(dayField, accepts(DatePrecision.DATE), !optional && isNeeded(DatePrecision.DATE), template);
        addField(timeField, accepts(DatePrecision.DATE_TIME), !optional && isNeeded(DatePrecision.DATE_TIME), template);
        addField(zoneField, accepts(DatePrecision.DATE_TIME), false, template);

        addValueCheck(this::checkNoPartSkipped);
        addValueCheck(this::checkPointInTime);
        addValueCheck(this::checkAcceptedPrecision);
        if (regex != null) addValueCheck(this::checkRegex);

        datatypeLabel = new Label("datatype", () -> describeDatatype(model.getObject()));
        datatypeLabel.setOutputMarkupId(true);
        add(datatypeLabel);
        if (accepts(DatePrecision.DATE_TIME) && AjaxZonedDateTimePicker.getClientZoneId() == null) {
            add(new ClientZoneBehavior());
        }
    }

    /**
     * Returns the model of this placeholder's value, creating it if no item has yet. A repeated
     * statement group hands the same placeholder to a second item, which has to go on editing
     * the value the first one holds.
     */
    private DateModel obtainModel(TemplateContext context) {
        Map<IRI, IModel<?>> componentModels = context.getComponentModels();
        if (componentModels.get(iri) instanceof DateModel existing) return existing;
        DateModel created = new DateModel();
        Object previous = (componentModels.get(iri) == null) ? null : componentModels.get(iri).getObject();
        if (previous != null) created.setObject(previous.toString());
        componentModels.put(iri, created);
        String postfix = Utils.getUriPostfix(iri);
        if (previous == null && context.hasParam(postfix)) {
            created.setObject(context.getParam(postfix));
            context.setParamFilled(iri);
        }
        return created;
    }

    /**
     * Whether a part is needed by every precision the template accepts, so that it cannot be
     * left out: a placeholder that takes a date at least needs the year, month and day.
     *
     * @param precision the precision that adds the part
     * @return true if even the coarsest accepted precision includes the part
     */
    private boolean isNeeded(DatePrecision precision) {
        return precisions.get(0).compareTo(precision) >= 0;
    }

    private boolean accepts(DatePrecision precision) {
        return precisions.stream().anyMatch(p -> p.compareTo(precision) >= 0);
    }

    private FormComponent<String> newYearField() {
        TextField<String> field = new TextField<>("year", new PartModel(model, Part.YEAR));
        field.add(new AttributeModifier("type", "number"));
        field.add(new AttributeModifier("placeholder", "YYYY"));
        field.add(new LiteralGregorianItem.CurrentYearOnFirstStep());
        field.add((IValidator<String>) v -> {
            if (!v.getValue().matches("-?\\d{4,}")) {
                v.error(new ValidationError("A year is four or more digits, e.g. 2026"));
            }
        });
        field.setLabel(Model.of("year of " + getFieldLabel(iri)));
        return field;
    }

    private FormComponent<String> newTimeField() {
        TextField<String> field = new TextField<>("time", new PartModel(model, Part.TIME));
        field.add(new AttributeModifier("type", "time"));
        field.add(new AttributeModifier("placeholder", "hh:mm"));
        field.add((IValidator<String>) v -> {
            if (!DateValue.isTimeOfDay(v.getValue())) {
                v.error(new ValidationError("'" + v.getValue() + "' is not a time of day, e.g. 09:00"));
            }
        });
        field.setLabel(Model.of("time of " + getFieldLabel(iri)));
        return field;
    }

    private FormComponent<String> newPartChoice(String id, Part part, List<String> choices, IChoiceRenderer<String> renderer,
                                                boolean canBeLeftOut) {
        DropDownChoice<String> field = new DropDownChoice<>(id, new PartModel(model, part), choices, renderer) {
            @Override
            protected String getNullKeyDisplayValue() {
                return "choose " + id;
            }

            @Override
            protected String getNullValidDisplayValue() {
                return "no " + id;
            }
        };
        field.setNullValid(canBeLeftOut);
        field.setLabel(Model.of(id + " of " + getFieldLabel(iri)));
        return field;
    }

    private FormComponent<String> newZoneChoice() {
        DropDownChoice<String> field = new DropDownChoice<>("zone", new PartModel(model, Part.ZONE), this::zoneChoices, zoneRenderer()) {
            @Override
            public boolean isNullValid() {
                return model.hasNoTimeZone();
            }

            @Override
            protected String getNullValidDisplayValue() {
                return "no timezone";
            }

            @Override
            protected String getNullKeyDisplayValue() {
                return "choose timezone";
            }
        };
        field.setLabel(Model.of("timezone of " + getFieldLabel(iri)));
        return field;
    }

    /**
     * Returns the timezones on offer: every offset some place is at right now, plus the one the
     * value has, which may be one no place is at today.
     */
    private List<String> zoneChoices() {
        List<ZoneOffset> offsets = new ArrayList<>(zonesByOffset.keySet());
        String current = model.getTimeZone();
        if (!current.isEmpty()) {
            ZoneOffset currentOffset = ZoneOffset.of(current);
            if (!offsets.contains(currentOffset)) offsets.add(currentOffset);
        }
        offsets.sort(Comparator.comparingInt(ZoneOffset::getTotalSeconds));
        return offsets.stream().map(ZoneOffset::getId).toList();
    }

    private IChoiceRenderer<String> zoneRenderer() {
        return partRenderer(id -> {
            ZoneOffset offset = ZoneOffset.of(id);
            return AjaxZonedDateTimePicker.getZoneChoiceLabel(offset, zonesByOffset.getOrDefault(offset, List.of()),
                    AjaxZonedDateTimePicker.getUserZoneId());
        });
    }

    /**
     * Adds one part control to the form, or a hidden placeholder for a part finer than the
     * template accepts.
     */
    private void addField(FormComponent<String> field, boolean used, boolean required, Template template) {
        add(field);
        if (!used) {
            field.setVisible(false);
            return;
        }
        field.setRequired(required);
        if (template.getLabel(iri) != null) {
            field.add(new AttributeModifier("title", template.getLabel(iri)));
        }
        field.setOutputMarkupId(true);
        field.add(new RefreshPartsOnChange(field));
        field.add(new InvalidityHighlighting());
        lockIfNeeded(field, iri);
        context.getComponents().add(field);
        fields.add(field);
    }

    /**
     * Stores a part as soon as it changes and repaints the other parts of the same value, whose
     * default timezone follows the date entered, along with the datatype the value is going to
     * have.
     * It is the only updating behavior of a part: a second one would store the part again from
     * an input that is empty by then (see {@link LiteralGregorianItem}, issue #670).
     */
    private class RefreshPartsOnChange extends OnChangeAjaxBehavior {

        private final FormComponent<String> source;

        RefreshPartsOnChange(FormComponent<String> source) {
            this.source = source;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            for (Component c : context.getComponents()) {
                if (c == source) continue;
                if (c.getDefaultModel() instanceof PartModel other && other.value == model) {
                    c.modelChanged();
                    target.add(c);
                }
            }
            target.add(datatypeLabel);
        }

    }

    /**
     * Asks the browser for its timezone where it has not said yet, and repaints the timezone
     * part once it has: until then the timezone offered is a guess from the server's own.
     */
    private class ClientZoneBehavior extends AjaxClientInfoBehavior {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onClientInfo(AjaxRequestTarget target, WebClientInfo clientInfo) {
            target.add(zoneField);
        }

    }

    /**
     * Adds a check of the value as a whole to every part, so that it runs whichever part the
     * user changed. Only the first part to report keeps its message: on a submit, where every
     * part is validated, the same complaint would otherwise be made once per part.
     */
    private void addValueCheck(IValidator<String> check) {
        for (FormComponent<String> field : fields) {
            field.add((IValidator<String>) v -> {
                if (fields.stream().anyMatch(FormComponent::hasErrorMessage)) return;
                check.validate(v);
            });
        }
    }

    /**
     * Reports a part filled in while a coarser one is left out, such as a day without a month,
     * which would otherwise be dropped from the value without a word. This is only decided on a
     * submit, so that the parts can be filled in in any order.
     */
    private void checkNoPartSkipped(IValidatable<String> v) {
        if (!yearField.getForm().isSubmitted()) return;
        List<FormComponent<String>> parts = List.of(yearField, monthField, dayField, timeField);
        List<String> names = List.of("year", "month", "day", "time");
        for (int finer = parts.size() - 1; finer > 0; finer--) {
            if (partInput(parts.get(finer)).isEmpty()) continue;
            for (int coarser = 0; coarser < finer; coarser++) {
                if (partInput(parts.get(coarser)).isEmpty()) {
                    v.error(new ValidationError("'" + getFieldLabel(iri) + "' has a " + names.get(finer)
                            + " but no " + names.get(coarser)));
                    return;
                }
            }
            return;
        }
    }

    private void checkPointInTime(IValidatable<String> v) {
        String problem = candidateValue().getProblem();
        if (problem != null) v.error(new ValidationError(problem));
    }

    /**
     * Reports a value coarser than the template accepts, or of a precision in between that it
     * does not. This is only decided on a submit: while the value is being entered part by part,
     * a year that is not yet a date may be a value on its way to becoming one.
     */
    private void checkAcceptedPrecision(IValidatable<String> v) {
        if (!yearField.getForm().isSubmitted()) return;
        DatePrecision precision = candidateValue().getPrecision();
        if (precision == null || precisions.contains(precision)) return;
        v.error(new ValidationError("Enter " + DatePrecision.describe(precisions) + " for '" + getFieldLabel(iri) + "'"));
    }

    private void checkRegex(IValidatable<String> v) {
        String candidate = candidateValue().toLexical();
        if (!candidate.isEmpty() && !candidate.matches(regex)) {
            v.error(new ValidationError("Value '" + candidate + "' of '" + getFieldLabel(iri) + "' doesn't match the pattern '" + regex + "'"));
        }
    }

    /**
     * The value the controls hold right now, rather than the one last accepted: judging the
     * stored value would measure every correction against the entry it corrects.
     */
    private DateValue candidateValue() {
        String zone = zoneField.isVisible() ? emptyAsNull(partInput(zoneField)) : null;
        return model.assemble(partInput(yearField), partInput(monthField), partInput(dayField), partInput(timeField), zone);
    }

    /**
     * What a control holds right now: the input being validated where there is one, and the
     * stored part otherwise.
     */
    private static String partInput(FormComponent<String> field) {
        if (!field.isVisible()) return "";
        String value = field.getValue();
        return (value == null) ? "" : value.trim();
    }

    private static String emptyAsNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static String describeDatatype(String lexical) {
        DateValue value = DateValue.parse(lexical);
        if (value == null) return "";
        return "(" + Utils.getDatatypeLabel(value.getPrecision().getDatatype()) + ")";
    }

    private static List<String> numberedChoices(int count) {
        List<String> choices = new ArrayList<>();
        for (int n = 1; n <= count; n++) choices.add(String.format("%02d", n));
        return choices;
    }

    private static IChoiceRenderer<String> monthRenderer() {
        return partRenderer(month -> Month.of(Integer.parseInt(month)).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
    }

    private static IChoiceRenderer<String> dayRenderer() {
        return partRenderer(day -> String.valueOf(Integer.parseInt(day)));
    }

    /**
     * A renderer that submits the part itself rather than its position in the list, so that the
     * input read while validating is the part and not an index into the choices.
     */
    private static IChoiceRenderer<String> partRenderer(SerializableFunction<String, String> label) {
        return new IChoiceRenderer<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getDisplayValue(String part) {
                return label.apply(part);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getIdValue(String part, int index) {
                return part;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getObject(String id, IModel<? extends List<? extends String>> choices) {
                return id;
            }

        };
    }

    /**
     * A {@link java.util.function.Function} that survives page serialization, as everything a
     * Wicket component holds on to has to.
     *
     * @param <T> the type of the input
     * @param <R> the type of the result
     */
    @FunctionalInterface
    private interface SerializableFunction<T, R> extends java.util.function.Function<T, R>, java.io.Serializable {
    }

    /**
     * The value as the rest of the form sees it: the lexical form of the precision entered, or ""
     * while it is not a point in time.
     *
     * @return the value model
     */
    IModel<String> getValueModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().removeAll(fields);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A value of any accepted precision unifies, whichever precision the field holds: a
     * nanopublication being superseded may well have been published with less, or more, than
     * the user now knows.
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (!(v instanceof Literal vL)) return false;
        DatePrecision precision = DatePrecision.of(vL.getDatatype());
        if (precision == null || !precisions.contains(precision)) return false;
        DateValue value = DateValue.parse(vL.stringValue());
        if (value == null || value.getPrecision() != precision) return false;
        if (regex != null && !vL.stringValue().matches(regex)) return false;
        String current = model.getObject();
        return current.isEmpty() || current.equals(vL.stringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (v == null) return;
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
        model.setObject(v.stringValue());
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
        return "[Literal flexible date item]";
    }

}
